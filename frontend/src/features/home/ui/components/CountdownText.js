import React, { useState, useEffect } from 'react';
import { Text, StyleSheet } from 'react-native';

export const CountdownText = ({ expiresAt, onExpire }) => {
    const [timeLeft, setTimeLeft] = useState('');
    const [isUrgent, setIsUrgent] = useState(false);

    useEffect(() => {
        const updateTime = () => {
            const now = Date.now();

            // Normalize separators without forcing timezone conversion.
            let normalizedDate = typeof expiresAt === 'string'
                ? expiresAt.trim().replace(' ', 'T')
                : expiresAt;

            const expiration = new Date(normalizedDate).getTime();

            if (isNaN(expiration)) {
                console.warn('CountdownText: Invalid date format provided in expiresAt:', expiresAt);
                setTimeLeft('Error');
                return;
            }

            const diff = expiration - now;

            if (diff <= 0) {
                if (onExpire) onExpire();
                setTimeLeft('Expired');
                return;
            }

            const totalMinutes = Math.ceil(diff / (1000 * 60));
            const hoursTotal = Math.floor(totalMinutes / 60);
            const minutesTotal = totalMinutes % 60;

            setIsUrgent(totalMinutes <= 15);
            setTimeLeft(`${hoursTotal}h ${minutesTotal < 10 ? '0' + minutesTotal : minutesTotal}m`);
        };

        updateTime();
        const timer = setInterval(updateTime, 60000);
        return () => clearInterval(timer);
    }, [expiresAt, onExpire]);

    if (timeLeft === 'Expired' || timeLeft === 'Error') return null;

    return (
        <Text style={[styles.timerText, isUrgent ? styles.urgent : null]}>
            ⏱️ {timeLeft}
        </Text>
    );
};

const styles = StyleSheet.create({
    timerText: {
        fontSize: 12,
        fontWeight: '900',
        marginTop: 8,
    },
    urgent: {
        color: '#D40000',
    }
});
