package ru.rugo.app.web;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.app.service.PartnerCabinetService;
import ru.rugo.app.service.PartnerEquipmentService;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.User;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.security.RequestContactValidator;
import ru.rugo.core.service.PartnerAccountService;
import ru.rugo.core.service.PartnerLeadService;
import ru.rugo.core.service.model.CreatePartnerLeadCommand;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartnerControllerTest {

    private static final String VALID_BODY = """
            {"phone":"+79001234567","contactAuthDate":"1771409719",
             "contactHash":"5aab6c8d133aa5e7072b67cc0615d9af7c8c492edeb71e3a93f8b131266c252e"}
            """;

    @Mock
    private MaxInitDataValidator validator;

    @Mock
    private InitDataUserExtractor userExtractor;

    @Mock
    private PartnerAccountService partnerAccountService;

    @Mock
    private PartnerEquipmentService equipmentService;

    @Mock
    private PartnerCabinetService cabinetService;

    @Mock
    private PartnerLeadService partnerLeadService;

    @Mock
    private AppProperties properties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PartnerController controller = new PartnerController(
                validator, userExtractor, new RequestContactValidator("123456:TESTTOKEN"),
                partnerAccountService, equipmentService, cabinetService, partnerLeadService,
                new FixedWindowRateLimiter(), new ClientIpResolver(properties), properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .build();

        lenient().when(properties.contactSignatureRequired()).thenReturn(true);
        lenient().when(properties.rateLimit())
                .thenReturn(new AppProperties.RateLimit(100, 3600, 100, 3600, 60));
    }

    private void signedInitData() {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(userExtractor.extractUserId(any())).thenReturn(67890L);
    }

    @Test
    void acceptsValidSignatureRegardlessOfAuthDateAge() throws Exception {
        signedInitData();
        when(partnerAccountService.resolveOrCreateForCabinet(any(), any(), any()))
                .thenReturn(User.builder().id(1L).phone("+79001234567").name("Иван").maxId(67890L).build());

        mockMvc.perform(post("/api/partner/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.phone").value("+79001234567"));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        signedInitData();

        String body = """
                {"phone":"+79001234567","contactAuthDate":"1771409719","contactHash":"bad"}
                """;

        mockMvc.perform(post("/api/partner/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());

        verify(partnerAccountService, never()).resolveOrCreateForCabinet(any(), any(), any());
    }

    @Test
    void returnsPhoneConflictFromService() throws Exception {
        signedInitData();
        String conflict = "Этот номер уже привязан к другому аккаунту MAX. Обратитесь в поддержку";
        when(partnerAccountService.resolveOrCreateForCabinet(any(), any(), any()))
                .thenThrow(new ApiValidationException(Map.of("phone", conflict)));

        mockMvc.perform(post("/api/partner/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value(conflict));
    }

    @Test
    void createsMaterialFromCabinet() throws Exception {
        signedInitData();
        when(partnerAccountService.findByMaxId(67890L))
                .thenReturn(User.builder().id(1L).name("Иван").phone("+79001234567")
                        .partnerType("individual").maxId(67890L).build());
        when(partnerLeadService.createCabinetLead(any())).thenReturn(55L);

        mockMvc.perform(post("/api/partner/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content("{\"category\":\"Дрова\",\"price\":2000,\"unit\":\"м³\",\"deliveryType\":\"DELIVERY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55));

        verify(partnerLeadService).createCabinetLead(any());
    }

    @Test
    void deletesOwnMaterial() throws Exception {
        signedInitData();
        when(partnerAccountService.findByMaxId(67890L))
                .thenReturn(User.builder().id(1L).maxId(67890L).build());

        mockMvc.perform(delete("/api/partner/materials/7").header("X-Init-Data", "signed"))
                .andExpect(status().isNoContent());

        verify(cabinetService).deleteOwnedMaterial(1L, 7L);
    }

    @Test
    void createsEquipmentForExistingCompanyWithStoredName() throws Exception {
        signedInitData();
        when(partnerAccountService.findByMaxId(67890L))
                .thenReturn(User.builder().id(1L).name("ООО Ромашка").phone("+79001234567")
                        .partnerType("company").contactName("Иван").maxId(67890L).build());
        when(partnerLeadService.createCabinetLead(any())).thenReturn(70L);

        mockMvc.perform(post("/api/partner/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content("{\"category\":\"Самосвал\",\"capacity\":10,\"price\":2000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(70));

        ArgumentCaptor<CreatePartnerLeadCommand> captor =
                ArgumentCaptor.forClass(CreatePartnerLeadCommand.class);
        verify(partnerLeadService).createCabinetLead(captor.capture());
        assertEquals("company", captor.getValue().partnerType());
        assertEquals("ООО Ромашка", captor.getValue().companyName());
        assertEquals("Иван", captor.getValue().contactName());
    }

    @Test
    void acceptsCompanyFieldsFromCabinetForm() throws Exception {
        signedInitData();
        when(partnerAccountService.findByMaxId(67890L))
                .thenReturn(User.builder().id(1L).name("Иван").phone("+79001234567")
                        .partnerType(null).maxId(67890L).build());
        when(partnerLeadService.createCabinetLead(any())).thenReturn(71L);

        mockMvc.perform(post("/api/partner/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Init-Data", "signed")
                        .content("{\"category\":\"Самосвал\",\"capacity\":10,\"price\":2000,"
                                + "\"partnerType\":\"company\",\"companyName\":\"ООО Ромашка\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(71));

        ArgumentCaptor<CreatePartnerLeadCommand> captor =
                ArgumentCaptor.forClass(CreatePartnerLeadCommand.class);
        verify(partnerLeadService).createCabinetLead(captor.capture());
        assertEquals("company", captor.getValue().partnerType());
        assertEquals("ООО Ромашка", captor.getValue().companyName());
    }
}
