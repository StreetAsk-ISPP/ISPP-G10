import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, ActivityIndicator, TextInput, Modal } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import apiClient from '../../../shared/services/http/apiClient';

export default function AdminUsersScreen() {
    const navigation = useNavigation();
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');

    const [modalVisible, setModalVisible] = useState(false);
    const [strikeModalVisible, setStrikeModalVisible] = useState(false);
    const [confirmDeleteVisible, setConfirmDeleteVisible] = useState(false);
    const [userToDelete, setUserToDelete] = useState(null);
    const [selectedUserForStrike, setSelectedUserForStrike] = useState(null);
    const [editingUser, setEditingUser] = useState(null);
    const [form, setForm] = useState({
        email: '',
        userName: '',
        firstName: '',
        lastName: '',
        authority: 'USER',
    });
    const [strikeForm, setStrikeForm] = useState({
        reason: '',
        description: '',
    });
    const [confirmStrikeVisible, setConfirmStrikeVisible] = useState(false);
    const [strikeCounts, setStrikeCounts] = useState({});

    const loadStrikeCounts = useCallback(async (usersList) => {
        const entries = await Promise.all(
            usersList
                .filter(user => {
                    const authority = user.authorities?.[0]?.authority;
                    return authority !== 'ADMIN' && authority !== 'ROLE_ADMIN';
                })
                .map(async (user) => {
                    try {
                        const response = await apiClient.get(
                            `/api/v1/moderation/users/${user.id}/strike-count`
                        );
                        return [user.id, response.data?.count ?? 0];
                    } catch (error) {
                        return [user.id, 0];
                    }
                })
        );
        setStrikeCounts(Object.fromEntries(entries));
    }, []);

    const fetchUsers = useCallback(async () => {
        setLoading(true);
        try {
            const response = await apiClient.get('/api/v1/users');

            let usersData = [];
            let data = response.data;

            if (typeof data === 'string') {
                data = JSON.parse(data);
            }

            if (Array.isArray(data)) {
                usersData = data;
            } else if (data && typeof data === 'object') {
                usersData = [];
            }

            setUsers(usersData);
            await loadStrikeCounts(usersData);
        } catch (error) {
            console.error("Error fetching users:", error);
            Alert.alert("Error", "Users could not be loaded");
        } finally {
            setLoading(false);
        }
    }, [loadStrikeCounts]);

    useEffect(() => {
        const timer = setTimeout(() => {
            fetchUsers();
        }, 500);
        return () => clearTimeout(timer);
    }, [fetchUsers]);

    const openEditModal = (user) => {
        setEditingUser(user);
        setForm({
            email: user.email || '',
            userName: user.username || user.userName || '',
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            authority: user.authorities?.[0]?.authority || 'USER',
        });
        setModalVisible(true);
    };

    const handleSaveUser = async () => {
        try {
            if (!form.email || !form.userName) {
                Alert.alert('Incomplete data', 'Email and username are required');
                return;
            }

            let response;

            if (editingUser) {
                const payload = {
                    ...editingUser,
                    email: form.email,
                    username: form.userName,
                    userName: form.userName,
                    firstName: form.firstName,
                    lastName: form.lastName,
                };

                response = await apiClient.put(`/api/v1/users/${editingUser.id}`, payload);
                const updated = response.data;

                setUsers(prev => prev.map(u => (u.id === editingUser.id ? updated : u)));
            } else {
                Alert.alert(
                    'Action not available',
                    'User creation must be done through the app signup flow.',
                );
                return;
            }

            setModalVisible(false);
            setEditingUser(null);
        } catch (error) {
            const msg =
                error?.response?.data?.message ||
                error?.response?.data?.error ||
                'The user could not be saved';
            Alert.alert('Error', msg);
        }
    };

    const handleDeleteUser = async (user) => {
        setUserToDelete(user);
        setConfirmDeleteVisible(true);
    };

    const confirmDeleteUserAction = async () => {
        try {
            await apiClient.delete(
                `/api/v1/moderation/users/${userToDelete.id}`,
                {
                    params: {
                        confirm: true
                    }
                }
            );

            setUsers(prev => prev.filter(u => u.id !== userToDelete.id));
            setConfirmDeleteVisible(false);
            setUserToDelete(null);
            Alert.alert('Éxito', 'Usuario eliminado correctamente');
        } catch (error) {
            let msg = 'No se pudo eliminar el usuario';

            if (error?.response?.status === 403) {
                msg = 'No tienes permiso para eliminar este usuario (debes ser admin)';
            } else if (error?.response?.status === 400) {
                msg = error?.response?.data?.message || 'Solicitud inválida';
            } else if (error?.response?.status === 404) {
                msg = 'Usuario no encontrado';
            } else if (error?.response?.status === 401) {
                msg = 'No autenticado. Por favor inicia sesión de nuevo';
            } else if (error?.response?.data?.message) {
                msg = error.response.data.message;
            } else if (error?.message) {
                msg = error.message;
            }

            Alert.alert('Error', msg);
        }
    };

    const openStrikeModal = (user) => {
        setSelectedUserForStrike(user);
        setStrikeForm({ reason: '', description: '' });
        setStrikeModalVisible(true);
    };

    const handleSendStrike = async () => {
        if (!strikeForm.reason.trim()) {
            Alert.alert('Error', 'El motivo del strike es obligatorio');
            return;
        }

        setConfirmStrikeVisible(true);
    };

    const confirmStrikeAction = async () => {
        try {
            const payload = {
                reason: strikeForm.reason,
                description: strikeForm.description || null,
            };

            await apiClient.post(
                `/api/v1/moderation/users/${selectedUserForStrike.id}/strike`,
                payload
            );
            setStrikeModalVisible(false);
            setConfirmStrikeVisible(false);
            setSelectedUserForStrike(null);
            setStrikeForm({ reason: '', description: '' });
            Alert.alert('Éxito', 'Strike enviado correctamente al usuario');
        } catch (error) {
            const msg =
                error?.response?.data?.message ||
                error?.response?.data?.error ||
                (error?.response?.status === 403
                    ? 'You cannot delete this user'
                    : 'The user could not be deleted');
            Alert.alert('Error', msg);
        }
    };

    const filteredUsers = users.filter(user =>
        user.username?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        user.email?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const renderUserItem = ({ item }) => {
        const displayName = item.username || item.userName || '';
        const isAdmin = item.authorities?.[0]?.authority === 'ADMIN' ||
            item.authorities?.[0]?.authority === 'ROLE_ADMIN';
        return (
            <View style={styles.userCard}>
                <View style={styles.userInfo}>
                    <View style={[styles.avatar, { backgroundColor: isAdmin ? '#e3f2fd' : '#f5f5f5' }]}>
                        <Text style={[styles.avatarText, { color: isAdmin ? '#007bff' : '#666' }]}>
                            {displayName ? displayName.charAt(0).toUpperCase() : '?'}
                        </Text>
                    </View>
                    <View style={styles.userDetails}>
                        <Text style={styles.userName}>{displayName}</Text>
                        <Text style={styles.userEmail}>{item.email}</Text>
                        <View style={styles.badgesContainer}>
                            <View style={[styles.badge, item.active ? styles.activeBadge : styles.inactiveBadge]}>
                                <Text style={[styles.badgeText, item.active ? styles.activeText : styles.inactiveText]}>
                                    {item.active ? 'Active' : 'Inactive'}
                                </Text>
                            </View>
                            {isAdmin && (
                                <View style={[styles.badge, styles.adminBadge]}>
                                    <Text style={[styles.badgeText, styles.adminText]}>ADMIN</Text>
                                </View>
                            )}
                            {strikeCounts[item.id] > 0 && (
                                <View style={[styles.badge, styles.strikeBadge]}>
                                    <Ionicons name="warning" size={14} color="#fff" />
                                    <Text style={[styles.badgeText, styles.strikeText]}>{strikeCounts[item.id]}</Text>
                                </View>
                            )}
                        </View>
                    </View>
                </View>
                <View style={styles.actions}>
                    {!isAdmin && (
                        <TouchableOpacity
                            style={[styles.actionButton, { backgroundColor: '#fff3cd' }]}
                            onPress={() => openStrikeModal(item)}
                            activeOpacity={0.7}
                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                        >
                            <Ionicons name="warning" size={20} color="#ff6b6b" />
                        </TouchableOpacity>
                    )}
                    <TouchableOpacity
                        style={[styles.actionButton, { backgroundColor: '#e3f2fd' }]}
                        onPress={() => openEditModal(item)}
                        activeOpacity={0.7}
                        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                    >
                        <Ionicons name="pencil-outline" size={20} color="#007bff" />
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.actionButton, styles.deleteButton]}
                        onPress={() => handleDeleteUser(item)}
                        activeOpacity={0.7}
                        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                    >
                        <Ionicons name="trash-outline" size={20} color="#d90429" />
                    </TouchableOpacity>
                </View>
            </View>
        );
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Manage Users</Text>
                <View style={{ flexDirection: 'row' }}>
                    <TouchableOpacity onPress={fetchUsers} style={styles.refreshButton}>
                        <Ionicons name="refresh" size={22} color="#007bff" />
                    </TouchableOpacity>
                </View>
            </View>

            <View style={styles.searchContainer}>
                <Ionicons name="search" size={20} color="#999" style={styles.searchIcon} />
                <TextInput
                    style={styles.searchInput}
                    placeholder="Search by name or email..."
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

            {loading ? (
                <ActivityIndicator size="large" color="#007bff" style={styles.loader} />
            ) : (
                <FlatList
                    data={filteredUsers}
                    renderItem={renderUserItem}
                    keyExtractor={item => item.id}
                    contentContainerStyle={styles.listContent}
                    ListEmptyComponent={
                        <Text style={styles.emptyText}>No users found</Text>
                    }
                />
            )}

            <Modal
                visible={modalVisible}
                transparent
                animationType="slide"
                onRequestClose={() => setModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>
                            {editingUser ? 'Edit user' : 'User'}
                        </Text>

                        {['email', 'userName', 'firstName', 'lastName'].map((field) => (
                            <View key={field} style={styles.modalField}>
                                <Text style={styles.inputLabel}>
                                    {field === 'userName'
                                        ? 'Username'
                                        : field === 'firstName'
                                            ? 'First name'
                                            : field === 'lastName'
                                                ? 'Last name'
                                                : 'Email'}
                                </Text>
                                <TextInput
                                    style={styles.modalInput}
                                    autoCapitalize={field === 'email' || field === 'userName' ? 'none' : 'sentences'}
                                    keyboardType={field === 'email' ? 'email-address' : 'default'}
                                    value={form[field]}
                                    onChangeText={(text) => setForm(prev => ({ ...prev, [field]: text }))}
                                />
                            </View>
                        ))}

                        <View style={styles.modalActions}>
                            <TouchableOpacity
                                style={[styles.modalButton, styles.cancelButton]}
                                onPress={() => setModalVisible(false)}
                            >
                                <Text style={styles.cancelButtonText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.modalButton, styles.saveButton]}
                                onPress={handleSaveUser}
                            >
                                <Text style={styles.saveButtonText}>Save</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                visible={strikeModalVisible}
                transparent
                animationType="slide"
                onRequestClose={() => setStrikeModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>Enviar Strike</Text>
                        <Text style={styles.modalSubtitle}>
                            {selectedUserForStrike && `Usuario: ${selectedUserForStrike.username || selectedUserForStrike.userName}`}
                        </Text>

                        <View style={styles.modalField}>
                            <Text style={styles.inputLabel}>Motivo *</Text>
                            <TextInput
                                style={styles.modalInput}
                                placeholder="Ej: Contenido inapropiado, spam, violación de normas..."
                                value={strikeForm.reason}
                                onChangeText={(text) => setStrikeForm(prev => ({ ...prev, reason: text }))}
                                multiline={true}
                                numberOfLines={3}
                                textAlignVertical="top"
                            />
                        </View>

                        <View style={styles.modalField}>
                            <Text style={styles.inputLabel}>Descripción (opcional)</Text>
                            <TextInput
                                style={styles.modalInput}
                                placeholder="Detalles adicionales sobre el strike..."
                                value={strikeForm.description}
                                onChangeText={(text) => setStrikeForm(prev => ({ ...prev, description: text }))}
                                multiline={true}
                                numberOfLines={3}
                                textAlignVertical="top"
                            />
                        </View>

                        <View style={styles.modalActions}>
                            <TouchableOpacity
                                style={[styles.modalButton, styles.cancelButton]}
                                onPress={() => setStrikeModalVisible(false)}
                            >
                                <Text style={styles.cancelButtonText}>Cancelar</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.modalButton, styles.strikeButton]}
                                onPress={handleSendStrike}
                            >
                                <Text style={styles.strikeButtonText}>Enviar Strike</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                visible={confirmDeleteVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setConfirmDeleteVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.confirmDialogContent}>
                        <Ionicons name="trash-outline" size={48} color="#d90429" style={{ marginBottom: 16 }} />
                        <Text style={styles.confirmDialogTitle}>Confirmar eliminación</Text>
                        <Text style={styles.confirmDialogMessage}>
                            ¿Estás seguro de que deseas eliminar la cuenta de {userToDelete?.username || userToDelete?.userName}?
                        </Text>
                        <Text style={styles.confirmDialogWarning}>
                            Esta acción no se puede deshacer.
                        </Text>

                        <View style={styles.confirmDialogActions}>
                            <TouchableOpacity
                                style={[styles.dialogButton, styles.dialogCancelButton]}
                                onPress={() => setConfirmDeleteVisible(false)}
                            >
                                <Text style={styles.dialogCancelButtonText}>Cancelar</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.dialogButton, styles.dialogDeleteButton]}
                                onPress={confirmDeleteUserAction}
                            >
                                <Text style={styles.dialogDeleteButtonText}>Eliminar usuario</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                visible={confirmStrikeVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setConfirmStrikeVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.confirmDialogContent}>
                        <Ionicons name="warning" size={48} color="#ff9800" style={{ marginBottom: 16 }} />
                        <Text style={styles.confirmDialogTitle}>Confirmar strike</Text>
                        <Text style={styles.confirmDialogMessage}>
                            ¿Estás seguro de que deseas enviar un strike a {selectedUserForStrike?.username || selectedUserForStrike?.userName}?
                        </Text>
                        <Text style={styles.confirmDialogWarning}>
                            Motivo: {strikeForm.reason}
                        </Text>

                        <View style={styles.confirmDialogActions}>
                            <TouchableOpacity
                                style={[styles.dialogButton, styles.dialogCancelButton]}
                                onPress={() => setConfirmStrikeVisible(false)}
                            >
                                <Text style={styles.dialogCancelButtonText}>Cancelar</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.dialogButton, styles.dialogStrikeButton]}
                                onPress={confirmStrikeAction}
                            >
                                <Text style={styles.dialogStrikeButtonText}>Enviar strike</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f8f9fa' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 15,
        backgroundColor: 'white',
        borderBottomWidth: 1,
        borderBottomColor: '#eee'
    },
    backButton: { padding: 5 },
    refreshButton: { padding: 5 },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#333' },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'white',
        margin: 15,
        paddingHorizontal: 15,
        borderRadius: 10,
        borderWidth: 1,
        borderColor: '#eee',
        height: 45
    },
    searchIcon: { marginRight: 10 },
    searchInput: { flex: 1, fontSize: 16 },
    listContent: { padding: 15 },
    loader: { marginTop: 50 },
    emptyText: { textAlign: 'center', marginTop: 50, color: '#999', fontSize: 16 },
    userCard: {
        backgroundColor: 'white',
        borderRadius: 12,
        padding: 15,
        marginBottom: 15,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
        elevation: 2
    },
    userInfo: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    avatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 15
    },
    avatarText: { fontSize: 20, fontWeight: 'bold' },
    userDetails: { flex: 1 },
    userName: { fontSize: 16, fontWeight: '600', color: '#333' },
    userEmail: { fontSize: 12, color: '#666', marginTop: 2 },
    badgesContainer: { flexDirection: 'row', marginTop: 5, gap: 5 },
    badge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: 4 },
    badgeText: { fontSize: 10, fontWeight: '600' },
    activeBadge: { backgroundColor: '#e8f5e9' },
    activeText: { color: '#2e7d32' },
    inactiveBadge: { backgroundColor: '#ffebee' },
    inactiveText: { color: '#c62828' },
    adminBadge: { backgroundColor: '#e3f2fd' },
    adminText: { color: '#1565c0' },
    strikeBadge: { backgroundColor: '#ff9800', flexDirection: 'row', alignItems: 'center', gap: 4 },
    strikeText: { color: '#fff' },
    actions: {
        flexDirection: 'row',
        gap: 5,
        justifyContent: 'flex-end'
    },
    actionButton: {
        padding: 10,
        marginLeft: 5,
        borderRadius: 8,
        backgroundColor: '#f5f5f5',
        justifyContent: 'center',
        alignItems: 'center',
        minWidth: 40,
        minHeight: 40
    },
    deleteButton: {
        backgroundColor: '#ffebee'
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.4)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    modalContent: {
        width: '90%',
        maxWidth: 420,
        backgroundColor: 'white',
        borderRadius: 12,
        padding: 20,
    },
    modalTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 16, color: '#111827' },
    modalField: { marginBottom: 12 },
    inputLabel: { marginBottom: 4, color: '#4b5563', fontSize: 14 },
    modalInput: {
        borderWidth: 1,
        borderColor: '#e5e7eb',
        borderRadius: 8,
        paddingHorizontal: 10,
        paddingVertical: 8,
        fontSize: 16,
    },
    modalActions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 12 },
    modalButton: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 8 },
    cancelButton: { marginRight: 8, backgroundColor: 'transparent' },
    cancelButtonText: { color: '#6b7280', fontWeight: '600' },
    saveButton: { backgroundColor: '#2563eb' },
    saveButtonText: { color: 'white', fontWeight: '600' },
    strikeButton: { backgroundColor: '#ff6b6b' },
    strikeButtonText: { color: 'white', fontWeight: '600' },
    modalSubtitle: { fontSize: 14, color: '#666', marginBottom: 12 },
    confirmDialogContent: {
        width: '85%',
        maxWidth: 380,
        backgroundColor: 'white',
        borderRadius: 16,
        padding: 24,
        alignItems: 'center',
    },
    confirmDialogTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#111827',
        marginBottom: 12,
        textAlign: 'center'
    },
    confirmDialogMessage: {
        fontSize: 14,
        color: '#4b5563',
        textAlign: 'center',
        marginBottom: 8,
    },
    confirmDialogWarning: {
        fontSize: 13,
        color: '#d90429',
        textAlign: 'center',
        marginBottom: 20,
        fontWeight: '600',
    },
    confirmDialogActions: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        width: '100%',
        gap: 12,
    },
    dialogButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: 'center',
    },
    dialogCancelButton: {
        backgroundColor: '#f3f4f6',
    },
    dialogCancelButtonText: {
        color: '#6b7280',
        fontWeight: '600',
        fontSize: 14,
    },
    dialogDeleteButton: {
        backgroundColor: '#d90429',
    },
    dialogDeleteButtonText: {
        color: 'white',
        fontWeight: '600',
        fontSize: 14,
    },
    dialogStrikeButton: {
        backgroundColor: '#ff9800',
    },
    dialogStrikeButtonText: {
        color: 'white',
        fontWeight: '600',
        fontSize: 14,
    },
});