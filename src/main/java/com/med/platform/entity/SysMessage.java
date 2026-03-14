package com.med.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class SysMessage {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    
    // JOIN_APPLY (申请加入), QUIT_APPLY (申请退出)
    private String type; 
    
    private String content;
    
    // UNREAD(未读), APPROVED(已同意), REJECTED(已拒绝)
    private String status; 

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // --- 连表展示字段 ---
    private String senderName;
    private String groupName;
}