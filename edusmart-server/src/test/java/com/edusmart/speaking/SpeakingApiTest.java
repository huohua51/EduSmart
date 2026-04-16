package com.edusmart.speaking;

import com.edusmart.security.JwtService;
import com.edusmart.user.User;
import com.edusmart.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 口语模块集成测试
 * 测试完整的 REST API 流程：认证 → 创建会话 → 添加消息 → 查询 → 统计 → 删除
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpeakingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SpeakingSessionRepository sessionRepository;

    @Autowired
    private SpeakingMessageRepository messageRepository;

    private String token;
    private Long userId;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        // 创建测试用户
        User user = new User();
        user.setEmail("test@edusmart.com");
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user = userRepository.save(user);
        userId = user.getId();

        // 生成 JWT token
        token = jwtService.createToken(userId, "test@edusmart.com");
    }

    // =================== 创建会话 ===================

    @Test
    void createSession_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "learningPurpose", "雅思考试",
                "scene", "机场值机",
                "duration", 0
        ));

        mockMvc.perform(post("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.learningPurpose").value("雅思考试"))
                .andExpect(jsonPath("$.scene").value("机场值机"))
                .andExpect(jsonPath("$.messageCount").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createSession_withCustomTopic() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "learningPurpose", "出国旅游",
                "scene", "自定义话题",
                "customTopic", "讨论人工智能的未来",
                "duration", 0
        ));

        mockMvc.perform(post("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customTopic").value("讨论人工智能的未来"));
    }

    @Test
    void createSession_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "learningPurpose", "雅思考试",
                "scene", "机场值机"
        ));

        mockMvc.perform(post("/api/speaking/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // =================== 添加消息 ===================

    @Test
    void addMessage_aiAndUser() throws Exception {
        // 先创建会话
        Long sessionId = createTestSession("托福考试", "校园生活");

        // 添加 AI 开场消息
        String aiMsg = objectMapper.writeValueAsString(Map.of(
                "role", "AI",
                "content", "Hello! Let's talk about campus life."
        ));

        mockMvc.perform(post("/api/speaking/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiMsg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AI"))
                .andExpect(jsonPath("$.content").value("Hello! Let's talk about campus life."))
                .andExpect(jsonPath("$.score").isEmpty());

        // 添加用户消息（带评分和翻译）
        String userMsg = objectMapper.writeValueAsString(Map.of(
                "role", "User",
                "content", "I really enjoy the library on campus.",
                "translation", "我真的很喜欢校园里的图书馆。",
                "score", 85.5
        ));

        mockMvc.perform(post("/api/speaking/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userMsg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("User"))
                .andExpect(jsonPath("$.score").value(85.5))
                .andExpect(jsonPath("$.translation").value("我真的很喜欢校园里的图书馆。"));
    }

    @Test
    void addMessage_sessionNotFound() throws Exception {
        String msg = objectMapper.writeValueAsString(Map.of(
                "role", "AI",
                "content", "Hello"
        ));

        mockMvc.perform(post("/api/speaking/sessions/99999/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(msg))
                .andExpect(status().isNotFound());
    }

    @Test
    void addMessage_updatesMessageCount() throws Exception {
        Long sessionId = createTestSession("雅思考试", "旅行");

        // 添加 2 条消息
        addTestMessage(sessionId, "AI", "Welcome!", null);
        addTestMessage(sessionId, "User", "Thank you!", 78.0f);

        // 验证 messageCount 更新
        mockMvc.perform(get("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2));
    }

    // =================== 查询会话列表 ===================

    @Test
    void listSessions_empty() throws Exception {
        mockMvc.perform(get("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listSessions_multipleSessions() throws Exception {
        createTestSession("雅思考试", "机场值机");
        createTestSession("出国旅游", "餐厅点餐");
        createTestSession("商务交流", "会议讨论");

        mockMvc.perform(get("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void listSessions_userIsolation() throws Exception {
        // 当前用户创建一个会话
        createTestSession("雅思考试", "机场值机");

        // 创建另一个用户
        User user2 = new User();
        user2.setEmail("user2@edusmart.com");
        user2.setUsername("user2");
        user2.setPasswordHash(passwordEncoder.encode("password123"));
        user2 = userRepository.save(user2);
        String token2 = jwtService.createToken(user2.getId(), "user2@edusmart.com");

        // 另一个用户查看列表应为空
        mockMvc.perform(get("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =================== 获取会话详情 ===================

    @Test
    void getSession_withMessages() throws Exception {
        Long sessionId = createTestSession("雅思考试", "机场值机");
        addTestMessage(sessionId, "AI", "Hello, how can I help you?", null);
        addTestMessage(sessionId, "User", "I need to check in for my flight.", 92.0f);

        mockMvc.perform(get("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.learningPurpose").value("雅思考试"))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("AI"))
                .andExpect(jsonPath("$.messages[1].role").value("User"))
                .andExpect(jsonPath("$.messages[1].score").value(92.0));
    }

    @Test
    void getSession_notFound() throws Exception {
        mockMvc.perform(get("/api/speaking/sessions/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // =================== 删除会话 ===================

    @Test
    void deleteSession_success() throws Exception {
        Long sessionId = createTestSession("雅思考试", "机场值机");
        addTestMessage(sessionId, "AI", "Hello!", null);

        // 删除
        mockMvc.perform(delete("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 验证已删除
        mockMvc.perform(get("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSession_cascadeDeletesMessages() throws Exception {
        Long sessionId = createTestSession("雅思考试", "机场值机");
        addTestMessage(sessionId, "AI", "Hello!", null);
        addTestMessage(sessionId, "User", "Hi there!", 80.0f);

        // 确认消息存在
        mockMvc.perform(get("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.messages", hasSize(2)));

        // 删除会话
        mockMvc.perform(delete("/api/speaking/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 确认列表为空
        mockMvc.perform(get("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteSession_notFound() throws Exception {
        mockMvc.perform(delete("/api/speaking/sessions/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // =================== 学习统计 ===================

    @Test
    void getStats_empty() throws Exception {
        mockMvc.perform(get("/api/speaking/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(0))
                .andExpect(jsonPath("$.totalDuration").value(0))
                .andExpect(jsonPath("$.averageScore").value(0.0));
    }

    @Test
    void getStats_withData() throws Exception {
        // 创建两个会话
        Long s1 = createTestSession("雅思考试", "机场值机");
        Long s2 = createTestSession("托福考试", "校园生活");

        // 添加带评分的消息
        addTestMessage(s1, "User", "msg1", 80.0f);
        addTestMessage(s1, "User", "msg2", 90.0f);
        addTestMessage(s2, "User", "msg3", 70.0f);
        // AI 消息不带评分
        addTestMessage(s1, "AI", "ai reply", null);

        mockMvc.perform(get("/api/speaking/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(2))
                .andExpect(jsonPath("$.averageScore").value(80.0)); // (80+90+70)/3 = 80
    }

    @Test
    void getStats_favoriteScene() throws Exception {
        // 机场值机出现2次，餐厅点餐出现1次
        createTestSession("雅思考试", "机场值机");
        createTestSession("雅思考试", "机场值机");
        createTestSession("出国旅游", "餐厅点餐");

        mockMvc.perform(get("/api/speaking/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteScene").value("机场值机"));
    }

    // =================== 辅助方法 ===================

    private Long createTestSession(String purpose, String scene) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "learningPurpose", purpose,
                "scene", scene,
                "duration", 60000
        ));

        MvcResult result = mockMvc.perform(post("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void addTestMessage(Long sessionId, String role, String content, Float score) throws Exception {
        var map = new java.util.HashMap<String, Object>();
        map.put("role", role);
        map.put("content", content);
        if (score != null) {
            map.put("score", score);
        }

        mockMvc.perform(post("/api/speaking/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map)))
                .andExpect(status().isOk());
    }
}
