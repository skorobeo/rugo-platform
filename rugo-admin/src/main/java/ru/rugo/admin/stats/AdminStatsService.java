package ru.rugo.admin.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.web.dto.AdminStatsDto;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.util.AnalyticsHash;

import java.util.List;

/** Сводка для команды {@code /stats} и экрана статистики веб-админки. */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AnalyticsEventRepository eventRepository;
    private final PartnerLeadRepository leadRepository;
    private final AdminUserRepository adminUserRepository;
    private final AppProperties properties;

    /** Текстовая сводка для бота {@code /stats}. */
    @Transactional(readOnly = true)
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Статистика RuGo\n");
        sb.append("Уникальных пользователей: ").append(distinctUsers()).append("\n");
        sb.append("Заходов (app_open): ").append(countByEvent("app_open")).append("\n");
        sb.append("По реферальным ссылкам (ref_open): ").append(countByEvent("ref_open")).append("\n");
        sb.append("Заявок всего: ").append(leadRepository.count())
                .append(" (новых ").append(leadRepository.countByStatus(PartnerLead.STATUS_NEW))
                .append(", принято ").append(leadRepository.countByStatus(PartnerLead.STATUS_ACCEPTED))
                .append(", отклонено ").append(leadRepository.countByStatus(PartnerLead.STATUS_REJECTED))
                .append(")\n");
        sb.append("\nСобытия:\n");
        for (Object[] row : countsByEvent()) {
            sb.append(row[0]).append(": ").append(row[1]).append("\n");
        }
        return sb.toString();
    }

    /** Структурированная статистика для веб-админки. */
    @Transactional(readOnly = true)
    public AdminStatsDto stats() {
        List<AdminStatsDto.EventRow> events = countsByEventWithUnique().stream()
                .map(row -> new AdminStatsDto.EventRow(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .toList();
        return new AdminStatsDto(
                distinctUsers(),
                countByEvent("app_open"),
                countByEvent("ref_open"),
                countByEvent("cta_click"),
                leadRepository.count(),
                leadRepository.countByStatus(PartnerLead.STATUS_NEW),
                leadRepository.countByStatus(PartnerLead.STATUS_ACCEPTED),
                leadRepository.countByStatus(PartnerLead.STATUS_REJECTED),
                events);
    }

    /** Хеши админов: их заходы не должны попадать в статистику. */
    private List<String> excludedHashes() {
        List<Long> ids = adminUserRepository.findAllMaxUserIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        String salt = properties.analyticsSalt() == null ? "" : properties.analyticsSalt();
        return ids.stream().map(id -> AnalyticsHash.userHash(salt, id)).toList();
    }

    private long distinctUsers() {
        List<String> excluded = excludedHashes();
        return excluded.isEmpty()
                ? eventRepository.countDistinctUsers()
                : eventRepository.countDistinctUsersExcluding(excluded);
    }

    private long countByEvent(String event) {
        List<String> excluded = excludedHashes();
        return excluded.isEmpty()
                ? eventRepository.countByEvent(event)
                : eventRepository.countByEventExcluding(event, excluded);
    }

    private List<Object[]> countsByEvent() {
        List<String> excluded = excludedHashes();
        return excluded.isEmpty()
                ? eventRepository.countsByEvent()
                : eventRepository.countsByEventExcluding(excluded);
    }

    private List<Object[]> countsByEventWithUnique() {
        List<String> excluded = excludedHashes();
        return excluded.isEmpty()
                ? eventRepository.countsByEventWithUnique()
                : eventRepository.countsByEventWithUniqueExcluding(excluded);
    }
}
