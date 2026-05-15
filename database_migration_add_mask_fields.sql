-- =====================================================
-- 新增真实标注 Mask 字段
-- 执行时间：2026-05-15
-- 说明：为 med_image 表添加标注文件相关字段
-- =====================================================

USE med_viewer_db;

ALTER TABLE med_image
ADD COLUMN IF NOT EXISTS mask_file_name VARCHAR(500) DEFAULT NULL COMMENT '原始标注文件名'
AFTER group_id;

ALTER TABLE med_image
ADD COLUMN IF NOT EXISTS mask_file_path VARCHAR(500) DEFAULT NULL COMMENT '存储后的标注文件名'
AFTER mask_file_name;

ALTER TABLE med_image
ADD COLUMN IF NOT EXISTS mask_file_size BIGINT DEFAULT NULL COMMENT '标注文件大小(字节)'
AFTER mask_file_path;

ALTER TABLE med_image
ADD COLUMN IF NOT EXISTS mask_format VARCHAR(50) DEFAULT NULL COMMENT '标注文件格式(NPZ/NIfTI)'
AFTER mask_file_size;

ALTER TABLE med_image
ADD COLUMN IF NOT EXISTS has_mask TINYINT(1) DEFAULT 0 COMMENT '是否有标注文件'
AFTER mask_format;

DESCRIBE med_image;

SELECT 'Mask 标注字段添加完成！' AS status;
