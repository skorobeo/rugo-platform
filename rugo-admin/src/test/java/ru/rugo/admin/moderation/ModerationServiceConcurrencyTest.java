package ru.rugo.admin.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.rugo.admin.catalog.CatalogCacheEvictor;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

/**
 * V-024: два админа одновременно одобряют одну заявку. Ожидается ровно одна карточка,
 * второй получает отказ «уже обработана», без дублей и без 500.
 *
 * <p>Проверка на H2 (MODE=PostgreSQL). Реальная PostgreSQL окончательно проверяется
 * в интеграционном наборе (см. docs/audit/DB-VERIFY.md).
 */
@SpringBootTest
@ActiveProfiles("test")
class ModerationServiceConcurrencyTest {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private PartnerLeadRepository leadRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @MockBean
    private AdminMessenger adminMessenger;

    @MockBean
    private CatalogCacheEvictor catalogCacheEvictor;

    @Test
    void concurrentApproveCreatesExactlyOneCard() throws Exception {
        long before = equipmentRepository.count();
        PartnerLead lead = leadRepository.save(PartnerLead.builder()
                .name("Иван")
                .phone("+79001234567")
                .offerTypes(List.of("equipment"))
                .details(Map.of("category", "Экскаватор", "price", 1000, "priceUnit", "hour"))
                .consentAt(OffsetDateTime.now())
                .build());
        Long id = lead.getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> task = () -> {
            start.await();
            return moderationService.approve(id);
        };
        Future<String> first = pool.submit(task);
        Future<String> second = pool.submit(task);
        start.countDown();
        String r1 = first.get(30, TimeUnit.SECONDS);
        String r2 = second.get(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(before + 1, equipmentRepository.count(), "должна создаться ровно одна карточка");
        long processed = (r1.contains("уже обработана") ? 1 : 0) + (r2.contains("уже обработана") ? 1 : 0);
        assertEquals(1, processed, "ровно один вызов должен получить отказ «уже обработана»");
        assertTrue((r1 + r2).contains("equipment"), "один вызов должен создать карточку");
    }

    @Test
    void failedPartnerNotificationDoesNotRollBackApproval() {
        doThrow(new RuntimeException("MAX недоступен")).when(adminMessenger).notifyPartner(any(), anyString());
        long before = equipmentRepository.count();
        PartnerLead lead = leadRepository.save(PartnerLead.builder()
                .name("Пётр")
                .phone("+79005554433")
                .offerTypes(List.of("equipment"))
                .details(Map.of("category", "Автокран", "price", 2000, "priceUnit", "hour"))
                .consentAt(OffsetDateTime.now())
                .build());

        String result = moderationService.approve(lead.getId());

        assertTrue(result.contains("equipment"), "одобрение должно пройти несмотря на сбой уведомления");
        assertEquals(before + 1, equipmentRepository.count(), "карточка должна быть сохранена");
        assertEquals(PartnerLead.STATUS_ACCEPTED, leadRepository.findById(lead.getId()).orElseThrow().getStatus());
    }
}
