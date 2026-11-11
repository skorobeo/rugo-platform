package ru.rugo.app.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.rugo.app.web.ClientIpResolver;
import ru.rugo.common.config.AppProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    private static ClientIpResolver resolver() {
        AppProperties properties = mock(AppProperties.class);
        when(properties.trustedProxies()).thenReturn(List.of("127.0.0.1", "::1"));
        return new ClientIpResolver(properties);
    }

    @Test
    void takesRealClientFromTrustedProxyChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertEquals("203.0.113.7", resolver().resolve(request));
    }

    @Test
    void ignoresSpoofedEntryPrependedByClient() {
        // Клиент прислал 1.2.3.4, Nginx дописал реальный адрес в конец.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7");

        assertEquals("203.0.113.7", resolver().resolve(request));
    }

    @Test
    void ignoresForwardedForFromUntrustedPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50"); // не доверенный прокси
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("203.0.113.50", resolver().resolve(request));
    }

    @Test
    void fallsBackToRealIpFromTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.9");

        assertEquals("203.0.113.9", resolver().resolve(request));
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.5");

        assertEquals("192.168.1.5", resolver().resolve(request));
    }

    @Test
    void handlesIpv6TrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        request.addHeader("X-Forwarded-For", "2001:db8::1");

        assertEquals("2001:db8::1", resolver().resolve(request));
    }
}
