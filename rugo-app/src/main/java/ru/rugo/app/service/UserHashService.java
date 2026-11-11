package ru.rugo.app.service;

import org.springframework.stereotype.Component;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.util.AnalyticsHash;

/**
 * HMAC-хеш идентификатора для аналитики. В events никогда не попадает сырой
 * max_user_id — только необратимый хеш с солью из окружения ({@code ANALYTICS_SALT}).
 * Алгоритм вынесен в {@link AnalyticsHash}, чтобы им же пользовалась фильтрация статистики.
 */
@Component
public class UserHashService {

    private final String salt;

    public UserHashService(AppProperties properties) {
        this.salt = properties.analyticsSalt() == null ? "" : properties.analyticsSalt();
    }

    public String hash(Long maxUserId, String sessionId) {
        if (maxUserId != null) {
            return AnalyticsHash.userHash(salt, maxUserId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return AnalyticsHash.sessionHash(salt, sessionId);
        }
        return AnalyticsHash.anonHash(salt);
    }
}
