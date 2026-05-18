const listeners = new Set();

export const onAuthSessionInvalidated = (listener) => {
    listeners.add(listener);

    return () => {
        listeners.delete(listener);
    };
};

export const emitAuthSessionInvalidated = () => {
    listeners.forEach((listener) => {
        try {
            listener();
        } catch (error) {
            console.warn('Auth session invalidation listener failed.', error);
        }
    });
};