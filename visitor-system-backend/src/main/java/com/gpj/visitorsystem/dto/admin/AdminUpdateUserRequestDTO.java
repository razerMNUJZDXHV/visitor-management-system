package com.gpj.visitorsystem.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 【业务模块】管理员修改用户请求DTO
 *
 * 【核心职责】
 * 1. 封装管理端修改用户信息的请求参数
 * 2. 支持修改手机号、姓名、角色、密码等字段
 * 3. 密码为空时表示不修改密码
 *
 * 【关键业务场景】
 * 1. 管理员修改用户信息（如调整角色、更新姓名）
 * 2. 重置用户密码（填写新密码）
 * 3. 修改手机号（会清空openid/sessionKey，需重新登录）
 * 4. 调整用户角色（如审批人转安保）
 *
 * 【依赖说明】
 * - 被AdminUserController.updateUser接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - userId为必填，标识要修改的用户
 * - phone为选填，修改后会清空openid/sessionKey
 * - realName为选填，修改用户展示姓名
 * - userType为选填，修改用户角色
 * - password为选填，为空时不修改密码，填写则更新密码
 * - 修改手机号需谨慎，用户需重新登录小程序
 */
@Data
public class AdminUpdateUserRequestDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    private String phone;
    private String realName;
    private Integer userType;  // 1-访客 2-审批人 3-安保 4-管理员
    private String password;     // 为空时不修改密码
}