package com.gpj.visitorsystem.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【业务模块】预约详情DTO（管理员用）
 *
 * 【核心职责】
 * 1. 封装管理员查看的预约详细信息
 * 2. 比普通Appointment多了审批人信息
 * 3. 支持管理端查看完整预约详情和审批记录
 *
 * 【关键业务场景】
 * 1. 管理员查看预约详情，了解访客信息和审批情况
 * 2. 展示审批人姓名和手机号，便于追溯责任
 * 3. 显示二维码信息和过期时间
 * 4. 管理端导出预约记录时包含审批人信息
 *
 * 【依赖说明】
 * - 由AdminAppointmentService查询组装
 * - 被AdminAppointmentController返回给管理端
 *
 * 【注意事项】
 * - visitorIdCard为AES加密，展示前需解密
 * - status：0-待审核 1-预约成功 2-预约失败 3-已取消 4-已签到 5-已完成 6-已过期
 * - qrCodeUrl为JWT二维码，预约成功后生成
 * - qrExpireTime为二维码过期时间，预计开始时间前失效
 * - processTime为审批处理时间，未审批时为null
 * - approverName/approverPhone为审批人信息，未审批时为null
 */
@Data
public class AdminAppointmentDetailDTO {
    private Long appointmentId;
    private Long visitorId;
    private String visitorName;
    private String visitorPhone;
    private String visitorIdCard;      // AES加密，需要解密后展示
    private String intervieweeName;    // 被访人姓名
    private String visitReason;         // 来访事由
    private LocalDateTime expectedStartTime;
    private LocalDateTime expectedEndTime;
    private Integer status;            // 预约状态
    private String qrCodeUrl;          // 签到二维码
    private LocalDateTime qrExpireTime;
    private String rejectReason;        // 拒绝原因
    private LocalDateTime createTime;
    private LocalDateTime processTime;

    // ==================== 审批人信息 ====================
    private Long approverId;      // 审批人ID
    private String approverName;   // 审批人姓名
    private String approverPhone;  // 审批人手机号

    // ==================== 删除限制校验字段 ====================
    /**
     * 是否滞留超时（仅状态=6已过期时用于删除限制校验）
     * true：已签到但未签离，属于滞留超时，禁止删除
     * false：未签到或已签离，允许删除
     */
    private Boolean overtimeStaying;
}
