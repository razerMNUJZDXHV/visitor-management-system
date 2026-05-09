package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【业务模块】安保告警DTO
 *
 * 【核心职责】
 * 1. 封装安保端告警消息的完整信息
 * 2. 通过WebSocket实时推送给安保端
 * 3. 包含告警类型、访客信息、超时情况等
 *
 * 【关键业务场景】
 * 1. overstay告警：访客超过预计离开时间+30分钟宽限期未签离
 * 2. signOut告警：访客已到预计离开时间，提醒安保关注
 * 3. 告警包含完整访客信息，安保可快速定位处理
 * 4. canManualSignOut标识是否允许手动签离（需授权）
 *
 * 【依赖说明】
 * - 由SecurityAlertCenter生成并推送
 * - 被WxSecurityAccessController.getAlerts返回
 *
 * 【注意事项】
 * - alertType：overstay-超时滞留，signOut-该签离
 * - graceExpireTime：宽限期截止时间，超过此时间触发告警
 * - overstayMinutes：超时分钟数，用于展示严重程度
 * - visitorIdCard为AES加密，展示前需解密
 * - occurredAt：告警发生时间，用于排序和统计
 */
@Data
public class SecurityAlertDTO {
    private String alertType;           // 告警类型：overstay-超时滞留，signOut-该签离
    private String message;              // 告警消息
    private Long appointmentId;         // 预约ID
    private String visitorName;         // 访客姓名
    private String visitorPhone;        // 访客手机号
    private String visitorIdCard;       // 身份证号（AES加密）
    private String visitReason;         // 来访事由
    private LocalDateTime signInTime;   // 签到时间
    private LocalDateTime expectedStartTime; // 预计到达时间
    private LocalDateTime expectedEndTime;   // 预计离开时间
    private LocalDateTime graceExpireTime;   // 宽限期截止时间
    private Integer appointmentStatus;      // 预约状态
    private Boolean canManualSignOut;      // 是否允许手动签离
    private Integer overstayMinutes;        // 超时分钟数
    private LocalDateTime occurredAt;       // 告警发生时间
}