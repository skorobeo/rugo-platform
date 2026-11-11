package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.catalog.AdminUserService;
import ru.rugo.admin.web.dto.AdminUserDto;

import java.util.List;
import java.util.Map;

/** Просмотр и управление пользователями. */
@RestController
@RequestMapping("/admin/api/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService userService;

    @GetMapping
    public List<AdminUserDto> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return userService.list(role, status);
    }

    @PostMapping("/{id}/status")
    public Map<String, String> setStatus(@PathVariable Long id, @RequestParam String value) {
        userService.setStatus(id, value);
        return Map.of("result", "обновлено");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
