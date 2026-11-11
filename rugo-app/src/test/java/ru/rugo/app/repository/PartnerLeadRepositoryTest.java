package ru.rugo.app.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PartnerLeadRepositoryTest {

    @Autowired
    private PartnerLeadRepository partnerLeadRepository;

    @Test
    void save_persistsJsonCollectionsAndTimestamp() {
        PartnerLead saved = partnerLeadRepository.save(PartnerLead.builder()
                .maxUserId(424242L)
                .name("Иван")
                .phone("+79001234567")
                .offerTypes(List.of("equipment", "materials"))
                .district("Левобережный")
                .details(Map.of("category", "Экскаватор", "price", 2800))
                .source("m_ivan")
                .consentAt(OffsetDateTime.now())
                .build());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        PartnerLead reloaded = partnerLeadRepository.findById(saved.getId()).orElseThrow();
        assertEquals(List.of("equipment", "materials"), reloaded.getOfferTypes());
        assertEquals("Экскаватор", reloaded.getDetails().get("category"));
        assertEquals(2800, reloaded.getDetails().get("price"));
        assertEquals("new", reloaded.getStatus());
    }

    @Test
    void existsByPhoneAndWindow_findsRecentDuplicateOnly() {
        partnerLeadRepository.save(PartnerLead.builder()
                .name("Пётр")
                .phone("+79005556677")
                .offerTypes(List.of("equipment"))
                .consentAt(OffsetDateTime.now())
                .build());

        assertTrue(partnerLeadRepository.existsByPhoneAndCreatedAtGreaterThanEqual(
                "+79005556677", OffsetDateTime.now().minusHours(1)));
        assertFalse(partnerLeadRepository.existsByPhoneAndCreatedAtGreaterThanEqual(
                "+79000000000", OffsetDateTime.now().minusHours(1)));
    }
}
