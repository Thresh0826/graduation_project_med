package com.med.platform.mapper;

import com.med.platform.entity.SysGroup;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysGroupMapper {
    
    @Insert("INSERT INTO sys_group(name, direction, description, leader_id, create_time) " +
            "VALUES(#{name}, #{direction}, #{description}, #{leaderId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysGroup group);

    @Select("SELECT g.*, u.real_name as leaderName FROM sys_group g LEFT JOIN sys_user u ON g.leader_id = u.id ORDER BY g.create_time DESC")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "direction", property = "direction"),
        @Result(column = "description", property = "description"),
        @Result(column = "leader_id", property = "leaderId"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "leaderName", property = "leaderName")
    })
    List<SysGroup> findAll();

    @Select("SELECT * FROM sys_group WHERE id = #{id}")
    SysGroup findById(Long id);
    
    @Update("UPDATE sys_group SET name = #{name}, direction = #{direction}, description = #{description} WHERE id = #{id}")
    int update(SysGroup group);
    
    @Delete("DELETE FROM sys_group WHERE id = #{id}")
    int delete(Long id);
}