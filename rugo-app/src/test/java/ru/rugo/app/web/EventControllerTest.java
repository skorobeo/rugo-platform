package ru.rugo.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.service.AnalyticsEventService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private static final String BODY = """
            {"events":[{"name":"app_open","props":{},"ts":1}],"sessionId":"sess","source":"m_ivan"}
            """;

    @Mock
    private AnalyticsEventService service;

    @Mock
    private MaxInitDataValidator validator;

    @Mock
    private InitDataUserExtractor userExtractor;

    @Mock
    private AppProperties properties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EventController controller = new EventController(
                service, validator, userExtractor, new FixedWindowRateLimiter(), new ClientIpResolver(properties),
                properties, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        lenient().when(properties.eventsLimit())
                .thenReturn(new AppProperties.EventsLimit(50, 32768, 4096, 120, 60));
        lenient().when(validator.validate(any())).thenReturn(Optional.empty());
    }

    @Test
    void acceptsValidBatchWith202() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted());

        verify(service).record(any(), any(), any(), any());
    }

    @Test
    void returns202OnMalformedJson() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isAccepted());

        verify(service, never()).record(any(), any(), any(), any());
    }

    @Test
    void returns202OnEmptyBody() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    void returns202AndDropsOversizedBody() throws Exception {
        when(properties.eventsLimit())
                .thenReturn(new AppProperties.EventsLimit(50, 10, 4096, 120, 60));
        String body = "{\"events\":[{\"name\":\"app_open\",\"props\":{}}]}";

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

        verify(service, never()).record(any(), any(), any(), any());
    }

    @Test
    void returns202WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("db down")).when(service).record(any(), any(), any(), any());

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted());
    }

    @Test
    void returns202WhenRateLimited() throws Exception {
        when(properties.eventsLimit())
                .thenReturn(new AppProperties.EventsLimit(50, 32768, 4096, 0, 60));

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted());

        verify(service, never()).record(any(), any(), any(), any());
    }

    @Test
    void passesParsedEventsToService() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted());

        verify(service).record(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(List.of(
                        new ru.rugo.core.service.model.EventEntry("app_open", Map.of(), 1L))));
    }
}
