/**
 * Claves de almacenamiento local (localStorage)
 * Centralizadas para mejorar mantenibilidad y seguridad
 */

export const STORAGE_KEYS = {
    // Clave para almacenar datos de checkout pendiente de registro empresarial
    // Se usa durante el flujo de pago de Stripe cuando un empleador se registra
    PENDING_BUSINESS_CHECKOUT: 'streetask.pendingBusinessCheckout',

    // Datos temporales para checkout de compra de StreetCoins
    PENDING_STREETCOINS_CHECKOUT: 'streetask.pendingStreetCoinsCheckout',

    // Aviso efimero para mostrar en Home tras una compra exitosa
    STREETCOINS_SUCCESS_NOTICE: 'streetask.streetCoinsSuccessNotice',

    // Destino post-checkout para casos especiales (ej. abrir Balance tras flujo desde CreateQuestion)
    STREETCOINS_POST_CHECKOUT_TARGET: 'streetask.streetCoinsPostCheckoutTarget',

    // Cache defensiva de datos del mapa para evitar pantalla vacia por fallos transitorios
    HOME_QUESTIONS_CACHE: 'streetask.homeQuestionsCache',
    HOME_EVENTS_CACHE: 'streetask.homeEventsCache',
};
