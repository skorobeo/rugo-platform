package ru.rugo.admin.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.security.AdminInitDataUserExtractor;
import ru.rugo.core.security.MaxInitDataValidator;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Доступ в админку только по MAX id (initData), без токена. */
@ExtendWith(MockitoExtension.class)
class AdminAuthFilterTest {

    @Mock
    private AdminDirectory adminDirectory;

    @Mock
    private MaxInitDataValidator adminInitDataValidator;

    @Mock
    private MaxInitDataValidator mainInitDataValidator;

    @Mock
    private AdminInitDataUserExtractor userExtractor;

    private AdminAuthFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(adminDirectory.isAdmin(any())).thenReturn(false);
        filter = new AdminAuthFilter(adminDirectory, adminInitDataValidator, mainInitDataValidator, userExtractor);
    }

    private MockHttpServletRequest apiRequest() {
        return new MockHttpServletRequest("GET", "/admin/api/leads");
    }

    @Test
    void allowsAdminInitData() throws Exception {
        when(adminInitDataValidator.validate("signed")).thenReturn(Optional.of(Map.of("user", "{}")));
        when(userExtractor.extractUserId(any())).thenReturn(13883640L);
        when(adminDirectory.isAdmin(13883640L)).thenReturn(true);
        MockHttpServletRequest request = apiRequest();
        request.addHeader("X-Init-Data", "signed");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsAdminInitDataSignedByMainBot() throws Exception {
        when(mainInitDataValidator.validate("signed")).thenReturn(Optional.of(Map.of("user", "{}")));
        when(userExtractor.extractUserId(any())).thenReturn(13883640L);
        when(adminDirectory.isAdmin(13883640L)).thenReturn(true);
        MockHttpServletRequest request = apiRequest();
        request.addHeader("X-Init-Data", "signed");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsNonAdmin() throws Exception {
        when(adminInitDataValidator.validate("signed")).thenReturn(Optional.of(Map.of("user", "{}")));
        when(userExtractor.extractUserId(any())).thenReturn(999L);
        MockHttpServletRequest request = apiRequest();
        request.addHeader("X-Init-Data", "signed");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsWithoutInitData() throws Exception {
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/assets/index.js");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
