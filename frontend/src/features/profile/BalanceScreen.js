import React, { useCallback, useEffect, useState } from 'react';
import {
    View, Text, StyleSheet, SafeAreaView,
    TouchableOpacity, ScrollView, ActivityIndicator,
    Modal, Pressable, Linking, Platform,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../app/providers/AuthProvider';
import apiClient from '../../shared/services/http/apiClient';
import { STORAGE_KEYS } from '../../shared/constants/storageKeys';

export default function BalanceScreen({ navigation, route }) {
    const { user } = useAuth();
    const [balance, setBalance] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [packs, setPacks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [buyModalVisible, setBuyModalVisible] = useState(false);
    const [isStartingCheckoutForPack, setIsStartingCheckoutForPack] = useState('');
    const [purchaseError, setPurchaseError] = useState('');

    const loadWalletData = useCallback(async () => {
        if (!user?.id) {
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const [balanceResponse, transactionsResponse, packsResponse] = await Promise.all([
                apiClient.get('/api/v1/streetcoins/balance'),
                apiClient.get('/api/v1/streetcoins/transactions'),
                apiClient.get('/api/v1/streetcoins/packs'),
            ]);

            setBalance(balanceResponse?.data?.balance ?? 0);
            setTransactions(Array.isArray(transactionsResponse?.data) ? transactionsResponse.data : []);
            setPacks(Array.isArray(packsResponse?.data) ? packsResponse.data : []);
        } catch (error) {
            setBalance(0);
            setTransactions([]);
            setPurchaseError('Unable to load wallet data. Try again in a few seconds.');
        } finally {
            setLoading(false);
        }
    }, [user?.id]);

    useFocusEffect(useCallback(() => {
        loadWalletData();
    }, [loadWalletData]));

    useEffect(() => {
        if (!route?.params?.openShopModal) {
            return;
        }

        setPurchaseError('');
        setBuyModalVisible(true);
        if (typeof navigation?.setParams === 'function') {
            navigation.setParams({ openShopModal: false });
        }
    }, [route?.params?.openShopModal, navigation]);

    const createIdempotencyKey = () => {
        if (Platform.OS === 'web' && typeof window !== 'undefined' && window.crypto?.randomUUID) {
            return window.crypto.randomUUID();
        }
        return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    };

    const formatEur = (amountCents) => `${(Number(amountCents || 0) / 100).toFixed(2)}€`;

    const startStreetCoinsCheckout = async (packId) => {
        setPurchaseError('');
        setIsStartingCheckoutForPack(packId);

        try {
            const idempotencyKey = createIdempotencyKey();
            const response = await apiClient.post(
                '/api/v1/streetcoins/purchase',
                { packId },
                { headers: { 'Idempotency-Key': idempotencyKey } }
            );

            const checkoutUrl = response?.data?.checkoutUrl;
            if (!checkoutUrl) {
                setPurchaseError('Stripe checkout session could not be initialized.');
                return;
            }

            if (Platform.OS === 'web' && typeof window !== 'undefined') {
                window.localStorage.setItem(
                    STORAGE_KEYS.PENDING_STREETCOINS_CHECKOUT,
                    JSON.stringify({
                        transactionId: response?.data?.transactionId,
                        sessionId: response?.data?.sessionId,
                        streetCoins: response?.data?.streetCoins,
                        checkoutOrigin: route?.params?.checkoutOrigin || 'balance',
                    })
                );
                window.location.assign(checkoutUrl);
                return;
            }

            await Linking.openURL(checkoutUrl);
            setPurchaseError('Complete the Stripe payment and return to the app.');
        } catch (error) {
            const rawMessage = error?.response?.data?.message || error?.response?.data || error?.message;
            const message = typeof rawMessage === 'string' ? rawMessage : JSON.stringify(rawMessage);
            setPurchaseError(message || 'Unable to start checkout.');
        } finally {
            setIsStartingCheckoutForPack('');
            setBuyModalVisible(false);
        }
    };

    if (loading) {
        return (
            <SafeAreaView style={[styles.safeArea, styles.center]}>
                <ActivityIndicator size="large" color="white" />
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.navBar}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={28} color="white" />
                </TouchableOpacity>
                <Text style={styles.navTitle}>Balance</Text>
            </View>

            <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.balanceCard}>
                    <Ionicons name="wallet" size={40} color="#FFD700" />
                    <Text style={styles.balanceLabel}>StreetCoins</Text>
                    <Text style={styles.balanceAmount}>{balance ?? 0}</Text>

                    <TouchableOpacity
                        style={styles.buyButton}
                        onPress={() => {
                            setPurchaseError('');
                            setBuyModalVisible(true);
                        }}
                        activeOpacity={0.85}
                    >
                        <Ionicons name="card-outline" size={18} color="#fff" />
                        <Text style={styles.buyButtonText}>Buy StreetCoins</Text>
                    </TouchableOpacity>
                </View>

                {purchaseError ? <Text style={styles.errorText}>{purchaseError}</Text> : null}

                <Text style={styles.sectionTitle}>Transaction History</Text>

                {transactions.length === 0 ? (
                    <View style={styles.emptyState}>
                        <Ionicons name="receipt-outline" size={48} color="#ccc" />
                        <Text style={styles.emptyText}>No transactions yet.</Text>
                    </View>
                ) : (
                    transactions.map((tx, idx) => (
                        <View key={tx.id ?? idx} style={styles.txItem}>
                            <View style={styles.txIconCircle}>
                                <Ionicons
                                    name={tx.amount >= 0 ? 'arrow-down-circle' : 'arrow-up-circle'}
                                    size={24}
                                    color={tx.amount >= 0 ? '#22c55e' : '#ef4444'}
                                />
                            </View>
                            <View style={styles.txInfo}>
                                <Text style={styles.txDescription}>{tx.description || 'Transaction'}</Text>
                                <Text style={styles.txDate}>
                                    {tx.createdAt ? new Date(tx.createdAt).toLocaleDateString() : ''}
                                </Text>
                            </View>
                            <Text style={[styles.txAmount, { color: tx.amount >= 0 ? '#22c55e' : '#ef4444' }]}>
                                {tx.amount >= 0 ? '+' : ''}{tx.amount}
                            </Text>
                        </View>
                    ))
                )}
                <View style={{ height: 40 }} />
            </ScrollView>

            <Modal
                visible={buyModalVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setBuyModalVisible(false)}
            >
                <Pressable style={styles.modalOverlay} onPress={() => setBuyModalVisible(false)}>
                    <Pressable style={styles.modalCard} onPress={() => { }}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Choose your StreetCoins pack</Text>
                            <TouchableOpacity onPress={() => setBuyModalVisible(false)}>
                                <Ionicons name="close" size={22} color="#6b7280" />
                            </TouchableOpacity>
                        </View>

                        {packs.length === 0 ? (
                            <View style={styles.modalEmptyState}>
                                <ActivityIndicator size="small" color="#d90429" />
                                <Text style={styles.modalEmptyText}>Loading packs...</Text>
                            </View>
                        ) : (
                            packs.map((pack) => {
                                const isLoadingPack = isStartingCheckoutForPack === pack.packId;

                                return (
                                    <TouchableOpacity
                                        key={pack.packId}
                                        style={[styles.packItem, isLoadingPack && styles.packItemDisabled]}
                                        activeOpacity={0.85}
                                        onPress={() => startStreetCoinsCheckout(pack.packId)}
                                        disabled={Boolean(isStartingCheckoutForPack)}
                                    >
                                        <View>
                                            <Text style={styles.packLabel}>{pack.label}</Text>
                                            <Text style={styles.packCoins}>{pack.streetCoins} StreetCoins</Text>
                                        </View>
                                        <View style={styles.packPriceContainer}>
                                            <Text style={styles.packPrice}>{formatEur(pack.amountCents)}</Text>
                                            {isLoadingPack ? (
                                                <ActivityIndicator size="small" color="#d90429" />
                                            ) : (
                                                <Ionicons name="chevron-forward" size={18} color="#6b7280" />
                                            )}
                                        </View>
                                    </TouchableOpacity>
                                );
                            })
                        )}
                    </Pressable>
                </Pressable>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#d90429' },
    center: { justifyContent: 'center', alignItems: 'center' },
    navBar: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 15,
        paddingTop: 40,
        justifyContent: 'center',
    },
    backButton: { position: 'absolute', left: 15, top: 40 },
    navTitle: { color: 'white', fontSize: 18, fontWeight: 'bold' },
    container: {
        flex: 1,
        backgroundColor: '#fff',
        borderTopLeftRadius: 25,
        borderTopRightRadius: 25,
        padding: 20,
    },
    balanceCard: {
        backgroundColor: '#1f2937',
        borderRadius: 20,
        padding: 30,
        alignItems: 'center',
        marginBottom: 28,
        elevation: 5,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.2,
        shadowRadius: 4,
    },
    balanceLabel: { color: '#9ca3af', fontSize: 14, marginTop: 10 },
    balanceAmount: { color: '#FFD700', fontSize: 48, fontWeight: 'bold', marginTop: 4 },
    buyButton: {
        marginTop: 16,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: '#d90429',
        borderRadius: 10,
        paddingHorizontal: 14,
        paddingVertical: 10,
    },
    buyButtonText: { color: '#fff', fontWeight: '700', fontSize: 14 },
    errorText: {
        color: '#dc2626',
        fontSize: 13,
        marginBottom: 14,
        marginTop: -12,
    },
    sectionTitle: { fontSize: 16, fontWeight: 'bold', color: '#333', marginBottom: 14 },
    emptyState: { alignItems: 'center', paddingVertical: 40 },
    emptyText: { color: '#999', marginTop: 12, fontStyle: 'italic' },
    txItem: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#f9fafb',
        borderRadius: 12,
        padding: 14,
        marginBottom: 10,
        borderWidth: 1,
        borderColor: '#f0f0f0',
    },
    txIconCircle: {
        width: 42,
        height: 42,
        borderRadius: 21,
        backgroundColor: '#fff',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    txInfo: { flex: 1 },
    txDescription: { fontSize: 14, fontWeight: '600', color: '#333' },
    txDate: { fontSize: 12, color: '#999', marginTop: 2 },
    txAmount: { fontSize: 16, fontWeight: 'bold' },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.45)',
        justifyContent: 'center',
        paddingHorizontal: 20,
    },
    modalCard: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 18,
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    modalHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    modalTitle: { fontSize: 16, fontWeight: '800', color: '#1f2937' },
    modalEmptyState: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        paddingVertical: 10,
    },
    modalEmptyText: { color: '#6b7280', fontSize: 13 },
    packItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#f3f4f6',
    },
    packItemDisabled: { opacity: 0.7 },
    packLabel: { fontSize: 14, fontWeight: '700', color: '#111827' },
    packCoins: { fontSize: 12, color: '#6b7280', marginTop: 2 },
    packPriceContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    packPrice: { fontSize: 15, fontWeight: '800', color: '#d90429' },
});
