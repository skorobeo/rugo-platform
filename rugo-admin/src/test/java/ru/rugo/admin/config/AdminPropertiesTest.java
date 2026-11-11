package ru.rugo.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdminPropertiesTest {

    private AdminProperties props(String base) {
        return new AdminProperties(base, 20, 100, 10000, 300, null);
    }

    @Test
    void buildsAbsolutePhotoUrl() {
        AdminProperties p = props("https://example.ru/");
        assertEquals("https://example.ru/uploads/equipment/a.jpg", p.absoluteUrl("/uploads/equipment/a.jpg"));
        assertEquals("https://example.ru/x.jpg", p.absoluteUrl("x.jpg"));
        assertEquals("http://cdn/x.jpg", p.absoluteUrl("http://cdn/x.jpg"));
        assertNull(p.absoluteUrl(null));
    }

    @Test
    void noBaseUrlReturnsNull() {
        assertNull(props("").absoluteUrl("/uploads/a.jpg"));
    }
}
