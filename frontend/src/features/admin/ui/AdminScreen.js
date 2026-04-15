import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useAuth } from '../../../app/providers/AuthProvider';
import ConfirmationModal from '../../../shared/components/ConfirmationModal';
import apiClient from '../../../shared/services/http/apiClient';

export default function AdminScreen() {
    const navigation = useNavigation();
    const { logout } = useAuth();
    const [stats, setStats] = useState({ users: 0, questions: 0, answers: 0 });
    const [loading, setLoading] = useState(true);
    const [showLogoutModal, setShowLogoutModal] = useState(false);

    const getCollectionCount = (payload) => {
        let data = payload;

        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch {
                return 0;
            }
        }

        if (Array.isArray(data)) {
            return data.length;
        }

        if (data && typeof data === 'object') {
            if (Array.isArray(data.content)) return data.content.length;
            if (Array.isArray(data.items)) return data.items.length;
            if (Array.isArray(data.results)) return data.results.length;

            const totalElements = Number(data.totalElements);
            if (Number.isFinite(totalElements)) return totalElements;

            const count = Number(data.count);
            if (Number.isFinite(count)) return count;
        }

        return 0;
    };

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = () => {
        setLoading(true);

        Promise.allSettled([
            apiClient.get('/api/v1/users'),
            apiClient.get('/api/v1/questions', { params: { active: true } }),
            apiClient.get('/api/v1/answers'),
        ])
            .then(([usersResult, questionsResult, answersResult]) => {
                const usersData = usersResult.status === 'fulfilled' ? usersResult.value.data : [];
                const allUsers = Array.isArray(usersData) ? usersData : [];
                const activeUsers = allUsers.filter(u => u.active !== false && u.accountType != null);

                const questionsData = questionsResult.status === 'fulfilled' ? questionsResult.value.data : [];
                const answersData = answersResult.status === 'fulfilled' ? answersResult.value.data : [];

                setStats({
                    users: activeUsers.length,
                    questions: getCollectionCount(questionsData),
                    answers: getCollectionCount(answersData),
                });
            })
            .finally(() => {
                setLoading(false);
            });
    };

    const handleLogoutClick = () => {
        setShowLogoutModal(true);
    };

    const handleLogoutConfirm = async () => {
        setShowLogoutModal(false);
        try {
            await logout();
        } catch (error) {
            console.error(error);
        }
    };

    const menuItems = [
        { id: 'users', title: 'Manage Users', icon: 'people', route: 'AdminUsers' },
        { id: 'businesses', title: 'Business Verification', icon: 'business', route: 'AdminBusinessVerification' },
        { id: 'feedback', title: 'Received Feedback', icon: 'chatbox-ellipses', route: 'AdminFeedback' },
        { id: 'app', title: 'Go to App', icon: 'phone-portrait', route: 'Home' },
    ];

    const renderMenuItem = ({ item }) => (
        <TouchableOpacity
            style={styles.menuItem}
            onPress={() => navigation.navigate(item.route)}
        >
            <View style={[styles.iconContainer, { backgroundColor: '#e3f2fd' }]}>
                <Ionicons name={item.icon} size={24} color="#007bff" />
            </View>
            <Text style={styles.menuItemText}>{item.title}</Text>
            <Ionicons name="chevron-forward" size={20} color="#ccc" />
        </TouchableOpacity>
    );

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#007bff" />
            </View>
        );
    }

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <View>
                    <Text style={styles.headerTitle}>Admin Dashboard</Text>
                    <Text style={styles.headerSubtitle}>Welcome, Admin</Text>
                </View>
                <TouchableOpacity onPress={handleLogoutClick} style={styles.logoutButton}>
                    <Ionicons name="log-out-outline" size={24} color="#d90429" />
                </TouchableOpacity>
            </View>

            <View style={styles.statsContainer}>
                <View style={styles.statCard}>
                    <Text style={styles.statValue}>{stats.users}</Text>
                    <Text style={styles.statLabel}>Users</Text>
                </View>
                <View style={styles.statCard}>
                    <Text style={styles.statValue}>{stats.questions}</Text>
                    <Text style={styles.statLabel}>Questions</Text>
                </View>
                <View style={styles.statCard}>
                    <Text style={styles.statValue}>{stats.answers}</Text>
                    <Text style={styles.statLabel}>Answers</Text>
                </View>
            </View>

            <Text style={styles.sectionTitle}>Quick Actions</Text>

            <FlatList
                data={menuItems}
                renderItem={renderMenuItem}
                keyExtractor={item => item.id}
                contentContainerStyle={styles.menuList}
                scrollEnabled={false}
            />

            <ConfirmationModal
                visible={showLogoutModal}
                title="Log out?"
                message="Are you sure you want to log out?"
                confirmText="Log out"
                cancelText="Go Back"
                onConfirm={handleLogoutConfirm}
                onCancel={() => setShowLogoutModal(false)}
                confirmButtonColor="danger"
            />
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f8f9fa', padding: 20 },
    loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 25,
        marginTop: 10
    },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: '#333' },
    headerSubtitle: { fontSize: 14, color: '#666' },
    logoutButton: { padding: 10, backgroundColor: '#fff0f3', borderRadius: 10 },
    statsContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 30
    },
    statCard: {
        backgroundColor: 'white',
        borderRadius: 15,
        padding: 15,
        width: '31%',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 5,
        elevation: 2
    },
    statValue: { fontSize: 22, fontWeight: 'bold', color: '#007bff', marginBottom: 5 },
    statLabel: { fontSize: 12, color: '#666' },
    sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#333', marginBottom: 15 },
    menuList: { gap: 15 },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'white',
        padding: 15,
        borderRadius: 15,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 3,
        elevation: 2,
        marginBottom: 15
    },
    iconContainer: {
        width: 45,
        height: 45,
        borderRadius: 12,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 15
    },
    menuItemText: { flex: 1, fontSize: 16, fontWeight: '500', color: '#333' }
});