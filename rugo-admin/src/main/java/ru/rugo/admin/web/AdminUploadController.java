package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.rugo.core.storage.PhotoStorageService;

import java.util.Map;

/** Загрузка фото из админки (те же проверки, что у партнёров). Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final PhotoStorageService photoStorageService;

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", photoStorageService.store(file)));
    }
}
