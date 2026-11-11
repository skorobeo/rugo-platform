package ru.rugo.app.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import jakarta.persistence.EntityManager;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EquipmentRepositoryTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .phone("+79000000001")
                .name("Иван")
                .role("PROVIDER")
                .build());
    }

    @Test
    void findByCategoryWithOwner_filtersByCategory() {
        saveEquipment("Самосвал", "КамАЗ-55111", "Воронеж", 1800, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));
        saveEquipment("Экскаватор", "ЭО-2621", "Воронеж", 1600, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));

        List<Equipment> result = equipmentRepository.findByCategoryWithOwner("Самосвал", "Воронеж");

        assertEquals(1, result.size());
        assertEquals("Самосвал", result.get(0).getCategory());
    }

    @Test
    void findByCategoryWithOwner_loadsOwner() {
        saveEquipment("Самосвал", "КамАЗ-55111", "Воронеж", 1800, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));

        Equipment result = equipmentRepository.findByCategoryWithOwner("Самосвал", "Воронеж").get(0);

        assertNotNull(result.getOwner());
        assertEquals("Иван", result.getOwner().getName());
        assertEquals("+79000000001", result.getOwner().getPhone());
    }

    @Test
    void findByCategoryWithOwner_filtersByLocation() {
        saveEquipment("Самосвал", "КамАЗ-55111", "Воронеж", 1800, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));
        saveEquipment("Самосвал", "КамАЗ-65115", "Липецк", 2000, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));

        List<Equipment> result = equipmentRepository.findByCategoryWithOwner("Самосвал", "Воронеж");

        assertEquals(1, result.size());
        assertEquals("КамАЗ-55111", result.get(0).getType());
    }

    @Test
    void findByStatusOrderByReliabilityDescPriceAsc_sortsAndFilters() {
        saveEquipment("Самосвал", "A", "Воронеж", 2000, EquipmentStatus.AVAILABLE, new BigDecimal("95.0"));
        saveEquipment("Самосвал", "B", "Воронеж", 3000, EquipmentStatus.AVAILABLE, new BigDecimal("99.0"));
        saveEquipment("Самосвал", "C", "Воронеж", 1500, EquipmentStatus.AVAILABLE, new BigDecimal("99.0"));
        saveEquipment("Самосвал", "D", "Воронеж", 1000, EquipmentStatus.BUSY, new BigDecimal("99.9"));

        List<Equipment> result = equipmentRepository.findByStatusOrderByReliabilityDescPriceAsc(EquipmentStatus.AVAILABLE);

        assertEquals(List.of("C", "B", "A"), result.stream().map(Equipment::getType).toList());
    }

    @Test
    void findByOwnerIdAndStatusNot_loadsOwnerAfterSessionClosed() {
        saveEquipment("Самосвал", "КамАЗ-55111", "Воронеж", 1800, EquipmentStatus.PENDING, new BigDecimal("95.0"));

        List<Equipment> result = equipmentRepository
                .findByOwnerIdAndStatusNotOrderByCreatedAtDesc(owner.getId(), EquipmentStatus.HIDDEN);

        entityManager.clear();

        assertEquals(1, result.size());
        assertEquals("Иван", result.get(0).getOwner().getName());
    }

    @Test
    void findByIdAndOwnerId_loadsOwnerAfterSessionClosed() {
        Equipment saved = saveEquipment("Самосвал", "КамАЗ-55111", "Воронеж", 1800,
                EquipmentStatus.PENDING, new BigDecimal("95.0"));

        Equipment loaded = equipmentRepository.findByIdAndOwnerId(saved.getId(), owner.getId()).orElseThrow();

        entityManager.clear();

        assertEquals("Иван", loaded.getOwner().getName());
        assertEquals("+79000000001", loaded.getOwner().getPhone());
    }

    private Equipment saveEquipment(String category, String type, String location,
                                    int price, EquipmentStatus status, BigDecimal reliability) {
        return equipmentRepository.save(Equipment.builder()
                .owner(owner)
                .category(category)
                .type(type)
                .location(location)
                .price(price)
                .priceType("HOUR")
                .status(status)
                .reliability(reliability)
                .build());
    }
}
