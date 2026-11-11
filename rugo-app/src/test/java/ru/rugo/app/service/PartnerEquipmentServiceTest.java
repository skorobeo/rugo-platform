package ru.rugo.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.EquipmentRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerEquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    private PartnerEquipmentService service;

    @BeforeEach
    void setUp() {
        service = new PartnerEquipmentService(equipmentRepository);
    }

    private User owner() {
        return User.builder().id(7L).name("Андрей").build();
    }

    @Test
    void deleteRemovesOwnedEquipment() {
        Equipment existing = Equipment.builder().id(5L).owner(owner()).build();
        when(equipmentRepository.findByIdAndOwnerId(5L, 7L)).thenReturn(Optional.of(existing));

        service.delete(owner(), 5L);

        verify(equipmentRepository).delete(existing);
    }

    @Test
    void deleteMissingOwnedEquipmentThrowsWithoutDelete() {
        when(equipmentRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.delete(owner(), 99L));
        verify(equipmentRepository, never()).delete(org.mockito.ArgumentMatchers.any(Equipment.class));
    }
}
