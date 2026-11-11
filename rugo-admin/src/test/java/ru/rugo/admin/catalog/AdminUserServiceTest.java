package ru.rugo.admin.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserService service;

    @Test
    void rejectsUnknownStatus() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.setStatus(1L, "HACKED"));

        assertThat(e.getErrors()).containsKey("status");
    }

    @Test
    void blocksUser() {
        User user = User.builder().id(5L).status(User.STATUS_ACTIVE).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        service.setStatus(5L, User.STATUS_BLOCKED);

        assertThat(user.getStatus()).isEqualTo(User.STATUS_BLOCKED);
        verify(userRepository).save(user);
    }
}
