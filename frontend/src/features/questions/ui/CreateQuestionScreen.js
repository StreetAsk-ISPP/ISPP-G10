import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TextInput,
  TouchableOpacity,
  Platform,
  ScrollView,
  useWindowDimensions,
  Modal,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Slider from '@react-native-community/slider';
import Toast from 'react-native-toast-message';

import apiClient from '../../../shared/services/http/apiClient';
import MapPickerWeb from '../../home/ui/components/MapPickerWeb';
import { useAuth } from '../../../app/providers/AuthProvider';
import { calculateDistanceInKm } from '../../../shared/utils/helpers';

const FREE_FIXED_RADIUS_KM = 0.5;
const FREE_FIXED_RADIUS_M = 500;
const PREMIUM_MIN_RADIUS_M = 50;
const PREMIUM_MAX_RADIUS_M = 1000;
const FREE_DURATION_HOURS = 6;
const PREMIUM_MIN_DURATION_HOURS = 1;
const PREMIUM_MAX_DURATION_HOURS = 24;
const FAKE_AD_DURATION_SECONDS = 30;
const DEFAULT_FALLBACK_LAT = 37.3886;
const DEFAULT_FALLBACK_LNG = -5.9823;

const SEARCH_VIEWBOX_DELTA = 0.35;

const parseSearchCoordinate = (value) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
};

const buildNominatimSearchUrl = (query, centerLat, centerLng, bounded = true) => {
    const params = new URLSearchParams({
        format: 'json',
        limit: '8',
        addressdetails: '1',
        q: query,
    });

    if (bounded && Number.isFinite(centerLat) && Number.isFinite(centerLng)) {
        params.set(
            'viewbox',
            [
                centerLng - SEARCH_VIEWBOX_DELTA,
                centerLat + SEARCH_VIEWBOX_DELTA,
                centerLng + SEARCH_VIEWBOX_DELTA,
                centerLat - SEARCH_VIEWBOX_DELTA,
            ].join(',')
        );
        params.set('bounded', '1');
    }

    return `https://nominatim.openstreetmap.org/search?${params.toString()}`;
};

const distanceToCenter = (item, centerLat, centerLng) => {
    const lat = Number(item.lat);
    const lon = Number(item.lon);

    if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
        return Number.MAX_VALUE;
    }

    if (!Number.isFinite(centerLat) || !Number.isFinite(centerLng)) {
        return Number.MAX_VALUE;
    }

    return calculateDistanceInKm(
        { latitude: centerLat, longitude: centerLng },
        { latitude: lat, longitude: lon }
    );
};

const mapAndPrioritizeSearchResults = (data, centerLat, centerLng) => {
    return (Array.isArray(data) ? data : [])
        .map((item) => ({
            label: item.display_name,
            lat: Number(item.lat),
            lon: Number(item.lon),
            distanceKm: distanceToCenter(item, centerLat, centerLng),
        }))
        .sort((a, b) => a.distanceKm - b.distanceKm);
};

const addHoursISO = (hours) => {
  const nowMs = Date.now();
  return new Date(nowMs + hours * 3600000).toISOString();
};

const parseRadiusMeters = (rawValue) => {
  const normalized = String(rawValue ?? '')
    .replace(',', '.')
    .trim();
  const value = Number(normalized);
  return Number.isFinite(value) && value > 0 ? value : null;
};

const parseHours = (rawValue) => {
  const normalized = String(rawValue ?? '').trim();
  const value = Number(normalized);
  return Number.isFinite(value) ? Math.floor(value) : null;
};

const isPremiumRadiusValid = (valueMeters) => {
  return (
    valueMeters !== null &&
    valueMeters >= PREMIUM_MIN_RADIUS_M &&
    valueMeters <= PREMIUM_MAX_RADIUS_M
  );
};

const isPremiumHoursValid = (value) => {
  return (
    value !== null && value >= PREMIUM_MIN_DURATION_HOURS && value <= PREMIUM_MAX_DURATION_HOURS
  );
};

const REQUIRED_LOCATION_MESSAGE = 'Please select a location on the map before submitting your question.';

const extractErrorMessage = (error) => {
  const message = error?.response?.data?.message;
  if (typeof message === 'string' && message.trim()) {
    return message.trim();
  }

  if (message && typeof message === 'object') {
    const merged = Object.values(message)
      .filter((value) => typeof value === 'string' && value.trim())
      .join(' ');
    if (merged) {
      return merged;
    }
  }

  if (typeof error?.response?.data === 'string' && error.response.data.trim()) {
    return error.response.data.trim();
  }

  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }

  return 'We could not create your question right now. Please try again.';
};

const isLocationValidationError = (message) => {
  const normalized = String(message || '').toLowerCase();
  return normalized.includes('location')
    || normalized.includes('latitude')
    || normalized.includes('longitude')
    || normalized.includes('geopoint');
};

export default function CreateQuestionScreen({ navigation, route }) {
  const { user } = useAuth();
  const { width } = useWindowDimensions();
  const isNarrow = width < 500;

  const eventData = route?.params?.eventData;
  const eventId = route?.params?.eventId || eventData?.id || null;
  const eventTitle = route?.params?.eventTitle || eventData?.title || null;
  const eventLoc = eventData?.location ?? null;
  const eventLat = Number(eventLoc?.latitude ?? eventLoc?.lat ?? eventLoc?.y ?? eventData?.latitude ?? eventData?.lat);
  const eventLng = Number(
    eventLoc?.longitude ?? eventLoc?.lng ?? eventLoc?.lon ?? eventLoc?.x ?? eventData?.longitude ?? eventData?.lng
  );
  const hasEventFixedLocation = eventId && Number.isFinite(eventLat) && Number.isFinite(eventLng);

  const [place, setPlace] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isPremium, setIsPremium] = useState(false);
  const [hoursInput, setHoursInput] = useState(String(FREE_DURATION_HOURS));
  const [radiusKm, setRadiusKm] = useState(FREE_FIXED_RADIUS_KM);
  const [radiusInput, setRadiusInput] = useState(String(FREE_FIXED_RADIUS_M));
  const [latitude, setLatitude] = useState(null);
  const [longitude, setLongitude] = useState(null);
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [pickedLabel, setPickedLabel] = useState('');
  const [pickMode, setPickMode] = useState(false);
  const [tempLat, setTempLat] = useState(null);
  const [tempLng, setTempLng] = useState(null);
  const [userLat, setUserLat] = useState(null);
  const [userLng, setUserLng] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [focusedField, setFocusedField] = useState(null);
  const [todayQuestionCount, setTodayQuestionCount] = useState(0);
  const [showFakeAd, setShowFakeAd] = useState(false);
  const [adSecondsLeft, setAdSecondsLeft] = useState(FAKE_AD_DURATION_SECONDS);
  const [queuedPayload, setQueuedPayload] = useState(null);
  const [canSkipAd, setCanSkipAd] = useState(false);
  const [showStreetCoinConsentModal, setShowStreetCoinConsentModal] = useState(false);
  const [streetCoinConsentPayload, setStreetCoinConsentPayload] = useState(null);
  const [streetCoinBalance, setStreetCoinBalance] = useState(null);
  const [locationError, setLocationError] = useState('');
  const [locationSectionY, setLocationSectionY] = useState(0);
  const formScrollRef = useRef(null);

  const getCurrentPositionWeb = useCallback(() => {
    if (Platform.OS !== 'web' || !navigator.geolocation) {
      return Promise.resolve(null);
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          resolve({
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
          });
        },
        () => resolve(null),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    });
  }, []);

  const focusLocationSection = useCallback(() => {
    if (!formScrollRef.current || typeof formScrollRef.current.scrollTo !== 'function') {
      return;
    }

    formScrollRef.current.scrollTo({
      y: Math.max(locationSectionY - 24, 0),
      animated: true,
    });
  }, [locationSectionY]);

  const clearLocationError = useCallback(() => {
    setLocationError('');
  }, []);

  const submitQuestion = useCallback(
    async (payload) => {
      console.log(payload);

      setIsSubmitting(true);
      try {
        const response = await apiClient.post('/api/v1/questions', payload);
        console.log('[CreateQuestionScreen] Created question response:', response?.data);

        if (Platform.OS === 'web' && typeof window !== 'undefined') {
          window.dispatchEvent(
            new CustomEvent('streetask:question-created', {
              detail: {
                questionId: response?.data?.id ?? null,
                eventId,
                question: response?.data ?? null,
              },
            })
          );
        }

        if (eventId) {
          const eventQuestionsResponse = await apiClient.get(`/api/v1/events/${eventId}/questions`);
          const eventQuestionsPayload = eventQuestionsResponse?.data;
          const eventQuestions = Array.isArray(eventQuestionsPayload)
            ? eventQuestionsPayload
            : Array.isArray(eventQuestionsPayload?.content)
              ? eventQuestionsPayload.content
              : [];

          const createdQuestionId = response?.data?.id;
          const createdQuestionInEventList = createdQuestionId
            ? eventQuestions.some((question) => question?.id === createdQuestionId)
            : false;

          console.log('[CreateQuestionScreen] Event questions right after create:', {
            eventId,
            createdQuestionId,
            totalQuestions: eventQuestions.length,
            createdQuestionInEventList,
            eventQuestions,
          });
        }

        Toast.show({
          type: 'success',
          text1: 'Success',
          text2: 'Question created!',
          position: 'top',
        });
        if (eventId) {
          navigation.navigate({
            name: 'EventDetails',
            params: { eventId, refreshNonce: Date.now() },
            merge: true,
          });
          return;
        }
        navigation.goBack();
      } catch (e) {
        const errorMessage = extractErrorMessage(e);

        if (!eventId && isLocationValidationError(errorMessage)) {
          setLocationError(REQUIRED_LOCATION_MESSAGE);
          focusLocationSection();
          Toast.show({
            type: 'error',
            text1: 'Location required',
            text2: REQUIRED_LOCATION_MESSAGE,
            position: 'top',
          });
          return;
        }

        Toast.show({
          type: 'error',
          text1: 'Error',
          text2: errorMessage,
          position: 'top',
        });
      } finally {
        setIsSubmitting(false);
      }
    },
    [navigation, eventId, focusLocationSection]
  );

  useEffect(() => {
    if (hasEventFixedLocation) {
      setLatitude(eventLat);
      setLongitude(eventLng);
      clearLocationError();
      setPlace((prev) => (eventData?.address ? eventData.address : (prev?.trim() ? prev : `(${eventLat.toFixed(5)}, ${eventLng.toFixed(5)})`)));
      setPickedLabel((prev) => (eventData?.address ? eventData.address : (prev?.trim() ? prev : `(${eventLat.toFixed(5)}, ${eventLng.toFixed(5)})`)));
      return;
    }

    if (eventData?.address) {
      setPlace((prev) => (prev?.trim() ? prev : eventData.address));
      setPickedLabel((prev) => (prev?.trim() ? prev : eventData.address));
    }
  }, [eventData, eventLat, eventLng, hasEventFixedLocation, clearLocationError]);

  useEffect(() => {
    const loadUserPlanSettings = async () => {
      if (!user?.id) return;
      try {
        const response = await apiClient.get(`/api/v1/users/${user.id}`);
        console.log(response.data);
        const premiumFlag = response?.data?.premiumActive === true;
        setIsPremium(premiumFlag);

        if (!premiumFlag) {
          setRadiusKm(FREE_FIXED_RADIUS_KM);
          setRadiusInput(String(FREE_FIXED_RADIUS_M));
          setHoursInput(String(FREE_DURATION_HOURS));
        }
      } catch (e) {
        console.warn('Unable to load user plan settings:', e?.message || e);
      }
    };

    loadUserPlanSettings();
  }, [user?.id, user?.accountType]);

  useEffect(() => {
    let isMounted = true;

    const loadTodayQuestionCount = async () => {
      if (!user?.id) return;
      try {
        const response = await apiClient.get('/api/v1/questions/today-count', {
          params: eventId ? { eventId } : undefined,
        });
        if (!isMounted) return;
        setTodayQuestionCount(response?.data ?? 0);
      } catch (e) {
        console.warn('Unable to load today question count:', e?.message || e);
      }
    };

    loadTodayQuestionCount();
    return () => {
      isMounted = false;
    };
  }, [user?.id, eventId]);

  useEffect(() => {
    let isMounted = true;

    const loadStreetCoinBalance = async () => {
      const mustSpendStreetCoin = !isPremium && todayQuestionCount >= 3;
      if (!mustSpendStreetCoin) {
        if (isMounted) {
          setStreetCoinBalance(null);
        }
        return;
      }

      try {
        const response = await apiClient.get('/api/v1/streetcoins/balance');
        if (!isMounted) return;
        const balance = Number(response?.data?.balance ?? 0);
        setStreetCoinBalance(Number.isFinite(balance) ? balance : 0);
      } catch {
        if (isMounted) {
          setStreetCoinBalance(0);
        }
      }
    };

    loadStreetCoinBalance();
    return () => {
      isMounted = false;
    };
  }, [isPremium, todayQuestionCount]);

  useEffect(() => {
    let isMounted = true;

    const preloadCurrentLocation = async () => {
      if (eventId) {
        return;
      }

      const coords = await getCurrentPositionWeb();
      if (!isMounted || !coords) {
        return;
      }

      const { lat, lng } = coords;
      setUserLat(lat);
      setUserLng(lng);
      setLatitude((prev) => (typeof prev === 'number' ? prev : lat));
      setLongitude((prev) => (typeof prev === 'number' ? prev : lng));
      clearLocationError();
      setPlace((prev) => (prev?.trim() ? prev : `(${lat.toFixed(5)}, ${lng.toFixed(5)})`));
    };

    preloadCurrentLocation();

    return () => {
      isMounted = false;
    };
  }, [eventId, getCurrentPositionWeb, clearLocationError]);

  useEffect(() => {
    if (!showFakeAd) return;

    const skipTimer = setTimeout(() => {
      setCanSkipAd(true);
    }, 3000); //3 segundos de cooldown hasta que salga la x

    const interval = setInterval(() => {
      setAdSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      clearTimeout(skipTimer);
      clearInterval(interval);
    };
  }, [showFakeAd]);

  useEffect(() => {
    if (!showFakeAd || adSecondsLeft > 0 || !queuedPayload) {
      return;
    }

    setShowFakeAd(false);
    setAdSecondsLeft(FAKE_AD_DURATION_SECONDS);
    const payloadToSubmit = queuedPayload;
    setQueuedPayload(null);
    submitQuestion(payloadToSubmit);
  }, [showFakeAd, adSecondsLeft, queuedPayload, submitQuestion]);

  const parsedRadiusMeters = parseRadiusMeters(radiusInput);
  const parsedHours = parseHours(hoursInput);
  const premiumRadiusValid = !isPremium || isPremiumRadiusValid(parsedRadiusMeters);
  const premiumHoursValid = !isPremium || isPremiumHoursValid(parsedHours);
  const showRadiusRangeError = isPremium && radiusInput.trim().length > 0 && !premiumRadiusValid;
  const dailyLimitReached = eventId ? todayQuestionCount >= 3 : (!isPremium && todayQuestionCount >= 3);
  const requiresStreetCoinSpend = !isPremium && todayQuestionCount >= 3;
  const hasZeroStreetCoins = requiresStreetCoinSpend && streetCoinBalance === 0;

  const canPost = useMemo(
    () =>
      title.trim() &&
      content.trim() &&
      premiumRadiusValid &&
      premiumHoursValid &&
      !hasZeroStreetCoins &&
      !isSubmitting,
    [title, content, premiumRadiusValid, premiumHoursValid, hasZeroStreetCoins, isSubmitting]
  );

  const proceedWithStreetCoinConsent = useCallback((payload) => {
    const payloadWithConsent = {
      ...payload,
      confirmStreetCoinSpend: true,
    };

    if (!isPremium) {
      setQueuedPayload(payloadWithConsent);
      setAdSecondsLeft(FAKE_AD_DURATION_SECONDS);
      setShowFakeAd(true);
      return;
    }

    submitQuestion(payloadWithConsent);
  }, [isPremium, submitQuestion]);

  const showStreetCoinConsentDialog = useCallback((payload) => {
    setStreetCoinConsentPayload(payload);
    setShowStreetCoinConsentModal(true);
  }, []);

  const cancelStreetCoinConsent = useCallback(() => {
    setShowStreetCoinConsentModal(false);
    setStreetCoinConsentPayload(null);
  }, []);

  const confirmStreetCoinConsent = useCallback(() => {
    if (!streetCoinConsentPayload) {
      return;
    }
    const payload = streetCoinConsentPayload;
    setShowStreetCoinConsentModal(false);
    setStreetCoinConsentPayload(null);
    proceedWithStreetCoinConsent(payload);
  }, [streetCoinConsentPayload, proceedWithStreetCoinConsent]);

    const searchAddress = async () => {
        const q = place.trim();

        if (!q) {
            Toast.show({
            type: 'info',
            text1: 'Search field is empty',
            text2: 'Enter an address or place before searching.',
            position: 'top',
            });
            return;
        }

        setSearching(true);

        try {
            const centerLat = Number.isFinite(userLat) ? userLat : latitude;
            const centerLng = Number.isFinite(userLng) ? userLng : longitude;

            const localUrl = buildNominatimSearchUrl(q, centerLat, centerLng, true);

            let res = await fetch(localUrl, {
            headers: {
                Accept: 'application/json',
                'Accept-Language': 'es',
            },
            });

            if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
            }

            let data = await res.json();
            let items = mapAndPrioritizeSearchResults(data, centerLat, centerLng);

            if (items.length === 0) {
            const globalUrl = buildNominatimSearchUrl(q, centerLat, centerLng, false);

            res = await fetch(globalUrl, {
                headers: {
                Accept: 'application/json',
                'Accept-Language': 'es',
                },
            });

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }

            data = await res.json();
            items = mapAndPrioritizeSearchResults(data, centerLat, centerLng);
            }

            setSearchResults(items);

            if (items.length === 0) {
            Toast.show({
                type: 'info',
                text1: 'No results',
                text2: 'No address was found.',
                position: 'top',
            });
            return;
            }

            if (items.length === 1) {
            setLatitude(items[0].lat);
            setLongitude(items[0].lon);
            setPlace(items[0].label);
            setPickedLabel(items[0].label);
            setSearchResults([]);
            clearLocationError();
            }
        } catch (e) {
            console.error('Nominatim search error:', e);

            Toast.show({
            type: 'error',
            text1: 'Search error',
            text2: 'Could not search the address.',
            position: 'top',
            });
        } finally {
            setSearching(false);
        }
    };

  const openMapPick = useCallback(async () => {
    if (eventId) {
      return;
    }

    let nextLat = typeof latitude === 'number' ? latitude : null;
    let nextLng = typeof longitude === 'number' ? longitude : null;

    if (typeof nextLat !== 'number' || typeof nextLng !== 'number') {
      if (typeof userLat === 'number' && typeof userLng === 'number') {
        nextLat = userLat;
        nextLng = userLng;
      } else {
        const coords = await getCurrentPositionWeb();
        if (coords) {
          nextLat = coords.lat;
          nextLng = coords.lng;
          setUserLat(coords.lat);
          setUserLng(coords.lng);
        }
      }
    }

    setTempLat(typeof nextLat === 'number' ? nextLat : DEFAULT_FALLBACK_LAT);
    setTempLng(typeof nextLng === 'number' ? nextLng : DEFAULT_FALLBACK_LNG);
    setPickMode(true);
  }, [eventId, latitude, longitude, userLat, userLng, getCurrentPositionWeb]);

  const cancelMapPick = () => {
    setPickMode(false);
    setTempLat(null);
    setTempLng(null);
  };

  const confirmMapPick = () => {
    if (typeof tempLat !== 'number' || typeof tempLng !== 'number') {
      Toast.show({
        type: 'info',
        text1: 'Select a point',
        text2: 'Tap on the map to choose a location.',
        position: 'top',
      });
      return;
    }
    setLatitude(tempLat);
    setLongitude(tempLng);
    setPlace(`(${tempLat.toFixed(5)}, ${tempLng.toFixed(5)})`);
    setPickedLabel('');
    setSearchResults([]);
    clearLocationError();
    setPickMode(false);
  };

  const onHoursInputChange = (text) => {
    if (!isPremium) {
      return;
    }
    setHoursInput(text);
  };

  const skipAd = () => {
    if (!canSkipAd || !queuedPayload) return;

    setShowFakeAd(false);
    setAdSecondsLeft(FAKE_AD_DURATION_SECONDS);
    setCanSkipAd(false);

    const payloadToSubmit = queuedPayload;
    setQueuedPayload(null);
    submitQuestion(payloadToSubmit);
  };
  const openStreetCoinsShop = useCallback(() => {
    navigation.replace('Balance', {
      openShopModal: true,
      checkoutOrigin: 'create-question-limit',
    });
  }, [navigation]);

  const onPost = async () => {
    if (!canPost) return;

    let finalRadiusKm = FREE_FIXED_RADIUS_KM;
    let finalHours = FREE_DURATION_HOURS;

    if (isPremium) {
      const premiumRadiusMeters = parseRadiusMeters(radiusInput);
      if (!isPremiumRadiusValid(premiumRadiusMeters)) {
        Toast.show({
          type: 'error',
          text1: 'Invalid radius',
          text2: 'Premium radius must be between 50 m and 1000 m.',
          position: 'top',
        });
        return;
      }

      const premiumHours = parseHours(hoursInput);
      if (!isPremiumHoursValid(premiumHours)) {
        Toast.show({
          type: 'error',
          text1: 'Invalid duration',
          text2: 'Premium duration must be between 1h and 24h.',
          position: 'top',
        });
        return;
      }

      finalRadiusKm = premiumRadiusMeters / 1000;
      finalHours = premiumHours;
    }

    const finalLatitude = hasEventFixedLocation ? eventLat : latitude;
    const finalLongitude = hasEventFixedLocation ? eventLng : longitude;

    if (!Number.isFinite(finalLatitude) || !Number.isFinite(finalLongitude)) {
      setLocationError(REQUIRED_LOCATION_MESSAGE);
      focusLocationSection();
      Toast.show({
        type: 'error',
        text1: 'Location required',
        text2: REQUIRED_LOCATION_MESSAGE,
        position: 'top',
      });
      return;
    }

    const payload = {
      title: title.trim(),
      content: content.trim(),
      radiusKm: finalRadiusKm,
      expiresAt: addHoursISO(finalHours),
      location: { latitude: finalLatitude, longitude: finalLongitude },
    };

    if (eventId) {
      payload.event = { id: eventId };
    }

    if (requiresStreetCoinSpend) {
      try {
        const balanceResponse = await apiClient.get('/api/v1/streetcoins/balance');
        const balance = Number(balanceResponse?.data?.balance ?? 0);
        setStreetCoinBalance(balance);

        if (balance < 1) {
          Toast.show({
            type: 'error',
            text1: 'Insufficient StreetCoins',
            text2: 'You need at least 1 StreetCoin to post an extra question.',
            position: 'top',
          });
          openStreetCoinsShop();
          return;
        }

        showStreetCoinConsentDialog(payload);
      } catch (e) {
        Toast.show({
          type: 'error',
          text1: 'Error',
          text2: e.response?.data?.message || e.message,
          position: 'top',
        });
      }
      return;
    }


    if (!isPremium) {
      setQueuedPayload(payload);
      setAdSecondsLeft(FAKE_AD_DURATION_SECONDS);
      setShowFakeAd(true);
      return;
    }

    submitQuestion(payload);
  };

  const selectedDisplay = pickedLabel
    ? pickedLabel
    : typeof latitude === 'number' && typeof longitude === 'number'
      ? `(${latitude.toFixed(5)}, ${longitude.toFixed(5)})`
      : 'None';

  // ━━━ MAP PICK MODE ━━━
  if (pickMode) {
    return (
      <SafeAreaView style={styles.screen}>
        <View style={{ flex: 1 }}>
          <View style={styles.mapFull}>
            <MapPickerWeb
              latitude={tempLat}
              longitude={tempLng}
              userLat={userLat ?? tempLat}
              userLng={userLng ?? tempLng}
              radiusKm={radiusKm}
              pickEnabled
              tempLat={tempLat}
              tempLng={tempLng}
              onPick={(lat, lng) => {
                setTempLat(lat);
                setTempLng(lng);
              }}
            />
          </View>
          <View style={styles.mapOverlay} pointerEvents="box-none">
            <View style={styles.mapHint} pointerEvents="auto">
              <Text style={styles.mapHintText}>Tap on the map to pick the question location</Text>
              <Text style={styles.mapHintCoords}>
                {tempLat?.toFixed?.(5) ?? '--'}, {tempLng?.toFixed?.(5) ?? '--'}
              </Text>
              <Text style={styles.mapRadiusLabel}>Response radius (m)</Text>
              {isPremium ? (
                <View style={styles.sliderBlock}>
                  <Slider
                    minimumValue={PREMIUM_MIN_RADIUS_M}
                    maximumValue={PREMIUM_MAX_RADIUS_M}
                    step={10}
                    value={parsedRadiusMeters ?? FREE_FIXED_RADIUS_M}
                    onValueChange={(value) => {
                      setRadiusInput(String(Math.round(value)));
                      setRadiusKm(value / 1000);
                    }}
                    minimumTrackTintColor="#a52019"
                    maximumTrackTintColor="#e5e7eb"
                    thumbTintColor="#a52019"
                  />

                  <View style={styles.sliderLabels}>
                    <Text style={styles.sliderMin}>50 m</Text>
                    <Text style={styles.radiusValueText}>
                      {parsedRadiusMeters ?? FREE_FIXED_RADIUS_M} m
                    </Text>
                    <Text style={styles.sliderMax}>1000 m</Text>
                  </View>
                </View>
              ) : (
                <View style={styles.lockedBox}>
                  <Ionicons name="lock-closed" size={14} color="#6b7280" />
                  <Text style={styles.lockedText}>Fixed for free plan: 500 m</Text>
                </View>
              )}
              {showRadiusRangeError ? (
                <Text style={styles.radiusErrorText}>
                  Premium radius must be between 50 m and 1000 m.
                </Text>
              ) : null}
              <Text style={styles.mapZoneText}>
                The red circle is the response area for this question.
              </Text>
            </View>
            <View style={styles.mapBtnRow} pointerEvents="auto">
              <TouchableOpacity
                style={styles.mapCancelBtn}
                onPress={cancelMapPick}
                activeOpacity={0.8}
              >
                <Text style={styles.mapCancelLabel}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.mapOkBtn}
                onPress={confirmMapPick}
                activeOpacity={0.8}
              >
                <Text style={styles.mapConfirmLabel}>Confirm</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // ━━━ FORM MODE ━━━
  return (
    <SafeAreaView style={styles.screen}>
      <View style={{ flex: 1, position: 'relative' }}>
        {/* Map background */}
        <View style={styles.mapBgPreview}>
          <MapPickerWeb
            latitude={latitude ?? userLat ?? DEFAULT_FALLBACK_LAT}
            longitude={longitude ?? userLng ?? DEFAULT_FALLBACK_LNG}
            userLat={userLat ?? DEFAULT_FALLBACK_LAT}
            userLng={userLng ?? DEFAULT_FALLBACK_LNG}
            radiusKm={radiusKm}
            pickEnabled={false}
            tempLat={tempLat}
            tempLng={tempLng}
            onPick={() => { }}
          />
        </View>

        {/* White card form */}
        <ScrollView
          ref={formScrollRef}
          style={styles.formScroll}
          contentContainerStyle={[
            styles.formScrollContent,
            { maxWidth: isNarrow ? '100%' : 520, alignSelf: 'center', width: '100%' },
          ]}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.card}>
            {/* Back button */}
            <TouchableOpacity
              style={styles.backRow}
              onPress={() => navigation.goBack()}
              activeOpacity={0.7}
            >
              <Ionicons name="chevron-back" size={22} color="#6b7280" />
              <Text style={styles.backText}>Back</Text>
            </TouchableOpacity>

            <Text style={styles.heading}>
              {eventTitle ? `Create a Question for ${eventTitle}` : 'Create a Question'}
            </Text>

            {eventId ? (
              <Text style={styles.helperText}>
                This question will be published inside the selected event forum.
              </Text>
            ) : null}

            {/* Location section */}
            <View
              onLayout={({ nativeEvent }) => setLocationSectionY(nativeEvent.layout.y)}
              style={[
                styles.locationSectionContainer,
                !eventId && locationError ? styles.locationSectionContainerError : null,
              ]}
            >
              <Text style={styles.sectionLabel}>{eventId ? 'Location' : 'Location *'}</Text>
              {!eventId ? (
                <Text style={styles.locationRequiredHint}>
                  Select a point on the map before posting your question.
                </Text>
              ) : null}

              {eventId ? (
                <View style={styles.lockedLocationCard}>
                  <View style={styles.lockedLocationTitleRow}>
                    <Ionicons name="lock-closed" size={15} color="#1d4ed8" />
                    <Text style={styles.lockedLocationTitle}>Fixed event location</Text>
                  </View>
                  <Text style={styles.lockedLocationText}>
                    {place?.trim() || (hasEventFixedLocation ? `(${eventLat.toFixed(5)}, ${eventLng.toFixed(5)})` : 'Location not available')}
                  </Text>
                </View>
              ) : (
                <>
                  <View style={[styles.inputWrapper, focusedField === 'place' && styles.inputFocused]}>
                    <Ionicons
                      name="search-outline"
                      size={18}
                      color="#9ca3af"
                      style={{ marginRight: 8 }}
                    />
                    <TextInput
                      value={place}
                      onChangeText={(t) => {
                        setPlace(t);
                        setSearchResults([]);
                        setPickedLabel('');
                      }}
                      onSubmitEditing={searchAddress}
                      returnKeyType="search"
                      placeholder="Search address or place..."
                      placeholderTextColor="#9ca3af"
                      style={styles.input}
                      onFocus={() => setFocusedField('place')}
                      onBlur={() => setFocusedField(null)}
                    />
                  </View>

                  <View style={styles.locationBtnRow}>
                    <TouchableOpacity
                      style={[styles.btnOutline, { flex: 1, opacity: searching ? 0.5 : 1 }]}
                      onPress={searchAddress}
                      disabled={searching}
                      activeOpacity={0.7}
                    >
                      <Ionicons name="search" size={16} color="#a52019" />
                      <Text style={styles.btnOutlineText}>{searching ? 'Searching...' : 'Search'}</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      style={[styles.btnOutline, { flex: 1 }]}
                      onPress={openMapPick}
                      activeOpacity={0.7}
                    >
                      <Ionicons name="location" size={16} color="#a52019" />
                      <Text style={styles.btnOutlineText}>Pick on map</Text>
                    </TouchableOpacity>
                  </View>
                </>
              )}

              {/* Search results */}
              {!eventId && searchResults.length > 1 &&
                searchResults.map((r, idx) => (
                  <TouchableOpacity
                    key={idx}
                    style={styles.resultItem}
                    onPress={() => {
                      setLatitude(r.lat);
                      setLongitude(r.lon);
                      setPickedLabel(r.label);
                      setSearchResults([]);
                      clearLocationError();
                    }}
                    activeOpacity={0.7}
                  >
                    <Ionicons name="location-outline" size={16} color="#667eea" />
                    <Text style={styles.resultText} numberOfLines={2}>
                      {r.label}
                    </Text>
                  </TouchableOpacity>
                ))}

              <View style={styles.selectedRow}>
                <Ionicons name="pin" size={14} color="#6b7280" />
                <Text style={styles.selectedText} numberOfLines={1}>
                  Selected: {selectedDisplay}
                </Text>
              </View>

              {!eventId && locationError ? (
                <Text style={styles.locationInlineError}>{locationError}</Text>
              ) : null}
            </View>
            <Text style={styles.helperText}>
              {eventId
                ? 'Event questions use the event location and cannot be moved.'
                : isPremium
                  ? 'Premium radius: choose between 50 m and 1000 m in Pick on map.'
                  : 'Free plan radius is fixed to 500 m.'}
            </Text>

            {/* Topic */}
            <Text style={styles.sectionLabel}>Topic *</Text>
            <View style={[styles.inputWrapper, focusedField === 'title' && styles.inputFocused]}>
              <TextInput
                value={title}
                onChangeText={setTitle}
                placeholder="What is this about?"
                placeholderTextColor="#9ca3af"
                style={styles.input}
                onFocus={() => setFocusedField('title')}
                onBlur={() => setFocusedField(null)}
              />
            </View>

            {/* Question */}
            <Text style={styles.sectionLabel}>Question *</Text>
            <View
              style={[
                styles.inputWrapper,
                styles.inputMultiline,
                focusedField === 'content' && styles.inputFocused,
              ]}
            >
              <TextInput
                value={content}
                onChangeText={setContent}
                placeholder="Type your question here..."
                placeholderTextColor="#9ca3af"
                style={[styles.input, { height: 80, textAlignVertical: 'top' }]}
                multiline
                onFocus={() => setFocusedField('content')}
                onBlur={() => setFocusedField(null)}
              />
            </View>

            {/* Time info */}
            <View style={styles.timeChip}>
              <Ionicons name="time-outline" size={16} color="#92400e" />
              {isPremium ? (
                <View style={styles.timePremiumRow}>
                  <Text style={styles.timeChipText}>Duration (1h-24h)</Text>
                  <TextInput
                    value={hoursInput}
                    onChangeText={onHoursInputChange}
                    keyboardType={Platform.OS === 'ios' ? 'number-pad' : 'numeric'}
                    placeholder="2"
                    placeholderTextColor="#a16207"
                    style={styles.timeInput}
                    onBlur={() => {
                      const parsed = parseHours(hoursInput);
                      if (parsed !== null) {
                        setHoursInput(String(parsed));
                      }
                    }}
                  />
                  <Text style={styles.timeChipText}>h</Text>
                </View>
              ) : (
                <View>
                  <Text style={styles.timeChipText}>Duration: 2h (fixed in free plan)</Text>
                  <Text style={styles.timeChipText}>
                    {eventId
                      ? 'Max 3 questions per day in this event'
                      : 'Max 3 questions a day (fixed in free plan)'}
                  </Text>
                </View>
              )}
            </View>

            {dailyLimitReached && (
              <View style={styles.dailyLimitWarning}>
                <Ionicons name="alert-circle" size={16} color="#dc2626" />
                <View style={{ flex: 1, marginLeft: 10 }}>
                  <Text style={styles.dailyLimitTitle}>Daily limit reached</Text>
                  <Text style={styles.dailyLimitText}>
                    {eventId
                      ? 'You reached your 3 free questions in this event. You can continue by spending 1 StreetCoin per extra question.'
                      : 'You reached your 3 free questions today. You can continue by spending 1 StreetCoin per extra question, or upgrade to premium.'}
                  </Text>
                  {hasZeroStreetCoins ? (
                    <Text style={styles.dailyLimitNoCoinsText}>
                      You have 0 StreetCoins. Use Shop to continue.
                    </Text>
                  ) : null}
                </View>
                <TouchableOpacity
                  style={styles.shopBtn}
                  onPress={openStreetCoinsShop}
                  activeOpacity={0.7}
                >
                  <Text style={styles.shopBtnText}>Shop</Text>
                </TouchableOpacity>
              </View>
            )}

            {/* Buttons */}
            <View style={styles.actionRow}>
              <TouchableOpacity
                style={styles.cancelBtn}
                onPress={() => navigation.goBack()}
                activeOpacity={0.7}
              >
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.postBtn, !canPost && { opacity: 0.4 }]}
                onPress={onPost}
                disabled={!canPost}
                activeOpacity={0.8}
              >
                <Text style={styles.postBtnText}>
                  {isSubmitting ? 'Posting...' : 'Post Question'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </View>
      <Modal
        visible={showStreetCoinConsentModal}
        transparent
        animationType="fade"
        onRequestClose={cancelStreetCoinConsent}
      >
        <View style={styles.consentOverlay}>
          <View style={styles.consentCard}>
            <View style={styles.consentIconWrap}>
              <Ionicons name="cash-outline" size={20} color="#a52019" />
            </View>
            <Text style={styles.consentTitle}>Spend 1 StreetCoin?</Text>
            <Text style={styles.consentText}>
              {eventId
                ? 'You already reached today\'s free limit in this event. Confirm to spend 1 StreetCoin and publish this question.'
                : 'You already reached today\'s free limit. Confirm to spend 1 StreetCoin and publish this question.'}
            </Text>
            {typeof streetCoinBalance === 'number' ? (
              <Text style={styles.consentBalanceText}>
                Current balance: {streetCoinBalance} StreetCoins{`\n`}
                After this question: {Math.max(0, streetCoinBalance - 1)} StreetCoins
              </Text>
            ) : null}
            <View style={styles.consentActions}>
              <TouchableOpacity
                style={styles.consentCancelBtn}
                onPress={cancelStreetCoinConsent}
                activeOpacity={0.8}
              >
                <Text style={styles.consentCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.consentConfirmBtn}
                onPress={confirmStreetCoinConsent}
                activeOpacity={0.8}
              >
                <Text style={styles.consentConfirmText}>Confirm</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
      <Modal visible={showFakeAd} transparent animationType="fade" onRequestClose={() => { }}>
        <View style={styles.adOverlay}>
          <View style={styles.adCard}>
            <Text style={styles.adBadge}>Sponsored</Text>
            <Text style={styles.adTitle}>University of Seville</Text>
            <Text style={styles.adText}>Simulated ad shown to free users before their question is published.</Text>
            <View style={styles.adVisual}>
              <Text style={styles.adVisualTitle}>Study, research, connect</Text>
              <Text style={styles.adVisualText}>Discover degrees, scholarships, campus life, and opportunities at the University of Seville.</Text>
            </View>
            <Text style={styles.adHelperText}>Your question will only be created when this countdown finishes.</Text>
            <Text style={styles.adCountdown}>Your question will be published in {adSecondsLeft}s</Text>
            {canSkipAd && (
              <TouchableOpacity style={styles.adSkipBtn} onPress={skipAd}>
                <Text style={styles.adSkipText}>X</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f3f4f6' },
  consentOverlay: {
    flex: 1,
    backgroundColor: 'rgba(17,24,39,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  consentCard: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#fff',
    borderRadius: 18,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.16,
    shadowRadius: 20,
    elevation: 8,
  },
  consentIconWrap: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#fee2e2',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  consentTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: '#1f2937',
  },
  consentText: {
    marginTop: 10,
    fontSize: 14,
    lineHeight: 20,
    color: '#4b5563',
  },
  consentBalanceText: {
    marginTop: 10,
    fontSize: 13,
    lineHeight: 18,
    color: '#a52019',
    fontWeight: '700',
  },
  consentActions: {
    marginTop: 18,
    flexDirection: 'row',
    gap: 10,
  },
  consentCancelBtn: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 11,
    backgroundColor: '#fff',
  },
  consentCancelText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#374151',
  },
  consentConfirmBtn: {
    flex: 1,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 11,
    backgroundColor: '#a52019',
  },
  consentConfirmText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#fff',
  },
  adOverlay: {
    flex: 1,
    backgroundColor: 'rgba(17,24,39,0.72)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  adCard: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#fff',
    borderRadius: 24,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.2,
    shadowRadius: 24,
    elevation: 8,
  },
  adBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: '#fee2e2',
    color: '#b91c1c',
    fontSize: 11,
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
  adTitle: {
    marginTop: 16,
    fontSize: 24,
    fontWeight: '800',
    color: '#111827',
  },
  adText: {
    marginTop: 8,
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  adVisual: {
    marginTop: 18,
    borderRadius: 18,
    padding: 18,
    backgroundColor: '#1d4ed8',
  },
  adVisualTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#fff',
  },
  adVisualText: {
    marginTop: 8,
    fontSize: 14,
    lineHeight: 20,
    color: '#dbeafe',
  },
  adHelperText: {
    marginTop: 16,
    textAlign: 'center',
    fontSize: 13,
    lineHeight: 18,
    color: '#4b5563',
  },
  adCountdown: {
    marginTop: 18,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '700',
    color: '#92400e',
  },

  /* ── Map backgrounds ── */
  mapFull: { flex: 1, width: '100%', height: '100%' },
  mapBgPreview: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 0 },
  mapOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    padding: 16,
    zIndex: 100,
    justifyContent: 'space-between',
  },
  mapHint: {
    backgroundColor: 'rgba(255,255,255,0.95)',
    padding: 16,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 6,
  },
  mapHintText: { fontWeight: '700', fontSize: 15, color: '#1f2937', textAlign: 'center' },
  mapHintCoords: { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 6 },
  mapRadiusLabel: {
    fontSize: 12,
    color: '#374151',
    fontWeight: '700',
    marginTop: 10,
    marginBottom: 6,
    textAlign: 'center',
  },
  mapRadiusInputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 12,
    height: 42,
  },
  mapZoneText: {
    fontSize: 12,
    color: '#a52019',
    textAlign: 'center',
    marginTop: 8,
    fontWeight: '600',
  },
  lockedBox: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#f3f4f6',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#d1d5db',
    paddingVertical: 10,
    paddingHorizontal: 12,
  },
  lockedText: {
    color: '#4b5563',
    fontSize: 12,
    fontWeight: '600',
  },
  mapBtnRow: { flexDirection: 'row', gap: 12 },
  mapCancelBtn: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  mapOkBtn: {
    flex: 1,
    backgroundColor: '#a52019',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    shadowColor: '#a52019',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 5,
  },
  mapCancelLabel: { fontWeight: '700', fontSize: 15, color: '#1f2937' },
  mapConfirmLabel: { fontWeight: '700', fontSize: 15, color: '#fff' },

  /* ── Form card ── */
  formScroll: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 10 },
  formScrollContent: { padding: 16, paddingTop: 40, paddingBottom: 40 },
  card: {
    backgroundColor: 'rgba(255,255,255,0.92)',
    borderRadius: 20,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 24,
    elevation: 8,
    backdropFilter: 'blur(12px)',
  },
  backRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 16, gap: 4 },
  backText: { fontSize: 14, color: '#6b7280', fontWeight: '500' },
  heading: { fontSize: 24, fontWeight: '800', color: '#1f2937', marginBottom: 20 },
  sectionLabel: {
    fontSize: 13,
    fontWeight: '700',
    color: '#374151',
    marginBottom: 8,
    marginTop: 16,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  locationSectionContainer: {
    marginTop: 10,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingBottom: 12,
    backgroundColor: '#ffffff',
  },
  locationSectionContainerError: {
    borderColor: '#dc2626',
    backgroundColor: '#fff7f7',
  },
  locationRequiredHint: {
    fontSize: 12,
    color: '#6b7280',
    marginTop: -2,
    marginBottom: 8,
  },
  locationInlineError: {
    marginTop: 8,
    fontSize: 12,
    color: '#dc2626',
    fontWeight: '600',
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 14,
    height: 48,
  },
  inputFocused: { borderColor: '#a52019', backgroundColor: '#fff' },
  inputError: { borderColor: '#dc2626' },
  inputMultiline: { height: 'auto', alignItems: 'flex-start', paddingVertical: 12 },
  input: { flex: 1, fontSize: 14, color: '#1f2937', outlineStyle: 'none' },
  radiusErrorText: {
    marginTop: 6,
    fontSize: 12,
    color: '#dc2626',
    fontWeight: '600',
    textAlign: 'center',
  },

  locationBtnRow: { flexDirection: 'row', gap: 10, marginTop: 10 },
  btnOutline: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1.5,
    borderColor: '#a52019',
    borderRadius: 12,
    paddingVertical: 11,
    backgroundColor: '#f0f0ff',
  },
  btnOutlineText: { fontSize: 13, fontWeight: '600', color: '#a52019' },

  lockedLocationCard: {
    backgroundColor: '#eff6ff',
    borderWidth: 1,
    borderColor: '#bfdbfe',
    borderRadius: 12,
    paddingVertical: 12,
    paddingHorizontal: 12,
  },
  lockedLocationTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 6,
  },
  lockedLocationTitle: {
    color: '#1d4ed8',
    fontSize: 12,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  lockedLocationText: {
    color: '#1e293b',
    fontSize: 13,
    fontWeight: '600',
  },

  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#f9fafb',
    borderRadius: 10,
    padding: 12,
    marginTop: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  resultText: { flex: 1, fontSize: 13, color: '#374151' },

  selectedRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 12 },
  selectedText: { fontSize: 12, color: '#6b7280', flex: 1 },
  helperText: { fontSize: 11, color: '#9ca3af', marginTop: 4 },

  timeChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#fef3c7',
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 14,
    marginTop: 20,
  },
  timeChipText: { fontSize: 13, fontWeight: '600', color: '#92400e' },
  timePremiumRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  timeInput: {
    minWidth: 52,
    height: 34,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#f59e0b',
    backgroundColor: '#fff9db',
    textAlign: 'center',
    fontSize: 14,
    color: '#92400e',
    fontWeight: '700',
    paddingHorizontal: 8,
    outlineStyle: 'none',
  },

  actionRow: { flexDirection: 'row', gap: 12, marginTop: 24 },
  cancelBtn: {
    flex: 0.35,
    backgroundColor: '#f3f4f6',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  cancelBtnText: { fontWeight: '600', fontSize: 15, color: '#6b7280' },
  postBtn: {
    flex: 0.65,
    backgroundColor: '#a52019',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    shadowColor: '#667eea',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 5,
  },
  radiusValueText: {
    textAlign: 'center',
    marginTop: 6,
    fontSize: 13,
    fontWeight: '700',
    color: '#a52019',
  },
  sliderBlock: {
    marginTop: 10,
  },

  sliderLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 6,
  },

  sliderMin: {
    fontSize: 11,
    color: '#9ca3af',
  },

  sliderMax: {
    fontSize: 11,
    color: '#9ca3af',
  },
  postBtnText: { fontWeight: '700', fontSize: 15, color: '#fff' },
  dailyLimitWarning: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
    backgroundColor: '#fee2e2',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#fecaca',
    paddingVertical: 12,
    paddingHorizontal: 14,
    marginTop: 16,
  },
  dailyLimitTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#dc2626',
    marginBottom: 2,
  },
  dailyLimitText: {
    fontSize: 12,
    color: '#991b1b',
    lineHeight: 16,
  },
  dailyLimitNoCoinsText: {
    marginTop: 6,
    fontSize: 12,
    color: '#991b1b',
    fontWeight: '700',
  },
  shopBtn: {
    backgroundColor: '#dc2626',
    borderRadius: 8,
    paddingVertical: 8,
    paddingHorizontal: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  shopBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#fff',
  },
  adSkipBtn: {
    position: 'absolute',
    top: 10,
    right: 10,
    alignItems: 'center',
  },

  adSkipText: {
    fontSize: 18,
    fontWeight: '800',
    color: '#111827',
  },

  adSkipLabel: {
    fontSize: 9,
    color: '#6b7280',
  },
});
