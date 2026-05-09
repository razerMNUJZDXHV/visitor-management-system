package com.gpj.visitorsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 【业务模块】应用基础配置
 *
 * 【核心职责】
 * 1. 配置BCrypt密码加密器，用于管理端密码加密存储
 * 2. 提供PasswordEncoder Bean供Service层使用
 * 3. 确保所有管理端密码不以明文形式存储
 *
 * 【关键业务场景】
 * 1. 管理员注册时，密码使用BCrypt加密后存入数据库
 * 2. 管理员登录时，用同一encoder比对数据库中的加密密码
 * 3. 创建审批人/安保账号时，若设置密码也使用BCrypt加密
 * 4. 修改密码时，新密码同样使用BCrypt加密
 *
 * 【依赖说明】
 * - 被AdminUserService使用：密码加密和比对
 * - 使用Spring Security的BCryptPasswordEncoder
 *
 * 【注意事项】
 * - BCrypt是单向哈希，密码不可逆向解密
 * - 同一密码每次加密结果不同（含随机salt），比对时用matches方法
 * - 密码强度由BCrypt自动处理，默认强度10（2^10轮哈希）
 * - 生产环境建议提高BCrypt强度（如12-14轮）
 */
@Configuration
public class AppConfig {

    /**
     * BCrypt密码加密器
     * 管理员登录时，用same encoder匹配数据库里的加密密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}