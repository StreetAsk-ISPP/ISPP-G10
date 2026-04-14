package com.streetask.app.payments;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CheckoutReturnUrlRequestResolver {

    private static final String HEADER_CHECKOUT_RETURN_URL = "X-Checkout-Return-Url";
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_REFERER = "Referer";

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String explicitReturnUrl = normalize(request.getHeader(HEADER_CHECKOUT_RETURN_URL));
        if (StringUtils.hasText(explicitReturnUrl)) {
            return explicitReturnUrl;
        }

        String origin = normalize(request.getHeader(HEADER_ORIGIN));
        if (StringUtils.hasText(origin)) {
            return origin;
        }

        return normalize(request.getHeader(HEADER_REFERER));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
