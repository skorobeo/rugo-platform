package ru.rugo.admin.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Отдельная статистика каталога ЖБК. */
public record AdminZhbkStatsDto(
        String period,
        OffsetDateTime from,
        OffsetDateTime to,
        Kpi kpi,
        List<FunnelStep> funnel,
        Audience audience,
        List<VisitorRow> visitors,
        List<CountRow> topGroups,
        List<CountRow> topCategories,
        List<CountRow> topProducts,
        List<CountRow> searches,
        List<CallRow> calls,
        Catalog catalog
) {

    public record Kpi(
            long visitors,
            long sessions,
            long categoryViews,
            long productViews,
            long calculatorViews,
            long calls,
            long uniqueCallers,
            double callConversion
    ) {
    }

    public record FunnelStep(String key, String label, long sessions, double conversion) {
    }

    public record Audience(long known, long unknownMax, long anonymous, long sessions) {
    }

    public record VisitorRow(
            String id,
            String type,
            String label,
            String phone,
            OffsetDateTime lastSeen,
            String platform,
            String source,
            String entry,
            String furthestStep,
            long productViews,
            long calls
    ) {
    }

    public record CountRow(String key, String label, long count) {
    }

    public record CallRow(
            OffsetDateTime time,
            String type,
            String label,
            String phone,
            String productName,
            String categoryTitle,
            String groupTitle,
            String grade,
            String variant,
            String temperatureBand,
            String volume,
            String total,
            String platform,
            String source
    ) {
    }

    public record Catalog(long groups, long categories, long products) {
    }
}
