package com.gpj.visitorsystem.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 【业务模块】管理员创建用户请求DTO
 *
 * 【核心职责】
 * 1. 封装管理端创建用户的请求参数
 * 2. 支持创建访客、审批人、安保、管理员四种角色
 * 3. 根据角色类型决定是否必填密码
 *
 * 【关键业务场景】
 * 1. 管理员在后台创建审批人账号（userType=2，密码可空）
 * 2. 管理员在后台创建安保账号（userType=3，密码可空）
 * 3. 管理员在后台创建其他管理员（userType=4，密码必填）
 * 4. 访客由小程序端自行注册，一般不走此接口
 *
 * 【依赖说明】
 * - 被AdminUserController.createUser接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - phone为必填，格式为11位手机号，系统中唯一
 * - realName为必填，用于展示用户姓名
 * - userType为必填：1-访客 2-审批人 3-安保 4-管理员
 * - userType=4时password必填，其他类型可空（小程序端注册）
 * - 审批人/安保密码为空时，首次小程序登录需绑定手机号
 * - 创建前需校验手机号是否已存在
 */
@Data
public class AdminCreateUserRequestDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "用户类型不能为空")
    private Integer userType; // 1-访客 2-审批人 3-安保 4-管理员

    // userType=4时必须填密码，其他类型留空（小程序端注册）
    private String password;
}