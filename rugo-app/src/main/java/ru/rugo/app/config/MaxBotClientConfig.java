package ru.rugo.app.config;

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
 * Клиент MAX Bot API основного бота. Используется для:
 * <ul>
 *   <li>доставки апдейтов — приветствие при «Начать»/{@code /start}
 *       (long polling или webhook, см. {@code ru.rugo.app.bot});</li>
 *   <li>проверки подписи initData мини-приложения (по токену бота).</li>
 * </ul>
 * Диалогов и меню у основного бота нет — только приветствие.
 */
@Configuration
public class MaxBotClientConfig {

    @Bean
    public MaxBotAPI maxBotAPI(@Value("${max.bot.token:}") String token) {
        return MaxBotAPI.create(token);
    }

    @Bean
    public MaxSubscriptionManager maxSubscriptionManager(
            MaxBotAPI maxBotAPI,
            MaxDeliveryState deliveryState,
            @Value("${max.bot.delivery-mode:longpoll}") String deliveryMode,
            @Value("${max.bot.webhook.url:}") String webhookUrl,
            @Value("${max.bot.webhook.secret:}") String webhookSecret,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        String url = webhookUrl == null || webhookUrl.isBlank()
                ? trimTrailingSlash(publicBaseUrl) + "/api/max/webhook"
                : webhookUrl.trim();
        return new MaxSubscriptionManager(
                maxBotAPI,
                DeliveryMode.parse(deliveryMode),
                url,
                webhookSecret,
                List.of(UpdateType.BOT_STARTED, UpdateType.MESSAGE_CREATED),
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
