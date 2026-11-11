package ru.rugo.admin.moderation;

import org.junit.jupiter.api.Test;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.entity.Workshop;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeadToCardMapperTest {

    private final LeadToCardMapper mapper = new LeadToCardMapper();

    private final User owner = User.builder().id(1L).name("Иван").phone("+79001234567").build();

    private PartnerLead lead(String type, String district, Map<String, Object> details) {
        return PartnerLead.builder()
                .id(10L)
                .name("Иван")
                .phone("+79001234567")
                .offerTypes(List.of(type))
                .district(district)
                .details(details)
                .build();
    }

    @Test
    void mapsEquipment() {
        PartnerLead lead = lead("equipment", "Левобережный", Map.of(
                "category", "Экскаватор", "brand", "JCB", "model", "3CX",
                "capacity", 8, "price", 2800, "priceUnit", "hour",
                "photos", List.of("/uploads/equipment/a.jpg", "/uploads/equipment/b.jpg")));

        Equipment equipment = mapper.toEquipment(lead, owner);

        assertEquals("Экскаватор", equipment.getCategory());
        assertEquals("JCB 3CX", equipment.getType());
        assertEquals("8 тонн", equipment.getCapacity());
        assertEquals(2800, equipment.getPrice());
        assertEquals("HOUR", equipment.getPriceType());
        assertEquals("Воронеж, Левобережный", equipment.getLocation());
        assertEquals("/uploads/equipment/a.jpg", equipment.getImageUrl());
        assertEquals(2, equipment.getImageUrls().size());
    }

    @Test
    void mapsMaterial() {
        PartnerLead lead = lead("materials", "Центральный", Map.of(
                "category", "Щебень гранитный", "fraction", "5-20", "price", 720,
                "unit", "тонна", "deliveryType", "DELIVERY",
                "photos", List.of("/uploads/materials/pesok.jpg")));

        Material material = mapper.toMaterial(lead, owner);

        assertEquals("Щебень гранитный", material.getType());
        assertEquals("5-20", material.getFraction());
        assertEquals(720, material.getPricePerUnit());
        assertEquals("тонна", material.getUnit());
        assertEquals("DELIVERY", material.getDeliveryType());
        assertEquals("Воронеж, Центральный", material.getLocation());
        assertEquals(Material.STATUS_APPROVED, material.getStatus());
        assertEquals("/uploads/materials/pesok.jpg", material.getImageUrl());
    }

    @Test
    void mapsWorkshop() {
        PartnerLead lead = lead("repair", "Левобережный", Map.of(
                "company", "ООО «Ромашка»", "city", "Воронеж", "specialization", "Ремонт гидравлики"));

        Workshop workshop = mapper.toWorkshop(lead);

        assertEquals("ООО «Ромашка»", workshop.getName());
        assertEquals("Ремонт гидравлики", workshop.getSpecialization());
        assertEquals("Воронеж, Левобережный", workshop.getLocation());
        assertEquals("+79001234567", workshop.getPhone());
    }

    @Test
    void workshopHasDefaultSpecialization() {
        PartnerLead lead = lead("repair", null, Map.of("company", "Мастер"));
        Workshop workshop = mapper.toWorkshop(lead);
        assertEquals("Ремонт спецтехники", workshop.getSpecialization());
    }
}
