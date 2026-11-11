package ru.rugo.admin.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.Update;
import ru.rugo.admin.bot.AdminUpdateHandler;
import ru.rugo.common.max.webhook.MaxDeliveryState;
import ru.rugo.common.max.webhook.WebhookDedup;
import ru.rugo.common.max.webhook.WebhookSecret;

/** Приём апдейтов админ-бота через webhook MAX. */
@Slf4j
@RestController
public class AdminMaxWebhookController {

    private static final int MAX_BODY_CHARS = 1_048_576;

    private final MaxBotAPI adminBotApi;
    private final AdminUpdateHandler updateHandler;
    private final MaxDeliveryState deliveryState;
    private final WebhookDedup dedup;
    private final String secret;

    public AdminMaxWebhookController(@Qualifier("adminBotApi") MaxBotAPI adminBotApi,
                                     AdminUpdateHandler updateHandler,
                                     MaxDeliveryState deliveryState,
                                     WebhookDedup dedup,
                                     @Value("${max.admin.bot.webhook.secret:}") String secret) {
        this.adminBotApi = adminBotApi;
        this.updateHandler = updateHandler;
        this.deliveryState = deliveryState;
        this.dedup = dedup;
        this.secret = secret;
    }

    @PostMapping(path = "/admin/max/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Max-Bot-Api-Secret", required = false) String providedSecret,
            @RequestBody(required = false) String body) {
        if (!deliveryState.webhookEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (!WebhookSecret.matches(secret, providedSecret)) {
            log.warn("MAX admin webhook: запрос с неверным секретом отклонён");
            return ResponseEntity.status(401).build();
        }
        if (body == null || body.isBlank() || body.length() > MAX_BODY_CHARS) {
            return ResponseEntity.badRequest().build();
        }

        String key = WebhookDedup.hash(body);
        if (dedup.isDuplicate(key)) {
            log.debug("MAX admin webhook: повторный апдейт пропущен");
            return ResponseEntity.ok().build();
        }

        Update update;
        try {
            update = adminBotApi.serializer().deserialize(body, Update.class);
        } catch (Exception e) {
            log.warn("MAX admin webhook: не удалось разобрать апдейт: {}", e.toString());
            return ResponseEntity.ok().build();
        }

        log.info("MAX admin webhook: получен апдейт {}", update.updateType());
        try {
            updateHandler.handle(update);
        } catch (Exception e) {
            log.error("MAX admin webhook: ошибка обработки апдейта {}", update.updateType(), e);
            return ResponseEntity.status(500).build();
        }

        dedup.markProcessed(key);
        return ResponseEntity.ok().build();
    }
}
