import axios from 'axios';
import { APP_CONFIG } from '../../../app/config/config';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { emitAuthSessionInvalidated } from './authSessionEvents';

const TOKEN_STORAGE_KEY = 'auth_token';

// Check if running in web environment
const isWebEnvironment = () => {
  return Platform.OS === 'web' || typeof window !== 'undefined';
};

// Helper to get token from storage (handles both native and web)
const getStoredToken = async () => {
  try {
    if (isWebEnvironment()) {
      // Use localStorage on web (synchronous)
      const token = window.localStorage?.getItem(TOKEN_STORAGE_KEY);
      return token;
    }

    // Use AsyncStorage on native platforms
    const token = await AsyncStorage.getItem(TOKEN_STORAGE_KEY);
    return token;
  } catch (error) {
    return null;
  }
};

const apiClient = axios.create({
  baseURL: APP_CONFIG.apiBaseUrl,
  timeout: APP_CONFIG.requestTimeoutMs,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(async (config) => {
  const token = await getStoredToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
      emitAuthSessionInvalidated();
    }

    return Promise.reject(error);
  }
);

export default apiClient;
