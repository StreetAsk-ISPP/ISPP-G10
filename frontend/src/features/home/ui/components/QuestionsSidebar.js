import React, { useEffect, useState, useCallback } from 'react';
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    TouchableOpacity,
    Animated,
    SafeAreaView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { calculateDistanceInKm } from '../../../../shared/utils/helpers';
import QuestionListItem from './QuestionListItem';

const toNum = (v) => {
    if (typeof v === 'number') return v;
    if (typeof v === 'string') {
        const n = parseFloat(v);
        return Number.isFinite(n) ? n : undefined;
    }
    return undefined;
};

const getQuestionCoords = (q) => {
    const loc = q?.location ?? {};
    const lat =
        toNum(loc.latitude) ?? toNum(loc.lat) ?? toNum(loc.y) ?? toNum(q?.latitude) ?? toNum(q?.lat);

    const lng =
        toNum(loc.longitude) ??
        toNum(loc.lng) ??
        toNum(loc.lon) ??
        toNum(loc.x) ??
        toNum(q?.longitude) ??
        toNum(q?.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    return { lat, lng };
};

const getEventCoords = (event) => {
    const loc = event?.location ?? {};
    const lat =
        toNum(loc.latitude) ?? toNum(loc.lat) ?? toNum(loc.y) ?? toNum(event?.latitude) ?? toNum(event?.lat);

    const lng =
        toNum(loc.longitude) ??
        toNum(loc.lng) ??
        toNum(loc.lon) ??
        toNum(loc.x) ??
        toNum(event?.longitude) ??
        toNum(event?.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    return { lat, lng };
};

const isEventStillVisible = (event, nowTs) => {
    if (!event || event.active === false) {
        return false;
    }

    if (!event.endsAt) {
        return true;
    }

    const endsAtTs = new Date(event.endsAt).getTime();
    if (!Number.isFinite(endsAtTs)) {
        return true;
    }

    return endsAtTs > nowTs;
};

const PAGE_SIZE = 5;

export default function QuestionsSidebar({
    visible,
    questions = [],
    events = [],
    visibleQuestionsIds = [],
    mapCenter = null,
    userLocation = null,
    onToggle,
    onQuestionPress,
    onEventNavigate,
    onEventViewDetails,
    activeTab = 'QUESTIONS',
    onTabChange,
}) {
    const [filteredQuestions, setFilteredQuestions] = useState([]);
    const [filteredEvents, setFilteredEvents] = useState([]);
    const [questionsPage, setQuestionsPage] = useState(1);
    const [eventsPage, setEventsPage] = useState(1);
    const [eventsNowTs, setEventsNowTs] = useState(() => Date.now());
    const animValue = React.useRef(new Animated.Value(visible ? 0 : -300)).current;

    useEffect(() => {
        const intervalId = setInterval(() => {
            setEventsNowTs(Date.now());
        }, 30000);

        return () => clearInterval(intervalId);
    }, []);

    // Filtrar preguntas que están en el viewport y ordenarlas por distancia
    useEffect(() => {
        if (!mapCenter || questions.length === 0) {
            setFilteredQuestions([]);
            return;
        }

        // Filtrar solo preguntas que están en el viewport actual
        const filtered = questions.filter((q) => visibleQuestionsIds.includes(q.id));

        // Sortear por distancia al centro del mapa
        const sorted = filtered
            .map((q) => {
                const coords = getQuestionCoords(q);
                if (!coords) return null;

                const dist = calculateDistanceInKm(
                    { latitude: mapCenter.lat, longitude: mapCenter.lng },
                    { latitude: coords.lat, longitude: coords.lng }
                );

                return { ...q, distance: dist, coords };
            })
            .filter((q) => q !== null)
            .sort((a, b) => a.distance - b.distance);

        setFilteredQuestions(sorted);
    }, [questions, visibleQuestionsIds, mapCenter]);

    useEffect(() => {
        if (events.length === 0) {
            setFilteredEvents([]);
            return;
        }

        const sortedEvents = events
            .filter((event) => isEventStillVisible(event, eventsNowTs))
            .map((event) => {
                const coords = getEventCoords(event);
                if (!coords) return { ...event, distance: null };

                const dist = mapCenter
                    ? calculateDistanceInKm(
                        { latitude: mapCenter.lat, longitude: mapCenter.lng },
                        { latitude: coords.lat, longitude: coords.lng }
                    )
                    : null;

                return { ...event, distance: dist, coords };
            })
            .sort((a, b) => {
                if (a.distance === null && b.distance === null) return 0;
                if (a.distance === null) return 1;
                if (b.distance === null) return -1;
                return a.distance - b.distance;
            });

        setFilteredEvents(sortedEvents);
    }, [events, mapCenter, eventsNowTs]);

    useEffect(() => {
        setQuestionsPage(1);
        setEventsPage(1);
    }, [activeTab, questions.length, events.length]);

    // Animar entrada/salida
    useEffect(() => {
        Animated.timing(animValue, {
            toValue: visible ? 0 : -300,
            duration: 300,
            useNativeDriver: true,
        }).start();
    }, [visible, animValue]);

    const questionsTotalPages = Math.max(1, Math.ceil(filteredQuestions.length / PAGE_SIZE));
    const eventsTotalPages = Math.max(1, Math.ceil(filteredEvents.length / PAGE_SIZE));

    const currentQuestionsPage = Math.min(questionsPage, questionsTotalPages);
    const currentEventsPage = Math.min(eventsPage, eventsTotalPages);

    const paginatedQuestions = filteredQuestions.slice(
        (currentQuestionsPage - 1) * PAGE_SIZE,
        currentQuestionsPage * PAGE_SIZE
    );

    const paginatedEvents = filteredEvents.slice(
        (currentEventsPage - 1) * PAGE_SIZE,
        currentEventsPage * PAGE_SIZE
    );

    const renderQuestion = useCallback(
        ({ item }) => {
            const coords = getQuestionCoords(item);
            if (!coords) return null;

            const radiusKm = toNum(item?.radiusKm);
            const distanceKm = userLocation
                ? calculateDistanceInKm(
                    { latitude: userLocation.latitude, longitude: userLocation.longitude },
                    { latitude: coords.lat, longitude: coords.lng }
                )
                : null;

            const canAnswer =
                !Number.isFinite(radiusKm) ||
                radiusKm <= 0 ||
                (distanceKm !== null && distanceKm <= radiusKm);

            return (
                <QuestionListItem
                    question={item}
                    distance={item.distance}
                    canAnswer={canAnswer}
                    featured={item.featured || false}
                    onPress={() => onQuestionPress?.(item.id)}
                />
            );
        },
        [userLocation, onQuestionPress]
    );

    const renderEvent = useCallback(
        ({ item }) => {
            const coords = getEventCoords(item);
            const attendeeCount = Number.isFinite(Number(item?.attendeeCount)) ? Number(item.attendeeCount) : 0;
            const distanceText = item?.distance !== null && Number.isFinite(item?.distance)
                ? item.distance < 0.1
                    ? '<100m'
                    : `${item.distance.toFixed(1)} km`
                : null;

            return (
                <TouchableOpacity style={styles.eventItem} activeOpacity={0.8}>
                    <View style={styles.eventHeader}>
                        <Text style={styles.eventTitle} numberOfLines={2}>
                            {item.title || 'Untitled event'}
                        </Text>
                        <Text style={styles.eventCategory}>{item.category || 'OTHER'}</Text>
                    </View>

                    <Text style={styles.eventDescription} numberOfLines={2}>
                        {item.description || 'No description available.'}
                    </Text>

                    <View style={styles.eventMetaRow}>
                        <View style={[styles.eventChip, styles.eventChipBlue]}>
                            <Ionicons name="people-outline" size={12} color="#1d4ed8" />
                            <Text style={styles.eventChipText}>{attendeeCount} going</Text>
                        </View>
                        {distanceText && (
                            <View style={[styles.eventChip, styles.eventChipPurple]}>
                                <Ionicons name="location-outline" size={12} color="#7c3aed" />
                                <Text style={[styles.eventChipText, styles.eventChipTextPurple]}>{distanceText}</Text>
                            </View>
                        )}
                    </View>

                    {item.address && (
                        <Text style={styles.eventAddress} numberOfLines={1}>
                            {item.address}
                        </Text>
                    )}

                    {coords && (
                        <Text style={styles.eventCoords}>
                            {coords.lat.toFixed(5)}, {coords.lng.toFixed(5)}
                        </Text>
                    )}

                    <TouchableOpacity
                        style={styles.eventActionBtn}
                        onPress={() => onEventNavigate?.(item, coords)}
                        activeOpacity={0.8}
                    >
                        <Ionicons name="navigate-outline" size={16} color="#fff" />
                        <Text style={styles.eventActionText}>
                            Go to event
                        </Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={styles.eventInfoBtn}
                        onPress={() => onEventViewDetails?.(item)}
                        activeOpacity={0.8}
                    >
                        <Ionicons name="information-circle-outline" size={16} color="#1d4ed8" />
                        <Text style={styles.eventInfoText}>View event info</Text>
                    </TouchableOpacity>
                </TouchableOpacity>
            );
        },
        [onEventNavigate, onEventViewDetails]
    );

    const activeQuestionsPageLabel = `${currentQuestionsPage}/${questionsTotalPages}`;
    const activeEventsPageLabel = `${currentEventsPage}/${eventsTotalPages}`;

    const keyExtractor = useCallback((item) => item.id.toString(), []);

    if (!visible && filteredQuestions.length === 0 && filteredEvents.length === 0) return null;

    const showQuestions = activeTab !== 'EVENTS';
    const showEvents = activeTab === 'EVENTS';

    return (
        <Animated.View
            style={[
                styles.container,
                { transform: [{ translateX: animValue }] },
            ]}
            pointerEvents={visible ? 'box-none' : 'none'}
        >
            {/* Overlay semi-transparente - permite clicks a través */}
            {visible && (
                <TouchableOpacity
                    style={styles.overlay}
                    onPress={onToggle}
                    activeOpacity={1}
                    pointerEvents="auto"
                />
            )}

            {/* Sidebar flotante - solo captura eventos en su área */}
            <Animated.View
                style={[
                    styles.sidebar,
                    { transform: [{ translateX: animValue }] },
                ]}
                pointerEvents="auto"
            >
                <SafeAreaView style={styles.sidebarContent}>
                    {/* Header */}
                    <View style={styles.header}>
                        <View style={styles.headerTabs}>
                            <TouchableOpacity
                                style={[styles.headerTab, showQuestions && styles.headerTabActive]}
                                onPress={() => onTabChange?.('QUESTIONS')}
                            >
                                <Text style={[styles.headerTabText, showQuestions && styles.headerTabTextActive]}>
                                    QUESTIONS
                                </Text>
                            </TouchableOpacity>
                            <Text style={styles.headerDivider}>|</Text>
                            <TouchableOpacity
                                style={[styles.headerTab, showEvents && styles.headerTabActive]}
                                onPress={() => onTabChange?.('EVENTS')}
                            >
                                <Text style={[styles.headerTabText, showEvents && styles.headerTabTextActive]}>
                                    EVENTS
                                </Text>
                            </TouchableOpacity>
                        </View>
                        <TouchableOpacity
                            style={styles.closeBtn}
                            onPress={onToggle}
                            activeOpacity={0.7}
                        >
                            <Ionicons name="close" size={24} color="#1f2937" />
                        </TouchableOpacity>
                    </View>

                    {/* Info text */}
                    {showQuestions && filteredQuestions.length > 0 && (
                        <View style={styles.infoBox}>
                            <Ionicons name="information-circle-outline" size={16} color="#0891b2" />
                            <Text style={styles.infoText}>
                                Sorted by distance from map center
                            </Text>
                        </View>
                    )}

                    {showEvents && filteredEvents.length > 0 && (
                        <View style={[styles.infoBox, styles.infoBoxEvents]}>
                            <Ionicons name="information-circle-outline" size={16} color="#7c3aed" />
                            <Text style={styles.infoText}>
                                Sorted by distance from map center
                            </Text>
                        </View>
                    )}

                    {/* Lista de preguntas */}
                    {showQuestions && paginatedQuestions.length === 0 ? (
                        <View style={styles.emptyState}>
                            <Ionicons name="map-outline" size={64} color="#d1d5db" />
                            <Text style={styles.emptyTitle}>No questions visible</Text>
                            <Text style={styles.emptyText}>
                                Zoom in or move the map to see questions
                            </Text>
                        </View>
                    ) : showEvents && paginatedEvents.length === 0 ? (
                        <View style={styles.emptyState}>
                            <Ionicons name="calendar-clear-outline" size={64} color="#d1d5db" />
                            <Text style={styles.emptyTitle}>No events visible</Text>
                            <Text style={styles.emptyText}>
                                Move the map or try again to see nearby events
                            </Text>
                        </View>
                    ) : (
                        <>
                            {showQuestions ? (
                                <FlatList
                                    data={paginatedQuestions}
                                    renderItem={renderQuestion}
                                    keyExtractor={keyExtractor}
                                    scrollEnabled={true}
                                    showsVerticalScrollIndicator={true}
                                    contentContainerStyle={styles.listContent}
                                />
                            ) : (
                                <FlatList
                                    data={paginatedEvents}
                                    renderItem={renderEvent}
                                    keyExtractor={keyExtractor}
                                    scrollEnabled={true}
                                    showsVerticalScrollIndicator={true}
                                    contentContainerStyle={styles.listContent}
                                />
                            )}

                            <View style={styles.paginationRow}>
                                <TouchableOpacity
                                    style={[styles.paginationBtn, (showQuestions ? currentQuestionsPage === 1 : currentEventsPage === 1) && styles.paginationBtnDisabled]}
                                    disabled={showQuestions ? currentQuestionsPage === 1 : currentEventsPage === 1}
                                    onPress={() => {
                                        if (showQuestions) {
                                            setQuestionsPage((page) => Math.max(1, page - 1));
                                        } else {
                                            setEventsPage((page) => Math.max(1, page - 1));
                                        }
                                    }}
                                >
                                    <Ionicons name="chevron-back" size={16} color="#1f2937" />
                                </TouchableOpacity>
                                <Text style={styles.paginationText}>
                                    {showQuestions ? activeQuestionsPageLabel : activeEventsPageLabel}
                                </Text>
                                <TouchableOpacity
                                    style={[
                                        styles.paginationBtn,
                                        (showQuestions ? currentQuestionsPage === questionsTotalPages : currentEventsPage === eventsTotalPages) && styles.paginationBtnDisabled,
                                    ]}
                                    disabled={showQuestions ? currentQuestionsPage === questionsTotalPages : currentEventsPage === eventsTotalPages}
                                    onPress={() => {
                                        if (showQuestions) {
                                            setQuestionsPage((page) => Math.min(questionsTotalPages, page + 1));
                                        } else {
                                            setEventsPage((page) => Math.min(eventsTotalPages, page + 1));
                                        }
                                    }}
                                >
                                    <Ionicons name="chevron-forward" size={16} color="#1f2937" />
                                </TouchableOpacity>
                            </View>
                        </>
                    )}
                </SafeAreaView>
            </Animated.View>
        </Animated.View>
    );
}

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        top: 0,
        left: 0,
        bottom: 0,
        width: 300,
        zIndex: 100,
        pointerEvents: 'box-none',
    },
    overlay: {
        position: 'absolute',
        top: 0,
        left: 300,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.2)',
        zIndex: 99,
    },
    sidebar: {
        position: 'absolute',
        top: 0,
        left: 0,
        bottom: 0,
        width: 300,
        backgroundColor: '#f9fafb',
        zIndex: 100,
        borderRightWidth: 1,
        borderRightColor: '#e5e7eb',
    },
    sidebarContent: {
        flex: 1,
        flexDirection: 'column',
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
        backgroundColor: '#ffffff',
    },
    headerTabs: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    headerTab: {
        paddingVertical: 4,
        paddingHorizontal: 2,
    },
    headerTabActive: {
        borderBottomWidth: 2,
        borderBottomColor: '#a52019',
    },
    headerTabText: {
        fontSize: 15,
        fontWeight: '700',
        color: '#6b7280',
    },
    headerTabTextActive: {
        color: '#1f2937',
    },
    headerDivider: {
        color: '#9ca3af',
        fontWeight: '800',
        fontSize: 14,
    },
    headerTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#1f2937',
    },
    headerCount: {
        fontSize: 14,
        fontWeight: '600',
        color: '#a52019',
    },
    closeBtn: {
        width: 36,
        height: 36,
        borderRadius: 8,
        backgroundColor: '#f3f4f6',
        alignItems: 'center',
        justifyContent: 'center',
    },
    infoBox: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        marginHorizontal: 12,
        marginTop: 12,
        paddingHorizontal: 10,
        paddingVertical: 8,
        backgroundColor: '#cffafe',
        borderRadius: 8,
        borderLeftWidth: 3,
        borderLeftColor: '#0891b2',
    },
    infoBoxEvents: {
        backgroundColor: '#ede9fe',
        borderLeftColor: '#7c3aed',
    },
    infoText: {
        flex: 1,
        fontSize: 12,
        color: '#164e63',
        fontWeight: '500',
    },
    listContent: {
        padding: 12,
        paddingBottom: 20,
    },
    emptyState: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 20,
    },
    emptyTitle: {
        fontSize: 15,
        fontWeight: '600',
        color: '#6b7280',
        marginTop: 12,
    },
    emptyText: {
        fontSize: 13,
        color: '#9ca3af',
        marginTop: 4,
        textAlign: 'center',
    },
    paginationRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        paddingHorizontal: 12,
        paddingBottom: 12,
    },
    paginationBtn: {
        width: 38,
        height: 38,
        borderRadius: 10,
        backgroundColor: '#e5e7eb',
        alignItems: 'center',
        justifyContent: 'center',
    },
    paginationBtnDisabled: {
        opacity: 0.45,
    },
    paginationText: {
        fontSize: 13,
        fontWeight: '700',
        color: '#374151',
    },
    eventItem: {
        backgroundColor: '#ffffff',
        padding: 12,
        marginBottom: 8,
        borderRadius: 10,
        borderLeftWidth: 4,
        borderLeftColor: '#0f766e',
        shadowColor: '#000',
        shadowOpacity: 0.08,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    eventHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 10,
        marginBottom: 8,
    },
    eventTitle: {
        flex: 1,
        fontSize: 14,
        fontWeight: '800',
        color: '#1f2937',
        lineHeight: 20,
    },
    eventCategory: {
        color: '#0f766e',
        fontSize: 12,
        fontWeight: '800',
    },
    eventDescription: {
        fontSize: 12,
        color: '#6b7280',
        marginBottom: 8,
        lineHeight: 16,
    },
    eventMetaRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
        marginBottom: 8,
    },
    eventChip: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 6,
    },
    eventChipBlue: {
        backgroundColor: '#dbeafe',
    },
    eventChipPurple: {
        backgroundColor: '#ede9fe',
    },
    eventChipText: {
        fontSize: 12,
        fontWeight: '700',
        color: '#1d4ed8',
    },
    eventChipTextPurple: {
        color: '#7c3aed',
    },
    eventAddress: {
        fontSize: 12,
        color: '#6b7280',
        marginBottom: 4,
    },
    eventCoords: {
        fontSize: 12,
        color: '#6b7280',
        marginBottom: 8,
    },
    eventActionBtn: {
        minHeight: 36,
        borderRadius: 10,
        paddingHorizontal: 10,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        backgroundColor: '#2563eb',
    },
    eventActionBtnJoin: {
        backgroundColor: '#0f766e',
    },
    eventActionText: {
        color: '#fff',
        fontWeight: '800',
        fontSize: 12,
    },
    eventInfoBtn: {
        marginTop: 8,
        minHeight: 36,
        borderRadius: 10,
        borderWidth: 1,
        borderColor: '#bfdbfe',
        paddingHorizontal: 10,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        backgroundColor: '#eff6ff',
    },
    eventInfoText: {
        color: '#1d4ed8',
        fontWeight: '800',
        fontSize: 12,
    },
});
