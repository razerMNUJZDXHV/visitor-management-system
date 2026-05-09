package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

/**
 * 【业务模块】安保扫码确认请求DTO
 *
 * 【核心职责】
 * 1. 封装安保确认通行的请求参数
 * 2. 传递预约ID供后端更新状态
 * 3. 是扫码通行流程的第二步（确认写库）
 *
 * 【关键业务场景】
 * 1. 安保扫码后看到预约信息，确认访客身份无误
 * 2. 安保点击"确认通行"，前端传appointmentId
 * 3. 后端更新预约状态（已签到/已完成）并写入通行记录
 * 4. 扫码核验为预检，此接口才真正写库
 *
 * 【依赖说明】
 * - 被WxSecurityAccessController.scanConfirm接收
 *
 * 【注意事项】
 * - appointmentId为扫码验证时返回的预约ID
 * - 确认后状态变更不可逆（已签到/已完成）
 * - 涉及状态更新，后端加@Transactional保证原子性
 * - 确认成功后返回更新结果，失败返回错误信息
 */
@Data
public class SecurityScanConfirmRequestDTO {
    private Long appointmentId;  // 预约ID
}
