import axios from 'axios';
import { APP_CONFIG } from '../../../app/config/config';
import { Platform } from 'react-native';

const TOKEN_STORAGE_KEY = 'auth_token';

// Check if running in web environment
const isWebEnvironment = () => {
  return Platform.OS === 'web' || typeof window !== 'undefined';
};

// Helper to get token from storage (handles both native and web)
const getStoredToken = () => {
  try {
    if (isWebEnvironment()) {
      // Use localStorage on web (synchronous)
      const token = window.localStorage?.getItem(TOKEN_STORAGE_KEY);
      return token;
    }
    // Note: AsyncStorage is async, but we need sync access here
    return null;
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

// Request interceptor - synchronous approach
apiClient.interceptors.request.use((config) => {
  const token = getStoredToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);

export default apiClient;
