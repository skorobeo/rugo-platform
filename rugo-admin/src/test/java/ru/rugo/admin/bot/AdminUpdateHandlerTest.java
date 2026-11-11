package ru.rugo.admin.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.max.botapi.model.Callback;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.User;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.moderation.ModerationService;
import ru.rugo.admin.notify.AdminMessenger;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUpdateHandlerTest {

    private static final long ADMIN_ID = 13883640L;

    @Mock
    private AdminMessenger messenger;
    @Mock
    private ModerationService moderationService;
    @Mock
    private AdminDirectory adminDirectory;
    @Mock
    private AdminCommands commands;

    private AdminUpdateHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(adminDirectory.isAdmin(ADMIN_ID)).thenReturn(true);
        handler = new AdminUpdateHandler(messenger, moderationService, adminDirectory, commands);
    }

    private MessageCallbackUpdate callback(long userId, String payload) {
        User user = new User(userId, "Admin", null, null, null, false, 0L);
        Callback cb = new Callback(0L, "cb-1", payload, user);
        return new MessageCallbackUpdate(0L, cb, null, null);
    }

    @Test
    void approveFromAdmin() {
        when(moderationService.approve(10L)).thenReturn("equipment #5");
        handler.handle(callback(ADMIN_ID, "lead:10:approve"));
        verify(messenger).answerCallback(eq("cb-1"), eq("Одобрено"));
        verify(messenger).sendText(eq(ADMIN_ID), contains("equipment #5"));
    }

    @Test
    void rejectFromAdmin() {
        when(moderationService.reject(11L)).thenReturn("отклонена");
        handler.handle(callback(ADMIN_ID, "lead:11:reject"));
        verify(messenger).answerCallback(eq("cb-1"), eq("Отклонено"));
    }

    @Test
    void contactFromAdmin() {
        when(moderationService.contactInfo(12L)).thenReturn("Имя: Иван");
        handler.handle(callback(ADMIN_ID, "lead:12:contact"));
        verify(messenger).answerCallback(eq("cb-1"), eq("Контакт отправлен"));
        verify(messenger).sendText(eq(ADMIN_ID), contains("Иван"));
    }

    @Test
    void nonAdminDenied() {
        handler.handle(callback(999999L, "lead:10:approve"));
        verify(moderationService, never()).approve(anyLong());
        verify(messenger).answerCallback(eq("cb-1"), eq("Нет доступа"));
    }
}
