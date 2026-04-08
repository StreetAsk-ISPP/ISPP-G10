import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, RefreshControl } from 'react-native';
import EventService from '../../shared/services/EventService';
import { theme } from '../../shared/ui/theme/theme';

export default function EventListScreen({ navigation }) {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => {
        loadEvents();
    }, []);

    const loadEvents = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await EventService.getAllEvents();
            setEvents(response || []);
        } catch (err) {
            console.error('Error loading events:', err);
            setError(err.message || 'Failed to load events');
        } finally {
            setLoading(false);
        }
    };

    const onRefresh = async () => {
        setRefreshing(true);
        await loadEvents();
        setRefreshing(false);
    };

    const handleEventPress = (eventId) => {
        navigation.navigate('EventDetails', { eventId });
    };

    const renderEventCard = ({ item }) => (
        <TouchableOpacity
            style={styles.eventCard}
            onPress={() => handleEventPress(item.id)}
        >
            <Text style={styles.eventTitle}>{item.title}</Text>
            <Text style={styles.eventDescription} numberOfLines={2}>{item.description}</Text>
            <View style={styles.eventMeta}>
                <Text style={styles.eventCategory}>{item.category}</Text>
                <Text style={styles.eventAttendees}>{item.attendeeCount} attending</Text>
            </View>
        </TouchableOpacity>
    );

    if (loading) {
        return (
            <View style={styles.centerContainer}>
                <ActivityIndicator size="large" color={theme.colors?.primary || '#0066cc'} />
            </View>
        );
    }

    if (error) {
        return (
            <View style={styles.centerContainer}>
                <Text style={styles.errorText}>{error}</Text>
                <TouchableOpacity style={styles.retryButton} onPress={loadEvents}>
                    <Text style={styles.retryButtonText}>Retry</Text>
                </TouchableOpacity>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <View style={styles.headerContainer}>
                <Text style={styles.header}>Events</Text>
            </View>
            {events.length === 0 ? (
                <View style={styles.centerContainer}>
                    <Text style={styles.emptyText}>No events found</Text>
                </View>
            ) : (
                <FlatList
                    data={events}
                    renderItem={renderEventCard}
                    keyExtractor={(item) => item.id}
                    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
                    contentContainerStyle={styles.listContent}
                />
            )}
        </View>
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
    headerContainer: {
        borderBottomWidth: 3,
        borderBottomColor: '#ef4444',
        paddingHorizontal: 16,
    },
    header: {
        fontSize: 24,
        fontWeight: 'bold',
        paddingVertical: 12,
        color: theme.colors?.text || '#000',
    },
    listContent: {
        paddingHorizontal: 12,
        paddingVertical: 8,
    },
    eventCard: {
        backgroundColor: theme.colors?.card || '#f5f5f5',
        borderRadius: 8,
        padding: 12,
        marginVertical: 8,
        borderLeftWidth: 4,
        borderLeftColor: theme.colors?.primary || '#0066cc',
    },
    eventTitle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: theme.colors?.text || '#000',
        marginBottom: 4,
    },
    eventDescription: {
        fontSize: 13,
        color: theme.colors?.textSecondary || '#666',
        marginBottom: 8,
    },
    eventMeta: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    eventCategory: {
        fontSize: 12,
        color: theme.colors?.primary || '#0066cc',
        fontWeight: '600',
    },
    eventAttendees: {
        fontSize: 12,
        color: theme.colors?.textSecondary || '#666',
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
    emptyText: {
        fontSize: 16,
        color: theme.colors?.textSecondary || '#666',
    },
});
