package com.med.platform.service;

import com.med.platform.entity.SysUser;

public interface UserService {
    SysUser login(String username, String password);
}