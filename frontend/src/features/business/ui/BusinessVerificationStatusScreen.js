import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, Alert, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import apiClient from '../../../shared/services/http/apiClient';

const STATUS_CONFIG = {
    PENDING: {
        icon: 'time-outline',
        color: '#f59e0b',
        bg: '#fffbeb',
        title: 'Verification Pending',
        description: 'Your business account is under review. An administrator will evaluate your registration soon.',
    },
    APPROVED: {
        icon: 'checkmark-circle-outline',
        color: '#22c55e',
        bg: '#f0fdf4',
        title: 'Verified Business',
        description: 'Your business account has been verified. You now have full access to all business features.',
    },
    REJECTED: {
        icon: 'close-circle-outline',
        color: '#ef4444',
        bg: '#fff0f0',
        title: 'Verification Rejected',
        description: 'Your business account registration was not approved.',
    },
};

export default function BusinessVerificationStatusScreen() {
    const navigation = useNavigation();
    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(true);

    const fetchStatus = useCallback(() => {
        setLoading(true);
        apiClient.get('/api/v1/businesses/me/verification')
            .then(res => setStatus(res.data))
            .catch(() => Alert.alert('Error', 'Could not load verification status'))
            .finally(() => setLoading(false));
    }, []);

    useFocusEffect(useCallback(() => {
        fetchStatus();
    }, [fetchStatus]));

    if (loading) {
        return (
            <SafeAreaView style={styles.container}>
                <View style={styles.centered}>
                    <ActivityIndicator size="large" color="#007bff" />
                </View>
            </SafeAreaView>
        );
    }

    const config = status ? STATUS_CONFIG[status.requestStatus] ?? STATUS_CONFIG.PENDING : null;

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Verification Status</Text>
            </View>

            <ScrollView contentContainerStyle={styles.content}>
                {status && config ? (
                    <>
                        <View style={[styles.statusCard, { backgroundColor: config.bg, borderColor: config.color + '44' }]}>
                            <Ionicons name={config.icon} size={56} color={config.color} />
                            <Text style={[styles.statusTitle, { color: config.color }]}>{config.title}</Text>
                            <Text style={styles.statusDescription}>{config.description}</Text>
                        </View>

                        {status.requestStatus === 'REJECTED' && status.rejectionReason ? (
                            <View style={styles.rejectionBox}>
                                <Text style={styles.rejectionLabel}>Reason provided by administrator:</Text>
                                <Text style={styles.rejectionText}>{status.rejectionReason}</Text>
                            </View>
                        ) : null}

                        <View style={styles.infoCard}>
                            <Text style={styles.infoTitle}>Business Details</Text>
                            <InfoRow icon="business-outline" label="Company" value={status.companyName} />
                            <InfoRow icon="card-outline" label="Tax ID" value={status.taxId} />
                            {status.address ? <InfoRow icon="location-outline" label="Address" value={status.address} /> : null}
                            {status.website ? <InfoRow icon="globe-outline" label="Website" value={status.website} /> : null}
                            {status.verifiedAt ? (
                                <InfoRow
                                    icon="calendar-outline"
                                    label={status.requestStatus === 'APPROVED' ? 'Approved at' : 'Reviewed at'}
                                    value={new Date(status.verifiedAt).toLocaleDateString()}
                                />
                            ) : null}
                        </View>
                    </>
                ) : (
                    <View style={styles.centered}>
                        <Text style={styles.noDataText}>No verification data available</Text>
                    </View>
                )}
            </ScrollView>
        </SafeAreaView>
    );
}

function InfoRow({ icon, label, value }) {
    return (
        <View style={styles.infoRow}>
            <Ionicons name={icon} size={16} color="#666" style={styles.infoIcon} />
            <Text style={styles.infoLabel}>{label}:</Text>
            <Text style={styles.infoValue}>{value}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f8f9fa' },
    centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 16,
        paddingTop: 8,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    backBtn: { marginRight: 12 },
    headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#333' },
    content: { padding: 20, gap: 16 },
    statusCard: {
        borderRadius: 16,
        borderWidth: 1,
        padding: 24,
        alignItems: 'center',
        gap: 10,
    },
    statusTitle: { fontSize: 20, fontWeight: 'bold', textAlign: 'center' },
    statusDescription: { fontSize: 14, color: '#555', textAlign: 'center', lineHeight: 20 },
    rejectionBox: {
        backgroundColor: '#fff0f0',
        borderRadius: 12,
        padding: 16,
        borderLeftWidth: 4,
        borderLeftColor: '#ef4444',
    },
    rejectionLabel: { fontSize: 13, fontWeight: '700', color: '#ef4444', marginBottom: 4 },
    rejectionText: { fontSize: 14, color: '#333', lineHeight: 20 },
    infoCard: {
        backgroundColor: '#fff',
        borderRadius: 14,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.06,
        shadowRadius: 4,
        elevation: 2,
        gap: 10,
    },
    infoTitle: { fontSize: 15, fontWeight: 'bold', color: '#333', marginBottom: 4 },
    infoRow: { flexDirection: 'row', alignItems: 'center' },
    infoIcon: { marginRight: 6 },
    infoLabel: { fontSize: 13, color: '#666', marginRight: 4, fontWeight: '600' },
    infoValue: { fontSize: 13, color: '#333', flex: 1 },
    noDataText: { fontSize: 16, color: '#999' },
});
