package ru.rugo.admin.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rugo.admin.web.dto.AdminWorkshopRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.WorkshopRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkshopServiceTest {

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private CatalogCacheEvictor cacheEvictor;

    @InjectMocks
    private AdminWorkshopService service;

    private AdminWorkshopRequest request(String name, String phone) {
        return new AdminWorkshopRequest(name, "Ремонт гидравлики", "Воронеж", "Левобережный", phone);
    }

    @Test
    void rejectsScriptInName() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.create(request("<script>alert(1)</script>", "+79001234567")));

        assertThat(e.getErrors()).containsKey("name");
        verify(workshopRepository, never()).save(any());
    }

    @Test
    void rejectsBadPhone() {
        ApiValidationException e = assertThrows(ApiValidationException.class,
                () -> service.create(request("Гараж", "не телефон")));

        assertThat(e.getErrors()).containsKey("phone");
        verify(workshopRepository, never()).save(any());
    }

    @Test
    void savesValid() {
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(invocation -> {
            Workshop w = invocation.getArgument(0);
            w.setId(3L);
            return w;
        });

        Long id = service.create(request("Гараж", "+79001234567"));

        assertThat(id).isEqualTo(3L);
    }

    @Test
    void listWithoutStatusDefaultsToApproved() {
        when(workshopRepository.findForAdmin(Workshop.STATUS_APPROVED)).thenReturn(List.of());

        service.list(null);

        verify(workshopRepository).findForAdmin(Workshop.STATUS_APPROVED);
    }

    @Test
    void listWithPendingStatusQueriesPending() {
        when(workshopRepository.findForAdmin(Workshop.STATUS_PENDING)).thenReturn(List.of());

        service.list("PENDING");

        verify(workshopRepository).findForAdmin(Workshop.STATUS_PENDING);
    }
}
