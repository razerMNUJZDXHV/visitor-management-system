package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

/**
 * 【业务模块】安保手动签离请求DTO
 *
 * 【核心职责】
 * 1. 封装安保手动签离的请求参数
 * 2. 支持访客二维码失效时的手动签离操作
 * 3. 需要授权人审批才能执行
 *
 * 【关键业务场景】
 * 1. 访客手机没电或二维码过期，无法扫码签离
 * 2. 安保手动输入预约ID，发起签离请求
 * 3. 需授权人（主管/队长）审批通过后执行
 * 4. 签离后更新预约状态为已完成，写入通行记录
 *
 * 【依赖说明】
 * - 被WxSecurityAccessController.manualSignOut接收
 *
 * 【注意事项】
 * - appointmentId为需要签离的预约ID
 * - 手动签离需授权人审批，不能安保直接执行
 * - 只有已签到的预约才能签离（状态=4）
 * - 签离后状态变为已完成（状态=5），不可再次签离
 */
@Data
public class SecurityManualSignOutRequestDTO {
    private Long appointmentId;  // 预约ID
}
