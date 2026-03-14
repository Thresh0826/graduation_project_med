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
            SysUser leader = userMapper.findById(group.getLeaderId());
            if(leader != null) {
                group.setLeaderName(leader.getRealName() != null ? leader.getRealName() : leader.getUsername());
            }
        }
        return group;
    }

    @Transactional
    public void applyJoin(Long userId, Long groupId, String reason) {
        SysGroup group = groupMapper.findById(groupId);
        if (group == null) throw new RuntimeException("课题组不存在");

        // 【优化】防止重复提交加入申请
        List<SysMessage> sent = messageMapper.findBySenderId(userId);
        boolean hasPending = sent.stream().anyMatch(m -> 
            m.getGroupId().equals(groupId) && 
            "JOIN_APPLY".equals(m.getType()) && 
            "UNREAD".equals(m.getStatus())
        );
        if (hasPending) {
            throw new RuntimeException("您已提交过加入申请，请勿重复提交，等待组长审批");
        }

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

        // 【优化】防止重复提交退出申请
        List<SysMessage> sent = messageMapper.findBySenderId(userId);
        boolean hasPending = sent.stream().anyMatch(m -> 
            m.getGroupId().equals(groupId) && 
            "QUIT_APPLY".equals(m.getType()) && 
            "UNREAD".equals(m.getStatus())
        );
        if (hasPending) {
            throw new RuntimeException("您已提交过退出申请，请勿重复提交，等待组长审批");
        }

        SysMessage msg = new SysMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(group.getLeaderId());
        msg.setGroupId(groupId);
        msg.setType("QUIT_APPLY");
        msg.setContent(reason != null ? reason : "申请退出课题组");
        msg.setStatus("UNREAD");
        messageMapper.insert(msg);
    }

    // --- 审批流 ---
    @Transactional
    public void handleJoinApply(Long messageId, boolean isAgree, Long leaderId) {
        SysMessage msg = messageMapper.findById(messageId);
        if (msg == null || !msg.getReceiverId().equals(leaderId)) throw new RuntimeException("无权处理");
        if (!"UNREAD".equals(msg.getStatus())) throw new RuntimeException("消息已处理");

        if (isAgree) {
            // 同意加入 (将该用户绑定到该组，且不作为组长)
            userMapper.updateUserGroup(msg.getSenderId(), msg.getGroupId(), 0);
            messageMapper.updateStatus(messageId, "APPROVED");
        } else {
            // 拒绝
            messageMapper.updateStatus(messageId, "REJECTED");
        }
    }

    @Transactional
    public void handleQuitApply(Long messageId, boolean isAgree, Long leaderId) {
        SysMessage msg = messageMapper.findById(messageId);
        if (msg == null || !msg.getReceiverId().equals(leaderId)) throw new RuntimeException("无权处理");
        if (!"UNREAD".equals(msg.getStatus())) throw new RuntimeException("消息已处理");

        if (isAgree) {
            // 同意退出
            userMapper.quitGroup(msg.getSenderId());
            messageMapper.updateStatus(messageId, "APPROVED");
        } else {
            // 拒绝
            messageMapper.updateStatus(messageId, "REJECTED");
        }
    }
    
    // --- 消息查询 ---
    public List<SysMessage> getMyMessages(Long userId) {
        return messageMapper.findByReceiverId(userId); 
    }
    
    public List<SysMessage> getMySentMessages(Long userId) {
        return messageMapper.findBySenderId(userId);
    }
}