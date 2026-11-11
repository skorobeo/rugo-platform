package ru.rugo.admin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.UpdateType;
import ru.rugo.common.max.webhook.DeliveryMode;
import ru.rugo.common.max.webhook.MaxDeliveryState;
import ru.rugo.common.max.webhook.MaxSubscriptionManager;

import java.util.List;

/**
 * Два клиента MAX Bot API:
 * <ul>
 *   <li>{@code adminBotApi} — админ-бот (апдейты через webhook/long polling, карточки модерации);</li>
 *   <li>{@code mainBotApi} — основной бот, для сообщений партнёру после одобрения.</li>
 * </ul>
 */
@Configuration
public class AdminBotConfig {

    @Bean
    public MaxBotAPI adminBotApi(@Value("${max.admin.bot.token:}") String token) {
        return MaxBotAPI.create(token);
    }

    @Bean
    public MaxBotAPI mainBotApi(@Value("${max.bot.token:}") String token) {
        return MaxBotAPI.create(token);
    }

    @Bean
    public MaxSubscriptionManager adminSubscriptionManager(
            @Qualifier("adminBotApi") MaxBotAPI adminBotApi,
            MaxDeliveryState deliveryState,
            @Value("${max.admin.bot.delivery-mode:longpoll}") String deliveryMode,
            @Value("${max.admin.bot.webhook.url:}") String webhookUrl,
            @Value("${max.admin.bot.webhook.secret:}") String webhookSecret,
            @Value("${admin.public-base-url:}") String publicBaseUrl) {
        String url = webhookUrl == null || webhookUrl.isBlank()
                ? trimTrailingSlash(publicBaseUrl) + "/admin/max/webhook"
                : webhookUrl.trim();
        return new MaxSubscriptionManager(
                adminBotApi,
                DeliveryMode.parse(deliveryMode),
                url,
                webhookSecret,
                List.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK),
                deliveryState,
                3);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
