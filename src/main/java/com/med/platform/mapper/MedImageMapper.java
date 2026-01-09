package com.med.platform.mapper;

import com.med.platform.entity.MedImage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MedImageMapper {
    @Insert("INSERT INTO med_image(file_name, file_path, file_size, format, dim_x, dim_y, dim_z) " +
            "VALUES(#{fileName}, #{filePath}, #{fileSize}, #{format}, #{dimX}, #{dimY}, #{dimZ})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedImage medImage);

    // 【关键修复】：通过 @Results 手动指定数据库列名和 Java 变量名的对应关系
    @Select("SELECT * FROM med_image ORDER BY create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "file_name", property = "fileName"),
            @Result(column = "file_path", property = "filePath"),
            @Result(column = "file_size", property = "fileSize"),
            @Result(column = "format", property = "format"),
            @Result(column = "dim_x", property = "dimX"),
            @Result(column = "dim_y", property = "dimY"),
            @Result(column = "dim_z", property = "dimZ"),
            @Result(column = "create_time", property = "createTime")
    })
    List<MedImage> findAll();
}