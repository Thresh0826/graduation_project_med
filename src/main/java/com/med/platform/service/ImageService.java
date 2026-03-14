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
import java.util.Collections;

@Service
public class ImageService {

    @Autowired
    private MedImageMapper imageMapper;

    @Autowired
    private PythonBridgeService pythonBridge;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public MedImage handleUpload(MultipartFile file, SysUser user, Integer visibility) throws IOException {
        if ((user.getGroupId() == null || user.getGroupId() <= 0) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("请先加入课题组方可上传数据");
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new IOException("非法文件名");
        }
        fileName = new File(fileName).getName();

        Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = rootLocation.toFile();
        if(!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        file.transferTo(dest);

        MedImage medImage = new MedImage();
        medImage.setFileName(fileName);
        medImage.setFilePath(fileName); 
        medImage.setFileSize(file.getSize());
        medImage.setOwnerName(user.getUsername());
        
        medImage.setGroupId(user.getGroupId() != null ? user.getGroupId() : 0L);
        medImage.setVisibility(visibility != null ? visibility : 1); 
        medImage.setStatus(0); 

        try {
            Map<String, Object> info = pythonBridge.inspectImage(fileName);
            if (info != null && "success".equals(info.get("status"))) {
                medImage.setFormat((String) info.get("format"));
                List<Integer> shape = (List<Integer>) info.get("shape");
                medImage.setDimX(((Number) shape.get(0)).intValue());
                medImage.setDimY(((Number) shape.get(1)).intValue());
                medImage.setDimZ(((Number) shape.get(2)).intValue());
                if (info.containsKey("spacing")) {
                    List<Double> spacing = (List<Double>) info.get("spacing");
                    medImage.setVoxResX(((Number) spacing.get(0)).doubleValue());
                    medImage.setVoxResY(((Number) spacing.get(1)).doubleValue());
                    medImage.setVoxResZ(((Number) spacing.get(2)).doubleValue());
                }
                if (info.containsKey("modality")) {
                    medImage.setModality((String) info.get("modality"));
                }
            } 
            imageMapper.insert(medImage);
            return medImage;
        } catch (Exception e) {
            e.printStackTrace();
            imageMapper.insert(medImage); 
            return medImage;
        }
    }

    public void markAsParsed(String fileName) {
        imageMapper.updateStatusToParsed(fileName);
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

    public void deleteImage(Long id) {
        MedImage image = imageMapper.selectById(id);
        if (image == null) throw new RuntimeException("文件不存在");

        int result = imageMapper.logicalDelete(id);
        if (result == 0) throw new RuntimeException("删除失败：记录状态异常。");
        
        // 物理删除文件。注意如果存在多条记录指向同一文件可能误删。此处为毕设演示保留。
        if (image.getFilePath() != null) {
            Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            File file = new File(rootLocation.toFile(), image.getFilePath()); 
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    public void updateVisibility(Long id, Integer visibility, SysUser user) {
        MedImage image = imageMapper.selectById(id);
        if (image == null) throw new RuntimeException("文件不存在");
        
        if (user.getIsLeader() != null && user.getIsLeader() == 1 && image.getGroupId().equals(user.getGroupId())) {
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