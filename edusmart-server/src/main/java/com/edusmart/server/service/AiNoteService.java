package com.edusmart.server.service;

import com.edusmart.server.common.BusinessException;
import com.edusmart.server.dto.ai.AiNoteRequests.SummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 DashScope 原生 Generation 接口的笔记智能能力。
 *
 * <p>这里不再经过 Spring AI/OpenAI 兼容层，而是直接调用阿里云官方原生接口：
 * <code>/api/v1/services/aigc/text-generation/generation</code>。
 * 这样可以与用户提供的官方 Python SDK 示例保持一致，规避兼容层鉴权差异。</p>
 */
@Service
public class AiNoteService {

    private static final Logger log = LoggerFactory.getLogger(AiNoteService.class);

    private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-*•]|\\d+[.、)])\\s*(.+)$");
    private static final int TITLE_MAX = 50;

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public AiNoteService(
            ObjectMapper objectMapper,
            @Value("${edusmart.ai.dashscope.base-url:https://dashscope.aliyuncs.com}") String baseUrl,
            @Value("${edusmart.ai.dashscope.api-key:not-configured}") String apiKey,
            @Value("${edusmart.ai.dashscope.model:qwen-plus}") String model,
            @Value("${edusmart.ai.dashscope.timeout-seconds:30}") long timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        int connectSeconds = (int) Math.max(timeoutSeconds, 5);
        int readSeconds = (int) Math.max(timeoutSeconds * 2, 30);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(Duration.ofSeconds(connectSeconds)))
                .setResponseTimeout(Timeout.of(Duration.ofSeconds(readSeconds)))
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public String polish(String content, String subject) {
        String prompt = """
                你是一位专业的笔记润色助手。请对以下笔记内容进行深度润色和扩展，要求：
                1. 保持原意和核心观点不变
                2. 改善表达，使语言更清晰、准确、生动
                3. 修正语法错误和错别字
                4. 优化段落结构，使逻辑更清晰
                5. 保持学术性和专业性
                6. 丰富内容细节，对关键概念进行适当展开和说明
                7. 添加必要的背景信息、解释和例证，使内容更加完整和深入
                8. 如果原内容过于简略，请根据主题和上下文进行合理的扩展和补充
                9. 使用更丰富的词汇和表达方式，避免简单重复
                10. 确保润色后的内容比原文更加详细、充实、有深度

                %s
                笔记内容：
                %s

                请直接返回润色和扩展后的完整内容，不要添加任何说明或标记。
                """.formatted(subjectLine(subject), content);
        return call(prompt).trim();
    }

    public SummaryResponse summarize(String content, String subject) {
        String prompt = """
                你是一位专业的笔记总结助手。请对以下笔记内容进行总结，要求：
                1. 生成一段简洁的摘要（100-200字）
                2. 提取3-5个关键要点
                3. 提取2-4个相关标签

                %s
                笔记内容：
                %s

                请严格按照以下格式返回：
                【摘要】
                [摘要内容]

                【关键要点】
                1. [要点1]
                2. [要点2]
                ...

                【标签】
                - [标签1]
                - [标签2]
                ...
                """.formatted(subjectLine(subject), content);
        String raw = call(prompt);

        String summary = extractSection(raw, "【摘要】", "【关键要点】");
        if (summary.isBlank()) summary = raw.length() > 200 ? raw.substring(0, 200) : raw;

        String keyPointsSection = extractSection(raw, "【关键要点】", "【标签】");
        List<String> keyPoints = parseBulletList(keyPointsSection, 5);

        String tagsSection = extractSection(raw, "【标签】", null);
        List<String> tags = parseBulletList(tagsSection, 4);

        return new SummaryResponse(summary, keyPoints, tags);
    }

    public String generateTitle(String content, String subject) {
        String snippet = content == null ? "" : content.substring(0, Math.min(content.length(), 500));
        String prompt = """
                请根据以下笔记内容，生成一个简洁、准确的标题（不超过20字）。

                %s
                笔记内容：
                %s

                请直接返回标题，不要添加任何说明或引号。
                """.formatted(subjectLine(subject), snippet);
        String title = call(prompt).trim().replaceAll("^[\"'「『]|[\"'」』]$", "");
        return title.length() > TITLE_MAX ? title.substring(0, TITLE_MAX) : title;
    }

    public List<String> extractKnowledgePoints(String content, String subject) {
        String prompt = """
                请从以下笔记内容中提取关键知识点，要求：
                1. 必须提取恰好3个核心知识点，不多不少
                2. 每个知识点用一句话概括
                3. 按重要性从高到低排序
                4. 知识点应该覆盖笔记的核心内容
                5. 如果笔记内容较少，请根据主题合理推断和补充

                %s
                笔记内容：
                %s

                请严格按照以下格式返回（必须3条）：
                - [知识点1]
                - [知识点2]
                - [知识点3]
                """.formatted(subjectLine(subject), content);
        List<String> points = parseBulletList(call(prompt), 3);
        if (points.size() >= 3) return points.subList(0, 3);
        return points;
    }

    public String generateSubject(String content, String title) {
        String titleLine = (title != null && !title.isBlank()) ? "标题：" + title + "\n" : "";
        String snippet = content == null ? "" : content.substring(0, Math.min(content.length(), 1000));
        String prompt = """
                请根据以下笔记内容，判断并生成最合适的科目名称。

                %s笔记内容：
                %s

                要求：
                1. 返回一个简洁的科目名称（2-6个字），例如：数学、语文、英语、物理、化学、生物、历史、地理、政治等
                2. 如果无法判断，返回"其他"
                3. 只返回科目名称，不要添加任何说明、引号或其他文字
                4. 如果是跨学科内容，选择最主要的科目
                """.formatted(titleLine, snippet);
        String s = call(prompt).trim()
                .replace("\"", "")
                .replace("'", "")
                .replace("科目：", "")
                .replace("科目:", "")
                .replace("建议科目：", "")
                .replace("建议科目:", "")
                .trim();
        if (s.isEmpty() || s.length() > 10) return "其他";
        return s;
    }

    public String answer(String noteContent, String question) {
        String prompt = """
                你是一位学习助手。请根据以下笔记内容回答问题。

                笔记内容：
                %s

                问题：
                %s

                要求：
                1. 基于笔记内容回答，不要编造信息
                2. 如果笔记中没有相关信息，请说明
                3. 回答要准确、简洁
                4. 不要使用 Markdown 格式；可用数字序号或换行组织内容
                5. 直接返回答案，不要添加"答案："等前缀
                """.formatted(noteContent, question);
        return call(prompt).trim();
    }

    /* -------------------- helpers -------------------- */

    private String subjectLine(String subject) {
        return (subject == null || subject.isBlank()) ? "" : "科目：" + subject;
    }

    private String call(String prompt) {
        if (apiKey == null || apiKey.isBlank() || "not-configured".equals(apiKey)) {
            throw new BusinessException("AI Key 未配置，请设置 DASHSCOPE_API_KEY");
        }
        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of("role", "system", "content", "You are a helpful assistant."),
                                    Map.of("role", "user", "content", prompt)
                            )
                    ),
                    "parameters", Map.of(
                            "result_format", "message"
                    )
            );

            String requestBody = objectMapper.writeValueAsString(request);
            String endpoint = baseUrl.endsWith("/")
                    ? baseUrl + "api/v1/services/aigc/text-generation/generation"
                    : baseUrl + "/api/v1/services/aigc/text-generation/generation";

            HttpPost httpRequest = new HttpPost(endpoint);
            httpRequest.setHeader("Authorization", "Bearer " + apiKey);
            httpRequest.setHeader("Content-Type", "application/json");
            httpRequest.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                String responseBody = response.getEntity() == null
                        ? ""
                        : new String(response.getEntity().getContent().readAllBytes());
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    String message = extractDashScopeError(responseBody);
                    throw new BusinessException("AI 调用失败: HTTP " + statusCode + " - " + message);
                }

                String result = extractDashScopeContent(responseBody);
                if (result == null || result.isBlank()) {
                    throw new BusinessException("AI 返回为空");
                }
                return result;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("调用 DashScope 原生接口失败", e);
            throw new BusinessException("AI 调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用 DashScope 原生接口失败", e);
            throw new BusinessException("AI 调用失败: " + e.getMessage());
        }
    }

    private String extractDashScopeContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");
    }

    private String extractDashScopeError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String code = root.path("code").asText("");
            String message = root.path("message").asText(responseBody);
            if (!code.isBlank()) {
                return code + ": " + message;
            }
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String errCode = error.path("code").asText("");
                String errMsg = error.path("message").asText(responseBody);
                return errCode.isBlank() ? errMsg : errCode + ": " + errMsg;
            }
            return message;
        } catch (Exception ignore) {
            return responseBody;
        }
    }

    private static String extractSection(String text, String start, String end) {
        if (text == null) return "";
        int i = text.indexOf(start);
        if (i < 0) return "";
        int s = i + start.length();
        int e = end == null ? text.length() : text.indexOf(end, s);
        if (e < 0) e = text.length();
        return text.substring(s, e).trim();
    }

    private static List<String> parseBulletList(String text, int max) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            Matcher m = BULLET.matcher(line);
            if (m.matches()) {
                String v = m.group(1).trim();
                if (!v.isEmpty()) out.add(v);
            } else {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && out.size() < max
                        && trimmed.length() > 3 && trimmed.length() < 120) {
                    out.add(trimmed);
                }
            }
            if (out.size() >= max) break;
        }
        return out;
    }
}
