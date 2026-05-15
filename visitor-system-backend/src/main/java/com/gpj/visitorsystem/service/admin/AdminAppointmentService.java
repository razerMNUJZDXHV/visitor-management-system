package com.gpj.visitorsystem.service.admin;

import com.gpj.visitorsystem.dto.admin.AdminAppointmentDetailDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.mapper.AccessLogMapper;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.util.AppointmentUtil;
import com.gpj.visitorsystem.util.AesEncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【业务模块】管理后台预约审批
 *
 * 【核心职责】
 * 1. 查询待审批与历史审批记录（支持范围与条件筛选）
 * 2. 审批通过/拒绝与状态流转处理
 * 3. 删除预约记录（单条/批量）
 *
 * 【关键业务场景】
 * 1. 身份证号加密存储，列表与导出需先解密，失败展示***不中断业务
 * 2. 待审批预约需要自动过期处理，避免过期预约被审批
 * 3. 审批通过后生成二维码（JWT Token）供安保扫码通行
 *
 * 【依赖说明】
 * - AppointmentMapper：预约查询、状态流转与删除
 * - AesEncryptUtil：身份证号解密展示
 * - AppointmentUtil：工具方法（状态文本、身份证解密等）
 *
 * 【注意事项】
 * - 仅允许审批状态为0（待审核）的预约
 * - 删除仅允许已拒绝/已取消/已完成/已过期等可删除状态
 * - 涉及状态更新的方法需加@Transactional保证一致性
 */
@Service
public class AdminAppointmentService {

    private static final Set<Integer> DELETABLE_STATUS = Set.of(2, 3, 5, 6);

    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private AccessLogMapper accessLogMapper;
    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    /**
     * 查询待审批列表
     *
     * 【业务背景】
     * 管理员查看待审批预约，需要先处理过期状态并解密身份证号展示。
     *
     * 【实现步骤】
     * 1. 更新过期待审批预约状态
     * 2. 根据scope查询待审批列表
     * 3. 解密身份证号字段
     *
     * 【参数说明】
     * @param scope 查询范围（all-所有，mine-我的）
     * @param adminId 管理员用户ID
     *
     * 【返回值】
     * @return 待审批预约列表，身份证号已解密
     *
     * 【异常情况】
     * @throws Exception 查询失败时抛出
     *
     * 【事务说明】
     * 使用@Transactional保证状态更新一致性
     */
    @Transactional
    public List<Appointment> listPendingAppointments(String scope, Integer adminId) {
        LocalDateTime now = LocalDateTime.now();
        appointmentMapper.expirePendingAppointments(now);
        List<Appointment> list = appointmentMapper.findAdminPending(normalizeScope(scope), adminId);
        if (list != null) {
            list.forEach(app -> AppointmentUtil.decryptIdCard(app, aesEncryptUtil));
        }
        return list;
    }

    /**
     * 查询历史审批记录
     *
     * 【业务背景】
     * 管理员查看已处理预约记录，支持筛选条件与时间范围。
     *
     * 【实现步骤】
     * 1. 更新过期待审批状态
     * 2. 根据筛选条件查询历史记录
     * 3. 解密身份证号字段
     *
     * 【参数说明】
     * @param scope 查询范围（all-所有，mine-我的）
     * @param adminId 管理员用户ID
     * @param keyword 关键词（可选，搜索访客姓名、身份证号、来访事由）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @param status 状态筛选（可选：1-已处理，2-已拒绝）
     * @param searchType 搜索类型（可选：create-按创建时间，process-按处理时间）
     *
     * 【返回值】
     * @return 历史预约列表，包含访客信息、审批时间、审批意见等，身份证号已解密
     *
     * 【异常情况】
     * @throws Exception 查询失败时抛出
     *
     * 【事务说明】
     * 无（只读查询）
     */
    public List<AdminAppointmentDetailDTO> listHistoryAppointments(String scope, Integer adminId,
        String keyword, String startDate, String endDate,
        Integer status, String searchType) {
        appointmentMapper.expirePendingAppointments(LocalDateTime.now());
        List<AdminAppointmentDetailDTO> list = appointmentMapper.findAdminHistory(
                normalizeScope(scope), adminId, keyword, startDate, endDate, status, searchType);
        if (list != null) {
            list.forEach(dto -> {
                AppointmentUtil.decryptIdCard(dto, aesEncryptUtil);
                dto.setOvertimeStaying(isOvertimeStaying(dto.getAppointmentId(), dto.getStatus()));
            });
        }
        return list;
    }

    /**
     * 获取预约详情
     *
     * 【业务背景】
     * 管理端查看预约详情，需要处理过期状态并解密身份证号。
     *
     * 【实现步骤】
     * 1. 查询预约详情
     * 2. 若待审核则检查是否过期并更新状态
     * 3. 解密身份证号
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     *
     * 【返回值】
     * @return 预约详情DTO
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在
     *
     * 【事务说明】
     * 无（只读查询）
     */
    public AdminAppointmentDetailDTO getAppointmentDetail(Long appointmentId) {
        AdminAppointmentDetailDTO detail = appointmentMapper.findDetailById(appointmentId);
        if (detail == null) {
            throw new BusinessException("预约不存在");
        }
        applyPendingExpireIfNeeded(detail);
        AppointmentUtil.decryptIdCard(detail, aesEncryptUtil);
        detail.setOvertimeStaying(isOvertimeStaying(appointmentId, detail.getStatus()));
        return detail;
    }

    /**
     * 审批通过
     *
     * 【业务背景】
     * 管理端审批预约申请，通过后预约状态变更为已通过。
     *
     * 【实现步骤】
     * 1. 校验预约存在且状态为待审核
     * 2. 检查是否过期，过期则更新状态并抛异常
     * 3. 更新预约状态为已通过并记录审批人
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param adminId 管理员用户ID
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、已被处理或已过期
     *
     * 【事务说明】
     * 使用@Transactional保证状态更新一致性
     */
    @Transactional
    public void approveAppointment(Long appointmentId, Integer adminId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null || appointment.getStatus() != 0) {
            throw new BusinessException("预约不存在或已被处理");
        }
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentUtil.isPendingExpired(appointment.getExpectedEndTime(), now)) {
            appointmentMapper.updateStatus(appointmentId, 6);
            throw new BusinessException("预约已过期，无法审批");
        }
        appointmentMapper.approve(appointmentId, adminId, LocalDateTime.now());
    }

    /**
     * 审批拒绝
     *
     * 【业务背景】
     * 管理端拒绝预约申请并记录拒绝理由。
     *
     * 【实现步骤】
     * 1. 校验预约存在且状态为待审核
     * 2. 检查是否过期，过期则更新状态并抛异常
     * 3. 校验拒绝理由必填
     * 4. 更新预约状态为已拒绝并记录审批人
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param adminId 管理员用户ID
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
    public void rejectAppointment(Long appointmentId, Integer adminId, String reason) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null || appointment.getStatus() != 0) {
            throw new BusinessException("预约不存在或已被处理");
        }
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentUtil.isPendingExpired(appointment.getExpectedEndTime(), now)) {
            appointmentMapper.updateStatus(appointmentId, 6);
            throw new BusinessException("预约已过期，无法审批");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("拒绝理由不能为空");
        }
        appointmentMapper.reject(appointmentId, adminId, reason.trim(), LocalDateTime.now());
    }

    /**
     * 删除单条预约记录
     *
     * 【业务背景】
     * 管理端删除历史预约，仅允许可删除状态。
     *
     * 【实现步骤】
     * 1. 校验预约存在
     * 2. 校验状态允许删除
     * 3. 执行删除
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在或状态不允许删除
     *
     * 【事务说明】
     * 使用@Transactional保证删除原子性
     */
    @Transactional
    public void deleteAppointment(Long appointmentId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        if (!DELETABLE_STATUS.contains(appointment.getStatus())) {
            throw new BusinessException("当前状态记录不允许删除");
        }
        if (isOvertimeStaying(appointmentId, appointment.getStatus())) {
            throw new BusinessException("该记录访客已签到但未签离，请完成签离操作后再删除");
        }
        appointmentMapper.deleteById(appointmentId);
    }

    /**
     * 批量删除预约记录
     *
     * 【业务背景】
     * 管理端批量删除历史预约，需逐条校验状态。
     *
     * 【实现步骤】
     * 1. 校验ID列表非空
     * 2. 逐条校验预约存在且状态可删除
     * 3. 执行批量删除
     *
     * 【参数说明】
     * @param appointmentIds 预约ID列表
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在或存在不可删除记录
     *
     * 【事务说明】
     * 使用@Transactional保证删除一致性
     */
    @Transactional
    public void batchDeleteAppointments(List<Long> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }

        List<Long> distinctIds = appointmentIds.stream().distinct().collect(Collectors.toList());

        for (Long id : distinctIds) {
            Appointment appointment = appointmentMapper.findById(id);
            if (appointment == null) {
                throw new BusinessException("预约不存在：" + id);
            }
            if (!DELETABLE_STATUS.contains(appointment.getStatus())) {
                throw new BusinessException("存在不允许删除的记录（ID：" + id + "），请重新选择");
            }
            if (isOvertimeStaying(id, appointment.getStatus())) {
                throw new BusinessException("存在不允许删除的记录（ID：" + id + "），该记录访客已签到但未签离，请完成签离操作后再删除");
            }
        }

        appointmentMapper.deleteByIds(distinctIds);
    }

    private String normalizeScope(String scope) {
        return "mine".equalsIgnoreCase(scope) ? "mine" : "all";
    }

    // 待审批预约过期检查
    private void applyPendingExpireIfNeeded(AdminAppointmentDetailDTO detail) {
        if (detail == null || detail.getStatus() == null || detail.getStatus() != 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentUtil.isPendingExpired(detail.getExpectedEndTime(), now)) {
            appointmentMapper.updateStatus(detail.getAppointmentId(), 6);
            detail.setStatus(6);
        }
    }

    /**
     * 判断是否滞留超时（已过期且已签到但未签离）
     *
     * @param appointmentId 预约ID
     * @param status 预约状态
     * @return true：滞留超时，禁止删除；false：非滞留，允许删除
     */
    private boolean isOvertimeStaying(Long appointmentId, Integer status) {
        if (!Integer.valueOf(6).equals(status)) {
            return false;
        }
        int signInCount = accessLogMapper.countByAppointmentAndType(appointmentId, 1);
        int signOutCount = accessLogMapper.countByAppointmentAndType(appointmentId, 2);
        return signInCount > 0 && signOutCount == 0;
    }
}
