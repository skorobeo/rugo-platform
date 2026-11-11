package ru.rugo.core.service;

import ru.rugo.core.service.model.EventEntry;

import java.util.List;

public interface AnalyticsEventService {

    /**
     * Сохраняет события, отбрасывая неизвестные имена и слишком большие props.
     *
     * @param maxUserId идентификатор пользователя MAX из проверенного initData (может быть null)
     * @param sessionId анонимный идентификатор сессии (для событий без initData)
     * @param source    код источника перехода
     */
    void record(Long maxUserId, String sessionId, String source, List<EventEntry> entries);
}
