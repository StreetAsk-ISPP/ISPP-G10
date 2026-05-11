/**
 * Configuration module for StreetTask load testing
 * Manages environment variables and constants
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const USER_EMAIL = __ENV.USER_EMAIL || 'user1@streetask.com';
export const USER_PASSWORD = __ENV.USER_PASSWORD || '4dm1n';
export const QUESTION_ID = __ENV.QUESTION_ID || 'dd000000-0000-0000-0000-000000000001';
export const ENABLE_WRITES = (__ENV.ENABLE_WRITES || 'true').toLowerCase() === 'true';

// API endpoints
export const ENDPOINTS = {
    SIGNIN: '/api/v1/auth/signin',
    VALIDATE: '/api/v1/auth/validate',
    QUESTIONS: '/api/v1/questions',
    ANSWERS: '/api/v1/answers',
    EVENTS: '/api/v1/events',
};

// Request headers
export const CONTENT_TYPE_JSON = { 'Content-Type': 'application/json' };
