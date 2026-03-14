package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysGroup;
import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysGroupMapper;
import com.med.platform.mapper.SysUserMapper;
import com.med.platform.service.GroupService; // 引入 Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private SysGroupMapper groupMapper;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private GroupService groupService; // 引入

    private boolean isAdmin(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        return user != null && "admin".equals(user.getRole());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");
        return ResponseEntity.ok(userMapper.findAllUsers()); 
    }

    @PostMapping("/group/add")
    @LogAction(module = "系统管理", action = "新增课题组")
    @Transactional
    public ResponseEntity<?> addGroup(@RequestBody SysGroup group, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");
        if (group.getName() == null || group.getName().trim().isEmpty()) return ResponseEntity.badRequest().body("课题组名称不能为空");
        if (group.getDirection() == null || group.getDirection().trim().isEmpty()) return ResponseEntity.badRequest().body("研究方向不能为空");

        try {
            groupMapper.insert(group);
            if (group.getLeaderId() != null) {
                userMapper.updateUserGroup(group.getLeaderId(), group.getId(), 1);
            }
            return ResponseEntity.ok("课题组创建成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/user/add")
    @LogAction(module = "系统管理", action = "新增人员")
    @Transactional
    public ResponseEntity<?> addUser(@RequestBody Map<String, Object> params, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");

        String username = (String) params.get("username");
        String realName = (String) params.get("realName");
        String password = (String) params.get("password");
        
        if (username == null || realName == null || password == null) {
            return ResponseEntity.badRequest().body("账号、姓名和密码为必填项");
        }

        if (userMapper.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body("账号名已存在");
        }

        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setRealName(realName);
        newUser.setRole("researcher");
        newUser.setPassword(passwordEncoder.encode(password));

        try {
            userMapper.insert(newUser);
            
            Object groupIdObj = params.get("groupId");
            Object isLeaderObj = params.get("isLeader");
            
            if (groupIdObj != null && !groupIdObj.toString().isEmpty()) {
                Long groupId = Long.valueOf(groupIdObj.toString());
                Integer isLeader = (isLeaderObj != null && Boolean.parseBoolean(isLeaderObj.toString())) ? 1 : 0;
                userMapper.updateUserGroup(newUser.getId(), groupId, isLeader);
                
                if (isLeader == 1) {
                    SysGroup g = groupMapper.findById(groupId);
                    if(g != null) {
                        g.setLeaderId(newUser.getId());
                        groupMapper.update(g);
                    }
                }
            }
            return ResponseEntity.ok("人员新增成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("新增失败: " + e.getMessage());
        }
    }
    
    // 【新增】指派组长接口
    @PostMapping("/group/assign-leader")
    @LogAction(module = "系统管理", action = "指派组长")
    public ResponseEntity<?> assignLeader(@RequestParam Long groupId, @RequestParam Long leaderId, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");
        
        try {
            groupService.assignLeader(groupId, leaderId);
            return ResponseEntity.ok("组长指派成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("操作失败: " + e.getMessage());
        }
    }
}