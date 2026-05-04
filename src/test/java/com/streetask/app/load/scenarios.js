/**
 * Scenarios module for StreetTask load testing
 * Defines test configuration, executor settings, and thresholds
 */

/**
 * Local ramping VU scenario configuration
 * Gradually increases from 0 to 10 VUs over 1 minute,
 * holds at 25 VUs for 3 minutes,
 * then ramps down to 0 over 1 minute
 * Total duration: ~5 minutes
 */
export const scenarios = {
    local_ramp: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '30s', target: 10 },
            { duration: '1m', target: 25 },
            { duration: '30s', target: 0 },
        ],
        gracefulRampDown: '10s',
    },
};

/**
 * Performance thresholds
 * - Failed requests: less than 1%
 * - 95th percentile response time: under 800ms
 * - 99th percentile response time: under 1500ms
 */
export const thresholds = {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
};

/**
 * Complete test options configuration
 * Exported for direct use in main test file
 */
export const options = {
    scenarios,
    thresholds,
};
