package ru.rugo.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Проверяет, что Spring-контекст админ-модуля поднимается (бин-бинд токенов, JPA, планировщик). */
@SpringBootTest
@ActiveProfiles("test")
class RuGoAdminApplicationTests {

    @Test
    void contextLoads() {
    }
}
