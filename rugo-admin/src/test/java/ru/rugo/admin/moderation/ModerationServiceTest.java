package ru.rugo.admin.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.admin.catalog.CatalogCacheEvictor;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.repository.WorkshopRepository;
import ru.rugo.core.service.PartnerAccountService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private PartnerLeadRepository leadRepository;
    @Mock
    private PartnerAccountService partnerAccountService;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private AdminMessenger messenger;
    @Mock
    private CatalogCacheEvictor cacheEvictor;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(leadRepository, partnerAccountService, equipmentRepository,
                materialRepository, workshopRepository, new LeadToCardMapper(), messenger, cacheEvictor);
    }

    private PartnerLead equipmentLead() {
        return PartnerLead.builder()
                .id(10L)
                .name("Иван")
                .phone("+79001234567")
                .offerTypes(List.of("equipment"))
                .district("Левобережный")
                .status(PartnerLead.STATUS_NEW)
                .details(Map.of("category", "Экскаватор", "brand", "JCB", "model", "3CX",
                        "capacity", 8, "price", 2800, "priceUnit", "hour"))
                .build();
    }

    @Test
    void approveCreatesCardAndMarksAccepted() {
        PartnerLead lead = equipmentLead();
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));
        when(partnerAccountService.resolveOrCreate(any(), any(), any(), any(), any()))
                .thenReturn(User.builder().id(1L).build());
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(invocation -> {
            Equipment equipment = invocation.getArgument(0);
            equipment.setId(5L);
            return equipment;
        });

        String result = service.approve(10L);

        assertEquals("equipment #5", result);
        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(leadRepository).save(captor.capture());
        assertEquals(PartnerLead.STATUS_ACCEPTED, captor.getValue().getStatus());
        verify(messenger).notifyPartner(any(), anyString());
    }

    @Test
    void approveCreatesMaterialForIndividual() {
        PartnerLead lead = PartnerLead.builder()
                .id(20L)
                .name("Пётр")
                .phone("+79005550000")
                .offerTypes(List.of("materials"))
                .partnerType("individual")
                .district("Советский")
                .status(PartnerLead.STATUS_NEW)
                .details(Map.of("category", "Чернозём", "price", 500, "unit", "м³",
                        "deliveryType", "DELIVERY"))
                .build();
        when(leadRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(lead));
        User supplier = User.builder().id(7L).partnerType("individual").build();
        when(partnerAccountService.resolveOrCreate(any(), any(), any(), any(), any()))
                .thenReturn(supplier);
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            material.setId(9L);
            return material;
        });

        String result = service.approve(20L);

        assertEquals("materials #9", result);
        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());
        Material saved = captor.getValue();
        assertEquals("Чернозём", saved.getType());
        assertEquals("м³", saved.getUnit());
        assertEquals(7L, saved.getSupplier().getId());
        assertEquals(Material.STATUS_APPROVED, saved.getStatus());
    }

    @Test
    void approveAppliesMaterialUpdate() {
        PartnerLead lead = PartnerLead.builder()
                .id(30L)
                .name("Пётр")
                .phone("+79005550000")
                .maxUserId(67890L)
                .offerTypes(List.of("materials"))
                .partnerType("individual")
                .district("Советский")
                .status(PartnerLead.STATUS_NEW)
                .details(Map.of("action", "update", "materialId", 9, "category", "Чернозём",
                        "price", 600, "unit", "м³", "deliveryType", "DELIVERY"))
                .build();
        when(leadRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(lead));
        Material material = Material.builder()
                .id(9L)
                .supplier(User.builder().id(7L).maxId(67890L).build())
                .build();
        when(materialRepository.findById(9L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = service.approve(30L);

        assertTrue(result.contains("обновлён"));
        assertEquals("Чернозём", material.getType());
        assertEquals(Material.STATUS_APPROVED, material.getStatus());
        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(leadRepository).save(captor.capture());
        assertEquals(PartnerLead.STATUS_ACCEPTED, captor.getValue().getStatus());
    }

    @Test
    void approveAppliesEquipmentUpdate() {
        PartnerLead lead = PartnerLead.builder()
                .id(32L)
                .name("Иван")
                .phone("+79001234567")
                .maxUserId(111L)
                .offerTypes(List.of("equipment"))
                .partnerType("individual")
                .district("Советский")
                .status(PartnerLead.STATUS_NEW)
                .details(Map.of("action", "update", "equipmentId", 5, "category", "Экскаватор",
                        "brand", "JCB", "model", "3CX", "capacity", 8, "price", 3000, "priceUnit", "hour"))
                .build();
        when(leadRepository.findByIdForUpdate(32L)).thenReturn(Optional.of(lead));
        Equipment equipment = Equipment.builder()
                .id(5L)
                .owner(User.builder().id(7L).maxId(111L).build())
                .build();
        when(equipmentRepository.findById(5L)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = service.approve(32L);

        assertTrue(result.contains("обновлён"));
        assertEquals("JCB 3CX", equipment.getType());
        assertEquals(EquipmentStatus.AVAILABLE, equipment.getStatus());
    }

    @Test
    void approveSkipsAlreadyProcessed() {
        PartnerLead lead = equipmentLead();
        lead.setStatus(PartnerLead.STATUS_ACCEPTED);
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));

        String result = service.approve(10L);

        assertTrue(result.contains("уже обработана"));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void rejectMarksRejected() {
        PartnerLead lead = equipmentLead();
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));

        service.reject(10L);

        ArgumentCaptor<PartnerLead> captor = ArgumentCaptor.forClass(PartnerLead.class);
        verify(leadRepository).save(captor.capture());
        assertEquals(PartnerLead.STATUS_REJECTED, captor.getValue().getStatus());
    }

    @Test
    void contactInfoContainsNameAndPhone() {
        PartnerLead lead = equipmentLead();
        when(leadRepository.findById(10L)).thenReturn(Optional.of(lead));

        String info = service.contactInfo(10L);

        assertTrue(info.contains("Иван"));
        assertTrue(info.contains("+79001234567"));
    }

    @Test
    void approveUnknownIdReturnsNotFound() {
        when(leadRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertTrue(service.approve(404L).contains("не найдена"));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void rejectAlreadyProcessedDoesNotChangeStatus() {
        PartnerLead lead = equipmentLead();
        lead.setStatus(PartnerLead.STATUS_REJECTED);
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));

        assertTrue(service.reject(10L).contains("уже обработана"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void reopenReturnsLeadToNewAndClearsDecision() {
        PartnerLead lead = equipmentLead();
        lead.setStatus(PartnerLead.STATUS_ACCEPTED);
        lead.setDecidedAt(java.time.OffsetDateTime.now());
        lead.setNotifiedAt(java.time.OffsetDateTime.now());
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));

        assertTrue(service.reopen(10L).contains("возвращена"));
        assertEquals(PartnerLead.STATUS_NEW, lead.getStatus());
        assertTrue(lead.getDecidedAt() == null && lead.getNotifiedAt() == null);
    }

    @Test
    void reopenAlreadyNewIsNoop() {
        PartnerLead lead = equipmentLead();
        when(leadRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lead));

        assertTrue(service.reopen(10L).contains("уже в новых"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void contactUnknownIdReturnsNotFound() {
        when(leadRepository.findById(404L)).thenReturn(Optional.empty());

        assertTrue(service.contactInfo(404L).contains("не найдена"));
    }
}
