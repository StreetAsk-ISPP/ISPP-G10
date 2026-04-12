import React, { useCallback, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
    useWindowDimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';

import apiClient from '../../../shared/services/http/apiClient';
import { useAuth } from '../../../app/providers/AuthProvider';
import ConfirmationModal from '../../../shared/components/ConfirmationModal';

const formatDateTime = (value) => {
    if (!value) return 'Sin fecha';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Sin fecha';
    return date.toLocaleString();
};

const getEventEndTime = (event) => {
    const rawValue = event?.endsAt || event?.startsAt;
    if (!rawValue) return null;

    const ms = new Date(rawValue).getTime();
    return Number.isFinite(ms) ? ms : null;
};

export default function MyAttendancesScreen({ navigation }) {
    const { user } = useAuth();
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;

    const isBusiness = useMemo(
        () => Array.isArray(user?.roles) && user.roles.includes('BUSINESS'),
        [user?.roles]
    );

    const [loading, setLoading] = useState(true);
    const [events, setEvents] = useState([]);
    const [updatingEventId, setUpdatingEventId] = useState(null);
    const [leaveConfirmVisible, setLeaveConfirmVisible] = useState(false);
    const [pendingLeaveEvent, setPendingLeaveEvent] = useState(null);

    const upcomingEvents = useMemo(() => {
        const now = Date.now();

        return events
            .filter((event) => event?.myAttendance === true)
            .filter((event) => {
                const eventEndTime = getEventEndTime(event);
                return eventEndTime === null || eventEndTime >= now;
            })
            .sort((a, b) => {
                const timeA = getEventEndTime(a);
                const timeB = getEventEndTime(b);

                if (timeA === null && timeB === null) return 0;
                if (timeA === null) return 1;
                if (timeB === null) return -1;
                return timeA - timeB;
            });
    }, [events]);

    const pastEvents = useMemo(() => {
        const now = Date.now();

        return events
            .filter((event) => event?.myAttendance === true)
            .filter((event) => {
                const eventEndTime = getEventEndTime(event);
                return eventEndTime !== null && eventEndTime < now;
            })
            .sort((a, b) => {
                const timeA = getEventEndTime(a) ?? 0;
                const timeB = getEventEndTime(b) ?? 0;
                return timeB - timeA;
            });
    }, [events]);

    const loadAttendingEvents = useCallback(async () => {
        try {
            setLoading(true);
            const response = await apiClient.get('/api/v1/events');
            const rawEvents = Array.isArray(response?.data) ? response.data : [];
            setEvents(rawEvents.filter((event) => event?.myAttendance === true));
        } catch (error) {
            console.error('Error loading attending events:', error);
            Alert.alert('Error', 'No se pudieron cargar tus eventos.');
        } finally {
            setLoading(false);
        }
    }, []);

    useFocusEffect(
        useCallback(() => {
            loadAttendingEvents();
        }, [loadAttendingEvents])
    );

    const performToggleAttendance = useCallback(async (eventItem) => {
        if (!eventItem?.id) {
            return;
        }

        setUpdatingEventId(eventItem.id);
        try {
            const response = await apiClient.post(`/api/v1/events/${eventItem.id}/attendance`);
            const updatedEvent = response?.data;

            setEvents((currentEvents) => {
                return currentEvents
                    .map((event) => (event.id === eventItem.id ? updatedEvent : event))
                    .filter((event) => event?.myAttendance === true);
            });
        } catch (error) {
            Alert.alert(
                'Error',
                error?.response?.data?.message || 'No se pudo actualizar tu asistencia.'
            );
        } finally {
            setUpdatingEventId(null);
        }
    }, []);

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

    if (isBusiness) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={styles.header}>
                    <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                        <Ionicons name="chevron-back" size={20} color="#374151" />
                    </TouchableOpacity>
                    <Text style={styles.title}>My events</Text>
                    <View style={styles.backBtn} />
                </View>
                <View style={styles.emptyState}>
                    <Ionicons name="calendar-outline" size={52} color="#9ca3af" />
                    <Text style={styles.emptyTitle}>Business accounts do not attend events</Text>
                    <Text style={styles.emptyText}>This screen is available for regular users.</Text>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.screen}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                    <Ionicons name="chevron-back" size={20} color="#374151" />
                </TouchableOpacity>
                <Text style={styles.title}>My events</Text>
                <TouchableOpacity style={styles.backBtn} onPress={loadAttendingEvents}>
                    <Ionicons name="refresh-outline" size={20} color="#374151" />
                </TouchableOpacity>
            </View>

            {loading ? (
                <View style={styles.loaderWrap}>
                    <ActivityIndicator size="large" color="#a52019" />
                    <Text style={styles.loaderText}>Loading your events...</Text>
                </View>
            ) : (
                <ScrollView contentContainerStyle={[styles.content, isNarrow && { paddingHorizontal: 14 }]}>
                    <View style={styles.heroCard}>
                        <View style={styles.heroBadge}>
                            <Ionicons name="calendar-outline" size={20} color="#1e40af" />
                        </View>
                        <View style={{ flex: 1 }}>
                            <Text style={styles.heroTitle}>Events you are going to</Text>
                            <Text style={styles.heroSubtitle}>
                                Check details for upcoming and past events you marked as going.
                            </Text>
                        </View>
                    </View>

                    {upcomingEvents.length === 0 && pastEvents.length === 0 ? (
                        <View style={styles.emptyStateList}>
                            <Ionicons name="calendar-clear-outline" size={50} color="#9ca3af" />
                            <Text style={styles.emptyTitle}>No events yet</Text>
                            <Text style={styles.emptyText}>
                                When you join an event, it will appear here.
                            </Text>
                        </View>
                    ) : (
                        <>
                            <View style={styles.sectionHeader}>
                                <Text style={styles.sectionTitle}>Upcoming events</Text>
                                <Text style={styles.sectionCount}>{upcomingEvents.length}</Text>
                            </View>

                            {upcomingEvents.length === 0 ? (
                                <Text style={styles.sectionEmptyText}>You are not going to any upcoming events.</Text>
                            ) : (
                                upcomingEvents.map((event) => (
                                    <View key={event.id} style={styles.card}>
                                        <View style={styles.cardHeader}>
                                            <Text style={styles.cardTitle}>{event.title || 'Untitled event'}</Text>
                                            <Text style={styles.cardCategory}>{event.category || 'OTHER'}</Text>
                                        </View>

                                        <Text style={styles.cardDescription} numberOfLines={3}>
                                            {event.description || 'No description available.'}
                                        </Text>

                                        <View style={styles.metaRow}>
                                            <Ionicons name="pin-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>{event.address || 'No address'}</Text>
                                        </View>
                                        <View style={styles.metaRow}>
                                            <Ionicons name="time-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>
                                                {formatDateTime(event.startsAt)} - {formatDateTime(event.endsAt)}
                                            </Text>
                                        </View>
                                        <View style={styles.metaRow}>
                                            <Ionicons name="people-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>{event.attendeeCount || 0} attendees</Text>
                                        </View>

                                        <TouchableOpacity
                                            style={[
                                                styles.attendanceBtn,
                                                updatingEventId === event.id && styles.attendanceBtnDisabled,
                                            ]}
                                            onPress={() => handleToggleAttendance(event)}
                                            disabled={updatingEventId === event.id}
                                            activeOpacity={0.85}
                                        >
                                            <Ionicons
                                                name={updatingEventId === event.id ? 'hourglass-outline' : 'log-out-outline'}
                                                size={18}
                                                color="#fff"
                                            />
                                            <Text style={styles.attendanceBtnText}>
                                                {updatingEventId === event.id ? 'Updating...' : 'Leave event'}
                                            </Text>
                                        </TouchableOpacity>
                                    </View>
                                ))
                            )}

                            <View style={styles.sectionHeader}>
                                <Text style={styles.sectionTitle}>Past events</Text>
                                <Text style={styles.sectionCount}>{pastEvents.length}</Text>
                            </View>

                            {pastEvents.length === 0 ? (
                                <Text style={styles.sectionEmptyText}>No past attended events yet.</Text>
                            ) : (
                                pastEvents.map((event) => (
                                    <View key={event.id} style={[styles.card, styles.pastCard]}>
                                        <View style={styles.cardHeader}>
                                            <Text style={styles.cardTitle}>{event.title || 'Untitled event'}</Text>
                                            <Text style={styles.cardCategory}>{event.category || 'OTHER'}</Text>
                                        </View>

                                        <Text style={styles.cardDescription} numberOfLines={3}>
                                            {event.description || 'No description available.'}
                                        </Text>

                                        <View style={styles.metaRow}>
                                            <Ionicons name="pin-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>{event.address || 'No address'}</Text>
                                        </View>
                                        <View style={styles.metaRow}>
                                            <Ionicons name="time-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>
                                                {formatDateTime(event.startsAt)} - {formatDateTime(event.endsAt)}
                                            </Text>
                                        </View>
                                        <View style={styles.metaRow}>
                                            <Ionicons name="people-outline" size={14} color="#6b7280" />
                                            <Text style={styles.metaText}>{event.attendeeCount || 0} attendees</Text>
                                        </View>
                                    </View>
                                ))
                            )}
                        </>
                    )}
                </ScrollView>
            )}

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
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f3f4f6',
    },
    header: {
        height: 58,
        paddingHorizontal: 14,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
    },
    backBtn: {
        width: 36,
        height: 36,
        borderRadius: 10,
        backgroundColor: '#f3f4f6',
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 18,
        fontWeight: '800',
        color: '#1f2937',
    },
    content: {
        padding: 16,
        paddingBottom: 28,
    },
    heroCard: {
        backgroundColor: '#fff',
        borderRadius: 18,
        padding: 16,
        flexDirection: 'row',
        gap: 12,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#dbeafe',
        marginBottom: 16,
    },
    heroBadge: {
        width: 44,
        height: 44,
        borderRadius: 14,
        backgroundColor: '#fef3c7',
        alignItems: 'center',
        justifyContent: 'center',
    },
    heroTitle: {
        fontSize: 18,
        fontWeight: '800',
        color: '#111827',
    },
    heroSubtitle: {
        marginTop: 4,
        color: '#6b7280',
        fontSize: 13,
        lineHeight: 18,
    },
    sectionHeader: {
        marginTop: 2,
        marginBottom: 10,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
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
        marginBottom: 12,
    },
    card: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 16,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        marginBottom: 12,
    },
    pastCard: {
        backgroundColor: '#f9fafb',
    },
    cardHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 10,
        marginBottom: 8,
    },
    cardTitle: {
        flex: 1,
        fontSize: 17,
        fontWeight: '800',
        color: '#111827',
    },
    cardCategory: {
        color: '#0f766e',
        fontSize: 12,
        fontWeight: '800',
    },
    cardDescription: {
        color: '#4b5563',
        fontSize: 14,
        lineHeight: 20,
    },
    metaRow: {
        flexDirection: 'row',
        gap: 8,
        alignItems: 'center',
        marginTop: 10,
    },
    metaText: {
        flex: 1,
        color: '#6b7280',
        fontSize: 12,
    },
    attendanceBtn: {
        marginTop: 14,
        borderRadius: 12,
        minHeight: 44,
        paddingHorizontal: 14,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        backgroundColor: '#b91c1c',
    },
    attendanceBtnDisabled: {
        opacity: 0.75,
    },
    attendanceBtnText: {
        color: '#fff',
        fontWeight: '800',
        fontSize: 14,
    },
    loaderWrap: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    loaderText: {
        marginTop: 10,
        color: '#6b7280',
        fontSize: 14,
    },
    emptyState: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 24,
    },
    emptyStateList: {
        backgroundColor: '#fff',
        borderRadius: 18,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        padding: 28,
        alignItems: 'center',
        justifyContent: 'center',
    },
    emptyTitle: {
        marginTop: 12,
        fontSize: 18,
        fontWeight: '800',
        color: '#374151',
        textAlign: 'center',
    },
    emptyText: {
        marginTop: 8,
        color: '#6b7280',
        fontSize: 14,
        textAlign: 'center',
        lineHeight: 20,
    },
});
