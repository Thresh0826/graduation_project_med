package com.med.platform.config;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAction {
    String module() default "";  // 模块名称，如：影像管理
    String action() default "";  // 行为描述，如：上传影像
}