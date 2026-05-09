package com.gpj.visitorsystem.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 【业务模块】管理员注册请求DTO
 *
 * 【核心职责】
 * 1. 封装管理端注册请求参数
 * 2. 提供手机号、姓名、密码的校验注解
 * 3. 密码强度校验确保安全性
 *
 * 【关键业务场景】
 * 1. 新管理员在Web后台注册账号
 * 2. 系统自动创建userType=4的管理员账号
 * 3. 密码使用BCrypt加密后存储到数据库
 * 4. 注册成功后可直接登录管理后台
 *
 * 【依赖说明】
 * - 被AdminAuthController.register接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - phone为必填，格式为11位手机号，系统中唯一
 * - realName为必填，用于展示管理员姓名
 * - password为必填，必须包含字母和数字，长度6-20位
 * - 密码使用BCrypt加密存储，不可逆
 * - 注册时自动设置userType=4
 */
@Data
public class AdminRegisterRequestDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "密码必须包含字母和数字，长度6-20位")
    private String password;  // BCrypt加密后存储
}