package ru.rugo.app.service;

import org.junit.jupiter.api.Test;
import ru.rugo.common.config.AppProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserHashServiceTest {

    private UserHashService service(String salt) {
        AppProperties properties = mock(AppProperties.class);
        when(properties.analyticsSalt()).thenReturn(salt);
        return new UserHashService(properties);
    }

    @Test
    void isDeterministicAndDistinguishesUsers() {
        UserHashService service = service("salt");
        assertEquals(service.hash(42L, null), service.hash(42L, null));
        assertNotEquals(service.hash(42L, null), service.hash(43L, null));
    }

    @Test
    void neverContainsRawId() {
        String hash = service("salt").hash(424242L, null);
        assertEquals(64, hash.length());
        assertFalse(hash.contains("424242"));
    }

    @Test
    void usesSessionWhenNoUser() {
        UserHashService service = service("salt");
        assertNotEquals(service.hash(null, "sess-1"), service.hash(null, "sess-2"));
        assertEquals(service.hash(null, null), service.hash(null, " "));
    }

    @Test
    void differentSaltsProduceDifferentHashes() {
        assertNotEquals(service("salt-a").hash(1L, null), service("salt-b").hash(1L, null));
    }
}
