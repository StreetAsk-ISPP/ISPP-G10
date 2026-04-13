import apiClient from './http/apiClient';

const EVENT_BASE_URL = '/api/v1/events';

const toQuestionList = (payload) => {
    if (!payload) return [];
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.questions)) return payload.questions;
    if (Array.isArray(payload?.data)) return payload.data;
    if (payload?.id && (payload?.title || payload?.content)) return [payload];
    return [];
};

const parsePayloadIfNeeded = (payload) => {
    if (typeof payload !== 'string') return payload;

    try {
        return JSON.parse(payload);
    } catch (_) {
        return payload;
    }
};

const normalizeQuestionList = (payload) => {
    const parsed = parsePayloadIfNeeded(payload);
    const rawList = toQuestionList(parsed);

    // Keep only the fields the modal needs to avoid huge nested payload issues.
    return rawList
        .filter((question) => question && question.id)
        .map((question) => ({
            id: question.id,
            title: question.title || '',
            content: question.content || '',
            createdAt: question.createdAt || null,
            eventId: question?.event?.id || question?.eventId || null,
        }));
};

const sortQuestionsChronologically = (questions) => {
    if (!Array.isArray(questions)) return [];

    return [...questions].sort((a, b) => {
        const aTime = new Date(a?.createdAt || 0).getTime();
        const bTime = new Date(b?.createdAt || 0).getTime();

        const safeATime = Number.isFinite(aTime) ? aTime : 0;
        const safeBTime = Number.isFinite(bTime) ? bTime : 0;
        return safeATime - safeBTime;
    });
};

const EventService = {
    getAllEvents: async (filters = {}) => {
        try {
            const params = new URLSearchParams();
            if (filters.active !== undefined) params.append('active', filters.active);
            if (filters.category) params.append('category', filters.category);
            const queryString = params.toString();
            const url = queryString ? `${EVENT_BASE_URL}?${queryString}` : EVENT_BASE_URL;
            const response = await apiClient.get(url);
            return response.data;
        } catch (error) {
            console.error('Error fetching events:', error);
            throw error;
        }
    },

    getEventDetails: async (eventId) => {
        try {
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching event details:`, error);
            throw error;
        }
    },

    getEventSummary: async (eventId) => {
        try {
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching event summary:`, error);
            throw error;
        }
    },

    getEventAttendees: async (eventId) => {
        try {
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}/attendees`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching attendees:`, error);
            throw error;
        }
    },

    getAttendeeCount: async (eventId) => {
        try {
            const attendees = await EventService.getEventAttendees(eventId);
            return Array.isArray(attendees) ? attendees.length : 0;
        } catch (error) {
            console.error(`Error fetching attendee count:`, error);
            throw error;
        }
    },

    getEventQuestions: async (eventId) => {
        try {
            console.log(`[EventService] Fetching event questions for eventId=${eventId}`);
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}/questions`);
            console.log('[EventService] Raw event questions response:', response?.data);
            console.log('[EventService] Raw event questions response type:', typeof response?.data, Array.isArray(response?.data));
            const normalized = sortQuestionsChronologically(normalizeQuestionList(response.data));
            console.log('[EventService] Normalized event questions:', normalized);
            return normalized;
        } catch (error) {
            console.error(`Error fetching event questions:`, error);
            throw error;
        }
    },
};

export default EventService;
