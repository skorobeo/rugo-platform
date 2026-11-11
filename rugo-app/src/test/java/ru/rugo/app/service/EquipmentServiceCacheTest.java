package ru.rugo.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import ru.max.botapi.client.MaxBotAPI;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.service.EquipmentService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@MockBean(MaxBotAPI.class)
class EquipmentServiceCacheTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private EquipmentRepository equipmentRepository;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("equipment");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void secondCallWithSameKeyIsServedFromCache() {
        equipmentService.findByCategoryWithOwner("Самосвал", "Воронеж");
        equipmentService.findByCategoryWithOwner("Самосвал", "Воронеж");

        verify(equipmentRepository, times(1)).findByCategoryWithOwner("Самосвал", "Воронеж");
    }

    @Test
    void differentKeysAreNotCachedTogether() {
        equipmentService.findByCategoryWithOwner("Самосвал", "Воронеж");
        equipmentService.findByCategoryWithOwner("Экскаватор", "Воронеж");

        verify(equipmentRepository).findByCategoryWithOwner("Самосвал", "Воронеж");
        verify(equipmentRepository).findByCategoryWithOwner("Экскаватор", "Воронеж");
    }
}
