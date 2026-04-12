import apiClient from './http/apiClient';

const EVENT_BASE_URL = '/api/v1/events';

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
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}/details`);
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
            const response = await apiClient.get(`${EVENT_BASE_URL}/${eventId}/attendees/count`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching attendee count:`, error);
            throw error;
        }
    },
};

export default EventService;
