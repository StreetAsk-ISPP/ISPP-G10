import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image, Linking, Platform } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../app/providers/AuthProvider';
import ConfirmationModal from '../../shared/components/ConfirmationModal';
import apiClient from '../../shared/services/http/apiClient';

export default function ProfileScreen({ navigation }) {
    const { user, logout } = useAuth();

    const [stats, setStats] = useState({
        questions: 0,
        answers: 0,
        rating: 0,
        bio: '',
        profilePictureUrl: null
    });
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const [businessSubscription, setBusinessSubscription] = useState(null);
    const [isStartingCheckout, setIsStartingCheckout] = useState(false);
    const [subscriptionActionError, setSubscriptionActionError] = useState('');

    const isBusinessUser = Array.isArray(user?.roles) && user.roles.includes('BUSINESS');

    const subscriptionExpiresAt = businessSubscription?.subscriptionExpiresAt
        ? new Date(businessSubscription.subscriptionExpiresAt)
        : null;
    const subscriptionNotExpired = subscriptionExpiresAt && !Number.isNaN(subscriptionExpiresAt.getTime())
        ? subscriptionExpiresAt.getTime() > Date.now()
        : false;
    const hasPremiumAccess = Boolean(businessSubscription?.premiumEligible);
    const isVerifiedBusiness = Boolean(businessSubscription?.verified);
    const canActivateOrRenew = isVerifiedBusiness && !hasPremiumAccess;

    const activationButtonLabel = businessSubscription?.subscriptionActive && !subscriptionNotExpired
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

    const handleLogoutConfirm = async () => {
        setShowLogoutModal(false);
        await logout();
    };

    const startBusinessStripeCheckout = async () => {
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
    };


    // Función para cargar los datos (se ejecuta al entrar en la pantalla)
    const loadProfileData = useCallback(() => {
        if (!user?.id) return;

        apiClient.get(`/api/v1/users/${user.id}/stats`)
            .then(res => {
                if (res.data) {
                    setStats({
                        questions: res.data.questionsCount || 0,
                        answers: res.data.answersCount || 0,
                        rating: res.data.rating != null ? res.data.rating : 0,
                        bio: res.data.bio || '',
                        profilePictureUrl: res.data.profilePictureUrl || null
                    });
                }
            })
            .catch((err) => {
                console.error("Error al refrescar el perfil:", err);
            });

        if (isBusinessUser) {
            apiClient.get('/api/v1/business-subscriptions/me')
                .then((res) => {
                    setBusinessSubscription(res?.data || null);
                })
                .catch(() => {
                    setBusinessSubscription(null);
                });
        } else {
            setBusinessSubscription(null);
        }
    }, [isBusinessUser, user?.id]);

    // Este hook se dispara cada vez que la pantalla gana el "foco" (al volver atrás)
    useFocusEffect(
        useCallback(() => {
            loadProfileData();
        }, [loadProfileData])
    );

    return (
        <SafeAreaView style={styles.screen}>
            <View style={styles.headerRed}>
                <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                    <Ionicons name="arrow-back" size={24} color="#fff" />
                </TouchableOpacity>

                <View style={styles.userInfoRow}>
                    <View style={styles.avatarCircle}>
                        {/* Renderizado condicional de la imagen de perfil */}
                        {stats.profilePictureUrl ? (
                            <Image
                                source={{ uri: stats.profilePictureUrl }}
                                style={styles.avatarImage}
                            />
                        ) : (
                            <Ionicons name="person" size={60} color="#666" />
                        )}
                    </View>
                    <View style={styles.userTextInfo}>
                        <Text style={styles.userName}>{user?.username?.toUpperCase() || 'USER NAME'}</Text>
                        <Text style={styles.userSubRole}>{user?.role || 'Registered User'}</Text>

                        {/* Bloque de Biografía - Ahora se actualiza solo */}
                        {stats.bio ? (
                            <Text style={styles.bioText} numberOfLines={3}>{stats.bio}</Text>
                        ) : (
                            <Text style={styles.noBioText}>No bio available</Text>
                        )}

                        <Text style={styles.userEmail}>{user?.email}</Text>
                    </View>
                </View>
            </View>

            {/* Stats card — floats above menu */}
            <View style={styles.statsBar}>
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>{stats.questions}</Text>
                    <Text style={styles.statItemLabel}>Asked{"\n"}questions</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>{stats.answers}</Text>
                    <Text style={styles.statItemLabel}>Answered{"\n"}questions</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>
                        {parseFloat(stats.rating).toFixed(1)}
                        <Text style={styles.statNumberSub}>/5</Text>
                    </Text>
                    <Text style={styles.statItemLabel}>Rated{"\n"}with</Text>
                </View>
            </View>

            <ScrollView style={styles.menuContainer}>
                <TouchableOpacity style={styles.editBtn}
                    onPress={() => navigation.navigate('EditProfile')}>
                    <Text style={styles.editBtnText}>EDIT PROFILE</Text>
                </TouchableOpacity>

                {isBusinessUser && businessSubscription ? (
                    <View style={styles.subscriptionCard}>
                        <Text style={styles.subscriptionTitle}>Business Subscription</Text>
                        <Text style={styles.subscriptionItem}>Verified: {businessSubscription.verified ? 'Yes' : 'No'}</Text>
                        <Text style={styles.subscriptionItem}>Active: {businessSubscription.subscriptionActive ? 'Yes' : 'No'}</Text>
                        <Text style={styles.subscriptionItem}>Premium access: {businessSubscription.premiumEligible ? 'Enabled' : 'Disabled'}</Text>
                        <Text style={styles.subscriptionItem}>Expires at: {formatDateTime(businessSubscription.subscriptionExpiresAt)}</Text>

                        {!isVerifiedBusiness ? (
                            <Text style={styles.subscriptionHint}>
                                Your business is pending admin verification.
                            </Text>
                        ) : null}

                        {hasPremiumAccess ? (
                            <Text style={styles.subscriptionHintSuccess}>
                                Your premium access is active. No additional payment is required right now.
                            </Text>
                        ) : null}

                        {canActivateOrRenew ? (
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
                            <Text style={styles.subscriptionHint}>{subscriptionActionError}</Text>
                        ) : null}
                    </View>
                ) : null}

                <TouchableOpacity
                    style={styles.menuItem}
                    onPress={() => navigation.navigate('ProfileStats')}>
                    <Ionicons name="stats-chart-outline" size={24} color="#fff" />
                    <Text style={styles.menuItemText}>Insights</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={styles.menuItem}
                    onPress={() => navigation.navigate('Balance')}>
                    <Ionicons name="wallet-outline" size={24} color="#fff" />
                    <Text style={styles.menuItemText}>Balance</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={styles.menuItem}
                    onPress={() => navigation.navigate('MyPurchases')}>
                    <Ionicons name="cart-outline" size={24} color="#fff" />
                    <Text style={styles.menuItemText}>My purchases</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={styles.menuItem}
                    onPress={() => navigation.navigate('Settings')}>
                    <Ionicons name="settings-outline" size={24} color="#fff" />
                    <Text style={styles.menuItemText}>Settings</Text>
                </TouchableOpacity>

                {user?.roles?.includes('BUSINESS') && (
                    <TouchableOpacity
                        style={[styles.menuItem, { backgroundColor: '#1e40af' }]}
                        onPress={() => navigation.navigate('BusinessVerificationStatus')}>
                        <Ionicons name="shield-checkmark-outline" size={24} color="#fff" />
                        <Text style={styles.menuItemText}>Verification Status</Text>
                    </TouchableOpacity>
                )}

                <TouchableOpacity
                    style={[styles.menuItem, { backgroundColor: '#4b5563', marginTop: 10 }]}
                    onPress={() => setShowLogoutModal(true)}>
                    <Ionicons name="log-out-outline" size={24} color="#fff" />
                    <Text style={styles.menuItemText}>Log out</Text>
                </TouchableOpacity>


                <TouchableOpacity style={[styles.menuItem, { marginTop: 20, backgroundColor: '#fee2e2' }]}>
                    <Ionicons name="trash-outline" size={24} color="#ef4444" />
                    <Text style={[styles.menuItemText, { color: '#ef4444' }]}>Delete my account</Text>
                </TouchableOpacity>
            </ScrollView>
            <ConfirmationModal
                visible={showLogoutModal}
                title="Log out?"
                message="Are you sure you want to log out?"
                confirmText="Log out"
                cancelText="Go Back"
                onConfirm={handleLogoutConfirm}
                onCancel={() => setShowLogoutModal(false)}
                confirmButtonColor="danger"
            />        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: { flex: 1, backgroundColor: '#fff' },
    headerRed: {
        backgroundColor: '#d90429',
        padding: 20,
        paddingTop: 40,
        paddingBottom: 45,
    },
    backBtn: { marginBottom: 10 },
    userInfoRow: { flexDirection: 'row', alignItems: 'flex-start' },
    avatarCircle: {
        width: 90,
        height: 90,
        borderRadius: 45,
        backgroundColor: '#fff',
        justifyContent: 'center',
        alignItems: 'center',
        overflow: 'hidden'
    },
    avatarImage: {
        width: '100%',
        height: '100%',
        resizeMode: 'cover'
    },
    userTextInfo: { marginLeft: 20, flex: 1 },
    userName: { color: '#fff', fontSize: 20, fontWeight: 'bold' },
    userSubRole: { color: '#fff', fontSize: 14, fontWeight: '600', marginTop: 2 },
    bioText: { color: '#fff', fontSize: 13, marginTop: 6, opacity: 0.95, lineHeight: 18 },
    noBioText: { color: '#fff', fontSize: 12, marginTop: 6, opacity: 0.6, fontStyle: 'italic' },
    userEmail: { color: '#fff', fontSize: 12, marginTop: 8, opacity: 0.8 },
    menuContainer: { padding: 20, paddingTop: 8 },
    editBtn: {
        backgroundColor: '#d90429',
        padding: 15,
        borderRadius: 8,
        alignItems: 'center',
        marginBottom: 25
    },
    editBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 18 },
    subscriptionCard: {
        backgroundColor: '#fff7ed',
        borderColor: '#fdba74',
        borderWidth: 1,
        borderRadius: 10,
        padding: 14,
        marginBottom: 16,
    },
    subscriptionTitle: {
        color: '#9a3412',
        fontWeight: '700',
        fontSize: 16,
        marginBottom: 8,
    },
    subscriptionItem: {
        color: '#7c2d12',
        fontSize: 13,
        marginBottom: 4,
    },
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
    subscriptionHint: {
        marginTop: 10,
        color: '#9a3412',
        fontSize: 12,
        fontWeight: '600',
    },
    subscriptionHintSuccess: {
        marginTop: 10,
        color: '#166534',
        fontSize: 12,
        fontWeight: '600',
    },
    menuItem: {
        backgroundColor: '#bcbcbc',
        flexDirection: 'row',
        alignItems: 'center',
        padding: 15,
        borderRadius: 8,
        marginBottom: 10
    },
    menuItemText: {
        marginLeft: 15,
        fontSize: 16,
        fontWeight: '600',
        color: '#fff'
    },
    statsBar: {
        flexDirection: 'row',
        backgroundColor: '#fff',
        borderRadius: 14,
        marginHorizontal: 16,
        marginTop: -28,
        paddingVertical: 14,
        elevation: 6,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.12,
        shadowRadius: 6,
        zIndex: 10,
    },
    statItem: { flex: 1, alignItems: 'center' },
    statNumber: { fontSize: 22, fontWeight: 'bold', color: '#d90429' },
    statNumberSub: { fontSize: 14, fontWeight: '600', color: '#d90429' },
    statItemLabel: { fontSize: 11, color: '#666', textAlign: 'center', marginTop: 2 },
    statDivider: { width: 1, backgroundColor: '#e5e7eb', marginVertical: 4 },
});