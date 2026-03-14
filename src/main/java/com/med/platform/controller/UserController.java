package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysUser;
import com.med.platform.service.UserService;
import com.med.platform.mapper.SysUserMapper; // 引入 Mapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*") 
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SysUserMapper userMapper;

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
        
        // 【核心修复】每次获取当前用户时，从数据库刷新最新状态
        // 这样组长在后台同意了申请，用户刷新页面就能立刻看到状态变化，无需重新登录
        SysUser latestUser = userMapper.findById(user.getId());
        if (latestUser != null) {
            latestUser.setPassword(null);
            session.setAttribute("user", latestUser); // 更新 session
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
}