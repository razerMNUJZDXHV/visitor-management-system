package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【业务模块】安保端通行记录DTO
 *
 * 【核心职责】
 * 1. 封装安保端查询的通行记录详细信息
 * 2. 区分扫码通行与手动登记的记录
 * 3. 包含访客信息、安保信息、授权人信息
 * 4. 支持管理端审计和统计
 *
 * 【关键业务场景】
 * 1. 安保端查询今日/历史通行记录列表
 * 2. 扫码通行记录关联预约，手动登记记录可能无关联预约
 * 3. 手动登记时visitorNameAlt/visitorPhoneAlt为实际填写值
 * 4. 管理端导出通行记录用于留档审计
 *
 * 【依赖说明】
 * - 由AccessLogMapper查询组装
 * - 被WxSecurityAccessService返回给安保端
 *
 * 【注意事项】
 * - accessType：1-签到 2-签离
 * - verifyMethod：1-扫码 2-手动登记
 * - visitorNameAlt/visitorPhoneAlt：手动登记时填写的备用信息
 * - visitorIdCard为AES加密，展示前需解密
 * - emergency=true表示紧急无预约通行
 * - appointmentStatus关联预约状态，用于判断记录是否可删除
 */
@Data
public class SecurityAccessRecordDTO {
    private Long logId;           // 通行记录ID
    private Long appointmentId;   // 关联的预约ID（手动登记时为null）
    private Long visitorId;       // 访客ID
    private Long securityId;       // 安保操作人ID
    private Integer accessType;    // 1-签到 2-签离
    private Integer verifyMethod;   // 1-扫码 2-手动登记
    private Long authorizerId;    // 手动登记时的授权人ID
    private String authorizerName;  // 授权人姓名
    private String authorizerPhone; // 授权人手机号
    private LocalDateTime accessTime; // 通行时间

    // ==================== 访客信息 ====================
    private String visitorName;     // 访客姓名（来自预约）
    private String visitorNameAlt;  // 访客姓名（备用，手动登记时填的）
    private String visitorPhone;    // 访客手机号（来自预约）
    private String visitorPhoneAlt; // 访客手机号（备用）
    private String visitorIdCard;  // 身份证号（AES加密）
    private String visitReason;     // 来访事由
    private LocalDateTime expectedStartTime; // 预计到达时间
    private LocalDateTime expectedEndTime;   // 预计离开时间

    // ==================== 安保信息 ====================
    private String securityName;    // 安保姓名
    private String securityPhone;   // 安保手机号

    private Boolean emergency;     // 是否紧急通行（无需预约）

    // ==================== 关联状态 ====================
    private Integer appointmentStatus; // 关联预约状态，判断是否可删除
}
