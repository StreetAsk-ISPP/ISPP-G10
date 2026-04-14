import React, { useEffect, useRef, useState } from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ActivityIndicator, Platform, View } from 'react-native';

import LoginScreen from '../../features/auth/ui/LoginScreen';
import SignUpScreen from '../../features/auth/ui/SignUpScreen';
import BusinessSignupScreen from '../../features/auth/ui/BusinessSignupScreen';
import ForgotPasswordScreen from '../../features/auth/ui/ForgotPasswordScreen';
import ResetPasswordScreen from '../../features/auth/ui/ResetPasswordScreen';
import HomeScreen from '../../features/home/ui/HomeScreen';
import CreateQuestionScreen from '../../features/questions/ui/CreateQuestionScreen';
import ManageEventsScreen from '../../features/events/ui/ManageEventsScreen';
import MyAttendancesScreen from '../../features/events/ui/MyAttendancesScreen';
import SubscriptionPlansScreen from '../../features/subscriptions/ui/SubscriptionPlansScreen';
import QuestionThreadScreen from '../../features/answers/ui/QuestionThreadScreen';
import ProfileScreen from '../../features/profile/ProfileScreen';
import ProfileStats from '../../features/profile/ProfileStats';
import AdminFeedbackScreen from '../../features/admin/ui/AdminFeedbackScreen';
import AdminScreen from '../../features/admin/ui/AdminScreen';
import AdminUsersScreen from '../../features/admin/ui/AdminUsersScreen';
import AdminBusinessVerificationScreen from '../../features/admin/ui/AdminBusinessVerificationScreen';
import BusinessVerificationStatusScreen from '../../features/business/ui/BusinessVerificationStatusScreen';
import EditProfileScreen from '../../features/profile/EditProfileScreen';
import BalanceScreen from '../../features/profile/BalanceScreen';
import MyPurchasesScreen from '../../features/profile/MyPurchasesScreen';
import EventDetailsScreen from '../../features/events/EventDetailsScreen';
import { useAuth } from '../providers/AuthProvider';
import { theme } from '../../shared/ui/theme/theme';
import apiClient from '../../shared/services/http/apiClient';
import { STORAGE_KEYS } from '../../shared/constants/storageKeys';

const Stack = createNativeStackNavigator();

export default function AppNavigator() {
    const { isAuthenticated, isLoadingAuth, user } = useAuth();
    const stripeCallbackHandledRef = useRef(false);
    const [navigationResetVersion, setNavigationResetVersion] = useState(0);

    useEffect(() => {
        if (isLoadingAuth) return;

        if (Platform.OS !== 'web' || typeof window === 'undefined') return;

        const params = new URLSearchParams(window.location.search);
        const paymentState = params.get('payment');
        const sessionId = params.get('session_id');
        const flow = params.get('flow');

        if (!paymentState || stripeCallbackHandledRef.current) return;

        if (flow === 'streetcoins' && !isAuthenticated) return;

        stripeCallbackHandledRef.current = true;

        const cleanUrl = `${window.location.pathname}${window.location.hash || ''}`;

        const clearUrlParams = () => {
            window.history.replaceState({}, document.title, cleanUrl);
        };

        const parsePendingCheckout = (rawValue) => {
            if (!rawValue) {
                return null;
            }

            try {
                return JSON.parse(rawValue);
            } catch {
                return null;
            }
        };

        const processStripeCallback = async () => {
            let callbackSucceeded = false;
            let shouldClearParams = true;
            let shouldClearStreetCoinsPending = true;
            let shouldClearBusinessSignupPending = flow !== 'streetcoins';
            let shouldClearBusinessSubscriptionPending = flow !== 'streetcoins';
            let shouldClearRegularPremiumPending = flow !== 'streetcoins';

            try {
                // =========================
                // STREETCOINS FLOW (feature/buy-streetcoins)
                // =========================
                const pendingStreetCoins = parsePendingCheckout(window.localStorage.getItem(
                    STORAGE_KEYS.PENDING_STREETCOINS_CHECKOUT
                ));

                const pendingBusinessSignup = parsePendingCheckout(window.localStorage.getItem(
                    STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT
                ));

                const pendingBusinessSubscription = parsePendingCheckout(window.localStorage.getItem(
                    STORAGE_KEYS.PENDING_BUSINESS_SUBSCRIPTION_CHECKOUT
                ));

                const rawPendingRegularPremium = window.localStorage.getItem(
                    STORAGE_KEYS.PENDING_REGULAR_PREMIUM_CHECKOUT
                );
                const pendingRegularPremium = parsePendingCheckout(rawPendingRegularPremium)
                    || (rawPendingRegularPremium ? { legacyFlag: true } : null);

                if (paymentState === 'success') {
                    if (flow === 'streetcoins') {
                        const effectiveStreetCoinsSessionId = sessionId || pendingStreetCoins?.sessionId;
                        if (!effectiveStreetCoinsSessionId) {
                            throw new Error('Missing Stripe session ID for StreetCoins checkout callback.');
                        }

                        const response = await apiClient.post(
                            '/api/v1/streetcoins/purchase/confirm',
                            { sessionId: effectiveStreetCoinsSessionId }
                        );

                        const addedStreetCoins = response?.data?.addedStreetCoins;

                        if (typeof addedStreetCoins === 'number' && addedStreetCoins > 0) {
                            window.localStorage.setItem(
                                STORAGE_KEYS.STREETCOINS_SUCCESS_NOTICE,
                                JSON.stringify({ addedStreetCoins })
                            );

                            window.dispatchEvent(
                                new CustomEvent('streetcoins:purchase-confirmed', {
                                    detail: { addedStreetCoins },
                                })
                            );
                        }

                        if (pendingStreetCoins?.checkoutOrigin === 'create-question-limit') {
                            window.localStorage.setItem(
                                STORAGE_KEYS.STREETCOINS_POST_CHECKOUT_TARGET,
                                'balance'
                            );
                        }

                        callbackSucceeded = true;
                        setNavigationResetVersion((v) => v + 1);
                        window.location.replace(cleanUrl);
                        return;
                    }

                    const effectiveSessionId = sessionId
                        || pendingBusinessSubscription?.sessionId
                        || pendingRegularPremium?.sessionId
                        || pendingBusinessSignup?.sessionId;

                    if (!effectiveSessionId) {
                        throw new Error('Missing Stripe session ID for checkout callback.');
                    }

                    // =========================
                    // BUSINESS FLOW (feature/buy-streetcoins + trunk)
                    // =========================
                    if (Array.isArray(user?.roles) && user.roles.includes('BUSINESS')) {
                        await apiClient.post(
                            '/api/v1/business-subscriptions/me/stripe/confirm-session',
                            { sessionId: effectiveSessionId }
                        );
                        callbackSucceeded = true;
                    } else {
                        // =========================
                        // PREMIUM USER (trunk)
                        // =========================
                        if (pendingRegularPremium && isAuthenticated) {
                            await apiClient.post(
                                '/api/v1/users/me/premium/stripe/confirm-session',
                                { sessionId: effectiveSessionId }
                            );
                            callbackSucceeded = true;
                        } else {
                            if (pendingBusinessSignup?.email && pendingBusinessSignup?.taxId) {
                                await apiClient.post(
                                    '/api/v1/business-subscriptions/stripe/confirm-session',
                                    {
                                        email: pendingBusinessSignup.email,
                                        taxId: pendingBusinessSignup.taxId,
                                        sessionId: effectiveSessionId,
                                    }
                                );
                                callbackSucceeded = true;
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Stripe callback processing failed:', error);

                stripeCallbackHandledRef.current = false;
                shouldClearParams = false;

                if (flow === 'streetcoins') {
                    shouldClearStreetCoinsPending = false;
                } else {
                    shouldClearBusinessSignupPending = false;
                    shouldClearBusinessSubscriptionPending = false;
                    shouldClearRegularPremiumPending = false;
                }
            } finally {
                if (shouldClearBusinessSignupPending) {
                    window.localStorage.removeItem(STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT);
                }

                if (shouldClearRegularPremiumPending) {
                    window.localStorage.removeItem(
                        STORAGE_KEYS.PENDING_REGULAR_PREMIUM_CHECKOUT
                    );
                }

                if (shouldClearBusinessSubscriptionPending) {
                    window.localStorage.removeItem(
                        STORAGE_KEYS.PENDING_BUSINESS_SUBSCRIPTION_CHECKOUT
                    );
                }

                if (shouldClearStreetCoinsPending) {
                    window.localStorage.removeItem(
                        STORAGE_KEYS.PENDING_STREETCOINS_CHECKOUT
                    );
                }

                if (shouldClearParams) {
                    clearUrlParams();
                }

                // Comportamiento de trunk: refrescar UI después de éxito (excepto streetcoins)
                if (callbackSucceeded && flow !== 'streetcoins') {
                    window.location.reload();
                }
            }
        };

        processStripeCallback();
    }, [isAuthenticated, isLoadingAuth, user?.roles]);

    if (isLoadingAuth) {
        return (
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.colors?.background }}>
                <ActivityIndicator size="large" color={theme.colors?.primary || '#0000ff'} />
            </View>
        );
    }

    const isAdmin = user?.roles?.includes('ADMIN');

    return (
        <Stack.Navigator
            key={`app-stack-${isAuthenticated ? 'auth' : 'guest'}-${navigationResetVersion}`}
            screenOptions={{ headerShown: false }}
        >
            {!isAuthenticated ? (
                <>
                    <Stack.Screen name="Login" component={LoginScreen} />
                    <Stack.Screen name="SignUp" component={SignUpScreen} />
                    <Stack.Screen name="BusinessSignup" component={BusinessSignupScreen} />
                    <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
                    <Stack.Screen name="ResetPassword" component={ResetPasswordScreen} />
                </>
            ) : (
                <>
                    {
                        isAdmin ? (
                            <>
                                <Stack.Screen name="AdminDashboard" component={AdminScreen} />
                                <Stack.Screen name="AdminUsers" component={AdminUsersScreen} />
                                <Stack.Screen name="AdminFeedback" component={AdminFeedbackScreen} />
                                <Stack.Screen name="AdminBusinessVerification" component={AdminBusinessVerificationScreen} />
                                <Stack.Screen name="Home" component={HomeScreen} />
                                <Stack.Screen name="SubscriptionPlans" component={SubscriptionPlansScreen} />
                                <Stack.Screen name="MyAttendances" component={MyAttendancesScreen} />
                                <Stack.Screen name="CreateQuestion" component={CreateQuestionScreen} />
                                <Stack.Screen name="ManageEvents" component={ManageEventsScreen} />
                                <Stack.Screen name="QuestionThread" component={QuestionThreadScreen} />
                                <Stack.Screen name="Profile" component={ProfileScreen} />
                                <Stack.Screen name="ProfileStats" component={ProfileStats} options={{ headerShown: false }} />
                                <Stack.Screen name="EditProfile" component={EditProfileScreen} />
                            </>
                        ) : (
                            <>
                                <Stack.Screen name="Home" component={HomeScreen} />
                                <Stack.Screen name="SubscriptionPlans" component={SubscriptionPlansScreen} />
                                <Stack.Screen name="MyAttendances" component={MyAttendancesScreen} />
                                <Stack.Screen name="CreateQuestion" component={CreateQuestionScreen} />
                                <Stack.Screen name="ManageEvents" component={ManageEventsScreen} />
                                <Stack.Screen name="QuestionThread" component={QuestionThreadScreen} />
                                <Stack.Screen name="Profile" component={ProfileScreen} />
                                <Stack.Screen name="ProfileStats" component={ProfileStats} options={{ headerShown: false }} />
                                <Stack.Screen name="EditProfile" component={EditProfileScreen} />
                                <Stack.Screen name="Balance" component={BalanceScreen} options={{ headerShown: false }} />
                                <Stack.Screen name="MyPurchases" component={MyPurchasesScreen} options={{ headerShown: false }} />
                                <Stack.Screen name="BusinessVerificationStatus" component={BusinessVerificationStatusScreen} options={{ headerShown: false }} />
                            </>
                        )
                    }
                    <Stack.Screen
                        name="EventDetails"
                        component={EventDetailsScreen}
                        options={{
                            presentation: 'transparentModal',
                            animation: 'fade',
                            contentStyle: { backgroundColor: 'transparent' },
                        }}
                    />
                </>
            )}

        </Stack.Navigator>
    );
}
