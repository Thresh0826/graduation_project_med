package com.med.platform.mapper;

import com.med.platform.entity.MedImage;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MedImageMapper {
    @Insert("INSERT INTO med_image(file_name, file_path, file_size, format, dim_x, dim_y, dim_z, is_deleted, modality, create_time, status, vox_res_x, vox_res_y, vox_res_z, visibility, owner_name, group_id, has_mock_lesion, mock_lesion_data) " +
            "VALUES(#{fileName}, #{filePath}, #{fileSize}, #{format}, #{dimX}, #{dimY}, #{dimZ}, 0, #{modality}, NOW(), #{status}, #{voxResX}, #{voxResY}, #{voxResZ}, #{visibility}, #{ownerName}, #{groupId}, #{hasMockLesion}, #{mockLesionData})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedImage medImage);

    @Select("SELECT i.*, g.name as groupName FROM med_image i LEFT JOIN sys_group g ON i.group_id = g.id WHERE i.is_deleted = 0 ORDER BY i.create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "file_name", property = "fileName"),
            @Result(column = "file_path", property = "filePath"),
            @Result(column = "file_size", property = "fileSize"),
            @Result(column = "format", property = "format"),
            @Result(column = "dim_x", property = "dimX"),
            @Result(column = "dim_y", property = "dimY"),
            @Result(column = "dim_z", property = "dimZ"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "modality", property = "modality"),
            @Result(column = "status", property = "status"),
            @Result(column = "visibility", property = "visibility"), 
            @Result(column = "owner_name", property = "ownerName"),
            @Result(column = "group_id", property = "groupId"),
            @Result(column = "groupName", property = "groupName"),
            @Result(column = "has_mock_lesion", property = "hasMockLesion"),
            @Result(column = "mock_lesion_data", property = "mockLesionData")
    })
    List<MedImage> findAll();

    @Select("SELECT i.*, g.name as groupName FROM med_image i LEFT JOIN sys_group g ON i.group_id = g.id WHERE i.is_deleted = 0 AND (i.group_id = #{groupId} OR i.visibility = 2) ORDER BY i.create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "file_name", property = "fileName"),
            @Result(column = "file_path", property = "filePath"),
            @Result(column = "file_size", property = "fileSize"),
            @Result(column = "format", property = "format"),
            @Result(column = "dim_x", property = "dimX"),
            @Result(column = "dim_y", property = "dimY"),
            @Result(column = "dim_z", property = "dimZ"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "modality", property = "modality"),
            @Result(column = "status", property = "status"),
            @Result(column = "visibility", property = "visibility"), 
            @Result(column = "owner_name", property = "ownerName"),
            @Result(column = "group_id", property = "groupId"),
            @Result(column = "groupName", property = "groupName"),
            @Result(column = "has_mock_lesion", property = "hasMockLesion"),
            @Result(column = "mock_lesion_data", property = "mockLesionData")
    })
    List<MedImage> findVisibleByGroup(Long groupId);

    @Select("SELECT * FROM med_image WHERE id = #{id}")
    MedImage selectById(Long id);

    @Update("UPDATE med_image SET is_deleted = 1 WHERE id = #{id}")
    int logicalDelete(Long id);

    @Update("UPDATE med_image SET status = 1 WHERE file_name = #{fileName} AND is_deleted = 0")
    void updateStatusToParsed(String fileName);
    
    @Update("UPDATE med_image SET visibility = #{visibility} WHERE id = #{id}")
    int updateVisibility(@Param("id") Long id, @Param("visibility") Integer visibility);

    // 【优化】带权限过滤的数据看板统计
    @Select("<script>" +
            "SELECT modality, COUNT(*) as count FROM med_image WHERE is_deleted = 0 " +
            "<if test='!isAdmin'> AND (group_id = #{groupId} OR visibility = 2) </if>" +
            "GROUP BY modality" +
            "</script>")
    List<Map<String, Object>> countByModality(@Param("groupId") Long groupId, @Param("isAdmin") boolean isAdmin);

    @Select("<script>" +
            "SELECT format, COUNT(*) as count FROM med_image WHERE is_deleted = 0 " +
            "<if test='!isAdmin'> AND (group_id = #{groupId} OR visibility = 2) </if>" +
            "GROUP BY format" +
            "</script>")
    List<Map<String, Object>> countByFormat(@Param("groupId") Long groupId, @Param("isAdmin") boolean isAdmin);

    @Select("<script>" +
            "SELECT status, COUNT(*) as count FROM med_image WHERE is_deleted = 0 " +
            "<if test='!isAdmin'> AND (group_id = #{groupId} OR visibility = 2) </if>" +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("groupId") Long groupId, @Param("isAdmin") boolean isAdmin);

    @Select("<script>" +
            "SELECT COUNT(*) FROM med_image WHERE is_deleted = 0 " +
            "<if test='!isAdmin'> AND (group_id = #{groupId} OR visibility = 2) </if>" +
            "</script>")
    int countTotal(@Param("groupId") Long groupId, @Param("isAdmin") boolean isAdmin);
}