import React, { useEffect, useRef } from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ActivityIndicator, Platform, View } from 'react-native';

import LoginScreen from '../../features/auth/ui/LoginScreen';
import SignUpScreen from '../../features/auth/ui/SignUpScreen';
import BusinessSignupScreen from '../../features/auth/ui/BusinessSignupScreen';
import ForgotPasswordScreen from '../../features/auth/ui/ForgotPasswordScreen';
import ResetPasswordScreen from '../../features/auth/ui/ResetPasswordScreen';
import HomeScreen from '../../features/home/ui/HomeScreen';
import CreateQuestionScreen from '../../features/questions/ui/CreateQuestionScreen';
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
import SettingsScreen from '../../features/profile/SettingsScreen';
import EventListScreen from '../../features/events/EventListScreen';
import EventDetailsScreen from '../../features/events/EventDetailsScreen';
import { useAuth } from '../providers/AuthProvider';
import { theme } from '../../shared/ui/theme/theme';
import apiClient from '../../shared/services/http/apiClient';
import { STORAGE_KEYS } from '../../shared/constants/storageKeys';

const Stack = createNativeStackNavigator();

export default function AppNavigator() {
    const { isAuthenticated, isLoadingAuth, user } = useAuth();
    const stripeCallbackHandledRef = useRef(false);

    useEffect(() => {
        if (Platform.OS !== 'web' || typeof window === 'undefined') {
            return;
        }

        const params = new URLSearchParams(window.location.search);
        const paymentState = params.get('payment');
        const sessionId = params.get('session_id');

        if (!paymentState || stripeCallbackHandledRef.current) {
            return;
        }

        stripeCallbackHandledRef.current = true;

        const clearUrlParams = () => {
            window.history.replaceState({}, document.title, window.location.pathname);
        };

        const processStripeCallback = async () => {
            try {
                if (paymentState === 'success' && sessionId) {
                    if (Array.isArray(user?.roles) && user.roles.includes('BUSINESS')) {
                        await apiClient.post('/api/v1/business-subscriptions/me/stripe/confirm-session', { sessionId });
                    } else {
                        const rawPendingData = window.localStorage.getItem(STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT);
                        if (rawPendingData) {
                            const pendingData = JSON.parse(rawPendingData);
                            if (pendingData?.email && pendingData?.taxId) {
                                await apiClient.post('/api/v1/business-subscriptions/stripe/confirm-session', {
                                    email: pendingData.email,
                                    taxId: pendingData.taxId,
                                    sessionId,
                                });
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Stripe callback processing failed:', error);
            } finally {
                window.localStorage.removeItem(STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT);
                clearUrlParams();
            }
        };

        processStripeCallback();
    }, [user?.roles]);

    if (isLoadingAuth) {
        return (
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.colors?.background }}>
                <ActivityIndicator size="large" color={theme.colors?.primary || '#0000ff'} />
            </View>
        );
    }

    const isAdmin = user?.roles?.includes('ADMIN');

    return (
        <Stack.Navigator screenOptions={{ headerShown: false }}>
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
                    {isAdmin && (
                        <>
                            <Stack.Screen name="AdminDashboard" component={AdminScreen} />
                            <Stack.Screen name="AdminUsers" component={AdminUsersScreen} />
                            <Stack.Screen name="AdminFeedback" component={AdminFeedbackScreen} />
                            <Stack.Screen name="AdminBusinessVerification" component={AdminBusinessVerificationScreen} />
                        </>
                    )}
                    <Stack.Screen name="Home" component={HomeScreen} />
                    <Stack.Screen name="EventList" component={EventListScreen} />
                    <Stack.Screen name="EventDetails" component={EventDetailsScreen} />
                    <Stack.Screen name="SubscriptionPlans" component={SubscriptionPlansScreen} />
                    <Stack.Screen name="CreateQuestion" component={CreateQuestionScreen} />
                    <Stack.Screen name="QuestionThread" component={QuestionThreadScreen} />
                    <Stack.Screen name="Profile" component={ProfileScreen} />
                    <Stack.Screen name="ProfileStats" component={ProfileStats} options={{ headerShown: false }} />
                    <Stack.Screen name="EditProfile" component={EditProfileScreen} />
                    {!isAdmin && (
                        <>
                            <Stack.Screen name="Balance" component={BalanceScreen} options={{ headerShown: false }} />
                            <Stack.Screen name="MyPurchases" component={MyPurchasesScreen} options={{ headerShown: false }} />
                            <Stack.Screen name="Settings" component={SettingsScreen} options={{ headerShown: false }} />
                            <Stack.Screen name="BusinessVerificationStatus" component={BusinessVerificationStatusScreen} options={{ headerShown: false }} />
                        </>
                    )}
                </>
            )}
        </Stack.Navigator>
    );
}
