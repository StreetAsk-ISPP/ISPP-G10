import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, FlatList, SafeAreaView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import EventService from '../../shared/services/EventService';
import { theme } from '../../shared/ui/theme/theme';

export default function EventDetailsScreen({ route, navigation }) {
    const { eventId } = route.params;
    const [event, setEvent] = useState(null);
    const [attendees, setAttendees] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadEventDetails = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);

            const [eventData, attendeesData] = await Promise.all([
                EventService.getEventDetails(eventId),
                EventService.getEventAttendees(eventId)
            ]);

            setEvent(eventData);
            setAttendees(attendeesData || []);
        } catch (err) {
            console.error('Error loading event details:', err);
            setError(err.message || 'Failed to load event details');
        } finally {
            setLoading(false);
        }
    }, [eventId]);

    useEffect(() => {
        loadEventDetails();
    }, [eventId, loadEventDetails]);

    if (loading) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={styles.headerRed}>
                    <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                        <Ionicons name="arrow-back" size={24} color="#fff" />
                    </TouchableOpacity>
                </View>
                <View style={styles.centerContainer}>
                    <ActivityIndicator size="large" color={theme.colors?.primary || '#0066cc'} />
                </View>
            </SafeAreaView>
        );
    }

    if (error || !event) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={styles.headerRed}>
                    <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                        <Ionicons name="arrow-back" size={24} color="#fff" />
                    </TouchableOpacity>
                </View>
                <View style={styles.centerContainer}>
                    <Text style={styles.errorText}>{error || 'Event not found'}</Text>
                    <TouchableOpacity style={styles.retryButton} onPress={loadEventDetails}>
                        <Text style={styles.retryButtonText}>Retry</Text>
                    </TouchableOpacity>
                </View>
            </SafeAreaView>
        );
    }

    const renderAttendee = ({ item }) => (
        <View style={styles.attendeeItem}>
            <Text style={styles.attendeeName}>{item.userName}</Text>
            <Text style={styles.attendeeEmail}>{item.email}</Text>
        </View>
    );

    return (
        <SafeAreaView style={styles.screen}>
            <View style={styles.headerRed}>
                <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                    <Ionicons name="arrow-back" size={24} color="#fff" />
                </TouchableOpacity>
            </View>
            <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.header}>
                    <View style={styles.headerContent}>
                        <Text style={styles.title}>{event.title}</Text>
                        <View style={styles.categoryBadge}>
                            <Text style={styles.category}>{event.category}</Text>
                        </View>
                    </View>
                </View>

                <View style={styles.contentContainer}>
                    <View style={styles.card}>
                        <View style={styles.sectionHeader}>
                            <Ionicons name="document-text" size={20} color={theme.colors?.primary || '#0066cc'} />
                            <Text style={styles.sectionTitle}>Description</Text>
                        </View>
                        <Text style={styles.description}>{event.description}</Text>
                    </View>

                    <View style={styles.card}>
                        <View style={styles.sectionHeader}>
                            <Ionicons name="calendar" size={20} color={theme.colors?.primary || '#0066cc'} />
                            <Text style={styles.sectionTitle}>Date & Time</Text>
                        </View>
                        <View style={styles.detailRow}>
                            <Text style={styles.detailLabel}>Date:</Text>
                            <Text style={styles.detail}>
                                {event.startsAt ? new Date(event.startsAt).toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) : 'No date set'}
                            </Text>
                        </View>
                        <View style={styles.detailRow}>
                            <Text style={styles.detailLabel}>Time:</Text>
                            <Text style={styles.detail}>
                                {event.startsAt ? new Date(event.startsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                                {event.startsAt && event.endsAt && ` - ${new Date(event.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
                            </Text>
                        </View>
                    </View>

                    <View style={styles.card}>
                        <View style={styles.sectionHeader}>
                            <Ionicons name="location" size={20} color={theme.colors?.primary || '#0066cc'} />
                            <Text style={styles.sectionTitle}>Location</Text>
                        </View>
                        <Text style={styles.detail}>{event.address || 'No address provided'}</Text>
                        {event.latitude && event.longitude && (
                            <Text style={styles.coordinates}>
                                📍 {event.latitude.toFixed(4)}, {event.longitude.toFixed(4)}
                            </Text>
                        )}
                    </View>

                    <View style={styles.card}>
                        <View style={styles.sectionHeader}>
                            <Ionicons name="people" size={20} color={theme.colors?.primary || '#0066cc'} />
                            <Text style={styles.sectionTitle}>Attendees</Text>
                            <View style={styles.badge}>
                                <Text style={styles.badgeText}>{event.attendeeCount || 0}</Text>
                            </View>
                        </View>
                        {attendees.length > 0 ? (
                            <FlatList
                                data={attendees}
                                renderItem={renderAttendee}
                                keyExtractor={(item) => item.userId}
                                scrollEnabled={false}
                                ItemSeparatorComponent={() => <View style={styles.separator} />}
                            />
                        ) : (
                            <Text style={styles.noData}>No attendees yet</Text>
                        )}
                    </View>

                    {event.creator && (
                        <View style={styles.card}>
                            <View style={styles.sectionHeader}>
                                <Ionicons name="person-circle" size={20} color={theme.colors?.primary || '#0066cc'} />
                                <Text style={styles.sectionTitle}>Organizer</Text>
                            </View>
                            <Text style={styles.detail}>{event.creator.companyName}</Text>
                            <Text style={styles.detailSmall}>{event.creator.email}</Text>
                        </View>
                    )}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    container: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    centerContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    headerRed: {
        backgroundColor: '#d90429',
        paddingVertical: 12,
        paddingHorizontal: 16,
    },
    backBtn: {
        paddingVertical: 8,
    },
    header: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        paddingHorizontal: 20,
        paddingVertical: 24,
        marginBottom: 16,
    },
    headerContent: {
        gap: 12,
    },
    title: {
        fontSize: 28,
        fontWeight: '700',
        color: '#fff',
    },
    categoryBadge: {
        alignSelf: 'flex-start',
        backgroundColor: 'rgba(255,255,255,0.2)',
        paddingHorizontal: 14,
        paddingVertical: 6,
        borderRadius: 16,
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.3)',
    },
    category: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 13,
    },
    contentContainer: {
        paddingHorizontal: 16,
        paddingBottom: 24,
        gap: 12,
    },
    card: {
        backgroundColor: '#fff',
        borderRadius: 12,
        padding: 18,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.08,
        shadowRadius: 8,
        elevation: 3,
    },
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        marginBottom: 14,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: theme.colors?.text || '#000',
        flex: 1,
    },
    badge: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 12,
    },
    badgeText: {
        color: '#fff',
        fontSize: 12,
        fontWeight: '600',
    },
    description: {
        fontSize: 15,
        color: theme.colors?.textSecondary || '#666',
        lineHeight: 22,
    },
    detailRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginVertical: 10,
        paddingVertical: 8,
    },
    detailLabel: {
        fontSize: 13,
        fontWeight: '600',
        color: theme.colors?.textSecondary || '#999',
        width: 70,
    },
    detail: {
        fontSize: 15,
        color: theme.colors?.text || '#000',
        flex: 1,
    },
    detailSmall: {
        fontSize: 13,
        color: theme.colors?.textSecondary || '#666',
        marginTop: 4,
    },
    coordinates: {
        fontSize: 13,
        color: theme.colors?.textSecondary || '#999',
        marginTop: 10,
        fontStyle: 'italic',
    },
    attendeeItem: {
        backgroundColor: '#f8f9fa',
        padding: 12,
        borderRadius: 8,
        marginVertical: 6,
    },
    attendeeName: {
        fontWeight: '700',
        color: theme.colors?.text || '#000',
        fontSize: 15,
    },
    attendeeEmail: {
        fontSize: 13,
        color: theme.colors?.textSecondary || '#666',
        marginTop: 4,
    },
    separator: {
        height: 1,
        backgroundColor: '#e0e0e0',
        marginVertical: 0,
    },
    noData: {
        fontSize: 15,
        color: theme.colors?.textSecondary || '#666',
        fontStyle: 'italic',
        textAlign: 'center',
        paddingVertical: 16,
    },
    errorText: {
        color: '#d32f2f',
        fontSize: 16,
        marginBottom: 16,
        textAlign: 'center',
    },
    retryButton: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        paddingHorizontal: 28,
        paddingVertical: 12,
        borderRadius: 8,
        marginTop: 8,
    },
    retryButtonText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 15,
    },
});
