package com.med.platform.entity;

import lombok.Data;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;     

    private Long groupId;    
    private Integer isLeader; 
    
    // 【新增 Requirement III】头像路径/Base64
    private String avatar;
    
    private String groupName;
}