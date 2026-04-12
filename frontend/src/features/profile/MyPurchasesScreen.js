import React, { useCallback, useEffect, useState } from 'react';
import {
    View, Text, StyleSheet, SafeAreaView,
    TouchableOpacity, FlatList, ActivityIndicator, Platform,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../app/providers/AuthProvider';
import apiClient from '../../shared/services/http/apiClient';

const STATUS_META = {
    success: { label: 'Completado', color: '#15803d', backgroundColor: '#dcfce7' },
    pending: { label: 'Pendiente', color: '#b45309', backgroundColor: '#fef3c7' },
    failed: { label: 'Fallido', color: '#b91c1c', backgroundColor: '#fee2e2' },
    unknown: { label: 'Desconocido', color: '#4b5563', backgroundColor: '#e5e7eb' },
};

const TYPE_LABELS = {
    purchase: 'Compra',
    spend: 'Gasto',
    earn: 'Ingreso',
};

const sortByDateDesc = (left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
    return rightTime - leftTime;
};

const normalizePurchases = (rawPurchases) => {
    if (!Array.isArray(rawPurchases)) {
        return [];
    }

    return rawPurchases
        .filter((item) => item && item.id)
        .map((item) => ({
            id: item.id,
            amount: Number.isFinite(Number(item.amount)) ? Number(item.amount) : 0,
            currency: item.currency || 'StreetCoins',
            status: typeof item.status === 'string' ? item.status.toLowerCase() : 'unknown',
            type: typeof item.type === 'string' ? item.type.toLowerCase() : 'unknown',
            createdAt: item.createdAt || null,
        }))
        .sort(sortByDateDesc);
};

const formatAmount = (amount, currency) => {
    const normalizedAmount = Number.isFinite(Number(amount)) ? Number(amount) : 0;
    const normalizedCurrency = currency || 'StreetCoins';
    const sign = normalizedAmount > 0 ? '+' : '';
    return `${sign}${normalizedAmount} ${normalizedCurrency}`;
};

const formatPurchaseDate = (value) => {
    if (!value) {
        return 'Fecha no disponible';
    }

    const parsedDate = new Date(value);
    if (Number.isNaN(parsedDate.getTime())) {
        return 'Fecha no disponible';
    }

    return parsedDate.toLocaleString();
};

export default function MyPurchasesScreen({ navigation }) {
    const { user } = useAuth();
    const [purchases, setPurchases] = useState([]);
    const [loading, setLoading] = useState(true);
    const [networkError, setNetworkError] = useState('');

    const loadPurchases = useCallback(async () => {
        if (!user?.id) {
            setPurchases([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        setNetworkError('');

        try {
            const response = await apiClient.get('/api/v1/streetcoins/purchases');
            setPurchases(normalizePurchases(response?.data));
        } catch (error) {
            const statusCode = error?.response?.status;

            if (statusCode === 403) {
                setNetworkError('No tienes permisos para consultar tus compras.');
            } else {
                setNetworkError('No se pudo cargar tu historial de compras. Intentalo de nuevo.');
            }

            setPurchases([]);
        } finally {
            setLoading(false);
        }
    }, [user?.id]);

    useFocusEffect(
        useCallback(() => {
            loadPurchases();
        }, [loadPurchases])
    );

    useEffect(() => {
        if (Platform.OS !== 'web' || typeof window === 'undefined') {
            return;
        }

        const handlePurchaseConfirmed = () => {
            loadPurchases();
        };

        window.addEventListener('streetcoins:purchase-confirmed', handlePurchaseConfirmed);

        return () => {
            window.removeEventListener('streetcoins:purchase-confirmed', handlePurchaseConfirmed);
        };
    }, [loadPurchases]);

    const renderPurchaseItem = ({ item }) => {
        const statusMeta = STATUS_META[item.status] || STATUS_META.unknown;
        const typeLabel = TYPE_LABELS[item.type] || 'Transaccion';

        return (
            <View style={styles.purchaseItem}>
                <View style={styles.purchaseIconCircle}>
                    <Ionicons name="receipt-outline" size={22} color="#d90429" />
                </View>

                <View style={styles.purchaseInfo}>
                    <View style={styles.purchaseTopRow}>
                        <Text style={styles.purchaseAmount}>{formatAmount(item.amount, item.currency)}</Text>
                        <View style={[styles.statusBadge, { backgroundColor: statusMeta.backgroundColor }]}>
                            <View style={[styles.statusDot, { backgroundColor: statusMeta.color }]} />
                            <Text style={[styles.statusText, { color: statusMeta.color }]}>{statusMeta.label}</Text>
                        </View>
                    </View>
                    <Text style={styles.purchaseMeta}>{formatPurchaseDate(item.createdAt)}</Text>
                    <Text style={styles.purchaseType}>{typeLabel}</Text>
                </View>
            </View>
        );
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
                <Text style={styles.navTitle}>Mis Compras</Text>
            </View>

            <View style={styles.container}>
                {networkError ? (
                    <View style={styles.feedbackState}>
                        <Ionicons name="wifi-outline" size={56} color="#9ca3af" />
                        <Text style={styles.feedbackTitle}>Error de red</Text>
                        <Text style={styles.feedbackSubtitle}>{networkError}</Text>
                        <TouchableOpacity style={styles.retryButton} onPress={loadPurchases}>
                            <Text style={styles.retryButtonText}>Reintentar</Text>
                        </TouchableOpacity>
                    </View>
                ) : purchases.length === 0 ? (
                    <View style={styles.feedbackState}>
                        <Ionicons name="cart-outline" size={64} color="#ccc" />
                        <Text style={styles.feedbackTitle}>Aún no tienes compras</Text>
                        <Text style={styles.feedbackSubtitle}>
                            Cuando realices un pago, aparecerá aquí automáticamente.
                        </Text>
                    </View>
                ) : (
                    <FlatList
                        data={purchases}
                        keyExtractor={(item) => String(item.id)}
                        renderItem={renderPurchaseItem}
                        contentContainerStyle={styles.purchaseListContent}
                        showsVerticalScrollIndicator={false}
                    />
                )}
            </View>
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
    feedbackState: {
        alignItems: 'center',
        paddingVertical: 60,
    },
    feedbackTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#333',
        marginTop: 16,
        textAlign: 'center',
    },
    feedbackSubtitle: {
        fontSize: 14,
        color: '#999',
        textAlign: 'center',
        marginTop: 8,
        paddingHorizontal: 20,
    },
    retryButton: {
        marginTop: 18,
        backgroundColor: '#d90429',
        borderRadius: 10,
        paddingHorizontal: 18,
        paddingVertical: 10,
    },
    retryButtonText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 14,
    },
    purchaseListContent: {
        paddingBottom: 24,
    },
    purchaseItem: {
        backgroundColor: '#f9fafb',
        borderRadius: 12,
        padding: 14,
        marginBottom: 10,
        borderWidth: 1,
        borderColor: '#f0f0f0',
        flexDirection: 'row',
        alignItems: 'center',
    },
    purchaseIconCircle: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#fff1f1',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    purchaseInfo: {
        flex: 1,
    },
    purchaseTopRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    purchaseAmount: {
        fontSize: 15,
        fontWeight: '700',
        color: '#111827',
        marginRight: 10,
    },
    purchaseMeta: {
        fontSize: 12,
        color: '#6b7280',
        marginTop: 4,
    },
    purchaseType: {
        fontSize: 12,
        color: '#374151',
        marginTop: 2,
        fontWeight: '600',
    },
    statusBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 999,
        paddingVertical: 4,
        paddingHorizontal: 8,
    },
    statusDot: {
        width: 7,
        height: 7,
        borderRadius: 999,
        marginRight: 6,
    },
    statusText: {
        fontSize: 11,
        fontWeight: '700',
        textTransform: 'uppercase',
    },
});
