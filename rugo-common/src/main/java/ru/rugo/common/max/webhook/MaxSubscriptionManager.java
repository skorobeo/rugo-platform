package ru.rugo.common.max.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UpdateType;

import java.util.List;

/**
 * Управляет подпиской MAX на webhook и переключением режимов доставки.
 *
 * <p>В режиме webhook на старте удаляет чужие подписки и регистрирует свою.
 * Если регистрация не удалась — возвращается к long polling, чтобы бот не
 * остался молчащим. В режиме long polling удаляет webhook-подписки: при
 * активной подписке MAX не отдаёт апдейты через {@code GET /updates}.</p>
 *
 * <p>Периодическая проверка нужна, потому что MAX сам отписывает бота после
 * 8 часов неуспешных доставок, а также чтобы подхватить изменение конфигурации
 * без рестарта.</p>
 */
@Slf4j
public class MaxSubscriptionManager {

    private final MaxBotAPI api;
    private final DeliveryMode mode;
    private final String url;
    private final String secret;
    private final List<UpdateType> updateTypes;
    private final MaxDeliveryState state;
    private final int maxConsecutiveFailures;

    private int consecutiveFailures;
    private boolean longPollingLogged;

    public MaxSubscriptionManager(MaxBotAPI api,
                                  DeliveryMode mode,
                                  String url,
                                  String secret,
                                  List<UpdateType> updateTypes,
                                  MaxDeliveryState state,
                                  int maxConsecutiveFailures) {
        this.api = api;
        this.mode = mode;
        this.url = url == null ? "" : url.trim();
        this.secret = secret == null ? "" : secret.trim();
        this.updateTypes = List.copyOf(updateTypes);
        this.state = state;
        this.maxConsecutiveFailures = Math.max(1, maxConsecutiveFailures);
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void onApplicationReady() {
        if (mode == DeliveryMode.WEBHOOK) {
            startWebhook();
        } else {
            ensureLongPolling();
        }
    }

    @Scheduled(fixedDelayString = "${max.webhook.check-delay-ms:300000}",
            initialDelayString = "${max.webhook.check-delay-ms:300000}")
    public synchronized void periodicCheck() {
        if (mode == DeliveryMode.WEBHOOK) {
            if (!state.webhookEnabled()) {
                return;
            }
            if (ensureSubscription()) {
                consecutiveFailures = 0;
                return;
            }
            consecutiveFailures++;
            log.warn("MAX webhook: проверка подписки не удалась ({}/{})",
                    consecutiveFailures, maxConsecutiveFailures);
            if (consecutiveFailures >= maxConsecutiveFailures) {
                log.error("MAX webhook: подряд {} ошибок, возвращаемся к long polling",
                        consecutiveFailures);
                ensureLongPolling();
            }
        } else {
            ensureLongPolling();
        }
    }

    private void startWebhook() {
        String invalidReason = validateWebhookConfig();
        if (invalidReason != null) {
            log.error("MAX webhook: {}, возвращаемся к long polling", invalidReason);
            ensureLongPolling();
            return;
        }
        state.disableAll();
        if (ensureSubscription()) {
            consecutiveFailures = 0;
            state.enableWebhook();
            log.info("MAX webhook включён: {}", url);
        } else {
            ensureLongPolling();
        }
    }

    private String validateWebhookConfig() {
        if (url.isBlank()) {
            return "не задан URL webhook";
        }
        if (!url.startsWith("https://")) {
            return "URL webhook должен начинаться с https://";
        }
        if (secret.length() < 5 || secret.length() > 256) {
            return "секрет webhook должен быть длиной 5..256 символов";
        }
        return null;
    }

    private void ensureLongPolling() {
        state.disableAll();
        deleteAllSubscriptions();
        state.enableLongPolling();
        if (!longPollingLogged) {
            log.info("MAX long polling включён (webhook-подписки удалены)");
            longPollingLogged = true;
        }
    }

    private boolean ensureSubscription() {
        try {
            List<Subscription> subscriptions = api.getSubscriptions().execute().subscriptions();
            boolean oursPresent = false;
            for (Subscription subscription : subscriptions) {
                if (url.equals(subscription.url())) {
                    oursPresent = true;
                } else {
                    api.unsubscribe(subscription.url()).execute();
                    log.info("MAX webhook: удалена чужая подписка {}", subscription.url());
                }
            }
            if (oursPresent) {
                return true;
            }
            SimpleQueryResult result = api.subscribe(
                    new SubscriptionRequestBody(url, updateTypes, secret)).execute();
            if (result == null || !result.success()) {
                log.error("MAX webhook: MAX отклонил подписку: {}",
                        result == null ? "пустой ответ" : result.message());
                return false;
            }
            log.info("MAX webhook: подписка зарегистрирована: {}", url);
            return true;
        } catch (Exception e) {
            log.error("MAX webhook: ошибка регистрации подписки", e);
            return false;
        }
    }

    private void deleteAllSubscriptions() {
        try {
            for (Subscription subscription : api.getSubscriptions().execute().subscriptions()) {
                api.unsubscribe(subscription.url()).execute();
                log.info("MAX: удалена webhook-подписка {}", subscription.url());
            }
        } catch (Exception e) {
            log.error("MAX: не удалось удалить webhook-подписки", e);
        }
    }
}
