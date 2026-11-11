package ru.rugo.common.max.webhook;

import org.springframework.stereotype.Component;

/**
 * Текущий режим доставки апдейтов. Пока режим не выбран при старте, оба флага
 * выключены: поллер не работает, webhook-контроллер отвечает 404.
 */
@Component
public class MaxDeliveryState {

    private volatile boolean longPollingEnabled;
    private volatile boolean webhookEnabled;

    public boolean longPollingEnabled() {
        return longPollingEnabled;
    }

    public boolean webhookEnabled() {
        return webhookEnabled;
    }

    public void enableLongPolling() {
        webhookEnabled = false;
        longPollingEnabled = true;
    }

    public void enableWebhook() {
        longPollingEnabled = false;
        webhookEnabled = true;
    }

    public void disableAll() {
        longPollingEnabled = false;
        webhookEnabled = false;
    }
}
