import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, FlatList } from 'react-native';
import EventService from '../../shared/services/EventService';
import { theme } from '../../shared/ui/theme/theme';

export default function EventDetailsScreen({ route }) {
    const { eventId } = route.params;
    const [event, setEvent] = useState(null);
    const [attendees, setAttendees] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadEventDetails();
    }, [eventId, loadEventDetails]);

    const loadEventDetails = async () => {
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
    };

    if (loading) {
        return (
            <View style={styles.centerContainer}>
                <ActivityIndicator size="large" color={theme.colors?.primary || '#0066cc'} />
            </View>
        );
    }

    if (error || !event) {
        return (
            <View style={styles.centerContainer}>
                <Text style={styles.errorText}>{error || 'Event not found'}</Text>
                <TouchableOpacity style={styles.retryButton} onPress={loadEventDetails}>
                    <Text style={styles.retryButtonText}>Retry</Text>
                </TouchableOpacity>
            </View>
        );
    }

    const renderAttendee = ({ item }) => (
        <View style={styles.attendeeItem}>
            <Text style={styles.attendeeName}>{item.userName}</Text>
            <Text style={styles.attendeeEmail}>{item.email}</Text>
        </View>
    );

    return (
        <ScrollView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.title}>{event.title}</Text>
                <View style={styles.categoryBadge}>
                    <Text style={styles.category}>{event.category}</Text>
                </View>
            </View>

            <View style={styles.section}>
                <Text style={styles.sectionTitle}>Description</Text>
                <Text style={styles.description}>{event.description}</Text>
            </View>

            <View style={styles.section}>
                <Text style={styles.sectionTitle}>Date & Time</Text>
                <Text style={styles.detail}>
                    📅 {event.startsAt ? new Date(event.startsAt).toLocaleDateString() : 'TBD'}
                </Text>
                <Text style={styles.detail}>
                    🕐 {event.startsAt ? new Date(event.startsAt).toLocaleTimeString() : 'TBD'} - {event.endsAt ? new Date(event.endsAt).toLocaleTimeString() : 'TBD'}
                </Text>
            </View>

            <View style={styles.section}>
                <Text style={styles.sectionTitle}>Location</Text>
                <Text style={styles.detail}>📍 {event.address || 'No address provided'}</Text>
                {event.latitude && event.longitude && (
                    <Text style={styles.coordinates}>
                        Coordinates: {event.latitude.toFixed(4)}, {event.longitude.toFixed(4)}
                    </Text>
                )}
            </View>

            <View style={styles.section}>
                <Text style={styles.sectionTitle}>Attendees ({event.attendeeCount || 0})</Text>
                {attendees.length > 0 ? (
                    <FlatList
                        data={attendees}
                        renderItem={renderAttendee}
                        keyExtractor={(item) => item.userId}
                        scrollEnabled={false}
                    />
                ) : (
                    <Text style={styles.noData}>No attendees yet</Text>
                )}
            </View>

            {event.creator && (
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>Organizer</Text>
                    <Text style={styles.detail}>{event.creator.companyName}</Text>
                    <Text style={styles.detail}>{event.creator.email}</Text>
                </View>
            )}
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: theme.colors?.background || '#fff',
    },
    centerContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    header: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        padding: 16,
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 8,
    },
    categoryBadge: {
        alignSelf: 'flex-start',
        backgroundColor: 'rgba(255,255,255,0.3)',
        paddingHorizontal: 12,
        paddingVertical: 4,
        borderRadius: 12,
    },
    category: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 12,
    },
    section: {
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderBottomWidth: 1,
        borderBottomColor: theme.colors?.border || '#e0e0e0',
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: theme.colors?.text || '#000',
        marginBottom: 8,
    },
    description: {
        fontSize: 14,
        color: theme.colors?.textSecondary || '#666',
        lineHeight: 20,
    },
    detail: {
        fontSize: 14,
        color: theme.colors?.text || '#000',
        marginVertical: 4,
    },
    coordinates: {
        fontSize: 12,
        color: theme.colors?.textSecondary || '#999',
        marginTop: 4,
    },
    attendeeItem: {
        backgroundColor: theme.colors?.card || '#f5f5f5',
        padding: 10,
        borderRadius: 6,
        marginVertical: 4,
    },
    attendeeName: {
        fontWeight: '600',
        color: theme.colors?.text || '#000',
    },
    attendeeEmail: {
        fontSize: 12,
        color: theme.colors?.textSecondary || '#666',
        marginTop: 2,
    },
    noData: {
        fontSize: 14,
        color: theme.colors?.textSecondary || '#666',
        fontStyle: 'italic',
    },
    errorText: {
        color: '#d32f2f',
        fontSize: 14,
        marginBottom: 16,
    },
    retryButton: {
        backgroundColor: theme.colors?.primary || '#0066cc',
        paddingHorizontal: 24,
        paddingVertical: 10,
        borderRadius: 6,
    },
    retryButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
});
