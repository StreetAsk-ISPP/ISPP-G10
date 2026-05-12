/**
 * Questions module for StreetTask load testing
 * Handles all question-related API operations
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, QUESTION_ID, ENDPOINTS, CONTENT_TYPE_JSON } from './config.js';

/**
 * Fetches list of all questions
 * @param {Object} headers - Request headers with authorization
 * @returns {Object} Response object
 */
export function listQuestions(headers) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.QUESTIONS}`, { headers });

    check(response, {
        'list questions returns 200': (r) => r.status === 200,
        'questions response is array': (r) => {
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
 * Fetches a single question by ID
 * @param {Object} headers - Request headers with authorization
 * @param {string} questionId - ID of the question to fetch (defaults to QUESTION_ID)
 * @returns {Object} Response object
 */
export function getQuestionById(headers, questionId = QUESTION_ID) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.QUESTIONS}/${questionId}`, { headers });

    check(response, {
        'get question returns 200': (r) => r.status === 200,
        'question has required fields': (r) => {
            try {
                const data = r.json();
                return data.id && data.title && data.content;
            } catch {
                return false;
            }
        },
    });

    return response;
}

/**
 * Creates a new question
 * @param {Object} headers - Request headers with authorization
 * @param {Object} questionData - Question object with title, content, radiusKm, confirmStreetCoinSpend
 * @returns {Object} Response object
 */
export function createQuestion(headers, questionData) {
    const response = http.post(
        `${BASE_URL}${ENDPOINTS.QUESTIONS}`,
        JSON.stringify(questionData),
        { headers }
    );

    const checkResult = check(response, {
        'create question returns 201': (r) => r.status === 201,
        'created question has id': (r) => {
            try {
                return !!r.json('id');
            } catch {
                return false;
            }
        },
    });

    if (!checkResult) {
        console.log(`[CREATE QUESTION FAILED] Status: ${response.status}, Body: ${response.body}`);
    }

    return response;
}

/**
 * Builds a default question payload for load testing
 * @returns {Object} Question payload with title, content, radiusKm, confirmStreetCoinSpend
 */
export function buildQuestionPayload() {
    const createdAt = new Date().toISOString();
    return {
        title: `Load test question ${__VU}-${__ITER}`,
        content: `Generated during local load testing at ${createdAt}`,
        radiusKm: 0.5,
        confirmStreetCoinSpend: true,
    };
}
