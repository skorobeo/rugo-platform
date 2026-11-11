package ru.rugo.app.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.WorkshopRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkshopRepositoryTest {

    @Autowired
    private WorkshopRepository workshopRepository;

    @Test
    void save_persistsWorkshop() {
        Workshop workshop = workshopRepository.save(Workshop.builder()
                .name("РемонтПро")
                .specialization("Ремонт гидравлики и двигателей")
                .location("Воронеж, Левобережный район")
                .phone("+79001112233")
                .consent(true)
                .build());

        assertNotNull(workshop.getId());
        assertNotNull(workshop.getCreatedAt());
        assertEquals("РемонтПро", workshop.getName());
        assertTrue(workshop.getConsent());
    }
}
