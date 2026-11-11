package ru.rugo.common.max.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.GetSubscriptionsQuery;
import ru.max.botapi.client.queries.SubscribeQuery;
import ru.max.botapi.client.queries.UnsubscribeQuery;
import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UpdateType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaxSubscriptionManagerTest {

    private static final String URL = "https://example.test/api/max/webhook";
    private static final String SECRET = "secret-12345";

    @Mock
    private MaxBotAPI api;
    @Mock
    private GetSubscriptionsQuery getSubscriptionsQuery;
    @Mock
    private SubscribeQuery subscribeQuery;
    @Mock
    private UnsubscribeQuery unsubscribeQuery;

    private MaxDeliveryState state;

    @BeforeEach
    void setUp() {
        state = new MaxDeliveryState();
        lenient().when(api.getSubscriptions()).thenReturn(getSubscriptionsQuery);
        lenient().when(api.subscribe(any(SubscriptionRequestBody.class))).thenReturn(subscribeQuery);
        lenient().when(api.unsubscribe(anyString())).thenReturn(unsubscribeQuery);
        lenient().when(getSubscriptionsQuery.execute())
                .thenReturn(new GetSubscriptionsResult(List.of()));
        lenient().when(subscribeQuery.execute()).thenReturn(new SimpleQueryResult(true, null));
        lenient().when(unsubscribeQuery.execute()).thenReturn(new SimpleQueryResult(true, null));
    }

    private MaxSubscriptionManager manager(DeliveryMode mode, String url, String secret) {
        return new MaxSubscriptionManager(api, mode, url, secret,
                List.of(UpdateType.BOT_STARTED, UpdateType.MESSAGE_CREATED), state, 3);
    }

    @Test
    void longPollingRemovesWebhookSubscriptions() {
        when(getSubscriptionsQuery.execute()).thenReturn(new GetSubscriptionsResult(
                List.of(new Subscription("https://old.example/webhook", null))));

        manager(DeliveryMode.LONGPOLL, URL, SECRET).onApplicationReady();

        verify(api).unsubscribe("https://old.example/webhook");
        assertTrue(state.longPollingEnabled());
        assertFalse(state.webhookEnabled());
        verify(api, never()).subscribe(any(SubscriptionRequestBody.class));
    }

    @Test
    void webhookRegistersSubscriptionAndRemovesStaleOne() {
        when(getSubscriptionsQuery.execute()).thenReturn(new GetSubscriptionsResult(
                List.of(new Subscription("https://old.example/webhook", null))));

        manager(DeliveryMode.WEBHOOK, URL, SECRET).onApplicationReady();

        verify(api).unsubscribe("https://old.example/webhook");
        ArgumentCaptor<SubscriptionRequestBody> body = ArgumentCaptor.forClass(SubscriptionRequestBody.class);
        verify(api).subscribe(body.capture());
        assertEquals(URL, body.getValue().url());
        assertEquals(SECRET, body.getValue().secret());
        assertTrue(state.webhookEnabled());
        assertFalse(state.longPollingEnabled());
    }

    @Test
    void webhookFailureFallsBackToLongPolling() {
        when(subscribeQuery.execute()).thenReturn(new SimpleQueryResult(false, "rejected"));

        manager(DeliveryMode.WEBHOOK, URL, SECRET).onApplicationReady();

        assertTrue(state.longPollingEnabled());
        assertFalse(state.webhookEnabled());
    }

    @Test
    void invalidWebhookUrlFallsBackToLongPolling() {
        manager(DeliveryMode.WEBHOOK, "", SECRET).onApplicationReady();

        verify(api, never()).subscribe(any(SubscriptionRequestBody.class));
        assertTrue(state.longPollingEnabled());
        assertFalse(state.webhookEnabled());
    }

    @Test
    void existingOwnSubscriptionIsNotRecreated() {
        when(getSubscriptionsQuery.execute()).thenReturn(new GetSubscriptionsResult(
                List.of(new Subscription(URL, null))));

        manager(DeliveryMode.WEBHOOK, URL, SECRET).onApplicationReady();

        verify(api, never()).subscribe(any(SubscriptionRequestBody.class));
        assertTrue(state.webhookEnabled());
    }
}
