package com.med.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置 Spring Security 过滤器链
     * 这里为了配合 Controller 中自定义的 Session 登录逻辑，我们放行所有接口，
     * 但保留 CSRF 禁用和 CORS 配置，以避免前端调用受阻。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF (跨站请求伪造) 保护，因为我们是前后端分离架构
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置跨域 (CORS)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 放行所有请求，让 Controller 层自己处理权限
                // 注意：生产环境中应该更严格，只放行 /api/user/login 等公开接口
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * 配置跨域资源共享 (CORS)
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 允许发送 Cookie
        config.addAllowedOriginPattern("*"); // 允许所有域名 (生产环境建议指定具体域名)
        config.addAllowedHeader("*"); // 允许所有请求头
        config.addAllowedMethod("*"); // 允许所有 HTTP 方法 (GET, POST, PUT, DELETE...)
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}