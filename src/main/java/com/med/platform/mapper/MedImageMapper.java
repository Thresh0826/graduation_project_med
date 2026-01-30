package com.med.platform.mapper;

import com.med.platform.entity.MedImage;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MedImageMapper {
    @Insert("INSERT INTO med_image(file_name, file_path, file_size, format, dim_x, dim_y, dim_z, is_deleted) " +
            "VALUES(#{fileName}, #{filePath}, #{fileSize}, #{format}, #{dimX}, #{dimY}, #{dimZ}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedImage medImage);

    // 【修改】：增加 WHERE is_deleted = 0 过滤
    @Select("SELECT * FROM med_image WHERE is_deleted = 0 ORDER BY create_time DESC")
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
            @Result(column = "is_deleted", property = "isDeleted") // 新增映射
    })
    List<MedImage> findAll();

    // 【新增】：逻辑删除
    @Update("UPDATE med_image SET is_deleted = 1 WHERE id = #{id}")
    int logicalDelete(Long id);
}