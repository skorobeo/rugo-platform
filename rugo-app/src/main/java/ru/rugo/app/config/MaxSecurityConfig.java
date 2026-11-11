package ru.rugo.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.security.RequestContactValidator;

@Configuration
public class MaxSecurityConfig {

    @Bean
    public MaxInitDataValidator maxInitDataValidator(
            @Value("${max.bot.token:}") String botToken,
            AppProperties properties) {
        return new MaxInitDataValidator(botToken, properties.initDataMaxAgeSeconds());
    }

    @Bean
    public RequestContactValidator requestContactValidator(@Value("${max.bot.token:}") String botToken) {
        return new RequestContactValidator(botToken);
    }
}
