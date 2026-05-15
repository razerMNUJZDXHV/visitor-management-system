package com.gpj.visitorsystem.mapper;

import com.gpj.visitorsystem.dto.admin.AdminAppointmentDetailDTO;
import com.gpj.visitorsystem.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【业务模块】预约表Mapper
 *
 * 【核心职责】
 * 1. 预约数据的增删改查（CRUD）
 * 2. 支持按访客、审批人、状态、时间等条件查询
 * 3. 提供爽约判定所需的查询方法
 * 4. 提供安保端所需的待签离、超时滞留查询
 * 5. 管理端分页查询和统计
 *
 * 【关键业务场景】
 * 1. 访客创建预约：insert插入新预约记录（status=0待审核）
 * 2. 审批人查询待审批：listByApproverIdAndStatus查询待审核列表
 * 3. 审批通过/拒绝：updateStatus更新状态和审批人
 * 4. 爽约判定：findLatestRelevantBefore查找上一次爽约记录，countAccessLogByAppointmentAndType检查是否已签到
 * 5. 安保查询待签离：listLeaveTimeReached找该签离的预约
 * 6. 超时滞留查询：listOvertimeStaying找超时滞留的预约
 * 7. 管理端列表：listAppointmentsPage分页查询，countAppointments查总数
 *
 * 【依赖说明】
 * - 对应数据库appointment表
 * - 被WxVisitorAppointmentService使用：访客预约管理
 * - 被WxApproverAppointmentService使用：审批人审批
 * - 被WxSecurityAccessService使用：安保通行管理
 * - 被AdminAppointmentService使用：管理端预约管理
 *
 * 【注意事项】
 * - 状态字段：0-待审核 1-预约成功 2-预约失败 3-已取消 4-已签到 5-已完成 6-已过期
 * - visitorIdCard使用AES加密存储，查询时解密
 * - 分页查询建议拆分count和list两个SQL
 * - 爽约判定涉及时间比较，注意时区问题（系统使用服务器时区）
 * - 定时任务扫描过期预约，将超时未处理的待审核预约标记为已过期
 */
@Mapper
public interface AppointmentMapper {

    /**
     * 新增预约
     */
    int insert(Appointment appointment);

    /**
     * 根据访客ID查预约列表
     * 小程序端"我的预约"用这个
     */
    List<Appointment> listByVisitorId(@Param("visitorId") Integer visitorId);

    /**
     * 根据预约ID查详情
     */
    Appointment findById(@Param("appointmentId") Long appointmentId);

    /**
     * 根据状态查询预约列表
     * 定时任务用：查询所有状态为指定值的预约
     */
    List<Appointment> listByStatus(@Param("status") Integer status);

    /**
     * 根据状态和访客ID查询预约列表
     * 实时处罚用：查询指定访客所有状态为指定值的预约
     */
    List<Appointment> listByStatusAndVisitorId(@Param("status") Integer status,
                                               @Param("visitorId") Integer visitorId);

    // ==================== 爽约判定相关 ====================

    /**
     * 查找上一次已处理的预约（爽约判定用）
     * 在beforeTime之前，状态是已处理（成功/失败/取消/已完成）的预约
     * 用来判断这次爽约是否应该处罚
     */
    Appointment findLatestRelevantBefore(@Param("visitorId") Long visitorId,
                                         @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定预约的通行记录数量（用于爽约判定：检查是否已签到）
     * @param appointmentId 预约ID
     * @param accessType 1-签到，2-签离
     */
    int countAccessLogByAppointmentAndType(@Param("appointmentId") Long appointmentId,
                                           @Param("accessType") Integer accessType);

    // ==================== 预约状态更新 ====================

    /**
     * 更新预约状态
     */
    int updateStatus(@Param("appointmentId") Long appointmentId, @Param("status") Integer status);

    /**
     * 过期处理：将超时未签到的预约标记为已过期
     * 定时任务调用，now是当前时间
     */
    int expirePendingAppointments(@Param("now") LocalDateTime now);

    /**
     * 更新预约二维码
     * 预约成功后生成二维码，写入qrCodeUrl和qrExpireTime
     */
    int updateQrCode(@Param("appointmentId") Long appointmentId,
                     @Param("qrCodeUrl") String qrCodeUrl,
                     @Param("qrExpireTime") String qrExpireTime);

    /**
     * 审批通过
     * 更新状态为1（预约成功），同时写入审批人ID和处理时间
     */
    int approve(@Param("appointmentId") Long appointmentId,
                @Param("approverId") Integer approverId,
                @Param("processTime") LocalDateTime processTime);

    /**
     * 审批拒绝
     * 更新状态为2（预约失败），写入拒绝原因
     */
    int reject(@Param("appointmentId") Long appointmentId,
               @Param("approverId") Integer approverId,
               @Param("reason") String reason,
               @Param("processTime") LocalDateTime processTime);

    // ==================== 删除和查询 ====================

    /**
     * 删除预约（物理删除）
     * 只有管理员能删，删除前要判断状态
     */
    int deleteById(@Param("appointmentId") Long appointmentId);

    /**
     * 根据审批人ID查预约列表
     */
    List<Appointment> findByApproverId(@Param("approverId") Integer approverId);

    /**
     * 重新分配审批人
     * 管理员可以把预约分配给其他审批人
     */
    int updateApprover(@Param("appointmentId") Long appointmentId, @Param("approverId") Integer approverId);

    /**
     * 更新处理时间
     */
    int updateProcessTime(@Param("appointmentId") Long appointmentId,
                          @Param("processTime") LocalDateTime processTime);

    // ==================== 审批人相关查询 ====================

    /**
     * 查审批人待处理预约
     */
    List<Appointment> findPendingByApprover(@Param("approverId") Integer approverId);

    /**
     * 查审批人历史预约（支持筛选）
     * @param searchType visitor-按访客名搜索，interviewee-按被访人搜索
     */
    List<Appointment> findHistoryByApprover(@Param("approverId") Integer approverId,
                                            @Param("keyword") String keyword,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("status") Integer status,
                                            @Param("searchType") String searchType);

    // ==================== 管理员查询 ====================

    /**
     * 查管理员待处理预约
     * @param scope all-全部，my-自己审批的
     */
    List<Appointment> findAdminPending(@Param("scope") String scope,
                                       @Param("adminId") Integer adminId);

    // ==================== 安保相关查询 ====================

    /**
     * 查该签离的预约（预计离开时间已到）
     * @param graceStart 宽限期内开始时间（避免太早提示签离）
     */
    List<Appointment> listLeaveTimeReached(@Param("now") LocalDateTime now,
                                           @Param("graceStart") LocalDateTime graceStart);

    /**
     * 查超时滞留的预约（超过离开时间未签离）
     * @param graceDeadline 宽限期截止时间
     */
    List<Appointment> listOvertimeStaying(@Param("graceDeadline") LocalDateTime graceDeadline);

    /**
     * 统计当前超时滞留的预约数量
     * 用途：管理端异常监控
     */
    long countOvertimeStaying(@Param("graceDeadline") LocalDateTime graceDeadline);

    /**
     * 统计今日爽约的预约数量
     * 条件：status=6 且 expected_end_time 在今日范围内 且无签到记录
     * 用途：保留统计，供后续扩展或报表使用；仪表盘当前主要展示 bannedUserCount
     */
    long countTodayNoShow(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /**
     * 查当前生效的紧急预约（无需审批，直接生效）
     */
    List<Appointment> listEmergencyActive();

    // ==================== 管理员历史记录（关联审批人信息）====================

    /**
     * 查管理员历史预约（关联审批人姓名）
     * 用AdminAppointmentDetailDTO返回，包含审批人信息
     */
    List<AdminAppointmentDetailDTO> findAdminHistory(@Param("scope") String scope,
                                                      @Param("adminId") Integer adminId,
                                                      @Param("keyword") String keyword,
                                                      @Param("startDate") String startDate,
                                                      @Param("endDate") String endDate,
                                                      @Param("status") Integer status,
                                                      @Param("searchType") String searchType);

    /**
     * 查预约详情（包含审批人信息）
     */
    AdminAppointmentDetailDTO findDetailById(@Param("appointmentId") Long appointmentId);

    /**
     * 批量删除预约
     * 管理员可以批量删除历史记录
     */
    int deleteByIds(@Param("appointmentIds") List<Long> appointmentIds);

    // ==================== 统计方法 ====================

    /**
     * 按状态统计预约数量
     */
    long countByStatus(@Param("status") Integer status);

    /**
     * 统计今日预约总数
     */
    long countTodayAppointments();

    /**
     * 统计指定日期的预约数量
     * @param date 格式：yyyy-MM-dd
     */
    long countByDate(@Param("date") String date);
}
