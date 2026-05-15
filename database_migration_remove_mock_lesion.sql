-- =====================================================
-- 移除 Mock 病灶字段
-- 执行时间：2026-05-15
-- 说明：删除 has_mock_lesion 和 mock_lesion_data 列
-- =====================================================

USE med_viewer_db;

ALTER TABLE med_image DROP COLUMN IF EXISTS has_mock_lesion;
ALTER TABLE med_image DROP COLUMN IF EXISTS mock_lesion_data;

SELECT 'Mock 病灶字段已删除' AS status;
