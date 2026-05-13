/**
 * Claves de almacenamiento local (localStorage)
 * Centralizadas para mejorar mantenibilidad y seguridad
 */
export const STORAGE_KEYS = {
    // Borrador temporal del checkout de registro empresarial (sessionStorage)
    PENDING_BUSINESS_CHECKOUT: 'streetask.pendingBusinessCheckout',

    // Checkout pendiente para suscripción de negocio (usuario BUSINESS autenticado)
    PENDING_BUSINESS_SUBSCRIPTION_CHECKOUT: 'streetask.pendingBusinessSubscriptionCheckout',

    // =========================
    // STREETCOINS
    // =========================

    // Datos temporales para checkout de compra de StreetCoins
    PENDING_STREETCOINS_CHECKOUT: 'streetask.pendingStreetCoinsCheckout',

    // Aviso efímero tras compra exitosa
    STREETCOINS_SUCCESS_NOTICE: 'streetask.streetCoinsSuccessNotice',

    // Redirección especial post-checkout
    STREETCOINS_POST_CHECKOUT_TARGET: 'streetask.streetCoinsPostCheckoutTarget',

    // =========================
    // PREMIUM (TRUNK)
    // =========================

    // Marca de checkout pendiente para upgrade premium
    PENDING_REGULAR_PREMIUM_CHECKOUT: 'streetask.pendingRegularPremiumCheckout',

    // Aviso efímero para mostrar resultado de callback de checkout
    CHECKOUT_CALLBACK_NOTICE: 'streetask.checkoutCallbackNotice',

    // =========================
    // CACHE HOME (FEATURE)
    // =========================

    // Cache defensiva para evitar pantallas vacías
    HOME_QUESTIONS_CACHE: 'streetask.homeQuestionsCache',
    HOME_EVENTS_CACHE: 'streetask.homeEventsCache',
};
