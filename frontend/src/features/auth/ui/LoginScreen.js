import { useState } from 'react';
import {
    Image, Keyboard, Platform, ScrollView, StyleSheet, Text,
    TextInput, TouchableOpacity, TouchableWithoutFeedback,
    View, useWindowDimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import apiClient from '../../../shared/services/http/apiClient';
import { useAuth } from '../../../app/providers/AuthProvider';

export default function LoginScreen({ navigation }) {
    const { login } = useAuth();
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;

    const [identifier, setIdentifier] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [focusedField, setFocusedField] = useState(null);
    const [pendingBusinessEmail, setPendingBusinessEmail] = useState(null);

    const handleLogin = async () => {
        setError('');
        if (!identifier.trim() || !password.trim()) {
            setError('Please enter username/email and password.');
            return;
        }
        try {
            setIsSubmitting(true);
            const response = await apiClient.post('/api/v1/auth/signin', {
                email: identifier,
                password,
            });
            const user = {
                id: response.data.id,
                username: response.data.username,
                roles: response.data.roles,
            };

            await login(response.data.token, user);

            // Navigate based on role
            /*
            if (user.roles && user.roles.includes('ADMIN')) {
                // The AppNavigator will handle the initial route based on the updated auth state
                // But we can force a check if needed or just let the state update trigger the re-render of navigator
            }
            */
        } catch {
            // Check if this is a pending business signup (user created basic account but didn't pay)
            try {
                const pendingRes = await apiClient.get('/api/v1/auth/signup/basic/pending', {
                    params: { identifier: identifier.trim() },
                });
                if (pendingRes.data?.pending === true && pendingRes.data?.email) {
                    setPendingBusinessEmail(pendingRes.data.email);
                    setError('Your business registration is pending payment. Complete your payment to activate your account.');
                    return;
                }
            } catch { /* ignore check failure */ }
            setPendingBusinessEmail(null);
            setError('Login failed. Please check your credentials.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const content = (
        <View style={styles.screen}>
            <ScrollView
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
                keyboardShouldPersistTaps="handled"
            >
                <View style={[styles.card, isNarrow && { marginHorizontal: 16, borderRadius: 24 }]}>
                    {/* Logo */}
                    <View style={styles.logoContainer}>
                        <Image
                            source={require('../../../../assets/logo.png')}
                            style={styles.logoImage}
                            resizeMode="contain"
                        />
                    </View>

                    <Text style={styles.title}>Welcome back</Text>
                    <Text style={styles.subtitle}>Sign in to your account</Text>

                    {/* Username */}
                    <Text style={styles.label}>Username or Email</Text>
                    <View style={[styles.inputWrapper, focusedField === 'id' && styles.inputFocused]}>
                        <Ionicons name="person-outline" size={18} color="#1f2937" style={styles.inputIcon} />
                        <TextInput
                            value={identifier}
                            onChangeText={setIdentifier}
                            autoCapitalize="none"
                            autoCorrect={false}
                            placeholder="you@example.com"
                            placeholderTextColor="#c0c5ce"
                            style={styles.input}
                            onFocus={() => setFocusedField('id')}
                            onBlur={() => setFocusedField(null)}
                        />
                    </View>

                    {/* Password */}
                    <Text style={styles.label}>Password</Text>
                    <View style={[styles.inputWrapper, focusedField === 'pw' && styles.inputFocused]}>
                        <Ionicons name="lock-closed-outline" size={18} color="#1f2937" style={styles.inputIcon} />
                        <TextInput
                            value={password}
                            onChangeText={setPassword}
                            secureTextEntry={!showPassword}
                            placeholder="********"
                            placeholderTextColor="#c0c5ce"
                            style={styles.input}
                            onFocus={() => setFocusedField('pw')}
                            onBlur={() => setFocusedField(null)}
                        />
                        <TouchableOpacity
                            onPress={() => setShowPassword((prev) => !prev)}
                            activeOpacity={0.7}
                            accessibilityRole="button"
                            accessibilityLabel={showPassword ? 'Hide password' : 'Show password'}
                        >
                            <Ionicons
                                name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                                size={20}
                                color="#6b7280"
                            />
                        </TouchableOpacity>
                    </View>

                    {error ? <Text style={styles.errorText}>{error}</Text> : null}

                    {pendingBusinessEmail ? (
                        <TouchableOpacity
                            style={styles.pendingPaymentBtn}
                            onPress={() => navigation.navigate('BusinessSignup', { email: pendingBusinessEmail, password })}
                            activeOpacity={0.85}
                        >
                            <Ionicons name="card-outline" size={18} color="#fff" />
                            <Text style={styles.pendingPaymentBtnText}>Complete Payment</Text>
                        </TouchableOpacity>
                    ) : null}

                    {/* Login Button */}
                    <TouchableOpacity
                        style={[styles.primaryBtn, isSubmitting && { opacity: 0.6 }]}
                        onPress={handleLogin}
                        disabled={isSubmitting}
                        activeOpacity={0.85}
                    >
                        <Text style={styles.primaryBtnText}>{isSubmitting ? 'Signing in...' : 'Sign In'}</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={styles.forgotLink}
                        activeOpacity={0.7}
                        onPress={() => navigation.navigate('ForgotPassword')}
                    >
                        <Text style={styles.forgotText}>Forgot password?</Text>
                    </TouchableOpacity>

                    <View style={styles.divider}>
                        <View style={styles.dividerLine} />
                        <Text style={styles.dividerText}>OR</Text>
                        <View style={styles.dividerLine} />
                    </View>

                    <TouchableOpacity
                        style={styles.secondaryBtn}
                        onPress={() => navigation.navigate('SignUp')}
                        activeOpacity={0.8}
                    >
                        <Text style={styles.secondaryBtnText}>Create an account</Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </View>
    );

    if (Platform.OS === 'web') return content;
    return (
        <TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
            {content}
        </TouchableWithoutFeedback>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f3f4f6',
    },
    scrollContent: {
        flexGrow: 1,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 24,
    },
    card: {
        width: '100%',
        maxWidth: 420,
        backgroundColor: '#fff',
        borderRadius: 28,
        padding: 32,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.06,
        shadowRadius: 24,
        elevation: 6,
    },
    logoContainer: {
        alignItems: 'center',
        marginBottom: 24,
    },
    logoImage: {
        width: 88,
        height: 88,
    },
    title: {
        fontSize: 26,
        fontWeight: '800',
        color: '#1f2937',
        textAlign: 'center',
    },
    subtitle: {
        fontSize: 14,
        color: '#9ca3af',
        textAlign: 'center',
        marginTop: 6,
        marginBottom: 28,
    },
    label: {
        fontSize: 13,
        fontWeight: '600',
        color: '#374151',
        marginBottom: 8,
        marginTop: 16,
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#f9fafb',
        borderWidth: 1.5,
        borderColor: '#e5e7eb',
        borderRadius: 14,
        paddingHorizontal: 14,
        height: 50,
    },
    inputFocused: {
        borderColor: '#dc2626',
        backgroundColor: '#fff',
    },
    inputIcon: {
        marginRight: 10,
    },
    input: {
        flex: 1,
        fontSize: 15,
        color: '#1f2937',
        height: '100%',
        outlineStyle: 'none',
    },
    errorText: {
        color: '#ef4444',
        fontSize: 13,
        textAlign: 'center',
        marginTop: 14,
    },
    primaryBtn: {
        backgroundColor: '#dc2626',
        borderRadius: 14,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 28,
        shadowColor: '#b91c1c',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.25,
        shadowRadius: 16,
        elevation: 5,
    },
    primaryBtnText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '700',
    },
    forgotLink: {
        marginTop: 16,
        alignItems: 'center',
    },
    forgotText: {
        fontSize: 13,
        color: '#b91c1c',
        fontWeight: '500',
    },
    divider: {
        flexDirection: 'row',
        alignItems: 'center',
        marginVertical: 24,
    },
    dividerLine: {
        flex: 1,
        height: 1,
        backgroundColor: '#e5e7eb',
    },
    dividerText: {
        paddingHorizontal: 12,
        fontSize: 12,
        color: '#9ca3af',
        fontWeight: '600',
    },
    pendingPaymentBtn: {
        backgroundColor: '#f59e0b',
        borderRadius: 14,
        paddingVertical: 14,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        marginTop: 12,
    },
    pendingPaymentBtnText: {
        color: '#fff',
        fontSize: 15,
        fontWeight: '700',
    },
    secondaryBtn: {
        backgroundColor: '#f3f4f6',
        borderRadius: 14,
        paddingVertical: 15,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    secondaryBtnText: {
        fontSize: 15,
        fontWeight: '600',
        color: '#374151',
    },
});
