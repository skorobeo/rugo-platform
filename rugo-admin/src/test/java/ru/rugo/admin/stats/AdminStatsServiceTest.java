package ru.rugo.admin.stats;

import org.junit.jupiter.api.Test;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.AnalyticsEventRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.util.AnalyticsHash;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStatsServiceTest {

    private final AnalyticsEventRepository events = mock(AnalyticsEventRepository.class);
    private final PartnerLeadRepository leads = mock(PartnerLeadRepository.class);
    private final AdminUserRepository admins = mock(AdminUserRepository.class);
    private final AppProperties properties = mock(AppProperties.class);

    private AdminStatsService service() {
        return new AdminStatsService(events, leads, admins, properties);
    }

    @Test
    void excludesAdminHashesFromSummary() {
        long ownerId = 185547050L;
        when(admins.findAllMaxUserIds()).thenReturn(List.of(ownerId));
        when(properties.analyticsSalt()).thenReturn("salt");
        when(events.countDistinctUsersExcluding(any())).thenReturn(8L);
        when(events.countByEventExcluding(anyString(), any())).thenReturn(70L);
        when(events.countsByEventExcluding(any())).thenReturn(List.of());
        when(leads.count()).thenReturn(0L);

        String summary = service().summary();

        String expectedHash = AnalyticsHash.userHash("salt", ownerId);
        verify(events).countDistinctUsersExcluding(List.of(expectedHash));
        verify(events).countByEventExcluding("app_open", List.of(expectedHash));
        verify(events, never()).countDistinctUsers();
        assertTrue(summary.contains("Уникальных пользователей: 8"));
        assertTrue(summary.contains("Заходов (app_open): 70"));
    }

    @Test
    void fallsBackToUnfilteredWhenNoAdmins() {
        when(admins.findAllMaxUserIds()).thenReturn(List.of());
        when(events.countDistinctUsers()).thenReturn(9L);
        when(events.countByEvent(anyString())).thenReturn(72L);
        when(events.countsByEvent()).thenReturn(List.of());
        when(leads.count()).thenReturn(0L);

        service().summary();

        verify(events).countDistinctUsers();
        verify(events).countByEvent("app_open");
        verify(events, never()).countByEventExcluding(anyString(), any());
    }
}
