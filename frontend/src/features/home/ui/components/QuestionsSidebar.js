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

export default function QuestionsSidebar({
    visible,
    questions = [],
    visibleQuestionsIds = [],
    mapCenter = null,
    userLocation = null,
    onToggle,
    onQuestionPress,
}) {
    const [filteredQuestions, setFilteredQuestions] = useState([]);
    const animValue = React.useRef(new Animated.Value(visible ? 0 : -300)).current;

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

    // Animar entrada/salida
    useEffect(() => {
        Animated.timing(animValue, {
            toValue: visible ? 0 : -300,
            duration: 300,
            useNativeDriver: true,
        }).start();
    }, [visible, animValue]);

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

    const keyExtractor = useCallback((item) => item.id.toString(), []);

    if (!visible && filteredQuestions.length === 0) return null;

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
                        <Text style={styles.headerTitle}>
                            Questions
                            {filteredQuestions.length > 0 && (
                                <Text style={styles.headerCount}>
                                    {' '}
                                    ({filteredQuestions.length})
                                </Text>
                            )}
                        </Text>
                        <TouchableOpacity
                            style={styles.closeBtn}
                            onPress={onToggle}
                            activeOpacity={0.7}
                        >
                            <Ionicons name="close" size={24} color="#1f2937" />
                        </TouchableOpacity>
                    </View>

                    {/* Info text */}
                    {filteredQuestions.length > 0 && (
                        <View style={styles.infoBox}>
                            <Ionicons name="information-circle-outline" size={16} color="#0891b2" />
                            <Text style={styles.infoText}>
                                Sorted by distance from map center
                            </Text>
                        </View>
                    )}

                    {/* Lista de preguntas */}
                    {filteredQuestions.length === 0 ? (
                        <View style={styles.emptyState}>
                            <Ionicons name="map-outline" size={64} color="#d1d5db" />
                            <Text style={styles.emptyTitle}>No questions visible</Text>
                            <Text style={styles.emptyText}>
                                Zoom in or move the map to see questions
                            </Text>
                        </View>
                    ) : (
                        <FlatList
                            data={filteredQuestions}
                            renderItem={renderQuestion}
                            keyExtractor={keyExtractor}
                            scrollEnabled={true}
                            showsVerticalScrollIndicator={true}
                            contentContainerStyle={styles.listContent}
                        />
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
});
