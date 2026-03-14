package com.med.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class SysGroup {
    private Long id;
    private String name;
    private String direction;
    private String description;
    private Long leaderId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // --- 连表查询附加字段 ---
    private String leaderName; // 组长姓名
    private List<SysUser> members; // 组内成员列表
}