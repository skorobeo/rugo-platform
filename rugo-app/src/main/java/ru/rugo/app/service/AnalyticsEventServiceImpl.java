package ru.rugo.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.entity.AnalyticsEvent;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.service.AnalyticsEventService;
import ru.rugo.core.service.model.EventEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    /**
     * Белый список имён событий (ТЗ §8). Всё остальное отбрасывается.
     * Набор обновлён под экраны мини-приложения; section_view/scroll_depth/tab_switch/faq_open
     * больше не используются.
     */
    static final Set<String> ALLOWED_EVENTS = Set.of(
            "app_open",
            "ref_open",
            "cta_click",
            "call_click",
            "write_click",
            "form_open",
            "form_start",
            "form_error",
            "form_submit",
            "form_success",
            "form_fail",
            "cabinet_interest",
            "client_error",
            "zhbk_open",
            "zhbk_group_open",
            "zhbk_category_open",
            "zhbk_search",
            "zhbk_products_view",
            "zhbk_product_open",
            "zhbk_calculator_open"
    );

    private final AnalyticsEventRepository repository;
    private final UserHashService userHashService;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(Long maxUserId, String sessionId, String source, List<EventEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int maxBatch = properties.eventsLimit().maxBatch();
        int maxPropsBytes = properties.eventsLimit().maxPropsBytes();
        String userHash = userHashService.hash(maxUserId, sessionId);

        List<AnalyticsEvent> toSave = entries.stream()
                .limit(maxBatch)
                .filter(entry -> entry.name() != null && ALLOWED_EVENTS.contains(entry.name()))
                .map(entry -> AnalyticsEvent.builder()
                        .userHash(userHash)
                        .event(entry.name())
                        .props(sanitizeProps(entry.props(), maxPropsBytes))
                        .source(source)
                        .build())
                .toList();

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
    }

    private Map<String, Object> sanitizeProps(Map<String, Object> props, int maxPropsBytes) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        try {
            if (objectMapper.writeValueAsBytes(props).length > maxPropsBytes) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return props;
    }
}
