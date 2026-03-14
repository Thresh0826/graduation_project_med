package com.med.platform.mapper;

import com.med.platform.entity.SysUser;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysUserMapper {
    
    @Select("SELECT u.*, g.name as group_name FROM sys_user u LEFT JOIN sys_group g ON u.group_id = g.id WHERE u.username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT u.*, g.name as group_name FROM sys_user u LEFT JOIN sys_group g ON u.group_id = g.id WHERE u.id = #{id}")
    SysUser findById(Long id);

    @Insert("INSERT INTO sys_user(username, password, real_name, role, avatar) VALUES(#{username}, #{password}, #{realName}, #{role}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Select("SELECT id, username, real_name as realName, role, is_leader as isLeader, avatar FROM sys_user WHERE group_id = #{groupId}")
    List<SysUser> findByGroupId(Long groupId);

    @Update("UPDATE sys_user SET group_id = #{groupId}, is_leader = #{isLeader} WHERE id = #{userId}")
    int updateUserGroup(@Param("userId") Long userId, @Param("groupId") Long groupId, @Param("isLeader") Integer isLeader);
    
    @Update("UPDATE sys_user SET group_id = NULL, is_leader = 0 WHERE id = #{userId}")
    int quitGroup(Long userId);
    
    // 【新增】获取所有用户 (供管理员分配使用)
    @Select("SELECT id, username, real_name as realName, role FROM sys_user ORDER BY id DESC")
    List<SysUser> findAllUsers();
    
    // 【新增】个人信息更新相关
    @Update("UPDATE sys_user SET real_name = #{realName} WHERE id = #{id}")
    int updateRealName(@Param("id") Long id, @Param("realName") String realName);
    
    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    @Update("UPDATE sys_user SET avatar = #{avatar} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);
}