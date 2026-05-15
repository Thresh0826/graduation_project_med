package com.med.platform;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PythonBridgeService {

    @Value("${python.service.url:http://127.0.0.1:8000}")
    private String pythonUrl;

    private final RestTemplate restTemplate;

    public PythonBridgeService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);    // 5 秒连接超时
        factory.setReadTimeout(30000);      // 30 秒读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用Python获取影像信息
     */
    public Map<String, Object> inspectImage(String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String url = pythonUrl + "/api/inspect?filename=" + encodedFilename;
        return restTemplate.getForObject(url, Map.class);
    }

    /**
     * 调用Python获取切片数据（可选Mask叠加）
     */
    public Map<String, Object> getSlice(String filename, String axis, int index, float ww, float wl,
                                         String maskFilename, float alpha) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String encodedAxis = URLEncoder.encode(axis, StandardCharsets.UTF_8);
        String url = String.format("%s/api/slice?filename=%s&axis=%s&index=%d&ww=%f&wl=%f",
                pythonUrl, encodedFilename, encodedAxis, index, ww, wl);
        if (maskFilename != null && !maskFilename.isEmpty()) {
            url += "&mask_filename=" + URLEncoder.encode(maskFilename, StandardCharsets.UTF_8);
            url += "&alpha=" + alpha;
        }
        return restTemplate.getForObject(url, Map.class);
    }
}