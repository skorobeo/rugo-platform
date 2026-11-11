package ru.rugo.core.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Сохраняет загруженное фото на диск. Имя файла — UUID (без пользовательских данных),
 * папка — {@code uploads/equipment/}. Проверяются тип, размер и сигнатура (magic bytes),
 * чтобы нельзя было загрузить не-картинку под видом картинки.
 * Общий для rugo-app (партнёры) и rugo-admin (админ).
 */
@Service
@RequiredArgsConstructor
public class PhotoStorageService {

    public static final String DIR = "uploads/equipment";
    public static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AppProperties properties;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiValidationException(Map.of("file", "Файл не выбран"));
        }
        long maxBytes = properties.photoMaxBytes();
        if (file.getSize() > maxBytes) {
            throw new ApiValidationException(Map.of("file", "Файл больше " + (maxBytes / (1024 * 1024)) + " МБ"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiValidationException(Map.of("file", "Только JPEG, PNG или WebP"));
        }
        if (!matchesSignature(file, contentType)) {
            throw new ApiValidationException(Map.of("file", "Повреждённый файл изображения"));
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String name = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Path dir = Paths.get(DIR);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name).toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл", e);
        }
        return "/" + DIR + "/" + name;
    }

    private boolean matchesSignature(MultipartFile file, String contentType) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (head.length < 12) {
                return false;
            }
            return switch (contentType) {
                case "image/jpeg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
                case "image/png" -> (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
                case "image/webp" -> head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }
}
