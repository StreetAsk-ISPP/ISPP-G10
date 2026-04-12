export default function AppNavigator() {
    const { isAuthenticated, isLoadingAuth, user } = useAuth();
    const stripeCallbackHandledRef = useRef(false);
    const [navigationResetVersion, setNavigationResetVersion] = useState(0);

    useEffect(() => {
        if (isLoadingAuth) return;

        if (Platform.OS !== 'web' || typeof window === 'undefined') return;

        const params = new URLSearchParams(window.location.search);
        const paymentState = params.get('payment');
        const sessionId = params.get('session_id');
        const flow = params.get('flow');

        if (!paymentState || stripeCallbackHandledRef.current) return;

        if (flow === 'streetcoins' && !isAuthenticated) return;

        stripeCallbackHandledRef.current = true;

        const cleanUrl = `${window.location.pathname}${window.location.hash || ''}`;

        const clearUrlParams = () => {
            window.history.replaceState({}, document.title, cleanUrl);
        };

        const processStripeCallback = async () => {
            let callbackSucceeded = false;
            let shouldClearParams = true;
            let shouldClearStreetCoinsPending = true;
            let shouldClearBusinessPending = true;

            try {
                // =========================
                // STREETCOINS FLOW
                // =========================
                const rawPendingStreetCoins = window.localStorage.getItem(
                    STORAGE_KEYS.PENDING_STREETCOINS_CHECKOUT
                );

                let pendingStreetCoins = null;
                if (rawPendingStreetCoins) {
                    try {
                        pendingStreetCoins = JSON.parse(rawPendingStreetCoins);
                    } catch {
                        pendingStreetCoins = null;
                    }
                }

                const effectiveSessionId = sessionId || pendingStreetCoins?.sessionId;

                if (paymentState === 'success' && effectiveSessionId) {
                    if (flow === 'streetcoins') {
                        const response = await apiClient.post(
                            '/api/v1/streetcoins/purchase/confirm',
                            { sessionId: effectiveSessionId }
                        );

                        const addedStreetCoins = response?.data?.addedStreetCoins;

                        if (typeof addedStreetCoins === 'number' && addedStreetCoins > 0) {
                            window.localStorage.setItem(
                                STORAGE_KEYS.STREETCOINS_SUCCESS_NOTICE,
                                JSON.stringify({ addedStreetCoins })
                            );

                            window.dispatchEvent(
                                new CustomEvent('streetcoins:purchase-confirmed', {
                                    detail: { addedStreetCoins },
                                })
                            );
                        }

                        if (pendingStreetCoins?.checkoutOrigin === 'create-question-limit') {
                            window.localStorage.setItem(
                                STORAGE_KEYS.STREETCOINS_POST_CHECKOUT_TARGET,
                                'balance'
                            );
                        }

                        callbackSucceeded = true;

                        // reset navegación (feature branch)
                        setNavigationResetVersion((v) => v + 1);

                        // evitar estado roto
                        window.location.replace(cleanUrl);
                        return;
                    }

                    // =========================
                    // BUSINESS FLOW
                    // =========================
                    if (Array.isArray(user?.roles) && user.roles.includes('BUSINESS')) {
                        await apiClient.post(
                            '/api/v1/business-subscriptions/me/stripe/confirm-session',
                            { sessionId: effectiveSessionId }
                        );
                        callbackSucceeded = true;
                    } else {
                        // =========================
                        // PREMIUM USER (TRUNK)
                        // =========================
                        const pendingRegularPremiumCheckout =
                            window.localStorage.getItem(
                                STORAGE_KEYS.PENDING_REGULAR_PREMIUM_CHECKOUT
                            );

                        if (pendingRegularPremiumCheckout && isAuthenticated) {
                            await apiClient.post(
                                '/api/v1/users/me/premium/stripe/confirm-session',
                                { sessionId: effectiveSessionId }
                            );
                            callbackSucceeded = true;
                        } else {
                            const rawPendingData = window.localStorage.getItem(
                                STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT
                            );

                            if (rawPendingData) {
                                const pendingData = JSON.parse(rawPendingData);

                                if (pendingData?.email && pendingData?.taxId) {
                                    await apiClient.post(
                                        '/api/v1/business-subscriptions/stripe/confirm-session',
                                        {
                                            email: pendingData.email,
                                            taxId: pendingData.taxId,
                                            sessionId: effectiveSessionId,
                                        }
                                    );
                                    callbackSucceeded = true;
                                }
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Stripe callback processing failed:', error);

                stripeCallbackHandledRef.current = false;
                shouldClearParams = false;

                if (flow === 'streetcoins') {
                    shouldClearStreetCoinsPending = false;
                } else {
                    shouldClearBusinessPending = false;
                }
            } finally {
                if (shouldClearBusinessPending) {
                    window.localStorage.removeItem(STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT);
                    window.localStorage.removeItem(
                        STORAGE_KEYS.PENDING_REGULAR_PREMIUM_CHECKOUT
                    );
                }

                if (shouldClearStreetCoinsPending) {
                    window.localStorage.removeItem(
                        STORAGE_KEYS.PENDING_STREETCOINS_CHECKOUT
                    );
                }

                if (shouldClearParams) {
                    clearUrlParams();
                }

                // comportamiento de trunk (refrescar UI)
                if (callbackSucceeded && flow !== 'streetcoins') {
                    window.location.reload();
                }
            }
        };

        processStripeCallback();
    }, [isAuthenticated, isLoadingAuth, user?.roles]);