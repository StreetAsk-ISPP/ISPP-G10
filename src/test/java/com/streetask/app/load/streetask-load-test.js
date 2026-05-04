import { sleep } from 'k6';
import { options } from './scenarios.js';
import { login, validateToken, authHeaders } from './auth.js';
import { listQuestions, getQuestionById, createQuestion, buildQuestionPayload } from './questions.js';
import { listAnswers, createAnswer, buildAnswerPayload } from './answers.js';
import { ENABLE_WRITES } from './config.js';

export { options };

export function setup() {
    const token = login();
    validateToken(token);
    return { token };
}


export default function (data) {
    const headers = authHeaders(data.token);

    // Question operations
    listQuestions(headers);
    getQuestionById(headers);

    // Answer operations
    listAnswers(headers);

    // Occasionally test write operations
    if (ENABLE_WRITES && __ITER % 10 === 0) {
        const questionPayload = buildQuestionPayload();
        createQuestion(headers, questionPayload);

        // Also create an answer every 20 iterations
        if (__ITER % 20 === 0) {
            const answerPayload = buildAnswerPayload();
            createAnswer(headers, answerPayload);
        }
    }

    sleep(1);
}