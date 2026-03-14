package com.med.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类 (Module 6)
 */
@Data
public class SysLog {
    private Long id;
    private String username;   // 操作人
    private String operation;  // 操作描述 (如: 上传影像)
    private String method;     // 请求方法 (GET/POST)
    private String params;     // 请求参数
    private String ip;         // 操作IP
    private LocalDateTime createTime;
}