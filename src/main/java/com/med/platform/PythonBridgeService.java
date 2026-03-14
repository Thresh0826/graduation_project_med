package com.med.platform;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PythonBridgeService {

    // 从配置文件读取Python地址
    @Value("${python.service.url:http://127.0.0.1:8000}")
    private String pythonUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用Python获取影像信息
     */
    public Map<String, Object> inspectImage(String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String url = pythonUrl + "/api/inspect?filename=" + encodedFilename;
        return restTemplate.getForObject(url, Map.class);
    }

    /**
     * 调用Python获取切片数据
     */
    public Map<String, Object> getSlice(String filename, String axis, int index, float ww, float wl) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String url = String.format("%s/api/slice?filename=%s&axis=%s&index=%d&ww=%f&wl=%f",
                pythonUrl, encodedFilename, axis, index, ww, wl);
        return restTemplate.getForObject(url, Map.class);
    }
}