package com.gpj.visitorsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 【业务模块】通行记录实体
 *
 * 【核心职责】
 * 1. 记录每次签到/签离的详细信息
 * 2. 区分扫码通行与手动登记两种核验方式
 * 3. 关联预约信息与安保操作人员
 * 4. 支持后续审计查询与流量统计
 *
 * 【关键业务场景】
 * 1. 访客出示二维码，安保扫码后自动写入签到记录（verifyMethod=1）
 * 2. 访客二维码失效或手机没电时，安保手动登记写入记录（verifyMethod=2）
 * 3. 手动登记需主管授权（authorizerId），记录授权人信息
 * 4. 管理端导出通行记录用于留档审计
 *
 * 【依赖说明】
 * - 与Appointment关联：appointmentId关联预约记录
 * - 与User关联：visitorId关联访客，securityId关联安保
 *
 * 【注意事项】
 * - accessType：1-签到 2-签离
 * - verifyMethod：1-扫码 2-手动登记
 * - 手动登记时authorizerId必填，扫码时可为null
 * - accessTime记录实际通行时间，与预约的expectedStartTime可能不同
 */
@Data
public class AccessLog {
    private Long logId;
    private Long appointmentId;  // 关联的预约ID
    private Long visitorId;     // 访客ID
    private Long securityId;    // 执行核验的安保ID

    // 通行类型：1-签到 2-签离
    private Integer accessType;

    private LocalDateTime accessTime;  // 通行时间

    // 核验方式：1-扫码（访客出示二维码） 2-手动登记（安保输入信息）
    private Integer verifyMethod;

    private Long authorizerId;   // 手动登记时的授权人ID（主管/队长）
}