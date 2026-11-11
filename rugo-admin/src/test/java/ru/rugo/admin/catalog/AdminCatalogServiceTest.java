package ru.rugo.admin.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.admin.web.dto.AdminEquipmentRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Строгая валидация ручного добавления техники из админки (поля как у партнёра). */
@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CatalogCacheEvictor cacheEvictor;

    @InjectMocks
    private AdminCatalogService service;

    private AdminEquipmentRequest request(String brand, Integer capacity, List<String> images) {
        return new AdminEquipmentRequest(
                "Экскаватор", brand, "ZX200", capacity, null, 3000, "hour", "Центральный", null, null, null, images, null);
    }

    @Test
    void rejectsScriptInBrand() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.addEquipment(request("<script>alert(1)</script>", 8, null)));

        assertThat(e.getErrors()).containsKey("brand");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.addEquipment(request("Hitachi", 0, null)));

        assertThat(e.getErrors()).containsKey("capacity");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void rejectsNonHttpImageUrl() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.addEquipment(request("Hitachi", 8, List.of("javascript:alert(1)"))));

        assertThat(e.getErrors()).containsKey("imageUrls");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void savesValidEquipment() {
        when(userRepository.findByPhone(any())).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(invocation -> {
            Equipment e = invocation.getArgument(0);
            e.setId(42L);
            return e;
        });

        Long id = service.addEquipment(request("Hitachi", 8, List.of("/uploads/equipment/x.png")));

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void listPassesFiltersToRepository() {
        when(equipmentRepository.findForAdmin(eq("Экскаватор"), eq(EquipmentStatus.AVAILABLE)))
                .thenReturn(List.of());

        service.list("Экскаватор", "AVAILABLE");

        verify(equipmentRepository).findForAdmin("Экскаватор", EquipmentStatus.AVAILABLE);
    }

    @Test
    void listWithoutStatusReturnsOnlyPublished() {
        when(equipmentRepository.findForAdminByStatuses(any(), any())).thenReturn(List.of());

        service.list(null, null);

        verify(equipmentRepository).findForAdminByStatuses(isNull(), argThat(statuses ->
                statuses.contains(EquipmentStatus.AVAILABLE)
                        && statuses.contains(EquipmentStatus.BUSY)
                        && statuses.contains(EquipmentStatus.RESERVED)
                        && statuses.contains(EquipmentStatus.BOOKED)
                        && !statuses.contains(EquipmentStatus.PENDING)
                        && !statuses.contains(EquipmentStatus.HIDDEN)));
        verify(equipmentRepository, never()).findForAdmin(any(), any());
    }

    @Test
    void listWithHiddenStatusQueriesExactStatus() {
        when(equipmentRepository.findForAdmin(null, EquipmentStatus.HIDDEN)).thenReturn(List.of());

        service.list(null, "HIDDEN");

        verify(equipmentRepository).findForAdmin(null, EquipmentStatus.HIDDEN);
    }

    @Test
    void listWithInvalidStatusReturnsEmptyWithoutQuery() {
        assertThat(service.list(null, "НЕТ_ТАКОГО")).isEmpty();
        verify(equipmentRepository, never()).findForAdmin(any(), any());
    }

    @Test
    void deleteUnknownThrows() {
        when(equipmentRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> service.delete(99L));
    }

    @Test
    void approveSetsAvailableAndEvictsCache() {
        Equipment e = Equipment.builder().id(5L).status(EquipmentStatus.HIDDEN).build();
        when(equipmentRepository.findById(5L)).thenReturn(Optional.of(e));

        assertThat(service.approve(5L)).isEqualTo("одобрено");
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        verify(cacheEvictor).evict();
    }

    @Test
    void rejectSetsHiddenAndEvictsCache() {
        Equipment e = Equipment.builder().id(6L).status(EquipmentStatus.AVAILABLE).build();
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(e));

        assertThat(service.reject(6L)).isEqualTo("отклонено");
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.HIDDEN);
        verify(cacheEvictor).evict();
    }
}
