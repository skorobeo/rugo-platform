package ru.rugo.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.storage.PhotoStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoStorageServiceTest {

    @Mock
    private AppProperties properties;

    private PhotoStorageService service;

    @BeforeEach
    void setUp() {
        service = new PhotoStorageService(properties);
        lenient().when(properties.photoMaxBytes()).thenReturn(5L * 1024 * 1024);
    }

    private static byte[] pngHeader() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    }

    @Test
    void storesValidPng() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngHeader());

        String url = service.store(file);

        assertTrue(url.startsWith("/uploads/equipment/"));
        assertTrue(url.endsWith(".png"));

        Path saved = Paths.get(PhotoStorageService.DIR, url.substring(url.lastIndexOf('/') + 1));
        assertTrue(Files.exists(saved));
        Files.deleteIfExists(saved);
    }

    @Test
    void rejectsWrongContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        assertThrows(ApiValidationException.class, () -> service.store(file));
    }

    @Test
    void rejectsOversizeFile() {
        when(properties.photoMaxBytes()).thenReturn(4L);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngHeader());

        assertThrows(ApiValidationException.class, () -> service.store(file));
    }

    @Test
    void rejectsFileWithWrongSignature() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "not a real image at all".getBytes());

        assertThrows(ApiValidationException.class, () -> service.store(file));
    }
}
