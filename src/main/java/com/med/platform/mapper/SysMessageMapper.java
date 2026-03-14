package com.med.platform.mapper;

import com.med.platform.entity.SysMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysMessageMapper {

    @Insert("INSERT INTO sys_message(sender_id, receiver_id, group_id, type, content, status, create_time) " +
            "VALUES(#{senderId}, #{receiverId}, #{groupId}, #{type}, #{content}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysMessage message);

    // 组长查看发给自己的审批消息
    @Select("SELECT m.*, u.real_name as senderName, g.name as groupName FROM sys_message m " +
            "LEFT JOIN sys_user u ON m.sender_id = u.id " +
            "LEFT JOIN sys_group g ON m.group_id = g.id " +
            "WHERE m.receiver_id = #{receiverId} ORDER BY m.create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "sender_id", property = "senderId"),
            @Result(column = "receiver_id", property = "receiverId"),
            @Result(column = "group_id", property = "groupId"),
            @Result(column = "type", property = "type"),
            @Result(column = "content", property = "content"),
            @Result(column = "status", property = "status"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "senderName", property = "senderName"),
            @Result(column = "groupName", property = "groupName")
    })
    List<SysMessage> findByReceiverId(Long receiverId);
    
    // 用户查看自己发出的申请
    @Select("SELECT m.*, g.name as groupName FROM sys_message m LEFT JOIN sys_group g ON m.group_id = g.id WHERE m.sender_id = #{senderId} ORDER BY m.create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "sender_id", property = "senderId"),
            @Result(column = "receiver_id", property = "receiverId"),
            @Result(column = "group_id", property = "groupId"),
            @Result(column = "type", property = "type"),
            @Result(column = "content", property = "content"),
            @Result(column = "status", property = "status"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "groupName", property = "groupName")
    })
    List<SysMessage> findBySenderId(Long senderId);

    @Select("SELECT * FROM sys_message WHERE id = #{id}")
    SysMessage findById(Long id);

    @Update("UPDATE sys_message SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}