package ru.rugo.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rugo.core.security.MaxInitDataValidator;

/**
 * Валидаторы initData для админки. Доступ разрешён, если подпись сходится
 * токеном админ-бота ({@code max.admin.bot.token}) — мини-приложение привязано к нему —
 * либо, как запас, токеном основного бота ({@code max.bot.token}).
 */
@Configuration
public class AdminSecurityConfig {

    @Bean
    public MaxInitDataValidator adminInitDataValidator(
            @Value("${max.admin.bot.token:}") String adminBotToken,
            @Value("${admin.init-data-max-age-seconds:3600}") long maxAgeSeconds) {
        return new MaxInitDataValidator(adminBotToken, maxAgeSeconds);
    }

    @Bean
    public MaxInitDataValidator mainInitDataValidator(
            @Value("${max.bot.token:}") String mainBotToken,
            @Value("${admin.init-data-max-age-seconds:3600}") long maxAgeSeconds) {
        return new MaxInitDataValidator(mainBotToken, maxAgeSeconds);
    }
}
