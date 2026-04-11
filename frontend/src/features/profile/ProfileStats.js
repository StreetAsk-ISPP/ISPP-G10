import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, SafeAreaView, ActivityIndicator, Modal } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useAuth } from '../../app/providers/AuthProvider';
import apiClient from '../../shared/services/http/apiClient';

export default function ProfileStats() {
    const navigation = useNavigation();
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState('questions');

    const [serverStats, setServerStats] = useState({
        questions: 0,
        answers: 0,
        username: '',
        role: '',
        coinBalance: 0,
        likes: 0,
        dislikes: 0,
        reputation: 0,
        rating: 0
    });
    const [userQuestions, setUserQuestions] = useState([]);
    const [userAnswers, setUserAnswers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showCoinInfo, setShowCoinInfo] = useState(false);

    useEffect(() => {
        const fetchAllData = async () => {
            if (!user?.id) {
                setLoading(false);
                return;
            }

            try {
                const [resStats, resQuestions, resAnswers] = await Promise.all([
                    apiClient.get(`/api/v1/users/${user.id}/stats`),
                    apiClient.get(`/api/v1/users/${user.id}/questions`),
                    apiClient.get(`/api/v1/users/${user.id}/answers`)
                ]);

                if (resStats.data) {
                    const data = resStats.data;
                    setServerStats({
                        questions: data.questionsCount || 0,
                        answers: data.answersCount || 0,
                        username: data.username || user.username,
                        role: data.role === 'ADMIN' ? 'Moderator' : 'Local Expert',
                        coinBalance: data.coinBalance || 0,
                        likes: data.likesCount || 0,
                        dislikes: data.dislikesCount || 0,
                        reputation: data.reputation || 0,
                        rating: data.rating != null ? data.rating : 0
                    });
                }

                if (resQuestions.data) setUserQuestions(resQuestions.data);
                if (resAnswers.data) setUserAnswers(resAnswers.data);

            } catch (error) {
                console.error("Error loading profile data:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchAllData();
    }, [user]);

    const renderHistoryItem = (item, type, index) => (
        <View key={item.id ? `${type}-${item.id}` : `${type}-${index}`} style={styles.historyItem}>
            <View style={styles.iconCircle}>
                <Ionicons
                    name={type === 'questions' ? "help-circle-outline" : "chatbubble-ellipses-outline"}
                    size={24}
                    color="#d90429"
                />
            </View>
            <View style={styles.historyText}>
                <Text style={styles.itemTitle} numberOfLines={2}>
                    <Text style={{ color: '#d90429', fontWeight: '800' }}>
                        {type === 'questions' ? 'Q: ' : 'A: '}
                    </Text>
                    <Text style={{ color: '#d90429', fontWeight: '800' }}></Text>
                    {type === 'answers'
                        ? (item.content || item.text || item.title || "No content")
                        : (item.title || item.text || item.content || "No content")}
                </Text>
                <View style={styles.itemFooter}>
                    <Ionicons name="calendar-outline" size={12} color="#999" />
                    <Text style={styles.itemDate}>
                        {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Unknown date'}
                    </Text>
                </View>
            </View>
            <Ionicons name="chevron-forward" size={18} color="#eee" />
        </View >
    );

    if (loading) {
        return (
            <SafeAreaView style={[styles.safeArea, styles.center]}>
                <ActivityIndicator size="large" color="white" />
                <Text style={styles.loadingText}>Syncing with Streetask...</Text>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.navBar}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={28} color="white" />
                </TouchableOpacity>
                <View style={styles.logoContainer}>
                    <Ionicons name="stats-chart" size={20} color="white" />
                    <Text style={styles.logoText}>INSIGHTS</Text>
                </View>
            </View>

            <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.headerCard}>
                    <Text style={styles.rangoText}>{serverStats.role}</Text>
                    <Text style={styles.userEmailText}>{user?.email || serverStats.username}</Text>
                    <View style={styles.coinsRow}>
                        <View style={styles.coinBadge}>
                            <Ionicons name="star" size={16} color="#FFD700" />
                            <Text style={styles.coinsText}>{serverStats.coinBalance} StreetCoins</Text>
                            <TouchableOpacity
                                onPress={() => setShowCoinInfo(true)}
                                style={styles.infoButton}
                                accessibilityRole="button"
                                accessibilityLabel="How StreetCoins work"
                            >
                                <Text style={styles.infoButtonText}>?</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>

                <View style={styles.statsGrid}>
                    <View style={styles.statBox}>
                        <Text style={styles.statValue}>{serverStats.questions}</Text>
                        <Text style={styles.statLabel}>Asked questions</Text>
                    </View>
                    <View style={styles.statBox}>
                        <Text style={styles.statValue}>{serverStats.answers}</Text>
                        <Text style={styles.statLabel}>Answered questions</Text>
                    </View>
                    <View style={[styles.statBox, styles.borderGreen]}>
                        <Text style={[styles.statValue, { color: '#2e7d32' }]}>{serverStats.likes} 👍</Text>
                        <Text style={styles.statLabel}>Likes received</Text>
                    </View>
                    <View style={[styles.statBox, styles.borderRed]}>
                        <Text style={[styles.statValue, { color: '#c62828' }]}>{serverStats.dislikes} 👎</Text>
                        <Text style={styles.statLabel}>Dislikes received</Text>
                    </View>
                </View>

                <View style={styles.reputationCard}>
                    <View style={styles.reputationLeft}>
                        <View style={styles.trophyCircle}>
                            <Ionicons name="trophy" size={28} color="#F5A623" />
                        </View>
                    </View>
                    <View style={styles.reputationCenter}>
                        <Text style={styles.reputationTitle}>Reputation Score</Text>
                        <Text style={styles.reputationSub}>Based on community trust</Text>
                    </View>
                    <View style={styles.reputationRight}>
                        <Text style={styles.reputationValue}>{parseFloat(serverStats.rating).toFixed(1)}</Text>
                        <Text style={styles.reputationPts}>/5</Text>
                    </View>
                </View>

                <Text style={styles.sectionTitle}>Activity History</Text>
                <View style={styles.tabContainer}>
                    <TouchableOpacity
                        style={[styles.tab, activeTab === 'questions' && styles.activeTab]}
                        onPress={() => setActiveTab('questions')}
                    >
                        <Text style={activeTab === 'questions' ? styles.activeTabText : styles.tabText}>Questions</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.tab, activeTab === 'answers' && styles.activeTab]}
                        onPress={() => setActiveTab('answers')}
                    >
                        <Text style={activeTab === 'answers' ? styles.activeTabText : styles.tabText}>Answers</Text>
                    </TouchableOpacity>
                </View>

                <View style={styles.historyList}>
                    {activeTab === 'questions' ? (
                        userQuestions.length > 0
                            ? userQuestions.map((q, index) => renderHistoryItem(q, 'questions', index))
                            : <Text style={styles.emptyText}>{"You haven't asked any questions yet."}</Text>
                    ) : (
                        userAnswers.length > 0
                            ? userAnswers.map((a, index) => renderHistoryItem(a, 'answers', index))
                            : <Text style={styles.emptyText}>{"You haven't answered any questions yet."}</Text>
                    )}
                </View>
                <View style={{ height: 40 }} />
            </ScrollView>

            <Modal
                visible={showCoinInfo}
                transparent
                animationType="fade"
                onRequestClose={() => setShowCoinInfo(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        <Text style={styles.modalTitle}>How StreetCoins work</Text>

                        <Text style={styles.modalLine}>- +1 coin for each answer you post.</Text>
                        <Text style={styles.modalLine}>- +1 extra if likes are greater than dislikes.</Text>
                        <Text style={styles.modalLine}>- -1 if dislikes are greater than likes.</Text>

                        <Text style={styles.modalNote}>
                            Coins update automatically when votes change.
                        </Text>

                        <TouchableOpacity style={styles.modalCloseButton} onPress={() => setShowCoinInfo(false)}>
                            <Text style={styles.modalCloseText}>Got it</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#d90429' },
    center: { justifyContent: 'center', alignItems: 'center' },
    loadingText: { color: 'white', marginTop: 12, fontWeight: '600', letterSpacing: 0.5 },
    navBar: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 15,
        paddingTop: 50,
        paddingBottom: 20,
        justifyContent: 'center',
    },
    backButton: { position: 'absolute', left: 15, top: 50 },
    logoContainer: { flexDirection: 'row', alignItems: 'center' },
    logoText: { color: 'white', fontSize: 16, fontWeight: '900', marginLeft: 8, letterSpacing: 1 },
    container: { flex: 1, backgroundColor: '#f8f9fa', borderTopLeftRadius: 30, borderTopRightRadius: 30, padding: 20 },
    headerCard: {
        backgroundColor: '#fff',
        padding: 24,
        borderRadius: 24,
        alignItems: 'center',
        marginBottom: 25,
        elevation: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 8,
    },
    rangoText: { color: '#d90429', fontSize: 24, fontWeight: '900' },
    userEmailText: { color: '#666', fontSize: 14, marginTop: 4, fontWeight: '500' },
    coinsRow: { marginTop: 15 },
    coinBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff9db', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, borderWidth: 1, borderColor: '#ffec99' },
    coinsText: { color: '#856404', marginLeft: 6, fontSize: 15, fontWeight: '700' },
    infoButton: {
        marginLeft: 8,
        width: 18,
        height: 18,
        borderRadius: 9,
        backgroundColor: '#856404',
        alignItems: 'center',
        justifyContent: 'center',
    },
    infoButtonText: { color: '#fff', fontSize: 12, fontWeight: '900' },
    statsGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 10 },
    statBox: {
        backgroundColor: '#fff',
        width: '48%',
        padding: 18,
        borderRadius: 20,
        marginBottom: 15,
        alignItems: 'center',
        elevation: 2,
        shadowColor: '#000',
        shadowOpacity: 0.05,
    },
    borderGreen: { borderLeftColor: '#4CAF50', borderLeftWidth: 5 },
    borderRed: { borderLeftColor: '#F44336', borderLeftWidth: 5 },
    reputationCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#343a40',
        borderRadius: 20,
        padding: 20,
        marginBottom: 30,
    },
    trophyCircle: { width: 50, height: 50, borderRadius: 25, backgroundColor: 'rgba(245, 166, 35, 0.15)', justifyContent: 'center', alignItems: 'center' },
    reputationLeft: { marginRight: 15 },
    reputationCenter: { flex: 1 },
    reputationTitle: { fontSize: 16, fontWeight: 'bold', color: '#fff' },
    reputationSub: { fontSize: 12, color: '#adb5bd', marginTop: 2 },
    reputationRight: { alignItems: 'flex-end' },
    reputationValue: { fontSize: 32, fontWeight: '900', color: '#F5A623' },
    reputationPts: { fontSize: 12, color: '#F5A623', fontWeight: '700', marginTop: -5 },
    statValue: { fontSize: 22, fontWeight: '900', color: '#1a1a1a' },
    statLabel: { fontSize: 11, color: '#888', marginTop: 4, fontWeight: '600', textTransform: 'uppercase' },
    sectionTitle: { fontSize: 18, fontWeight: '900', marginBottom: 15, color: '#1a1a1a', letterSpacing: 0.5 },
    tabContainer: { flexDirection: 'row', backgroundColor: '#e9ecef', borderRadius: 15, padding: 5, marginBottom: 20 },
    tab: { flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 12 },
    activeTab: { backgroundColor: '#fff', elevation: 3, shadowColor: '#000', shadowOpacity: 0.1 },
    tabText: { color: '#6c757d', fontWeight: '600' },
    activeTabText: { color: '#d90429', fontWeight: '800' },
    historyItem: {
        flexDirection: 'row',
        backgroundColor: '#fff',
        padding: 16,
        borderRadius: 18,
        marginBottom: 12,
        alignItems: 'center',
        elevation: 1,
    },
    iconCircle: { width: 44, height: 44, borderRadius: 12, backgroundColor: '#fff1f1', justifyContent: 'center', alignItems: 'center' },
    historyText: { marginLeft: 15, flex: 1 },
    itemTitle: { fontSize: 14, fontWeight: '700', color: '#2d3436', lineHeight: 20 },
    itemFooter: { flexDirection: 'row', alignItems: 'center', marginTop: 6 },
    itemDate: { fontSize: 11, color: '#999', marginLeft: 4, fontWeight: '500' },
    emptyText: { textAlign: 'center', color: '#adb5bd', marginTop: 30, fontStyle: 'italic', fontSize: 14 },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.45)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 24,
    },
    modalCard: {
        width: '100%',
        maxWidth: 360,
        backgroundColor: '#fff',
        borderRadius: 18,
        padding: 20,
    },
    modalTitle: { fontSize: 18, fontWeight: '900', color: '#1a1a1a', marginBottom: 12 },
    modalLine: { fontSize: 14, color: '#343a40', marginBottom: 8, lineHeight: 20 },
    modalNote: { fontSize: 12, color: '#6c757d', marginTop: 6, marginBottom: 16 },
    modalCloseButton: {
        backgroundColor: '#d90429',
        borderRadius: 12,
        paddingVertical: 10,
        alignItems: 'center',
    },
    modalCloseText: { color: '#fff', fontSize: 14, fontWeight: '800' },
});