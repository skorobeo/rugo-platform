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
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.service.MaterialService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@MockBean(MaxBotAPI.class)
class MaterialServiceCacheTest {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private MaterialRepository materialRepository;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("materials");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void secondCallWithSameTypeIsServedFromCache() {
        materialService.findByTypeWithSupplier("Щебень гранитный 5-20");
        materialService.findByTypeWithSupplier("Щебень гранитный 5-20");

        verify(materialRepository, times(1)).findByTypeWithSupplier("Щебень гранитный 5-20");
    }

    @Test
    void differentTypesAreNotCachedTogether() {
        materialService.findByTypeWithSupplier("Песок речной");
        materialService.findByTypeWithSupplier("Песок карьерный");

        verify(materialRepository).findByTypeWithSupplier("Песок речной");
        verify(materialRepository).findByTypeWithSupplier("Песок карьерный");
    }
}
