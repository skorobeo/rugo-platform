package ru.rugo.admin.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Данные общего дашборда. Имена полей совпадают с фронтендом. */
public record AdminDashboardDto(
        String period,
        OffsetDateTime from,
        OffsetDateTime to,
        Kpi kpi,
        List<DailyRow> dynamics,
        List<DirectionRow> directions,
        Funnel funnel,
        Audience audience,
        List<VisitorRow> visitors,
        List<AttentionRow> attention,
        List<SourceRow> sources,
        List<DistrictRow> districts,
        List<EventRow> appEvents,
        List<RecentLeadRow> recentLeads
) {

    public record Kpi(
            long leadsCreated,
            long leadsAccepted,
            long leadsRejected,
            long leadsPending,
            double conversion,
            long avgDecisionMinutes,
            long usersTotal,
            long usersNew,
            long cardsPublished,
            long cardsHidden,
            long cardsPending
    ) {
    }

    public record DailyRow(String date, long created, long accepted, long rejected) {
    }

    public record DirectionRow(
            String direction,
            String label,
            long created,
            long accepted,
            long rejected,
            double conversion,
            long avgDecisionMinutes
    ) {
    }

    public record Funnel(
            long sessionsOpened,
            long sessionsCta,
            long sessionsFormOpened,
            long sessionsFormStarted,
            long sessionsSubmitted,
            long sessionsSucceeded,
            long sessionsFailed
    ) {
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
            String furthestStep,
            long events
    ) {
    }

    public record AttentionRow(String key, String label, long count, String detail, String link) {
    }

    public record SourceRow(String source, long count) {
    }

    public record DistrictRow(String district, long count) {
    }

    public record EventRow(String event, String label, long total, long unique) {
    }

    public record RecentLeadRow(
            Long id,
            String name,
            String companyName,
            String direction,
            String status,
            OffsetDateTime createdAt,
            long ageMinutes
    ) {
    }
}
