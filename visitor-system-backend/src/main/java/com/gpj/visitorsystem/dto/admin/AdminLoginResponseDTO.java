package com.gpj.visitorsystem.dto.admin;

import lombok.Data;

/**
 * 【业务模块】管理员登录响应DTO
 *
 * 【核心职责】
 * 1. 封装管理端登录成功后的返回数据
 * 2. 返回用户基本信息和JWT认证令牌
 * 3. 前端需持久化存储token到localStorage
 *
 * 【关键业务场景】
 * 1. 管理员登录成功后返回，前端保存到localStorage
 * 2. 前端根据realName展示当前登录用户信息
 * 3. token用于后续接口鉴权（放在Authorization头中）
 * 4. token过期后需重新登录（管理端暂不支持refresh token）
 *
 * 【依赖说明】
 * - 由AdminUserService组装
 * - 被AdminAuthController封装到ResultDTO返回
 *
 * 【注意事项】
 * - token为JWT Token，有效期2小时
 * - 前端应将token持久化存储到localStorage
 * - 每次请求需在Authorization头中携带"Bearer {token}"
 * - token过期后前端需跳转登录页重新登录
 * - userType固定为4（管理员）
 */
@Data
public class AdminLoginResponseDTO {
    private Integer userId;
    private String phone;
    private String realName;
    private Integer userType;  // 1-访客 2-审批人 3-安保 4-管理员
    private String token;        // JWT Token，有效期2小时
}