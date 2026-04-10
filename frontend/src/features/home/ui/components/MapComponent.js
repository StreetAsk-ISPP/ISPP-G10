import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
    View,
    Text,
    StyleSheet,
    ScrollView,
    TouchableOpacity,
    ActivityIndicator,
    Alert,
    Platform,
} from 'react-native';
import * as Location from 'expo-location';
import { locationService } from '../../../../shared/services/location/locationService';
import { CountdownText } from './CountdownText';
import { calculateDistanceInKm } from '../../../../shared/utils/helpers';
import Toast from 'react-native-toast-message';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';

// Para web: importar Leaflet y CSS
let MapContainer, TileLayer, Marker, Popup;
let L;

if (Platform.OS === 'web') {
    try {
        require('leaflet/dist/leaflet.css');
        MapContainer = require('react-leaflet').MapContainer;
        TileLayer = require('react-leaflet').TileLayer;
        Marker = require('react-leaflet').Marker;
        Popup = require('react-leaflet').Popup;
        L = require('leaflet');
    } catch (e) {
        console.error('Error loading Leaflet:', e);
    }
}

// Componente que detecta cambios en los bounds del mapa
const MapBoundsTrackerComponent = ({ questions = [], onBoundsChange, onVisibleQuestionsChange, mapRef }) => {
    useEffect(() => {
        if (!mapRef?.current) return;

        const map = mapRef.current;

        const updateBounds = () => {
            try {
                const bounds = map.getBounds();
                const center = map.getCenter();

                onBoundsChange?.({
                    lat: center.lat,
                    lng: center.lng,
                    north: bounds.getNorthEast().lat,
                    south: bounds.getSouthWest().lat,
                    east: bounds.getNorthEast().lng,
                    west: bounds.getSouthWest().lng,
                });

                // Filtrar preguntas dentro de los bounds
                const visibleIds = questions
                    .filter((q) => {
                        const coords = getQuestionCoords(q);
                        if (!coords) return false;
                        return coords.lat >= bounds.getSouthWest().lat &&
                            coords.lat <= bounds.getNorthEast().lat &&
                            coords.lng >= bounds.getSouthWest().lng &&
                            coords.lng <= bounds.getNorthEast().lng;
                    })
                    .map((q) => q.id);

                onVisibleQuestionsChange?.(visibleIds);
            } catch (error) {
                console.warn('Error updating bounds:', error);
            }
        };

        // Agregar event listeners
        map.on('moveend', updateBounds);
        map.on('zoomend', updateBounds);
        map.on('load', updateBounds);

        // Actualizar al cargar por primera vez
        const timeout = setTimeout(updateBounds, 100);

        return () => {
            clearTimeout(timeout);
            map.off('moveend', updateBounds);
            map.off('zoomend', updateBounds);
            map.off('load', updateBounds);
        };
    }, [mapRef, questions, onBoundsChange, onVisibleQuestionsChange]);

    return null;
};

// Función para crear iconos SVG personalizados
const createCustomIcon = (color) => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">
        <path fill="${color}" d="M16 0C9.383 0 4 5.383 4 12c0 8 12 28 12 28s12-20 12-28c0-6.617-5.383-12-12-12z"/>
        <circle fill="white" cx="16" cy="12" r="4"/>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [32, 40],
        iconAnchor: [16, 40],
        popupAnchor: [0, -40],
    });
};

const createFeaturedIcon = () => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="44" height="56" viewBox="0 0 44 56">
        <defs>
            <radialGradient id="pinGrad" cx="40%" cy="30%" r="70%">
                <stop offset="0%" stop-color="#FFE566"/>
                <stop offset="60%" stop-color="#F59E0B"/>
                <stop offset="100%" stop-color="#B45309"/>
            </radialGradient>
            <filter id="shadow" x="-20%" y="-10%" width="140%" height="130%">
                <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#00000055"/>
            </filter>
        </defs>
        <path filter="url(#shadow)" fill="url(#pinGrad)" stroke="#92400E" stroke-width="1.2"
              d="M22 1C13.163 1 6 8.163 6 17c0 11 16 38 16 38s16-27 16-38C38 8.163 30.837 1 22 1z"/>
        <circle fill="white" fill-opacity="0.25" cx="22" cy="16" r="10"/>
        <polygon fill="white"
                 points="22,8 24.35,14.2 31,14.2 25.8,18.1 27.9,24.3 22,20.5 16.1,24.3 18.2,18.1 13,14.2 19.65,14.2"/>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [44, 56],
        iconAnchor: [22, 56],
        popupAnchor: [0, -56],
    });
};

const createEventIcon = () => {
    if (!L) return undefined;

    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="44" viewBox="0 0 34 44">
        <path fill="#0F766E" d="M17 0C9.82 0 4 5.82 4 13c0 8.7 13 31 13 31s13-22.3 13-31C30 5.82 24.18 0 17 0z"/>
        <rect x="10" y="9" width="14" height="11" rx="2" fill="#FFFFFF"/>
        <rect x="10" y="9" width="14" height="3" rx="1" fill="#14B8A6"/>
        <circle cx="13" cy="15" r="1" fill="#0F766E"/>
        <circle cx="17" cy="15" r="1" fill="#0F766E"/>
        <circle cx="21" cy="15" r="1" fill="#0F766E"/>
    </svg>`;

    return L.icon({
        iconUrl: `data:image/svg+xml;base64,${btoa(svgIcon)}`,
        iconSize: [34, 44],
        iconAnchor: [17, 44],
        popupAnchor: [0, -44],
    });
};

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
    const lat = toNum(loc.latitude) ?? toNum(loc.lat) ?? toNum(event?.latitude) ?? toNum(event?.lat);
    const lng =
        toNum(loc.longitude) ??
        toNum(loc.lng) ??
        toNum(loc.lon) ??
        toNum(event?.longitude) ??
        toNum(event?.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    return { lat, lng };
};

const formatDateTime = (value) => {
    if (!value) return null;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return date.toLocaleString();
};

export default function MapComponent({
    questions = [],
    events = [],
    onQuestionPress,
    onLocationChange,
    onPermissionChange,
    onMapBoundsChange,
    onVisibleQuestionsChange,
    showQuestions = true,
}) {
    const [location, setLocation] = useState(null);
    const [publicLocations, setPublicLocations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [publishing, setPublishing] = useState(false);
    const mapRef = useRef(null);
    const [visibleQuestions, setVisibleQuestions] = useState([]);
    const userLocationIcon = useMemo(() => {
        if (!L) return undefined;
        return L.divIcon({
            className: '',
            html: `<div style="
                width: 16px;
                height: 16px;
                border-radius: 999px;
                background: #a52019;
                border: 3px solid #ffffff;
                box-shadow: 0 0 0 3px rgba(165,32,25,0.35);
            "></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8],
            popupAnchor: [0, -10],
        });
    }, []);

    useEffect(() => {
        let locationSubscription;

        const requestLocationPermission = async () => {
            try {
                const { status } = await Location.requestForegroundPermissionsAsync();

                if (status !== 'granted') {
                    setError('Location permission denied');
                    setLoading(false);
                    if (typeof onPermissionChange === 'function') {
                        onPermissionChange(false);
                    }
                    Toast.show({
                        type: 'error',
                        text1: 'Location disabled',
                        text2: 'Please enable location to ask questions.',
                        position: 'top',
                        visibilityTime: 10000,
                    });
                    return;
                }

                if (typeof onPermissionChange === 'function') {
                    onPermissionChange(true);
                }

                const initialLocation = await Location.getCurrentPositionAsync({
                    accuracy: Location.Accuracy.High,
                });

                setLocation(initialLocation.coords);
                if (typeof onLocationChange === 'function') {
                    onLocationChange(initialLocation.coords);
                }
                setLoading(false);

                await loadPublicLocations();

                // Solo usar watchPositionAsync en móvil, no en web
                if (Platform.OS !== 'web') {
                    locationSubscription = await Location.watchPositionAsync(
                        {
                            accuracy: Location.Accuracy.High,
                            timeInterval: 2000,
                            distanceInterval: 5,
                        },
                        (newLocation) => {
                            setLocation(newLocation.coords);
                            if (typeof onLocationChange === 'function') {
                                onLocationChange(newLocation.coords);
                            }
                        }
                    );
                }
            } catch (err) {
                setError('Error getting location: ' + err.message);
                setLoading(false);
                if (typeof onPermissionChange === 'function') {
                    onPermissionChange(false);
                }
            }
        };

        requestLocationPermission();

        return () => {
            if (locationSubscription && typeof locationSubscription.remove === 'function') {
                locationSubscription.remove();
            }
        };
    }, [onLocationChange, onPermissionChange]);

    const loadPublicLocations = async () => {
        try {
            const response = await locationService.getPublicLocationsSince(30);
            // Asegurar que siempre es un array
            const locations = Array.isArray(response)
                ? response
                : Array.isArray(response?.data)
                    ? response.data
                    : [];
            setPublicLocations(locations);
        } catch (err) {
            console.warn('Error loading public locations:', err);
            setPublicLocations([]); // Asegurar array vacío en caso de error
        }
    };

    useEffect(() => {
        const ahora = new Date().getTime();

        // Si showQuestions es false, retornar array vacío
        if (!showQuestions) {
            setVisibleQuestions([]);
            return;
        }

        // Filtramos las preguntas que ya vencieron antes de guardarlas en el estado
        const preguntasActivas = questions.filter((q) => {
            const fechaExpiracion = new Date(q.expiresAt).getTime();
            return fechaExpiracion > ahora; // Solo dejamos las que expiran en el futuro
        });

        setVisibleQuestions(preguntasActivas);
    }, [questions, showQuestions]);

    const handleQuestionExpire = (questionId) => {
        setVisibleQuestions((prev) => prev.filter((q) => q.id !== questionId));
    };

    const handlePublishLocation = async () => {
        if (!location) {
            Alert.alert('Error', 'Location is not available yet');
            return;
        }

        setPublishing(true);
        try {
            await locationService.publishLocation(
                location.latitude,
                location.longitude,
                location.accuracy,
                true
            );
            Alert.alert('Success', 'Location published!');
            await loadPublicLocations();
        } catch (err) {
            console.error('Error publishing location:', err);
            Alert.alert(
                'Error',
                'Error publishing location: ' + (err.response?.data?.message || err.message)
            );
        } finally {
            setPublishing(false);
        }
    };

    if (loading) {
        return (
            <View style={styles.container}>
                <ActivityIndicator size="large" color="#007AFF" />
                <Text style={styles.loadingText}>Getting location...</Text>
            </View>
        );
    }

    if (error) {
        return (
            <View style={styles.container}>
                <MaterialIcons name="location-off" size={84} color="darkred" />

                <Text style={styles.errorText}>{error}</Text>
            </View>
        );
    }

    if (!location) {
        return (
            <View style={styles.container}>
                <Text style={styles.errorText}>Location could not be retrieved</Text>
            </View>
        );
    }

    // Versión WEB con Leaflet
    if (Platform.OS === 'web') {
        if (!MapContainer || !TileLayer || !Marker) {
            // Fallback si Leaflet no se cargó
            return (
                <div style={{ padding: '20px', textAlign: 'center' }}>
                    <h2>Error loading the map</h2>
                    <p>Leaflet is not available. Please refresh the page.</p>
                </div>
            );
        }

        return (
            <div style={webStyles.container}>
                {/* Mapa */}
                <MapContainer
                    center={[location.latitude, location.longitude]}
                    zoom={15}
                    ref={mapRef}
                    style={webStyles.map}
                >
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />

                    {/* Rastreador de bounds del mapa */}
                    <MapBoundsTrackerComponent
                        questions={visibleQuestions}
                        onBoundsChange={onMapBoundsChange}
                        onVisibleQuestionsChange={onVisibleQuestionsChange}
                        mapRef={mapRef}
                    />

                    {/* Marcador de tu ubicación */}
                    <Marker position={[location.latitude, location.longitude]} icon={userLocationIcon}>
                        <Popup>
                            <div style={{ fontSize: '12px' }}>
                                <strong>Your location</strong>
                                <br />
                                {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
                                <br />
                                Accuracy: {location.accuracy?.toFixed(2) || 'N/A'} m
                            </div>
                        </Popup>
                    </Marker>

                    {/* Question Markers */}
                    {(Array.isArray(visibleQuestions) ? visibleQuestions : []).map((q) => {
                        const coords = getQuestionCoords(q);
                        if (!coords) return null;
                        const radiusKm = toNum(q?.radiusKm);
                        const { lat, lng } = coords;
                        const distanceKm = calculateDistanceInKm(
                            { latitude: location.latitude, longitude: location.longitude },
                            { latitude: lat, longitude: lng }
                        );
                        const canAnswer = !Number.isFinite(radiusKm) || radiusKm <= 0 || distanceKm <= radiusKm;
                        const questionColor = canAnswer ? '#f59e0b' : '#9ca3af';

                        return (
                            <Marker
                                key={q.id}
                                position={[lat, lng]}
                                icon={q.featured ? createFeaturedIcon() : createCustomIcon(questionColor)}
                                eventHandlers={{
                                    click: () => onQuestionPress?.(q.id),
                                }}
                            >
                                <Popup>
                                    <div style={{ fontSize: '12px' }}>
                                        <strong>{q.featured ? '⭐ ' : ''}{q.title || 'Question'}</strong>
                                        <br />
                                        <div style={{ marginBottom: '8px' }}>
                                            <CountdownText
                                                expiresAt={q.expiresAt}
                                                onExpire={() => handleQuestionExpire(q.id)}
                                            />
                                        </div>
                                        <span style={{ color: canAnswer ? '#ea580c' : '#6b7280', fontWeight: 700 }}>
                                            {canAnswer ? 'You can answer' : 'Out of your range'}
                                        </span>
                                        <br />
                                        {Number.isFinite(radiusKm) && radiusKm > 0 && (
                                            <>
                                                <span style={{ opacity: 0.85 }}>
                                                    Answer radius: {radiusKm.toFixed(2)} km
                                                </span>
                                                <br />
                                                <span style={{ opacity: 0.85 }}>
                                                    Your distance: {distanceKm.toFixed(2)} km
                                                </span>
                                                <br />
                                            </>
                                        )}
                                        <span style={{ opacity: 0.8 }}>
                                            {lat.toFixed(5)}, {lng.toFixed(5)}
                                        </span>
                                        <br />
                                        <span style={{ color: '#007AFF', fontWeight: 600 }}>Click to open</span>
                                    </div>
                                </Popup>
                            </Marker>
                        );
                    })}

                    {/* Event markers */}
                    {(Array.isArray(events) ? events : []).map((event) => {
                        const coords = getEventCoords(event);
                        if (!coords) return null;

                        const startsAt = formatDateTime(event.startsAt);
                        const endsAt = formatDateTime(event.endsAt);

                        return (
                            <Marker
                                key={event.id}
                                position={[coords.lat, coords.lng]}
                                icon={createEventIcon()}
                            >
                                <Popup>
                                    <div style={{ fontSize: '12px' }}>
                                        <strong>Event: {event.title || 'Untitled event'}</strong>
                                        <br />
                                        {event.category && (
                                            <>
                                                <span style={{ color: '#0f766e', fontWeight: 700 }}>
                                                    {event.category}
                                                </span>
                                                <br />
                                            </>
                                        )}
                                        {startsAt && (
                                            <>
                                                <span>Starts: {startsAt}</span>
                                                <br />
                                            </>
                                        )}
                                        {endsAt && (
                                            <>
                                                <span>Ends: {endsAt}</span>
                                                <br />
                                            </>
                                        )}
                                        {event.address && (
                                            <>
                                                <span>{event.address}</span>
                                                <br />
                                            </>
                                        )}
                                        <span style={{ opacity: 0.8 }}>
                                            {coords.lat.toFixed(5)}, {coords.lng.toFixed(5)}
                                        </span>
                                    </div>
                                </Popup>
                            </Marker>
                        );
                    })}

                    {/* Marcadores de ubicaciones públicas */}
                    {publicLocations &&
                        publicLocations.map((pubLocation) => (
                            <Marker
                                key={pubLocation.id}
                                position={[pubLocation.latitude, pubLocation.longitude]}
                                icon={createCustomIcon('#FF3B30')}
                            >
                                <Popup>
                                    <div style={{ fontSize: '12px' }}>
                                        <strong>User {pubLocation.user?.id || 'Unknown'}</strong>
                                        <br />
                                        {pubLocation.latitude.toFixed(6)}, {pubLocation.longitude.toFixed(6)}
                                        <br />
                                        {getTimeAgo(pubLocation.timestamp)}
                                    </div>
                                </Popup>
                            </Marker>
                        ))}
                </MapContainer>
            </div>
        );
    }

    // Fallback: Versión simple para móvil o si Leaflet no está disponible
    return (
        <ScrollView style={styles.webContainer}>
            <View style={styles.webContent}>
                <Text style={styles.webTitle}>📍 Your location</Text>
                <View style={styles.locationCard}>
                    <Text style={styles.locationText}>Latitude: {location.latitude.toFixed(6)}</Text>
                    <Text style={styles.locationText}>Longitude: {location.longitude.toFixed(6)}</Text>
                    <Text style={styles.locationText}>
                        Accuracy: {location.accuracy?.toFixed(2) || 'N/A'} m
                    </Text>
                </View>

                <TouchableOpacity
                    style={[styles.publishButton, publishing && styles.publishButtonDisabled]}
                    onPress={handlePublishLocation}
                    disabled={publishing}
                >
                    <Text style={styles.publishButtonText}>
                        {publishing ? 'Publishing...' : 'Publish location'}
                    </Text>
                </TouchableOpacity>

                <Text style={[styles.webTitle, { marginTop: 20 }]}>
                    🔴 Public locations ({publicLocations.length})
                </Text>
                {publicLocations.length === 0 ? (
                    <Text style={styles.noLocationsText}>No public locations visible</Text>
                ) : (
                    publicLocations.map((pubLocation) => (
                        <View key={pubLocation.id} style={styles.locationCard}>
                            <Text style={styles.locationText}>
                                User ID: {pubLocation.user?.id || 'Unknown'}
                            </Text>
                            <Text style={styles.locationText}>
                                Lat: {pubLocation.latitude.toFixed(6)}, Lon: {pubLocation.longitude.toFixed(6)}
                            </Text>
                            <Text style={styles.timeText}>{getTimeAgo(pubLocation.timestamp)}</Text>
                        </View>
                    ))
                )}
            </View>
        </ScrollView>
    );
}

function getTimeAgo(timestamp) {
    const now = new Date();
    const then = new Date(timestamp);
    const seconds = Math.floor((now - then) / 1000);

    if (seconds < 60) return 'hace poco';
    if (seconds < 3600) return `hace ${Math.floor(seconds / 60)}m`;
    if (seconds < 86400) return `hace ${Math.floor(seconds / 3600)}h`;
    return `hace ${Math.floor(seconds / 86400)}d`;
}

// Estilos para web
const webStyles = {
    container: {
        position: 'relative',
        width: '100%',
        height: '100vh',
        margin: 0,
        padding: 0,
    },
    map: {
        width: '100%',
        height: '100%',
    },
    publishButton: {
        position: 'fixed',
        bottom: '60px',
        right: '16px',
        backgroundColor: '#007AFF',
        color: 'white',
        border: 'none',
        padding: '12px 16px',
        borderRadius: '8px',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        zIndex: 1000,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
    },
    publishButtonDisabled: {
        backgroundColor: '#9E9E9E',
        opacity: 0.7,
        cursor: 'not-allowed',
    },
    infoBox: {
        position: 'fixed',
        bottom: '16px',
        left: '16px',
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        color: 'white',
        padding: '8px 12px',
        borderRadius: '8px',
        zIndex: 1000,
    },
    infoText: {
        fontSize: '12px',
        color: 'white',
    },
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5F5F5',
    },
    loadingText: {
        marginTop: 16,
        fontSize: 16,
        color: '#757575',
    },
    errorText: {
        fontSize: 16,
        color: '#D32F2F',
        textAlign: 'center',
        padding: 16,
    },
    // Web Map Styles
    webMapContainer: {
        flex: 1,
        position: 'relative',
    },
    mapDivWeb: {
        flex: 1,
        width: '100%',
        height: 400,
    },
    publishButton: {
        position: 'absolute',
        bottom: 60,
        right: 16,
        backgroundColor: '#007AFF',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 8,
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 1000,
    },
    publishButtonDisabled: {
        backgroundColor: '#9E9E9E',
        opacity: 0.7,
    },
    publishButtonText: {
        color: '#FFF',
        fontSize: 16,
        fontWeight: '600',
    },
    infoBox: {
        position: 'absolute',
        bottom: 16,
        left: 16,
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        zIndex: 1000,
    },
    infoText: {
        color: '#FFF',
        fontSize: 12,
    },
    // Fallback styles
    webContainer: {
        flex: 1,
        backgroundColor: '#F5F5F5',
        padding: 16,
    },
    webContent: {
        paddingBottom: 20,
    },
    webTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#000',
        marginBottom: 12,
        marginTop: 16,
    },
    locationCard: {
        backgroundColor: '#FFF',
        borderRadius: 12,
        padding: 12,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    locationText: {
        fontSize: 14,
        color: '#000',
        marginBottom: 4,
    },
    timeText: {
        fontSize: 12,
        color: '#757575',
        marginTop: 4,
    },
    noLocationsText: {
        fontSize: 14,
        color: '#757575',
        textAlign: 'center',
        padding: 16,
        fontStyle: 'italic',
    },
});
