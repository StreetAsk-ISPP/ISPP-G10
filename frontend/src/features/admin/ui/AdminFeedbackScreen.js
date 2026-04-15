import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import apiClient from '../../../shared/services/http/apiClient';

export default function AdminFeedbackScreen() {
    const navigation = useNavigation();
    const [feedback, setFeedback] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchFeedback();
    }, []);

    const fetchFeedback = async () => {
        setLoading(true);
        try {
            const response = await apiClient.get('/api/v1/feedback');
            // Assuming response.data handles the list directly.
            setFeedback(response.data || []);
        } catch (error) {
            console.error("Error fetching feedback:", error);
            Alert.alert("Error", "Feedback could not be loaded");
        } finally {
            setLoading(false);
        }
    };

    const renderFeedbackItem = ({ item }) => (
        <View style={styles.card}>
            <View style={styles.cardHeader}>
                <Text style={styles.username}>{item.userName || item.user}</Text>
                {item.rating && (
                    <View style={styles.rating}>
                        <Ionicons name="star" size={14} color="#FFD700" />
                        <Text style={styles.ratingText}>{item.rating}/5</Text>
                    </View>
                )}
            </View>
            <Text style={styles.content}>{item.message || item.content}</Text>
            <Text style={styles.date}>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'No date'}</Text>
        </View>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>User Feedback</Text>
                <View style={{ width: 30 }} />
            </View>

            {loading ? (
                <ActivityIndicator size="large" color="#007bff" style={styles.loader} />
            ) : (
                <FlatList
                    data={feedback}
                    renderItem={renderFeedbackItem}
                    keyExtractor={item => item.id}
                    contentContainerStyle={styles.listContent}
                    ListEmptyComponent={
                        <Text style={styles.emptyText}>No feedback registered</Text>
                    }
                />
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f8f9fa' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 15,
        backgroundColor: 'white',
        borderBottomWidth: 1,
        borderBottomColor: '#eee'
    },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#333' },
    listContent: { padding: 15 },
    loader: { marginTop: 50 },
    emptyText: { textAlign: 'center', marginTop: 50, color: '#999', fontSize: 16 },
    card: {
        backgroundColor: 'white',
        borderRadius: 12,
        padding: 15,
        marginBottom: 15,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
        elevation: 2
    },
    cardHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
    username: { fontWeight: 'bold', fontSize: 15, color: '#007bff' },
    rating: { flexDirection: 'row', alignItems: 'center' },
    ratingText: { marginLeft: 4, fontWeight: '600', color: '#666', fontSize: 12 },
    content: { color: '#333', lineHeight: 20 },
    date: { color: '#999', fontSize: 11, marginTop: 10, textAlign: 'right' }
});