package com.example.androidfeasibility;

import java.net.URL;

public final class EndpointValidator {
    private EndpointValidator() { }

    public static URL validate(URL endpoint) {
        if (endpoint == null) throw new IllegalArgumentException("endpoint is null");
        String protocol = endpoint.getProtocol();
        if ("https".equalsIgnoreCase(protocol)) {
            rejectUrlDecorators(endpoint);
            return endpoint;
        }
        if (!"http".equalsIgnoreCase(protocol)) {
            throw new IllegalArgumentException("endpoint must use HTTPS or local HTTP");
        }
        String host = endpoint.getHost();
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "10.0.2.2".equals(host);
        if (!local) throw new IllegalArgumentException("cleartext endpoint must be local");
        rejectUrlDecorators(endpoint);
        return endpoint;
    }

    public static URL validateRedirect(URL origin, URL redirect) {
        validate(origin);
        validate(redirect);
        if ("https".equalsIgnoreCase(origin.getProtocol())
                && !"https".equalsIgnoreCase(redirect.getProtocol())) {
            throw new IllegalArgumentException("redirect may not downgrade HTTPS");
        }
        if (!origin.getHost().equalsIgnoreCase(redirect.getHost())) {
            throw new IllegalArgumentException("redirect host is not allowed");
        }
        return redirect;
    }

    private static void rejectUrlDecorators(URL endpoint) {
        if (endpoint.getUserInfo() != null || endpoint.getRef() != null) {
            throw new IllegalArgumentException("endpoint must not contain user info or fragment");
        }
    }
}
