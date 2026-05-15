package com.med.platform.controller;

import com.med.platform.config.LogAction; 
import com.med.platform.entity.MedImage;
import com.med.platform.entity.SysUser;
import com.med.platform.service.ImageService;
import com.med.platform.service.ai.AIService;
import com.med.platform.controller.dto.AnalysisRequest;
import com.med.platform.controller.dto.AnalysisResponse;
import com.med.platform.PythonBridgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession; 
import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private PythonBridgeService pythonBridge; 

    @Autowired
    private AIService aiService;

    @PostMapping("/upload")
    @LogAction(module = "影像管理", action = "上传影像")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "maskFile", required = false) MultipartFile maskFile,
                                    @RequestParam(value = "visibility", defaultValue = "1") Integer visibility,
                                    HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.status(401).body("请先登录");

        if (file.isEmpty()) return ResponseEntity.badRequest().body("请选择文件");
        try {
            MedImage result = imageService.handleUpload(file, maskFile, currentUser, visibility);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public List<MedImage> list(HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) return Collections.emptyList();
        return imageService.getAllImages(currentUser);
    }
    
    @PutMapping("/visibility/{id}")
    @LogAction(module = "影像管理", action = "修改权限")
    public ResponseEntity<?> updateVisibility(@PathVariable Long id, @RequestParam Integer visibility, HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.status(401).body("请先登录");
        
        try {
            imageService.updateVisibility(id, visibility, currentUser);
            return ResponseEntity.ok("权限修改成功");
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/stats")
    @LogAction(module = "数据看板", action = "查看报表") 
    public ResponseEntity<?> stats(HttpSession session) {
        // 【修改核心】获取当前用户并传入 Service
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.status(401).body("请先登录");

        try {
            Map<String, Object> stats = imageService.getDashboardStats(currentUser);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("统计数据获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<?> viewSlice(
            @RequestParam String file,
            @RequestParam(defaultValue = "axial") String axis,
            @RequestParam(defaultValue = "0") int index,
            @RequestParam(defaultValue = "400") float ww,
            @RequestParam(defaultValue = "40") float wl,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String maskFile,
            @RequestParam(defaultValue = "0.4") float alpha) {
        try {
            Map<String, Object> result = pythonBridge.getSlice(file, axis, index, ww, wl, maskFile, alpha);
            if (id != null) {
                imageService.markAsParsed(id);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("查看切片失败: " + e.getMessage());
            return ResponseEntity.status(500).body("获取切片失败，请检查 Python 服务日志。");
        }
    }

    @DeleteMapping("/delete/{id}")
    @LogAction(module = "影像管理", action = "删除影像")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            imageService.deleteImage(id, currentUser);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/analyze")
    @LogAction(module = "影像解析", action = "AI 智能分析影像切片")
    public ResponseEntity<?> analyzeImage(@RequestBody AnalysisRequest request, HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        if (request == null || request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
            return ResponseEntity.badRequest().body("请求参数错误：缺失图像数据");
        }

        try {
            String report = aiService.analyzeImage(request.getImageBase64());
            return ResponseEntity.ok(new AnalysisResponse(report));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("AI 分析过程出现异常: " + e.getMessage());
        }
    }
}
