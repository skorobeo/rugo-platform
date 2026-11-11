package ru.rugo.app.web;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.DuplicateLeadException;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.security.RequestContactValidator;
import ru.rugo.core.service.PartnerLeadService;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartnerLeadControllerTest {

    private static final String BODY = """
            {"name":"Иван","phone":"+79001234567","offerTypes":["equipment"],
             "consent":true,"source":"m_ivan"}
            """;

    @Mock
    private PartnerLeadService service;

    @Mock
    private MaxInitDataValidator validator;

    @Mock
    private InitDataUserExtractor userExtractor;

    @Mock
    private AppProperties properties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        buildMvc(new RequestContactValidator(""));

        lenient().when(properties.allowAnonLeads()).thenReturn(false);
        lenient().when(properties.contactSignatureRequired()).thenReturn(true);
        lenient().when(properties.rateLimit())
                .thenReturn(new AppProperties.RateLimit(100, 3600, 100, 3600, 60));
    }

    private void buildMvc(RequestContactValidator contactValidator) {
        PartnerLeadController controller = new PartnerLeadController(
                service, validator, userExtractor, contactValidator,
                new FixedWindowRateLimiter(), new ClientIpResolver(properties), properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @Test
    void returns201WithId() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(service.createLead(any())).thenReturn(7L);

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void returns401WhenInitDataInvalidAndAnonDisabled() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.initData").exists());

        verify(service, never()).createLead(any());
    }

    @Test
    void allowsAnonWhenFlagEnabled() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.empty());
        when(properties.allowAnonLeads()).thenReturn(true);
        when(service.createLead(any())).thenReturn(8L);

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    void honeypotReturns201WithoutSaving() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        String body = BODY.replace("}", ",\"website\":\"spam\"}");

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(body))
                .andExpect(status().isCreated());

        verify(service, never()).createLead(any());
    }

    @Test
    void returns400OnValidationError() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(service.createLead(any()))
                .thenThrow(new ApiValidationException(Map.of("phone", "Неверный номер")));

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Неверный номер"));
    }

    @Test
    void returns409OnDuplicate() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(service.createLead(any())).thenThrow(new DuplicateLeadException());

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void returns429WhenRateLimited() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(properties.rateLimit()).thenReturn(new AppProperties.RateLimit(0, 3600, 100, 3600, 60));

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void returns400OnUnreadableBody() throws Exception {
        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenContactSignatureMissingWithToken() throws Exception {
        buildMvc(new RequestContactValidator("123456:TESTTOKEN"));
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(userExtractor.extractUserId(any())).thenReturn(67890L);

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());

        verify(service, never()).createLead(any());
    }

    @Test
    void returns400WhenNameTooLongByBeanValidation() throws Exception {
        String longName = "И".repeat(61);
        String body = BODY.replace("\"Иван\"", "\"" + longName + "\"");

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(service, never()).createLead(any());
    }

    @Test
    void returns201WhenContactSignatureValid() throws Exception {
        buildMvc(new RequestContactValidator("123456:TESTTOKEN"));
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(userExtractor.extractUserId(any())).thenReturn(67890L);
        when(service.createLead(any())).thenReturn(9L);

        String body = BODY.replace("}",
                ",\"contactAuthDate\":\"1771409719\","
                        + "\"contactHash\":\"5aab6c8d133aa5e7072b67cc0615d9af7c8c492edeb71e3a93f8b131266c252e\"}");

        mockMvc.perform(post("/api/partner-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }
}
