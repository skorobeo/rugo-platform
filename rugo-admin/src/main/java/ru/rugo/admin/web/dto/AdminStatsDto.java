package ru.rugo.admin.web.dto;

import java.util.List;

/** Структурированная статистика для веб-админки. */
public record AdminStatsDto(
        long uniqueUsers,
        long appOpen,
        long refOpen,
        long ctaClick,
        long leadsTotal,
        long leadsNew,
        long leadsAccepted,
        long leadsRejected,
        List<EventRow> events
) {

    /** Одно событие: всего записей и уникальных пользователей. */
    public record EventRow(String event, long total, long unique) {
    }
}
