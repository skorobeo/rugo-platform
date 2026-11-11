package ru.rugo.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import ru.rugo.common.config.AppProperties;

/**
 * Не позволяет приложению стартовать в профиле {@code prod} с пустой солью аналитики
 * (V-031): без соли {@code user_hash} предсказуем. Вне prod — предупреждение.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsSaltValidator implements InitializingBean {

    private final AppProperties properties;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        String salt = properties.analyticsSalt();
        if (salt != null && !salt.isBlank()) {
            return;
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "ANALYTICS_SALT обязательна в профиле prod (иначе user_hash в аналитике предсказуем)");
        }
        log.warn("ANALYTICS_SALT не задана — аналитика использует небезопасный дефолт. "
                + "Допустимо только вне prod.");
    }
}
