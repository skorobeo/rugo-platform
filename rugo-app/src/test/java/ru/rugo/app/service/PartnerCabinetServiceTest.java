package ru.rugo.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerCabinetServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private PartnerLeadRepository leadRepository;

    private PartnerCabinetService service;

    @BeforeEach
    void setUp() {
        service = new PartnerCabinetService(materialRepository, leadRepository);
    }

    @Test
    void deletesOwnMaterial() {
        Material material = Material.builder()
                .id(1L)
                .supplier(User.builder().id(5L).build())
                .build();
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        service.deleteOwnedMaterial(5L, 1L);

        verify(materialRepository).delete(material);
    }

    @Test
    void rejectsDeletingForeignMaterial() {
        Material material = Material.builder()
                .id(1L)
                .supplier(User.builder().id(9L).build())
                .build();
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        assertThrows(ApiValidationException.class, () -> service.deleteOwnedMaterial(5L, 1L));
        verify(materialRepository, never()).delete(any());
    }
}
