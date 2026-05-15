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
        textContent.put("text",
            "你是一个专业的医学影像科研分析助手，你的服务对象是从事医学影像研究的科研人员" +
            "（包括影像组学、深度学习、生物医学工程等领域的研究者）。" +
            "请基于这张医学影像切片截图，从科研视角输出一份结构化的分析报告，涵盖以下维度：\n\n" +
            "## 1. 影像数据概况\n" +
            "- 推测的成像模态（CT/MRI/PET/超声等）及其判断依据\n" +
            "- 大致解剖区域或组织类型\n" +
            "- 图像矩阵尺寸、灰度位深等基本参数估计\n\n" +
            "## 2. 图像质量与预处理潜力\n" +
            "- 信噪比（SNR）与对比度噪声比（CNR）的定性评估\n" +
            "- 是否存在运动伪影、金属伪影或部分容积效应\n" +
            "- 针对该图像的预处理建议（如偏场校正、配准、归一化策略）\n\n" +
            "## 3. 科研特征分析\n" +
            "- 可辨识的组织结构及其在影像组学中的潜在价值\n" +
            "- 纹理特征、形态学特征的初步观察\n" +
            "- 若存在异常信号区域，从影像标志物角度进行描述（仅供科研参考，不构成临床诊断）\n\n" +
            "## 4. 研究方向建议\n" +
            "- 该类型影像适合开展哪些科研方向（如分割、分类、配准、生成模型等）\n" +
            "- 建议关注的相关文献领域或公开数据集\n" +
            "- 在深度学习训练中可能的注意事项（如类别不平衡、标注难度等）\n\n" +
            "请使用专业、客观的学术语言撰写，避免临床诊断性表述，强调科研探索价值。");
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
