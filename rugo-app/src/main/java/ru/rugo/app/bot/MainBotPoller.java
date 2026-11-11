package ru.rugo.app.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UpdateType;
import ru.rugo.common.max.webhook.MaxDeliveryState;

import java.util.Set;

/**
 * Long-polling апдейтов основного бота — только для приветствия на «Начать».
 * Админ-бот опрашивает свой токен, конфликта нет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainBotPoller {

    private static final Set<UpdateType> TYPES = Set.of(UpdateType.BOT_STARTED, UpdateType.MESSAGE_CREATED);

    private final MaxBotAPI api;
    private final MainBotUpdateHandler handler;
    private final MaxDeliveryState deliveryState;

    @Value("${max.bot.token:}")
    private String token;

    private volatile Long marker;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (!deliveryState.longPollingEnabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            GetUpdatesQuery query = api.getUpdates()
                    .limit(50)
                    .timeout(20)
                    .types(TYPES);
            if (marker != null) {
                query.marker(marker);
            }
            UpdateList result = query.execute();
            if (result.marker() != null) {
                marker = result.marker();
            }
            for (Update update : result.updates()) {
                try {
                    handler.handle(update);
                } catch (Exception e) {
                    log.warn("Ошибка обработки апдейта {}: {}", update.updateType(), e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Long-polling основного бота: {}", e.toString());
        }
    }
}
