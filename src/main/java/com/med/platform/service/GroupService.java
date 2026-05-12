package com.med.platform.service;

import com.med.platform.entity.SysGroup;
import com.med.platform.entity.SysMessage;
import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysGroupMapper;
import com.med.platform.mapper.SysMessageMapper;
import com.med.platform.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class GroupService {

    @Autowired
    private SysGroupMapper groupMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysMessageMapper messageMapper;

    public List<SysGroup> getAllGroups() {
        return groupMapper.findAll();
    }

    public SysGroup getGroupDetail(Long id) {
        SysGroup group = groupMapper.findById(id);
        if (group != null) {
            group.setMembers(userMapper.findByGroupId(id));
            if (group.getLeaderId() != null) {
                SysUser leader = userMapper.findById(group.getLeaderId());
                if(leader != null) {
                    group.setLeaderName(leader.getRealName() != null ? leader.getRealName() : leader.getUsername());
                }
            }
        }
        return group;
    }

    @Transactional
    public void applyJoin(Long userId, Long groupId, String reason) {
        SysGroup group = groupMapper.findById(groupId);
        if (group == null) throw new RuntimeException("课题组不存在");
        if (group.getLeaderId() == null) throw new RuntimeException("该课题组暂无组长，无法处理申请");

        List<SysMessage> sent = messageMapper.findBySenderId(userId);
        boolean hasPending = sent.stream().anyMatch(m ->
            Objects.equals(m.getGroupId(), groupId) && "JOIN_APPLY".equals(m.getType()) && "UNREAD".equals(m.getStatus())
        );
        if (hasPending) throw new RuntimeException("您已提交过加入申请，请勿重复提交");

        SysMessage msg = new SysMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(group.getLeaderId());
        msg.setGroupId(groupId);
        msg.setType("JOIN_APPLY");
        msg.setContent(reason != null ? reason : "申请加入课题组");
        msg.setStatus("UNREAD");
        messageMapper.insert(msg);
    }

    @Transactional
    public void applyQuit(Long userId, Long groupId, String reason) {
        SysGroup group = groupMapper.findById(groupId);
        if (group == null) throw new RuntimeException("课题组不存在");
        if (group.getLeaderId() == null) throw new RuntimeException("该课题组暂无组长，无法处理申请");

        List<SysMessage> sent = messageMapper.findBySenderId(userId);
        boolean hasPending = sent.stream().anyMatch(m ->
            Objects.equals(m.getGroupId(), groupId) && "QUIT_APPLY".equals(m.getType()) && "UNREAD".equals(m.getStatus())
        );
        if (hasPending) throw new RuntimeException("您已提交过退出申请，请勿重复提交");

        SysMessage msg = new SysMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(group.getLeaderId());
        msg.setGroupId(groupId);
        msg.setType("QUIT_APPLY");
        msg.setContent(reason != null ? reason : "申请退出课题组");
        msg.setStatus("UNREAD");
        messageMapper.insert(msg);
    }

    @Transactional
    public void handleJoinApply(Long messageId, boolean isAgree, Long leaderId) {
        SysMessage msg = messageMapper.findById(messageId);
        if (msg == null || !msg.getReceiverId().equals(leaderId)) throw new RuntimeException("无权处理");
        if (!"UNREAD".equals(msg.getStatus())) throw new RuntimeException("消息已处理");

        if (isAgree) {
            userMapper.updateUserGroup(msg.getSenderId(), msg.getGroupId(), 0);
            messageMapper.updateStatus(messageId, "APPROVED");
        } else {
            messageMapper.updateStatus(messageId, "REJECTED");
        }
    }

    @Transactional
    public void handleQuitApply(Long messageId, boolean isAgree, Long leaderId) {
        SysMessage msg = messageMapper.findById(messageId);
        if (msg == null || !msg.getReceiverId().equals(leaderId)) throw new RuntimeException("无权处理");
        if (!"UNREAD".equals(msg.getStatus())) throw new RuntimeException("消息已处理");

        if (isAgree) {
            userMapper.quitGroup(msg.getSenderId());
            messageMapper.updateStatus(messageId, "APPROVED");
        } else {
            messageMapper.updateStatus(messageId, "REJECTED");
        }
    }
    
    // 【新增】管理员指派组长
    @Transactional
    public void assignLeader(Long groupId, Long newLeaderId) {
        SysGroup group = groupMapper.findById(groupId);
        if (group == null) throw new RuntimeException("课题组不存在");
        
        SysUser newLeader = userMapper.findById(newLeaderId);
        if (newLeader == null) throw new RuntimeException("指定的用户不存在");

        if (group.getLeaderId() != null && !group.getLeaderId().equals(newLeaderId)) {
            userMapper.updateUserGroup(group.getLeaderId(), group.getId(), 0); 
        }

        group.setLeaderId(newLeaderId);
        groupMapper.update(group);
        
        userMapper.updateUserGroup(newLeaderId, groupId, 1);
    }

    // 【新增】组长踢人
    @Transactional
    public void kickMember(Long groupId, Long targetUserId, Long currentLeaderId) {
        SysGroup group = groupMapper.findById(groupId);
        if (group == null) throw new RuntimeException("课题组不存在");
        
        // 校验当前用户是否为该组的组长 (或者允许管理员操作)
        SysUser leader = userMapper.findById(currentLeaderId);
        boolean isLeader = group.getLeaderId() != null && group.getLeaderId().equals(currentLeaderId);
        boolean isAdmin = leader != null && "admin".equals(leader.getRole());
        
        if (!isLeader && !isAdmin) {
            throw new RuntimeException("权限不足，只有该课题组长或系统管理员可以移除成员");
        }

        if (Objects.equals(targetUserId, group.getLeaderId())) {
            throw new RuntimeException("不能将组长移出课题组，请先指派新组长");
        }

        SysUser targetUser = userMapper.findById(targetUserId);
        if (targetUser == null || targetUser.getGroupId() == null || !targetUser.getGroupId().equals(groupId)) {
             throw new RuntimeException("目标用户不在此课题组中");
        }

        // 执行踢出
        userMapper.quitGroup(targetUserId);
    }

    public List<SysMessage> getMyMessages(Long userId) {
        return messageMapper.findByReceiverId(userId); 
    }
    
    public List<SysMessage> getMySentMessages(Long userId) {
        return messageMapper.findBySenderId(userId);
    }
}