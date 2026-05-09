package com.gpj.visitorsystem.controller.wx.approver;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.service.wx.approver.WxApproverAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【业务模块】审批人端预约接口
 *
 * 【核心职责】
 * 1. 查询待审批与审批历史
 * 2. 审批通过/拒绝预约
 * 3. 获取审批人视角预约详情
 *
 * 【关键业务场景】
 * 审批人仅能处理分配给自己的预约，审批结果通过通知推送给访客。
 *
 * 【依赖说明】
 * - WxApproverAppointmentService：审批业务处理
 *
 * 【注意事项】
 * - 仅状态为0（待审核）的预约可审批
 * - 预约详情包含敏感信息需脱敏处理
 */
@RestController
@RequestMapping("/api/wx/approver/appointment")
public class WxApproverAppointmentController {

    @Autowired
    private WxApproverAppointmentService wxApproverAppointmentService;

    /**
     * 获取待审批列表
     *
     * 【接口说明】
     * 返回当前审批人可处理的待审核预约列表。
     *
     * 【请求参数】
     * @param approverId 审批人用户ID（从JWT中获取，通过@RequestAttribute注入）
     *
     * 【返回值】
     * @return 待审批预约列表，包含访客信息、预约时间、访问事由等
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/pending")
    public ResultDTO<List<Appointment>> pendingList(@RequestAttribute("userId") Integer approverId) {
        return ResultDTO.success(wxApproverAppointmentService.listPendingAppointments(approverId));
    }

    /**
     * 获取预约详情（审批人视角）
     *
     * 【接口说明】
     * 查询审批人可查看的预约详情。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param approverId 审批人用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 预约详情，包含访客信息、预约时间、访问事由、审批历史等
     *
     * 【异常情况】
     * - 预约不存在/无权限查看：返回对应错误信息
     */
    @GetMapping("/detail")
    public ResultDTO<Appointment> getDetail(@RequestParam Long appointmentId,
                                            @RequestAttribute("userId") Integer approverId) {
        return ResultDTO.success(wxApproverAppointmentService.getApproverAppointmentDetail(appointmentId, approverId));
    }

    /**
     * 审批通过
     *
     * 【接口说明】
     * 审批人通过预约申请，更新预约状态并记录审批人。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param approverId 审批人用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 成功（无返回数据）
     *
     * 【异常情况】
     * - 预约不存在/状态异常/无权限操作：返回对应错误信息
     */
    @PostMapping("/approve")
    public ResultDTO<Void> approve(@RequestParam Long appointmentId,
                                   @RequestAttribute("userId") Integer approverId) {
        wxApproverAppointmentService.approveAppointment(appointmentId, approverId);
        return ResultDTO.success(null);
    }

    /**
     * 审批拒绝
     *
     * 【接口说明】
     * 审批人拒绝预约申请并记录拒绝理由。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param reason 拒绝理由（必填）
     * @param approverId 审批人用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 成功（无返回数据）
     *
     * 【异常情况】
     * - 预约不存在/状态异常/无权限操作/拒绝理由为空：返回对应错误信息
     */
    @PostMapping("/reject")
    public ResultDTO<Void> reject(@RequestParam Long appointmentId,
                                  @RequestParam String reason,
                                  @RequestAttribute("userId") Integer approverId) {
        wxApproverAppointmentService.rejectAppointment(appointmentId, approverId, reason);
        return ResultDTO.success(null);
    }

    /**
     * 查询审批历史
     *
     * 【接口说明】
     * 查询当前审批人已处理的预约记录。
     *
     * 【请求参数】
     * @param approverId 审批人用户ID（从JWT中获取）
     * @param keyword 关键词（可选，搜索访客姓名、身份证号、访问事由）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @param status 状态筛选（可选：1-已通过，2-已拒绝）
     * @param searchType 搜索类型（可选：create-按创建时间排序，process-按处理时间排序，默认process）
     *
     * 【返回值】
     * @return 历史预约列表，包含访客信息、预约时间、审批时间、审批意见等
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/history")
    public ResultDTO<List<Appointment>> historyList(@RequestAttribute("userId") Integer approverId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String searchType) {
        return ResultDTO.success(wxApproverAppointmentService.listApprovedHistory(approverId, keyword, startDate, endDate, status, searchType));
    }
}