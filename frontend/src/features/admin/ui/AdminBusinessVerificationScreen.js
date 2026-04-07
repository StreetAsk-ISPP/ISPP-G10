import React, { useState, useEffect, useCallback } from 'react';
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    TouchableOpacity,
    Alert,
    ActivityIndicator,
    TextInput,
    Modal,
    ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import apiClient from '../../../shared/services/http/apiClient';

const STATUS_TABS = ['PENDING', 'APPROVED', 'REJECTED'];

const STATUS_COLOR = {
    PENDING: '#f59e0b',
    APPROVED: '#22c55e',
    REJECTED: '#ef4444',
};

export default function AdminBusinessVerificationScreen() {
    const navigation = useNavigation();
    const [activeTab, setActiveTab] = useState('PENDING');
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [rejectModalVisible, setRejectModalVisible] = useState(false);
    const [approveModalVisible, setApproveModalVisible] = useState(false);
    const [selectedAccount, setSelectedAccount] = useState(null);
    const [rejectReason, setRejectReason] = useState('');
    const [actionLoading, setActionLoading] = useState(false);

    const fetchAccounts = useCallback((status) => {
        setLoading(true);
        apiClient.get(`/api/v1/moderation/businesses?status=${status}`)
            .then(res => setAccounts(res.data || []))
            .catch(() => Alert.alert('Error', 'Could not load business accounts'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        fetchAccounts(activeTab);
    }, [activeTab, fetchAccounts]);

    const handleApproveOpen = (account) => {
        setSelectedAccount(account);
        setApproveModalVisible(true);
    };

    const handleApproveConfirm = () => {
        if (!selectedAccount) return;
        setActionLoading(true);
        apiClient.post(`/api/v1/moderation/businesses/${selectedAccount.id}/approve`)
            .then(() => {
                setApproveModalVisible(false);
                Alert.alert('Success', `${selectedAccount.companyName} has been approved.`);
                fetchAccounts(activeTab);
            })
            .catch((err) => {
                console.error('Approve error:', err?.response?.data || err.message);
                Alert.alert('Error', 'Could not approve business account');
            })
            .finally(() => setActionLoading(false));
    };

    const handleRejectOpen = (account) => {
        setSelectedAccount(account);
        setRejectReason('');
        setRejectModalVisible(true);
    };

    const handleRejectConfirm = () => {
        if (!selectedAccount) return;
        setActionLoading(true);
        apiClient.post(`/api/v1/moderation/businesses/${selectedAccount.id}/reject`, {
            reason: rejectReason.trim() || null,
        })
            .then(() => {
                setRejectModalVisible(false);
                Alert.alert('Done', `${selectedAccount.companyName} has been rejected.`);
                fetchAccounts(activeTab);
            })
            .catch(() => Alert.alert('Error', 'Could not reject business account'))
            .finally(() => setActionLoading(false));
    };

    const renderAccount = ({ item }) => (
        <View style={styles.card}>
            <View style={styles.cardHeader}>
                <Text style={styles.companyName}>{item.companyName}</Text>
                <View style={[styles.statusBadge, { backgroundColor: STATUS_COLOR[item.requestStatus] + '22' }]}>
                    <Text style={[styles.statusText, { color: STATUS_COLOR[item.requestStatus] }]}>
                        {item.requestStatus}
                    </Text>
                </View>
            </View>

            <View style={styles.cardRow}>
                <Ionicons name="card-outline" size={14} color="#666" />
                <Text style={styles.cardDetail}> Tax ID: {item.taxId}</Text>
            </View>
            <View style={styles.cardRow}>
                <Ionicons name="person-outline" size={14} color="#666" />
                <Text style={styles.cardDetail}> {item.ownerFirstName} {item.ownerLastName} ({item.ownerUserName})</Text>
            </View>
            <View style={styles.cardRow}>
                <Ionicons name="mail-outline" size={14} color="#666" />
                <Text style={styles.cardDetail}> {item.ownerEmail}</Text>
            </View>
            {item.address ? (
                <View style={styles.cardRow}>
                    <Ionicons name="location-outline" size={14} color="#666" />
                    <Text style={styles.cardDetail}> {item.address}</Text>
                </View>
            ) : null}
            {item.website ? (
                <View style={styles.cardRow}>
                    <Ionicons name="globe-outline" size={14} color="#666" />
                    <Text style={styles.cardDetail}> {item.website}</Text>
                </View>
            ) : null}
            {item.description ? (
                <Text style={styles.description}>{item.description}</Text>
            ) : null}
            {item.rejectionReason ? (
                <View style={styles.rejectionReasonBox}>
                    <Text style={styles.rejectionReasonLabel}>Rejection reason:</Text>
                    <Text style={styles.rejectionReasonText}>{item.rejectionReason}</Text>
                </View>
            ) : null}

            {item.requestStatus === 'PENDING' && (
                <View style={styles.actionsRow}>
                    <TouchableOpacity
                        style={[styles.actionBtn, styles.approveBtn]}
                        onPress={() => handleApproveOpen(item)}
                        disabled={actionLoading}
                    >
                        <Ionicons name="checkmark-circle-outline" size={16} color="#fff" />
                        <Text style={styles.actionBtnText}>Approve</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.actionBtn, styles.rejectBtn]}
                        onPress={() => handleRejectOpen(item)}
                        disabled={actionLoading}
                    >
                        <Ionicons name="close-circle-outline" size={16} color="#fff" />
                        <Text style={styles.actionBtnText}>Reject</Text>
                    </TouchableOpacity>
                </View>
            )}
            {item.requestStatus === 'APPROVED' && (
                <TouchableOpacity
                    style={[styles.actionBtn, styles.rejectBtn, { alignSelf: 'flex-start', marginTop: 10 }]}
                    onPress={() => handleRejectOpen(item)}
                    disabled={actionLoading}
                >
                    <Ionicons name="close-circle-outline" size={16} color="#fff" />
                    <Text style={styles.actionBtnText}>Revoke</Text>
                </TouchableOpacity>
            )}
            {item.requestStatus === 'REJECTED' && (
                <TouchableOpacity
                    style={[styles.actionBtn, styles.approveBtn, { alignSelf: 'flex-start', marginTop: 10 }]}
                    onPress={() => handleApproveOpen(item)}
                    disabled={actionLoading}
                >
                    <Ionicons name="checkmark-circle-outline" size={16} color="#fff" />
                    <Text style={styles.actionBtnText}>Approve</Text>
                </TouchableOpacity>
            )}
        </View>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Business Verification</Text>
            </View>

            <View style={styles.tabs}>
                {STATUS_TABS.map(tab => (
                    <TouchableOpacity
                        key={tab}
                        style={[styles.tab, activeTab === tab && styles.activeTab]}
                        onPress={() => setActiveTab(tab)}
                    >
                        <Text style={[styles.tabText, activeTab === tab && { color: STATUS_COLOR[tab] }]}>
                            {tab}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>

            {loading ? (
                <View style={styles.centered}>
                    <ActivityIndicator size="large" color="#007bff" />
                </View>
            ) : accounts.length === 0 ? (
                <View style={styles.centered}>
                    <Ionicons name="business-outline" size={48} color="#ccc" />
                    <Text style={styles.emptyText}>No {activeTab.toLowerCase()} requests</Text>
                </View>
            ) : (
                <FlatList
                    data={accounts}
                    renderItem={renderAccount}
                    keyExtractor={item => item.id}
                    contentContainerStyle={styles.list}
                />
            )}

            <Modal visible={approveModalVisible} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalBox}>
                        <Text style={styles.modalTitle}>Approve Business</Text>
                        {selectedAccount && (
                            <Text style={styles.modalSubtitle}>
                                Are you sure you want to approve "{selectedAccount.companyName}"?
                            </Text>
                        )}
                        <View style={styles.modalActions}>
                            <TouchableOpacity
                                style={[styles.modalBtn, styles.modalCancelBtn]}
                                onPress={() => setApproveModalVisible(false)}
                            >
                                <Text style={styles.modalCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.modalBtn, { backgroundColor: '#22c55e' }]}
                                onPress={handleApproveConfirm}
                                disabled={actionLoading}
                            >
                                {actionLoading
                                    ? <ActivityIndicator size="small" color="#fff" />
                                    : <Text style={styles.modalRejectText}>Confirm Approve</Text>
                                }
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal visible={rejectModalVisible} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalBox}>
                        <Text style={styles.modalTitle}>Reject Business</Text>
                        {selectedAccount && (
                            <Text style={styles.modalSubtitle}>{selectedAccount.companyName}</Text>
                        )}
                        <Text style={styles.modalLabel}>Reason (optional)</Text>
                        <TextInput
                            style={styles.reasonInput}
                            placeholder="Enter rejection reason..."
                            value={rejectReason}
                            onChangeText={setRejectReason}
                            multiline
                            numberOfLines={3}
                        />
                        <View style={styles.modalActions}>
                            <TouchableOpacity
                                style={[styles.modalBtn, styles.modalCancelBtn]}
                                onPress={() => setRejectModalVisible(false)}
                            >
                                <Text style={styles.modalCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.modalBtn, styles.modalRejectBtn]}
                                onPress={handleRejectConfirm}
                                disabled={actionLoading}
                            >
                                {actionLoading
                                    ? <ActivityIndicator size="small" color="#fff" />
                                    : <Text style={styles.modalRejectText}>Confirm Reject</Text>
                                }
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
        padding: 16,
        paddingTop: 8,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    backBtn: { marginRight: 12 },
    headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#333' },
    tabs: {
        flexDirection: 'row',
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    tab: {
        flex: 1,
        paddingVertical: 12,
        alignItems: 'center',
    },
    activeTab: {
        borderBottomWidth: 2,
        borderBottomColor: '#007bff',
    },
    tabText: { fontSize: 13, fontWeight: '600', color: '#999' },
    list: { padding: 16, gap: 12 },
    centered: { flex: 1, justifyContent: 'center', alignItems: 'center', gap: 12 },
    emptyText: { fontSize: 16, color: '#999' },
    card: {
        backgroundColor: '#fff',
        borderRadius: 14,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.06,
        shadowRadius: 4,
        elevation: 2,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 10,
    },
    companyName: { fontSize: 16, fontWeight: 'bold', color: '#333', flex: 1 },
    statusBadge: {
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 20,
        marginLeft: 8,
    },
    statusText: { fontSize: 11, fontWeight: '700' },
    cardRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 4 },
    cardDetail: { fontSize: 13, color: '#555' },
    description: {
        fontSize: 13,
        color: '#666',
        marginTop: 8,
        fontStyle: 'italic',
        borderTopWidth: 1,
        borderTopColor: '#f0f0f0',
        paddingTop: 8,
    },
    rejectionReasonBox: {
        backgroundColor: '#fff0f0',
        borderRadius: 8,
        padding: 10,
        marginTop: 8,
    },
    rejectionReasonLabel: { fontSize: 12, fontWeight: '700', color: '#ef4444', marginBottom: 2 },
    rejectionReasonText: { fontSize: 13, color: '#333' },
    actionsRow: { flexDirection: 'row', gap: 10, marginTop: 12 },
    actionBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingVertical: 9,
        paddingHorizontal: 16,
        borderRadius: 8,
    },
    approveBtn: { backgroundColor: '#22c55e', flex: 1, justifyContent: 'center' },
    rejectBtn: { backgroundColor: '#ef4444', flex: 1, justifyContent: 'center' },
    actionBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.4)',
        justifyContent: 'center',
        padding: 24,
    },
    modalBox: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 24,
    },
    modalTitle: { fontSize: 18, fontWeight: 'bold', color: '#333', marginBottom: 4 },
    modalSubtitle: { fontSize: 14, color: '#666', marginBottom: 16 },
    modalLabel: { fontSize: 13, fontWeight: '600', color: '#555', marginBottom: 6 },
    reasonInput: {
        borderWidth: 1,
        borderColor: '#ddd',
        borderRadius: 8,
        padding: 10,
        fontSize: 14,
        color: '#333',
        minHeight: 80,
        textAlignVertical: 'top',
        marginBottom: 16,
    },
    modalActions: { flexDirection: 'row', gap: 10 },
    modalBtn: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: 'center',
    },
    modalCancelBtn: { backgroundColor: '#f0f0f0' },
    modalRejectBtn: { backgroundColor: '#ef4444' },
    modalCancelText: { color: '#555', fontWeight: '600' },
    modalRejectText: { color: '#fff', fontWeight: '600' },
});
