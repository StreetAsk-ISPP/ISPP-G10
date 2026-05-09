/**
 * Events module for StreetTask load testing
 * Handles all event-related API operations
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, ENDPOINTS } from './config.js';

/**
 * Fetches list of all events
 * @param {Object} headers - Request headers with authorization
 * @returns {Object} Response object
 */
export function listEvents(headers) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.EVENTS}`, { headers });

    check(response, {
        'list events returns 200': (r) => r.status === 200,
        'events response is array': (r) => {
            try {
                const data = r.json();
                return Array.isArray(data);
            } catch {
                return false;
            }
        },
    });

    return response;
}

/**
 * Fetches a single event by ID
 * @param {Object} headers - Request headers with authorization
 * @param {string} eventId - ID of the event to fetch
 * @returns {Object} Response object
 */
export function getEventById(headers, eventId) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.EVENTS}/${eventId}`, { headers });

    check(response, {
        'get event returns 200': (r) => r.status === 200,
        'event has required fields': (r) => {
            try {
                const data = r.json();
                return data.id && data.title;
            } catch {
                return false;
            }
        },
    });

    return response;
}
