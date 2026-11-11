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
import ru.rugo.admin.catalog.AdminCatalogService;
import ru.rugo.admin.web.dto.AdminEquipmentDto;
import ru.rugo.admin.web.dto.AdminEquipmentRequest;

import java.util.List;
import java.util.Map;

/** CRUD техники из админки. Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/equipment")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService catalogService;

    @GetMapping
    public List<AdminEquipmentDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return catalogService.list(category, status);
    }

    @GetMapping("/{id}")
    public AdminEquipmentDto get(@PathVariable Long id) {
        return catalogService.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody AdminEquipmentRequest request) {
        Long id = catalogService.addEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(@PathVariable Long id, @Valid @RequestBody AdminEquipmentRequest request) {
        return Map.of("id", catalogService.updateEquipment(id, request));
    }

    @PostMapping("/{id}/approve")
    public Map<String, String> approve(@PathVariable Long id) {
        return Map.of("status", catalogService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public Map<String, String> reject(@PathVariable Long id) {
        return Map.of("status", catalogService.reject(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
