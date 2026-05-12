/**
 * Endpoints module for StreetTask load testing
 * Contains all API endpoint calls and checks
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, QUESTION_ID, ENDPOINTS, CONTENT_TYPE_JSON } from './config.js';

/**
 * Fetches list of questions
 * @param {Object} headers - Request headers with authorization
 * @returns {Object} Response object
 */
export function getQuestions(headers) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.QUESTIONS}`, { headers });

    check(response, {
        'questions list returns 200': (r) => r.status === 200,
    });

    return response;
}

/**
 * Fetches a single question by ID
 * @param {Object} headers - Request headers with authorization
 * @param {string} questionId - ID of the question to fetch (defaults to QUESTION_ID)
 * @returns {Object} Response object
 */
export function getQuestion(headers, questionId = QUESTION_ID) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.QUESTIONS}/${questionId}`, { headers });

    check(response, {
        'question detail returns 200': (r) => r.status === 200,
    });

    return response;
}

/**
 * Fetches answers for a question
 * @param {Object} headers - Request headers with authorization
 * @param {string} questionId - ID of the question to fetch answers for (defaults to QUESTION_ID)
 * @returns {Object} Response object
 */
export function getAnswers(headers, questionId = QUESTION_ID) {
    const response = http.get(
        `${BASE_URL}${ENDPOINTS.ANSWERS}?questionId=${questionId}&sort=date`,
        { headers }
    );

    check(response, {
        'answers list returns 200': (r) => r.status === 200,
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

    check(response, {
        'question create returns 201': (r) => r.status === 201,
    });

    return response;
}

/**
 * Creates a default question payload for load testing
 * @returns {Object} Question payload
 */
export function buildQuestionPayload() {
    const createdAt = new Date().toISOString();
    return {
        title: `Load test question ${__VU}-${__ITER}`,
        content: `Generated during local load testing at ${createdAt}`,
        radiusKm: 0.5,
        confirmStreetCoinSpend: false,
    };
}
