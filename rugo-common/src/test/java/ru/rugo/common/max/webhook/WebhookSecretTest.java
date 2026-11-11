package ru.rugo.common.max.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSecretTest {

    @Test
    void acceptsMatchingSecret() {
        assertTrue(WebhookSecret.matches("secret-123", "secret-123"));
    }

    @Test
    void rejectsDifferentOrMissingSecret() {
        assertFalse(WebhookSecret.matches("secret-123", "secret-124"));
        assertFalse(WebhookSecret.matches("secret-123", null));
        assertFalse(WebhookSecret.matches(null, "secret-123"));
        assertFalse(WebhookSecret.matches("", ""));
    }
}
