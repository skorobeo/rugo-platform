package ru.rugo.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import ru.rugo.common.config.AppProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsSaltValidatorTest {

    private static AppProperties props(String salt) {
        AppProperties p = mock(AppProperties.class);
        when(p.analyticsSalt()).thenReturn(salt);
        return p;
    }

    @Test
    void throwsInProdWithBlankSalt() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class,
                () -> new AnalyticsSaltValidator(props(null), env).afterPropertiesSet());
    }

    @Test
    void allowsBlankSaltOutsideProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertDoesNotThrow(() -> new AnalyticsSaltValidator(props(""), env).afterPropertiesSet());
    }
}
