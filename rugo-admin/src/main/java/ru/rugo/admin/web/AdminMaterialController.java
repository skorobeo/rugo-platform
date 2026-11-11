package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.catalog.AdminMaterialService;
import ru.rugo.admin.web.dto.AdminMaterialDto;
import ru.rugo.admin.web.dto.AdminMaterialRequest;

import java.util.List;
import java.util.Map;

/** CRUD материалов из админки. */
@RestController
@RequestMapping("/admin/api/materials")
@RequiredArgsConstructor
public class AdminMaterialController {

    private final AdminMaterialService materialService;

    @GetMapping
    public List<AdminMaterialDto> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return materialService.list(type, status);
    }

    @GetMapping("/{id}")
    public AdminMaterialDto get(@PathVariable Long id) {
        return materialService.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody AdminMaterialRequest request) {
        Long id = materialService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(@PathVariable Long id, @Valid @RequestBody AdminMaterialRequest request) {
        return Map.of("id", materialService.update(id, request));
    }

    @PostMapping("/{id}/status")
    public Map<String, String> setStatus(@PathVariable Long id, @RequestParam String value) {
        return Map.of("status", materialService.setStatus(id, value));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
