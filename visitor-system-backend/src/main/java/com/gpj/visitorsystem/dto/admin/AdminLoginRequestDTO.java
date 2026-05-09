package com.gpj.visitorsystem.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 【业务模块】管理员登录请求DTO
 *
 * 【核心职责】
 * 1. 封装管理端登录请求参数
 * 2. 提供手机号和密码的校验注解
 * 3. 支持管理后台Web端登录
 *
 * 【关键业务场景】
 * 1. 管理员在Web后台输入手机号和密码登录
 * 2. 前端调用此接口获取JWT Token
 * 3. 登录成功后前端保存token到localStorage
 * 4. 后续请求在Authorization头中携带token
 *
 * 【依赖说明】
 * - 被AdminAuthController.login接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - phone为必填，格式为11位手机号
 * - password为必填，管理端登录密码
 * - 密码使用BCrypt加密存储，登录时比对加密后的值
 * - 只有userType=4的管理员才能登录Web后台
 */
@Data
public class AdminLoginRequestDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "密码不能为空")
    private String password;
}