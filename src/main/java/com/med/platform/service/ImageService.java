package com.med.platform.service;

import com.med.platform.PythonBridgeService;
import com.med.platform.entity.MedImage;
import com.med.platform.mapper.MedImageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class ImageService {

    @Autowired
    private MedImageMapper imageMapper;

    @Autowired
    private PythonBridgeService pythonBridge;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public MedImage handleUpload(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        // 【关键修复】：将路径转换为绝对路径，避免被甩到 Tomcat 临时目录
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = uploadPath.toFile();
        if(!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);

        // 保存文件
        file.transferTo(dest);

        try {
            Map<String, Object> info = pythonBridge.inspectImage(fileName);
            MedImage medImage = new MedImage();
            medImage.setFileName(fileName);
            medImage.setFilePath(dest.getAbsolutePath());
            medImage.setFileSize(file.getSize());

            if (info != null && "success".equals(info.get("status"))) {
                medImage.setFormat((String) info.get("format"));
                List<Integer> shape = (List<Integer>) info.get("shape");
                medImage.setDimX(((Number) shape.get(0)).intValue());
                medImage.setDimY(((Number) shape.get(1)).intValue());
                medImage.setDimZ(((Number) shape.get(2)).intValue());
            }
            imageMapper.insert(medImage);
            return medImage;
        } catch (Exception e) {
            throw new IOException("Python 解析失败: " + e.getMessage());
        }
    }

    public List<MedImage> getAllImages() {
        return imageMapper.findAll();
    }
}