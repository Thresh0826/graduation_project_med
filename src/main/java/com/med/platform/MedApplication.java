package com.med.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.med.platform.mapper") // 扫描写账员
public class MedApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedApplication.class, args);
    }
}