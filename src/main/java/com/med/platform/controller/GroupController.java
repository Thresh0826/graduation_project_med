package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysGroup;
import com.med.platform.entity.SysUser;
import com.med.platform.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/group")
@CrossOrigin(origins = "*")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @GetMapping("/list")
    public ResponseEntity<?> listGroups(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请先登录");
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请先登录");
        SysGroup group = groupService.getGroupDetail(id);
        if (group != null) return ResponseEntity.ok(group);
        return ResponseEntity.status(404).body("课题组不存在");
    }

    @PostMapping("/apply/join")
    @LogAction(module = "课题组管理", action = "申请加入")
    public ResponseEntity<?> applyJoin(@RequestParam Long groupId, @RequestParam(required = false) String reason, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");
        if (user.getGroupId() != null) return ResponseEntity.badRequest().body("您已加入其他课题组");

        try {
            groupService.applyJoin(user.getId(), groupId, reason);
            return ResponseEntity.ok("申请已提交，等待组长审批");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/apply/quit")
    @LogAction(module = "课题组管理", action = "申请退出")
    public ResponseEntity<?> applyQuit(@RequestParam Long groupId, @RequestParam(required = false) String reason, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");
        if (user.getGroupId() == null || !user.getGroupId().equals(groupId)) return ResponseEntity.badRequest().body("您不属于该课题组");
        if (user.getIsLeader() != null && user.getIsLeader() == 1) return ResponseEntity.badRequest().body("组长不能直接退出，请先转让或解散课题组");

        try {
            groupService.applyQuit(user.getId(), groupId, reason);
            return ResponseEntity.ok("申请已提交，等待组长审批");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/kick")
    @LogAction(module = "课题组管理", action = "踢出成员")
    public ResponseEntity<?> kickMember(@RequestParam Long groupId, @RequestParam Long targetUserId, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("请登录");

        try {
            groupService.kickMember(groupId, targetUserId, user.getId());
            return ResponseEntity.ok("已将该成员移出课题组");
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}