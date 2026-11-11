package ru.rugo.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PartnerAccountService service;

    @Test
    void returnsExistingPartnerByMaxId() {
        User existing = User.builder().id(1L).phone("+79001234567").name("Иван").maxId(5L).build();
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.of(existing));

        User result = service.resolveOrCreateForCabinet(5L, "+79001234567", "Иван");

        assertEquals(existing, result);
        verify(userRepository, never()).findByPhone(any());
    }

    @Test
    void throwsWhenPhoneLinkedToAnotherAccount() {
        User other = User.builder().id(1L).phone("+79001234567").name("Пётр").maxId(111L).build();
        when(userRepository.findByMaxId(222L)).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(other));

        ApiValidationException error = assertThrows(ApiValidationException.class,
                () -> service.resolveOrCreateForCabinet(222L, "+79001234567", "Иван"));

        assertTrue(error.getErrors().get("phone").contains("другому аккаунту MAX"));
    }

    @Test
    void linksMaxIdWhenPhoneExistsWithoutOne() {
        User existing = User.builder().id(1L).phone("+79001234567").name("").maxId(null).build();
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.resolveOrCreateForCabinet(5L, "+79001234567", "Иван");

        assertEquals(5L, result.getMaxId());
        assertEquals("Иван", result.getName());
        assertEquals(User.ROLE_PROVIDER, result.getRole());
    }

    @Test
    void createsNewPartnerWhenNothingFound() {
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.resolveOrCreateForCabinet(5L, "+79001234567", null);

        assertNotNull(result);
        assertEquals("+79001234567", result.getPhone());
        assertEquals(5L, result.getMaxId());
        assertEquals(User.ROLE_PROVIDER, result.getRole());
        assertEquals(User.STATUS_ACTIVE, result.getStatus());
    }

    @Test
    void promotesExistingUserToCompanyOnApproval() {
        User existing = User.builder().id(1L).phone("+79001234567").name("Иван")
                .partnerType(null).maxId(5L).build();
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.resolveOrCreate(5L, "+79001234567", "ООО Ромашка",
                "company", "Пётр");

        assertEquals("company", result.getPartnerType());
        assertEquals("ООО Ромашка", result.getName());
        assertEquals("Пётр", result.getContactName());
    }

    @Test
    void doesNotDowngradeCompanyToIndividual() {
        User existing = User.builder().id(1L).phone("+79001234567").name("ООО Ромашка")
                .partnerType("company").contactName("Пётр").maxId(5L).build();
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.resolveOrCreate(5L, "+79001234567", "Иван",
                "individual", null);

        assertEquals("company", result.getPartnerType());
        assertEquals("ООО Ромашка", result.getName());
        assertEquals("Пётр", result.getContactName());
    }

    @Test
    void createsNewCompanyPartner() {
        when(userRepository.findByMaxId(5L)).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.resolveOrCreate(5L, "+79001234567", "ООО Ромашка",
                "company", "Пётр");

        assertEquals("company", result.getPartnerType());
        assertEquals("ООО Ромашка", result.getName());
        assertEquals("Пётр", result.getContactName());
    }
}
