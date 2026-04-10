import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    View, Text, StyleSheet, SafeAreaView, FlatList, TextInput,
    KeyboardAvoidingView, Platform, Pressable, ActivityIndicator, Modal,
    TouchableOpacity, useWindowDimensions,
} from 'react-native';
import { theme } from '../../../shared/ui/theme/theme';
import * as Location from 'expo-location';
import { Ionicons } from '@expo/vector-icons';
import apiClient from '../../../shared/services/http/apiClient';
import { useAuth } from '../../../app/providers/AuthProvider';
import Toast from 'react-native-toast-message';
import MapPickerWeb from '../../home/ui/components/MapPickerWeb';
import { calculateDistanceInKm } from '../../../shared/utils/helpers'; // Haversine formula para calcular distancia entre 2 puntos

const pad2 = (n) => String(n).padStart(2, '0');
const formatHms = (t) => {
    const s = Math.max(0, Math.floor(t));
    return `${Math.floor(s / 3600)}:${pad2(Math.floor((s % 3600) / 60))}:${pad2(s % 60)}`;
};
const avatarColors = ['#dbeafe', '#fce7f3', '#fef3c7', '#d1fae5', '#ede9fe', '#e0e7ff'];
const ANSWERS_PAGE_SIZE = 5;
const SORT_LIKES_DESC = 'likes_desc';
const SORT_DATE_DESC = 'date_desc';
const parsePositiveNumber = (value) => {
    const num = Number(value);
    return Number.isFinite(num) && num > 0 ? num : null;
};
const REPORT_REASON_OPTIONS = [
    { value: 'OFFENSIVE', label: 'Offensive' },
    { value: 'SPAM', label: 'Spam' },
    { value: 'IRRELEVANT', label: 'Irrelevant' },
    { value: 'OTHER', label: 'Other' },
];

export default function QuestionThreadScreen({ route, navigation }) {
    const { questionId } = route?.params || {};
    const { user, isAuthenticated } = useAuth();
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;

    const [question, setQuestion] = useState(null);
    const [answers, setAnswers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [refreshingAnswers, setRefreshingAnswers] = useState(false);
    const [hasMoreAnswers, setHasMoreAnswers] = useState(true);
    const [answersPage, setAnswersPage] = useState(0);
    const [selectedSort, setSelectedSort] = useState(SORT_LIKES_DESC);
    const [error, setError] = useState(null);
    const [myVotes, setMyVotes] = useState({});
    const [draft, setDraft] = useState('');
    const [sendingAnswer, setSendingAnswer] = useState(false);
    const [userLocation, setUserLocation] = useState(null);
    const [pickMode, setPickMode] = useState(false);
    const [tempLat, setTempLat] = useState(null);
    const [tempLng, setTempLng] = useState(null);
    const [reportModalVisible, setReportModalVisible] = useState(false);
    const [reportTargetAnswerId, setReportTargetAnswerId] = useState(null);
    const [reportReason, setReportReason] = useState('OFFENSIVE');
    const [reportDescription, setReportDescription] = useState('');
    const [reportSubmitting, setReportSubmitting] = useState(false);
    const [reportedAnswers, setReportedAnswers] = useState({});
    const [questionReportModalVisible, setQuestionReportModalVisible] = useState(false);
    const [questionReportReason, setQuestionReportReason] = useState('OFFENSIVE');
    const [questionReportDescription, setQuestionReportDescription] = useState('');
    const [questionReportSubmitting, setQuestionReportSubmitting] = useState(false);
    const [questionReported, setQuestionReported] = useState(false);
    const inputRef = useRef(null);

    const getMinutesAgo = (d) => d ? Math.floor((Date.now() - new Date(d)) / 60000) : 0;

    const pickColor = useCallback((key) => {
        let hash = 0;
        const str = String(key || '');
        for (let i = 0; i < str.length; i++) {
            hash = (hash * 31 + str.charCodeAt(i)) | 0;
        }
        return avatarColors[Math.abs(hash) % avatarColors.length];
    }, []);

    const mapAnswer = useCallback((a) => ({
        id: a.id,
        author: a.user?.userName || a.user?.username || 'Anonymous',
        color: pickColor(a.id),
        text: a.content || '',
        likes: Number(a.upvotes) || 0,
        dislikes: Number(a.downvotes) || 0,
        minutesAgo: getMinutesAgo(a.createdAt),
        createdAt: a.createdAt || null,
        userId: a.user?.id,
        isVerified: a.isVerified,
    }), [pickColor]);

    const sortAnswersInMemory = useCallback((items, sort) => {
        const copy = [...items];
        if (sort === SORT_DATE_DESC) {
            copy.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
            return copy;
        }
        copy.sort((a, b) => {
            const likesDiff = (b.likes || 0) - (a.likes || 0);
            if (likesDiff !== 0) return likesDiff;
            return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime();
        });
        return copy;
    }, []);

    const fetchAnswers = useCallback(async ({ pageToLoad = 0, replace = false } = {}) => {
        if (!questionId) return;

        if (replace) {
            setRefreshingAnswers(true);
            setError(null);
        } else {
            setLoadingMore(true);
        }

        try {
            const aRes = await apiClient.get(
                `/api/v1/answers?questionId=${questionId}&sort=${selectedSort}&page=${pageToLoad}&size=${ANSWERS_PAGE_SIZE}`
            );
            const list = Array.isArray(aRes.data) ? aRes.data : Array.from(aRes.data || []);

            // Some environments may ignore page/size and return the full dataset.
            // In that case, paginate defensively on the client so UX is still correct.
            const offset = pageToLoad * ANSWERS_PAGE_SIZE;
            const end = offset + ANSWERS_PAGE_SIZE;
            const usingClientFallbackPaging = list.length > ANSWERS_PAGE_SIZE;
            const pageSlice = usingClientFallbackPaging ? list.slice(offset, end) : list;
            const mapped = pageSlice.map(mapAnswer);

            setAnswers((prev) => {
                if (replace) {
                    return sortAnswersInMemory(mapped, selectedSort);
                }

                const seen = new Set(prev.map((item) => item.id));
                const merged = [...prev, ...mapped.filter((item) => !seen.has(item.id))];
                return sortAnswersInMemory(merged, selectedSort);
            });

            setHasMoreAnswers(usingClientFallbackPaging ? end < list.length : mapped.length === ANSWERS_PAGE_SIZE);
            setAnswersPage(pageToLoad);
        } catch (e) {
            setError(e.message || 'Error loading answers');
        } finally {
            if (replace) {
                setRefreshingAnswers(false);
            } else {
                setLoadingMore(false);
            }
        }
    }, [mapAnswer, questionId, selectedSort, sortAnswersInMemory]);

    const canSend = useMemo(() => draft.trim().length > 0, [draft]);
    const questionRadiusKm = useMemo(() => parsePositiveNumber(question?.radiusKm), [question?.radiusKm]);
    const questionCoords = useMemo(() => {
        const qLat = Number(question?.location?.latitude);
        const qLng = Number(question?.location?.longitude);
        if (!Number.isFinite(qLat) || !Number.isFinite(qLng)) return null;
        return { latitude: qLat, longitude: qLng };
    }, [question?.location?.latitude, question?.location?.longitude]);
    const hasGeoRestriction = useMemo(
        () => questionCoords !== null && questionRadiusKm != null,
        [questionCoords, questionRadiusKm]
    );

    const isWithinRadius = useMemo(() => {
        if (!hasGeoRestriction) {
            return true;
        }
        if (!userLocation) return null;

        const distKm = calculateDistanceInKm(
            { latitude: userLocation.latitude, longitude: userLocation.longitude },
            questionCoords
        );

        return distKm <= questionRadiusKm;
    }, [hasGeoRestriction, questionCoords, questionRadiusKm, userLocation]);

    useEffect(() => {
        if (Platform.OS === 'web' && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (pos) => setUserLocation({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
                (error) => {
                    console.warn('[Geolocation] Failed:', error.code, error.message);

                    navigator.geolocation.getCurrentPosition(
                        (pos) => setUserLocation({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
                        (retryErr) => console.warn('[Geolocation] Retry failed:', retryErr.code, retryErr.message),
                        { enableHighAccuracy: true, timeout: 20000 }
                    );
                },
                { enableHighAccuracy: false, timeout: 8000, maximumAge: 180000 }
            );
        } else {
            (async () => {
                try {
                    const { status } = await Location.requestForegroundPermissionsAsync();
                    if (status === 'granted') {
                        const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
                        setUserLocation(loc.coords);
                    }
                } catch (_) { /* location unavailable */ }
            })();
        }
    }, []);

    useEffect(() => {
        if (!questionId) return;
        (async () => {
            try {
                setLoading(true);
                setError(null);
                const qRes = await apiClient.get(`/api/v1/questions/${questionId}`);
                setQuestion(qRes.data);
                // Cargar los votos previos del usuario para esta pregunta
                if (user?.id) {
                    try {
                        const vRes = await apiClient.get(`/api/v1/answers/votes?userId=${user.id}&questionId=${questionId}`);
                        // vRes.data es un mapa { answerId: 'LIKE'|'DISLIKE' }
                        if (vRes.data && typeof vRes.data === 'object') {
                            setMyVotes(vRes.data);
                        }
                    } catch (_) { /* ignorar si falla la carga de votos */ }
                }
            } catch (e) {
                setError(e.message || 'Error loading data');
            } finally {
                setLoading(false);
            }
        })();
    }, [questionId, user?.id]);

    useEffect(() => {
        if (!questionId) return;
        fetchAnswers({ pageToLoad: 0, replace: true });
    }, [fetchAnswers, questionId, selectedSort]);

    useEffect(() => {
        setQuestionReported(false);
        setQuestionReportModalVisible(false);
        setQuestionReportReason('OFFENSIVE');
        setQuestionReportDescription('');
        setQuestionReportSubmitting(false);
    }, [questionId]);

    const [timeLeft, setTimeLeft] = useState(0);
    useEffect(() => {
        if (!question?.expiresAt) return;
        const dateStr = String(question.expiresAt).replace(' ', 'T');

        const exp = new Date(dateStr).getTime();


        const tick = () => setTimeLeft(Math.max(0, Math.ceil((exp - Date.now()) / 1000)));
        tick();
        const id = setInterval(tick, 1000);
        return () => clearInterval(id);
    }, [question?.expiresAt]);

    const sendAnswerHandler = async () => {
        const content = draft.trim();
        if (!content || !question) return;

        const optimistic = { id: `tmp-${Date.now()}`, author: '@me', minutesAgo: 0, text: content, likes: 0, dislikes: 0, color: '#47d22b', optimistic: true };
        setAnswers((p) => sortAnswersInMemory([...p, optimistic], selectedSort));
        setDraft('');
        inputRef.current?.blur();
        setSendingAnswer(true);

        try {
            let loc = userLocation;
            if (!loc) {
                try {
                    if (Platform.OS === 'web' && navigator.geolocation) {
                        loc = await new Promise((resolve, reject) => {
                            navigator.geolocation.getCurrentPosition(
                                (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
                                () => reject(new Error('Location unavailable')),
                                { enableHighAccuracy: true, timeout: 8000 }
                            );
                        });
                        setUserLocation(loc);
                    } else {
                        const { status } = await Location.requestForegroundPermissionsAsync();
                        if (status === 'granted') {
                            const l = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
                            loc = l.coords;
                            setUserLocation(loc);
                        }
                    }
                } catch (_) {
                    // Location not available — send without it; backend will reject only if the question requires it
                }
            }

            const payload = {
                content,
                question: { id: question.id },
                user: { id: user?.id },
            };
            if (loc) {
                payload.userLocation = { latitude: loc.latitude, longitude: loc.longitude };
            }

            if (hasGeoRestriction) {
                if (!loc) {
                    setAnswers((p) => p.filter((a) => a.id !== optimistic.id));
                    setDraft(content);
                    Toast.show({
                        type: 'error',
                        text1: 'Location required',
                        text2: 'You must enable location to answer this question.',
                        position: 'top'
                    });
                    return;
                }
                const distKm = calculateDistanceInKm(
                    { latitude: loc.latitude, longitude: loc.longitude },
                    questionCoords
                );
                if (distKm > questionRadiusKm) {
                    setAnswers((p) => p.filter((a) => a.id !== optimistic.id));
                    setDraft(content);
                    Toast.show({
                        type: 'error',
                        text1: 'Out of range',
                        text2: 'You cannot answer this question because you are outside the allowed radius.',
                        position: 'top'
                    });
                    return;
                }
            }

            const res = await apiClient.post('/api/v1/answers', payload);

            const saved = res.data;
            setAnswers((p) => {
                const updated = p.map((a) => (a.id === optimistic.id ? {
                    id: saved.id,
                    author: saved.user?.userName || saved.user?.username || 'Anonymous',
                    color: theme.colors.primary,
                    text: saved.content,
                    likes: saved.upvotes || 0,
                    dislikes: saved.downvotes || 0,
                    minutesAgo: 0,
                    createdAt: saved.createdAt,
                    userId: saved.user?.id,
                    isVerified: saved.isVerified,
                } : a));
                return sortAnswersInMemory(updated, selectedSort);
            });
            await fetchAnswers({ pageToLoad: 0, replace: true });
        } catch (e) {
            setAnswers((p) => p.filter((a) => a.id !== optimistic.id));
            let msg = 'Error sending answer';
            if (e.response) {
                const s = e.response.status;
                const d = e.response.data;
                if (s === 400) msg = `Bad Request: ${d?.message || JSON.stringify(d)}`;
                else if (s === 401) msg = 'Unauthorized: Please log in again';
                else if (s === 403) msg = "Forbidden: You don't have permission";
                else msg = `Error (${s}): ${d?.message || e.message}`;
            } else msg = `Network error: ${e.message}`;

            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: msg,
                position: 'top'
            });
            setDraft(content);
        } finally {
            setSendingAnswer(false);
        }
    };

    const vote = async (answerId, type) => {
        const cur = myVotes[answerId];

        if (cur === type) {
            // Deselect: remove vote
            setAnswers((pa) => sortAnswersInMemory(pa.map((a) => {
                if (a.id !== answerId) return a;
                return {
                    ...a,
                    likes: type === 'LIKE' ? Math.max(0, (a.likes || 0) - 1) : (a.likes || 0),
                    dislikes: type === 'DISLIKE' ? Math.max(0, (a.dislikes || 0) - 1) : (a.dislikes || 0),
                };
            }), selectedSort));
            setMyVotes((prev) => { const n = { ...prev }; delete n[answerId]; return n; });
            try {
                await apiClient.delete(`/api/v1/answers/${answerId}/votes?userId=${user.id}`);
            } catch (error) {
                console.error('Error removing vote:', error);
            }
            return;
        }

        // Optimistic update (new vote or vote change)
        setAnswers((pa) => sortAnswersInMemory(pa.map((a) => {
            if (a.id !== answerId) return a;
            const newLikes = type === 'LIKE'
                ? (a.likes || 0) + 1
                : cur === 'LIKE' ? Math.max(0, (a.likes || 0) - 1) : (a.likes || 0);
            const newDislikes = type === 'DISLIKE'
                ? (a.dislikes || 0) + 1
                : cur === 'DISLIKE' ? Math.max(0, (a.dislikes || 0) - 1) : (a.dislikes || 0);
            return { ...a, likes: newLikes, dislikes: newDislikes };
        }), selectedSort));
        setMyVotes((prev) => ({ ...prev, [answerId]: type }));

        try {
            await apiClient.put(
                `/api/v1/answers/${answerId}/votes?userId=${user.id}&voteType=${type}`
            );
        } catch (error) {
            console.error('Error voting:', error);
        }
    };

    const openReportModal = (answerId) => {
        setReportTargetAnswerId(answerId);
        setReportReason('OFFENSIVE');
        setReportDescription('');
        setReportModalVisible(true);
    };

    const closeReportModal = () => {
        if (reportSubmitting) {
            return;
        }
        setReportModalVisible(false);
        setReportTargetAnswerId(null);
        setReportDescription('');
    };

    const submitAnswerReport = async () => {
        if (!reportTargetAnswerId || reportSubmitting) {
            return;
        }

        try {
            setReportSubmitting(true);
            await apiClient.post('/api/v1/reports/answers', {
                answerId: reportTargetAnswerId,
                reason: reportReason,
                description: reportDescription?.trim() || null,
            });

            setReportedAnswers((prev) => ({ ...prev, [reportTargetAnswerId]: true }));
            setReportModalVisible(false);
            setReportTargetAnswerId(null);
            setReportDescription('');
            Toast.show({
                type: 'success',
                text1: 'Report sent',
                text2: 'Thanks for helping keep the community safe.',
                position: 'top',
            });
        } catch (e) {
            const status = e?.response?.status;
            if (status === 409) {
                setReportedAnswers((prev) => ({ ...prev, [reportTargetAnswerId]: true }));
            }

            Toast.show({
                type: 'error',
                text1: status === 409 ? 'Already reported' : 'Could not send report',
                text2: status === 409
                    ? 'You already reported this answer before.'
                    : e?.response?.data?.message || e?.message || 'Try again in a few seconds.',
                position: 'top',
            });
        } finally {
            setReportSubmitting(false);
        }
    };

    const openQuestionReportModal = () => {
        if (questionReportSubmitting || questionReported) {
            return;
        }
        setQuestionReportReason('OFFENSIVE');
        setQuestionReportDescription('');
        setQuestionReportModalVisible(true);
    };

    const closeQuestionReportModal = () => {
        if (questionReportSubmitting) {
            return;
        }
        setQuestionReportModalVisible(false);
        setQuestionReportDescription('');
    };

    const submitQuestionReport = async () => {
        if (!question?.id || questionReportSubmitting || questionReported) {
            return;
        }

        if (!questionReportReason) {
            Toast.show({
                type: 'error',
                text1: 'Reason required',
                text2: 'Select a reason to report the question.',
                position: 'top',
            });
            return;
        }

        if (questionReportDescription?.length > 500) {
            Toast.show({
                type: 'error',
                text1: 'Description too long',
                text2: 'Description cannot exceed 500 characters.',
                position: 'top',
            });
            return;
        }

        try {
            setQuestionReportSubmitting(true);
            await apiClient.post('/api/v1/reports/questions', {
                questionId: question.id,
                reason: questionReportReason,
                description: questionReportDescription?.trim() || null,
            });

            setQuestionReported(true);
            setQuestionReportModalVisible(false);
            setQuestionReportDescription('');
            Toast.show({
                type: 'success',
                text1: 'Report sent successfully',
                text2: 'Thanks for helping keep the community safe.',
                position: 'top',
            });
        } catch (e) {
            const status = e?.response?.status;
            if (status === 409) {
                setQuestionReported(true);
            }

            Toast.show({
                type: 'error',
                text1: status === 409 ? 'You already reported this question' : 'Could not send report',
                text2: status === 409
                    ? 'You already reported this question before.'
                    : e?.response?.data?.message || e?.message || 'Try again in a few seconds.',
                position: 'top',
            });
        } finally {
            setQuestionReportSubmitting(false);
        }
    };

    const cancelMapPick = () => { setPickMode(false); setTempLat(null); setTempLng(null); };
    const confirmMapPick = () => {
        if (typeof tempLat !== 'number' || typeof tempLng !== 'number') {
            Toast.show({
                type: 'info',
                text1: 'Select a point',
                text2: 'Tap on the map to choose your location.',
                position: 'top'
            });
            return;
        }
        setUserLocation({ latitude: tempLat, longitude: tempLng });
        setPickMode(false);
    };

    const renderAnswer = ({ item, index }) => {
        const v = myVotes[item.id];
        const isLast = index === answers.length - 1;
        const alreadyReported = reportedAnswers[item.id] === true;
        return (
            <View style={styles.threadRow}>
                {/* Thread line + dot */}
                <View style={styles.threadLineCol}>
                    <View style={[styles.threadDot, { backgroundColor: item.color }]} />
                    {!isLast && <View style={styles.threadLine} />}
                </View>

                {/* Content */}
                <View style={styles.threadContent}>
                    <View style={styles.threadHeader}>
                        <Text style={styles.threadAuthor}>@{item.author}</Text>
                        <Text style={styles.threadTime}>{item.minutesAgo < 60 ? `${item.minutesAgo}m ago` : `${Math.floor(item.minutesAgo / 60)}h ago`}</Text>
                        <Text style={styles.threadIndex}>#{index + 1}</Text>
                    </View>
                    <Text style={styles.threadBody}>{item.text}</Text>
                    <View style={styles.threadActions}>
                        <Pressable onPress={() => vote(item.id, 'LIKE')} style={[styles.threadVoteBtn, v === 'DISLIKE' && { opacity: 0.3 }]}>
                            <Ionicons name={v === 'LIKE' ? 'thumbs-up' : 'thumbs-up-outline'} size={18} color={v === 'LIKE' ? '#22c55e' : '#9ca3af'} />
                            <Text style={[styles.threadVoteCount, v === 'LIKE' && { color: '#22c55e' }]}>{item.likes || 0}</Text>
                        </Pressable>
                        <Pressable onPress={() => vote(item.id, 'DISLIKE')} style={[styles.threadVoteBtn, v === 'LIKE' && { opacity: 0.3 }]}>
                            <Ionicons name={v === 'DISLIKE' ? 'thumbs-down' : 'thumbs-down-outline'} size={18} color={v === 'DISLIKE' ? '#ef4444' : '#9ca3af'} />
                            <Text style={[styles.threadVoteCount, v === 'DISLIKE' && { color: '#ef4444' }]}>{item.dislikes || 0}</Text>
                        </Pressable>
                        <Pressable
                            onPress={() => openReportModal(item.id)}
                            style={[styles.threadReportBtn, alreadyReported && styles.threadReportBtnDisabled]}
                            disabled={alreadyReported}
                        >
                            <Ionicons
                                name={alreadyReported ? 'flag' : 'flag-outline'}
                                size={16}
                                color={alreadyReported ? '#9ca3af' : '#b91c1c'}
                            />
                            <Text style={[styles.threadReportText, alreadyReported && styles.threadReportTextDisabled]}>
                                {alreadyReported ? 'Reported' : 'Report'}
                            </Text>
                        </Pressable>
                    </View>
                </View>
            </View>
        );
    };

    const loadMoreAnswers = () => {
        if (loading || loadingMore || refreshingAnswers || !hasMoreAnswers) return;
        fetchAnswers({ pageToLoad: answersPage + 1, replace: false });
    };

    if (pickMode) {
        return (
            <SafeAreaView style={styles.screen}>
                <View style={{ flex: 1 }}>
                    <View style={{ flex: 1, width: '100%', height: '100%' }}>
                        <MapPickerWeb
                            latitude={tempLat} longitude={tempLng}
                            userLat={userLocation?.latitude} userLng={userLocation?.longitude}
                            radiusKm={questionRadiusKm} pickEnabled tempLat={tempLat} tempLng={tempLng}
                            onPick={(lat, lng) => { setTempLat(lat); setTempLng(lng); }}
                        />
                    </View>
                    <View style={styles.mapOverlay} pointerEvents="box-none">
                        <View style={styles.mapHint} pointerEvents="none">
                            <Text style={styles.mapHintText}>Tap the map to pick your location</Text>
                            <Text style={styles.mapHintCoords}>
                                {tempLat?.toFixed?.(5) ?? '--'}, {tempLng?.toFixed?.(5) ?? '--'}
                            </Text>
                        </View>
                        <View style={styles.mapBtnRow} pointerEvents="auto">
                            <TouchableOpacity style={styles.mapCancelBtn} onPress={cancelMapPick} activeOpacity={0.8}>
                                <Text style={styles.mapCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.mapOkBtn} onPress={confirmMapPick} activeOpacity={0.8}>
                                <Text style={styles.mapOkText}>Confirm</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.screen}>
            <KeyboardAvoidingView
                style={{ flex: 1 }}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 80 : 0}
            >
                {loading && (
                    <View style={styles.center}>
                        <ActivityIndicator size="large" color={theme.colors.accent} />
                    </View>
                )}

                {error && !loading && (
                    <View style={styles.center}>
                        <Text style={{ color: '#ef4444', fontSize: 15, textAlign: 'center', paddingHorizontal: 20 }}>{error}</Text>
                    </View>
                )}

                {!loading && !error && question && (
                    <>
                        {/* ── Header Card ── */}
                        <View style={[styles.headerCard, isNarrow && { marginHorizontal: 12, marginTop: 12 }]}>
                            {/* Back button */}
                            {navigation && (
                                <TouchableOpacity
                                    style={styles.backBtn}
                                    onPress={() => navigation.goBack()}
                                    activeOpacity={0.7}
                                >
                                    <Ionicons name="chevron-back" size={22} color="#b91c1c" />
                                </TouchableOpacity>
                            )}

                            <View style={styles.headerContent}>
                                <View style={styles.qAvatar}>
                                    <Ionicons name="chatbubble-ellipses" size={20} color="#fff" />
                                </View>
                                <View style={{ flex: 1 }}>
                                    <Text style={styles.qTitle}>{question.title}</Text>
                                    <Text style={styles.qBody}>{question.content}</Text>
                                    <Text style={styles.qAuthor}>by @{question.creator?.userName || question.creator?.username || 'unknown'}</Text>
                                </View>
                            </View>

                            <View style={styles.chips}>
                                <View style={styles.chip}>
                                    <Ionicons name="chatbubbles-outline" size={14} color="#f59e0b" />
                                    <Text style={styles.chipVal}>{answers.length}</Text>
                                </View>
                                <View style={styles.chip}>
                                    <Ionicons name="time-outline" size={14} color="#1f2937" />
                                    <Text style={styles.chipVal}>{formatHms(timeLeft)}</Text>
                                </View>
                            </View>

                            {isAuthenticated && (
                                <View style={styles.questionReportRow}>
                                    <TouchableOpacity
                                        onPress={openQuestionReportModal}
                                        style={[styles.questionReportBtn, questionReported && styles.questionReportBtnDisabled]}
                                        activeOpacity={0.85}
                                        disabled={questionReported || questionReportSubmitting}
                                    >
                                        <Ionicons
                                            name={questionReported ? 'flag' : 'flag-outline'}
                                            size={16}
                                            color={questionReported ? '#9ca3af' : '#b91c1c'}
                                        />
                                        <Text style={[styles.questionReportText, questionReported && styles.questionReportTextDisabled]}>
                                            {questionReported ? 'Reported' : 'Report'}
                                        </Text>
                                    </TouchableOpacity>
                                </View>
                            )}
                        </View>

                        <View style={[styles.answersToolbar, isNarrow && { marginHorizontal: 12 }]}>
                            <Text style={styles.answersToolbarTitle}>Answers</Text>
                            <View style={styles.sortSwitch}>
                                <Pressable
                                    style={[styles.sortOption, selectedSort === SORT_LIKES_DESC && styles.sortOptionActive]}
                                    onPress={() => setSelectedSort(SORT_LIKES_DESC)}
                                >
                                    <Text style={[styles.sortOptionLabel, selectedSort === SORT_LIKES_DESC && styles.sortOptionLabelActive]}>Top</Text>
                                </Pressable>
                                <Pressable
                                    style={[styles.sortOption, selectedSort === SORT_DATE_DESC && styles.sortOptionActive]}
                                    onPress={() => setSelectedSort(SORT_DATE_DESC)}
                                >
                                    <Text style={[styles.sortOptionLabel, selectedSort === SORT_DATE_DESC && styles.sortOptionLabelActive]}>Newest</Text>
                                </Pressable>
                            </View>
                        </View>

                        {/* ── Answers ── */}
                        <FlatList
                            data={answers}
                            keyExtractor={(i) => String(i.id)}
                            contentContainerStyle={[styles.listContent, isNarrow && { paddingHorizontal: 12 }]}
                            renderItem={renderAnswer}
                            ListFooterComponent={
                                <View style={styles.listFooterWrap}>
                                    {loadingMore && (
                                        <ActivityIndicator style={styles.listFooterLoader} size="small" color={theme.colors.accent} />
                                    )}
                                    {!loadingMore && hasMoreAnswers && answers.length > 0 && (
                                        <Pressable style={styles.loadMoreBtn} onPress={loadMoreAnswers}>
                                            <Text style={styles.loadMoreText}>Load more</Text>
                                        </Pressable>
                                    )}
                                </View>
                            }
                            ListEmptyComponent={
                                refreshingAnswers
                                    ? null
                                    : <Text style={styles.emptyText}>No answers yet. Be the first!</Text>
                            }
                        />
                    </>
                )}

                {/* ── Composer ── */}
                {!loading && !error && (
                    hasGeoRestriction && isWithinRadius !== true ? (
                        <View style={styles.outOfRange}>
                            <Ionicons name="location-outline" size={18} color="#ef4444" />
                            <Text style={styles.outOfRangeText}>
                                {isWithinRadius === false
                                    ? 'You are not in the question area. Move closer to answer.'
                                    : 'Your location could not be validated to answer this question.'}
                            </Text>
                        </View>
                    ) : (
                        <View style={[styles.composer, isNarrow && { paddingHorizontal: 12 }]}>
                            <TextInput
                                ref={inputRef}
                                value={draft}
                                onChangeText={setDraft}
                                placeholder="Write your answer..."
                                placeholderTextColor="#9ca3af"
                                style={styles.composerInput}
                                editable={isWithinRadius !== false && !sendingAnswer}
                            />
                            <Pressable
                                onPress={sendAnswerHandler}
                                disabled={!canSend}
                                style={[styles.sendBtn, !canSend && { opacity: 0.4 }]}
                            >
                                {sendingAnswer
                                    ? <ActivityIndicator size="small" color="#fff" />
                                    : <Ionicons name="send" size={18} color="#fff" />
                                }
                            </Pressable>
                        </View>
                    )
                )}
            </KeyboardAvoidingView>

            <Modal
                visible={questionReportModalVisible}
                transparent
                animationType="fade"
                onRequestClose={closeQuestionReportModal}
            >
                <View style={styles.reportModalOverlay}>
                    <View style={styles.reportModalCard}>
                        <Text style={styles.reportModalTitle}>Report question</Text>
                        <Text style={styles.reportModalSubtitle}>Why are you reporting this question?</Text>

                        <View style={styles.reportReasonWrap}>
                            {REPORT_REASON_OPTIONS.map((option) => {
                                const selected = questionReportReason === option.value;
                                return (
                                    <Pressable
                                        key={option.value}
                                        onPress={() => setQuestionReportReason(option.value)}
                                        style={[styles.reportReasonChip, selected && styles.reportReasonChipSelected]}
                                    >
                                        <Text
                                            style={[styles.reportReasonChipText, selected && styles.reportReasonChipTextSelected]}
                                        >
                                            {option.label}
                                        </Text>
                                    </Pressable>
                                );
                            })}
                        </View>

                        <Text style={styles.reportDescriptionLabel}>Description (optional)</Text>
                        <TextInput
                            value={questionReportDescription}
                            onChangeText={setQuestionReportDescription}
                            placeholder="Add more context to help moderation"
                            placeholderTextColor="#9ca3af"
                            multiline
                            maxLength={500}
                            style={styles.reportDescriptionInput}
                            editable={!questionReportSubmitting}
                        />

                        <View style={styles.reportModalActions}>
                            <TouchableOpacity
                                onPress={closeQuestionReportModal}
                                style={styles.reportCancelBtn}
                                activeOpacity={0.85}
                                disabled={questionReportSubmitting}
                            >
                                <Text style={styles.reportCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                onPress={submitQuestionReport}
                                style={[styles.reportSubmitBtn, questionReportSubmitting && styles.reportSubmitBtnDisabled]}
                                activeOpacity={0.85}
                                disabled={questionReportSubmitting}
                            >
                                {questionReportSubmitting ? (
                                    <ActivityIndicator size="small" color="#fff" />
                                ) : (
                                    <Text style={styles.reportSubmitText}>Submit report</Text>
                                )}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                visible={reportModalVisible}
                transparent
                animationType="fade"
                onRequestClose={closeReportModal}
            >
                <View style={styles.reportModalOverlay}>
                    <View style={styles.reportModalCard}>
                        <Text style={styles.reportModalTitle}>Report answer</Text>
                        <Text style={styles.reportModalSubtitle}>Why are you reporting this answer?</Text>

                        <View style={styles.reportReasonWrap}>
                            {REPORT_REASON_OPTIONS.map((option) => {
                                const selected = reportReason === option.value;
                                return (
                                    <Pressable
                                        key={option.value}
                                        onPress={() => setReportReason(option.value)}
                                        style={[styles.reportReasonChip, selected && styles.reportReasonChipSelected]}
                                    >
                                        <Text
                                            style={[styles.reportReasonChipText, selected && styles.reportReasonChipTextSelected]}
                                        >
                                            {option.label}
                                        </Text>
                                    </Pressable>
                                );
                            })}
                        </View>

                        <Text style={styles.reportDescriptionLabel}>Description (optional)</Text>
                        <TextInput
                            value={reportDescription}
                            onChangeText={setReportDescription}
                            placeholder="Add more context to help moderation"
                            placeholderTextColor="#9ca3af"
                            multiline
                            maxLength={500}
                            style={styles.reportDescriptionInput}
                            editable={!reportSubmitting}
                        />

                        <View style={styles.reportModalActions}>
                            <TouchableOpacity
                                onPress={closeReportModal}
                                style={styles.reportCancelBtn}
                                activeOpacity={0.85}
                                disabled={reportSubmitting}
                            >
                                <Text style={styles.reportCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                onPress={submitAnswerReport}
                                style={[styles.reportSubmitBtn, reportSubmitting && styles.reportSubmitBtnDisabled]}
                                activeOpacity={0.85}
                                disabled={reportSubmitting}
                            >
                                {reportSubmitting ? (
                                    <ActivityIndicator size="small" color="#fff" />
                                ) : (
                                    <Text style={styles.reportSubmitText}>Submit report</Text>
                                )}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f3f4f6',
    },
    center: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },

    /* ── Header Card ── */
    headerCard: {
        backgroundColor: '#fff',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 20,
        padding: 20,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.06,
        shadowRadius: 16,
        elevation: 4,
    },
    backBtn: {
        width: 36,
        height: 36,
        borderRadius: 10,
        backgroundColor: '#f3f4f6',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 12,
    },
    headerContent: {
        flexDirection: 'row',
        gap: 14,
    },
    qAvatar: {
        width: 42,
        height: 42,
        borderRadius: 14,
        backgroundColor: '#b91c1c',
        alignItems: 'center',
        justifyContent: 'center',
    },
    qTitle: {
        fontSize: 18,
        fontWeight: '800',
        color: '#1f2937',
    },
    qBody: {
        fontSize: 14,
        color: '#6b7280',
        marginTop: 4,
        lineHeight: 20,
    },
    qAuthor: {
        fontSize: 12,
        color: '#9ca3af',
        marginTop: 6,
    },
    chips: {
        flexDirection: 'row',
        gap: 10,
        marginTop: 14,
    },
    chip: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        backgroundColor: '#f3f4f6',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 20,
    },
    chipVal: {
        fontSize: 13,
        fontWeight: '700',
        color: '#374151',
    },
    questionReportRow: {
        marginTop: 12,
        alignItems: 'flex-start',
    },
    questionReportBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 999,
        borderWidth: 1,
        borderColor: '#fecaca',
        backgroundColor: '#fff1f2',
    },
    questionReportBtnDisabled: {
        borderColor: '#e5e7eb',
        backgroundColor: '#f3f4f6',
    },
    questionReportText: {
        fontSize: 12,
        fontWeight: '700',
        color: '#b91c1c',
    },
    questionReportTextDisabled: {
        color: '#9ca3af',
    },

    answersToolbar: {
        marginHorizontal: 16,
        marginTop: 12,
        marginBottom: 4,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    answersToolbarTitle: {
        fontSize: 15,
        fontWeight: '700',
        color: '#374151',
    },
    sortSwitch: {
        flexDirection: 'row',
        backgroundColor: '#e5e7eb',
        borderRadius: 12,
        padding: 3,
        gap: 4,
    },
    sortOption: {
        minWidth: 74,
        paddingVertical: 6,
        paddingHorizontal: 10,
        borderRadius: 9,
        alignItems: 'center',
    },
    sortOptionActive: {
        backgroundColor: '#b91c1c',
    },
    sortOptionLabel: {
        fontSize: 12,
        fontWeight: '700',
        color: '#4b5563',
    },
    sortOptionLabelActive: {
        color: '#fff',
    },

    /* ── Answer List (Forum Thread) ── */
    listContent: {
        padding: 16,
        paddingBottom: 90,
    },
    emptyText: {
        textAlign: 'center',
        color: '#9ca3af',
        marginTop: 24,
        fontSize: 14,
    },
    listFooterLoader: {
        marginTop: 8,
        marginBottom: 12,
    },
    listFooterWrap: {
        paddingBottom: 12,
    },
    loadMoreBtn: {
        alignSelf: 'center',
        marginTop: 8,
        paddingVertical: 10,
        paddingHorizontal: 16,
        borderRadius: 10,
        backgroundColor: '#b91c1c',
    },
    loadMoreText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 13,
    },
    threadRow: {
        flexDirection: 'row',
        minHeight: 60,
    },
    threadLineCol: {
        width: 28,
        alignItems: 'center',
    },
    threadDot: {
        width: 12,
        height: 12,
        borderRadius: 6,
        borderWidth: 2,
        borderColor: '#fff',
        marginTop: 4,
        zIndex: 1,
    },
    threadLine: {
        flex: 1,
        width: 2,
        backgroundColor: '#e5e7eb',
        marginTop: 2,
    },
    threadContent: {
        flex: 1,
        marginLeft: 10,
        marginBottom: 16,
        backgroundColor: '#fff',
        borderRadius: 14,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        padding: 12,
    },
    threadHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        marginBottom: 6,
    },
    threadAuthor: {
        fontSize: 13,
        fontWeight: '700',
        color: '#b91c1c',
    },
    threadTime: {
        fontSize: 11,
        color: '#9ca3af',
    },
    threadIndex: {
        fontSize: 11,
        color: '#d1d5db',
        fontWeight: '700',
        marginLeft: 'auto',
    },
    threadBody: {
        fontSize: 14,
        color: '#1f2937',
        lineHeight: 20,
    },
    threadActions: {
        flexDirection: 'row',
        gap: 16,
        marginTop: 8,
        paddingTop: 8,
        borderTopWidth: 1,
        borderTopColor: '#f3f4f6',
    },
    threadVoteBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    threadVoteCount: {
        fontSize: 12,
        fontWeight: '600',
        color: '#6b7280',
    },
    threadReportBtn: {
        marginLeft: 'auto',
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        paddingHorizontal: 10,
        paddingVertical: 5,
        borderRadius: 999,
        backgroundColor: '#fef2f2',
        borderWidth: 1,
        borderColor: '#fecaca',
    },
    threadReportBtnDisabled: {
        backgroundColor: '#f9fafb',
        borderColor: '#e5e7eb',
    },
    threadReportText: {
        fontSize: 12,
        fontWeight: '700',
        color: '#b91c1c',
    },
    threadReportTextDisabled: {
        color: '#9ca3af',
    },

    reportModalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.5)',
        justifyContent: 'center',
        paddingHorizontal: 18,
    },
    reportModalCard: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 18,
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    reportModalTitle: {
        fontSize: 19,
        fontWeight: '800',
        color: '#111827',
    },
    reportModalSubtitle: {
        marginTop: 4,
        fontSize: 13,
        color: '#6b7280',
    },
    reportReasonWrap: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
        marginTop: 14,
    },
    reportReasonChip: {
        borderWidth: 1,
        borderColor: '#d1d5db',
        borderRadius: 999,
        paddingHorizontal: 12,
        paddingVertical: 8,
        backgroundColor: '#fff',
    },
    reportReasonChipSelected: {
        backgroundColor: '#fee2e2',
        borderColor: '#fca5a5',
    },
    reportReasonChipText: {
        fontSize: 13,
        color: '#374151',
        fontWeight: '700',
    },
    reportReasonChipTextSelected: {
        color: '#991b1b',
    },
    reportDescriptionLabel: {
        marginTop: 16,
        fontSize: 13,
        fontWeight: '700',
        color: '#374151',
    },
    reportDescriptionInput: {
        marginTop: 8,
        minHeight: 86,
        borderWidth: 1,
        borderColor: '#d1d5db',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,
        textAlignVertical: 'top',
        fontSize: 14,
        color: '#111827',
        backgroundColor: '#f9fafb',
        outlineStyle: 'none',
    },
    reportModalActions: {
        marginTop: 16,
        flexDirection: 'row',
        gap: 10,
    },
    reportCancelBtn: {
        flex: 1,
        borderWidth: 1,
        borderColor: '#d1d5db',
        borderRadius: 12,
        paddingVertical: 12,
        alignItems: 'center',
        backgroundColor: '#fff',
    },
    reportCancelText: {
        color: '#374151',
        fontSize: 14,
        fontWeight: '700',
    },
    reportSubmitBtn: {
        flex: 1,
        borderRadius: 12,
        paddingVertical: 12,
        alignItems: 'center',
        backgroundColor: '#b91c1c',
    },
    reportSubmitBtnDisabled: {
        opacity: 0.75,
    },
    reportSubmitText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '800',
    },

    /* ── Composer ── */
    composer: {
        position: 'absolute',
        left: 0,
        right: 0,
        bottom: 0,
        paddingHorizontal: 16,
        paddingVertical: 10,
        flexDirection: 'row',
        gap: 10,
        backgroundColor: '#fff',
        borderTopWidth: 1,
        borderTopColor: '#e5e7eb',
    },
    composerInput: {
        flex: 1,
        borderWidth: 1.5,
        borderColor: '#e5e7eb',
        borderRadius: 14,
        backgroundColor: '#f9fafb',
        paddingHorizontal: 16,
        paddingVertical: 10,
        fontSize: 15,
        color: '#1f2937',
        outlineStyle: 'none',
    },
    sendBtn: {
        width: 46,
        height: 46,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#dc2626',
        shadowColor: '#b91c1c',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.25,
        shadowRadius: 10,
        elevation: 4,
    },
    locationBtn: {
        width: 46,
        height: 46,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f3f4f6',
        borderWidth: 1.5,
        borderColor: '#e5e7eb',
    },
    mapOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, padding: 16, zIndex: 100, justifyContent: 'space-between' },
    mapHint: { backgroundColor: 'rgba(255,255,255,0.95)', padding: 16, borderRadius: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.12, shadowRadius: 12, elevation: 6 },
    mapHintText: { fontWeight: '700', fontSize: 15, color: '#1f2937', textAlign: 'center' },
    mapHintCoords: { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 6 },
    mapBtnRow: { flexDirection: 'row', gap: 12 },
    mapCancelBtn: { flex: 1, backgroundColor: '#fff', borderRadius: 14, paddingVertical: 14, alignItems: 'center', borderWidth: 1, borderColor: '#e5e7eb', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 6, elevation: 3 },
    mapCancelText: { fontWeight: '700', fontSize: 15, color: '#6b7280' },
    mapOkBtn: { flex: 1, backgroundColor: '#dc2626', borderRadius: 14, paddingVertical: 14, alignItems: 'center', shadowColor: '#b91c1c', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 12, elevation: 5 },
    mapOkText: { fontWeight: '700', fontSize: 15, color: '#fff' },
    // Geolocation: estilo del banner de "fuera del radio"
    outOfRange: {
        // Banner rojo con icono de ubicación, se muestra cuando usuario está fuera del radio permitido.
        flexDirection: 'row', alignItems: 'center', gap: 10,
        margin: 12, padding: 14,
        backgroundColor: '#fef2f2', borderRadius: 14, // Fondo rojo claro
        borderWidth: 1, borderColor: '#fecaca', // Borde rojo
    },
    outOfRangeText: { flex: 1, fontSize: 13, fontWeight: '600', color: '#ef4444', lineHeight: 18 }, // Texto rojo explicativo
});
