import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    ScrollView,
    Modal,
    Pressable,
    TouchableOpacity,
    ActivityIndicator,
    Alert,
    Platform,
} from 'react-native';
import * as Location from 'expo-location';
import { locationService } from '../../../../shared/services/location/locationService';
import { CountdownText } from './CountdownText';
import { calculateDistanceInKm } from '../../../../shared/utils/helpers';
import Toast from 'react-native-toast-message';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import apiClient from '../../../../shared/services/http/apiClient';
import ConfirmationModal from '../../../../shared/components/ConfirmationModal';

// Para web: importar Leaflet y CSS
let MapContainer, TileLayer, Marker, Popup;
let L;

if (Platform.OS === 'web') {
    try {
        require('leaflet/dist/leaflet.css');
        MapContainer = require('react-leaflet').MapContainer;
        TileLayer = require('react-leaflet').TileLayer;
        Marker = require('react-leaflet').Marker;
        Popup = require('react-leaflet').Popup;
        L = require('leaflet');
    } catch (e) {
        console.error('Error loading Leaflet:', e);
    }
}

// Componente que detecta cambios en los bounds del mapa
const MapBoundsTrackerComponent = ({ questions = [], onBoundsChange, onVisibleQuestionsChange, mapRef }) => {
    useEffect(() => {
        if (!mapRef?.current) return;

        const map = mapRef.current;

        const updateBounds = () => {
            try {
                const bounds = map.getBounds();
                const center = map.getCenter();

                onBoundsChange?.({
                    lat: center.lat,
                    lng: center.lng,
                    north: bounds.getNorthEast().lat,
                    south: bounds.getSouthWest().lat,
                    east: bounds.getNorthEast().lng,
                    west: bounds.getSouthWest().lng,
                });

                // Filtrar preguntas dentro de los bounds
                const visibleIds = questions
                    .filter((q) => {
                        const coords = getQuestionCoords(q);
                        if (!coords) return false;
                        return coords.lat >= bounds.getSouthWest().lat &&
                            coords.lat <= bounds.getNorthEast().lat &&
                            coords.lng >= bounds.getSouthWest().lng &&
                            coords.lng <= bounds.getNorthEast().lng;
                    })
                    .map((q) => q.id);

                onVisibleQuestionsChange?.(visibleIds);
            } catch (error) {
                console.warn('Error updating bounds:', error);
            }
        };

        // Agregar event listeners
        map.on('moveend', updateBounds);
        map.on('zoomend', updateBounds);
        map.on('load', updateBounds);

        // Actualizar al cargar por primera vez
        const timeout = setTimeout(updateBounds, 100);

        return () => {
            clearTimeout(timeout);
            map.off('moveend', updateBounds);
            map.off('zoomend', updateBounds);
            map.off('load', updateBounds);
        };
    }, [mapRef, questions, onBoundsChange, onVisibleQuestionsChange]);

    return null;
};

// Función para crear iconos SVG personalizados
const createCustomIcon = (color) => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="44" viewBox="0 0 34 44">
        <defs>
            <linearGradient id="questionGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#f87171"/>
                <stop offset="55%" stop-color="${color}"/>
                <stop offset="100%" stop-color="#991b1b"/>
            </linearGradient>
            <filter id="questionShadow" x="-20%" y="-10%" width="140%" height="140%">
                <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#00000033"/>
            </filter>
        </defs>
        <path filter="url(#questionShadow)" fill="url(#questionGrad)" d="M17 0C9.82 0 4 5.82 4 13c0 8.7 13 31 13 31s13-22.3 13-31C30 5.82 24.18 0 17 0z"/>
        <circle cx="17" cy="14" r="7.2" fill="#ffffff"/>
        <text x="17" y="17" text-anchor="middle" font-size="11" font-weight="800" fill="#b91c1c" font-family="Arial, Helvetica, sans-serif">?</text>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [34, 44],
        iconAnchor: [17, 44],
        popupAnchor: [0, -44],
    });
};

const createFeaturedIcon = () => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="44" height="56" viewBox="0 0 44 56">
        <defs>
            <radialGradient id="pinGrad" cx="40%" cy="30%" r="70%">
                <stop offset="0%" stop-color="#FFE566"/>
                <stop offset="60%" stop-color="#F59E0B"/>
                <stop offset="100%" stop-color="#B45309"/>
            </radialGradient>
            <filter id="shadow" x="-20%" y="-10%" width="140%" height="130%">
                <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#00000055"/>
            </filter>
        </defs>
        <path filter="url(#shadow)" fill="url(#pinGrad)" stroke="#92400E" stroke-width="1.2"
              d="M22 1C13.163 1 6 8.163 6 17c0 11 16 38 16 38s16-27 16-38C38 8.163 30.837 1 22 1z"/>
        <circle fill="white" fill-opacity="0.25" cx="22" cy="16" r="10"/>
        <polygon fill="white"
                 points="22,8 24.35,14.2 31,14.2 25.8,18.1 27.9,24.3 22,20.5 16.1,24.3 18.2,18.1 13,14.2 19.65,14.2"/>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [44, 56],
        iconAnchor: [22, 56],
        popupAnchor: [0, -56],
    });
};

const createEventIcon = () => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="38" height="48" viewBox="0 0 38 48">
        <defs>
            <linearGradient id="eventGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#2dd4bf"/>
                <stop offset="55%" stop-color="#0f766e"/>
                <stop offset="100%" stop-color="#0f4c4a"/>
            </linearGradient>
            <filter id="eventShadow" x="-20%" y="-10%" width="140%" height="140%">
                <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#00000033"/>
            </filter>
        </defs>
        <path filter="url(#eventShadow)" fill="url(#eventGrad)" d="M19 0C11.16 0 5 6.16 5 14c0 9.2 14 34 14 34s14-24.8 14-34C33 6.16 26.84 0 19 0z"/>
        <rect x="11" y="9" width="16" height="13" rx="3" fill="#ffffff"/>
        <rect x="11" y="9" width="16" height="4" rx="2" fill="#99f6e4"/>
        <circle cx="15" cy="16" r="1.2" fill="#0f766e"/>
        <circle cx="19" cy="16" r="1.2" fill="#0f766e"/>
        <circle cx="23" cy="16" r="1.2" fill="#0f766e"/>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [38, 48],
        iconAnchor: [19, 48],
        popupAnchor: [0, -48],
    });
};

const createGroupedEventIcon = (count) => {
    if (!L) return undefined;

    const label = count > 99 ? '99+' : String(count);
    return L.divIcon({
        className: '',
        html: `
            <div style="position: relative; width: 52px; height: 58px; overflow: visible;">
                <div style="position: absolute; left: 9px; top: 6px; width: 34px; height: 48px; filter: drop-shadow(0 2px 5px rgba(0,0,0,0.20));">
                    <svg xmlns="http://www.w3.org/2000/svg" width="34" height="48" viewBox="0 0 38 48">
                        <defs>
                            <linearGradient id="eventGroupGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stop-color="#2dd4bf"/>
                                <stop offset="55%" stop-color="#0f766e"/>
                                <stop offset="100%" stop-color="#0f4c4a"/>
                            </linearGradient>
                        </defs>
                        <path fill="url(#eventGroupGrad)" d="M19 0C11.16 0 5 6.16 5 14c0 9.2 14 34 14 34s14-24.8 14-34C33 6.16 26.84 0 19 0z"/>
                        <rect x="11" y="9" width="16" height="13" rx="3" fill="#ffffff"/>
                        <rect x="11" y="9" width="16" height="4" rx="2" fill="#99f6e4"/>
                        <circle cx="15" cy="16" r="1.2" fill="#0f766e"/>
                        <circle cx="19" cy="16" r="1.2" fill="#0f766e"/>
                        <circle cx="23" cy="16" r="1.2" fill="#0f766e"/>
                    </svg>
                </div>
                <div style="
                    position: absolute;
                    right: -14px;
                    top: -12px;
                    min-width: 24px;
                    height: 24px;
                    padding: 0 6px;
                    border-radius: 999px;
                    background: #A52019;
                    color: #ffffff;
                    border: 2px solid #ffffff;
                    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.22);
                    font-size: ${label.length > 2 ? 10 : 11}px;
                    font-weight: 800;
                    line-height: 20px;
                    text-align: center;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                ">${label}</div>
            </div>
        `,
        iconSize: [52, 58],
        iconAnchor: [26, 58],
        popupAnchor: [0, -58],
    });
};

const toNum = (v) => {
    if (typeof v === 'number') return v;
    if (typeof v === 'string') {
        const n = parseFloat(v);
        return Number.isFinite(n) ? n : undefined;
    }
    return undefined;
};

const getQuestionCoords = (q) => {
    const loc = q?.location ?? {};
    const lat =
        toNum(loc.latitude) ?? toNum(loc.lat) ?? toNum(loc.y) ?? toNum(q?.latitude) ?? toNum(q?.lat);

    const lng =
        toNum(loc.longitude) ??
        toNum(loc.lng) ??
        toNum(loc.lon) ??
        toNum(loc.x) ??
        toNum(q?.longitude) ??
        toNum(q?.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    return { lat, lng };
};

const getEventCoords = (event) => {
    const loc = event?.location ?? {};
    const lat =
        toNum(loc.latitude) ?? toNum(loc.lat) ?? toNum(loc.y) ?? toNum(event?.latitude) ?? toNum(event?.lat);
    const lng =
        toNum(loc.longitude) ??
        toNum(loc.lng) ??
        toNum(loc.lon) ??
        toNum(loc.x) ??
        toNum(event?.longitude) ??
        toNum(event?.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    return { lat, lng };
};

const formatDateTime = (value) => {
    if (!value) return null;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return date.toLocaleString();
};

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

const EVENT_GROUPING_DISTANCE_KM = 0.025;

const areCoordsClose = (a, b, thresholdKm = EVENT_GROUPING_DISTANCE_KM) => {
    if (!a || !b) return false;

    return calculateDistanceInKm(
        { latitude: a.lat, longitude: a.lng },
        { latitude: b.lat, longitude: b.lng }
    ) <= thresholdKm;
};

const getEventSortTime = (event) => {
    if (!event?.startsAt) {
        return Number.POSITIVE_INFINITY;
    }

    const startsAtTs = new Date(event.startsAt).getTime();
    return Number.isFinite(startsAtTs) ? startsAtTs : Number.POSITIVE_INFINITY;
};

const groupEventsByLocation = (events = []) => {
    const clusters = [];

    events.forEach((event) => {
        const coords = getEventCoords(event);
        if (!coords) return;

        const enrichedEvent = { ...event, coords };
        const matchIndex = clusters.findIndex((cluster) =>
            cluster.events.some((clusterEvent) => areCoordsClose(clusterEvent.coords, coords))
        );

        if (matchIndex >= 0) {
            clusters[matchIndex].events.push(enrichedEvent);
            return;
        }

        clusters.push({ events: [enrichedEvent] });
    });

    return clusters
        .map((cluster, index) => {
            const sortedEvents = [...cluster.events].sort((a, b) => {
                const timeDiff = getEventSortTime(a) - getEventSortTime(b);
                if (timeDiff !== 0) return timeDiff;

                return (a.title || '').localeCompare(b.title || '');
            });

            const sumCoords = sortedEvents.reduce(
                (acc, event) => {
                    acc.lat += event.coords.lat;
                    acc.lng += event.coords.lng;
                    return acc;
                },
                { lat: 0, lng: 0 }
            );

            return {
                id: `event-cluster-${index}-${sortedEvents.map((event) => event.id).join('-')}`,
                events: sortedEvents,
                coords: {
                    lat: sumCoords.lat / sortedEvents.length,
                    lng: sumCoords.lng / sortedEvents.length,
                },
            };
        })
        .sort((a, b) => {
            const aFirst = a.events[0];
            const bFirst = b.events[0];
            const timeDiff = getEventSortTime(aFirst) - getEventSortTime(bFirst);
            if (timeDiff !== 0) return timeDiff;

            return (aFirst?.title || '').localeCompare(bFirst?.title || '');
        });
};

export default function MapComponent({
    questions = [],
    events = [],
    canAttendEvents = false,
    onEventAttendanceUpdate,
    eventNavigationTarget = null,
    onQuestionPress,
    onLocationChange,
    onPermissionChange,
    onMapBoundsChange,
    onVisibleQuestionsChange,
    showQuestions = true,
    onOpenEventDetails,
}) {
    const [location, setLocation] = useState(null);
    const [publicLocations, setPublicLocations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [publishing, setPublishing] = useState(false);
    const [togglingEventId, setTogglingEventId] = useState(null);
    const [leaveConfirmVisible, setLeaveConfirmVisible] = useState(false);
    const [pendingLeaveEvent, setPendingLeaveEvent] = useState(null);
    const [eventsNowTs, setEventsNowTs] = useState(() => Date.now());
    const mapRef = useRef(null);
    const [visibleQuestions, setVisibleQuestions] = useState([]);
    const [selectedGroupedEvents, setSelectedGroupedEvents] = useState(null);

    const visibleEvents = useMemo(() => {
        const source = Array.isArray(events) ? events : [];
        return source.filter((event) => isEventStillVisible(event, eventsNowTs));
    }, [events, eventsNowTs]);

    const visibleEventClusters = useMemo(() => groupEventsByLocation(visibleEvents), [visibleEvents]);

    const closeGroupedEventsModal = useCallback(() => {
        setSelectedGroupedEvents(null);
    }, []);

    const openEventDetails = useCallback(
        (eventItem) => {
            if (!eventItem?.id) {
                return;
            }

            closeGroupedEventsModal();
            onOpenEventDetails?.(eventItem);
        },
        [closeGroupedEventsModal, onOpenEventDetails]
    );

    const renderGroupedEventItem = useCallback(
        ({ item }) => {
            const startsAt = formatDateTime(item.startsAt);
            const endsAt = formatDateTime(item.endsAt);
            const attendeeCount = Number.isFinite(Number(item?.attendeeCount))
                ? Number(item.attendeeCount)
                : 0;

            return (
                <TouchableOpacity
                    style={styles.groupEventItem}
                    onPress={() => openEventDetails(item)}
                    activeOpacity={0.85}
                >
                    <View style={styles.groupEventHeader}>
                        <View style={styles.groupEventTitleWrap}>
                            <Text style={styles.groupEventTitle} numberOfLines={2}>
                                {item.title || 'Untitled event'}
                            </Text>
                            <Text style={styles.groupEventSubtitle} numberOfLines={1}>
                                {item.category || 'OTHER'}
                            </Text>
                        </View>
                        <View style={styles.groupEventBadge}>
                            <Text style={styles.groupEventBadgeText}>{attendeeCount}</Text>
                            <Text style={styles.groupEventBadgeLabel}>going</Text>
                        </View>
                    </View>

                    {item.description ? (
                        <Text style={styles.groupEventDescription} numberOfLines={2}>
                            {item.description}
                        </Text>
                    ) : null}

                    <View style={styles.groupEventMeta}>
                        {startsAt ? <Text style={styles.groupEventMetaText}>Starts: {startsAt}</Text> : null}
                        {endsAt ? <Text style={styles.groupEventMetaText}>Ends: {endsAt}</Text> : null}
                        {item.address ? (
                            <Text style={styles.groupEventMetaText} numberOfLines={1}>
                                {item.address}
                            </Text>
                        ) : null}
                    </View>

                    <View style={styles.groupEventActionRow}>
                        <Text style={styles.groupEventActionText}>Open event details</Text>
                        <MaterialIcons name="chevron-right" size={18} color="#1d4ed8" />
                    </View>
                </TouchableOpacity>
            );
        },
        [openEventDetails]
    );

    useEffect(() => {
        const intervalId = setInterval(() => {
            setEventsNowTs(Date.now());
        }, 30000);

        return () => clearInterval(intervalId);
    }, []);

    useEffect(() => {
        if (Platform.OS !== 'web') {
            return;
        }

        const map = mapRef.current;
        if (!map || !eventNavigationTarget || !Number.isFinite(eventNavigationTarget.lat) || !Number.isFinite(eventNavigationTarget.lng)) {
            return;
        }

        if (typeof map.setView === 'function') {
            map.setView([eventNavigationTarget.lat, eventNavigationTarget.lng], 16, { animate: true });
        }
    }, [eventNavigationTarget]);

    const userLocationIcon = useMemo(() => {
        if (!L) return undefined;
        return L.divIcon({
            className: '',
            html: `<div style="
                width: 16px;
                height: 16px;
                border-radius: 999px;
                background: #a52019;
                border: 3px solid #ffffff;
                box-shadow: 0 0 0 3px rgba(165,32,25,0.35);
            "></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8],
            popupAnchor: [0, -10],
        });
    }, []);

    useEffect(() => {
        let locationSubscription;

        const requestLocationPermission = async () => {
            try {
                const { status } = await Location.requestForegroundPermissionsAsync();

                if (status !== 'granted') {
                    setError('Location permission denied');
                    setLoading(false);
                    if (typeof onPermissionChange === 'function') {
                        onPermissionChange(false);
                    }
                    Toast.show({
                        type: 'error',
                        text1: 'Location disabled',
                        text2: 'Please enable location to ask questions.',
                        position: 'top',
                        visibilityTime: 10000,
                    });
                    return;
                }

                if (typeof onPermissionChange === 'function') {
                    onPermissionChange(true);
                }

                const initialLocation = await Location.getCurrentPositionAsync({
                    accuracy: Location.Accuracy.High,
                });

                setLocation(initialLocation.coords);
                if (typeof onLocationChange === 'function') {
                    onLocationChange(initialLocation.coords);
                }
                setLoading(false);

                await loadPublicLocations();

                // Solo usar watchPositionAsync en móvil, no en web
                if (Platform.OS !== 'web') {
                    locationSubscription = await Location.watchPositionAsync(
                        {
                            accuracy: Location.Accuracy.High,
                            timeInterval: 2000,
                            distanceInterval: 5,
                        },
                        (newLocation) => {
                            setLocation(newLocation.coords);
                            if (typeof onLocationChange === 'function') {
                                onLocationChange(newLocation.coords);
                            }
                        }
                    );
                }
            } catch (err) {
                setError('Error getting location: ' + err.message);
                setLoading(false);
                if (typeof onPermissionChange === 'function') {
                    onPermissionChange(false);
                }
            }
        };

        requestLocationPermission();

        return () => {
            if (locationSubscription && typeof locationSubscription.remove === 'function') {
                locationSubscription.remove();
            }
        };
    }, [onLocationChange, onPermissionChange]);

    const loadPublicLocations = async () => {
        try {
            const response = await locationService.getPublicLocationsSince(30);
            // Asegurar que siempre es un array
            const locations = Array.isArray(response)
                ? response
                : Array.isArray(response?.data)
                    ? response.data
                    : [];
            setPublicLocations(locations);
        } catch (err) {
            console.warn('Error loading public locations:', err);
            setPublicLocations([]); // Asegurar array vacío en caso de error
        }
    };

    useEffect(() => {
        const ahora = new Date().getTime();

        // Si showQuestions es false, retornar array vacío
        if (!showQuestions) {
            setVisibleQuestions([]);
            return;
        }

        // Filtramos las preguntas que ya vencieron antes de guardarlas en el estado
        // También excluimos las preguntas asociadas a eventos (se muestran dentro del evento)
        const preguntasActivas = questions.filter((q) => {
            if (q?.event) {
                return false;
            }

            if (q?.active === false) {
                return false;
            }

            if (!q?.expiresAt) {
                return true;
            }

            const fechaExpiracion = new Date(q.expiresAt).getTime();
            if (!Number.isFinite(fechaExpiracion)) {
                return true;
            }

            return fechaExpiracion > ahora; // Solo dejamos las que expiran en el futuro
        });

        setVisibleQuestions(preguntasActivas.length > 0 ? preguntasActivas : questions.filter((q) => !q?.event && q?.active !== false));
    }, [questions, showQuestions]);

    const handleQuestionExpire = (questionId) => {
        setVisibleQuestions((prev) => prev.filter((q) => q.id !== questionId));
    };

    const handlePublishLocation = async () => {
        if (!location) {
            Alert.alert('Error', 'Location is not available yet');
            return;
        }

        setPublishing(true);
        try {
            await locationService.publishLocation(
                location.latitude,
                location.longitude,
                location.accuracy,
                true
            );
            Alert.alert('Success', 'Location published!');
            await loadPublicLocations();
        } catch (err) {
            console.error('Error publishing location:', err);
            Alert.alert(
                'Error',
                'Error publishing location: ' + (err.response?.data?.message || err.message)
            );
        } finally {
            setPublishing(false);
        }
    };

    const performToggleAttendance = useCallback(async (eventItem) => {
        if (!eventItem?.id) {
            return;
        }

        setTogglingEventId(eventItem.id);
        try {
            const response = await apiClient.post(`/api/v1/events/${eventItem.id}/attendance`);
            const updatedEvent = response?.data;
            if (updatedEvent?.id) {
                onEventAttendanceUpdate?.(updatedEvent);
            }
        } catch (error) {
            Alert.alert(
                'Error',
                error?.response?.data?.message || 'Could not update your attendance.'
            );
        } finally {
            setTogglingEventId(null);
        }
    }, [onEventAttendanceUpdate]);

    const handleToggleAttendance = useCallback((eventItem) => {
        if (!eventItem?.id) {
            return;
        }

        if (eventItem?.myAttendance === true) {
            setPendingLeaveEvent(eventItem);
            setLeaveConfirmVisible(true);
            return;
        }

        performToggleAttendance(eventItem);
    }, [performToggleAttendance]);

    const handleCancelLeave = useCallback(() => {
        setLeaveConfirmVisible(false);
        setPendingLeaveEvent(null);
    }, []);

    const handleConfirmLeave = useCallback(() => {
        const eventToLeave = pendingLeaveEvent;
        setLeaveConfirmVisible(false);
        setPendingLeaveEvent(null);

        if (eventToLeave) {
            performToggleAttendance(eventToLeave);
        }
    }, [pendingLeaveEvent, performToggleAttendance]);

    if (loading) {
        return (
            <View style={styles.container}>
                <ActivityIndicator size="large" color="#007AFF" />
                <Text style={styles.loadingText}>Getting location...</Text>
            </View>
        );
    }

    if (error) {
        return (
            <View style={styles.container}>
                <MaterialIcons name="location-off" size={84} color="darkred" />

                <Text style={styles.errorText}>{error}</Text>
            </View>
        );
    }

    if (!location) {
        return (
            <View style={styles.container}>
                <Text style={styles.errorText}>Location could not be retrieved</Text>
            </View>
        );
    }

    // Versión WEB con Leaflet
    if (Platform.OS === 'web') {
        if (!MapContainer || !TileLayer || !Marker) {
            // Fallback si Leaflet no se cargó
            return (
                <div style={{ padding: '20px', textAlign: 'center' }}>
                    <h2>Error loading the map</h2>
                    <p>Leaflet is not available. Please refresh the page.</p>
                </div>
            );
        }

        return (
            <>
                <div style={webStyles.container}>
                    {/* Mapa */}
                    <MapContainer
                        center={[location.latitude, location.longitude]}
                        zoom={15}
                        ref={mapRef}
                        style={webStyles.map}
                    >
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />

                        {/* Rastreador de bounds del mapa */}
                        <MapBoundsTrackerComponent
                            questions={visibleQuestions}
                            onBoundsChange={onMapBoundsChange}
                            onVisibleQuestionsChange={onVisibleQuestionsChange}
                            mapRef={mapRef}
                        />

                        {/* Marcador de tu ubicación */}
                        <Marker position={[location.latitude, location.longitude]} icon={userLocationIcon}>
                            <Popup>
                                <div style={{ fontSize: '12px' }}>
                                    <strong>Your location</strong>
                                    <br />
                                    {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
                                    <br />
                                    Accuracy: {location.accuracy?.toFixed(2) || 'N/A'} m
                                </div>
                            </Popup>
                        </Marker>

                        {/* Question Markers */}
                        {(Array.isArray(visibleQuestions) ? visibleQuestions : []).map((q) => {
                            const coords = getQuestionCoords(q);
                            if (!coords) return null;
                            const radiusKm = toNum(q?.radiusKm);
                            const { lat, lng } = coords;
                            const distanceKm = calculateDistanceInKm(
                                { latitude: location.latitude, longitude: location.longitude },
                                { latitude: lat, longitude: lng }
                            );
                            const canAnswer = !Number.isFinite(radiusKm) || radiusKm <= 0 || distanceKm <= radiusKm;
                            const questionColor = canAnswer ? '#dc2626' : '#991b1b';

                            return (
                                <Marker
                                    key={q.id}
                                    position={[lat, lng]}
                                    icon={q.featured ? createFeaturedIcon() : createCustomIcon(questionColor)}
                                    eventHandlers={{
                                        click: () => onQuestionPress?.(q.id),
                                    }}
                                >
                                    <Popup>
                                        <div style={{ fontSize: '12px' }}>
                                            <strong>{q.featured ? '⭐ ' : ''}{q.title || 'Question'}</strong>
                                            <br />
                                            <div style={{ marginBottom: '8px' }}>
                                                <CountdownText
                                                    expiresAt={q.expiresAt}
                                                    onExpire={() => handleQuestionExpire(q.id)}
                                                />
                                            </div>
                                            <span style={{ color: canAnswer ? '#ea580c' : '#6b7280', fontWeight: 700 }}>
                                                {canAnswer ? 'You can answer' : 'Out of your range'}
                                            </span>
                                            <br />
                                            {Number.isFinite(radiusKm) && radiusKm > 0 && (
                                                <>
                                                    <span style={{ opacity: 0.85 }}>
                                                        Answer radius: {radiusKm.toFixed(2)} km
                                                    </span>
                                                    <br />
                                                    <span style={{ opacity: 0.85 }}>
                                                        Your distance: {distanceKm.toFixed(2)} km
                                                    </span>
                                                    <br />
                                                </>
                                            )}
                                            <span style={{ opacity: 0.8 }}>
                                                {lat.toFixed(5)}, {lng.toFixed(5)}
                                            </span>
                                            <br />
                                            <span style={{ color: '#007AFF', fontWeight: 600 }}>Click to open</span>
                                        </div>
                                    </Popup>
                                </Marker>
                            );
                        })}

                        {/* Event markers */}
                        {visibleEventClusters.map((cluster) => {
                            const isGrouped = cluster.events.length > 1;
                            const eventItem = cluster.events[0];
                            const markerIcon = isGrouped
                                ? createGroupedEventIcon(cluster.events.length)
                                : createEventIcon();

                            return (
                                <Marker
                                    key={cluster.id}
                                    position={[cluster.coords.lat, cluster.coords.lng]}
                                    icon={markerIcon}
                                    eventHandlers={isGrouped ? {
                                        click: () => setSelectedGroupedEvents(cluster),
                                    } : undefined}
                                >
                                    {!isGrouped && (
                                        <Popup>
                                            <div style={{ fontSize: '12px' }}>
                                                <strong>Event: {eventItem.title || 'Untitled event'}</strong>
                                                <br />
                                                {eventItem.category && (
                                                    <>
                                                        <span style={{ color: '#0f766e', fontWeight: 700 }}>
                                                            {eventItem.category}
                                                        </span>
                                                        <br />
                                                    </>
                                                )}
                                                {formatDateTime(eventItem.startsAt) && (
                                                    <>
                                                        <span>Starts: {formatDateTime(eventItem.startsAt)}</span>
                                                        <br />
                                                    </>
                                                )}
                                                {formatDateTime(eventItem.endsAt) && (
                                                    <>
                                                        <span>Ends: {formatDateTime(eventItem.endsAt)}</span>
                                                        <br />
                                                    </>
                                                )}
                                                {eventItem.address && (
                                                    <>
                                                        <span>{eventItem.address}</span>
                                                        <br />
                                                    </>
                                                )}
                                                <span style={{ fontWeight: 700, color: '#111827' }}>
                                                    Attendees: {Number.isFinite(Number(eventItem?.attendeeCount))
                                                        ? Number(eventItem.attendeeCount)
                                                        : 0}
                                                </span>
                                                <br />
                                                {canAttendEvents && (
                                                    <>
                                                        <span style={{ opacity: 0.85 }}>
                                                            {eventItem?.myAttendance === true ? 'You are going' : 'Not attending yet'}
                                                        </span>
                                                        <br />
                                                        <button
                                                            type="button"
                                                            onClick={() => handleToggleAttendance(eventItem)}
                                                            disabled={togglingEventId === eventItem.id}
                                                            style={{
                                                                marginTop: '8px',
                                                                width: '100%',
                                                                border: 'none',
                                                                borderRadius: '10px',
                                                                padding: '10px 12px',
                                                                backgroundColor: eventItem?.myAttendance === true ? '#b91c1c' : '#0f766e',
                                                                color: '#ffffff',
                                                                fontWeight: 700,
                                                                cursor: togglingEventId === eventItem.id ? 'wait' : 'pointer',
                                                                opacity: togglingEventId === eventItem.id ? 0.75 : 1,
                                                            }}
                                                        >
                                                            {togglingEventId === eventItem.id
                                                                ? 'Updating...'
                                                                : (eventItem?.myAttendance === true ? 'Leave event' : 'Join event')}
                                                        </button>
                                                    </>
                                                )}
                                                <button
                                                    type="button"
                                                    onClick={() => onOpenEventDetails?.(eventItem)}
                                                    style={{
                                                        marginTop: '8px',
                                                        width: '100%',
                                                        border: '1px solid rgba(29, 78, 216, 0.18)',
                                                        borderRadius: '999px',
                                                        padding: '6px 10px',
                                                        backgroundColor: 'rgba(29, 78, 216, 0.06)',
                                                        color: '#1d4ed8',
                                                        fontWeight: 600,
                                                        fontSize: '12px',
                                                        cursor: 'pointer',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        gap: '6px',
                                                        lineHeight: 1,
                                                        opacity: 0.95,
                                                    }}
                                                >
                                                    <span aria-hidden="true" style={{ fontSize: '13px', lineHeight: 1 }}>👁</span>
                                                    <span>View event info</span>
                                                </button>
                                                <span style={{ opacity: 0.8 }}>
                                                    {cluster.coords.lat.toFixed(5)}, {cluster.coords.lng.toFixed(5)}
                                                </span>
                                            </div>
                                        </Popup>
                                    )}
                                </Marker>
                            );
                        })}

                        {/* Marcadores de ubicaciones públicas */}
                        {publicLocations &&
                            publicLocations.map((pubLocation) => (
                                <Marker
                                    key={pubLocation.id}
                                    position={[pubLocation.latitude, pubLocation.longitude]}
                                    icon={createCustomIcon('#FF3B30')}
                                >
                                    <Popup>
                                        <div style={{ fontSize: '12px' }}>
                                            <strong>User {pubLocation.user?.id || 'Unknown'}</strong>
                                            <br />
                                            {pubLocation.latitude.toFixed(6)}, {pubLocation.longitude.toFixed(6)}
                                            <br />
                                            {getTimeAgo(pubLocation.timestamp)}
                                        </div>
                                    </Popup>
                                </Marker>
                            ))}
                    </MapContainer>
                </div>

                <Modal
                    visible={Boolean(selectedGroupedEvents)}
                    transparent
                    animationType="fade"
                    onRequestClose={closeGroupedEventsModal}
                >
                    <Pressable style={styles.groupModalOverlay} onPress={closeGroupedEventsModal}>
                        <Pressable style={styles.groupModalCard} onPress={() => { }}>
                            <View style={styles.groupModalHeader}>
                                <View style={styles.groupModalHeaderText}>
                                    <Text style={styles.groupModalTitle}>Events at this location</Text>
                                    <Text style={styles.groupModalSubtitle}>
                                        {selectedGroupedEvents?.events?.length || 0} events found nearby
                                    </Text>
                                </View>

                                <TouchableOpacity onPress={closeGroupedEventsModal} style={styles.groupModalCloseBtn}>
                                    <MaterialIcons name="close" size={20} color="#334155" />
                                </TouchableOpacity>
                            </View>

                            <Text style={styles.groupModalHint}>
                                Tap any event to open its details page.
                            </Text>

                            <FlatList
                                data={selectedGroupedEvents?.events || []}
                                keyExtractor={(item) => String(item.id)}
                                renderItem={renderGroupedEventItem}
                                ItemSeparatorComponent={() => <View style={styles.groupEventSeparator} />}
                                contentContainerStyle={styles.groupEventList}
                                showsVerticalScrollIndicator={true}
                            />
                        </Pressable>
                    </Pressable>
                </Modal>

                <ConfirmationModal
                    visible={leaveConfirmVisible}
                    title="Leave event"
                    message="Are you sure you want to stop attending this event?"
                    confirmText="Yes, leave"
                    cancelText="Cancel"
                    onConfirm={handleConfirmLeave}
                    onCancel={handleCancelLeave}
                    confirmButtonColor="danger"
                />
            </>
        );
    }

    // Fallback: Versión simple para móvil o si Leaflet no está disponible
    return (
        <>
            <ScrollView style={styles.webContainer}>
                <View style={styles.webContent}>
                    <Text style={styles.webTitle}>📍 Your location</Text>
                    <View style={styles.locationCard}>
                        <Text style={styles.locationText}>Latitude: {location.latitude.toFixed(6)}</Text>
                        <Text style={styles.locationText}>Longitude: {location.longitude.toFixed(6)}</Text>
                        <Text style={styles.locationText}>
                            Accuracy: {location.accuracy?.toFixed(2) || 'N/A'} m
                        </Text>
                    </View>

                    <TouchableOpacity
                        style={[styles.publishButton, publishing && styles.publishButtonDisabled]}
                        onPress={handlePublishLocation}
                        disabled={publishing}
                    >
                        <Text style={styles.publishButtonText}>
                            {publishing ? 'Publishing...' : 'Publish location'}
                        </Text>
                    </TouchableOpacity>

                    <Text style={[styles.webTitle, { marginTop: 20 }]}>
                        🔴 Public locations ({publicLocations.length})
                    </Text>
                    {publicLocations.length === 0 ? (
                        <Text style={styles.noLocationsText}>No public locations visible</Text>
                    ) : (
                        publicLocations.map((pubLocation) => (
                            <View key={pubLocation.id} style={styles.locationCard}>
                                <Text style={styles.locationText}>
                                    User ID: {pubLocation.user?.id || 'Unknown'}
                                </Text>
                                <Text style={styles.locationText}>
                                    Lat: {pubLocation.latitude.toFixed(6)}, Lon: {pubLocation.longitude.toFixed(6)}
                                </Text>
                                <Text style={styles.timeText}>{getTimeAgo(pubLocation.timestamp)}</Text>
                            </View>
                        ))
                    )}
                </View>
            </ScrollView>

            <ConfirmationModal
                visible={leaveConfirmVisible}
                title="Leave event"
                message="Are you sure you want to stop attending this event?"
                confirmText="Yes, leave"
                cancelText="Cancel"
                onConfirm={handleConfirmLeave}
                onCancel={handleCancelLeave}
                confirmButtonColor="danger"
            />
        </>
    );
}

function getTimeAgo(timestamp) {
    const now = new Date();
    const then = new Date(timestamp);
    const seconds = Math.floor((now - then) / 1000);

    if (seconds < 60) return 'hace poco';
    if (seconds < 3600) return `hace ${Math.floor(seconds / 60)}m`;
    if (seconds < 86400) return `hace ${Math.floor(seconds / 3600)}h`;
    return `hace ${Math.floor(seconds / 86400)}d`;
}

// Estilos para web
const webStyles = {
    container: {
        position: 'relative',
        width: '100%',
        height: '100vh',
        margin: 0,
        padding: 0,
    },
    map: {
        width: '100%',
        height: '100%',
    },
    publishButton: {
        position: 'fixed',
        bottom: '60px',
        right: '16px',
        backgroundColor: '#007AFF',
        color: 'white',
        border: 'none',
        padding: '12px 16px',
        borderRadius: '8px',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        zIndex: 1000,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
    },
    publishButtonDisabled: {
        backgroundColor: '#9E9E9E',
        opacity: 0.7,
        cursor: 'not-allowed',
    },
    infoBox: {
        position: 'fixed',
        bottom: '16px',
        left: '16px',
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        color: 'white',
        padding: '8px 12px',
        borderRadius: '8px',
        zIndex: 1000,
    },
    infoText: {
        fontSize: '12px',
        color: 'white',
    },
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5F5F5',
    },
    loadingText: {
        marginTop: 16,
        fontSize: 16,
        color: '#757575',
    },
    errorText: {
        fontSize: 16,
        color: '#D32F2F',
        textAlign: 'center',
        padding: 16,
    },
    // Web Map Styles
    webMapContainer: {
        flex: 1,
        position: 'relative',
    },
    mapDivWeb: {
        flex: 1,
        width: '100%',
        height: 400,
    },
    publishButton: {
        position: 'absolute',
        bottom: 60,
        right: 16,
        backgroundColor: '#007AFF',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 8,
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 1000,
    },
    publishButtonDisabled: {
        backgroundColor: '#9E9E9E',
        opacity: 0.7,
    },
    publishButtonText: {
        color: '#FFF',
        fontSize: 16,
        fontWeight: '600',
    },
    infoBox: {
        position: 'absolute',
        bottom: 16,
        left: 16,
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        zIndex: 1000,
    },
    infoText: {
        color: '#FFF',
        fontSize: 12,
    },
    groupModalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(15, 23, 42, 0.55)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 16,
    },
    groupModalCard: {
        width: '92%',
        maxWidth: 560,
        maxHeight: '80%',
        backgroundColor: '#ffffff',
        borderRadius: 20,
        padding: 18,
        shadowColor: '#000000',
        shadowOpacity: 0.18,
        shadowRadius: 24,
        shadowOffset: { width: 0, height: 12 },
        elevation: 8,
    },
    groupModalHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 12,
    },
    groupModalHeaderText: {
        flex: 1,
    },
    groupModalTitle: {
        fontSize: 18,
        fontWeight: '800',
        color: '#0f172a',
    },
    groupModalSubtitle: {
        marginTop: 4,
        fontSize: 13,
        color: '#64748b',
    },
    groupModalCloseBtn: {
        width: 34,
        height: 34,
        borderRadius: 17,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f1f5f9',
    },
    groupModalHint: {
        marginTop: 10,
        marginBottom: 14,
        fontSize: 12,
        color: '#64748b',
    },
    groupEventList: {
        paddingBottom: 4,
    },
    groupEventSeparator: {
        height: 10,
    },
    groupEventItem: {
        borderRadius: 16,
        borderWidth: 1,
        borderColor: '#e2e8f0',
        backgroundColor: '#f8fafc',
        padding: 14,
    },
    groupEventHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 10,
    },
    groupEventTitleWrap: {
        flex: 1,
    },
    groupEventTitle: {
        fontSize: 15,
        fontWeight: '800',
        color: '#0f172a',
    },
    groupEventSubtitle: {
        marginTop: 2,
        fontSize: 12,
        fontWeight: '700',
        color: '#0f766e',
    },
    groupEventBadge: {
        minWidth: 52,
        borderRadius: 999,
        backgroundColor: '#0f766e',
        paddingVertical: 8,
        paddingHorizontal: 10,
        alignItems: 'center',
    },
    groupEventBadgeText: {
        color: '#ffffff',
        fontSize: 15,
        fontWeight: '900',
        lineHeight: 18,
    },
    groupEventBadgeLabel: {
        color: '#d1fae5',
        fontSize: 10,
        fontWeight: '700',
        lineHeight: 12,
    },
    groupEventDescription: {
        marginTop: 10,
        color: '#334155',
        fontSize: 13,
        lineHeight: 18,
    },
    groupEventMeta: {
        marginTop: 10,
        gap: 4,
    },
    groupEventMetaText: {
        color: '#475569',
        fontSize: 12,
    },
    groupEventActionRow: {
        marginTop: 12,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    groupEventActionText: {
        color: '#1d4ed8',
        fontSize: 13,
        fontWeight: '700',
    },
    // Fallback styles
    webContainer: {
        flex: 1,
        backgroundColor: '#F5F5F5',
        padding: 16,
    },
    webContent: {
        paddingBottom: 20,
    },
    webTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#000',
        marginBottom: 12,
        marginTop: 16,
    },
    locationCard: {
        backgroundColor: '#FFF',
        borderRadius: 12,
        padding: 12,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    locationText: {
        fontSize: 14,
        color: '#000',
        marginBottom: 4,
    },
    timeText: {
        fontSize: 12,
        color: '#757575',
        marginTop: 4,
    },
    noLocationsText: {
        fontSize: 14,
        color: '#757575',
        textAlign: 'center',
        padding: 16,
        fontStyle: 'italic',
    },
});
