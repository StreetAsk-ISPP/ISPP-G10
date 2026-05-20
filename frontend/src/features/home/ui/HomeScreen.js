import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useFocusEffect, useIsFocused } from '@react-navigation/native';
import {
    View,
    Text,
    StyleSheet,
    SafeAreaView,
    TouchableOpacity,
    Switch,
    useWindowDimensions,
    Modal,
    Pressable,
    Platform,
    TextInput,
    Alert,
    ActivityIndicator,
    Image,
    Animated,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import MapComponent from './components/MapComponent';
import QuestionsSidebar from './components/QuestionsSidebar';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useNotifications } from '../../../app/providers/NotificationProvider';
import ConfirmationModal from '../../../shared/components/ConfirmationModal';
import { APP_CONFIG } from '../../../app/config/config';
import apiClient from '../../../shared/services/http/apiClient';
import { bootstrapWebPushNotifications } from '../../../shared/services/notifications/webPushBootstrap';
import { updateWebPushZone } from '../../../shared/services/notifications/webPushService';
import { resolveZoneKey } from '../../../shared/services/notifications/zoneService';
import { STORAGE_KEYS } from '../../../shared/constants/storageKeys';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const readCachedArray = (key) => {
    if (Platform.OS !== 'web' || typeof window === 'undefined') {
        return [];
    }

    const raw = window.localStorage.getItem(key);
    if (!raw) {
        return [];
    }

    try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch {
        return [];
    }
};

const writeCachedArray = (key, value) => {
    if (Platform.OS !== 'web' || typeof window === 'undefined' || !Array.isArray(value) || value.length === 0) {
        return;
    }

    window.localStorage.setItem(key, JSON.stringify(value));
};

export default function HomeScreen({ navigation, route }) {
    const { logout, token, user } = useAuth();
    const { ephemeralNotification, observeNotifications } = useNotifications();
    const { width } = useWindowDimensions();
    const isNarrow = width < 500;
    const isBusinessUser = Array.isArray(user?.roles) && user.roles.includes('BUSINESS');

    const [questions, setQuestions] = useState([]);
    const [events, setEvents] = useState([]);
    const [showQuestions, setShowQuestions] = useState(!isBusinessUser);
    const [currentLocation, setCurrentLocation] = useState(null);
    const [isPremium, setIsPremium] = useState(false);
    const [sidebarTab, setSidebarTab] = useState('QUESTIONS');

    // null = revisando, true = concedido, false = denegado
    const [hasLocationPermission, setHasLocationPermission] = useState(null);
    const [mapKey, setMapKey] = useState(0);

    const [feedbackVisible, setFeedbackVisible] = useState(false);
    const [feedbackType, setFeedbackType] = useState('SUGGESTION');
    const [feedbackMessage, setFeedbackMessage] = useState('');
    const [sendingFeedback, setSendingFeedback] = useState(false);
    const [feedbackSuccessVisible, setFeedbackSuccessVisible] = useState(false);
    const [feedbackSuccessMessage, setFeedbackSuccessMessage] = useState('');
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const [streetCoinsNotice, setStreetCoinsNotice] = useState('');
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [mapCenter, setMapCenter] = useState(null);
    const [visibleQuestionsIds, setVisibleQuestionsIds] = useState([]);
    const [selectedEventTarget, setSelectedEventTarget] = useState(null);

    const handleLogoutConfirm = async () => {
        setShowLogoutModal(false);
        await logout();
    };

    const pushBootstrappedRef = useRef(false);
    const pushSubscriptionRef = useRef(null);
    const menuAnimValue = useRef(new Animated.Value(-350)).current;
    const pushZoneKeyRef = useRef(null);
    const coinsToastAnim = useRef(new Animated.Value(0)).current;

    const isFocused = useIsFocused();
    const latestRequestRef = useRef(0);
    const isBusiness = Array.isArray(user?.roles) && user.roles.includes('BUSINESS');
    const latestEventsRequestRef = useRef(0);
    const bboxFetchTimeoutRef = useRef(null);
    const eventsBboxFetchTimeoutRef = useRef(null);

    const handleEventAttendanceUpdate = useCallback((updatedEvent) => {
        if (!updatedEvent?.id) {
            return;
        }

        setEvents((currentEvents) => {
            const nextEvents = currentEvents.map((event) => (
                event.id === updatedEvent.id ? updatedEvent : event
            ));
            writeCachedArray(STORAGE_KEYS.HOME_EVENTS_CACHE, nextEvents);
            return nextEvents;
        });
    }, []);

    const handleSidebarNavigateToEvent = useCallback((eventItem, coords) => {
        if (!coords) {
            return;
        }

        setSelectedEventTarget({
            id: eventItem?.id,
            lat: coords.lat,
            lng: coords.lng,
            title: eventItem?.title || 'Event',
        });
        setSidebarVisible(false);
    }, []);

    const loadQuestions = useCallback(async () => {
        const requestId = ++latestRequestRef.current;

        for (let attempt = 1; attempt <= 3; attempt += 1) {
            try {
                const res = await apiClient.get('/api/v1/questions/compact');
                const raw = Array.isArray(res?.data) ? res.data : [];

                if (requestId !== latestRequestRef.current) {
                    return;
                }

                setQuestions(raw);
                writeCachedArray(STORAGE_KEYS.HOME_QUESTIONS_CACHE, raw);
                return;
            } catch (e) {
                if (attempt === 3) {
                    if (requestId !== latestRequestRef.current) {
                        return;
                    }

                    const cachedQuestions = readCachedArray(STORAGE_KEYS.HOME_QUESTIONS_CACHE);
                    if (cachedQuestions.length > 0) {
                        setQuestions(cachedQuestions);
                    }
                    console.warn('Failed to load questions', e);
                    return;
                }

                await sleep(250 * attempt);
            }
        }
    }, []);

    const loadEvents = useCallback(async () => {
        const requestId = ++latestEventsRequestRef.current;

        for (let attempt = 1; attempt <= 3; attempt += 1) {
            try {
                const res = await apiClient.get('/api/v1/events?active=true');
                const raw = Array.isArray(res?.data) ? res.data : [];

                if (requestId !== latestEventsRequestRef.current) {
                    return;
                }

                setEvents(raw);
                writeCachedArray(STORAGE_KEYS.HOME_EVENTS_CACHE, raw);
                return;
            } catch (e) {
                if (attempt === 3) {
                    if (requestId !== latestEventsRequestRef.current) {
                        return;
                    }

                    const cachedEvents = readCachedArray(STORAGE_KEYS.HOME_EVENTS_CACHE);
                    if (cachedEvents.length > 0) {
                        setEvents(cachedEvents);
                    }
                    console.warn('Failed to load events', e);
                    return;
                }

                await sleep(250 * attempt);
            }
        }
    }, []);

    useEffect(() => {
        if (Platform.OS !== 'web' || typeof window === 'undefined') {
            return;
        }

        if (questions.length === 0) {
            const cachedQuestions = readCachedArray(STORAGE_KEYS.HOME_QUESTIONS_CACHE);
            if (cachedQuestions.length > 0) {
                setQuestions(cachedQuestions);
            }
        }

        if (events.length === 0) {
            const cachedEvents = readCachedArray(STORAGE_KEYS.HOME_EVENTS_CACHE);
            if (cachedEvents.length > 0) {
                setEvents(cachedEvents);
            }
        }
    }, [events.length, questions.length]);

    useFocusEffect(useCallback(() => {
        loadQuestions();
        loadEvents();

        let isMounted = true;
        const checkPremium = async () => {
            if (!user?.id) return;
            try {
                const res = await apiClient.get(`/api/v1/users/${user.id}`);
                if (isMounted) setIsPremium(res?.data?.premiumActive === true);
            } catch (_) {
                // silently ignore — stays false
            }
        };
        checkPremium();
        return () => { isMounted = false; };
    }, [loadEvents, loadQuestions, user?.id]));

    // Handle map bounds changes coming from MapComponent
    const handleMapBoundsChange = useCallback((bounds) => {
        // bounds: { lat, lng, north, south, east, west }
        setMapCenter(bounds);

        if (!bounds || bounds.north == null) return;

        // Debounce bbox fetch (200ms) for questions
        if (bboxFetchTimeoutRef.current) clearTimeout(bboxFetchTimeoutRef.current);
        bboxFetchTimeoutRef.current = setTimeout(async () => {
            const requestId = ++latestRequestRef.current;
            try {
                const res = await apiClient.get('/api/v1/questions/compact/bbox', {
                    params: {
                        south: bounds.south,
                        west: bounds.west,
                        north: bounds.north,
                        east: bounds.east,
                        active: true,
                    },
                });

                if (requestId !== latestRequestRef.current) return;

                const raw = Array.isArray(res?.data) ? res.data : [];
                setQuestions(raw);
                // update cached questions for web
                if (Platform.OS === 'web' && typeof window !== 'undefined') {
                    window.localStorage.setItem(STORAGE_KEYS.HOME_QUESTIONS_CACHE, JSON.stringify(raw));
                }
            } catch (e) {
                console.warn('Failed to load bbox questions', e);
            }
        }, 200);

        // Debounce bbox fetch (200ms) for events
        if (eventsBboxFetchTimeoutRef.current) clearTimeout(eventsBboxFetchTimeoutRef.current);
        eventsBboxFetchTimeoutRef.current = setTimeout(async () => {
            const requestId = ++latestEventsRequestRef.current;
            try {
                const res = await apiClient.get('/api/v1/events/compact/bbox', {
                    params: {
                        south: bounds.south,
                        west: bounds.west,
                        north: bounds.north,
                        east: bounds.east,
                        active: true,
                    },
                });

                if (requestId !== latestEventsRequestRef.current) return;

                const raw = Array.isArray(res?.data) ? res.data : [];
                setEvents(raw);
                // update cached events for web
                if (Platform.OS === 'web' && typeof window !== 'undefined') {
                    window.localStorage.setItem(STORAGE_KEYS.HOME_EVENTS_CACHE, JSON.stringify(raw));
                }
            } catch (e) {
                console.warn('Failed to load bbox events', e);
            }
        }, 200);
    }, [STORAGE_KEYS.HOME_QUESTIONS_CACHE, STORAGE_KEYS.HOME_EVENTS_CACHE]);

    useEffect(() => {
        const unsub = observeNotifications((n) => {
            if (!isFocused) return;

            if (n?.type === 'NEARBY_QUESTION') {
                loadQuestions();
            }

            if (n?.type === 'NEARBY_EVENT') {
                loadEvents();
            }

            if (
                (n?.type === 'NEARBY_QUESTION' || n?.type === 'ANSWER_TO_QUESTION') &&
                n?.referenceId
            ) {
                // TODO: enable automatic navigation to the question thread when a notification is received.
                // navigation.navigate('QuestionThread', { questionId: n.referenceId });
            }
        });

        return unsub;
    }, [isFocused, loadEvents, loadQuestions, observeNotifications]);

    useEffect(() => {
        if (route.params?.refreshNonce) {
            const createdQuestion = route.params?.newQuestion;

            if (createdQuestion?.id) {
                setQuestions((currentQuestions) => {
                    const nextQuestions = [
                        createdQuestion,
                        ...currentQuestions.filter((question) => question?.id !== createdQuestion.id),
                    ];
                    try {
                        writeCachedArray(STORAGE_KEYS.HOME_QUESTIONS_CACHE, nextQuestions);
                    } catch (error) {
                        console.warn("Cache full, ignoring saved data to prevent app crashes");
                    }
                    return nextQuestions;
                });
            }

            loadQuestions();
            loadEvents();

            navigation.setParams({ refreshNonce: undefined, newQuestion: undefined });
        }
    }, [route.params?.refreshNonce, route.params?.newQuestion, loadQuestions, loadEvents, navigation]);

    useEffect(() => {
        if (!isFocused || Platform.OS !== 'web' || typeof window === 'undefined') {
            return;
        }

        const postCheckoutTarget = window.localStorage.getItem(STORAGE_KEYS.STREETCOINS_POST_CHECKOUT_TARGET);
        if (postCheckoutTarget === 'balance') {
            window.localStorage.removeItem(STORAGE_KEYS.STREETCOINS_POST_CHECKOUT_TARGET);
            navigation.navigate('Balance');
            return;
        }

        const rawNotice = window.localStorage.getItem(STORAGE_KEYS.STREETCOINS_SUCCESS_NOTICE);
        if (!rawNotice) {
            return;
        }

        try {
            const parsedNotice = JSON.parse(rawNotice);
            const addedStreetCoins = Number(parsedNotice?.addedStreetCoins);

            if (Number.isFinite(addedStreetCoins) && addedStreetCoins > 0) {
                setStreetCoinsNotice(`+ ${addedStreetCoins} streetcoins`);
                loadQuestions();
                loadEvents();
            }
        } catch (error) {
            console.warn('Invalid StreetCoins success notice format.', error);
        } finally {
            window.localStorage.removeItem(STORAGE_KEYS.STREETCOINS_SUCCESS_NOTICE);
        }
    }, [isFocused, loadEvents, loadQuestions, navigation]);

    useEffect(() => {
        if (Platform.OS !== 'web' || typeof window === 'undefined') {
            return;
        }

        const handleStreetCoinsPurchaseConfirmed = (event) => {
            const addedStreetCoins = Number(event?.detail?.addedStreetCoins);
            if (Number.isFinite(addedStreetCoins) && addedStreetCoins > 0) {
                setStreetCoinsNotice(`+ ${addedStreetCoins} streetcoins`);
            }
            loadQuestions();
            loadEvents();
        };

        window.addEventListener('streetcoins:purchase-confirmed', handleStreetCoinsPurchaseConfirmed);
        return () => {
            window.removeEventListener('streetcoins:purchase-confirmed', handleStreetCoinsPurchaseConfirmed);
        };
    }, [loadEvents, loadQuestions]);

    useEffect(() => {
        if (!streetCoinsNotice) {
            return;
        }

        coinsToastAnim.stopAnimation();
        Animated.timing(coinsToastAnim, {
            toValue: 1,
            duration: 220,
            useNativeDriver: true,
        }).start();

        const hideTimer = setTimeout(() => {
            Animated.timing(coinsToastAnim, {
                toValue: 0,
                duration: 220,
                useNativeDriver: true,
            }).start(() => setStreetCoinsNotice(''));
        }, 3200);

        return () => clearTimeout(hideTimer);
    }, [streetCoinsNotice, coinsToastAnim]);

    useEffect(() => {
        async function initPush() {
            try {
                if (Platform.OS !== 'web') {
                    return;
                }

                if (!token) {
                    pushBootstrappedRef.current = false;
                    pushSubscriptionRef.current = null;
                    pushZoneKeyRef.current = null;
                    return;
                }

                if (pushBootstrappedRef.current) {
                    return;
                }

                const apiBaseUrl = APP_CONFIG.apiBaseUrl;

                const { subscription } = await bootstrapWebPushNotifications({
                    authToken: token,
                    apiBaseUrl,
                    latitude: currentLocation?.latitude,
                    longitude: currentLocation?.longitude,
                    onNotificationClick: (data) => {
                        if (
                            (data?.type === 'NEARBY_QUESTION' || data?.type === 'ANSWER_TO_QUESTION') &&
                            data?.referenceId
                        ) {
                            navigation.navigate('QuestionThread', { questionId: data.referenceId });
                        }
                    },
                });

                if (subscription) {
                    pushSubscriptionRef.current = subscription;
                    pushBootstrappedRef.current = true;
                }
            } catch (error) {
                console.error('Error bootstrapping web push notifications:', error);
            }
        }

        initPush();
    }, [token, currentLocation, navigation]);

    useEffect(() => {
        async function syncPushZone() {
            try {
                if (Platform.OS !== 'web' || !token || !pushSubscriptionRef.current) {
                    return;
                }

                if (
                    typeof currentLocation?.latitude !== 'number' ||
                    typeof currentLocation?.longitude !== 'number'
                ) {
                    return;
                }

                const zoneKey = resolveZoneKey(currentLocation.latitude, currentLocation.longitude);

                if (!zoneKey || pushZoneKeyRef.current === zoneKey) {
                    return;
                }

                await updateWebPushZone(
                    pushSubscriptionRef.current,
                    zoneKey,
                    token,
                    APP_CONFIG.apiBaseUrl,
                    currentLocation.latitude,
                    currentLocation.longitude
                );

                pushZoneKeyRef.current = zoneKey;
            } catch (error) {
                console.error('Error updating push notification zone:', error);
            }
        }

        syncPushZone();
    }, [token, currentLocation]);

    useEffect(() => {
        if (menuOpen) {
            Animated.timing(menuAnimValue, {
                toValue: 0,
                duration: 350,
                useNativeDriver: true,
            }).start();
        } else {
            Animated.timing(menuAnimValue, {
                toValue: -350,
                duration: 300,
                useNativeDriver: true,
            }).start();
        }
    }, [menuOpen, menuAnimValue]);

    const resetFeedbackForm = () => {
        setFeedbackType('SUGGESTION');
        setFeedbackMessage('');
    };

    const closeFeedbackModal = () => {
        if (sendingFeedback) return;
        setFeedbackVisible(false);
        resetFeedbackForm();
    };

    const sendFeedback = async () => {
        const trimmedMessage = feedbackMessage.trim();

        if (!trimmedMessage) {
            Alert.alert('Feedback required', 'Please write a message before sending.');
            return;
        }

        try {
            setSendingFeedback(true);

            await apiClient.post('/api/v1/feedback', {
                type: feedbackType,
                message: trimmedMessage,
            });

            setFeedbackVisible(false);
            resetFeedbackForm();
            setFeedbackSuccessMessage('Thank you for helping us improve StreetAsk.');
            setFeedbackSuccessVisible(true);
        } catch (error) {
            console.error('Error sending feedback:', error);
            Alert.alert('Error', 'Your feedback could not be sent. Please try again later.');
        } finally {
            setSendingFeedback(false);
        }
    };

    const handleRefresh = async () => {
        try {
            setMapKey((k) => k + 1);
            await loadQuestions();
            await loadEvents();
        } catch (e) {
            console.warn('Error refreshing data', e);
        }
    };

    return (
        <>
            <SafeAreaView style={styles.screen}>
                <View style={styles.container}>
                    <View style={[styles.topBar, isNarrow && { paddingHorizontal: 12 }]}>
                        <TouchableOpacity
                            style={styles.topBarLeft}
                            activeOpacity={0.7}
                            onPress={handleRefresh}
                        >
                            <View style={styles.logoBadge}>
                                <Image
                                    source={require('../../../../assets/logo.png')}
                                    style={{ width: 18, height: 28 }}
                                />
                            </View>

                            <Text style={styles.appName}>StreetAsk</Text>
                        </TouchableOpacity>

                        <View style={styles.topBarRight}>
                            {!isBusinessUser && (isPremium ? (
                                <TouchableOpacity
                                    style={styles.proBadge}
                                    activeOpacity={0.85}
                                    onPress={() => navigation.navigate('SubscriptionPlans')}
                                >
                                    <Text style={styles.proBadgeText}>⭐ PRO</Text>
                                </TouchableOpacity>
                            ) : (
                                <TouchableOpacity
                                    style={styles.goProBtn}
                                    activeOpacity={0.85}
                                    onPress={() => navigation.navigate('SubscriptionPlans')}
                                >
                                    <Text style={styles.goProText}>👑 GO PRO</Text>
                                </TouchableOpacity>
                            ))}

                            {!isBusinessUser && (
                                <TouchableOpacity
                                    style={[styles.iconBtn, styles.attendancesBtn]}
                                    activeOpacity={0.7}
                                    onPress={() => navigation.navigate('MyAttendances')}
                                >
                                    <Ionicons name="calendar-outline" size={20} color="#1e40af" />
                                </TouchableOpacity>
                            )}

                            {isNarrow ? (
                                <TouchableOpacity
                                    style={styles.iconBtn}
                                    activeOpacity={0.7}
                                    onPress={() => setMenuOpen(!menuOpen)}
                                >
                                    <Ionicons name="menu" size={24} color="#374151" />
                                </TouchableOpacity>
                            ) : (
                                <>
                                    {user?.roles?.includes('ADMIN') && (
                                        <TouchableOpacity
                                            style={styles.iconBtn}
                                            activeOpacity={0.7}
                                            onPress={() => navigation.navigate('AdminDashboard')}
                                        >
                                            <Ionicons name="shield-checkmark-outline" size={20} color="#374151" />
                                        </TouchableOpacity>
                                    )}

                                    <TouchableOpacity
                                        style={styles.iconBtn}
                                        activeOpacity={0.7}
                                        onPress={() => navigation.navigate('Profile')}
                                    >
                                        <Ionicons name="person-outline" size={20} color="#374151" />
                                    </TouchableOpacity>
                                    <TouchableOpacity
                                        style={styles.iconBtn}
                                        activeOpacity={0.7}
                                        onPress={() => setFeedbackVisible(true)}
                                    >
                                        <Ionicons name="chatbox-ellipses-outline" size={20} color="#a52019" />
                                    </TouchableOpacity>
                                    <TouchableOpacity
                                        style={[styles.iconBtn, styles.logoutBtn]}
                                        onPress={() => setShowLogoutModal(true)}
                                        activeOpacity={0.7}
                                    >
                                        <Ionicons name="log-out-outline" size={20} color="#ef4444" />
                                    </TouchableOpacity>
                                </>
                            )}
                        </View>
                    </View>

                    {ephemeralNotification ? (
                        <View style={styles.notifBanner}>
                            <Ionicons name="information-circle" size={18} color="#92400e" />
                            <View style={{ flex: 1, marginLeft: 8 }}>
                                <Text style={styles.notifTitle}>
                                    {ephemeralNotification.title || 'Notification'}
                                </Text>
                                <Text style={styles.notifMsg}>
                                    {ephemeralNotification.message || ''}
                                </Text>
                            </View>
                        </View>
                    ) : null}

                    {streetCoinsNotice ? (
                        <Animated.View
                            style={[
                                styles.coinsSideToast,
                                {
                                    opacity: coinsToastAnim,
                                    transform: [
                                        {
                                            translateX: coinsToastAnim.interpolate({
                                                inputRange: [0, 1],
                                                outputRange: [220, 0],
                                            }),
                                        },
                                    ],
                                },
                            ]}
                        >
                            <Ionicons name="wallet" size={18} color="#065f46" />
                            <Text style={styles.coinsSideToastText}>{streetCoinsNotice}</Text>
                        </Animated.View>
                    ) : null}

                    <Modal visible={menuOpen} transparent animationType="fade" onRequestClose={() => setMenuOpen(false)}>
                        <Pressable style={styles.menuOverlay} onPress={() => setMenuOpen(false)}>
                            <Animated.View style={[styles.menuContent, { transform: [{ translateY: menuAnimValue }] }]}>
                                <TouchableOpacity
                                    style={styles.menuItem}
                                    onPress={() => { navigation.navigate('Profile'); setMenuOpen(false); }}
                                >
                                    <Ionicons name="person-outline" size={18} color="#374151" />
                                    <Text style={styles.menuItemLabel}>Profile</Text>
                                </TouchableOpacity>

                                {user?.roles?.includes('ADMIN') && (
                                    <TouchableOpacity
                                        style={styles.menuItem}
                                        onPress={() => { navigation.navigate('AdminDashboard'); setMenuOpen(false); }}
                                    >
                                        <Ionicons name="shield-checkmark-outline" size={18} color="#374151" />
                                        <Text style={styles.menuItemLabel}>Admin</Text>
                                    </TouchableOpacity>
                                )}

                                <TouchableOpacity
                                    style={styles.menuItem}
                                    onPress={() => { setFeedbackVisible(true); setMenuOpen(false); }}
                                >
                                    <Ionicons name="chatbox-ellipses-outline" size={18} color="#a52019" />
                                    <Text style={styles.menuItemLabel}>Feedback</Text>
                                </TouchableOpacity>

                                <View style={styles.menuDivider} />

                                <TouchableOpacity
                                    style={styles.menuItem}
                                    onPress={() => { setShowLogoutModal(true); setMenuOpen(false); }}
                                >
                                    <Ionicons name="log-out-outline" size={18} color="#ef4444" />
                                    <Text style={{ ...styles.menuItemLabel, color: '#ef4444' }}>Logout</Text>
                                </TouchableOpacity>
                            </Animated.View>
                        </Pressable>
                    </Modal>

                    <View style={styles.mapWrapper}>
                        <MapComponent
                            key={mapKey}
                            questions={showQuestions ? questions : []}
                            events={events}
                            eventNavigationTarget={selectedEventTarget}
                            canAttendEvents={Boolean(user) && !isBusiness}
                            onEventAttendanceUpdate={handleEventAttendanceUpdate}
                            onOpenEventDetails={(eventItem) =>
                                navigation.navigate('EventDetails', { eventId: eventItem?.id })
                            }
                            onQuestionPress={(qId) =>
                                navigation.navigate('QuestionThread', { questionId: qId })
                            }
                            onLocationChange={setCurrentLocation}
                            onPermissionChange={setHasLocationPermission}
                            onMapBoundsChange={handleMapBoundsChange}
                            onVisibleQuestionsChange={setVisibleQuestionsIds}
                        />
                    </View>

                    {!sidebarVisible && (
                        <TouchableOpacity
                            style={styles.sidebarToggleBtn}
                            onPress={() => setSidebarVisible(true)}
                            activeOpacity={0.7}
                        >
                            <Ionicons
                                name="list-outline"
                                size={24}
                                color="#fff"
                            />
                        </TouchableOpacity>
                    )}

                    <QuestionsSidebar
                        visible={sidebarVisible}
                        questions={showQuestions ? questions : []}
                        events={events}
                        visibleQuestionsIds={visibleQuestionsIds}
                        mapCenter={mapCenter}
                        userLocation={currentLocation}
                        onToggle={() => setSidebarVisible(!sidebarVisible)}
                        activeTab={sidebarTab}
                        onTabChange={setSidebarTab}
                        onQuestionPress={(qId) => {
                            setSidebarVisible(false);
                            navigation.navigate('QuestionThread', { questionId: qId });
                        }}
                        onEventNavigate={handleSidebarNavigateToEvent}
                        onEventViewDetails={(eventItem) => {
                            setSidebarVisible(false);
                            navigation.navigate('EventDetails', { eventId: eventItem?.id });
                        }}
                    />

                    {!isBusinessUser && (
                        <View style={[styles.footer, isNarrow && { paddingHorizontal: 14 }]}>
                            <Text style={styles.toggleLabel}>Show Questions</Text>
                            <Switch
                                value={showQuestions}
                                onValueChange={setShowQuestions}
                                trackColor={{ false: '#d1d5db', true: '#a52019' }}
                                thumbColor="#fff"
                            />
                        </View>
                    )}
                    {hasLocationPermission && (
                        <>
                            {isBusiness && (
                                <TouchableOpacity
                                    style={[styles.fab, styles.fabSecondary, isNarrow && { width: 220 }]}
                                    onPress={() => navigation.navigate('ManageEvents')}
                                    activeOpacity={0.85}
                                >
                                    <Ionicons name="calendar-outline" size={20} color="#fff" />
                                    <Text style={styles.fabText}>Manage events</Text>
                                </TouchableOpacity>
                            )}

                            {!isBusiness && (
                                <TouchableOpacity
                                    style={[styles.fab, isNarrow && { width: 220 }]}
                                    onPress={() => navigation.navigate('CreateQuestion')}
                                    activeOpacity={0.85}
                                >
                                    <Ionicons name="chatbubble-ellipses" size={20} color="#fff" />
                                    <Text style={styles.fabText}>Ask a question</Text>
                                </TouchableOpacity>
                            )}
                        </>
                    )}


                </View>
            </SafeAreaView>

            <Modal
                visible={feedbackVisible}
                transparent
                animationType="fade"
                onRequestClose={closeFeedbackModal}
            >
                <Pressable style={styles.modalOverlay} onPress={closeFeedbackModal}>
                    <Pressable style={styles.feedbackModalBox} onPress={() => { }}>
                        <View style={styles.feedbackHeader}>
                            <View style={styles.feedbackHeaderLeft}>
                                <View style={styles.feedbackIconWrap}>
                                    <Ionicons name="chatbox-ellipses-outline" size={20} color="#a52019" />
                                </View>
                                <View>
                                    <Text style={styles.feedbackTitle}>Pilot feedback</Text>
                                    <Text style={styles.feedbackSubtitle}>
                                        Help us improve StreetAsk
                                    </Text>
                                </View>
                            </View>

                            <TouchableOpacity
                                onPress={closeFeedbackModal}
                                disabled={sendingFeedback}
                                style={styles.feedbackCloseBtn}
                            >
                                <Ionicons name="close" size={20} color="#6b7280" />
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.feedbackLabel}>Type</Text>
                        <View style={styles.feedbackTypeRow}>
                            <TouchableOpacity
                                style={[
                                    styles.feedbackTypeChip,
                                    feedbackType === 'BUG' && styles.feedbackTypeChipActive,
                                ]}
                                onPress={() => setFeedbackType('BUG')}
                                disabled={sendingFeedback}
                            >
                                <Text
                                    style={[
                                        styles.feedbackTypeText,
                                        feedbackType === 'BUG' && styles.feedbackTypeTextActive,
                                    ]}
                                >
                                    Bug
                                </Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={[
                                    styles.feedbackTypeChip,
                                    feedbackType === 'SUGGESTION' && styles.feedbackTypeChipActive,
                                ]}
                                onPress={() => setFeedbackType('SUGGESTION')}
                                disabled={sendingFeedback}
                            >
                                <Text
                                    style={[
                                        styles.feedbackTypeText,
                                        feedbackType === 'SUGGESTION' && styles.feedbackTypeTextActive,
                                    ]}
                                >
                                    Suggestion
                                </Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={[
                                    styles.feedbackTypeChip,
                                    feedbackType === 'OTHER' && styles.feedbackTypeChipActive,
                                ]}
                                onPress={() => setFeedbackType('OTHER')}
                                disabled={sendingFeedback}
                            >
                                <Text
                                    style={[
                                        styles.feedbackTypeText,
                                        feedbackType === 'OTHER' && styles.feedbackTypeTextActive,
                                    ]}
                                >
                                    Other
                                </Text>
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.feedbackLabel}>Message</Text>
                        <TextInput
                            style={styles.feedbackInput}
                            placeholder="Tell us what happened or what we could improve..."
                            placeholderTextColor="#9ca3af"
                            multiline
                            value={feedbackMessage}
                            onChangeText={setFeedbackMessage}
                            editable={!sendingFeedback}
                            textAlignVertical="top"
                        />

                        <TouchableOpacity
                            style={[
                                styles.feedbackSendBtn,
                                sendingFeedback && styles.feedbackSendBtnDisabled,
                            ]}
                            onPress={sendFeedback}
                            activeOpacity={0.85}
                            disabled={sendingFeedback}
                        >
                            {sendingFeedback ? (
                                <ActivityIndicator color="#fff" />
                            ) : (
                                <>
                                    <Ionicons name="send-outline" size={18} color="#fff" />
                                    <Text style={styles.feedbackSendBtnText}>Send feedback</Text>
                                </>
                            )}
                        </TouchableOpacity>
                    </Pressable>
                </Pressable>
            </Modal>

            <Modal
                visible={feedbackSuccessVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setFeedbackSuccessVisible(false)}
            >
                <Pressable
                    style={styles.modalOverlay}
                    onPress={() => setFeedbackSuccessVisible(false)}
                >
                    <Pressable style={styles.successModalBox} onPress={() => { }}>
                        <View style={styles.successIconWrap}>
                            <Ionicons name="checkmark" size={28} color="#fff" />
                        </View>

                        <Text style={styles.successTitle}>Feedback sent</Text>

                        <Text style={styles.successMsg}>
                            {feedbackSuccessMessage}
                        </Text>

                        <TouchableOpacity
                            style={styles.successBtn}
                            onPress={() => setFeedbackSuccessVisible(false)}
                            activeOpacity={0.85}
                        >
                            <Text style={styles.successBtnText}>Great</Text>
                        </TouchableOpacity>
                    </Pressable>
                </Pressable>
            </Modal>

            <ConfirmationModal
                visible={showLogoutModal}
                title="Log out?"
                message="Are you sure you want to log out?"
                confirmText="Log out"
                cancelText="Go Back"
                onConfirm={handleLogoutConfirm}
                onCancel={() => setShowLogoutModal(false)}
                confirmButtonColor="danger"
            />
        </>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#f3f4f6',
    },
    container: {
        flex: 1,
        position: 'relative',
    },
    topBar: {
        backgroundColor: '#fff',
        paddingVertical: 10,
        paddingHorizontal: 16,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
    },
    topBarLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    logoBadge: {
        width: 38,
        height: 38,
        borderRadius: 10,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
    },
    appName: {
        fontSize: 17,
        fontWeight: '800',
        color: '#1f2937',
    },
    topBarRight: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    goProBtn: {
        paddingHorizontal: 12,
        height: 38,
        borderRadius: 12,
        backgroundColor: '#1d4ed8',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: '#1e40af',
    },
    goProText: {
        color: '#fff',
        fontSize: 12,
        fontWeight: '800',
        letterSpacing: 0.4,
    },
    proBadge: {
        paddingHorizontal: 12,
        height: 38,
        borderRadius: 12,
        backgroundColor: '#fbbf24',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: '#f59e0b',
    },
    proBadgeText: {
        color: '#1f2937',
        fontSize: 12,
        fontWeight: '800',
        letterSpacing: 0.4,
    },
    iconBtn: {
        width: 38,
        height: 38,
        borderRadius: 12,
        backgroundColor: '#f3f4f6',
        alignItems: 'center',
        justifyContent: 'center',
    },
    attendancesBtn: {
        backgroundColor: '#fde68a',
        borderWidth: 1,
        borderColor: '#f59e0b',
    },
    logoutBtn: {
        backgroundColor: '#fef2f2',
    },
    notifBanner: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#fffbeb',
        borderBottomWidth: 1,
        borderBottomColor: '#fcd34d',
        paddingHorizontal: 16,
        paddingVertical: 10,
    },
    notifTitle: {
        fontSize: 13,
        fontWeight: '700',
        color: '#92400e',
    },
    notifMsg: {
        fontSize: 13,
        color: '#78350f',
        marginTop: 1,
    },
    coinsSideToast: {
        position: 'absolute',
        right: 14,
        top: 86,
        zIndex: 220,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: '#ecfdf5',
        borderWidth: 1,
        borderColor: '#6ee7b7',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.12,
        shadowRadius: 12,
        elevation: 6,
    },
    coinsSideToastText: {
        fontSize: 12,
        color: '#065f46',
        fontWeight: '800',
        textTransform: 'uppercase',
    },
    mapWrapper: {
        flex: 1,
        overflow: 'hidden',
    },
    sidebarToggleBtn: {
        position: 'absolute',
        top: '50%',
        left: 12,
        width: 48,
        height: 48,
        borderRadius: 12,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOpacity: 0.2,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 3 },
        elevation: 5,
        zIndex: 101,
        transform: [{ translateY: -24 }],
    },
    footer: {
        backgroundColor: '#fff',
        paddingVertical: 12,
        paddingHorizontal: 20,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'flex-end',
        borderTopWidth: 1,
        borderTopColor: '#e5e7eb',
    },
    toggleLabel: {
        fontSize: 14,
        fontWeight: '600',
        color: '#a52019',
        marginRight: 12,
    },
    fab: {
        position: 'absolute',
        bottom: 76,
        alignSelf: 'center',
        width: 260,
        height: 52,
        borderRadius: 26,
        backgroundColor: '#a52019',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
        shadowColor: '#a52019',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.35,
        shadowRadius: 16,
        elevation: 10,
    },
    fabText: {
        color: '#fff',
        fontSize: 15,
        fontWeight: '700',
    },
    fabSecondary: {
        bottom: 136,
        backgroundColor: '#0f766e',
        shadowColor: '#0f766e',
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.35)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 20,
    },
    feedbackModalBox: {
        width: '100%',
        maxWidth: 420,
        backgroundColor: '#fff',
        borderRadius: 20,
        padding: 18,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.18,
        shadowRadius: 24,
        elevation: 14,
    },
    feedbackHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        marginBottom: 16,
    },
    feedbackHeaderLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        flex: 1,
    },
    feedbackIconWrap: {
        width: 40,
        height: 40,
        borderRadius: 12,
        backgroundColor: '#fef2f2',
        alignItems: 'center',
        justifyContent: 'center',
    },
    feedbackTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#1f2937',
    },
    feedbackSubtitle: {
        fontSize: 13,
        color: '#6b7280',
        marginTop: 2,
    },
    feedbackCloseBtn: {
        width: 34,
        height: 34,
        borderRadius: 10,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f9fafb',
    },
    feedbackLabel: {
        fontSize: 13,
        fontWeight: '700',
        color: '#374151',
        marginBottom: 8,
    },
    feedbackTypeRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 16,
        flexWrap: 'wrap',
    },
    feedbackTypeChip: {
        paddingVertical: 8,
        paddingHorizontal: 12,
        borderRadius: 999,
        backgroundColor: '#f3f4f6',
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    feedbackTypeChipActive: {
        backgroundColor: '#a52019',
        borderColor: '#a52019',
    },
    feedbackTypeText: {
        fontSize: 13,
        fontWeight: '600',
        color: '#6b7280',
    },
    feedbackTypeTextActive: {
        color: '#fff',
    },
    feedbackInput: {
        minHeight: 120,
        maxHeight: 180,
        borderWidth: 1,
        borderColor: '#e5e7eb',
        borderRadius: 14,
        paddingHorizontal: 14,
        paddingVertical: 12,
        backgroundColor: '#f9fafb',
        fontSize: 14,
        color: '#111827',
        marginBottom: 18,
    },
    feedbackSendBtn: {
        height: 48,
        borderRadius: 14,
        backgroundColor: '#a52019',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
    },
    feedbackSendBtnDisabled: {
        opacity: 0.7,
    },
    feedbackSendBtnText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '700',
    },
    successModalBox: {
        width: 280,
        backgroundColor: '#fff',
        borderRadius: 22,
        paddingVertical: 28,
        paddingHorizontal: 24,
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.18,
        shadowRadius: 24,
        elevation: 14,
    },
    successIconWrap: {
        width: 58,
        height: 58,
        borderRadius: 29,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
    },
    successTitle: {
        marginTop: 16,
        fontSize: 20,
        fontWeight: '700',
        color: '#1f2937',
    },
    successMsg: {
        marginTop: 8,
        fontSize: 14,
        lineHeight: 20,
        color: '#6b7280',
        textAlign: 'center',
    },
    successBtn: {
        marginTop: 20,
        minWidth: 110,
        height: 44,
        borderRadius: 12,
        backgroundColor: '#a52019',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 20,
    },
    successBtnText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '700',
    },
    menuOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.15)',
        justifyContent: 'flex-start',
        paddingTop: 60,
    },
    menuContent: {
        backgroundColor: '#fff',
        marginHorizontal: 12,
        borderRadius: 12,
        paddingVertical: 8,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.15,
        shadowRadius: 8,
        elevation: 5,
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 16,
        gap: 12,
    },
    menuItemLabel: {
        fontSize: 15,
        fontWeight: '500',
        color: '#374151',
    },
    menuDivider: {
        height: 1,
        backgroundColor: '#e5e7eb',
        marginVertical: 6,
    },
});
