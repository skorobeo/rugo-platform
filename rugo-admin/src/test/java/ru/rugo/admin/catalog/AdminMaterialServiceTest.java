package ru.rugo.admin.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.admin.web.dto.AdminMaterialRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CatalogCacheEvictor cacheEvictor;

    @InjectMocks
    private AdminMaterialService service;

    private AdminMaterialRequest request(String category, String fraction) {
        return new AdminMaterialRequest(category, fraction, 720, "тонна", "DELIVERY", "Центральный", null, null, null);
    }

    private AdminMaterialRequest requestWithPhoto(String imageUrl) {
        return new AdminMaterialRequest(
                "Щебень гранитный", "5-20", 720, "тонна", "DELIVERY", "Центральный",
                null, null, List.of(imageUrl));
    }

    @Test
    void rejectsUnknownType() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.create(request("Абракадабра", null)));

        assertThat(e.getErrors()).containsKey("category");
        verify(materialRepository, never()).save(any());
    }

    @Test
    void rejectsScriptInFraction() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.create(request("Щебень гранитный", "<script>alert(1)</script>")));

        assertThat(e.getErrors()).containsKey("fraction");
        verify(materialRepository, never()).save(any());
    }

    @Test
    void savesValid() {
        when(userRepository.findByPhone(any())).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material m = invocation.getArgument(0);
            m.setId(7L);
            return m;
        });

        Long id = service.create(request("Щебень гранитный", "5-20"));

        assertThat(id).isEqualTo(7L);
    }

    @Test
    void updateKeepsExistingImageUrl() {
        Material existing = Material.builder()
                .id(3L)
                .type("Щебень гранитный")
                .imageUrl("/uploads/materials/pgs.jpg")
                .status("APPROVED")
                .available(true)
                .supplier(User.builder().id(1L).build())
                .build();
        when(materialRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(3L, request("Щебень гранитный", "5-20"));

        assertThat(existing.getImageUrl()).isEqualTo("/uploads/materials/pgs.jpg");
    }

    @Test
    void createStoresFirstPhoto() {
        when(userRepository.findByPhone(any())).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material m = invocation.getArgument(0);
            m.setId(7L);
            return m;
        });

        service.create(requestWithPhoto("/uploads/materials/pesok.jpg"));

        org.mockito.ArgumentCaptor<Material> captor = org.mockito.ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl()).isEqualTo("/uploads/materials/pesok.jpg");
    }

    @Test
    void updateReplacesImageUrlWhenProvided() {
        Material existing = Material.builder()
                .id(3L)
                .type("Щебень гранитный")
                .imageUrl("/uploads/materials/old.jpg")
                .status("APPROVED")
                .available(true)
                .supplier(User.builder().id(1L).build())
                .build();
        when(materialRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(3L, requestWithPhoto("/uploads/materials/new.jpg"));

        assertThat(existing.getImageUrl()).isEqualTo("/uploads/materials/new.jpg");
    }

    @Test
    void listWithoutStatusDefaultsToApproved() {
        when(materialRepository.findForAdmin(null, Material.STATUS_APPROVED)).thenReturn(List.of());

        service.list(null, null);

        verify(materialRepository).findForAdmin(null, Material.STATUS_APPROVED);
    }

    @Test
    void listWithPendingStatusQueriesPending() {
        when(materialRepository.findForAdmin(null, Material.STATUS_PENDING)).thenReturn(List.of());

        service.list(null, "PENDING");

        verify(materialRepository).findForAdmin(null, Material.STATUS_PENDING);
    }
}
