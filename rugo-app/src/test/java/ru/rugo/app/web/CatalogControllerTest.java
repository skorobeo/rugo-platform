package ru.rugo.app.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.service.EquipmentService;
import ru.rugo.core.service.MaterialService;
import ru.rugo.core.service.WorkshopService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private EquipmentService equipmentService;

    @Mock
    private MaterialService materialService;

    @Mock
    private WorkshopService workshopService;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private MaterialRepository materialRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CatalogController controller = new CatalogController(
                equipmentService, materialService, workshopService, equipmentRepository, materialRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsCategoryCountsSorted() throws Exception {
        when(equipmentRepository.countsByCategory(any(), any()))
                .thenReturn(List.of(new Object[]{"Самосвал", 3L}, new Object[]{"Экскаватор", 1L}));

        mockMvc.perform(get("/api/equipment/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("Самосвал"))
                .andExpect(jsonPath("$[0].count").value(3));
    }

    @Test
    void returnsEquipmentWithOwnerContact() throws Exception {
        User owner = User.builder().name("Владелец №1").phone("+79000000001").build();
        Equipment equipment = Equipment.builder()
                .id(5L)
                .category("Самосвал")
                .type("КамАЗ-55111")
                .capacity("13 т")
                .price(1800)
                .priceType("HOUR")
                .location("Воронеж")
                .owner(owner)
                .build();
        when(equipmentService.findAvailableByCategoryWithOwner("Самосвал", "Воронеж"))
                .thenReturn(List.of(equipment));

        mockMvc.perform(get("/api/equipment").param("category", "Самосвал"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].type").value("КамАЗ-55111"))
                .andExpect(jsonPath("$[0].ownerName").value("Владелец №1"))
                .andExpect(jsonPath("$[0].ownerPhone").value("+79000000001"));
    }

    @Test
    void returnsMaterialsWithSupplierContact() throws Exception {
        User supplier = User.builder().name("Поставщик №1").phone("+79000000002").build();
        Material material = Material.builder()
                .id(9L)
                .type("Щебень гранитный")
                .fraction("5-20")
                .pricePerUnit(720)
                .unit("тонна")
                .available(true)
                .deliveryType(Material.DELIVERY_TYPE_DELIVERY)
                .supplier(supplier)
                .build();
        when(materialService.findApprovedAvailableByTypeWithSupplier("Щебень гранитный 5-20"))
                .thenReturn(List.of(material));

        mockMvc.perform(get("/api/materials").param("type", "Щебень гранитный 5-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Щебень гранитный"))
                .andExpect(jsonPath("$[0].pricePerUnit").value(720))
                .andExpect(jsonPath("$[0].supplierName").value("Поставщик №1"))
                .andExpect(jsonPath("$[0].supplierPhone").value("+79000000002"));
    }

    @Test
    void returnsApprovedWorkshops() throws Exception {
        Workshop workshop = Workshop.builder()
                .id(3L)
                .name("Ремонт-Воронеж")
                .specialization("Ремонт гидравлики")
                .location("Воронеж")
                .phone("+79000000003")
                .build();
        when(workshopService.findApproved()).thenReturn(List.of(workshop));

        mockMvc.perform(get("/api/workshops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ремонт-Воронеж"))
                .andExpect(jsonPath("$[0].phone").value("+79000000003"));
    }

    @Test
    void returns400WhenCategoryMissing() throws Exception {
        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isBadRequest());
    }
}
