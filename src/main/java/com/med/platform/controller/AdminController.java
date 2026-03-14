package com.med.platform.controller;

import com.med.platform.config.LogAction;
import com.med.platform.entity.SysGroup;
import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysGroupMapper;
import com.med.platform.mapper.SysUserMapper;
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

    // 鉴权辅助方法
    private boolean isAdmin(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        return user != null && "admin".equals(user.getRole());
    }

    // 1. 获取所有用户列表 (供新增课题组时选择组长，或直接展示)
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");
        // 这里为了演示复用了现有的 mapper，实际应该有一个 findAllUsers，我们在下方添加
        return ResponseEntity.ok(userMapper.findAllUsers()); 
    }

    // 2. 新增课题组
    @PostMapping("/group/add")
    @LogAction(module = "系统管理", action = "新增课题组")
    @Transactional
    public ResponseEntity<?> addGroup(@RequestBody SysGroup group, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("仅系统管理员可用");
        if (group.getName() == null || group.getName().trim().isEmpty()) return ResponseEntity.badRequest().body("课题组名称不能为空");
        if (group.getDirection() == null || group.getDirection().trim().isEmpty()) return ResponseEntity.badRequest().body("研究方向不能为空");

        try {
            groupMapper.insert(group);
            // 如果指定了组长，同步更新该用户的信息
            if (group.getLeaderId() != null) {
                userMapper.updateUserGroup(group.getLeaderId(), group.getId(), 1);
            }
            return ResponseEntity.ok("课题组创建成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("创建失败: " + e.getMessage());
        }
    }

    // 3. 新增人员
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

        // 检查用户是否已存在
        if (userMapper.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body("账号名已存在");
        }

        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setRealName(realName);
        newUser.setRole("researcher"); // 默认都是研究人员
        
        // BCrypt 盐值默认 10 加密
        newUser.setPassword(passwordEncoder.encode(password));

        try {
            userMapper.insert(newUser);
            
            // 处理可选的课题组与组长分配
            Object groupIdObj = params.get("groupId");
            Object isLeaderObj = params.get("isLeader");
            
            if (groupIdObj != null) {
                Long groupId = Long.valueOf(groupIdObj.toString());
                Integer isLeader = (isLeaderObj != null && Boolean.parseBoolean(isLeaderObj.toString())) ? 1 : 0;
                userMapper.updateUserGroup(newUser.getId(), groupId, isLeader);
                
                // 如果是组长，还需要同步更新课题组表
                if (isLeader == 1) {
                    SysGroup g = groupMapper.findById(groupId);
                    if(g != null) {
                        g.setLeaderId(newUser.getId());
                        groupMapper.update(g); // 需确保 mapper 有这个全量 update
                    }
                }
            }
            return ResponseEntity.ok("人员新增成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("新增失败: " + e.getMessage());
        }
    }
}