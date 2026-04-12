import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, Alert, ScrollView, TouchableOpacity, Linking, Platform } from 'react-native';
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
    const [verification, setVerification] = useState(null);
    const [subscription, setSubscription] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isStartingCheckout, setIsStartingCheckout] = useState(false);
    const [subscriptionActionError, setSubscriptionActionError] = useState('');

    const fetchStatus = useCallback(() => {
        setLoading(true);
        Promise.all([
            apiClient.get('/api/v1/businesses/me/verification'),
            apiClient.get('/api/v1/business-subscriptions/me')
        ])
            .then(([verificationRes, subscriptionRes]) => {
                setVerification(verificationRes?.data || null);
                setSubscription(subscriptionRes?.data || null);
            })
            .catch(() => Alert.alert('Error', 'Could not load verification status'))
            .finally(() => setLoading(false));
    }, []);

    useFocusEffect(useCallback(() => {
        fetchStatus();
    }, [fetchStatus]));

    const startBusinessStripeCheckout = useCallback(async () => {
        setSubscriptionActionError('');
        setIsStartingCheckout(true);

        try {
            const response = await apiClient.post('/api/v1/business-subscriptions/me/stripe/checkout-session', {});
            const checkoutUrl = response?.data?.checkoutUrl;

            if (!checkoutUrl) {
                setSubscriptionActionError('Stripe checkout session could not be initialized.');
                return;
            }

            if (Platform.OS === 'web' && typeof window !== 'undefined') {
                window.location.assign(checkoutUrl);
                return;
            }

            await Linking.openURL(checkoutUrl);
            setSubscriptionActionError('Complete the payment in Stripe and return to the app.');
        } catch (error) {
            const rawMessage = error?.response?.data?.message || error?.response?.data || error?.message;
            const message = typeof rawMessage === 'string' ? rawMessage : JSON.stringify(rawMessage);
            setSubscriptionActionError(message || 'Unable to start Stripe checkout.');
        } finally {
            setIsStartingCheckout(false);
        }
    }, []);

    if (loading) {
        return (
            <SafeAreaView style={styles.container}>
                <View style={styles.centered}>
                    <ActivityIndicator size="large" color="#007bff" />
                </View>
            </SafeAreaView>
        );
    }

    const config = verification ? STATUS_CONFIG[verification.requestStatus] ?? STATUS_CONFIG.PENDING : null;
    const isApproved = verification?.requestStatus === 'APPROVED';
    const hasPremiumAccess = Boolean(subscription?.premiumEligible);
    const subscriptionExpiresAt = subscription?.subscriptionExpiresAt
        ? new Date(subscription.subscriptionExpiresAt)
        : null;
    const subscriptionNotExpired = subscriptionExpiresAt && !Number.isNaN(subscriptionExpiresAt.getTime())
        ? subscriptionExpiresAt.getTime() > Date.now()
        : false;
    const canPayFromThisScreen = isApproved && !hasPremiumAccess;
    const activationButtonLabel = subscription?.subscriptionActive && !subscriptionNotExpired
        ? 'Renew with Stripe payment'
        : 'Activate with Stripe payment';

    const formatDateTime = (value) => {
        if (!value) {
            return 'N/A';
        }
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return 'N/A';
        }
        return parsed.toLocaleString();
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Verification Status</Text>
            </View>

            <ScrollView contentContainerStyle={styles.content}>
                {verification && config ? (
                    <>
                        <View style={[styles.statusCard, { backgroundColor: config.bg, borderColor: config.color + '44' }]}>
                            <Ionicons name={config.icon} size={56} color={config.color} />
                            <Text style={[styles.statusTitle, { color: config.color }]}>{config.title}</Text>
                            <Text style={styles.statusDescription}>{config.description}</Text>
                        </View>

                        {verification.requestStatus === 'REJECTED' && verification.rejectionReason ? (
                            <View style={styles.rejectionBox}>
                                <Text style={styles.rejectionLabel}>Reason provided by administrator:</Text>
                                <Text style={styles.rejectionText}>{verification.rejectionReason}</Text>
                            </View>
                        ) : null}

                        {subscription ? (
                            <View style={styles.infoCard}>
                                <Text style={styles.infoTitle}>Business Subscription</Text>
                                <InfoRow icon="shield-checkmark-outline" label="Verification" value={verification.requestStatus} />
                                <InfoRow icon="checkmark-circle-outline" label="Subscription active" value={subscription.subscriptionActive ? 'Yes' : 'No'} />
                                <InfoRow icon="flash-outline" label="Premium access" value={subscription.premiumEligible ? 'Enabled' : 'Disabled'} />
                                <InfoRow icon="calendar-outline" label="Expires at" value={formatDateTime(subscription.subscriptionExpiresAt)} />

                                {verification.requestStatus === 'PENDING' ? (
                                    <Text style={styles.warningText}>
                                        Your premium access will be activated once your business is approved.
                                    </Text>
                                ) : null}

                                {verification.requestStatus === 'REJECTED' ? (
                                    <Text style={styles.errorText}>
                                        Payment is disabled while your verification status is rejected.
                                    </Text>
                                ) : null}

                                {verification.requestStatus === 'APPROVED' && hasPremiumAccess ? (
                                    <Text style={styles.successText}>
                                        Your premium access is active. No payment is required right now.
                                    </Text>
                                ) : null}

                                {canPayFromThisScreen ? (
                                    <TouchableOpacity
                                        style={[styles.subscriptionActionBtn, isStartingCheckout && styles.disabledButton]}
                                        onPress={startBusinessStripeCheckout}
                                        disabled={isStartingCheckout}
                                        activeOpacity={0.8}
                                    >
                                        <Text style={styles.subscriptionActionBtnText}>
                                            {isStartingCheckout ? 'Redirecting to Stripe...' : activationButtonLabel}
                                        </Text>
                                    </TouchableOpacity>
                                ) : null}

                                {subscriptionActionError ? (
                                    <Text style={styles.errorText}>{subscriptionActionError}</Text>
                                ) : null}
                            </View>
                        ) : null}

                        <View style={styles.infoCard}>
                            <Text style={styles.infoTitle}>Business Details</Text>
                            <InfoRow icon="business-outline" label="Company" value={verification.companyName} />
                            <InfoRow icon="card-outline" label="Tax ID" value={verification.taxId} />
                            {verification.address ? <InfoRow icon="location-outline" label="Address" value={verification.address} /> : null}
                            {verification.website ? <InfoRow icon="globe-outline" label="Website" value={verification.website} /> : null}
                            {verification.verifiedAt ? (
                                <InfoRow
                                    icon="calendar-outline"
                                    label={verification.requestStatus === 'APPROVED' ? 'Approved at' : 'Reviewed at'}
                                    value={new Date(verification.verifiedAt).toLocaleDateString()}
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
    subscriptionActionBtn: {
        marginTop: 10,
        backgroundColor: '#c2410c',
        borderRadius: 8,
        paddingVertical: 10,
        alignItems: 'center',
    },
    subscriptionActionBtnText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 13,
    },
    disabledButton: {
        opacity: 0.7,
    },
    successText: {
        marginTop: 10,
        color: '#166534',
        fontSize: 12,
        fontWeight: '600',
    },
    warningText: {
        marginTop: 10,
        color: '#9a3412',
        fontSize: 12,
        fontWeight: '600',
    },
    errorText: {
        marginTop: 10,
        color: '#b91c1c',
        fontSize: 12,
        fontWeight: '600',
    },
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
