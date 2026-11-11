package ru.rugo.app.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private UserRepository userRepository;

    private User supplier;

    @BeforeEach
    void setUp() {
        supplier = userRepository.save(User.builder()
                .phone("+79000000002")
                .name("Карьер")
                .role("PROVIDER")
                .build());
    }

    @Test
    void findByTypeWithSupplier_matchesTypeAndFraction() {
        saveMaterial("Щебень гранитный", "5-20", 720, true);
        saveMaterial("Щебень гранитный", "20-40", 680, true);
        saveMaterial("Песок речной", null, 320, true);

        List<Material> result = materialRepository.findByTypeWithSupplier("Щебень гранитный 5-20");

        assertEquals(1, result.size());
        assertEquals("Щебень гранитный", result.get(0).getType());
        assertEquals("5-20", result.get(0).getFraction());
    }

    @Test
    void findByTypeWithSupplier_matchesTypeWithoutFraction() {
        saveMaterial("Песок речной", null, 320, true);
        saveMaterial("Песок карьерный", null, 240, true);

        List<Material> result = materialRepository.findByTypeWithSupplier("Песок речной");

        assertEquals(1, result.size());
        assertEquals("Песок речной", result.get(0).getType());
    }

    @Test
    void findByTypeWithSupplier_loadsSupplier() {
        saveMaterial("Песок речной", null, 320, true);

        Material result = materialRepository.findByTypeWithSupplier("Песок речной").get(0);

        assertNotNull(result.getSupplier());
        assertEquals("Карьер", result.getSupplier().getName());
    }

    @Test
    void findByAvailableTrueOrderByPricePerUnitAsc_filtersAndSorts() {
        saveMaterial("Песок речной", null, 400, true);
        saveMaterial("Песок речной", null, 200, true);
        saveMaterial("Песок речной", null, 300, false);

        List<Material> result = materialRepository.findByAvailableTrueOrderByPricePerUnitAsc();

        assertEquals(List.of(200, 400), result.stream().map(Material::getPricePerUnit).toList());
    }

    private void saveMaterial(String type, String fraction, int price, boolean available) {
        materialRepository.save(Material.builder()
                .supplier(supplier)
                .type(type)
                .fraction(fraction)
                .pricePerUnit(price)
                .unit("тонна")
                .location("Воронеж")
                .available(available)
                .build());
    }
}
