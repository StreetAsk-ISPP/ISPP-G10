package com.streetask.app.payments;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

@Component
public class StripeRedirectUrlResolver {

    private static final String DEFAULT_LOCAL_RETURN_URL = "http://localhost:8081";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    private final List<String> allowedOriginPatterns;

    public StripeRedirectUrlResolver(
            @Value("${streetask.stripe.allowed-return-origin-patterns:http://localhost:*,http://127.0.0.1:*,https://localhost:*,https://127.0.0.1:*}")
            String[] allowedOriginPatterns) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns == null ? new String[0] : allowedOriginPatterns)
                .map(pattern -> pattern == null ? "" : pattern.trim())
                .filter(StringUtils::hasText)
                .toList();
    }

    public String resolveCheckoutBaseUrl(String requestedReturnUrl, String configuredFallbackUrl) {
        String normalizedFallback = normalizeBaseUrl(configuredFallbackUrl, DEFAULT_LOCAL_RETURN_URL);
        String normalizedRequested = normalizeBaseUrl(requestedReturnUrl, null);

        if (StringUtils.hasText(normalizedRequested) && isAllowed(normalizedRequested, normalizedFallback)) {
            return normalizedRequested;
        }

        return normalizedFallback;
    }

    private boolean isAllowed(String requestedBaseUrl, String fallbackBaseUrl) {
        String requestedOrigin = originOf(requestedBaseUrl);
        String fallbackOrigin = originOf(fallbackBaseUrl);

        if (!StringUtils.hasText(requestedOrigin)) {
            return false;
        }

        if (requestedOrigin.equalsIgnoreCase(fallbackOrigin)) {
            return true;
        }

        if (allowedOriginPatterns.isEmpty()) {
            return false;
        }

        for (String pattern : allowedOriginPatterns) {
            if (PatternMatchUtils.simpleMatch(pattern, requestedOrigin)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeBaseUrl(String rawUrl, String defaultValue) {
        if (!StringUtils.hasText(rawUrl)) {
            return defaultValue;
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if ((!HTTP_SCHEME.equals(scheme) && !HTTPS_SCHEME.equals(scheme)) || !StringUtils.hasText(host)) {
            return defaultValue;
        }

        if (StringUtils.hasText(uri.getRawUserInfo())) {
            return defaultValue;
        }

        String portPart = uri.getPort() > -1 ? ":" + uri.getPort() : "";
        String path = uri.getPath() == null ? "" : uri.getPath().trim();
        if ("/".equals(path)) {
            path = "";
        } else if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        return scheme + "://" + host + portPart + path;
    }

    private String originOf(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }

        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
                return "";
            }
            String portPart = uri.getPort() > -1 ? ":" + uri.getPort() : "";
            return scheme + "://" + host + portPart;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
