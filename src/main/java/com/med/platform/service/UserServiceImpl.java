package com.med.platform.service;

import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public SysUser login(String username, String password) {
        SysUser user = userMapper.findByUsername(username);
        // 1. 检查用户是否存在  2. 检查密码是否匹配
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
}