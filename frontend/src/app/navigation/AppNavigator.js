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
import Toast from 'react-native-toast-message';

const Stack = createNativeStackNavigator();

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

const normalizeReturnPath = (rawPath) => {
    if (Platform.OS !== 'web' || typeof window === 'undefined') {
        return null;
    }

    if (typeof rawPath !== 'string') {
        return null;
    }

    const trimmedPath = rawPath.trim();
    if (!trimmedPath) {
        return null;
    }

    try {
        const parsed = new URL(trimmedPath, window.location.origin);
        if (parsed.origin !== window.location.origin) {
            return null;
        }

        return `${parsed.pathname || '/'}${parsed.search || ''}${parsed.hash || ''}`;
    } catch {
        return null;
    }
};

export default function AppNavigator() {
    const { isAuthenticated, isLoadingAuth, user, login } = useAuth();
    const stripeCallbackHandledRef = useRef(false);
    const [navigationResetVersion, setNavigationResetVersion] = useState(0);

    useEffect(() => {
        if (Platform.OS !== 'web' || typeof window === 'undefined') return;

        const rawNotice = window.localStorage.getItem(STORAGE_KEYS.CHECKOUT_CALLBACK_NOTICE);
        if (!rawNotice) return;

        try {
            const parsedNotice = JSON.parse(rawNotice);
            const type = parsedNotice?.type === 'error' ? 'error' : 'success';
            const text1 = typeof parsedNotice?.text1 === 'string' ? parsedNotice.text1.trim() : '';
            const text2 = typeof parsedNotice?.text2 === 'string' ? parsedNotice.text2.trim() : '';

            if (text1) {
                Toast.show({
                    type,
                    text1,
                    text2,
                    visibilityTime: 4500,
                    autoHide: true,
                    topOffset: 16,
                });
            }
        } catch (error) {
            console.warn('Invalid checkout callback notice format.', error);
        } finally {
            window.localStorage.removeItem(STORAGE_KEYS.CHECKOUT_CALLBACK_NOTICE);
        }
    }, [navigationResetVersion]);

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
        const mapUrl = window.location.origin;

        const clearUrlParams = () => {
            window.history.replaceState({}, document.title, cleanUrl);
        };

        const queueCheckoutNotice = (type, text1, text2 = '') => {
            window.localStorage.setItem(
                STORAGE_KEYS.CHECKOUT_CALLBACK_NOTICE,
                JSON.stringify({
                    type,
                    text1,
                    text2,
                    createdAt: Date.now(),
                })
            );
        };

        const processStripeCallback = async () => {
            let callbackSucceeded = false;
            let shouldClearParams = true;
            let shouldClearStreetCoinsPending = true;
            let shouldClearBusinessSignupPending = false;
            let shouldClearBusinessSubscriptionPending = flow !== 'streetcoins';
            let shouldClearRegularPremiumPending = flow !== 'streetcoins';
            let redirectAfterCallback = null;

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

            const pendingReturnTo = flow === 'streetcoins'
                ? pendingStreetCoins?.returnTo
                : pendingBusinessSubscription?.returnTo
                || pendingRegularPremium?.returnTo
                || pendingBusinessSignup?.returnTo;
            const fallbackReturnTo = normalizeReturnPath(
                pendingReturnTo
                || pendingStreetCoins?.returnTo
                || pendingBusinessSubscription?.returnTo
                || pendingRegularPremium?.returnTo
                || pendingBusinessSignup?.returnTo
            );

            try {
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

                    const hasBusinessRole = Array.isArray(user?.roles) && user.roles.includes('BUSINESS');
                    const isBusinessSignupCallback = flow === 'business-signup'
                        || (!flow && pendingBusinessSignup?.email && pendingBusinessSignup?.taxId);
                    const isBusinessSubscriptionCallback = flow === 'business-subscription'
                        || (!flow && !isBusinessSignupCallback && (pendingBusinessSubscription || hasBusinessRole));
                    const isRegularPremiumCallback = flow === 'regular-premium'
                        || (!flow && !isBusinessSignupCallback && !isBusinessSubscriptionCallback
                            && pendingRegularPremium && isAuthenticated);

                    if (isBusinessSignupCallback) {
                        if (!pendingBusinessSignup?.email || !pendingBusinessSignup?.taxId) {
                            throw new Error('Missing pending business signup data for checkout callback.');
                        }

                        await apiClient.post(
                            '/api/v1/business-subscriptions/stripe/confirm-session',
                            {
                                email: pendingBusinessSignup.email,
                                taxId: pendingBusinessSignup.taxId,
                                sessionId: effectiveSessionId,
                            }
                        );

                        const pendingBusinessPassword = typeof pendingBusinessSignup?.password === 'string'
                            ? pendingBusinessSignup.password.trim()
                            : '';

                        if (!isAuthenticated && pendingBusinessPassword) {
                            const signInResponse = await apiClient.post('/api/v1/auth/signin', {
                                email: pendingBusinessSignup.email,
                                password: pendingBusinessPassword,
                            });

                            await login(signInResponse?.data?.token, {
                                id: signInResponse?.data?.id,
                                username: signInResponse?.data?.username,
                                roles: signInResponse?.data?.roles,
                            });
                        }

                        shouldClearBusinessSignupPending = true;
                        callbackSucceeded = true;
                    } else if (isBusinessSubscriptionCallback) {
                        await apiClient.post(
                            '/api/v1/business-subscriptions/me/stripe/confirm-session',
                            { sessionId: effectiveSessionId }
                        );
                        callbackSucceeded = true;
                    } else if (isRegularPremiumCallback) {
                        await apiClient.post(
                            '/api/v1/users/me/premium/stripe/confirm-session',
                            { sessionId: effectiveSessionId }
                        );
                        callbackSucceeded = true;
                    }

                    if (!callbackSucceeded) {
                        throw new Error('Stripe callback could not resolve a business/premium flow.');
                    }

                    const successDescription = pendingBusinessSignup?.email
                        ? 'Business account activated successfully.'
                        : (pendingBusinessSubscription || (Array.isArray(user?.roles) && user.roles.includes('BUSINESS')))
                            ? 'Business premium subscription activated.'
                            : 'Premium plan activated successfully.';

                    queueCheckoutNotice('success', 'Payment completed', successDescription);
                    redirectAfterCallback = mapUrl;
                    return;
                }

                const failureTitle = paymentState === 'cancel'
                    ? 'Payment canceled'
                    : 'Payment failed';
                queueCheckoutNotice(
                    'error',
                    failureTitle,
                    'You were returned to the previous screen.'
                );
                redirectAfterCallback = fallbackReturnTo;
            } catch (error) {
                console.error('Stripe callback processing failed:', error);

                if (flow === 'streetcoins') {
                    stripeCallbackHandledRef.current = false;
                    shouldClearParams = false;
                    shouldClearStreetCoinsPending = false;
                    return;
                }

                queueCheckoutNotice(
                    'error',
                    'Payment confirmation failed',
                    'We returned you to the previous screen. Please try again.'
                );
                redirectAfterCallback = fallbackReturnTo;
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

                if (callbackSucceeded && flow !== 'streetcoins') {
                    window.location.replace(mapUrl);
                } else if (redirectAfterCallback) {
                    window.location.replace(redirectAfterCallback);
                }
            }
        };

        processStripeCallback();
    }, [isAuthenticated, isLoadingAuth, login, user?.roles]);

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
