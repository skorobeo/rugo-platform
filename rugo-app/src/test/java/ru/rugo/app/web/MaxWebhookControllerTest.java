package ru.rugo.app.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.User;
import ru.rugo.app.bot.MainBotUpdateHandler;
import ru.rugo.common.max.webhook.MaxDeliveryState;
import ru.rugo.common.max.webhook.WebhookDedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaxWebhookControllerTest {

    private static final String SECRET = "secret-12345";
    private static final String BODY = "{\"update_type\":\"bot_started\"}";

    @Mock
    private MaxBotAPI api;
    @Mock
    private MaxSerializer serializer;
    @Mock
    private MainBotUpdateHandler handler;

    private MaxDeliveryState state;
    private MaxWebhookController controller;
    private Update update;

    @BeforeEach
    void setUp() {
        state = new MaxDeliveryState();
        state.enableWebhook();
        controller = new MaxWebhookController(api, handler, state, new WebhookDedup(), SECRET);
        update = new BotStartedUpdate(0L, 100L, new User(1L, "User", null, null, null, false, 0L), null, null);
        lenient().when(api.serializer()).thenReturn(serializer);
        lenient().when(serializer.deserialize(anyString(), eq(Update.class))).thenReturn(update);
    }

    @Test
    void acceptsValidUpdate() {
        ResponseEntity<Void> response = controller.receive(SECRET, BODY);

        assertEquals(200, response.getStatusCode().value());
        verify(handler).handle(update);
    }

    @Test
    void rejectsWrongSecret() {
        ResponseEntity<Void> response = controller.receive("wrong-secret", BODY);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(handler);
    }

    @Test
    void rejectsWhenWebhookDisabled() {
        state.disableAll();

        ResponseEntity<Void> response = controller.receive(SECRET, BODY);

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(handler);
    }

    @Test
    void ignoresDuplicateDelivery() {
        controller.receive(SECRET, BODY);
        controller.receive(SECRET, BODY);

        verify(handler, times(1)).handle(update);
    }

    @Test
    void ignoresMalformedBody() {
        when(serializer.deserialize(anyString(), eq(Update.class)))
                .thenThrow(new IllegalArgumentException("bad json"));

        ResponseEntity<Void> response = controller.receive(SECRET, "{bad");

        assertEquals(200, response.getStatusCode().value());
        verify(handler, never()).handle(update);
    }

    @Test
    void returns500WhenHandlerFails() {
        doThrow(new IllegalStateException("boom")).when(handler).handle(update);

        ResponseEntity<Void> response = controller.receive(SECRET, BODY);

        assertEquals(500, response.getStatusCode().value());
    }
}
