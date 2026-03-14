package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysUser;
import com.med.platform.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/message")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private GroupService groupService;

    // 获取我的待办审批（如果是组长）
    @GetMapping("/todo")
    public ResponseEntity<?> getTodos(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");
        
        return ResponseEntity.ok(groupService.getMyMessages(user.getId()));
    }
    
    // 获取我发出的申请记录
    @GetMapping("/sent")
    public ResponseEntity<?> getSent(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");
        
        return ResponseEntity.ok(groupService.getMySentMessages(user.getId()));
    }

    // 审批加入
    @PostMapping("/approve/join")
    @LogAction(module = "消息系统", action = "审批加入申请")
    public ResponseEntity<?> approveJoin(@RequestParam Long messageId, @RequestParam boolean isAgree, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");

        try {
            groupService.handleJoinApply(messageId, isAgree, user.getId());
            return ResponseEntity.ok("审批完成");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // 审批退出
    @PostMapping("/approve/quit")
    @LogAction(module = "消息系统", action = "审批退出申请")
    public ResponseEntity<?> approveQuit(@RequestParam Long messageId, @RequestParam boolean isAgree, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");

        try {
            groupService.handleQuitApply(messageId, isAgree, user.getId());
            return ResponseEntity.ok("审批完成");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}