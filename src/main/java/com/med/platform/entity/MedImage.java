package com.med.platform.entity;
import lombok.Data;
import java.time.LocalDateTime;

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
    private LocalDateTime createTime;
}