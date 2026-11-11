package ru.rugo.app.web.dto;

import java.util.Map;

public record EventRequestItem(String name, Map<String, Object> props, Long ts) {
}
