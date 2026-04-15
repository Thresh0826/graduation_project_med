package com.med.platform.service.ai;

public interface AIService {
    /**
     * 调用外部视觉大模型分析医学影像截图
     * @param imageBase64 前端截取的 Base64 编码的图像数据
     * @return AI 生成的分析报告 (Markdown 格式)
     */
    String analyzeImage(String imageBase64);
}
