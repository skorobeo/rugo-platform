package ru.rugo.admin.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.admin.stats.AdminIdentityResolver.Identity;
import ru.rugo.admin.stats.AdminIdentityResolver.Type;
import ru.rugo.admin.web.dto.AdminDashboardDto;
import ru.rugo.core.entity.AnalyticsEvent;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.repository.UserRepository;
import ru.rugo.core.repository.WorkshopRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Общий дашборд: заявки, каталог, люди, воронка и оперативные подсказки. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final List<String> DIRECTIONS = List.of("equipment", "materials", "repair", "park");
    private static final Map<String, String> DIRECTION_LABELS = Map.of(
            "equipment", "Техника",
            "materials", "Материалы",
            "repair", "Мастерские",
            "park", "Парк техники"
    );
    private static final Map<String, String> EVENT_LABELS = Map.ofEntries(
            Map.entry("app_open", "Заходы в приложение"),
            Map.entry("cta_click", "Клики по CTA"),
            Map.entry("ref_open", "Реферальные заходы"),
            Map.entry("form_open", "Открытия формы"),
            Map.entry("form_start", "Начало заполнения"),
            Map.entry("form_submit", "Отправки формы"),
            Map.entry("form_success", "Успешные отправки"),
            Map.entry("form_fail", "Ошибки отправки"),
            Map.entry("form_error", "Ошибки полей"),
            Map.entry("call_click", "Звонки"),
            Map.entry("write_click", "Написать")
    );
    private static final Set<String> GENERAL_EVENTS = Set.of(
            "app_open", "cta_click", "ref_open", "form_open", "form_start",
            "form_submit", "form_success", "form_fail", "form_error", "call_click", "write_click"
    );
    private static final Set<EquipmentStatus> PUBLISHED = Set.of(
            EquipmentStatus.AVAILABLE, EquipmentStatus.BUSY, EquipmentStatus.RESERVED, EquipmentStatus.BOOKED);

    private final PartnerLeadRepository leadRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;
    private final WorkshopRepository workshopRepository;
    private final AnalyticsEventRepository eventRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminProperties properties;
    private final AdminIdentityResolver identityResolver;

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard(String period) {
        AdminStatsWindow window = AdminStatsWindow.of(period, properties);
        List<PartnerLead> leads = leadRepository.findAllByOrderByCreatedAtDesc();
        List<User> users = userRepository.findAll();
        List<Equipment> equipment = equipmentRepository.findAll();
        List<Material> materials = materialRepository.findAll();
        List<Workshop> workshops = workshopRepository.findAll();
        List<AnalyticsEvent> events = eventRepository
                .findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(window.from());
        Map<String, Identity> identities = identityResolver.resolve(events);
        List<AnalyticsEvent> visible = events.stream()
                .filter(event -> identities.get(event.getUserHash()) != null
                        && identities.get(event.getUserHash()).type() != Type.ADMIN)
                .toList();

        List<PartnerLead> createdLeads = leads.stream()
                .filter(lead -> window.contains(lead.getCreatedAt()))
                .toList();
        List<PartnerLead> decidedLeads = leads.stream()
                .filter(lead -> window.contains(lead.getDecidedAt()))
                .filter(lead -> PartnerLead.STATUS_ACCEPTED.equals(lead.getStatus())
                        || PartnerLead.STATUS_REJECTED.equals(lead.getStatus()))
                .toList();

        long accepted = decidedLeads.stream()
                .filter(lead -> PartnerLead.STATUS_ACCEPTED.equals(lead.getStatus())).count();
        long rejected = decidedLeads.stream()
                .filter(lead -> PartnerLead.STATUS_REJECTED.equals(lead.getStatus())).count();
        long pending = leads.stream()
                .filter(lead -> PartnerLead.STATUS_NEW.equals(lead.getStatus())).count();

        long cardsPublished = equipment.stream().filter(e -> PUBLISHED.contains(e.getStatus())).count()
                + materials.stream().filter(m -> Material.STATUS_APPROVED.equals(m.getStatus())).count()
                + workshops.stream().filter(w -> Workshop.STATUS_APPROVED.equals(w.getStatus())).count();
        long cardsHidden = equipment.stream().filter(e -> e.getStatus() == EquipmentStatus.HIDDEN).count()
                + materials.stream().filter(m -> Material.STATUS_HIDDEN.equals(m.getStatus())).count();
        long cardsPending = equipment.stream().filter(e -> e.getStatus() == EquipmentStatus.PENDING).count()
                + materials.stream().filter(m -> Material.STATUS_PENDING.equals(m.getStatus())).count()
                + workshops.stream().filter(w -> Workshop.STATUS_PENDING.equals(w.getStatus())).count();

        long usersNew = users.stream().filter(user -> {
            if (user.getCreatedAt() == null) {
                return false;
            }
            return window.contains(user.getCreatedAt().atZone(window.zone()).toOffsetDateTime());
        }).count();

        AdminDashboardDto.Kpi kpi = new AdminDashboardDto.Kpi(
                createdLeads.size(),
                accepted,
                rejected,
                pending,
                percent(accepted, accepted + rejected),
                averageMinutes(decidedLeads),
                users.size(),
                usersNew,
                cardsPublished,
                cardsHidden,
                cardsPending
        );

        return new AdminDashboardDto(
                window.period(),
                window.from(),
                window.to(),
                kpi,
                dynamics(window, createdLeads, decidedLeads),
                directions(createdLeads, decidedLeads),
                funnel(visible),
                audience(identities, visible, window),
                visitors(identities, visible),
                attention(leads, users, equipment, materials, visible, pending),
                sources(createdLeads),
                districts(createdLeads),
                appEvents(visible),
                recentLeads(leads, window)
        );
    }

    private List<AdminDashboardDto.DailyRow> dynamics(
            AdminStatsWindow window, List<PartnerLead> createdLeads, List<PartnerLead> decidedLeads) {
        Map<LocalDate, long[]> counts = new TreeMap<>();
        if (!"all".equals(window.period())) {
            LocalDate date = window.from().atZoneSameInstant(window.zone()).toLocalDate();
            LocalDate end = window.to().atZoneSameInstant(window.zone()).toLocalDate();
            while (!date.isAfter(end)) {
                counts.put(date, new long[3]);
                date = date.plusDays(1);
            }
        }
        for (PartnerLead lead : createdLeads) {
            LocalDate date = lead.getCreatedAt().atZoneSameInstant(window.zone()).toLocalDate();
            counts.computeIfAbsent(date, ignored -> new long[3])[0]++;
        }
        for (PartnerLead lead : decidedLeads) {
            LocalDate date = lead.getDecidedAt().atZoneSameInstant(window.zone()).toLocalDate();
            long[] row = counts.computeIfAbsent(date, ignored -> new long[3]);
            if (PartnerLead.STATUS_ACCEPTED.equals(lead.getStatus())) {
                row[1]++;
            } else if (PartnerLead.STATUS_REJECTED.equals(lead.getStatus())) {
                row[2]++;
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new AdminDashboardDto.DailyRow(
                        entry.getKey().toString(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
    }

    private List<AdminDashboardDto.DirectionRow> directions(
            List<PartnerLead> createdLeads, List<PartnerLead> decidedLeads) {
        Map<String, Long> created = createdLeads.stream()
                .collect(Collectors.groupingBy(this::direction, Collectors.counting()));
        Map<String, List<PartnerLead>> decided = decidedLeads.stream()
                .collect(Collectors.groupingBy(this::direction));
        List<AdminDashboardDto.DirectionRow> rows = new ArrayList<>();
        for (String direction : DIRECTIONS) {
            List<PartnerLead> processed = decided.getOrDefault(direction, List.of());
            long accepted = processed.stream()
                    .filter(lead -> PartnerLead.STATUS_ACCEPTED.equals(lead.getStatus())).count();
            long rejected = processed.stream()
                    .filter(lead -> PartnerLead.STATUS_REJECTED.equals(lead.getStatus())).count();
            rows.add(new AdminDashboardDto.DirectionRow(
                    direction,
                    DIRECTION_LABELS.get(direction),
                    created.getOrDefault(direction, 0L),
                    accepted,
                    rejected,
                    percent(accepted, accepted + rejected),
                    averageMinutes(processed)
            ));
        }
        return rows;
    }

    private AdminDashboardDto.Funnel funnel(List<AnalyticsEvent> events) {
        Map<String, Set<String>> sessions = new HashMap<>();
        for (AnalyticsEvent event : events) {
            if (!GENERAL_EVENTS.contains(event.getEvent())) {
                continue;
            }
            sessions.computeIfAbsent(event.getEvent(), ignored -> new HashSet<>()).add(sessionKey(event));
        }
        return new AdminDashboardDto.Funnel(
                size(sessions, "app_open"),
                size(sessions, "cta_click"),
                size(sessions, "form_open"),
                size(sessions, "form_start"),
                size(sessions, "form_submit"),
                size(sessions, "form_success"),
                size(sessions, "form_fail")
        );
    }

    private AdminDashboardDto.Audience audience(
            Map<String, Identity> identities, List<AnalyticsEvent> events, AdminStatsWindow window) {
        long known = identities.values().stream().filter(i -> i.type() == Type.KNOWN).count();
        long unknown = identities.values().stream().filter(i -> i.type() == Type.UNKNOWN_MAX).count();
        long anonymous = identities.values().stream().filter(i -> i.type() == Type.ANONYMOUS).count();
        Set<String> sessions = events.stream().map(this::sessionKey).collect(Collectors.toSet());
        return new AdminDashboardDto.Audience(known, unknown, anonymous, sessions.size());
    }

    private List<AdminDashboardDto.VisitorRow> visitors(
            Map<String, Identity> identities, List<AnalyticsEvent> events) {
        Map<String, List<AnalyticsEvent>> byHash = events.stream()
                .collect(Collectors.groupingBy(AnalyticsEvent::getUserHash));
        List<AdminDashboardDto.VisitorRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<AnalyticsEvent>> entry : byHash.entrySet()) {
            Identity identity = identities.get(entry.getKey());
            if (identity == null || identity.type() == Type.ADMIN) {
                continue;
            }
            List<AnalyticsEvent> list = entry.getValue().stream()
                    .sorted(Comparator.comparing(AnalyticsEvent::getCreatedAt))
                    .toList();
            AnalyticsEvent last = list.get(list.size() - 1);
            rows.add(new AdminDashboardDto.VisitorRow(
                    identity.hash(),
                    identity.type().name().toLowerCase(Locale.ROOT),
                    identity.label(),
                    identity.phone(),
                    last.getCreatedAt(),
                    stringProp(last, "platform"),
                    last.getSource(),
                    furthestGeneralStep(list),
                    list.size()
            ));
        }
        return rows.stream()
                .sorted(Comparator.comparing(AdminDashboardDto.VisitorRow::lastSeen,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(200)
                .toList();
    }

    private List<AdminDashboardDto.AttentionRow> attention(
            List<PartnerLead> leads,
            List<User> users,
            List<Equipment> equipment,
            List<Material> materials,
            List<AnalyticsEvent> events,
            long pending) {
        List<AdminDashboardDto.AttentionRow> rows = new ArrayList<>();
        OffsetDateTime oldest = leads.stream()
                .filter(lead -> PartnerLead.STATUS_NEW.equals(lead.getStatus()))
                .map(PartnerLead::getCreatedAt)
                .filter(value -> value != null)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
        long oldestMinutes = oldest == null ? 0 : Duration.between(oldest, OffsetDateTime.now()).toMinutes();
        rows.add(new AdminDashboardDto.AttentionRow(
                "pending", "Ждут решения", pending,
                oldest == null ? "Нет новых заявок" : "Самая старая ждёт " + humanDuration(oldestMinutes),
                "/leads?status=new"));
        long hiddenEquipment = equipment.stream()
                .filter(e -> e.getStatus() == EquipmentStatus.HIDDEN).count();
        long hiddenMaterials = materials.stream()
                .filter(m -> Material.STATUS_HIDDEN.equals(m.getStatus())).count();
        long hidden = hiddenEquipment + hiddenMaterials;
        String hiddenLink = hidden == 0
                ? "/catalog"
                : hiddenEquipment > 0
                        ? "/catalog?status=HIDDEN&type=equipment"
                        : "/catalog?status=HIDDEN&type=materials";
        rows.add(new AdminDashboardDto.AttentionRow(
                "hidden", "Скрытые карточки", hidden,
                "Сняты с публикации · техника " + hiddenEquipment + " · материалы " + hiddenMaterials,
                hiddenLink));
        Set<Long> withCards = new HashSet<>();
        equipment.forEach(item -> {
            if (item.getOwner() != null) {
                withCards.add(item.getOwner().getId());
            }
        });
        materials.forEach(item -> {
            if (item.getSupplier() != null) {
                withCards.add(item.getSupplier().getId());
            }
        });
        Set<Long> adminMaxIds = new HashSet<>(adminUserRepository.findAllMaxUserIds());
        long withoutCards = users.stream()
                .filter(user -> user.getMaxId() == null || !adminMaxIds.contains(user.getMaxId()))
                .filter(user -> !withCards.contains(user.getId()))
                .count();
        rows.add(new AdminDashboardDto.AttentionRow(
                "no-cards", "Пользователи без карточек", withoutCards,
                "Без техники и материалов", "/users?filter=no-cards"));
        long formFails = events.stream().filter(event -> "form_fail".equals(event.getEvent())).count();
        rows.add(new AdminDashboardDto.AttentionRow(
                "form-fail", "Ошибки формы", formFails, "Попытки отправки", "/leads"));
        long parkNew = leads.stream()
                .filter(lead -> PartnerLead.STATUS_NEW.equals(lead.getStatus()))
                .filter(lead -> lead.getOfferTypes() != null && lead.getOfferTypes().contains("park"))
                .count();
        rows.add(new AdminDashboardDto.AttentionRow(
                "park", "Заявки на парк", parkNew, "Ручная обработка", "/leads?direction=park&status=new"));
        return rows;
    }

    private List<AdminDashboardDto.SourceRow> sources(List<PartnerLead> leads) {
        Map<String, Long> counts = leads.stream().collect(Collectors.groupingBy(
                lead -> lead.getSource() == null || lead.getSource().isBlank() ? "Прямой заход" : lead.getSource(),
                Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new AdminDashboardDto.SourceRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AdminDashboardDto.DistrictRow> districts(List<PartnerLead> leads) {
        Map<String, Long> counts = leads.stream().collect(Collectors.groupingBy(
                lead -> lead.getDistrict() == null || lead.getDistrict().isBlank() ? "Не указан" : lead.getDistrict(),
                Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new AdminDashboardDto.DistrictRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AdminDashboardDto.EventRow> appEvents(List<AnalyticsEvent> events) {
        Map<String, List<AnalyticsEvent>> grouped = events.stream()
                .filter(event -> GENERAL_EVENTS.contains(event.getEvent()))
                .collect(Collectors.groupingBy(AnalyticsEvent::getEvent));
        return grouped.entrySet().stream()
                .map(entry -> new AdminDashboardDto.EventRow(
                        entry.getKey(),
                        EVENT_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue().size(),
                        entry.getValue().stream().map(AnalyticsEvent::getUserHash).distinct().count()))
                .sorted(Comparator.comparingLong(AdminDashboardDto.EventRow::total).reversed())
                .toList();
    }

    private List<AdminDashboardDto.RecentLeadRow> recentLeads(
            List<PartnerLead> leads, AdminStatsWindow window) {
        return leads.stream()
                .filter(lead -> window.contains(lead.getCreatedAt()))
                .limit(5)
                .map(lead -> new AdminDashboardDto.RecentLeadRow(
                        lead.getId(),
                        lead.getName(),
                        lead.getCompanyName(),
                        direction(lead),
                        lead.getStatus(),
                        lead.getCreatedAt(),
                        lead.getCreatedAt() == null ? 0
                                : Duration.between(lead.getCreatedAt(), OffsetDateTime.now()).toMinutes()))
                .toList();
    }

    private String furthestGeneralStep(List<AnalyticsEvent> events) {
        Set<String> names = events.stream().map(AnalyticsEvent::getEvent).collect(Collectors.toSet());
        if (names.contains("form_success")) return "Успешная отправка";
        if (names.contains("form_fail")) return "Ошибка отправки";
        if (names.contains("form_submit")) return "Отправка";
        if (names.contains("form_start")) return "Заполнение";
        if (names.contains("form_open")) return "Открыл форму";
        if (names.contains("cta_click")) return "Кликнул CTA";
        return "Открыл приложение";
    }

    private String direction(PartnerLead lead) {
        List<String> types = lead.getOfferTypes();
        if (types == null || types.isEmpty()) {
            return "unknown";
        }
        if (types.contains("park")) return "park";
        if (types.contains("equipment")) return "equipment";
        if (types.contains("materials")) return "materials";
        if (types.contains("repair")) return "repair";
        return types.get(0);
    }

    private long averageMinutes(List<PartnerLead> leads) {
        return Math.round(leads.stream()
                .filter(lead -> lead.getCreatedAt() != null && lead.getDecidedAt() != null)
                .mapToLong(lead -> Duration.between(lead.getCreatedAt(), lead.getDecidedAt()).toMinutes())
                .average()
                .orElse(0));
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    private long size(Map<String, Set<String>> sessions, String event) {
        return sessions.getOrDefault(event, Set.of()).size();
    }

    private String sessionKey(AnalyticsEvent event) {
        Object session = event.getProps() == null ? null : event.getProps().get("session_id");
        return session instanceof String value && !value.isBlank() ? value : event.getUserHash();
    }

    private String stringProp(AnalyticsEvent event, String key) {
        Object value = event.getProps() == null ? null : event.getProps().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String humanDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " мин";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours < 24) {
            return hours + " ч" + (rest == 0 ? "" : " " + rest + " мин");
        }
        long days = hours / 24;
        return days + " д" + (hours % 24 == 0 ? "" : " " + (hours % 24) + " ч");
    }
}
