package com.gpj.visitorsystem.service.wx.security;

import com.gpj.visitorsystem.dto.wx.security.SecurityAlertDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.util.AppointmentUtil;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【业务模块】安保告警中心
 *
 * 【核心职责】
 * 1. 发布与管理安保告警
 * 2. 统一计算宽限期与滞留分钟数
 * 3. 提供告警列表与状态更新能力
 *
 * 【关键业务场景】
 * 告警存储在内存中，服务重启会丢失，但告警由定时扫描重新生成。
 * 宽限期为预计离开时间+30分钟，超过才触发签离超时告警。
 *
 * 【依赖说明】
 * - ConcurrentHashMap：内存告警存储
 * - Appointment：告警需要关联预约与访客信息
 *
 * 【注意事项】
 * - 告警为内存态，重启后需重新扫描生成
 * - 告警发布需保证必填信息完整，避免前端展示异常
 */
@Component
public class SecurityAlertCenter {

    /** 活跃的告警Map（key: alertType-appointmentId） */
    private final Map<String, SecurityAlertDTO> activeAlerts = new ConcurrentHashMap<>();

    /**
     * 【功能】发布告警（简化版，无预约实体）
     *
     * 【业务背景】
     * 适用于不需要预约详情的告警场景，如系统告警、一般性通知等。
     *
     * 【实现逻辑】
     * 1. 接收告警类型、告警信息、预约ID、访客姓名
     * 2. 调用完整版publishInternal方法（其他字段为null）
     * 3. 将告警存入activeAlerts Map
     *
     * 【参数说明】
     * @param alertType 告警类型（如：APPROVAL_TIMEOUT、SECURITY_ISSUE）
     * @param message 告警信息（展示给安保人员的描述）
     * @param appointmentId 预约ID（可为null，用于关联预约）
     * @param visitorName 访客姓名（可为null，用于展示）
     *
     * 【注意事项】
     * - 此方法是重载方法之一，适用于简单告警场景
     * - 会调用完整版publishInternal（14个参数）
     * - 告警存入内存Map，服务重启会丢失
     */
    public void publish(String alertType, String message, Long appointmentId, String visitorName) {
        // 调用完整版，其他字段为null（14个参数）
        publishInternal(alertType, message, 
                appointmentId, visitorName, 
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 【功能】发布告警（含预约实体，自动计算宽限过期时间和滞留分钟数）
     *
     * 【业务背景】
     * 适用于有预约实体的告警场景，如签离超时、预约即将过期等。
     * 会自动计算宽限过期时间和滞留分钟数。
     *
     * 【实现逻辑】
     * 1. 判断预约实体是否为null，为null则调用简化版
     * 2. 计算宽限过期时间（expectedEndTime + 30分钟）
     * 3. 计算滞留分钟数（如果未提供）
     * 4. 调用完整版publishInternal方法
     * 5. 将告警存入activeAlerts Map
     *
     * 【参数说明】
     * @param alertType 告警类型（如：SIGN_OUT_TIMEOUT、APPROVAL_TIMEOUT）
     * @param message 告警信息（展示给安保人员的描述）
     * @param appointment 预约实体（包含访客信息、时间信息等）
     * @param canManualSignOut 是否可以手动签离（true-可手动签离，false-不可）
     * @param overstayMinutes 滞留分钟数（可为null，方法会自动计算）
     *
     * 【注意事项】
     * - 宽限期为30分钟（QR_GRACE_MINUTES）
     * - 滞留分钟数 = 当前时间 - 宽限过期时间
     * - 此方法是重载方法之一，最常用
     */
    public void publish(String alertType,
                        String message,
                        Appointment appointment,
                        Boolean canManualSignOut,
                        Integer overstayMinutes) {
        // 如果预约为null，使用简化版
        if (appointment == null) {
            publish(alertType, message, (Long) null, (String) null);
            return;
        }
        // 计算宽限过期时间和滞留分钟数（委托给通用方法）
        Object[] calc = resolveGraceAndOverstay(appointment, overstayMinutes);
        LocalDateTime expectedEndTime = (LocalDateTime) calc[0];
        LocalDateTime graceExpireTime = (LocalDateTime) calc[1];
        Integer resolvedOverstay = (Integer) calc[2];
        // 调用完整版（14个参数）
        publishInternal(alertType,
                message,
                appointment.getAppointmentId(),
                appointment.getVisitorName(),
                appointment.getVisitorPhone(),
                appointment.getExpectedStartTime(),
                expectedEndTime,
                graceExpireTime,
                canManualSignOut,
                resolvedOverstay,
                appointment.getStatus(),
                null,  // visitorIdCard 未在此重载中提供
                null,  // visitReason 未在此重载中提供
                null   // signInTime 未在此重载中提供
        );
    }

    /**
     * 【功能】发布告警（含预约实体和额外信息）
     */
    public void publish(String alertType,
                        String message,
                        Appointment appointment,
                        Boolean canManualSignOut,
                        Integer overstayMinutes,
                        String visitorIdCard,
                        String visitReason,
                        LocalDateTime signInTime) {
        // 如果预约为null，使用简化版
        if (appointment == null) {
            publish(alertType, message, (Long) null, (String) null);
            return;
        }
        // 计算宽限过期时间和滞留分钟数（委托给通用方法）
        Object[] calc = resolveGraceAndOverstay(appointment, overstayMinutes);
        LocalDateTime expectedEndTime = (LocalDateTime) calc[0];
        LocalDateTime graceExpireTime = (LocalDateTime) calc[1];
        Integer resolvedOverstay = (Integer) calc[2];
        // 调用完整版（14个参数）
        publishInternal(alertType,
                message,
                appointment.getAppointmentId(),
                appointment.getVisitorName(),
                appointment.getVisitorPhone(),
                appointment.getExpectedStartTime(),
                expectedEndTime,
                graceExpireTime,
                canManualSignOut,
                resolvedOverstay,
                appointment.getStatus(),
                visitorIdCard,
                visitReason,
                signInTime
        );
    }

    /**
     * 统一计算宽限过期时间和滞留分钟数
     *
     * 【业务背景】
     * 两个 publish 重载都需要计算：
     * 1. 宽限过期时间 = expectedEndTime + QR_GRACE_MINUTES
     * 2. 滞留分钟数（如果调用方未提供）
     *
     * 【参数说明】
     * @param appointment 预约实体
     * @param overstayMinutes 调用方提供的滞留分钟数（可为null）
     *
     * 【返回值】
     * @return Object[] {expectedEndTime, graceExpireTime, resolvedOverstay}
     */
    private Object[] resolveGraceAndOverstay(Appointment appointment, Integer overstayMinutes) {
        LocalDateTime expectedEndTime = appointment.getExpectedEndTime();
        LocalDateTime graceExpireTime = expectedEndTime == null ? null : expectedEndTime.plusMinutes(AppointmentUtil.QR_GRACE_MINUTES);
        Integer resolvedOverstay = overstayMinutes;
        if (resolvedOverstay == null && graceExpireTime != null) {
            long minutes = Duration.between(graceExpireTime, LocalDateTime.now()).toMinutes();
            resolvedOverstay = (int) Math.max(minutes, 0);
        }
        return new Object[]{expectedEndTime, graceExpireTime, resolvedOverstay};
    }

    /**
     * 【功能】发布告警（完整版，内部使用）
     *
     * 【业务背景】
     * 此方法是所有publish重载方法的最终调用方法。
     * 负责构建SecurityAlertDTO并存入activeAlerts Map。
     *
     * 【实现逻辑】
     * 1. 构建SecurityAlertDTO对象
     * 2. 设置所有告警字段（告警类型、消息、访客信息、时间信息等）
     * 3. 生成key（alertType-appointmentId）
     * 4. 存入activeAlerts Map
     *
     * 【参数说明】
     * @param alertType 告警类型
     * @param message 告警信息
     * @param appointmentId 预约ID（可为null）
     * @param visitorName 访客姓名（可为null）
     * @param visitorPhone 访客手机号（可为null）
     * @param expectedStartTime 预计到达时间（可为null）
     * @param expectedEndTime 预计离开时间（可为null）
     * @param graceExpireTime 宽限过期时间（可为null）
     * @param canManualSignOut 是否可以手动签离
     * @param overstayMinutes 滞留分钟数（可为null）
     * @param appointmentStatus 预约状态（可为null）
     * @param visitorIdCard 身份证号（可为null）
     * @param visitReason 来访事由（可为null）
     * @param signInTime 签到时间（可为null）
     *
     * 【注意事项】
     * - 此方法是private的，只允许类内部调用
     * - key格式：alertType-appointmentId（appointmentId为null时使用"none"）
     * - 如果key已存在，会覆盖之前的告警
     */
    private void publishInternal(String alertType,
                         String message,
                         Long appointmentId,
                         String visitorName,
                         String visitorPhone,
                         LocalDateTime expectedStartTime,
                         LocalDateTime expectedEndTime,
                         LocalDateTime graceExpireTime,
                         Boolean canManualSignOut,
                         Integer overstayMinutes,
                         Integer appointmentStatus,
                         String visitorIdCard,
                         String visitReason,
                         LocalDateTime signInTime) {
        // 构建告警DTO
        SecurityAlertDTO dto = new SecurityAlertDTO();
        dto.setAlertType(alertType);
        dto.setMessage(message);
        dto.setAppointmentId(appointmentId);
        dto.setVisitorName(visitorName);
        dto.setVisitorPhone(visitorPhone);
        dto.setVisitorIdCard(visitorIdCard);
        dto.setVisitReason(visitReason);
        dto.setSignInTime(signInTime);
        dto.setExpectedStartTime(expectedStartTime);
        dto.setExpectedEndTime(expectedEndTime);
        dto.setGraceExpireTime(graceExpireTime);
        dto.setCanManualSignOut(canManualSignOut != null && canManualSignOut);
        dto.setOverstayMinutes(overstayMinutes);
        dto.setAppointmentStatus(appointmentStatus);
        dto.setOccurredAt(LocalDateTime.now());

        // 存入Map（key: alertType-appointmentId）
        String key = alertType + "-" + (appointmentId == null ? "none" : appointmentId);
        activeAlerts.put(key, dto);
    }

    /**
     * 【功能】拉取所有活跃告警
     *
     * 【接口地址】此方法为内部方法，供Controller调用
     *
     * 【业务背景】
     * 安保人员查看告警列表时调用此接口。
     * 只读取告警列表，不清除告警（清除操作由业务处理完成后手动调用）。
     *
     * 【实现逻辑】
     * 1. 从activeAlerts Map中获取所有告警
     * 2. 转换为List
     * 3. 按发生时间倒序排列（最新的在前）
     * 4. 返回告警列表
     *
     * 【返回值】
     * @return 告警DTO列表，按发生时间倒序排列
     *
     * 【注意事项】
     * - 此方法只读取不清空（peek含义）
     * - 告警在业务处理完成后需手动调用clearByAppointmentId清除
     * - 如果告警已处理但未清除，会一直显示在列表中
     */
    public List<SecurityAlertDTO> peek() {
        List<SecurityAlertDTO> result = new ArrayList<>(activeAlerts.values());
        result.sort(Comparator.comparing(SecurityAlertDTO::getOccurredAt).reversed());
        return result;
    }

    /**
     * 【功能】清除指定预约的所有告警
     *
     * 【业务背景】
     * 当预约状态变更（如审批通过、签到、签离）时，
     * 需要清除该预约相关的告警信息。
     *
     * 【实现逻辑】
     * 1. 判断预约ID是否为null，为null则直接返回
     * 2. 遍历activeAlerts的key，删除所有以"-appointmentId"结尾的key
     * 3. 清除完成
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     *
     * 【注意事项】
     * - 预约ID为null时直接返回，不做任何操作
     * - 会删除该预约的所有类型告警（如审批超时、签离超时等）
     * - 此方法在预约状态变更时调用（如审批通过、签到、签离）
     */
    public void clearByAppointmentId(Long appointmentId) {
        if (appointmentId == null) {
            return;
        }
        // 删除所有以 "-appointmentId" 结尾的key
        String suffix = "-" + appointmentId;
        activeAlerts.keySet().removeIf(key -> key.endsWith(suffix));
    }
}
