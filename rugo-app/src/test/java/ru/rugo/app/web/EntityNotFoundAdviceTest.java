package ru.rugo.app.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.common.exception.EntityNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** V-020: EntityNotFoundException в rugo-app должен давать 404, а не 500. */
class EntityNotFoundAdviceTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/test/not-found")
        ResponseEntity<Void> get() {
            throw new EntityNotFoundException("Техника #1 не найдена");
        }
    }

    @Test
    void mapsEntityNotFoundTo404WithoutInternalDetails() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.request").value("Не найдено"));
    }
}
