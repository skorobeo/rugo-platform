package ru.rugo.app.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.SendMessageQuery;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.ChatType;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageRecipient;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.User;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainBotUpdateHandlerTest {

    @Mock
    private MaxBotAPI api;

    @Mock
    private SendMessageQuery query;

    private MainBotUpdateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MainBotUpdateHandler(api, "Здравствуйте!");
        lenient().when(api.sendMessage(any(NewMessageBody.class))).thenReturn(query);
        lenient().when(query.chatId(anyLong())).thenReturn(query);
    }

    private MessageCreatedUpdate textMessage(String text) {
        User user = new User(1L, "User", null, null, null, false, 0L);
        MessageRecipient recipient = new MessageRecipient(100L, ChatType.DIALOG);
        MessageBody body = new MessageBody("mid-1", 1L, text, null, null);
        Message message = new Message(user, recipient, 0L, null, body, null, null, null);
        return new MessageCreatedUpdate(0L, message, null);
    }

    @Test
    void sendsGreetingOnBotStarted() {
        User user = new User(1L, "User", null, null, null, false, 0L);
        handler.handle(new BotStartedUpdate(0L, 100L, user, null, null));
        verify(query, times(1)).enqueue();
    }

    @Test
    void sendsGreetingOnStartCommand() {
        handler.handle(textMessage("/start"));
        verify(query, times(1)).enqueue();
    }

    @Test
    void ignoresOtherMessages() {
        handler.handle(textMessage("привет, сколько стоит?"));
        verify(query, never()).enqueue();
    }

    @Test
    void doesNothingWithoutGreeting() {
        MainBotUpdateHandler empty = new MainBotUpdateHandler(api, "");
        User user = new User(1L, "User", null, null, null, false, 0L);
        empty.handle(new BotStartedUpdate(0L, 100L, user, null, null));
        verify(query, never()).enqueue();
    }
}
