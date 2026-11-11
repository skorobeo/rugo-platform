package ru.rugo.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        boolean seedEnabled,
        boolean allowAnonLeads,
        boolean contactSignatureRequired,
        boolean photoUploadsEnabled,
        long photoMaxBytes,
        String analyticsSalt,
        long initDataMaxAgeSeconds,
        long contactSignatureMaxAgeSeconds,
        String webappOrigin,
        List<String> trustedProxies,
        RateLimit rateLimit,
        EventsLimit eventsLimit
) {
    public record RateLimit(
            int leadPerIp,
            int leadIpWindowSeconds,
            int leadPerUser,
            int leadUserWindowSeconds,
            int leadDedupMinutes
    ) {
    }

    public record EventsLimit(
            int maxBatch,
            int maxBodyBytes,
            int maxPropsBytes,
            int ratePerIp,
            int rateWindowSeconds
    ) {
    }
}
