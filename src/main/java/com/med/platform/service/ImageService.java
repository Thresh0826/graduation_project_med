package com.med.platform.service;

import com.med.platform.PythonBridgeService;
import com.med.platform.entity.MedImage;
import com.med.platform.entity.SysUser;
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
import java.util.HashMap;
import java.util.Objects;
import java.util.Collections;
import java.util.UUID;
import java.util.Random;

@Service
public class ImageService {

    @Autowired
    private MedImageMapper imageMapper;

    @Autowired
    private PythonBridgeService pythonBridge;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final Random random = new Random();

    public MedImage handleUpload(MultipartFile file, SysUser user, Integer visibility) throws IOException {
        if ((user.getGroupId() == null || user.getGroupId() <= 0) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("请先加入课题组方可上传数据");
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new IOException("非法文件名");
        }
        fileName = new File(fileName).getName();
        String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + fileName;

        Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = rootLocation.toFile();
        if(!dir.exists()) dir.mkdirs();

        File dest = new File(dir, storedName);
        file.transferTo(dest);

        MedImage medImage = new MedImage();
        medImage.setFileName(fileName);
        medImage.setFilePath(storedName); 
        medImage.setFileSize(file.getSize());
        medImage.setOwnerName(user.getUsername());
        
        medImage.setGroupId(user.getGroupId() != null ? user.getGroupId() : 0L);
        medImage.setVisibility(visibility != null ? visibility : 1); 
        medImage.setStatus(0); 

        try {
            Map<String, Object> info = pythonBridge.inspectImage(storedName);
            if (info != null && "success".equals(info.get("status"))) {
                medImage.setFormat((String) info.get("format"));
                List<?> shape = (List<?>) info.get("shape");
                medImage.setDimX(((Number) shape.get(0)).intValue());
                medImage.setDimY(((Number) shape.get(1)).intValue());
                medImage.setDimZ(((Number) shape.get(2)).intValue());
                if (info.containsKey("spacing")) {
                    List<?> spacing = (List<?>) info.get("spacing");
                    medImage.setVoxResX(((Number) spacing.get(0)).doubleValue());
                    medImage.setVoxResY(((Number) spacing.get(1)).doubleValue());
                    medImage.setVoxResZ(((Number) spacing.get(2)).doubleValue());
                }
                if (info.containsKey("modality")) {
                    medImage.setModality((String) info.get("modality"));
                }
            }

            // 【需求七】在获取影像尺寸后生成 Mock 病灶数据
            generateMockLesionData(medImage);

            imageMapper.insert(medImage);
            return medImage;
        } catch (Exception e) {
            // Clean up the uploaded file on failure
            try { dest.delete(); } catch (Exception ignored) {}
            throw new IOException("影像解析失败，Python 服务异常: " + e.getMessage(), e);
        }
    }

    /**
     * 【需求七】为影像生成随机的 Mock 病灶数据
     * 50% 概率生成一个椭圆区域的坐标信息
     * 优化：病灶集中在图像中央区域（40%-60%），尺寸更小（2%-8%）
     */
    private void generateMockLesionData(MedImage medImage) {
        // 50% 概率有病灶
        boolean hasLesion = random.nextBoolean();
        medImage.setHasMockLesion(hasLesion);
        
        if (hasLesion && medImage.getDimX() != null && medImage.getDimY() != null) {
            // 随机生成椭圆参数（基于影像尺寸的相对位置）
            int dimX = medImage.getDimX();
            int dimY = medImage.getDimY();
            
            // 中心点：集中在图像中央区域（40%-60% 范围）
            // 这样病灶会出现在实际解剖结构附近，而不是黑色背景区域
            double centerX = dimX * (0.40 + random.nextDouble() * 0.20);
            double centerY = dimY * (0.40 + random.nextDouble() * 0.20);
            
            // 长短轴：影像尺寸的 2%-8%（更小的病灶，更真实）
            double radiusX = dimX * (0.02 + random.nextDouble() * 0.06);
            double radiusY = dimY * (0.02 + random.nextDouble() * 0.06);
            
            // 构建 JSON 格式的病灶数据
            String lesionJson = String.format(
                "{\"centerX\":%.2f,\"centerY\":%.2f,\"radiusX\":%.2f,\"radiusY\":%.2f,\"type\":\"ellipse\"}",
                centerX, centerY, radiusX, radiusY
            );
            medImage.setMockLesionData(lesionJson);
            
            // 日志输出（调试用）
            System.out.println("[Mock 病灶] 生成病灶数据: " + lesionJson);
        } else {
            medImage.setMockLesionData(null);
        }
    }

    public void markAsParsed(Long id) {
        imageMapper.updateStatusToParsed(id);
    }

    public List<MedImage> getAllImages(SysUser user) {
        if (user == null) return Collections.emptyList(); 

        if ("admin".equals(user.getRole())) {
            return imageMapper.findAll();
        }
        
        if (user.getGroupId() == null) {
             return imageMapper.findVisibleByGroup(0L); 
        }

        return imageMapper.findVisibleByGroup(user.getGroupId());
    }

    public void deleteImage(Long id, SysUser user) {
        MedImage image = imageMapper.selectById(id);
        if (image == null) throw new RuntimeException("文件不存在");

        // 权限校验：管理员可删除所有，组长仅可删除本组影像
        if (!"admin".equals(user.getRole())) {
            if (user.getIsLeader() == null || user.getIsLeader() == 0) {
                throw new RuntimeException("权限不足，只有管理员或组长可以删除影像。");
            }
            if (image.getGroupId() == null || !image.getGroupId().equals(user.getGroupId())) {
                throw new RuntimeException("权限不足，只能删除本课题组影像。");
            }
        }

        int result = imageMapper.logicalDelete(id);
        if (result == 0) throw new RuntimeException("删除失败：记录状态异常。");

        if (image.getFilePath() != null) {
            Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            File file = new File(rootLocation.toFile(), image.getFilePath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("警告：无法删除物理文件 " + file.getAbsolutePath());
                }
            }
        }
    }
    
    public void updateVisibility(Long id, Integer visibility, SysUser user) {
        MedImage image = imageMapper.selectById(id);
        if (image == null) throw new RuntimeException("文件不存在");
        
        if (user.getIsLeader() != null && user.getIsLeader() == 1 && Objects.equals(image.getGroupId(), user.getGroupId())) {
            imageMapper.updateVisibility(id, visibility);
        } else if ("admin".equals(user.getRole())) {
            imageMapper.updateVisibility(id, visibility);
        } else {
            throw new RuntimeException("权限不足，仅组长可修改权限");
        }
    }
    
    // 【修改核心】加入基于角色的数据隔离
    public Map<String, Object> getDashboardStats(SysUser user) {
        Map<String, Object> stats = new HashMap<>();
        if (user == null) return stats;
        
        boolean isAdmin = "admin".equals(user.getRole());
        Long groupId = user.getGroupId();

        stats.put("totalImages", imageMapper.countTotal(groupId, isAdmin));
        stats.put("modalityDistribution", imageMapper.countByModality(groupId, isAdmin));
        stats.put("formatDistribution", imageMapper.countByFormat(groupId, isAdmin));
        stats.put("statusDistribution", imageMapper.countByStatus(groupId, isAdmin));
        return stats;
    }
}