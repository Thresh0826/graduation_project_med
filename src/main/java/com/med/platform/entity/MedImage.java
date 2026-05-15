package com.med.platform.entity;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class MedImage {
    private Long id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String format;
    private Integer dimX;
    private Integer dimY;
    private Integer dimZ;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private Integer isDeleted;
    private String patientId;
    private String modality; 
    
    // 可见性: 1-仅本课题组可见, 2-共享给所有课题组 (取消了 0-私有)
    private Integer visibility = 1; 
    private String ownerName;

    private Integer status; 

    private Double voxResX;
    private Double voxResY;
    private Double voxResZ;

    // --- 新增 ---
    private Long groupId; // 归属课题组ID
    
    // 前端展示用的冗余字段
    private String groupName;

    // --- 真实标注 Mask 文件 ---
    private String maskFileName;    // 原始标注文件名
    private String maskFilePath;    // 存储后的标注文件名
    private Long maskFileSize;      // 文件大小
    private String maskFormat;      // 标注文件格式 (NPZ/NIfTI)
    private Boolean hasMask;        // 是否有标注
}