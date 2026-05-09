package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.admin.AdminAppointmentDetailDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.service.admin.AdminAppointmentService;
import com.gpj.visitorsystem.service.common.UserService;
import com.gpj.visitorsystem.task.AppointmentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 【业务模块】管理后台预约审批接口
 *
 * 【核心职责】
 * 1. 查询待审批与历史审批记录
 * 2. 审批通过/拒绝预约
 * 3. 删除预约记录（单条/批量）
 *
 * 【关键业务场景】
 * 所有接口均需管理员权限（userType=4），通过拦截器解析Token获取userId。
 *
 * 【依赖说明】
 * - AdminAppointmentService：预约审批与记录管理
 *
 * 【注意事项】
 * - 待审核预约才能审批
 * - 删除仅允许已拒绝/已取消/已完成/已过期等状态
 */
@RestController
@RequestMapping("/api/admin/appointment")
public class AdminAppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAppointmentController.class);

    @Autowired
    private AdminAppointmentService adminAppointmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentTask appointmentTask;

    /**
     * 查询待审批预约列表
     *
     * 【接口说明】
     * 管理员查看待审批预约，自动更新过期状态并解密身份证号。
     *
     * 【请求参数】
     * @param adminId 管理员用户ID（从Token解析，通过@RequestAttribute传递）
     * @param scope 查询范围（all-全部，mine-仅自己）
     *
     * 【返回值】
     * @return 待审批预约列表
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/pending")
    public ResultDTO<List<Appointment>> pendingList(@RequestAttribute("userId") Integer adminId,
                                                    @RequestParam(required = false) String scope) {
        return ResultDTO.success(adminAppointmentService.listPendingAppointments(scope, adminId));
    }

    /**
     * 查询预约历史记录
     *
     * 【接口说明】
     * 支持按范围与条件筛选历史审批记录，返回详情列表。
     *
     * 【请求参数】
     * @param adminId 管理员用户ID（从Token解析，通过@RequestAttribute传递）
     * @param scope 查询范围（all-全部，mine-仅自己）
     * @param keyword 关键词（访客姓名/审批人姓名，可选）
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param status 状态筛选（1-已通过，2-已拒绝，可选）
     * @param searchType 搜索类型（create-按创建时间，process-按处理时间，默认process）
     *
     * 【返回值】
     * @return 历史预约详情DTO列表，身份证号已解密
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/history")
    public ResultDTO<List<AdminAppointmentDetailDTO>> historyList(@RequestAttribute("userId") Integer adminId,
                                                                   @RequestParam(defaultValue = "all") String scope,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) String startDate,
                                                                   @RequestParam(required = false) String endDate,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @RequestParam(defaultValue = "process") String searchType) {
        return ResultDTO.success(adminAppointmentService.listHistoryAppointments(scope, adminId, keyword, startDate, endDate, status, searchType));
    }

    /**
     * 查询预约详情
     *
     * 【接口说明】
     * 查询单条预约详情，包含审批人信息。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     *
     * 【返回值】
     * @return 预约详情DTO，包含审批人信息，身份证号已解密
     *
     * 【异常情况】
     * - 预约不存在：返回“预约不存在”
     */
    @GetMapping("/detail")
    public ResultDTO<AdminAppointmentDetailDTO> detail(@RequestParam Long appointmentId) {
        return ResultDTO.success(adminAppointmentService.getAppointmentDetail(appointmentId));
    }

    /**
     * 审批通过预约
     *
     * 【接口说明】
     * 将预约状态更新为已通过，并记录审批人。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param adminId 管理员用户ID（从Token解析，通过@RequestAttribute传递）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 预约不存在/已被处理/已过期：返回对应错误信息
     */
    @PostMapping("/approve")
    public ResultDTO<Void> approve(@RequestParam Long appointmentId,
                                   @RequestAttribute("userId") Integer adminId) {
        adminAppointmentService.approveAppointment(appointmentId, adminId);
        return ResultDTO.success(null);
    }

    /**
     * 审批拒绝预约
     *
     * 【接口说明】
     * 将预约状态更新为已拒绝并记录拒绝理由。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param reason 拒绝理由（必填）
     * @param adminId 管理员用户ID（从Token解析，通过@RequestAttribute传递）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 预约不存在/已被处理/已过期/拒绝理由为空：返回对应错误信息
     */
    @PostMapping("/reject")
    public ResultDTO<Void> reject(@RequestParam Long appointmentId,
                                  @RequestParam String reason,
                                  @RequestAttribute("userId") Integer adminId) {
        adminAppointmentService.rejectAppointment(appointmentId, adminId, reason);
        return ResultDTO.success(null);
    }

    /**
     * 删除预约记录
     *
     * 【接口说明】
     * 仅允许删除可删除状态的预约记录。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 预约不存在/状态不允许删除：返回对应错误信息
     */
    @DeleteMapping("/delete")
    public ResultDTO<Void> delete(@RequestParam Long appointmentId) {
        adminAppointmentService.deleteAppointment(appointmentId);
        return ResultDTO.success(null);
    }

    /**
     * 批量删除预约记录
     *
     * 【接口说明】
     * 批量删除指定预约记录，需全部满足可删除状态。
     *
     * 【请求参数】
     * @param request 请求体，包含appointmentIds（预约ID列表）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 预约不存在/存在不可删除记录：返回对应错误信息
     */
    @DeleteMapping("/batch-delete")
    public ResultDTO<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> appointmentIds = request.get("appointmentIds");
        adminAppointmentService.batchDeleteAppointments(appointmentIds);
        return ResultDTO.success(null);
    }

    /**
     * 【管理员】手动触发爽约判定
     *
     * 【接口说明】
     * 立即执行爽约判定任务，无需等待定时任务。
     * 用于数据修复或紧急处理场景。
     * 只有管理员(userType=4)可以执行此操作。
     *
     * 【请求参数】
     * 无
     *
     * 【返回值】
     * @return 处理结果详情（处理记录数）
     *
     * 【异常情况】
     * - 无权限：返回 403
     * - 系统异常：返回 500
     */
    @PostMapping("/trigger-no-show-check")
    public ResultDTO<String> triggerNoShowCheck(@RequestAttribute("userId") Integer adminId) {
        // 权限校验：只有管理员可以手动触发
        User admin = userService.findById(adminId);
        if (admin == null || admin.getUserType() == null || admin.getUserType() != 4) {
            return ResultDTO.error(403, "无权执行此操作，仅管理员可操作");
        }

        // 执行爽约判定
        int processedCount = appointmentTask.processNoShowAppointments();

        // 记录操作日志
        logger.warn("管理员 {} 手动触发爽约判定，处理记录数：{}", adminId, processedCount);

        return ResultDTO.success(String.format("爽约判定执行完成，共处理 %d 条过期预约记录", processedCount));
    }
}
