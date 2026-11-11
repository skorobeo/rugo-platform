package ru.rugo.app.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.Update;
import ru.rugo.app.bot.MainBotUpdateHandler;
import ru.rugo.common.max.webhook.MaxDeliveryState;
import ru.rugo.common.max.webhook.WebhookDedup;
import ru.rugo.common.max.webhook.WebhookSecret;

/** Приём апдейтов основного бота через webhook MAX. */
@Slf4j
@RestController
public class MaxWebhookController {

    private static final int MAX_BODY_CHARS = 1_048_576;

    private final MaxBotAPI maxBotAPI;
    private final MainBotUpdateHandler updateHandler;
    private final MaxDeliveryState deliveryState;
    private final WebhookDedup dedup;
    private final String secret;

    public MaxWebhookController(MaxBotAPI maxBotAPI,
                                MainBotUpdateHandler updateHandler,
                                MaxDeliveryState deliveryState,
                                WebhookDedup dedup,
                                @Value("${max.bot.webhook.secret:}") String secret) {
        this.maxBotAPI = maxBotAPI;
        this.updateHandler = updateHandler;
        this.deliveryState = deliveryState;
        this.dedup = dedup;
        this.secret = secret;
    }

    @PostMapping(path = "/api/max/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Max-Bot-Api-Secret", required = false) String providedSecret,
            @RequestBody(required = false) String body) {
        if (!deliveryState.webhookEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (!WebhookSecret.matches(secret, providedSecret)) {
            log.warn("MAX webhook: запрос с неверным секретом отклонён");
            return ResponseEntity.status(401).build();
        }
        if (body == null || body.isBlank() || body.length() > MAX_BODY_CHARS) {
            return ResponseEntity.badRequest().build();
        }

        String key = WebhookDedup.hash(body);
        if (dedup.isDuplicate(key)) {
            log.debug("MAX webhook: повторный апдейт пропущен");
            return ResponseEntity.ok().build();
        }

        Update update;
        try {
            update = maxBotAPI.serializer().deserialize(body, Update.class);
        } catch (Exception e) {
            // Повторная доставка не поможет: тело не разбирается.
            log.warn("MAX webhook: не удалось разобрать апдейт: {}", e.toString());
            return ResponseEntity.ok().build();
        }

        log.info("MAX webhook: получен апдейт {}", update.updateType());
        try {
            updateHandler.handle(update);
        } catch (Exception e) {
            log.error("MAX webhook: ошибка обработки апдейта {}", update.updateType(), e);
            return ResponseEntity.status(500).build();
        }

        dedup.markProcessed(key);
        return ResponseEntity.ok().build();
    }
}
