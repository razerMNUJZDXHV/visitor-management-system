package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【业务模块】安保扫码验证响应DTO
 *
 * 【核心职责】
 * 1. 封装扫码验证后的结果信息
 * 2. 标识是否允许通行（passed）
 * 3. 判断是否需要二次确认（pendingConfirm）
 * 4. 返回预约详细信息和告警提示
 *
 * 【关键业务场景】
 * 1. 安保扫访客二维码后，后端验证Token并返回此DTO
 * 2. passed=true时展示预约信息，安保可确认通行
 * 3. passed=false时提示不能通行原因（如预约未通过、已过期）
 * 4. pendingConfirm=true时需二次确认（如超时滞留、紧急登记）
 * 5. 包含完整访客信息，安保核对身份后放行
 *
 * 【依赖说明】
 * - 由WxSecurityAccessService.scanVerify组装
 * - 包含预约信息和访客身份信息
 *
 * 【注意事项】
 * - passed：true表示允许通行，false表示禁止通行
 * - pendingConfirm：true表示需要二次确认（如超时滞留）
 * - alertType/alertMessage：告警类型和提示信息
 * - actionType：SIGN_IN-签到，SIGN_OUT-签离
 * - visitorIdCard为AES加密字符串，需解密后展示
 */
@Data
public class SecurityScanVerifyResponseDTO {
    private boolean passed;         // 是否允许通行
    private Boolean pendingConfirm; // 是否需要二次确认
    private String alertType;       // 告警类型
    private String alertMessage;    // 告警消息
    private String actionType;      // SIGN_IN-签到，SIGN_OUT-签离

    // ==================== 预约信息 ====================
    private Long appointmentId;     // 预约ID
    private Long visitorId;         // 访客ID
    private String visitorName;    // 访客姓名
    private String visitorPhone;   // 访客手机号
    private String visitorIdCard; // 访客身份证号（AES加密）
    private Integer accessType;     // 1-签到，2-签离
    private Integer verifyMethod;   // 1-扫码，2-手动
    private LocalDateTime accessTime; // 通行时间
}