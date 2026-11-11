package ru.rugo.app.web.dto;

import java.util.List;

public record EventsRequest(List<EventRequestItem> events, String sessionId, String source) {
}
