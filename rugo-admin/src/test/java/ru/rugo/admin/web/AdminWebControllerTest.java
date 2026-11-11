package ru.rugo.admin.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Веб-API админки. Авторизация (фильтр) проверяется отдельно в AdminAuthFilterTest. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsLeads() throws Exception {
        mockMvc.perform(get("/admin/api/leads"))
                .andExpect(status().isOk());
    }

    @Test
    void listsAllLeads() throws Exception {
        mockMvc.perform(get("/admin/api/leads?status=all"))
                .andExpect(status().isOk());
    }

    @Test
    void reopenEndpointWired() throws Exception {
        mockMvc.perform(post("/admin/api/leads/999999/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists());
    }

    @Test
    void statsEndpointWired() throws Exception {
        mockMvc.perform(get("/admin/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniqueUsers").exists())
                .andExpect(jsonPath("$.events").isArray());
    }
}
