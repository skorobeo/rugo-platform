package ru.rugo.admin.stats;

import ru.rugo.admin.config.AdminProperties;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Единое окно статистики: период, точка старта и часовой пояс Воронежа.
 * Всё, что раньше {@link #from()}, в отчёты не попадает.
 */
public record AdminStatsWindow(String period, OffsetDateTime from, OffsetDateTime to, ZoneId zone) {

    public static final ZoneId VORONEZH = ZoneId.of("Europe/Moscow");
    private static final OffsetDateTime EPOCH = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    public static AdminStatsWindow of(String rawPeriod, AdminProperties properties) {
        String period = normalize(rawPeriod);
        OffsetDateTime now = OffsetDateTime.now(VORONEZH);
        LocalDate today = now.toLocalDate();
        OffsetDateTime periodStart = switch (period) {
            case "today" -> today.atStartOfDay(VORONEZH).toOffsetDateTime();
            case "30d" -> today.minusDays(29).atStartOfDay(VORONEZH).toOffsetDateTime();
            case "all" -> EPOCH;
            default -> today.minusDays(6).atStartOfDay(VORONEZH).toOffsetDateTime();
        };
        OffsetDateTime statsStart = properties == null ? null : properties.statsStartAtOrNull();
        OffsetDateTime from = statsStart != null && statsStart.isAfter(periodStart) ? statsStart : periodStart;
        return new AdminStatsWindow(period.equals("all") && statsStart != null ? "all" : period, from, now, VORONEZH);
    }

    public static String normalize(String rawPeriod) {
        if (rawPeriod == null || rawPeriod.isBlank()) {
            return "7d";
        }
        return switch (rawPeriod.trim().toLowerCase()) {
            case "today", "day" -> "today";
            case "30d", "30" -> "30d";
            case "all", "all-time" -> "all";
            default -> "7d";
        };
    }

    public boolean contains(OffsetDateTime value) {
        return value != null && !value.isBefore(from) && !value.isAfter(to);
    }
}
