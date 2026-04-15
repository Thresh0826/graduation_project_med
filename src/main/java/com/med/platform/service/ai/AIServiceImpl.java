package com.med.platform.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Value("${ai.qwen.api-key}")
    private String apiKey;

    @Value("${ai.qwen.model}")
    private String model;

    @Value("${ai.qwen.endpoint}")
    private String endpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String analyzeImage(String imageBase64) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return "错误：未接收到图像数据。";
        }

        // 确保 base64 字符串有 data:image/jpeg;base64, 前缀。Qwen-VL 可能需要。
        String imageUri = imageBase64;
        if (!imageUri.startsWith("data:image")) {
             // 假设前端发来的是纯 base64，添加默认前缀
             imageUri = "data:image/jpeg;base64," + imageBase64;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        // Dashscope 需要的额外 Header
        headers.set("X-DashScope-SSE", "disable");

        // 构建请求体 (根据阿里云 DashScope Qwen-VL API 规范)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        
        List<Map<String, Object>> contentList = new ArrayList<>();
        
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("image", imageUri);
        contentList.add(imageContent);
        
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("text", "你是一个专业的医学影像研究助手。请分析这张医学影像切片截图。请从以下几个维度输出结构化报告：\n1. 影像模态（如CT/MRI）及大致解剖部位；\n2. 影像质量与特征描述；\n3. 潜在的病灶或异常区域提示（仅供研究参考）。\n请使用严谨的医学术语。");
        contentList.add(textContent);

        message.put("content", contentList);
        messages.add(message);
        input.put("messages", messages);
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        // parameters.put("temperature", 0.7); // 根据需要调整参数
        requestBody.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Sending request to Qwen-VL API...");
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                if (output != null) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");
                        if (responseMessage != null) {
                            List<Map<String, Object>> responseContentList = (List<Map<String, Object>>) responseMessage.get("content");
                            if (responseContentList != null && !responseContentList.isEmpty()) {
                                // 提取文本内容
                                for (Map<String, Object> responseContent : responseContentList) {
                                    if (responseContent.containsKey("text")) {
                                         return (String) responseContent.get("text");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            log.error("Failed to parse response from Qwen-VL: {}", response.getBody());
            return "错误：调用大模型失败或返回格式无法解析。请检查控制台日志。";
        } catch (Exception e) {
            log.error("Error calling Qwen-VL API", e);
            return "错误：调用大模型期间发生异常: " + e.getMessage();
        }
    }
}
