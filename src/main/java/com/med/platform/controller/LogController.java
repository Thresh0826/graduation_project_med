package com.med.platform.controller;

import com.med.platform.entity.SysLog;
import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private SysLogMapper logMapper;

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null || !"admin".equals(user.getRole())) {
            return ResponseEntity.status(403).body("仅系统管理员可查看日志");
        }
        return ResponseEntity.ok(logMapper.findAll());
    }
}