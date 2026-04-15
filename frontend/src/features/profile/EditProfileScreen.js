import React, { useCallback, useMemo, useState } from 'react';
import {
    View,
    Text,
    StyleSheet,
    SafeAreaView,
    TouchableOpacity,
    ScrollView,
    TextInput,
    ActivityIndicator,
    Modal,
    Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import * as ImagePicker from 'expo-image-picker';
import { useAuth } from '../../app/providers/AuthProvider';
import apiClient from '../../shared/services/http/apiClient';

// --- CONFIGURACIÓN DE AVATARES PREDETERMINADOS ---
const PRESET_AVATARS = [
    { id: '1', url: 'https://cdn-icons-png.flaticon.com/512/616/616408.png' },
    { id: '2', url: 'https://cdn-icons-png.flaticon.com/512/616/616430.png' },
    { id: '3', url: 'https://cdn-icons-png.flaticon.com/512/616/616408.png' },
    { id: '4', url: 'https://cdn-icons-png.flaticon.com/512/1998/1998627.png' },
    { id: '5', url: 'https://cdn-icons-png.flaticon.com/512/3069/3069172.png' },
    { id: '6', url: 'https://cdn-icons-png.flaticon.com/512/616/616554.png' },
];

const PROFILE_LIMITS = {
    email: 255,
    userName: 255,
    firstName: 255,
    lastName: 255,
    bio: 255,
    passwordMin: 6,
    passwordMax: 128,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function EditProfileScreen({ navigation }) {
    const { user, setUser } = useAuth();

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [originalUser, setOriginalUser] = useState(null);

    const [form, setForm] = useState({
        email: '',
        userName: '',
        firstName: '',
        lastName: '',
        bio: '',
        profilePictureUrl: '',
    });

    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [passwordForm, setPasswordForm] = useState({ newPassword: '', confirmPassword: '' });
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);

    const [feedback, setFeedback] = useState({
        visible: false,
        title: '',
        message: '',
        type: 'success',
        goBackOnClose: false,
    });

    const userId = useMemo(() => user?.id, [user]);

    const pickImage = async () => {
        const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (status !== 'granted') {
            showFeedback('Permissions', 'We need access to your photos to change your profile image.', 'error');
            return;
        }

        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ImagePicker.MediaTypeOptions.Images,
            allowsEditing: true,
            aspect: [1, 1],
            quality: 0.5,
            base64: true,
        });

        if (!result.canceled) {
            const base64Image = `data:image/jpeg;base64,${result.assets[0].base64}`;
            updateField('profilePictureUrl', base64Image);
        }
    };

    const showFeedback = (title, message, type = 'success', goBackOnClose = false) => {
        setFeedback({
            visible: true,
            title,
            message,
            type,
            goBackOnClose,
        });
    };

    const closeFeedback = () => {
        const shouldGoBack = feedback.goBackOnClose;
        setFeedback(prev => ({ ...prev, visible: false, goBackOnClose: false }));
        if (shouldGoBack) { navigation.goBack(); }
    };

    useFocusEffect(
        useCallback(() => {
            if (!userId) { setLoading(false); return; }
            let cancelled = false;
            const loadProfile = async () => {
                setLoading(true);
                try {
                    const res = await apiClient.get(`/api/v1/users/${userId}`);
                    if (cancelled) return;
                    const currentUser = res.data;
                    setOriginalUser(currentUser);
                    setForm({
                        email: currentUser?.email || '',
                        userName: currentUser?.userName || '',
                        firstName: currentUser?.firstName || '',
                        lastName: currentUser?.lastName || '',
                        bio: currentUser?.bio || '',
                        profilePictureUrl: currentUser?.profilePictureUrl || '',
                    });
                    setPasswordForm({ newPassword: '', confirmPassword: '' });
                } catch (error) {
                    if (!cancelled) showFeedback('Error', 'Your data could not be loaded.', 'error');
                } finally { if (!cancelled) setLoading(false); }
            };
            loadProfile();
            return () => { cancelled = true; };
        }, [userId])
    );

    const updateField = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const openPasswordModal = () => {
        setPasswordForm({ newPassword: '', confirmPassword: '' });
        setShowPassword(false);
        setShowConfirmPassword(false);
        setShowPasswordModal(true);
    };

    const handleSavePassword = async () => {
        if (!passwordForm.newPassword.trim()) {
            showFeedback('Validation', 'Password is required.', 'error'); return;
        }
        if (passwordForm.newPassword.length < PROFILE_LIMITS.passwordMin) {
            showFeedback('Validation', `Password must be at least ${PROFILE_LIMITS.passwordMin} characters.`, 'error'); return;
        }
        if (passwordForm.newPassword.length > PROFILE_LIMITS.passwordMax) {
            showFeedback('Validation', `Password cannot exceed ${PROFILE_LIMITS.passwordMax} characters.`, 'error'); return;
        }
        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            showFeedback('Validation', 'Passwords do not match.', 'error'); return;
        }
        if (!originalUser || !userId) return;

        setSavingPassword(true);
        try {
            const payload = { ...originalUser, password: passwordForm.newPassword.trim() };
            await apiClient.put(`/api/v1/users/${userId}`, payload);
            setShowPasswordModal(false);
            showFeedback('Password updated', 'Your password was changed successfully.', 'success');
        } catch (error) {
            const message = error?.response?.data?.message || 'Error while saving.';
            showFeedback('Error', message, 'error');
        } finally { setSavingPassword(false); }
    };

    const validateForm = () => {
        if (!form.email.trim()) { showFeedback('Validation', 'Email is required.', 'error'); return false; }
        if (!EMAIL_PATTERN.test(form.email.trim())) { showFeedback('Validation', 'Enter a valid email address.', 'error'); return false; }
        if (form.email.trim().length > PROFILE_LIMITS.email) {
            showFeedback('Validation', `Email cannot exceed ${PROFILE_LIMITS.email} characters.`, 'error');
            return false;
        }
        if (!form.userName.trim()) { showFeedback('Validation', 'Username is required.', 'error'); return false; }
        if (form.userName.trim().length > PROFILE_LIMITS.userName) {
            showFeedback('Validation', `Username cannot exceed ${PROFILE_LIMITS.userName} characters.`, 'error');
            return false;
        }
        if (form.firstName.trim().length > PROFILE_LIMITS.firstName) {
            showFeedback('Validation', `First name cannot exceed ${PROFILE_LIMITS.firstName} characters.`, 'error');
            return false;
        }
        if (form.lastName.trim().length > PROFILE_LIMITS.lastName) {
            showFeedback('Validation', `Last name cannot exceed ${PROFILE_LIMITS.lastName} characters.`, 'error');
            return false;
        }
        if (form.bio.length > PROFILE_LIMITS.bio) { showFeedback('Validation', `Bio cannot exceed ${PROFILE_LIMITS.bio} characters.`, 'error'); return false; }
        return true;
    };

    const handleSave = async () => {
        if (!originalUser || !userId) return;
        if (!validateForm()) return;
        setSaving(true);

        try {
            const payload = {
                ...originalUser,
                email: form.email.trim(),
                userName: form.userName.trim(),
                firstName: form.firstName.trim(),
                lastName: form.lastName.trim(),
                bio: form.bio.trim(),
                profilePictureUrl: form.profilePictureUrl,
            };

            delete payload.password;

            const res = await apiClient.put(`/api/v1/users/${userId}`, payload);
            const updatedUser = res.data;

            setOriginalUser(updatedUser);

            if (setUser) {
                setUser(prev => ({
                    ...prev,
                    id: updatedUser?.id || prev?.id,
                    username: updatedUser?.email || prev?.username,
                    email: updatedUser?.email || '',
                    userName: updatedUser?.userName || '',
                    firstName: updatedUser?.firstName || '',
                    lastName: updatedUser?.lastName || '',
                    bio: updatedUser?.bio || '',
                    profilePictureUrl: updatedUser?.profilePictureUrl || '',
                }));
            }

            showFeedback('Profile updated', 'Your changes were saved successfully.', 'success', true);
        } catch (error) {
            const message = error?.response?.data?.message || 'Error while saving.';
            showFeedback('Error', message, 'error');
        } finally { setSaving(false); }
    };

    if (loading) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="large" color="#d90429" />
                    <Text style={styles.loadingText}>Loading profile...</Text>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.screen}>
            <View style={styles.headerRed}>
                <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
                    <Ionicons name="arrow-back" size={24} color="#fff" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>EDIT PROFILE</Text>
                <Text style={styles.headerSubtitle}>Customize your presence on the platform.</Text>
            </View>

            <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer} keyboardShouldPersistTaps="handled">

                {/* CARD 1: APARIENCIA */}
                <View style={styles.card}>
                    <Text style={styles.sectionTitle}>Appearance</Text>

                    <View style={styles.avatarPickerSection}>
                        <TouchableOpacity onPress={pickImage} style={styles.avatarTouchable}>
                            <View style={styles.avatarCircle}>
                                {form.profilePictureUrl ? (
                                    <Image source={{ uri: form.profilePictureUrl }} style={styles.avatarImg} />
                                ) : (
                                    <Ionicons name="person" size={50} color="#9ca3af" />
                                )}
                            </View>
                            <View style={styles.cameraBadge}>
                                <Ionicons name="camera" size={18} color="#fff" />
                            </View>
                        </TouchableOpacity>
                        <View style={styles.avatarTextInfo}>
                            <Text style={styles.avatarTitle}>Profile picture</Text>
                            <Text style={styles.avatarSub}>Upload a photo or choose an avatar</Text>
                        </View>
                    </View>

                    {/* --- NUEVA FILA DE AVATARES PREDETERMINADOS --- */}
                    <Text style={styles.labelSmall}>Quick avatars</Text>
                    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.presetRow}>
                        {PRESET_AVATARS.map((avatar) => (
                            <TouchableOpacity
                                key={avatar.id}
                                onPress={() => updateField('profilePictureUrl', avatar.url)}
                                style={[
                                    styles.presetCircle,
                                    form.profilePictureUrl === avatar.url && styles.presetSelected
                                ]}
                            >
                                <Image source={{ uri: avatar.url }} style={styles.presetImg} />
                            </TouchableOpacity>
                        ))}
                    </ScrollView>

                    <Text style={styles.label}>Bio</Text>
                    <TextInput
                        style={[styles.input, styles.textArea]}
                        value={form.bio}
                        onChangeText={text => updateField('bio', text)}
                        placeholder="Tell us about yourself..."
                        placeholderTextColor="#9ca3af"
                        multiline
                    />
                    <Text style={styles.charCount}>{form.bio.length}/{PROFILE_LIMITS.bio}</Text>
                </View>

                {/* CARD 2: DATOS PERSONALES */}
                <View style={styles.card}>
                    <Text style={styles.sectionTitle}>Personal information</Text>
                    <Text style={styles.label}>First name</Text>
                    <TextInput style={styles.input} value={form.firstName} onChangeText={text => updateField('firstName', text)} placeholder="Your first name" placeholderTextColor="#9ca3af" />

                    <Text style={styles.label}>Last name</Text>
                    <TextInput style={styles.input} value={form.lastName} onChangeText={text => updateField('lastName', text)} placeholder="Your last name" placeholderTextColor="#9ca3af" />

                    <Text style={styles.label}>Username</Text>
                    <TextInput style={styles.input} value={form.userName} onChangeText={text => updateField('userName', text)} placeholder="Your username" autoCapitalize="none" placeholderTextColor="#9ca3af" />

                    <Text style={styles.label}>Email</Text>
                    <TextInput style={styles.input} value={form.email} onChangeText={text => updateField('email', text)} placeholder="you@example.com" autoCapitalize="none" keyboardType="email-address" placeholderTextColor="#9ca3af" />
                </View>

                {/* CARD 3: SEGURIDAD */}
                <View style={styles.card}>
                    <Text style={styles.sectionTitle}>Security</Text>
                    <TouchableOpacity style={styles.changePasswordBtn} onPress={openPasswordModal}>
                        <Ionicons name="lock-closed-outline" size={20} color="#d90429" />
                        <Text style={styles.changePasswordText}>Change password</Text>
                        <Ionicons name="chevron-forward" size={18} color="#9ca3af" />
                    </TouchableOpacity>
                </View>

                <TouchableOpacity style={[styles.saveBtn, saving && styles.saveBtnDisabled]} onPress={handleSave} disabled={saving}>
                    {saving ? <ActivityIndicator color="#fff" /> : <Text style={styles.saveBtnText}>SAVE CHANGES</Text>}
                </TouchableOpacity>
            </ScrollView>

            <Modal transparent visible={showPasswordModal} animationType="fade" onRequestClose={() => setShowPasswordModal(false)}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        <Text style={styles.modalTitle}>Change password</Text>

                        <Text style={styles.label}>New password</Text>
                        <View style={styles.passwordWrapper}>
                            <TextInput style={styles.passwordInput} value={passwordForm.newPassword} onChangeText={text => setPasswordForm(prev => ({ ...prev, newPassword: text }))} placeholder="New password" secureTextEntry={!showPassword} autoCapitalize="none" autoComplete="new-password" textContentType="newPassword" placeholderTextColor="#9ca3af" />
                            <TouchableOpacity onPress={() => setShowPassword(prev => !prev)}>
                                <Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={22} color="#6b7280" />
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.label}>Confirm new password</Text>
                        <View style={styles.passwordWrapper}>
                            <TextInput style={styles.passwordInput} value={passwordForm.confirmPassword} onChangeText={text => setPasswordForm(prev => ({ ...prev, confirmPassword: text }))} placeholder="Repeat new password" secureTextEntry={!showConfirmPassword} autoCapitalize="none" autoComplete="new-password" textContentType="newPassword" placeholderTextColor="#9ca3af" />
                            <TouchableOpacity onPress={() => setShowConfirmPassword(prev => !prev)}>
                                <Ionicons name={showConfirmPassword ? 'eye-off-outline' : 'eye-outline'} size={22} color="#6b7280" />
                            </TouchableOpacity>
                        </View>

                        <View style={styles.passwordModalButtons}>
                            <TouchableOpacity style={styles.passwordModalCancel} onPress={() => setShowPasswordModal(false)}>
                                <Text style={styles.passwordModalCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.saveBtn, { flex: 1 }, savingPassword && styles.saveBtnDisabled]} onPress={handleSavePassword} disabled={savingPassword}>
                                {savingPassword ? <ActivityIndicator color="#fff" /> : <Text style={styles.saveBtnText}>Save</Text>}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal transparent visible={feedback.visible} animationType="fade" onRequestClose={closeFeedback}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        <View style={[styles.modalIconWrap, feedback.type === 'success' ? styles.modalSuccess : styles.modalError]}>
                            <Ionicons name={feedback.type === 'success' ? 'checkmark-outline' : 'close-outline'} size={28} color="#fff" />
                        </View>
                        <Text style={styles.modalTitle}>{feedback.title}</Text>
                        <Text style={styles.modalMessage}>{feedback.message}</Text>
                        <TouchableOpacity style={[styles.modalButton, feedback.type === 'success' ? styles.modalButtonSuccess : styles.modalButtonError]} onPress={closeFeedback}>
                            <Text style={styles.modalButtonText}>Accept</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: { flex: 1, backgroundColor: '#f9fafb' },
    headerRed: { backgroundColor: '#d90429', paddingHorizontal: 20, paddingTop: 40, paddingBottom: 24 },
    backBtn: { marginBottom: 14 },
    headerTitle: { color: '#fff', fontSize: 24, fontWeight: 'bold' },
    headerSubtitle: { color: '#ffe5e9', fontSize: 14, marginTop: 6, lineHeight: 20 },
    container: { flex: 1 },
    contentContainer: { padding: 16, paddingBottom: 28 },
    card: { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 16, elevation: 2, shadowColor: '#000', shadowOpacity: 0.06 },
    sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#111827', marginBottom: 14 },
    label: { fontSize: 14, fontWeight: '600', color: '#374151', marginBottom: 8, marginTop: 10 },
    labelSmall: { fontSize: 12, fontWeight: 'bold', color: '#9ca3af', marginBottom: 10, textTransform: 'uppercase' },
    input: { backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 13, fontSize: 15, color: '#111827', borderWidth: 1, borderColor: '#e5e7eb' },
    textArea: { height: 100, textAlignVertical: 'top' },
    charCount: { textAlign: 'right', fontSize: 12, color: '#9ca3af', marginTop: 4 },

    avatarPickerSection: { flexDirection: 'row', alignItems: 'center', marginBottom: 16 },
    avatarTouchable: { position: 'relative' },
    avatarCircle: { width: 90, height: 90, borderRadius: 45, backgroundColor: '#f3f4f6', justifyContent: 'center', alignItems: 'center', overflow: 'hidden', borderWidth: 1, borderColor: '#e5e7eb' },
    avatarImg: { width: '100%', height: '100%' },
    cameraBadge: { position: 'absolute', bottom: 0, right: 0, backgroundColor: '#d90429', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: '#fff' },
    avatarTextInfo: { marginLeft: 20 },
    avatarTitle: { fontSize: 16, fontWeight: 'bold', color: '#111827' },
    avatarSub: { fontSize: 13, color: '#6b7280', marginTop: 2 },

    presetRow: { flexDirection: 'row', marginBottom: 20 },
    presetCircle: { width: 54, height: 54, borderRadius: 27, marginRight: 12, borderWidth: 2, borderColor: 'transparent', overflow: 'hidden', backgroundColor: '#f3f4f6' },
    presetSelected: { borderColor: '#d90429' },
    presetImg: {
        width: '100%',
        height: '100%',
        resizeMode: 'contain',
    },

    changePasswordBtn: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12, gap: 10 },
    changePasswordText: { flex: 1, fontSize: 15, fontWeight: '600', color: '#d90429' },
    passwordWrapper: { backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 4, borderWidth: 1, borderColor: '#e5e7eb', flexDirection: 'row', alignItems: 'center', marginBottom: 4 },
    passwordInput: { flex: 1, paddingVertical: 10, fontSize: 15, color: '#111827' },
    passwordModalButtons: { flexDirection: 'row', gap: 10, marginTop: 18 },
    passwordModalCancel: { flex: 1, borderRadius: 12, paddingVertical: 16, alignItems: 'center', backgroundColor: '#f3f4f6' },
    passwordModalCancelText: { fontSize: 16, fontWeight: 'bold', color: '#6b7280' },
    saveBtn: { backgroundColor: '#d90429', borderRadius: 12, paddingVertical: 16, alignItems: 'center', marginTop: 4 },
    saveBtnDisabled: { opacity: 0.7 },
    saveBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
    loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
    loadingText: { marginTop: 12, fontSize: 15, color: '#4b5563' },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)', justifyContent: 'center', alignItems: 'center', paddingHorizontal: 24 },
    modalCard: { width: '100%', maxWidth: 380, backgroundColor: '#fff', borderRadius: 22, padding: 24, alignItems: 'center' },
    modalIconWrap: { width: 62, height: 62, borderRadius: 31, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
    modalSuccess: { backgroundColor: '#16a34a' },
    modalError: { backgroundColor: '#dc2626' },
    modalTitle: { fontSize: 22, fontWeight: 'bold', color: '#111827', marginBottom: 10 },
    modalMessage: { fontSize: 15, color: '#4b5563', textAlign: 'center', marginBottom: 22 },
    modalButton: { minWidth: 140, paddingVertical: 14, borderRadius: 14, alignItems: 'center' },
    modalButtonSuccess: { backgroundColor: '#16a34a' },
    modalButtonError: { backgroundColor: '#dc2626' },
    modalButtonText: { color: '#fff', fontSize: 15, fontWeight: '700' },
});
