package com.med.platform.config;

import com.med.platform.entity.SysLog;
import com.med.platform.entity.SysUser;
import com.med.platform.mapper.SysLogMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysLogMapper logMapper;

    // 只拦截带有 @LogAction 注解的方法
    @Pointcut("@annotation(com.med.platform.config.LogAction)")
    public void logPointCut() {}

    @AfterReturning(pointcut = "logPointCut()")
    public void doAfterReturning(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;
        HttpServletRequest request = attributes.getRequest();
        
        SysLog sysLog = new SysLog();
        
        // 1. 获取用户信息
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            SysUser user = (SysUser) session.getAttribute("user");
            sysLog.setUsername(user.getUsername());
        } else {
            // 如果 session 为空，尝试从参数中获取（主要是登录接口）
            // 这里简单处理，登录接口会在 Controller 内部手动记录，或者通过 AOP 捕获返回值
            // 对于 Login 接口，AOP 切入时 session 可能还没写入 user
            // 所以 Login 的日志我们建议在 Controller 里手动写，或者这里不做特殊处理，记为 Guest
            sysLog.setUsername("Guest/System");
        }

        // 2. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAction logAction = method.getAnnotation(LogAction.class);
        
        if (logAction != null) {
            sysLog.setOperation(logAction.action()); // 设置具体的行为描述，如：上传影像
            sysLog.setMethod(logAction.module());    // 设置模块名称，如：影像管理
        } else {
            sysLog.setOperation("未知操作");
            sysLog.setMethod("未知模块");
        }
        
        // 3. 基础信息
        sysLog.setParams(request.getRequestURI());
        sysLog.setIp(request.getRemoteAddr());

        logMapper.insert(sysLog);
    }
}