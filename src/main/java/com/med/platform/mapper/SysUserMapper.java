package com.med.platform.mapper;

import com.med.platform.entity.SysUser;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysUserMapper {
    
    // 【修改】联表查询出课题组名称
    @Select("SELECT u.*, g.name as group_name FROM sys_user u LEFT JOIN sys_group g ON u.group_id = g.id WHERE u.username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT u.*, g.name as group_name FROM sys_user u LEFT JOIN sys_group g ON u.group_id = g.id WHERE u.id = #{id}")
    SysUser findById(Long id);

    @Insert("INSERT INTO sys_user(username, password, role) VALUES(#{username}, #{password}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    // 查询某课题组下的所有成员
    @Select("SELECT id, username, real_name as realName, role, is_leader as isLeader FROM sys_user WHERE group_id = #{groupId}")
    List<SysUser> findByGroupId(Long groupId);

    // 更新用户的课题组信息
    @Update("UPDATE sys_user SET group_id = #{groupId}, is_leader = #{isLeader} WHERE id = #{userId}")
    int updateUserGroup(@Param("userId") Long userId, @Param("groupId") Long groupId, @Param("isLeader") Integer isLeader);
    
    // 退出课题组
    @Update("UPDATE sys_user SET group_id = NULL, is_leader = 0 WHERE id = #{userId}")
    int quitGroup(Long userId);
}