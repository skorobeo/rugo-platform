package ru.rugo.admin.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.admin.stats.AdminIdentityResolver.Identity;
import ru.rugo.admin.stats.AdminIdentityResolver.Type;
import ru.rugo.admin.web.dto.AdminZhbkStatsDto;
import ru.rugo.core.entity.AnalyticsEvent;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.repository.PartnerCategoryRepository;
import ru.rugo.core.repository.PartnerGroupRepository;
import ru.rugo.core.repository.PartnerProductRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Отдельная аналитика каталога ЖБК: посетители, клики, звонки. */
@Service
@RequiredArgsConstructor
public class AdminZhbkStatsService {

    private static final Set<String> ZHBK_EVENTS = Set.of(
            "zhbk_open", "zhbk_group_open", "zhbk_category_open", "zhbk_search",
            "zhbk_products_view", "zhbk_product_open", "zhbk_calculator_open", "call_click"
    );

    private final AnalyticsEventRepository eventRepository;
    private final PartnerGroupRepository groupRepository;
    private final PartnerCategoryRepository categoryRepository;
    private final PartnerProductRepository productRepository;
    private final AdminProperties properties;
    private final AdminIdentityResolver identityResolver;

    @Transactional(readOnly = true)
    public AdminZhbkStatsDto stats(String period) {
        AdminStatsWindow window = AdminStatsWindow.of(period, properties);
        List<AnalyticsEvent> all = eventRepository
                .findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(window.from());
        Map<String, Identity> identities = identityResolver.resolve(all);
        List<AnalyticsEvent> events = all.stream()
                .filter(this::isZhbkEvent)
                .filter(event -> identities.get(event.getUserHash()) != null
                        && identities.get(event.getUserHash()).type() != Type.ADMIN)
                .toList();

        Map<String, Set<String>> sessions = sessionsByEvent(events);
        Set<String> visitors = events.stream()
                .filter(event -> "zhbk_open".equals(event.getEvent()))
                .map(AnalyticsEvent::getUserHash)
                .collect(Collectors.toSet());
        Set<String> callers = events.stream()
                .filter(event -> "call_click".equals(event.getEvent()))
                .map(AnalyticsEvent::getUserHash)
                .collect(Collectors.toSet());
        long calls = events.stream().filter(event -> "call_click".equals(event.getEvent())).count();

        AdminZhbkStatsDto.Kpi kpi = new AdminZhbkStatsDto.Kpi(
                visitors.size(),
                sessions.getOrDefault("zhbk_open", Set.of()).size(),
                sessions.getOrDefault("zhbk_category_open", Set.of()).size(),
                sessions.getOrDefault("zhbk_product_open", Set.of()).size(),
                sessions.getOrDefault("zhbk_calculator_open", Set.of()).size(),
                calls,
                callers.size(),
                percent(callers.size(), visitors.size())
        );

        return new AdminZhbkStatsDto(
                window.period(),
                window.from(),
                window.to(),
                kpi,
                funnel(sessions),
                audience(identities, events),
                visitors(identities, events),
                top(events, "zhbk_group_open", "groupSlug", "groupTitle"),
                top(events, "zhbk_category_open", "categorySlug", "categoryTitle"),
                top(events, "zhbk_product_open", "productId", "productName"),
                searches(events),
                calls(identities, events),
                new AdminZhbkStatsDto.Catalog(
                        groupRepository.findAllByActiveTrueOrderBySortOrderAsc().size(),
                        categoryRepository.findAllByActiveTrueOrderBySortOrderAsc().size(),
                        productRepository.countByActiveTrue())
        );
    }

    private List<AdminZhbkStatsDto.FunnelStep> funnel(Map<String, Set<String>> sessions) {
        List<String[]> steps = List.of(
                new String[]{"open", "Открыли ЖБК", "zhbk_open"},
                new String[]{"group", "Открыли группу", "zhbk_group_open"},
                new String[]{"category", "Открыли категорию", "zhbk_category_open"},
                new String[]{"products", "Открыли список изделий", "zhbk_products_view"},
                new String[]{"product", "Открыли изделие", "zhbk_product_open"},
                new String[]{"calculator", "Открыли калькулятор", "zhbk_calculator_open"},
                new String[]{"call", "Позвонили", "call_click"}
        );
        List<AdminZhbkStatsDto.FunnelStep> result = new ArrayList<>();
        long previous = 0;
        for (String[] step : steps) {
            long count = sessions.getOrDefault(step[2], Set.of()).size();
            result.add(new AdminZhbkStatsDto.FunnelStep(
                    step[0], step[1], count, previous == 0 ? 0 : percent(count, previous)));
            previous = count;
        }
        return result;
    }

    private AdminZhbkStatsDto.Audience audience(
            Map<String, Identity> identities, List<AnalyticsEvent> events) {
        Set<String> present = events.stream()
                .map(AnalyticsEvent::getUserHash)
                .collect(Collectors.toSet());
        long known = present.stream().map(identities::get).filter(identity -> identity != null && identity.type() == Type.KNOWN).count();
        long unknown = present.stream().map(identities::get).filter(identity -> identity != null && identity.type() == Type.UNKNOWN_MAX).count();
        long anonymous = present.stream().map(identities::get).filter(identity -> identity != null && identity.type() == Type.ANONYMOUS).count();
        return new AdminZhbkStatsDto.Audience(known, unknown, anonymous, known + unknown + anonymous);
    }

    private List<AdminZhbkStatsDto.VisitorRow> visitors(
            Map<String, Identity> identities, List<AnalyticsEvent> events) {
        Map<String, List<AnalyticsEvent>> byHash = events.stream()
                .collect(Collectors.groupingBy(AnalyticsEvent::getUserHash));
        List<AdminZhbkStatsDto.VisitorRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<AnalyticsEvent>> entry : byHash.entrySet()) {
            Identity identity = identities.get(entry.getKey());
            if (identity == null || identity.type() == Type.ADMIN) {
                continue;
            }
            List<AnalyticsEvent> list = entry.getValue().stream()
                    .sorted(Comparator.comparing(AnalyticsEvent::getCreatedAt))
                    .toList();
            AnalyticsEvent last = list.get(list.size() - 1);
            long productViews = list.stream()
                    .filter(event -> "zhbk_product_open".equals(event.getEvent())).count();
            long calls = list.stream().filter(event -> "call_click".equals(event.getEvent())).count();
            rows.add(new AdminZhbkStatsDto.VisitorRow(
                    identity.hash(),
                    identity.type().name().toLowerCase(Locale.ROOT),
                    identity.label(),
                    identity.phone(),
                    last.getCreatedAt(),
                    stringProp(last, "platform"),
                    last.getSource(),
                    entry(list),
                    furthestStep(list),
                    productViews,
                    calls
            ));
        }
        return rows.stream()
                .sorted(Comparator.comparing(AdminZhbkStatsDto.VisitorRow::lastSeen,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(200)
                .toList();
    }

    private List<AdminZhbkStatsDto.CallRow> calls(
            Map<String, Identity> identities, List<AnalyticsEvent> events) {
        return events.stream()
                .filter(event -> "call_click".equals(event.getEvent()))
                .sorted(Comparator.comparing(AnalyticsEvent::getCreatedAt).reversed())
                .limit(100)
                .map(event -> {
                    Identity identity = identities.get(event.getUserHash());
                    return new AdminZhbkStatsDto.CallRow(
                            event.getCreatedAt(),
                            identity == null ? "anonymous" : identity.type().name().toLowerCase(Locale.ROOT),
                            identity == null ? "Анонимная сессия" : identity.label(),
                            identity == null ? null : identity.phone(),
                            stringProp(event, "productName"),
                            stringProp(event, "categoryTitle"),
                            stringProp(event, "groupTitle"),
                            stringProp(event, "grade"),
                            stringProp(event, "variant"),
                            stringProp(event, "temperatureBand"),
                            stringProp(event, "volume"),
                            stringProp(event, "total"),
                            stringProp(event, "platform"),
                            event.getSource()
                    );
                })
                .toList();
    }

    private List<AdminZhbkStatsDto.CountRow> top(
            List<AnalyticsEvent> events, String eventName, String keyProp, String labelProp) {
        Map<String, AdminZhbkStatsDto.CountRow> counts = new LinkedHashMap<>();
        for (AnalyticsEvent event : events) {
            if (!eventName.equals(event.getEvent())) {
                continue;
            }
            String key = stringProp(event, keyProp);
            String label = stringProp(event, labelProp);
            if (key == null || key.isBlank()) {
                continue;
            }
            String finalLabel = label == null || label.isBlank() ? key : label;
            counts.compute(key, (ignored, existing) -> existing == null
                    ? new AdminZhbkStatsDto.CountRow(key, finalLabel, 1)
                    : new AdminZhbkStatsDto.CountRow(key, existing.label(), existing.count() + 1));
        }
        return counts.values().stream()
                .sorted(Comparator.comparingLong(AdminZhbkStatsDto.CountRow::count).reversed())
                .limit(10)
                .toList();
    }

    private List<AdminZhbkStatsDto.CountRow> searches(List<AnalyticsEvent> events) {
        Map<String, AdminZhbkStatsDto.CountRow> counts = new HashMap<>();
        for (AnalyticsEvent event : events) {
            if (!"zhbk_search".equals(event.getEvent())) {
                continue;
            }
            String query = stringProp(event, "query");
            if (query == null || query.isBlank()) {
                continue;
            }
            String trimmed = query.trim();
            String key = trimmed.toLowerCase(Locale.ROOT);
            counts.compute(key, (ignored, existing) -> existing == null
                    ? new AdminZhbkStatsDto.CountRow(key, trimmed, 1)
                    : new AdminZhbkStatsDto.CountRow(key, existing.label(), existing.count() + 1));
        }
        return counts.values().stream()
                .sorted(Comparator.comparingLong(AdminZhbkStatsDto.CountRow::count).reversed())
                .limit(10)
                .toList();
    }

    private Map<String, Set<String>> sessionsByEvent(List<AnalyticsEvent> events) {
        Map<String, Set<String>> result = new HashMap<>();
        for (AnalyticsEvent event : events) {
            result.computeIfAbsent(event.getEvent(), ignored -> new HashSet<>()).add(sessionKey(event));
        }
        return result;
    }

    private boolean isZhbkEvent(AnalyticsEvent event) {
        String name = event.getEvent();
        if (name == null) {
            return false;
        }
        if (ZHBK_EVENTS.contains(name) && !"call_click".equals(name)) {
            return true;
        }
        if (!"call_click".equals(name)) {
            return false;
        }
        String surface = stringProp(event, "surface");
        String place = stringProp(event, "place");
        return "zhbk".equals(surface) || "partner_product".equals(place) || "concrete_calculator".equals(place);
    }

    private String furthestStep(List<AnalyticsEvent> events) {
        Set<String> names = events.stream().map(AnalyticsEvent::getEvent).collect(Collectors.toSet());
        if (names.contains("call_click")) return "Позвонил";
        if (names.contains("zhbk_product_open")) return "Открыл изделие";
        if (names.contains("zhbk_calculator_open")) return "Открыл калькулятор";
        if (names.contains("zhbk_products_view")) return "Открыл список изделий";
        if (names.contains("zhbk_category_open")) return "Открыл категорию";
        if (names.contains("zhbk_group_open")) return "Открыл группу";
        return "Открыл ЖБК";
    }

    private String entry(List<AnalyticsEvent> events) {
        return events.stream()
                .filter(event -> "zhbk_open".equals(event.getEvent()))
                .map(event -> stringProp(event, "entry"))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String sessionKey(AnalyticsEvent event) {
        Object session = event.getProps() == null ? null : event.getProps().get("session_id");
        return session instanceof String value && !value.isBlank() ? value : event.getUserHash();
    }

    private String stringProp(AnalyticsEvent event, String key) {
        Object value = event.getProps() == null ? null : event.getProps().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }
}
