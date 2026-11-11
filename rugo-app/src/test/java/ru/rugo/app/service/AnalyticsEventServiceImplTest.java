package ru.rugo.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.entity.AnalyticsEvent;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.service.model.EventEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AnalyticsEventServiceImplTest {

    @Mock
    private AnalyticsEventRepository repository;

    @Mock
    private UserHashService userHashService;

    @Mock
    private AppProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AnalyticsEventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsEventServiceImpl(repository, userHashService, properties, objectMapper);
        lenient().when(properties.eventsLimit())
                .thenReturn(new AppProperties.EventsLimit(50, 32768, 4096, 120, 60));
        lenient().when(userHashService.hash(any(), any())).thenReturn("hash");
    }

    @Test
    void savesOnlyWhitelistedEvents() {
        service.record(42L, "sess", "m_ivan", List.of(
                new EventEntry("app_open", Map.of(), 1L),
                new EventEntry("not_allowed_event", Map.of(), 2L),
                new EventEntry("cta_click", Map.of("place", "hero"), 3L)));

        ArgumentCaptor<List<AnalyticsEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<AnalyticsEvent> saved = captor.getValue();

        assertEquals(2, saved.size());
        assertEquals("app_open", saved.get(0).getEvent());
        assertEquals("cta_click", saved.get(1).getEvent());
        assertEquals("hash", saved.get(0).getUserHash());
        assertEquals("m_ivan", saved.get(0).getSource());
    }

    @Test
    void capsBatchSize() {
        when(properties.eventsLimit())
                .thenReturn(new AppProperties.EventsLimit(2, 32768, 4096, 120, 60));

        service.record(null, "sess", null, List.of(
                new EventEntry("app_open", Map.of(), 1L),
                new EventEntry("app_open", Map.of(), 2L),
                new EventEntry("app_open", Map.of(), 3L)));

        ArgumentCaptor<List<AnalyticsEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void dropsOversizedProps() {
        String big = "x".repeat(5000);
        service.record(1L, null, null, List.of(new EventEntry("cta_click", Map.of("data", big), 1L)));

        ArgumentCaptor<List<AnalyticsEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertNull(captor.getValue().get(0).getProps());
    }

    @Test
    void ignoresEmptyBatch() {
        service.record(1L, null, null, List.of());
        verify(repository, never()).saveAll(any());
    }
}
