package ru.rugo.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.DuplicateLeadException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.service.model.CreatePartnerLeadCommand;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerLeadServiceImplTest {

    @Mock
    private PartnerLeadRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private AppProperties properties;

    @Mock

    private PartnerLeadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PartnerLeadServiceImpl(repository, materialRepository, equipmentRepository, properties);
        lenient().when(properties.rateLimit())
                .thenReturn(new AppProperties.RateLimit(10, 3600, 5, 3600, 60));
    }

    private CreatePartnerLeadCommand command(Map<String, Object> details, boolean consent, List<String> offerTypes) {
        return new CreatePartnerLeadCommand(42L, "Иван", "+7 (900) 123-45-67",
                offerTypes, "company", "ООО Тест", null, "Левобережный", details, "m_ivan", consent);
    }

    @Test
    void savesValidLeadWithNormalizedPhone() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> {
            PartnerLead lead = invocation.getArgument(0);
            lead.setId(5L);
            return lead;
        });

        Long id = service.createLead(command(Map.of("category", "Экскаватор"), true, List.of("equipment")));

        assertEquals(5L, id);
        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        PartnerLead saved = captor.getValue();
        assertEquals("+79001234567", saved.getPhone());
        assertEquals(42L, saved.getMaxUserId());
        assertEquals("m_ivan", saved.getSource());
        assertEquals("Экскаватор", saved.getDetails().get("category"));
    }

    @Test
    void rejectsInvalidPhone() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "123",
                        List.of("equipment"), "individual", null, null, null, Map.of(), null, true)));

        assertTrue(e.getErrors().containsKey("phone"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingConsent() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of(), false, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("consent"));
    }

    @Test
    void rejectsUnknownOfferType() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of(), true, List.of("spaceships"))));

        assertTrue(e.getErrors().containsKey("offerTypes"));
    }

    @Test
    void rejectsInvalidPrice() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("price", -5), true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.price"));
    }

    @Test
    void rejectsDuplicatePhoneWithinWindow() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(true);

        assertThrows(DuplicateLeadException.class,
                () -> service.createLead(command(Map.of(), true, List.of("equipment"))));
        verify(repository, never()).save(any());
    }

    @Test
    void storesExtendedDetails() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(command(
                Map.of("category", "Экскаватор", "brand", "JCB", "model", "3CX",
                        "capacity", 8, "price", 2800, "priceUnit", "hour"),
                true, List.of("equipment")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("JCB", saved.get("brand"));
        assertEquals(8, saved.get("capacity"));
        assertEquals(2800, saved.get("price"));
    }

    @Test
    void rejectsInvalidMaterialUnit() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("unit", "мешок"), true, List.of("materials"))));

        assertTrue(e.getErrors().containsKey("details.unit"));
    }

    @Test
    void storesNullDetailsWhenEmpty() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(command(Map.of(), true, List.of("equipment")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getDetails());
    }

    @Test
    void rejectsUnknownDistrict() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "+79001234567",
                        List.of("equipment"), "individual", null, null, "Москва", Map.of(), null, true)));

        assertTrue(e.getErrors().containsKey("district"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnsafeDetailText() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("brand", "<script>alert(1)</script>"),
                        true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.brand"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsQuotesAmpersandAndBackslashInDetailText() {
        for (String unsafe : List.of("\"onmouseover=x", "a&b", "a\\b", "a`b", "a{b}")) {
            ApiValidationException e = assertThrows(ApiValidationException.class,
                    () -> service.createLead(command(Map.of("model", unsafe), true, List.of("equipment"))),
                    "ожидался отказ для: " + unsafe);
            assertTrue(e.getErrors().containsKey("details.model"));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsCyrillicTextAndNumericCapacity() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(command(
                Map.of("category", "Экскаватор", "brand", "КамАЗ", "model", "65115", "capacity", 8),
                true, List.of("equipment")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("КамАЗ", saved.get("brand"));
        assertEquals(8, saved.get("capacity"));
    }

    @Test
    void rejectsNonPositiveCapacity() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("capacity", 0), true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.capacity"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownCategory() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("category", "Абракадабра"), true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.category"));
        verify(repository, never()).save(any());
    }

    @Test
    void storesMaterialCategoryAndFraction() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(command(
                Map.of("category", "Щебень гранитный", "fraction", "5-20", "price", 720,
                        "unit", "тонна", "deliveryType", "DELIVERY"),
                true, List.of("materials")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("Щебень гранитный", saved.get("category"));
        assertEquals("5-20", saved.get("fraction"));
    }

    @Test
    void rejectsTooManyPhotos() {
        List<String> photos = List.of("/uploads/equipment/a.jpg", "/uploads/equipment/b.jpg",
                "/uploads/equipment/c.jpg", "/uploads/equipment/d.jpg");
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("category", "Экскаватор", "photos", photos),
                        true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.photos"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnsafePhotoPath() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("category", "Экскаватор",
                        "photos", List.of("http://evil.example/x.jpg")), true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.photos"));
        verify(repository, never()).save(any());
    }

    @Test
    void storesPhotos() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> photos = List.of("/uploads/equipment/a.jpg", "/uploads/equipment/b.jpg");
        service.createLead(command(Map.of("category", "Экскаватор", "photos", photos),
                true, List.of("equipment")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        assertEquals(photos, captor.getValue().getDetails().get("photos"));
    }

    @Test
    void rejectsRepairWithoutCompanyAndCity() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "+79001234567",
                        List.of("repair"), "company", null, null, "Левобережный", Map.of(), null, true)));

        assertTrue(e.getErrors().containsKey("details.company"));
        assertTrue(e.getErrors().containsKey("details.city"));
        verify(repository, never()).save(any());
    }

    @Test
    void storesRepairCompanyAndCity() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "+79001234567",
                List.of("repair"), "company", "ООО «Ромашка»", null, "Левобережный",
                Map.of("company", "ООО «Ромашка»", "city", "Воронеж", "specialization", "Ремонт гидравлики"),
                null, true));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("ООО «Ромашка»", saved.get("company"));
        assertEquals("Воронеж", saved.get("city"));
        assertEquals("Ремонт гидравлики", saved.get("specialization"));
    }

    @Test
    void storesParkLeadAsCompany() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(new CreatePartnerLeadCommand(1L, null, "+79001234567",
                List.of("park"), "company", "ООО Парк", null, null,
                Map.of("units", 20, "equipment", "самосвалы, экскаваторы"), null, true));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        PartnerLead saved = captor.getValue();
        assertEquals("company", saved.getPartnerType());
        assertEquals("ООО Парк", saved.getCompanyName());
        assertEquals("ООО Парк", saved.getName());
        assertEquals(20, saved.getDetails().get("units"));
        assertEquals("самосвалы, экскаваторы", saved.getDetails().get("equipment"));
    }

    @Test
    void storesMaterialsForIndividual() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "+79001234567",
                List.of("materials"), "individual", null, null, null,
                Map.of("category", "Песок речной", "price", 500), null, true));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        PartnerLead saved = captor.getValue();
        assertEquals("individual", saved.getPartnerType());
        assertNull(saved.getCompanyName());
        assertEquals("Иван", saved.getName());
    }

    @Test
    void acceptsMaterialChangeForOwnMaterial() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Material material = Material.builder()
                .id(1L)
                .supplier(User.builder().maxId(42L).build())
                .build();
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        service.createLead(new CreatePartnerLeadCommand(42L, "Иван", "+79001234567",
                List.of("materials"), "individual", null, null, "Левобережный",
                Map.of("action", "update", "materialId", 1, "category", "Песок речной",
                        "price", 500, "unit", "тонна", "deliveryType", "DELIVERY"),
                null, true));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("update", saved.get("action"));
        assertEquals(1, saved.get("materialId"));
    }

    @Test
    void rejectsMaterialChangeForForeignMaterial() {
        Material material = Material.builder()
                .id(1L)
                .supplier(User.builder().maxId(99L).build())
                .build();
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(new CreatePartnerLeadCommand(42L, "Иван", "+79001234567",
                        List.of("materials"), "individual", null, null, null,
                        Map.of("action", "update", "materialId", 1), null, true)));

        assertTrue(e.getErrors().containsKey("details.materialId"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsCompanyWithoutCompanyName() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(new CreatePartnerLeadCommand(1L, "Иван", "+79001234567",
                        List.of("equipment"), "company", null, null, null,
                        Map.of("category", "Экскаватор"), null, true)));

        assertTrue(e.getErrors().containsKey("companyName"));
        verify(repository, never()).save(any());
    }

    @Test
    void storesNeedForNoMatch() {
        when(repository.existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLead(command(Map.of("noMatch", true, "need", "Нужен автокран 25 т"),
                true, List.of("equipment")));

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(repository).save(captor.capture());
        Map<String, Object> saved = captor.getValue().getDetails();
        assertEquals("Нужен автокран 25 т", saved.get("need"));
        assertEquals(Boolean.TRUE, saved.get("noMatch"));
    }

    @Test
    void rejectsNoMatchWithoutNeed() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.createLead(command(Map.of("noMatch", true), true, List.of("equipment"))));

        assertTrue(e.getErrors().containsKey("details.need"));
        verify(repository, never()).save(any());
    }

    @Test
    void cabinetLeadSkipsPhoneDeduplication() {
        when(repository.save(any(PartnerLead.class))).thenAnswer(invocation -> {
            PartnerLead lead = invocation.getArgument(0);
            lead.setId(100L);
            return lead;
        });

        service.createCabinetLead(command(Map.of("category", "Экскаватор"), true, List.of("equipment")));
        service.createCabinetLead(command(Map.of("category", "Дрова", "price", 2000), true, List.of("materials")));
        service.createCabinetLead(command(Map.of("category", "Самосвал"), true, List.of("equipment")));

        verify(repository, never())
                .existsByPhoneAndCreatedAtGreaterThanEqual(anyString(), any(OffsetDateTime.class));
        verify(repository, org.mockito.Mockito.times(3)).save(any(PartnerLead.class));
    }
}
