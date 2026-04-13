import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, FlatList, Pressable, useWindowDimensions } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import EventService from '../../shared/services/EventService';
import { theme } from '../../shared/ui/theme/theme';

export default function EventDetailsScreen({ route, navigation }) {
    const { eventId } = route.params;
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;
    const [event, setEvent] = useState(null);
    const [attendees, setAttendees] = useState([]);
    const [eventQuestions, setEventQuestions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadEventDetails = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            console.log('[EventDetailsScreen] Loading event details for eventId:', eventId);

            const [eventData, questionsData] = await Promise.all([
                EventService.getEventDetails(eventId),
                EventService.getEventQuestions(eventId),
            ]);

            let attendeesData = [];
            try {
                attendeesData = await EventService.getEventAttendees(eventId);
            } catch (_) {
                attendeesData = [];
            }

            setEvent(eventData);
            setAttendees(attendeesData || []);
            const eventQuestionsList = Array.isArray(questionsData) ? questionsData : [];
            console.log('[EventDetailsScreen] Loaded event questions:', eventQuestionsList.length, eventQuestionsList);
            setEventQuestions(eventQuestionsList);
        } catch (err) {
            console.error('Error loading event details:', err);
            setError(err.message || 'Failed to load event details');
        } finally {
            setLoading(false);
        }
    }, [eventId]);

    useFocusEffect(useCallback(() => {
        loadEventDetails();
    }, [loadEventDetails]));

    useEffect(() => {
        if (route?.params?.refreshNonce) {
            loadEventDetails();
        }
    }, [route?.params?.refreshNonce, loadEventDetails]);

    const getLocationText = (eventData) => {
        const address = String(eventData?.address || '').trim();
        if (address) {
            return address;
        }

        const latitude = Number(eventData?.location?.latitude);
        const longitude = Number(eventData?.location?.longitude);
        if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
            return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
        }

        return null;
    };

    const getEventDateValue = (eventData) =>
        eventData?.endsAt ?? eventData?.endAt ?? eventData?.endDate ?? eventData?.finishAt ?? null;

    const formatEventDate = (value) => {
        if (!value) return null;
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) return null;
        return parsed.toLocaleDateString('es-ES', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });
    };

    const formatEventTime = (value) => {
        if (!value) return null;
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) return null;
        return parsed.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const openQuestionThread = useCallback((questionId) => {
        if (!questionId) {
            return;
        }

        if (typeof navigation.replace === 'function') {
            navigation.replace('QuestionThread', { questionId });
            return;
        }

        navigation.goBack();
        requestAnimationFrame(() => {
            navigation.navigate('QuestionThread', { questionId });
        });
    }, [navigation]);

    if (loading) {
        return (
            <Pressable style={styles.modalOverlay} onPress={() => navigation.goBack()}>
                <View style={styles.centerContainer}>
                    <ActivityIndicator size="large" color={theme.colors?.primary || '#0066cc'} />
                </View>
            </Pressable>
        );
    }

    if (error || !event) {
        return (
            <Pressable style={styles.modalOverlay} onPress={() => navigation.goBack()}>
                <Pressable style={[styles.modalCard, isNarrow && { width: '90%' }]} onPress={() => { }}>
                    <View style={styles.modalHeader}>
                        <Text style={styles.modalTitle}>Event Details</Text>
                        <TouchableOpacity onPress={() => navigation.goBack()}>
                            <Ionicons name="close" size={22} color="#6b7280" />
                        </TouchableOpacity>
                    </View>
                    <View style={styles.centerContainer}>
                        <Text style={styles.errorText}>{error || 'Event not found'}</Text>
                        <TouchableOpacity style={styles.retryButton} onPress={loadEventDetails}>
                            <Text style={styles.retryButtonText}>Retry</Text>
                        </TouchableOpacity>
                    </View>
                </Pressable>
            </Pressable>
        );
    }

    const renderAttendee = ({ item }) => (
        <View style={styles.attendeeItem}>
            <Text style={styles.attendeeName}>{item.userName}</Text>
            <Text style={styles.attendeeEmail}>{item.email}</Text>
        </View>
    );

    const renderEventQuestion = ({ item }) => (
        <TouchableOpacity
            style={styles.questionItem}
            onPress={() => openQuestionThread(item.id)}
            activeOpacity={0.8}
        >
            <Text style={styles.questionTitle}>{item.title || 'Untitled question'}</Text>
            <Text style={styles.questionContent} numberOfLines={2}>
                {item.content || 'No content'}
            </Text>
        </TouchableOpacity>
    );

    return (
        <Pressable style={styles.modalOverlay} onPress={() => navigation.goBack()}>
            <Pressable style={[styles.modalCard, isNarrow && { width: '90%' }]} onPress={() => { }}>
                <View style={styles.modalHeader}>
                    <Text style={styles.modalTitle}>{event.title}</Text>
                    <TouchableOpacity onPress={() => navigation.goBack()}>
                        <Ionicons name="close" size={22} color="#6b7280" />
                    </TouchableOpacity>
                </View>
                <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
                    <View style={styles.categoryBadge}>
                        <Text style={styles.category}>{event.category}</Text>
                    </View>

                    <View style={styles.contentContainer}>
                        <View style={styles.card}>
                            <View style={styles.sectionHeader}>
                                <Ionicons name="help-circle-outline" size={18} color={theme.colors?.primary || '#0066cc'} />
                                <Text style={styles.sectionTitle}>Event questions</Text>
                                <View style={styles.badge}>
                                    <Text style={styles.badgeText}>{eventQuestions.length}</Text>
                                </View>
                            </View>
                            {eventQuestions.length > 0 ? (
                                <FlatList
                                    data={eventQuestions}
                                    renderItem={renderEventQuestion}
                                    keyExtractor={(item) => item.id}
                                    scrollEnabled={false}
                                    ItemSeparatorComponent={() => <View style={styles.separator} />}
                                />
                            ) : (
                                <Text style={styles.noData}>No questions in this event yet</Text>
                            )}
                        </View>

                        <View style={styles.card}>
                            <View style={styles.sectionHeader}>
                                <Ionicons name="people" size={18} color={theme.colors?.primary || '#0066cc'} />
                                <Text style={styles.sectionTitle}>Attendees</Text>
                                <View style={styles.badge}>
                                    <Text style={styles.badgeText}>{event.attendeeCount || 0}</Text>
                                </View>
                            </View>
                            {attendees.length > 0 ? (
                                <FlatList
                                    data={attendees}
                                    renderItem={renderAttendee}
                                    keyExtractor={(item) => item.id}
                                    scrollEnabled={false}
                                    ItemSeparatorComponent={() => <View style={styles.separator} />}
                                />
                            ) : (
                                <Text style={styles.noData}>No attendees yet</Text>
                            )}
                        </View>

                        <View style={styles.metaGrid}>
                            <View style={[styles.metaCard, isNarrow && styles.metaCardNarrow]}>
                                <View style={styles.metaLabelRow}>
                                    <Ionicons name="location-outline" size={14} color="#64748b" />
                                    <Text style={styles.metaLabel}>Location</Text>
                                </View>
                                <Text style={styles.metaValue} numberOfLines={2}>
                                    {getLocationText(event) || 'Location not available'}
                                </Text>
                            </View>

                            <View style={[styles.metaCard, isNarrow && styles.metaCardNarrow]}>
                                <View style={styles.metaLabelRow}>
                                    <Ionicons name="calendar-outline" size={14} color="#64748b" />
                                    <Text style={styles.metaLabel}>End date</Text>
                                </View>
                                <Text style={styles.metaValue} numberOfLines={2}>
                                    {formatEventDate(getEventDateValue(event)) || 'Schedule not available'}
                                </Text>
                                <Text style={styles.metaSubValue} numberOfLines={1}>
                                    {formatEventTime(getEventDateValue(event)) || ''}
                                </Text>
                            </View>

                            <View style={[styles.metaCard, isNarrow && styles.metaCardNarrow]}>
                                <View style={styles.metaLabelRow}>
                                    <Ionicons name="document-text-outline" size={14} color="#64748b" />
                                    <Text style={styles.metaLabel}>Description</Text>
                                </View>
                                <Text style={styles.metaValue} numberOfLines={3}>
                                    {event.description || 'No description'}
                                </Text>
                            </View>

                            {event.creator && (
                                <View style={[styles.metaCard, isNarrow && styles.metaCardNarrow]}>
                                    <View style={styles.metaLabelRow}>
                                        <Ionicons name="person-outline" size={14} color="#64748b" />
                                        <Text style={styles.metaLabel}>Organizer</Text>
                                    </View>
                                    <Text style={styles.metaValue} numberOfLines={1}>
                                        {event.creator.companyName}
                                    </Text>
                                    <Text style={styles.metaSubValue} numberOfLines={1}>
                                        {event.creator.email}
                                    </Text>
                                </View>
                            )}
                        </View>

                        <TouchableOpacity
                            style={styles.askQuestionButton}
                            onPress={() =>
                                navigation.navigate('CreateQuestion', {
                                    eventId: event.id,
                                    eventTitle: event.title,
                                    eventData: event,
                                })
                            }
                            activeOpacity={0.8}
                        >
                            <Ionicons name="chatbubble-ellipses-outline" size={16} color="#fff" />
                            <Text style={styles.askQuestionButtonText}>Ask a question in this event</Text>
                        </TouchableOpacity>
                    </View>
                </ScrollView>
            </Pressable>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.35)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
    },
    modalCard: {
        width: '100%',
        maxWidth: 620,
        maxHeight: '92%',
        backgroundColor: '#ffffff',
        borderRadius: 24,
        overflow: 'hidden',
        borderWidth: 1,
        borderColor: 'rgba(15, 23, 42, 0.08)',
        shadowColor: '#0f172a',
        shadowOffset: { width: 0, height: 18 },
        shadowOpacity: 0.2,
        shadowRadius: 28,
        elevation: 10,
    },
    modalHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 18,
        paddingVertical: 14,
        borderBottomWidth: 1,
        borderBottomColor: '#dbeafe',
        backgroundColor: '#0f766e',
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '800',
        color: '#ffffff',
        flex: 1,
        paddingRight: 12,
    },
    centerContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingVertical: 40,
    },
    categoryBadge: {
        alignSelf: 'flex-start',
        backgroundColor: '#ecfeff',
        paddingHorizontal: 12,
        paddingVertical: 7,
        borderRadius: 999,
        marginHorizontal: 18,
        marginTop: 14,
        borderWidth: 1,
        borderColor: '#99f6e4',
    },
    category: {
        color: '#0f766e',
        fontWeight: '700',
        fontSize: 12,
    },
    contentContainer: {
        paddingHorizontal: 18,
        paddingTop: 4,
        paddingBottom: 18,
        gap: 10,
    },
    card: {
        backgroundColor: '#f8fafc',
        borderRadius: 18,
        padding: 16,
        borderWidth: 1,
        borderColor: '#e2e8f0',
        borderLeftWidth: 4,
        borderLeftColor: '#0f766e',
    },
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        marginBottom: 12,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '800',
        color: '#1f2937',
        flex: 1,
    },
    badge: {
        backgroundColor: '#0f766e',
        paddingHorizontal: 8,
        paddingVertical: 3,
        borderRadius: 10,
    },
    badgeText: {
        color: '#fff',
        fontSize: 11,
        fontWeight: '600',
    },
    askQuestionButton: {
        backgroundColor: '#0f766e',
        borderRadius: 16,
        paddingVertical: 14,
        paddingHorizontal: 14,
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'row',
        gap: 8,
        marginTop: 6,
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.12)',
    },
    askQuestionButtonText: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 14,
    },
    metaGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        gap: 10,
    },
    metaCard: {
        width: '48.5%',
        backgroundColor: '#f8fafc',
        borderRadius: 14,
        borderWidth: 1,
        borderColor: '#e2e8f0',
        paddingVertical: 12,
        paddingHorizontal: 12,
        minHeight: 92,
    },
    metaCardNarrow: {
        width: '100%',
    },
    metaLabelRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        marginBottom: 8,
    },
    metaLabel: {
        fontSize: 11,
        fontWeight: '700',
        color: '#64748b',
        textTransform: 'uppercase',
    },
    metaValue: {
        fontSize: 14,
        fontWeight: '700',
        color: '#1f2937',
    },
    metaSubValue: {
        marginTop: 4,
        fontSize: 12,
        color: '#64748b',
        fontWeight: '600',
    },
    description: {
        fontSize: 14,
        color: '#4b5563',
        lineHeight: 20,
    },
    detailRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        paddingVertical: 8,
    },
    detailLabel: {
        fontSize: 12,
        fontWeight: '600',
        color: '#6b7280',
        width: 74,
    },
    detail: {
        fontSize: 14,
        color: '#1f2937',
        flex: 1,
    },
    detailSmall: {
        fontSize: 12,
        color: '#6b7280',
        marginTop: 4,
    },
    coordinates: {
        fontSize: 12,
        color: '#9ca3af',
        marginTop: 8,
        fontStyle: 'italic',
    },
    attendeeItem: {
        backgroundColor: '#ffffff',
        padding: 10,
        borderRadius: 10,
        marginVertical: 4,
        borderWidth: 1,
        borderColor: '#eef2f7',
    },
    questionItem: {
        backgroundColor: '#ffffff',
        padding: 11,
        borderRadius: 12,
        marginVertical: 4,
        borderWidth: 1,
        borderColor: '#eef2f7',
    },
    questionTitle: {
        fontWeight: '600',
        color: '#1f2937',
        fontSize: 14,
    },
    questionContent: {
        fontSize: 12,
        color: '#6b7280',
        marginTop: 3,
    },
    attendeeName: {
        fontWeight: '600',
        color: '#1f2937',
        fontSize: 14,
    },
    attendeeEmail: {
        fontSize: 12,
        color: '#6b7280',
        marginTop: 3,
    },
    separator: {
        height: 1,
        backgroundColor: '#e5e7eb',
        marginVertical: 0,
    },
    noData: {
        fontSize: 14,
        color: '#9ca3af',
        fontStyle: 'italic',
        textAlign: 'center',
        paddingVertical: 12,
    },
    errorText: {
        color: '#dc2626',
        fontSize: 15,
        marginBottom: 16,
        textAlign: 'center',
    },
    retryButton: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        paddingHorizontal: 24,
        paddingVertical: 10,
        borderRadius: 8,
        marginTop: 8,
    },
    retryButtonText: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 14,
    },
});
