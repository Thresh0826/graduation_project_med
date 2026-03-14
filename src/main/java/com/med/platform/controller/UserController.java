package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysUser;
import com.med.platform.service.UserService;
import com.med.platform.mapper.SysUserMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Base64;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*") 
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @LogAction(module = "系统接入", action = "登录") 
    public ResponseEntity<?> login(@RequestBody Map<String, String> params, HttpSession session) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("用户名和密码不能为空");
        }

        SysUser user = userService.login(username, password);

        if (user != null) {
            session.setAttribute("user", user);
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).body("用户名或密码错误。");
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> currentUser(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("用户未登录或会话已过期。");
        }
        
        SysUser latestUser = userMapper.findById(user.getId());
        if (latestUser != null) {
            latestUser.setPassword(null);
            session.setAttribute("user", latestUser);
            return ResponseEntity.ok(latestUser);
        }
        
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    @LogAction(module = "系统接入", action = "退出")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); 
        return ResponseEntity.ok("退出成功");
    }
    
    // ====== 个人信息维护 ======
    
    @PutMapping("/profile/name")
    @LogAction(module = "个人中心", action = "修改姓名")
    public ResponseEntity<?> updateName(@RequestParam String realName, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请先登录");
        
        try {
            userMapper.updateRealName(user.getId(), realName);
            return ResponseEntity.ok("姓名修改成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("修改失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/profile/password")
    @LogAction(module = "个人中心", action = "修改密码")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> params, HttpSession session) {
        SysUser sessionUser = (SysUser) session.getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body("请先登录");
        
        String oldPwd = params.get("oldPassword");
        String newPwd = params.get("newPassword");
        
        // 校验旧密码
        SysUser dbUser = userMapper.findById(sessionUser.getId());
        if (!passwordEncoder.matches(oldPwd, dbUser.getPassword())) {
            return ResponseEntity.badRequest().body("原密码错误");
        }
        
        try {
            userMapper.updatePassword(sessionUser.getId(), passwordEncoder.encode(newPwd));
            return ResponseEntity.ok("密码修改成功，请重新登录");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("修改失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/profile/avatar")
    @LogAction(module = "个人中心", action = "修改头像")
    public ResponseEntity<?> updateAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请先登录");
        
        try {
            byte[] bytes = file.getBytes();
            String base64Image = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(bytes);
            
            userMapper.updateAvatar(user.getId(), base64Image);
            return ResponseEntity.ok(base64Image);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("头像修改失败: " + e.getMessage());
        }
    }
}