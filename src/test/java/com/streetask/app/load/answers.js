/**
 * Answers module for StreetTask load testing
 * Handles all answer-related API operations
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, QUESTION_ID, ENDPOINTS } from './config.js';

/**
 * Fetches answers for a specific question
 * @param {Object} headers - Request headers with authorization
 * @param {string} questionId - ID of the question to fetch answers for (defaults to QUESTION_ID)
 * @param {Object} options - Additional query parameters (sort, limit, etc.)
 * @returns {Object} Response object
 */
export function listAnswers(headers, questionId = QUESTION_ID, options = {}) {
    let queryParams = [
        `questionId=${questionId}`,
        `sort=${options.sort || 'date'}`
    ];

    if (options.limit) {
        queryParams.push(`limit=${options.limit}`);
    }
    if (options.offset) {
        queryParams.push(`offset=${options.offset}`);
    }

    const response = http.get(
        `${BASE_URL}${ENDPOINTS.ANSWERS}?${queryParams.join('&')}`,
        { headers }
    );

    check(response, {
        'list answers returns 200': (r) => r.status === 200,
        'answers response is array': (r) => {
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
 * Fetches a single answer by ID
 * @param {Object} headers - Request headers with authorization
 * @param {string} answerId - ID of the answer to fetch
 * @returns {Object} Response object
 */
export function getAnswerById(headers, answerId) {
    const response = http.get(`${BASE_URL}${ENDPOINTS.ANSWERS}/${answerId}`, { headers });

    check(response, {
        'get answer returns 200': (r) => r.status === 200,
        'answer has required fields': (r) => {
            try {
                const data = r.json();
                return data.id && data.content;
            } catch {
                return false;
            }
        },
    });

    return response;
}

/**
 * Creates a new answer for a question
 * @param {Object} headers - Request headers with authorization
 * @param {Object} answerData - Answer object with content, questionId, etc.
 * @returns {Object} Response object
 */
export function createAnswer(headers, answerData) {
    const response = http.post(
        `${BASE_URL}${ENDPOINTS.ANSWERS}`,
        JSON.stringify(answerData),
        { headers }
    );

    return response;
}

/**
 * Builds a default answer payload for load testing
 * @param {string} questionId - Question ID to answer
 * @returns {Object} Answer payload with content and question object
 */
export function buildAnswerPayload(questionId = QUESTION_ID) {
    const createdAt = new Date().toISOString();
    return {
        content: `Load test answer from VU ${__VU} at ${createdAt}`,
        question: {
            id: questionId
        },
    };
}
