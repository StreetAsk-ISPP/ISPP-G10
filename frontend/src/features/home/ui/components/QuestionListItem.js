import React from 'react';
import {
    View,
    Text,
    StyleSheet,
    TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { CountdownText } from './CountdownText';

export default function QuestionListItem({
    question,
    distance,
    canAnswer,
    featured,
    onPress,
}) {
    const truncateText = (text, limit) => {
        if (!text) return '';
        return text.length > limit ? text.substring(0, limit) + '...' : text;
    };

    const getAnswerBadgeColor = () => {
        if (featured) return '#f59e0b';
        return canAnswer ? '#10b981' : '#9ca3af';
    };

    const getAnswerBadgeText = () => {
        if (featured) return '⭐ Featured';
        return canAnswer ? '✓ Can answer' : '✗ Out of range';
    };

    return (
        <TouchableOpacity
            style={[
                styles.container,
                featured && styles.containerFeatured,
            ]}
            onPress={onPress}
            activeOpacity={0.7}
        >
            {/* Header: título y botón close */}
            <View style={styles.header}>
                <View style={styles.titleContainer}>
                    {featured && <Text style={styles.star}>⭐</Text>}
                    <Text style={styles.title} numberOfLines={2}>
                        {truncateText(question.title, 40)}
                    </Text>
                </View>
            </View>

            {/* Badge: distancia y estado */}
            <View style={styles.badgeRow}>
                <View style={[styles.badge, { backgroundColor: getAnswerBadgeColor() + '20' }]}>
                    <Text style={[styles.badgeText, { color: getAnswerBadgeColor() }]}>
                        {getAnswerBadgeText()}
                    </Text>
                </View>
                {distance !== null && (
                    <View style={styles.distanceBadge}>
                        <Ionicons name="location" size={14} color="#6366f1" />
                        <Text style={styles.distanceText}>
                            {distance < 0.1 ? '<100m' : distance.toFixed(1) + ' km'}
                        </Text>
                    </View>
                )}
            </View>

            {/* Descripción breve */}
            {question.description && (
                <Text style={styles.description} numberOfLines={2}>
                    {truncateText(question.description, 60)}
                </Text>
            )}

            {/* Countdown */}
            <View style={styles.expiresRow}>
                <Ionicons name="time-outline" size={14} color="#6b7280" />
                <CountdownText
                    expiresAt={question.expiresAt}
                    textStyle={styles.expiresText}
                />
            </View>

            {/* Metadata: respuestas y vistas (opcional) */}
            {(question.answersCount !== undefined || question.viewsCount !== undefined) && (
                <View style={styles.metaRow}>
                    {question.answersCount !== undefined && (
                        <View style={styles.metaItem}>
                            <Ionicons name="chatbubbles-outline" size={13} color="#6b7280" />
                            <Text style={styles.metaText}>{question.answersCount}</Text>
                        </View>
                    )}
                    {question.viewsCount !== undefined && (
                        <View style={styles.metaItem}>
                            <Ionicons name="eye-outline" size={13} color="#6b7280" />
                            <Text style={styles.metaText}>{question.viewsCount}</Text>
                        </View>
                    )}
                </View>
            )}

            {/* Arrow indicador */}
            <View style={styles.arrow}>
                <Ionicons name="chevron-forward" size={16} color="#a52019" />
            </View>
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#ffffff',
        padding: 12,
        marginBottom: 8,
        borderRadius: 10,
        borderLeftWidth: 4,
        borderLeftColor: '#a52019',
        shadowColor: '#000',
        shadowOpacity: 0.08,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    containerFeatured: {
        borderLeftColor: '#f59e0b',
        backgroundColor: '#fffbf0',
    },
    header: {
        marginBottom: 10,
    },
    titleContainer: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        gap: 6,
    },
    star: {
        fontSize: 14,
        marginTop: 2,
    },
    title: {
        flex: 1,
        fontSize: 14,
        fontWeight: '700',
        color: '#1f2937',
        lineHeight: 20,
    },
    badgeRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 8,
        alignItems: 'center',
    },
    badge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 6,
    },
    badgeText: {
        fontSize: 12,
        fontWeight: '600',
    },
    distanceBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        paddingHorizontal: 8,
        paddingVertical: 4,
        backgroundColor: '#e0e7ff',
        borderRadius: 6,
    },
    distanceText: {
        fontSize: 12,
        fontWeight: '600',
        color: '#4338ca',
    },
    description: {
        fontSize: 12,
        color: '#6b7280',
        marginBottom: 8,
        lineHeight: 16,
    },
    expiresRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 5,
        marginBottom: 8,
    },
    expiresText: {
        fontSize: 12,
        color: '#6b7280',
    },
    metaRow: {
        flexDirection: 'row',
        gap: 12,
        alignItems: 'center',
    },
    metaItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    metaText: {
        fontSize: 12,
        color: '#6b7280',
    },
    arrow: {
        position: 'absolute',
        right: 12,
        top: '50%',
        transform: [{ translateY: -8 }],
    },
});
