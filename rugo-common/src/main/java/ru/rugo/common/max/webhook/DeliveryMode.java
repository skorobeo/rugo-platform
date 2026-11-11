package ru.rugo.common.max.webhook;

import java.util.Locale;

/** Способ доставки апдейтов MAX: long polling или webhook. */
public enum DeliveryMode {

    LONGPOLL,
    WEBHOOK;

    public static DeliveryMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LONGPOLL;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "webhook" -> WEBHOOK;
            default -> LONGPOLL;
        };
    }
}
