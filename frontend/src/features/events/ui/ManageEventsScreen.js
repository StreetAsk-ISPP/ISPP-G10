import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    View,
    Text,
    StyleSheet,
    SafeAreaView,
    ScrollView,
    TouchableOpacity,
    Modal,
    Pressable,
    TextInput,
    ActivityIndicator,
    Alert,
    Switch,
    useWindowDimensions,
    Platform,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Toast from 'react-native-toast-message';

import apiClient from '../../../shared/services/http/apiClient';
import { useAuth } from '../../../app/providers/AuthProvider';
import MapPickerWeb from '../../home/ui/components/MapPickerWeb';

const EVENT_CATEGORIES = ['LEISURE', 'WELLNESS', 'CULTURE', 'GASTRONOMY', 'EMERGENCY', 'OTHER'];
const DEFAULT_FALLBACK_LAT = 37.3886;
const DEFAULT_FALLBACK_LNG = -5.9823;

const DEFAULT_FORM = {
    title: '',
    description: '',
    address: '',
    category: 'OTHER',
    endsAtInput: '',
    latitude: '',
    longitude: '',
    active: true,
    featured: false,
};

const toIsoOrNull = (value) => {
    const trimmed = String(value || '').trim();
    return trimmed.length > 0 ? trimmed : null;
};

const twoDaysFromNowLocalIso = () => {
    const date = new Date();
    date.setDate(date.getDate() + 2);
    return date.toISOString().slice(0, 19);
};

const twoDaysFromNowInput = () => formatEndsAtForInput(twoDaysFromNowLocalIso());

const parseCoordinate = (value) => {
    const normalized = String(value ?? '').trim();
    if (!normalized) {
        return null;
    }

    const parsed = Number(normalized.replace(',', '.'));
    return Number.isFinite(parsed) ? parsed : null;
};

const formatEndsAtForInput = (value) => {
    if (!value) {
        return '';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return '';
    }

    const year = parsed.getFullYear();
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const day = String(parsed.getDate()).padStart(2, '0');
    const hours = String(parsed.getHours()).padStart(2, '0');
    const minutes = String(parsed.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}`;
};

const toEndsAtIsoFromInput = (value) => {
    const raw = String(value || '').trim();
    if (!raw) {
        return null;
    }

    const normalized = raw.replace('T', ' ');
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})(?::\d{2})?$/);

    if (match) {
        return `${match[1]}T${match[2]}:00`;
    }

    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
        return null;
    }

    return parsed.toISOString().slice(0, 19);
};

const buildPayload = (formState) => {
    const latitude = parseCoordinate(formState.latitude);
    const longitude = parseCoordinate(formState.longitude);

    const payload = {
        title: formState.title.trim(),
        description: formState.description.trim(),
        address: formState.address.trim() || null,
        category: formState.category,
        endsAt: toIsoOrNull(toEndsAtIsoFromInput(formState.endsAtInput)),
        active: formState.active,
        featured: formState.featured,
    };

    if (latitude !== null && longitude !== null) {
        payload.location = { latitude, longitude };
    }

    return payload;
};

const mapEventToForm = (event) => ({
    endsAtInput: formatEndsAtForInput(event?.endsAt),
    title: event?.title || '',
    description: event?.description || '',
    address: event?.address || '',
    category: event?.category || 'OTHER',
    latitude:
        event?.location?.latitude != null ? String(event.location.latitude) : '',
    longitude:
        event?.location?.longitude != null ? String(event.location.longitude) : '',
    active: event?.active !== false,
    featured: event?.featured === true,
});

const isEventStillVisible = (event, nowTs) => {
    if (!event || event.active === false) {
        return false;
    }

    if (!event.endsAt) {
        return true;
    }

    const endsAtTs = new Date(event.endsAt).getTime();
    if (!Number.isFinite(endsAtTs)) {
        return true;
    }

    return endsAtTs > nowTs;
};

const isPastEventFromLastWeek = (event, nowTs) => {
    if (!event?.endsAt) {
        return false;
    }

    const endsAtTs = new Date(event.endsAt).getTime();
    if (!Number.isFinite(endsAtTs)) {
        return false;
    }

    const oneWeekMs = 7 * 24 * 60 * 60 * 1000;
    return endsAtTs <= nowTs && endsAtTs >= nowTs - oneWeekMs;
};

export default function ManageEventsScreen({ navigation }) {
    const { user } = useAuth();
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [events, setEvents] = useState([]);
    const [selectedEventId, setSelectedEventId] = useState(null);
    const [attendeesVisible, setAttendeesVisible] = useState(false);
    const [attendeesLoading, setAttendeesLoading] = useState(false);
    const [attendees, setAttendees] = useState([]);

    const [editorVisible, setEditorVisible] = useState(false);
    const [editorMode, setEditorMode] = useState('create');
    const [editingEventId, setEditingEventId] = useState(null);
    const [form, setForm] = useState(DEFAULT_FORM);

    const [place, setPlace] = useState('');
    const [searching, setSearching] = useState(false);
    const [searchResults, setSearchResults] = useState([]);
    const [pickedLabel, setPickedLabel] = useState('');
    const [pickMode, setPickMode] = useState(false);
    const [tempLat, setTempLat] = useState(null);
    const [tempLng, setTempLng] = useState(null);
    const [userLat, setUserLat] = useState(null);
    const [userLng, setUserLng] = useState(null);
    const [eventsNowTs, setEventsNowTs] = useState(() => Date.now());

    const isBusiness = useMemo(
        () => Array.isArray(user?.roles) && user.roles.includes('BUSINESS'),
        [user?.roles]
    );

    const activeManagedEvents = useMemo(
        () => events.filter((event) => isEventStillVisible(event, eventsNowTs)),
        [events, eventsNowTs]
    );

    const recentPastManagedEvents = useMemo(
        () => events
            .filter((event) => isPastEventFromLastWeek(event, eventsNowTs))
            .sort((a, b) => new Date(b.endsAt).getTime() - new Date(a.endsAt).getTime()),
        [events, eventsNowTs]
    );

    const selectedEvent = useMemo(
        () => activeManagedEvents.find((evt) => evt.id === selectedEventId) || null,
        [activeManagedEvents, selectedEventId]
    );

    const loadMyEvents = useCallback(async () => {
        try {
            setLoading(true);
            const res = await apiClient.get('/api/v1/events');
            const allEvents = Array.isArray(res?.data) ? res.data : [];
            const mine = allEvents.filter((event) => event?.creator?.id === user?.id);
            setEvents(mine);
        } catch (error) {
            console.error('Error loading events:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Could not load your events.',
                position: 'top',
            });
        } finally {
            setLoading(false);
        }
    }, [user?.id]);

    useEffect(() => {
        const intervalId = setInterval(() => {
            setEventsNowTs(Date.now());
        }, 30000);

        return () => clearInterval(intervalId);
    }, []);

    useEffect(() => {
        if (activeManagedEvents.length === 0) {
            setSelectedEventId(null);
            return;
        }

        if (!activeManagedEvents.some((evt) => evt.id === selectedEventId)) {
            setSelectedEventId(activeManagedEvents[0].id);
        }
    }, [activeManagedEvents, selectedEventId]);

    useEffect(() => {
        loadMyEvents();
    }, [loadMyEvents]);

    const getCurrentPositionWeb = useCallback(() => {
        if (Platform.OS !== 'web' || !navigator.geolocation) {
            return Promise.resolve(null);
        }

        return new Promise((resolve) => {
            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    resolve({
                        lat: pos.coords.latitude,
                        lng: pos.coords.longitude,
                    });
                },
                () => resolve(null),
                { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
            );
        });
    }, []);

    useEffect(() => {
        let isMounted = true;

        const preloadCurrentLocation = async () => {
            const coords = await getCurrentPositionWeb();
            if (!isMounted || !coords) {
                return;
            }

            setUserLat(coords.lat);
            setUserLng(coords.lng);
        };

        preloadCurrentLocation();

        return () => {
            isMounted = false;
        };
    }, [getCurrentPositionWeb]);

    const openCreate = () => {
        setEditorMode('create');
        setEditingEventId(null);
        setForm({ ...DEFAULT_FORM, endsAtInput: twoDaysFromNowInput() });
        setPlace('');
        setPickedLabel('');
        setSearchResults([]);
        setEditorVisible(true);
    };

    const openEdit = (eventToEdit = selectedEvent) => {
        if (!eventToEdit) {
            Alert.alert('Select an event', 'Choose the event you want to edit first.');
            return;
        }

        setEditorMode('edit');
        setEditingEventId(eventToEdit.id);
        setSelectedEventId(eventToEdit.id);
        setForm(mapEventToForm(eventToEdit));
        setPlace(eventToEdit?.address || '');
        setPickedLabel('');
        setSearchResults([]);
        setEditorVisible(true);
    };

    const searchAddress = async () => {
        const q = place.trim();
        if (!q) {
            Toast.show({
                type: 'info',
                text1: 'Empty address',
                text2: 'Enter an address or place to search.',
                position: 'top',
            });
            return;
        }

        setSearching(true);
        try {
            const url = `https://nominatim.openstreetmap.org/search?format=json&limit=5&addressdetails=1&q=${encodeURIComponent(
                q
            )}`;
            const res = await fetch(url, { headers: { Accept: 'application/json' } });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            const items = (data || []).map((it) => ({
                label: it.display_name,
                lat: Number(it.lat),
                lon: Number(it.lon),
            }));

            setSearchResults(items);

            if (items.length === 0) {
                Toast.show({
                    type: 'info',
                    text1: 'No results',
                    text2: 'No address was found.',
                    position: 'top',
                });
            }

            if (items.length === 1) {
                setForm((prev) => ({
                    ...prev,
                    latitude: String(items[0].lat),
                    longitude: String(items[0].lon),
                    address: items[0].label,
                }));
                setPlace(items[0].label);
                setPickedLabel(items[0].label);
                setSearchResults([]);
            }
        } catch (e) {
            console.error('Nominatim search error:', e);
            Toast.show({
                type: 'error',
                text1: 'Search error',
                text2: 'Could not search the address.',
                position: 'top',
            });
        } finally {
            setSearching(false);
        }
    };

    const openMapPick = useCallback(async () => {
        let nextLat = parseCoordinate(form.latitude);
        let nextLng = parseCoordinate(form.longitude);

        if (nextLat === null || nextLng === null) {
            if (Number.isFinite(userLat) && Number.isFinite(userLng)) {
                nextLat = userLat;
                nextLng = userLng;
            } else {
                const coords = await getCurrentPositionWeb();
                if (coords) {
                    nextLat = coords.lat;
                    nextLng = coords.lng;
                    setUserLat(coords.lat);
                    setUserLng(coords.lng);
                }
            }
        }

        setTempLat(Number.isFinite(nextLat) ? nextLat : DEFAULT_FALLBACK_LAT);
        setTempLng(Number.isFinite(nextLng) ? nextLng : DEFAULT_FALLBACK_LNG);
        setPickMode(true);
    }, [form.latitude, form.longitude, getCurrentPositionWeb, userLat, userLng]);

    const cancelMapPick = () => {
        setPickMode(false);
        setTempLat(null);
        setTempLng(null);
    };

    const confirmMapPick = () => {
        if (typeof tempLat !== 'number' || typeof tempLng !== 'number') {
            Toast.show({
                type: 'info',
                text1: 'Select a point',
                text2: 'Tap the map to choose a location.',
                position: 'top',
            });
            return;
        }

        setForm((prev) => ({
            ...prev,
            latitude: String(tempLat),
            longitude: String(tempLng),
        }));
        setPlace(`(${tempLat.toFixed(5)}, ${tempLng.toFixed(5)})`);
        setPickedLabel('');
        setSearchResults([]);
        setPickMode(false);
    };

    const handleDelete = (eventToDelete = selectedEvent) => {
        if (!eventToDelete) {
            Alert.alert('Select an event', 'Choose the event you want to delete first.');
            return;
        }

        const performDelete = async () => {
            try {
                setSubmitting(true);
                await apiClient.delete(`/api/v1/events/${eventToDelete.id}`);
                Toast.show({
                    type: 'success',
                    text1: 'Event deleted',
                    text2: 'The event was deleted successfully.',
                    position: 'top',
                });
                await loadMyEvents();
            } catch (error) {
                console.error('Error deleting event:', error);
                Toast.show({
                    type: 'error',
                    text1: 'Error',
                    text2: 'Could not delete the event.',
                    position: 'top',
                });
            } finally {
                setSubmitting(false);
            }
        };

        performDelete();
    };

    const openAttendees = useCallback(async (eventToInspect = selectedEvent) => {
        if (!eventToInspect) {
            Alert.alert('Select an event', 'Choose the event you want to review first.');
            return;
        }

        try {
            setAttendeesVisible(true);
            setAttendeesLoading(true);
            const response = await apiClient.get(`/api/v1/events/${eventToInspect.id}/attendees`);
            setAttendees(Array.isArray(response?.data) ? response.data : []);
        } catch (error) {
            console.error('Error loading attendees:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Could not load attendees.',
                position: 'top',
            });
            setAttendeesVisible(false);
        } finally {
            setAttendeesLoading(false);
        }
    }, [selectedEvent]);

    const submitEditor = async () => {
        const payload = buildPayload(form);

        if (!payload.title || !payload.description) {
            Alert.alert('Required fields', 'Title and description are required.');
            return;
        }

        const endsAtInput = form.endsAtInput?.trim();
        if (endsAtInput && !toEndsAtIsoFromInput(endsAtInput)) {
            Alert.alert('Invalid date', 'Use format YYYY-MM-DD HH:MM.');
            return;
        }

        try {
            setSubmitting(true);
            if (editorMode === 'create') {
                const createPayload = {
                    ...payload,
                    active: true,
                    endsAt: payload.endsAt || twoDaysFromNowLocalIso(),
                };
                await apiClient.post('/api/v1/events', createPayload);
                Toast.show({
                    type: 'success',
                    text1: 'Event created',
                    text2: 'Your event was created successfully.',
                    position: 'top',
                });
            } else {
                const eventIdToUpdate = editingEventId || selectedEvent?.id;
                if (!eventIdToUpdate) {
                    Alert.alert('Error', 'No event selected to edit.');
                    return;
                }
                await apiClient.put(`/api/v1/events/${eventIdToUpdate}`, payload);
                Toast.show({
                    type: 'success',
                    text1: 'Event updated',
                    text2: 'Changes were saved successfully.',
                    position: 'top',
                });
            }

            setEditorVisible(false);
            setEditingEventId(null);
            setForm(DEFAULT_FORM);
            setPlace('');
            setPickedLabel('');
            setSearchResults([]);
            await loadMyEvents();
        } catch (error) {
            console.error('Error saving event:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Could not save the event. Please review the data.',
                position: 'top',
            });
        } finally {
            setSubmitting(false);
        }
    };

    if (!isBusiness) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={styles.header}>
                    <TouchableOpacity style={styles.iconBtn} onPress={() => navigation.goBack()}>
                        <Ionicons name="arrow-back" size={20} color="#374151" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>Event management</Text>
                    <View style={styles.iconBtnPlaceholder} />
                </View>
                <View style={styles.centerBox}>
                    <Ionicons name="lock-closed-outline" size={42} color="#6b7280" />
                    <Text style={styles.centerTitle}>Business accounts only</Text>
                    <Text style={styles.centerText}>
                        You need a BUSINESS account to create, edit, or delete events.
                    </Text>
                </View>
            </SafeAreaView>
        );
    }

    if (pickMode) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={{ flex: 1 }}>
                    <View style={styles.mapFull}>
                        <MapPickerWeb
                            latitude={tempLat}
                            longitude={tempLng}
                            userLat={userLat ?? tempLat}
                            userLng={userLng ?? tempLng}
                            pickEnabled
                            tempLat={tempLat}
                            tempLng={tempLng}
                            onPick={(lat, lng) => {
                                setTempLat(lat);
                                setTempLng(lng);
                            }}
                        />
                    </View>

                    <View style={styles.mapOverlay} pointerEvents="box-none">
                        <View style={styles.mapHint} pointerEvents="auto">
                            <Text style={styles.mapHintText}>Tap the map to choose a location</Text>
                            <Text style={styles.mapHintCoords}>
                                {tempLat?.toFixed?.(5) ?? '--'}, {tempLng?.toFixed?.(5) ?? '--'}
                            </Text>
                        </View>

                        <View style={styles.mapBtnRow} pointerEvents="auto">
                            <TouchableOpacity
                                style={styles.mapCancelBtn}
                                onPress={cancelMapPick}
                                activeOpacity={0.8}
                            >
                                <Text style={styles.mapCancelLabel}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.mapOkBtn}
                                onPress={confirmMapPick}
                                activeOpacity={0.8}
                            >
                                <Text style={styles.mapConfirmLabel}>Confirm</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.screen}>
            <View style={{ flex: 1, position: 'relative' }}>
                <View style={styles.mapBgPreview}>
                    <MapPickerWeb
                        latitude={parseCoordinate(form.latitude) ?? userLat ?? DEFAULT_FALLBACK_LAT}
                        longitude={parseCoordinate(form.longitude) ?? userLng ?? DEFAULT_FALLBACK_LNG}
                        userLat={userLat || DEFAULT_FALLBACK_LAT}
                        userLng={userLng || DEFAULT_FALLBACK_LNG}
                        pickEnabled={false}
                        tempLat={tempLat}
                        tempLng={tempLng}
                        onPick={() => { }}
                    />
                </View>

                <ScrollView
                    style={styles.formScroll}
                    contentContainerStyle={[
                        styles.formScrollContent,
                        { maxWidth: isNarrow ? '100%' : 620, alignSelf: 'center', width: '100%' },
                    ]}
                    showsVerticalScrollIndicator={false}
                >
                    <View style={styles.card}>
                        <View style={styles.cardTopRow}>
                            <TouchableOpacity
                                style={styles.backRow}
                                onPress={() => navigation.goBack()}
                                activeOpacity={0.7}
                            >
                                <Ionicons name="chevron-back" size={22} color="#6b7280" />
                                <Text style={styles.backText}>Back</Text>
                            </TouchableOpacity>

                            <TouchableOpacity style={styles.iconBtn} onPress={loadMyEvents}>
                                <Ionicons name="refresh-outline" size={20} color="#374151" />
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.heading}>Manage Events</Text>

                        <TouchableOpacity
                            style={[styles.createActionBtn, submitting && styles.submitBtnDisabled]}
                            onPress={openCreate}
                            disabled={submitting}
                            activeOpacity={0.8}
                        >
                            <Ionicons name="add-circle-outline" size={18} color="#fff" />
                            <Text style={styles.createActionText}>Create event</Text>
                        </TouchableOpacity>

                        {loading ? (
                            <View style={styles.centerBox}>
                                <ActivityIndicator size="large" color="#a52019" />
                                <Text style={styles.centerText}>Loading events...</Text>
                            </View>
                        ) : activeManagedEvents.length === 0 && recentPastManagedEvents.length === 0 ? (
                            <View style={styles.centerBox}>
                                <Ionicons name="calendar-clear-outline" size={44} color="#9ca3af" />
                                <Text style={styles.centerTitle}>No events</Text>
                                <Text style={styles.centerText}>
                                    You do not have any events right now.
                                </Text>
                            </View>
                        ) : (
                            <>
                                <View style={styles.sectionHeader}>
                                    <Text style={styles.sectionTitle}>Active events</Text>
                                    <Text style={styles.sectionCount}>{activeManagedEvents.length}</Text>
                                </View>

                                {activeManagedEvents.length === 0 ? (
                                    <Text style={styles.sectionEmptyText}>You do not have any active events.</Text>
                                ) : (
                                    activeManagedEvents.map((item) => {
                                        const selected = item.id === selectedEventId;
                                        return (
                                            <TouchableOpacity
                                                key={item.id}
                                                activeOpacity={0.8}
                                                onPress={() => setSelectedEventId(item.id)}
                                                style={[styles.eventCard, selected && styles.eventCardSelected]}
                                            >
                                                <View style={styles.eventCardHeader}>
                                                    <Text style={styles.eventTitle}>{item.title || 'Untitled'}</Text>
                                                    <Text style={styles.eventCategory}>{item.category || 'OTHER'}</Text>
                                                </View>
                                                <Text style={styles.eventDescription} numberOfLines={2}>
                                                    {item.description || 'No description'}
                                                </Text>
                                                <Text style={styles.eventMeta}>
                                                    {item.address || 'No address'}
                                                </Text>

                                                <View style={styles.rowActions}>
                                                    <TouchableOpacity
                                                        style={[styles.rowActionBtn, styles.rowEditBtn]}
                                                        onPress={() => openEdit(item)}
                                                        disabled={submitting}
                                                    >
                                                        <Ionicons name="pencil-outline" size={16} color="#fff" />
                                                        <Text style={styles.rowActionText}>Edit</Text>
                                                    </TouchableOpacity>

                                                    <TouchableOpacity
                                                        style={[styles.rowActionBtn, styles.rowDeleteBtn]}
                                                        onPress={() => handleDelete(item)}
                                                        disabled={submitting}
                                                    >
                                                        <Ionicons name="trash-outline" size={16} color="#fff" />
                                                        <Text style={styles.rowActionText}>Delete</Text>
                                                    </TouchableOpacity>

                                                    <TouchableOpacity
                                                        style={[styles.rowActionBtn, styles.rowViewBtn]}
                                                        onPress={() => openAttendees(item)}
                                                        disabled={submitting}
                                                    >
                                                        <Ionicons name="people-outline" size={16} color="#fff" />
                                                        <Text style={styles.rowActionText}>See attendants</Text>
                                                    </TouchableOpacity>
                                                </View>
                                            </TouchableOpacity>
                                        );
                                    })
                                )}

                                <View style={[styles.sectionHeader, styles.sectionHeaderPast]}>
                                    <Text style={styles.sectionTitle}>Past (last week)</Text>
                                    <Text style={styles.sectionCount}>{recentPastManagedEvents.length}</Text>
                                </View>

                                {recentPastManagedEvents.length === 0 ? (
                                    <Text style={styles.sectionEmptyText}>No recent past events.</Text>
                                ) : (
                                    recentPastManagedEvents.map((item) => (
                                        <View key={item.id} style={[styles.eventCard, styles.pastEventCard]}>
                                            <View style={styles.eventCardHeader}>
                                                <Text style={styles.eventTitle}>{item.title || 'Untitled'}</Text>
                                                <Text style={styles.eventCategory}>{item.category || 'OTHER'}</Text>
                                            </View>
                                            <Text style={styles.eventDescription} numberOfLines={2}>
                                                {item.description || 'No description'}
                                            </Text>
                                            <Text style={styles.eventMeta}>{item.address || 'No address'}</Text>
                                            <Text style={styles.eventMeta}>
                                                Ended: {formatEndsAtForInput(item.endsAt) || 'No date'}
                                            </Text>
                                        </View>
                                    ))
                                )}
                            </>
                        )}
                    </View>
                </ScrollView>
            </View>

            <Modal
                visible={editorVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setEditorVisible(false)}
            >
                <Pressable style={styles.modalOverlay} onPress={() => setEditorVisible(false)}>
                    <Pressable style={styles.editorCard} onPress={() => { }}>
                        <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
                            <View style={styles.editorHeader}>
                                <Text style={styles.editorTitle}>
                                    {editorMode === 'create' ? 'Create event' : 'Edit event'}
                                </Text>
                                <TouchableOpacity onPress={() => setEditorVisible(false)}>
                                    <Ionicons name="close" size={22} color="#6b7280" />
                                </TouchableOpacity>
                            </View>

                            <Text style={styles.sectionLabel}>Location</Text>
                            <View style={styles.locationInputWrapper}>
                                <Ionicons
                                    name="search-outline"
                                    size={18}
                                    color="#9ca3af"
                                    style={{ marginRight: 8 }}
                                />
                                <TextInput
                                    value={place}
                                    onChangeText={(text) => {
                                        setPlace(text);
                                        setSearchResults([]);
                                        setPickedLabel('');
                                    }}
                                    onSubmitEditing={searchAddress}
                                    returnKeyType="search"
                                    placeholder="Search address or place..."
                                    placeholderTextColor="#9ca3af"
                                    style={styles.locationInput}
                                />
                            </View>

                            <View style={styles.locationBtnRow}>
                                <TouchableOpacity
                                    style={[styles.locationBtnOutline, { flex: 1, opacity: searching ? 0.5 : 1 }]}
                                    onPress={searchAddress}
                                    disabled={searching}
                                    activeOpacity={0.7}
                                >
                                    <Ionicons name="search" size={16} color="#a52019" />
                                    <Text style={styles.locationBtnOutlineText}>
                                        {searching ? 'Searching...' : 'Search'}
                                    </Text>
                                </TouchableOpacity>

                                <TouchableOpacity
                                    style={[styles.locationBtnOutline, { flex: 1 }]}
                                    onPress={openMapPick}
                                    activeOpacity={0.7}
                                >
                                    <Ionicons name="location" size={16} color="#a52019" />
                                    <Text style={styles.locationBtnOutlineText}>Pick on map</Text>
                                </TouchableOpacity>
                            </View>

                            {searchResults.length > 1 &&
                                searchResults.map((result, idx) => (
                                    <TouchableOpacity
                                        key={`${result.label}-${idx}`}
                                        style={styles.resultItem}
                                        onPress={() => {
                                            setForm((prev) => ({
                                                ...prev,
                                                latitude: String(result.lat),
                                                longitude: String(result.lon),
                                                address: result.label,
                                            }));
                                            setPlace(result.label);
                                            setPickedLabel(result.label);
                                            setSearchResults([]);
                                        }}
                                        activeOpacity={0.7}
                                    >
                                        <Ionicons name="location-outline" size={16} color="#667eea" />
                                        <Text style={styles.resultText} numberOfLines={2}>
                                            {result.label}
                                        </Text>
                                    </TouchableOpacity>
                                ))}

                            <View style={styles.selectedRow}>
                                <Ionicons name="pin" size={14} color="#6b7280" />
                                <Text style={styles.selectedText} numberOfLines={1}>
                                    Selected:{' '}
                                    {pickedLabel
                                        ? pickedLabel
                                        : form.latitude && form.longitude
                                            ? `(${parseCoordinate(form.latitude)?.toFixed(5)}, ${parseCoordinate(form.longitude)?.toFixed(5)})`
                                            : 'None'}
                                </Text>
                            </View>

                            <TextInput
                                style={styles.input}
                                placeholder="Title *"
                                value={form.title}
                                onChangeText={(value) => setForm((prev) => ({ ...prev, title: value }))}
                            />

                            <TextInput
                                style={[styles.input, styles.textArea]}
                                placeholder="Description *"
                                multiline
                                value={form.description}
                                onChangeText={(value) => setForm((prev) => ({ ...prev, description: value }))}
                            />

                            <TextInput
                                style={styles.input}
                                placeholder="Address"
                                value={form.address}
                                onChangeText={(value) => setForm((prev) => ({ ...prev, address: value }))}
                            />

                            <Text style={styles.sectionLabel}>Category</Text>
                            <View style={styles.chipsRow}>
                                {EVENT_CATEGORIES.map((category) => {
                                    const selected = category === form.category;
                                    return (
                                        <TouchableOpacity
                                            key={category}
                                            style={[styles.chip, selected && styles.chipSelected]}
                                            onPress={() => setForm((prev) => ({ ...prev, category }))}
                                        >
                                            <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                                                {category}
                                            </Text>
                                        </TouchableOpacity>
                                    );
                                })}
                            </View>

                            <Text style={styles.sectionLabel}>End date</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="YYYY-MM-DD HH:MM"
                                value={form.endsAtInput}
                                onChangeText={(value) => setForm((prev) => ({ ...prev, endsAtInput: value }))}
                            />

                            <View style={styles.switchRow}>
                                <Text style={styles.switchLabel}>Featured</Text>
                                <Switch
                                    value={form.featured}
                                    onValueChange={(value) => setForm((prev) => ({ ...prev, featured: value }))}
                                    trackColor={{ false: '#d1d5db', true: '#a52019' }}
                                    thumbColor="#fff"
                                />
                            </View>

                            <TouchableOpacity
                                style={[styles.submitBtn, submitting && styles.submitBtnDisabled]}
                                disabled={submitting}
                                onPress={submitEditor}
                            >
                                {submitting ? (
                                    <ActivityIndicator color="#fff" />
                                ) : (
                                    <Text style={styles.submitBtnText}>
                                        {editorMode === 'create' ? 'Create event' : 'Save changes'}
                                    </Text>
                                )}
                            </TouchableOpacity>
                        </ScrollView>
                    </Pressable>
                </Pressable>
            </Modal>

            <Modal
                visible={attendeesVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setAttendeesVisible(false)}
            >
                <Pressable style={styles.modalOverlay} onPress={() => setAttendeesVisible(false)}>
                    <Pressable style={styles.editorCard} onPress={() => { }}>
                        <View style={styles.editorHeader}>
                            <Text style={styles.editorTitle}>Attendees</Text>
                            <TouchableOpacity onPress={() => setAttendeesVisible(false)}>
                                <Ionicons name="close" size={22} color="#6b7280" />
                            </TouchableOpacity>
                        </View>

                        {attendeesLoading ? (
                            <View style={styles.centerBox}>
                                <ActivityIndicator size="large" color="#a52019" />
                                <Text style={styles.centerText}>Loading attendees...</Text>
                            </View>
                        ) : attendees.length === 0 ? (
                            <View style={styles.centerBox}>
                                <Ionicons name="people-outline" size={44} color="#9ca3af" />
                                <Text style={styles.centerTitle}>No attendees</Text>
                                <Text style={styles.centerText}>No one has joined yet.</Text>
                            </View>
                        ) : (
                            <ScrollView showsVerticalScrollIndicator={false}>
                                {attendees.map((attendee) => (
                                    <View key={attendee.id} style={styles.attendeeRow}>
                                        <View style={styles.attendeeAvatar}>
                                            <Text style={styles.attendeeAvatarText}>
                                                {(attendee.firstName?.[0] || attendee.userName?.[0] || '?').toUpperCase()}
                                            </Text>
                                        </View>
                                        <View style={styles.attendeeInfo}>
                                            <Text style={styles.attendeeName} numberOfLines={1}>
                                                {attendee.firstName || attendee.userName || 'No name'}{' '}
                                                {attendee.lastName || ''}
                                            </Text>
                                            <Text style={styles.attendeeMeta} numberOfLines={1}>
                                                @{attendee.userName || 'unknown'} · {attendee.email || 'no email'}
                                            </Text>
                                        </View>
                                    </View>
                                ))}
                            </ScrollView>
                        )}
                    </Pressable>
                </Pressable>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f3f4f6',
    },
    mapBgPreview: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 0 },
    formScroll: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 10 },
    formScrollContent: { padding: 16, paddingTop: 36, paddingBottom: 40 },
    card: {
        backgroundColor: 'rgba(255,255,255,0.92)',
        borderRadius: 20,
        padding: 22,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.08,
        shadowRadius: 24,
        elevation: 8,
        backdropFilter: 'blur(12px)',
    },
    cardTopRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 14,
    },
    backRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
    backText: { fontSize: 14, color: '#6b7280', fontWeight: '500' },
    heading: { fontSize: 24, fontWeight: '800', color: '#1f2937', marginBottom: 16 },
    sectionHeader: {
        marginTop: 8,
        marginBottom: 10,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    sectionHeaderPast: {
        marginTop: 14,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '800',
        color: '#1f2937',
    },
    sectionCount: {
        minWidth: 26,
        height: 26,
        borderRadius: 13,
        textAlign: 'center',
        textAlignVertical: 'center',
        backgroundColor: '#e5e7eb',
        color: '#374151',
        fontWeight: '800',
        fontSize: 12,
        overflow: 'hidden',
        paddingTop: 5,
    },
    sectionEmptyText: {
        color: '#6b7280',
        fontSize: 14,
        marginBottom: 10,
    },
    createActionBtn: {
        minHeight: 46,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'row',
        gap: 8,
        backgroundColor: '#a52019',
        marginBottom: 14,
    },
    createActionText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 14,
    },
    header: {
        height: 56,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
        paddingHorizontal: 16,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#1f2937',
    },
    iconBtn: {
        width: 36,
        height: 36,
        borderRadius: 10,
        backgroundColor: '#f3f4f6',
        alignItems: 'center',
        justifyContent: 'center',
    },
    iconBtnPlaceholder: {
        width: 36,
        height: 36,
    },
    actionRow: {
        flexDirection: 'row',
        gap: 10,
        padding: 14,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
    },
    actionRowNarrow: {
        flexWrap: 'wrap',
    },
    actionBtn: {
        flex: 1,
        minHeight: 44,
        borderRadius: 12,
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'row',
        gap: 8,
    },
    createBtn: {
        backgroundColor: '#a52019',
    },
    editBtn: {
        backgroundColor: '#2563eb',
    },
    deleteBtn: {
        backgroundColor: '#dc2626',
    },
    actionBtnText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 14,
    },
    listContent: {
        padding: 14,
        paddingBottom: 40,
    },
    eventCard: {
        backgroundColor: '#fff',
        borderRadius: 14,
        padding: 14,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        marginBottom: 10,
    },
    pastEventCard: {
        backgroundColor: '#f9fafb',
        borderColor: '#d1d5db',
    },
    eventCardSelected: {
        borderColor: '#a52019',
        shadowColor: '#a52019',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.12,
        shadowRadius: 10,
        elevation: 4,
    },
    eventCardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 6,
        gap: 8,
    },
    eventTitle: {
        flex: 1,
        fontSize: 16,
        fontWeight: '700',
        color: '#1f2937',
    },
    eventCategory: {
        color: '#0f766e',
        fontSize: 12,
        fontWeight: '700',
    },
    eventDescription: {
        color: '#4b5563',
        fontSize: 14,
    },
    eventMeta: {
        marginTop: 6,
        color: '#6b7280',
        fontSize: 12,
    },
    rowActions: {
        marginTop: 12,
        flexDirection: 'row',
        gap: 8,
    },
    rowActionBtn: {
        minHeight: 34,
        borderRadius: 10,
        paddingHorizontal: 10,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
    },
    rowEditBtn: {
        backgroundColor: '#2563eb',
    },
    rowDeleteBtn: {
        backgroundColor: '#dc2626',
    },
    rowViewBtn: {
        backgroundColor: '#0f766e',
    },
    rowActionText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 12,
    },
    centerBox: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 24,
    },
    centerTitle: {
        marginTop: 12,
        fontSize: 18,
        fontWeight: '700',
        color: '#374151',
    },
    centerText: {
        marginTop: 8,
        fontSize: 14,
        color: '#6b7280',
        textAlign: 'center',
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.35)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
    },
    editorCard: {
        width: '100%',
        maxWidth: 520,
        maxHeight: '88%',
        backgroundColor: '#fff',
        borderRadius: 18,
        padding: 16,
    },
    attendeeRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        paddingVertical: 10,
        borderBottomWidth: 1,
        borderBottomColor: '#eef2f7',
    },
    attendeeAvatar: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
    },
    attendeeAvatarText: {
        color: '#fff',
        fontWeight: '800',
        fontSize: 14,
    },
    attendeeInfo: {
        flex: 1,
    },
    attendeeName: {
        fontSize: 15,
        fontWeight: '700',
        color: '#1f2937',
    },
    attendeeMeta: {
        marginTop: 2,
        fontSize: 12,
        color: '#6b7280',
    },
    mapFull: { flex: 1, width: '100%', height: '100%' },
    mapOverlay: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        padding: 16,
        zIndex: 100,
        justifyContent: 'space-between',
    },
    mapHint: {
        backgroundColor: 'rgba(255,255,255,0.95)',
        padding: 16,
        borderRadius: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.12,
        shadowRadius: 12,
        elevation: 6,
    },
    mapHintText: { fontWeight: '700', fontSize: 15, color: '#1f2937', textAlign: 'center' },
    mapHintCoords: { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 6 },
    mapBtnRow: { flexDirection: 'row', gap: 12 },
    mapCancelBtn: {
        flex: 1,
        backgroundColor: '#fff',
        borderRadius: 14,
        paddingVertical: 14,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    mapOkBtn: {
        flex: 1,
        backgroundColor: '#a52019',
        borderRadius: 14,
        paddingVertical: 14,
        alignItems: 'center',
    },
    mapCancelLabel: { fontWeight: '700', fontSize: 15, color: '#1f2937' },
    mapConfirmLabel: { fontWeight: '700', fontSize: 15, color: '#fff' },
    locationInputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#f9fafb',
        borderWidth: 1.5,
        borderColor: '#e5e7eb',
        borderRadius: 12,
        paddingHorizontal: 14,
        height: 48,
        marginBottom: 10,
    },
    locationInput: {
        flex: 1,
        fontSize: 14,
        color: '#111827',
    },
    locationBtnRow: {
        flexDirection: 'row',
        gap: 10,
        marginBottom: 10,
    },
    locationBtnOutline: {
        minHeight: 42,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#f3d1cf',
        backgroundColor: '#fff',
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'row',
        gap: 8,
    },
    locationBtnOutlineText: {
        color: '#a52019',
        fontWeight: '700',
        fontSize: 13,
    },
    resultItem: {
        marginBottom: 8,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        borderRadius: 10,
        padding: 10,
        backgroundColor: '#fff',
        flexDirection: 'row',
        gap: 8,
        alignItems: 'center',
    },
    resultText: {
        flex: 1,
        color: '#374151',
        fontSize: 13,
    },
    selectedRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        marginBottom: 10,
    },
    selectedText: {
        flex: 1,
        fontSize: 12,
        color: '#6b7280',
    },
    editorHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    editorTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#111827',
    },
    input: {
        minHeight: 44,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#d1d5db',
        paddingHorizontal: 12,
        backgroundColor: '#f9fafb',
        marginBottom: 10,
    },
    textArea: {
        minHeight: 90,
        textAlignVertical: 'top',
        paddingTop: 10,
    },
    sectionLabel: {
        marginBottom: 8,
        marginTop: 2,
        fontSize: 13,
        fontWeight: '700',
        color: '#374151',
    },
    chipsRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
        marginBottom: 10,
    },
    chip: {
        borderRadius: 999,
        borderWidth: 1,
        borderColor: '#d1d5db',
        paddingHorizontal: 10,
        paddingVertical: 6,
        backgroundColor: '#fff',
    },
    chipSelected: {
        backgroundColor: '#a52019',
        borderColor: '#a52019',
    },
    chipText: {
        color: '#4b5563',
        fontSize: 12,
        fontWeight: '600',
    },
    chipTextSelected: {
        color: '#fff',
    },
    twoCols: {
        flexDirection: 'row',
        gap: 8,
    },
    halfInput: {
        flex: 1,
    },
    switchRow: {
        minHeight: 42,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        paddingHorizontal: 12,
        marginBottom: 10,
        backgroundColor: '#fff',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    switchLabel: {
        color: '#374151',
        fontWeight: '600',
        fontSize: 13,
    },
    submitBtn: {
        marginTop: 6,
        minHeight: 46,
        borderRadius: 12,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
    },
    submitBtnDisabled: {
        opacity: 0.7,
    },
    submitBtnText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 14,
    },
});
