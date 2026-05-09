package com.gpj.visitorsystem.service.wx.approver;

import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.util.AppointmentUtil;
import com.gpj.visitorsystem.util.AesEncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【业务模块】审批人端预约审批
 *
 * 【核心职责】
 * 1. 查询待审批与历史审批记录
 * 2. 审批通过/拒绝并更新预约状态
 * 3. 获取审批人视角预约详情
 *
 * 【关键业务场景】
 * 审批人仅能处理分配给自己的预约，审批前需校验是否过期与权限。
 *
 * 【依赖说明】
 * - AppointmentMapper：预约查询与状态更新
 * - AesEncryptUtil：身份证号解密展示
 * - UserMapper：审批人信息校验
 *
 * 【注意事项】
 * - 仅状态为0（待审核）的预约可审批
 * - 过期预约需先更新为已过期再提示
 * - 关键更新操作需加@Transactional保证一致性
 */
@Service
public class WxApproverAppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    /**
     * 查询待审批预约列表
     *
     * 【业务背景】
     * 审批人查看待审核预约，需先更新过期状态并解密身份证号。
     *
     * 【实现步骤】
     * 1. 更新过期待审核预约状态
     * 2. 查询待审核预约列表
     * 3. 解密身份证号字段
     *
     * 【参数说明】
     * @param approverId 审批人用户ID
     *
     * 【返回值】
     * @return 待审批预约列表
     *
     * 【异常情况】
     * @throws Exception 查询或解密失败时抛出
     *
     * 【事务说明】
     * 使用@Transactional保证状态更新一致性
     */
    @Transactional
    public List<Appointment> listPendingAppointments(Integer approverId) {
        appointmentMapper.expirePendingAppointments(LocalDateTime.now());
        List<Appointment> list = appointmentMapper.findPendingByApprover(approverId);
        // 解密身份证号（用于前端展示）
        for (Appointment app : list) {
            AppointmentUtil.decryptIdCard(app, aesEncryptUtil);
        }
        return list;
    }

    /**
     * 审批通过预约
     *
     * 【业务背景】
     * 审批通过后预约状态变更为已通过。
     *
     * 【实现步骤】
     * 1. 校验预约存在且状态为待审核
     * 2. 校验过期状态与审批人归属
     * 3. 更新预约状态为已通过并记录审批人
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param approverId 审批人用户ID
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、已被处理、已过期或无权限
     *
     * 【事务说明】
     * 使用@Transactional保证状态更新一致性
     */
    @Transactional
    public void approveAppointment(Long appointmentId, Integer approverId) {
        Appointment app = appointmentMapper.findById(appointmentId);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("预约不存在或已被处理");
        }
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentUtil.isPendingExpired(app.getExpectedEndTime(), now)) {
            appointmentMapper.updateStatus(appointmentId, 6);
            throw new BusinessException("预约已过期，无法审批");
        }
        // 检查是否已被其他审批人认领
        if (app.getApproverId() != null && !app.getApproverId().equals(Long.valueOf(approverId))) {
            throw new BusinessException("该预约已指派给其他审批人");
        }
        // 更新状态为 1（预约成功），记录审批人和审批时间
        appointmentMapper.approve(appointmentId, approverId, LocalDateTime.now());
    }

    /**
     * 审批拒绝预约
     *
     * 【业务背景】
     * 拒绝预约需填写拒绝理由并更新状态。
     *
     * 【实现步骤】
     * 1. 校验预约存在且状态为待审核
     * 2. 校验过期状态与拒绝理由
     * 3. 更新预约状态为已拒绝并记录审批人
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param approverId 审批人用户ID
     * @param reason 拒绝理由
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、已被处理、已过期或拒绝理由为空
     *
     * 【事务说明】
     * 使用@Transactional保证状态更新一致性
     */
    @Transactional
    public void rejectAppointment(Long appointmentId, Integer approverId, String reason) {
        Appointment app = appointmentMapper.findById(appointmentId);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("预约不存在或已被处理");
        }
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentUtil.isPendingExpired(app.getExpectedEndTime(), now)) {
            appointmentMapper.updateStatus(appointmentId, 6);
            throw new BusinessException("预约已过期，无法审批");
        }
        // 拒绝理由必填
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("拒绝理由不能为空");
        }
        // 更新状态为 2（预约失败），记录审批人、拒绝理由和审批时间
        appointmentMapper.reject(appointmentId, approverId, reason, LocalDateTime.now());
    }

    /**
     * 查询审批历史记录
     *
     * 【业务背景】
     * 审批人查看自己已处理的预约记录。
     *
     * 【实现步骤】
     * 1. 根据筛选条件查询历史记录
     * 2. 解密身份证号字段
     *
     * 【参数说明】
     * @param approverId 审批人用户ID
     * @param keyword 关键词（访客姓名/手机号，可为空）
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @param status 状态筛选（1-已通过，2-已拒绝，可为空）
     * @param searchType 搜索类型（create/process）
     *
     * 【返回值】
     * @return 历史预约列表
     *
     * 【异常情况】
     * @throws Exception 查询或解密失败时抛出
     *
     * 【事务说明】
     * 无
     */
    public List<Appointment> listApprovedHistory(Integer approverId, String keyword,
                                                 String startDate, String endDate,
                                                 Integer status, String searchType) {
        List<Appointment> list = appointmentMapper.findHistoryByApprover(approverId, keyword, startDate, endDate, status, searchType);
        // 解密身份证号
        for (Appointment app : list) {
            AppointmentUtil.decryptIdCard(app, aesEncryptUtil);
        }
        return list;
    }

    /**
     * 获取审批人视角的预约详情
     *
     * 【业务背景】
     * 审批人查看预约详情需校验审批归属与状态。
     *
     * 【实现步骤】
     * 1. 查询预约
     * 2. 根据状态校验审批人权限
     * 3. 解密身份证号字段
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param approverId 审批人用户ID
     *
     * 【返回值】
     * @return 预约详情
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在或无权限查看
     *
     * 【事务说明】
     * 无
     */
    public Appointment getApproverAppointmentDetail(Long appointmentId, Integer approverId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        Long approverIdLong = approverId == null ? null : approverId.longValue();
        // 待审核：NULL 或自己可查看
        if (appointment.getStatus() == 0) {
            if (appointment.getApproverId() != null && !appointment.getApproverId().equals(approverIdLong)) {
                throw new BusinessException("无权查看该预约");
            }
        } else {
            // 已处理：必须是自己审批的
            if (appointment.getApproverId() == null || !appointment.getApproverId().equals(approverIdLong)) {
                throw new BusinessException("无权查看该预约");
            }
        }
        // 解密身份证号
        AppointmentUtil.decryptIdCard(appointment, aesEncryptUtil);
        return appointment;
    }

}