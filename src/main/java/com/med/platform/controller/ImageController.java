package com.med.platform.controller;

import com.med.platform.entity.MedImage;
import com.med.platform.service.ImageService;
import com.med.platform.PythonBridgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private PythonBridgeService pythonBridge; // 注入通讯兵

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("请选择文件");
        try {
            MedImage result = imageService.handleUpload(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public List<MedImage> list() {
        return imageService.getAllImages();
    }

    /**
     * 【新增】获取切片接口
     * 访问地址: http://localhost:8080/api/image/view?file=xxx.nii.gz&index=20
     */
    // 仅需更新 viewSlice 方法
    @GetMapping("/view")
    public ResponseEntity<?> viewSlice(
            @RequestParam String file,
            @RequestParam(defaultValue = "axial") String axis,
            @RequestParam(defaultValue = "0") int index,
            @RequestParam(defaultValue = "400") float ww,
            @RequestParam(defaultValue = "40") float wl) {
        try {
            // 喊通讯兵去找 Python
            Map<String, Object> result = pythonBridge.getSlice(file, axis, index, ww, wl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 如果 Python 那边崩了，把原因传回前端
            System.err.println("查看切片失败: " + e.getMessage());
            return ResponseEntity.status(500).body("获取切片失败，请检查 Python 服务日志。");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            imageService.deleteImage(id); // 稍后在 Service 中实现
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("删除失败: " + e.getMessage());
        }
    }

}