/**
 * Authentication module for StreetTask load testing
 * Handles login and token management
 */

import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL, ADMIN_EMAIL, ADMIN_PASSWORD, ENDPOINTS, CONTENT_TYPE_JSON } from './config.js';

/**
 * Creates authorization headers with Bearer token
 * @param {string} token - JWT token
 * @returns {Object} Headers object with Authorization and Content-Type
 */
export function authHeaders(token) {
    return {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
    };
}

/**
 * Authenticates user and returns JWT token
 * @returns {string} JWT token
 */
export function login() {
    const response = http.post(
        `${BASE_URL}${ENDPOINTS.SIGNIN}`,
        JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
        { headers: CONTENT_TYPE_JSON }
    );

    check(response, {
        'signin returns 200': (r) => r.status === 200,
        'signin returns token': (r) => !!r.json('token'),
    }) || fail(`Signin failed with status ${response.status}: ${response.body}`);

    return response.json('token');
}

/**
 * Validates a JWT token
 * @param {string} token - JWT token to validate
 * @returns {boolean} Whether token is valid
 */
export function validateToken(token) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.VALIDATE}?token=${token}`);

    check(response, {
        'token validation returns true': (r) => r.status === 200 && r.body === 'true',
    }) || fail(`Token validation failed with status ${response.status}: ${response.body}`);

    return response.status === 200;
}
