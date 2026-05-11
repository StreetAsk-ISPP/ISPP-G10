import { sleep } from 'k6';
import { options } from './scenarios.js';
import { login, validateToken, authHeaders } from './auth.js';
import { listQuestions, getQuestionById } from './questions.js';
import { listAnswers } from './answers.js';
import { listEvents, getEventById } from './events.js';

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

    // Event operations
    listEvents(headers);

    sleep(1);
}