import { Platform } from 'react-native';

const CHECKOUT_RETURN_HEADER = 'X-Checkout-Return-Url';

export const getCheckoutReturnUrl = () => {
    if (Platform.OS !== 'web' || typeof window === 'undefined') {
        return null;
    }

    const origin = typeof window.location?.origin === 'string' ? window.location.origin.trim() : '';
    if (!origin) {
        return null;
    }

    return origin;
};

export const getCurrentWebPathWithSearchAndHash = () => {
    if (Platform.OS !== 'web' || typeof window === 'undefined') {
        return null;
    }

    const pathname = typeof window.location?.pathname === 'string'
        ? window.location.pathname
        : '/';
    const search = typeof window.location?.search === 'string'
        ? window.location.search
        : '';
    const hash = typeof window.location?.hash === 'string'
        ? window.location.hash
        : '';

    return `${pathname}${search}${hash}`;
};

export const withCheckoutReturnHeader = (requestConfig = {}) => {
    const checkoutReturnUrl = getCheckoutReturnUrl();
    if (!checkoutReturnUrl) {
        return requestConfig;
    }

    return {
        ...requestConfig,
        headers: {
            ...(requestConfig.headers || {}),
            [CHECKOUT_RETURN_HEADER]: checkoutReturnUrl,
        },
    };
};
