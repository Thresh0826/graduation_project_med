-- =====================================================
-- 需求七：Mock 病灶数据字段迁移脚本
-- 执行时间：2026-04-18
-- 说明：为 med_image 表添加 Mock 病灶相关字段
-- =====================================================

-- 1. 添加布尔型字段：标识是否有模拟病灶
ALTER TABLE med_image 
ADD COLUMN has_mock_lesion TINYINT(1) DEFAULT 0 COMMENT '是否有模拟病灶（0=无，1=有）' 
AFTER group_id;

-- 2. 添加 JSON 字段：存储病灶坐标数据
ALTER TABLE med_image 
ADD COLUMN mock_lesion_data TEXT COMMENT '模拟病灶的坐标数据（JSON格式：{centerX, centerY, radiusX, radiusY, type}）' 
AFTER has_mock_lesion;

-- 3. 验证字段是否添加成功
DESCRIBE med_image;

-- =====================================================
-- 使用说明：
-- 1. 在 MySQL 客户端中执行此脚本
-- 2. 确保数据库名为 med_viewer_db
-- 3. 执行后检查 med_image 表结构
-- =====================================================
