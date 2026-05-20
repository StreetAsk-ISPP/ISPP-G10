const rawApiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL?.trim() || 'http://localhost:8080';
const apiBaseUrl = rawApiBaseUrl.replace(/\/+$/, '');
const configuredTimeoutMs = Number(process.env.EXPO_PUBLIC_API_TIMEOUT_MS);

const WS_ENDPOINT_PATH = '/ws';
const DEFAULT_WS_RECONNECT_DELAY_MS = 5000;
const DEFAULT_ZONE_CELL_SIZE_DEGREES = 0.02;
const USER_NOTIFICATIONS_TOPIC = '/user/queue/notifications';
const ZONE_TOPIC_PREFIX = '/topic/zones';
const ZONE_TOPIC_SUFFIX = '/notifications';
const wsUrl = `${apiBaseUrl}${WS_ENDPOINT_PATH}`.replace(/\/+$/, '');

export const APP_CONFIG = {
  appName: 'ISPP Frontend',
  apiBaseUrl,
  requestTimeoutMs: Number.isFinite(configuredTimeoutMs) && configuredTimeoutMs > 0 ? configuredTimeoutMs : 60000,
  websocket: {
    url: wsUrl,
    reconnectDelayMs: DEFAULT_WS_RECONNECT_DELAY_MS,
    zoneCellSizeDegrees: DEFAULT_ZONE_CELL_SIZE_DEGREES,
    topics: {
      userNotifications: USER_NOTIFICATIONS_TOPIC,
      zonePrefix: ZONE_TOPIC_PREFIX,
      zoneSuffix: ZONE_TOPIC_SUFFIX,
    },
  },
};

// Exportar también de forma individual para compatibilidad
export const API_BASE_URL = apiBaseUrl;
export const API_TIMEOUT_MS = APP_CONFIG.requestTimeoutMs;
export const WS_URL = APP_CONFIG.websocket.url;
export const WS_RECONNECT_DELAY_MS = APP_CONFIG.websocket.reconnectDelayMs;
