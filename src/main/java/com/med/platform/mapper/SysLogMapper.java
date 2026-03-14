package com.med.platform.mapper;

import com.med.platform.entity.SysLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysLogMapper {
    @Insert("INSERT INTO sys_log(username, operation, method, params, ip, create_time) " +
            "VALUES(#{username}, #{operation}, #{method}, #{params}, #{ip}, NOW())")
    int insert(SysLog log);

    // Module 6: 管理员可查询所有日志，普通用户查不到 (或仅查自己)
    // 这里为了演示方便，先提供全部查询接口
    @Select("SELECT * FROM sys_log ORDER BY create_time DESC")
    List<SysLog> findAll();
}