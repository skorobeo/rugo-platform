package ru.rugo.admin.bot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadDispatcherTest {

    @Mock
    private PartnerLeadRepository leadRepository;

    @Mock
    private AdminMessenger messenger;

    @Mock
    private AdminDirectory adminDirectory;

    @InjectMocks
    private LeadDispatcher dispatcher;

    private void enableCards() {
        ReflectionTestUtils.setField(dispatcher, "notifyCardsEnabled", true);
    }

    @Test
    void skipsWhenCardsDisabled() {
        ReflectionTestUtils.setField(dispatcher, "notifyCardsEnabled", false);

        dispatcher.dispatch();

        verifyNoInteractions(leadRepository);
    }

    @Test
    void skipsWhenTokenMissing() {
        enableCards();
        ReflectionTestUtils.setField(dispatcher, "adminToken", "");

        dispatcher.dispatch();

        verifyNoInteractions(leadRepository);
    }

    @Test
    void skipsWhenNoAdmins() {
        enableCards();
        ReflectionTestUtils.setField(dispatcher, "adminToken", "token");
        when(adminDirectory.hasAdmins()).thenReturn(false);

        dispatcher.dispatch();

        verifyNoInteractions(leadRepository);
    }

    @Test
    void marksNotifiedOnlyAfterDelivery() {
        enableCards();
        ReflectionTestUtils.setField(dispatcher, "adminToken", "token");
        when(adminDirectory.hasAdmins()).thenReturn(true);
        PartnerLead lead = PartnerLead.builder().id(1L).build();
        when(leadRepository.findByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(PartnerLead.STATUS_NEW))
                .thenReturn(List.of(lead));
        when(messenger.sendCard(lead)).thenReturn(true);

        dispatcher.dispatch();

        assertNotNull(lead.getNotifiedAt());
        verify(leadRepository).save(lead);
    }

    @Test
    void keepsLeadForRetryWhenDeliveryFails() {
        enableCards();
        ReflectionTestUtils.setField(dispatcher, "adminToken", "token");
        when(adminDirectory.hasAdmins()).thenReturn(true);
        PartnerLead lead = PartnerLead.builder().id(2L).build();
        when(leadRepository.findByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(PartnerLead.STATUS_NEW))
                .thenReturn(List.of(lead));
        when(messenger.sendCard(lead)).thenReturn(false);

        dispatcher.dispatch();

        assertNull(lead.getNotifiedAt());
        verify(leadRepository, never()).save(any());
    }
}
