package ru.rugo.common.max.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookDedupTest {

    @Test
    void marksOnlyAfterProcessing() {
        WebhookDedup dedup = new WebhookDedup();
        String key = WebhookDedup.hash("{\"update_type\":\"bot_started\"}");

        assertFalse(dedup.isDuplicate(key));
        dedup.markProcessed(key);
        assertTrue(dedup.isDuplicate(key));
    }

    @Test
    void differentBodiesHaveDifferentKeys() {
        assertNotEquals(WebhookDedup.hash("a"), WebhookDedup.hash("b"));
    }
}
