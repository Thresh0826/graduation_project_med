package com.med.platform.entity;

import lombok.Data;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;     

    // --- 新增课题组相关字段 ---
    private Long groupId;    // 所属课题组ID (null表示未加入)
    private Integer isLeader; // 是否为组长: 1-是, 0-否
    
    // 连表查询使用的冗余字段（不映射到 sys_user 表）
    private String groupName;
}