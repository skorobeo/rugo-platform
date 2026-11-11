package ru.rugo.core.service.model;

import java.util.Map;

/** Одно аналитическое событие из батча. */
public record EventEntry(String name, Map<String, Object> props, Long ts) {
}
