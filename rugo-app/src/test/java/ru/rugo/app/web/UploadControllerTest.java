package ru.rugo.app.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.rugo.core.storage.PhotoStorageService;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.security.MaxInitDataValidator;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock
    private MaxInitDataValidator validator;

    @Mock
    private PhotoStorageService photoStorageService;

    @Mock
    private AppProperties properties;

    private MockMvc mockMvc;

    private final MockMultipartFile file =
            new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());

    @BeforeEach
    void setUp() {
        UploadController controller = new UploadController(
                validator, photoStorageService, new FixedWindowRateLimiter(), new ClientIpResolver(properties), properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        lenient().when(properties.photoUploadsEnabled()).thenReturn(true);
        lenient().when(properties.allowAnonLeads()).thenReturn(false);
        lenient().when(properties.rateLimit())
                .thenReturn(new AppProperties.RateLimit(100, 3600, 100, 3600, 60));
    }

    @Test
    void returns201WithUrl() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.of(Map.of()));
        when(photoStorageService.store(any())).thenReturn("/uploads/equipment/abc.png");

        mockMvc.perform(multipart("/api/uploads").file(file).header("X-Init-Data", "signed"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/equipment/abc.png"));
    }

    @Test
    void returns404WhenDisabled() throws Exception {
        when(properties.photoUploadsEnabled()).thenReturn(false);

        mockMvc.perform(multipart("/api/uploads").file(file).header("X-Init-Data", "signed"))
                .andExpect(status().isNotFound());

        verify(photoStorageService, never()).store(any());
    }

    @Test
    void returns401WithoutInitDataWhenAnonDisabled() throws Exception {
        when(validator.validate(any())).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isUnauthorized());

        verify(photoStorageService, never()).store(any());
    }

    @Test
    void returns400WhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/uploads").header("X-Init-Data", "signed"))
                .andExpect(status().isBadRequest());
    }
}
