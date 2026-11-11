package ru.rugo.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.app.web.dto.EventRequestItem;
import ru.rugo.app.web.dto.EventsRequest;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.service.AnalyticsEventService;
import ru.rugo.core.service.model.EventEntry;
import ru.rugo.core.util.SourceCode;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Приём аналитических событий. Всегда отвечает 202 и никогда не ломает интерфейс:
 * тело разбирается внутри try, любые ошибки гасятся и логируются.
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final AnalyticsEventService eventService;
    private final MaxInitDataValidator initDataValidator;
    private final InitDataUserExtractor userExtractor;
    private final FixedWindowRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> collect(
            @RequestBody(required = false) String body,
            @RequestHeader(value = "X-Init-Data", required = false) String initData,
            HttpServletRequest httpRequest) {
        try {
            AppProperties.EventsLimit limits = properties.eventsLimit();

            if (body == null || body.isBlank()
                    || body.getBytes(StandardCharsets.UTF_8).length > limits.maxBodyBytes()) {
                return accepted();
            }

            String ip = clientIpResolver.resolve(httpRequest);
            if (!rateLimiter.tryAcquire("events:" + ip, limits.ratePerIp(), limits.rateWindowSeconds())) {
                return accepted();
            }

            EventsRequest request = objectMapper.readValue(body, EventsRequest.class);
            if (request.events() == null || request.events().isEmpty()) {
                return accepted();
            }

            Long maxUserId = initDataValidator.validate(initData)
                    .map(userExtractor::extractUserId)
                    .orElse(null);
            String source = SourceCode.sanitize(request.source());

            List<EventEntry> entries = request.events().stream()
                    .map(item -> new EventEntry(item.name(), item.props(), item.ts()))
                    .toList();

            eventService.record(maxUserId, request.sessionId(), source, entries);
        } catch (Exception e) {
            log.warn("Не удалось обработать батч событий аналитики", e);
        }
        return accepted();
    }

    private ResponseEntity<Void> accepted() {
        return ResponseEntity.accepted().build();
    }
}
