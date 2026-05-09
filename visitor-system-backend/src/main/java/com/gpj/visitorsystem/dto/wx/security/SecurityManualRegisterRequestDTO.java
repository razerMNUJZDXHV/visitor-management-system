package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

/**
 * 【业务模块】安保手动登记请求DTO
 *
 * 【核心职责】
 * 1. 封装安保手动登记通行的请求参数
 * 2. 支持有预约手动登记和紧急无预约登记两种场景
 * 3. 紧急登记需授权人审批
 *
 * 【关键业务场景】
 * 1. scenario=1：访客有预约但二维码失效，安保手动登记签到/签离
 * 2. scenario=2：访客无预约但有紧急情况，安保手动登记通行
 * 3. scenario=2需授权人审批（authorizerId必填）
 * 4. 登记后写入通行记录，scenario=2标记为紧急通行
 *
 * 【依赖说明】
 * - 被WxSecurityAccessController.manualRegister接收
 *
 * 【注意事项】
 * - accessType：1-签到 2-签离
 * - scenario：1-有预约 2-紧急无预约
 * - appointmentId：scenario=1时必填，关联已有预约
 * - authorizerId：scenario=2时必填，授权审批人ID
 * - visitorIdCard：身份证号，AES加密存储
 * - 紧急登记标记为emergency=true，用于统计和审计
 */
@Data
public class SecurityManualRegisterRequestDTO {
    private Integer accessType;    // 1-签到，2-签离
    private Integer scenario;     // 1-有预约，2-紧急无预约
    private Long appointmentId;  // 关联预约ID（scenario=1时必填）
    private String visitorName;   // 访客姓名
    private String visitorPhone;  // 访客手机号
    private String visitorIdCard; // 访客身份证号（AES加密）
    private String visitReason;    // 访问事由
    private Long authorizerId;   // 授权审批人ID（scenario=2时必填）
}