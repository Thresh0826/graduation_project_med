package com.med.platform.controller;

import com.med.platform.config.LogAction; 
import com.med.platform.entity.MedImage;
import com.med.platform.entity.SysUser;
import com.med.platform.service.ImageService;
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

    @PostMapping("/upload")
    @LogAction(module = "影像管理", action = "上传影像") 
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, 
                                    @RequestParam(value = "visibility", defaultValue = "1") Integer visibility,
                                    HttpSession session) {
        SysUser currentUser = (SysUser) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.status(401).body("请先登录");

        if (file.isEmpty()) return ResponseEntity.badRequest().body("请选择文件");
        try {
            MedImage result = imageService.handleUpload(file, currentUser, visibility);
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
            @RequestParam(defaultValue = "40") float wl) {
        try {
            Map<String, Object> result = pythonBridge.getSlice(file, axis, index, ww, wl);
            imageService.markAsParsed(file);
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

        if (!"admin".equals(currentUser.getRole()) && (currentUser.getIsLeader() == null || currentUser.getIsLeader() == 0)) {
            return ResponseEntity.status(403).body("权限不足，只有管理员或组长可以删除影像。");
        }
        
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("删除失败: " + e.getMessage());
        }
    }
}