package com.med.platform.controller;

import com.med.platform.entity.SysLog;
import com.med.platform.mapper.SysLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private SysLogMapper logMapper;

    @GetMapping
    public List<SysLog> list() {
        return logMapper.findAll();
    }
}