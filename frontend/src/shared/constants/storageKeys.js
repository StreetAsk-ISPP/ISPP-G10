/**
 * Claves de almacenamiento local (localStorage)
 * Centralizadas para mejorar mantenibilidad y seguridad
 */

export const STORAGE_KEYS = {
    // Clave para almacenar datos de checkout pendiente de registro empresarial
    // Se usa durante el flujo de pago de Stripe cuando un empleador se registra
    PENDING_BUSINESS_CHECKOUT: 'streetask.pendingBusinessCheckout',
};
