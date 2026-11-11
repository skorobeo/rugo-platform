package ru.rugo.app.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.rugo.common.max.webhook.MaxSubscriptionManager;
import ru.rugo.core.repository.PartnerLeadRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий кабинета без реального MAX: H2 + валидный initData.
 * Проверяет, что подряд можно добавить технику, материал и ещё одну технику.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "max.bot.token=test-token",
        "app.contact-signature-required=false"
})
class PartnerCabinetFlowIntegrationTest {

    private static final long MAX_USER_ID = 900001L;
    private static final String BOT_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartnerLeadRepository leadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaxSubscriptionManager maxSubscriptionManager;

    @Test
    void partnerCanAddEquipmentMaterialAndEquipmentAgain() throws Exception {
        String initData = initData(MAX_USER_ID);

        mockMvc.perform(post("/api/partner/phone")
                        .header("X-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+79001112233\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true));

        postEquipment(initData, "Самосвал", 10, 2000);
        postMaterial(initData);
        postEquipment(initData, "Газель", 2, 1000);

        assertEquals(3, leadRepository.count());

        String response = mockMvc.perform(get("/api/partner/leads").header("X-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode leads = objectMapper.readTree(response);
        for (JsonNode lead : leads) {
            assertEquals("new", lead.get("status").asText());
        }
    }

    private void postEquipment(String initData, String category, int capacity, int price) throws Exception {
        mockMvc.perform(post("/api/partner/equipment")
                        .header("X-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"" + category + "\",\"capacity\":" + capacity
                                + ",\"price\":" + price + ",\"priceUnit\":\"hour\"}"))
                .andExpect(status().isCreated());
    }

    private void postMaterial(String initData) throws Exception {
        mockMvc.perform(post("/api/partner/materials")
                        .header("X-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Дрова\",\"price\":2000,\"unit\":\"м³\","
                                + "\"deliveryType\":\"DELIVERY\"}"))
                .andExpect(status().isCreated());
    }

    private static String initData(long userId) {
        String authDate = String.valueOf(Instant.now().getEpochSecond());
        String user = "{\"id\":" + userId + ",\"first_name\":\"Тест\"}";
        String launchParams = "auth_date=" + authDate + "\nuser=" + user;
        String signature = hexHmac(hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                BOT_TOKEN.getBytes(StandardCharsets.UTF_8)), launchParams);
        return "auth_date=" + authDate
                + "&user=" + URLEncoder.encode(user, StandardCharsets.UTF_8)
                + "&hash=" + signature;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hexHmac(byte[] key, String data) {
        return HexFormat.of().formatHex(hmac(key, data.getBytes(StandardCharsets.UTF_8)));
    }
}
