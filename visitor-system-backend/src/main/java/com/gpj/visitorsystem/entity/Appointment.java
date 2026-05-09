package com.gpj.visitorsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 【业务模块】预约实体
 *
 * 【核心职责】
 * 1. 存储访客提交的预约申请信息
 * 2. 记录预约状态流转（待审核→预约成功→已签到→已完成）
 * 3. 管理审批结果与拒绝原因
 * 4. 存储签到二维码信息供安保核验
 *
 * 【关键业务场景】
 * 1. 状态流转：
 *    0-待审核 → 1-预约成功（审批人通过）→ 4-已签到（安保核验）→ 5-已完成（签离）
 *                ↘ 2-预约失败（审批人拒绝）
 *    访客可取消：3-已取消
 *    超时未签到：6-已过期
 * 2. 访客提交预约时填写被访人、来访事由、预计时间段
 * 3. 审批通过后生成JWT二维码，访客出示给安保扫码通行
 * 4. 爽约判定：超过预计结束时间+30分钟宽限期未签到即视为爽约
 *
 * 【依赖说明】
 * - 与User关联：visitorId关联访客，approverId关联审批人
 * - 与AccessLog关联：appointmentId关联通行记录
 *
 * 【注意事项】
 * - visitorIdCard身份证号使用AES加密存储，查询时解密展示
 * - qrCodeUrl包含JWT Token，预约成功后生成，预计开始时间前失效
 * - 预约状态变更需保证原子性，涉及多表更新时加事务
 */
@Data
public class Appointment {
    private Long appointmentId;
    private Long visitorId;      // 访客ID
    private Long approverId;     // 审批人ID，审批通过后填充
    private String visitorName;  // 访客姓名（手动填写，非账号真实姓名）
    private String visitorPhone; // 访客联系电话
    private String visitorIdCard; // 身份证号，AES加密存储
    private String intervieweeName;  // 被访人姓名
    private String visitReason;      // 来访事由
    private LocalDateTime expectedStartTime;  // 预计到达时间
    private LocalDateTime expectedEndTime;    // 预计离开时间

    // 预约状态：0-待审核 1-预约成功 2-预约失败 3-已取消 4-已签到 5-已完成 6-已过期
    private Integer status;

    private String qrCodeUrl;       // 签到二维码URL，预约成功后生成
    private LocalDateTime qrExpireTime;  // 二维码过期时间（预计开始时间前失效）
    private String rejectReason;    // 审批人拒绝原因
    private LocalDateTime createTime;    // 提交时间
    private LocalDateTime processTime;   // 审批处理时间
}