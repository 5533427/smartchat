package com.smartchat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证流程测试：注册 → 登录 → 鉴权
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueName() {
        return "user" + System.nanoTime() % 100000000;
    }

    private JsonNode postJson(String url, Object body, String token) throws Exception {
        var builder = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return perform(builder);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        var builder = get(url);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return perform(builder);
    }

    private JsonNode perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String username = uniqueName();

        // 注册
        JsonNode reg = postJson("/api/auth/register",
                java.util.Map.of("username", username, "password", "123456", "nickname", "测试用户"), null);
        assertThat(reg.path("code").asInt()).isZero();
        String token = reg.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 登录
        JsonNode login = postJson("/api/auth/login",
                java.util.Map.of("username", username, "password", "123456"), null);
        assertThat(login.path("code").asInt()).isZero();
        assertThat(login.path("data").path("user").path("username").asText()).isEqualTo(username);

        // 带 token 访问 /me
        JsonNode me = getJson("/api/auth/me", token);
        assertThat(me.path("code").asInt()).isZero();
        assertThat(me.path("data").path("username").asText()).isEqualTo(username);
    }

    @Test
    void duplicateUsernameRejected() throws Exception {
        String username = uniqueName();
        postJson("/api/auth/register",
                java.util.Map.of("username", username, "password", "123456"), null);

        JsonNode dup = postJson("/api/auth/register",
                java.util.Map.of("username", username, "password", "123456"), null);
        assertThat(dup.path("code").asInt()).isEqualTo(400);
        assertThat(dup.path("message").asText()).contains("用户名已存在");
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        String username = uniqueName();
        postJson("/api/auth/register",
                java.util.Map.of("username", username, "password", "123456"), null);

        JsonNode login = postJson("/api/auth/login",
                java.util.Map.of("username", username, "password", "wrong-pass"), null);
        assertThat(login.path("code").asInt()).isEqualTo(401);
    }

    @Test
    void protectedApiRequiresToken() throws Exception {
        // 未登录访问受保护接口 → 401
        JsonNode result = getJson("/api/stats/me", null);
        assertThat(result.path("code").asInt()).isEqualTo(401);
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
