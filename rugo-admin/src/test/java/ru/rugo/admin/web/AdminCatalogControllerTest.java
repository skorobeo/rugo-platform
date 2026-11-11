package ru.rugo.admin.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.rugo.core.repository.EquipmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ручное добавление техники из веб-админки (фильтр авторизации отключён). */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private static final String VALID = """
            {
              "category": "Экскаватор",
              "brand": "Hitachi",
              "model": "ZX200",
              "capacity": 8,
              "price": 3000,
              "priceUnit": "hour",
              "district": "Центральный",
              "imageUrls": ["/uploads/equipment/ekskavator.png"]
            }
            """;

    @Test
    void createsEquipment() throws Exception {
        long before = equipmentRepository.count();

        mockMvc.perform(post("/admin/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        assertThat(equipmentRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        mockMvc.perform(post("/admin/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "Абракадабра", "brand": "X", "capacity": 8, "price": 100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists());
    }

    @Test
    void rejectsScriptInBrand() throws Exception {
        mockMvc.perform(post("/admin/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "Экскаватор", "brand": "<script>alert(1)</script>", "capacity": 8, "price": 100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.brand").exists());
    }

    @Test
    void rejectsNonPositivePrice() throws Exception {
        mockMvc.perform(post("/admin/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "Экскаватор", "brand": "X", "capacity": 8, "price": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void rejectsTooLongBrandByBeanValidation() throws Exception {
        String longBrand = "X".repeat(101);

        mockMvc.perform(post("/admin/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "Экскаватор", "brand": "%s", "capacity": 8, "price": 100}
                                """.formatted(longBrand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.brand").exists());
    }
}
