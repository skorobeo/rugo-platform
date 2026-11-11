package ru.rugo.admin.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UpdateType;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.common.max.webhook.MaxDeliveryState;

import java.util.Set;

/** Long-polling апдейтов админ-бота. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBotPoller {

    private static final Set<UpdateType> TYPES = Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK);

    @Qualifier("adminBotApi")
    private final MaxBotAPI adminBotApi;

    private final AdminUpdateHandler updateHandler;
    private final AdminProperties properties;
    private final AdminDirectory adminDirectory;
    private final MaxDeliveryState deliveryState;

    @Value("${max.admin.bot.token:}")
    private String adminToken;

    private volatile Long marker;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (!deliveryState.longPollingEnabled()) {
            return;
        }
        if (adminToken == null || adminToken.isBlank() || !adminDirectory.hasAdmins()) {
            return;
        }
        try {
            GetUpdatesQuery query = adminBotApi.getUpdates()
                    .limit(properties.pollLimit())
                    .timeout(properties.pollTimeoutSeconds())
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
                    updateHandler.handle(update);
                } catch (Exception e) {
                    log.warn("Ошибка обработки апдейта {}: {}", update.updateType(), e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Long-polling ошибка: {}", e.toString());
        }
    }
}
