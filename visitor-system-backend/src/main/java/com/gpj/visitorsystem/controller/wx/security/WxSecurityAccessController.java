package com.gpj.visitorsystem.controller.wx.security;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.wx.security.*;
import com.gpj.visitorsystem.service.wx.security.WxSecurityAccessService;
import com.gpj.visitorsystem.util.ParamParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【业务模块】安保端通行接口
 *
 * 【核心职责】
 * 1. 扫码核验与确认签到/签离
 * 2. 手动登记与手动签离
 * 3. 通行记录查询与统计
 * 4. 告警拉取与展示
 *
 * 【关键业务场景】
 * 扫码核验仅做预检，真正写库在确认接口；紧急登记无需预约但需完整信息。
 *
 * 【依赖说明】
 * - WxSecurityAccessService：通行业务处理与告警
 *
 * 【注意事项】
 * - 所有接口都需要安保人员Token校验（通过WxSecurityAuthInterceptor拦截器）
 * - 二维码Token有效期为2小时，超时需手动处理
 * - 告警通过WebSocket实时推送
 */
@RestController
@RequestMapping("/api/wx/security/access")
public class WxSecurityAccessController {

    @Autowired
    private WxSecurityAccessService wxSecurityAccessService;

    /**
     * 扫码核验预约二维码
     *
     * 【接口说明】
     * 安保人员扫码后解析JWT Token并返回预约信息用于确认。
     *
     * 【请求参数】
     * @param request 扫码核验请求DTO，包含qrToken（二维码中的JWT Token）
     * @param userId 安保人员用户ID（从JWT中获取，通过@RequestAttribute注入）
     *
     * 【返回值】
     * @return 核验响应DTO，包含预约详情、访客信息、二维码状态等
     *
     * 【异常情况】
     * - Token无效/预约不存在/状态异常：返回对应错误信息
     */
    @PostMapping("/scan-verify")
    public ResultDTO<SecurityScanVerifyResponseDTO> scanVerify(@RequestBody SecurityScanVerifyRequestDTO request,
                                                               @RequestAttribute("userId") Integer userId) {
        return ResultDTO.success(wxSecurityAccessService.scanVerify(request.getQrToken(), userId));
    }

    /**
     * 确认扫码操作（签到/签离）
     *
     * 【接口说明】
     * 核验通过后执行签到或签离，写入通行记录并更新预约状态。
     *
     * 【请求参数】
     * @param request 扫码确认请求DTO，包含appointmentId（预约ID）
     * @param userId 安保人员用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 操作结果DTO，包含操作类型、操作时间、通行记录ID
     *
     * 【异常情况】
     * - 预约状态异常/重复操作/预约已过期：返回对应错误信息
     */
    @PostMapping("/scan-confirm")
    public ResultDTO<SecurityScanVerifyResponseDTO> scanConfirm(@RequestBody SecurityScanConfirmRequestDTO request,
                                                                @RequestAttribute("userId") Integer userId) {
        return ResultDTO.success(wxSecurityAccessService.confirmScanAction(request.getAppointmentId(), userId));
    }

    /**
     * 手动登记（关联预约/紧急登记）
     *
     * 【接口说明】
     * 访客无法出示二维码时，安保可手动登记通行记录。
     *
     * 【请求参数】
     * @param request 手动登记请求DTO，包含预约ID（可选）、访客信息、登记类型
     * @param userId 安保人员用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 通行记录DTO，包含访客信息、预约信息、登记时间、通行类型
     *
     * 【异常情况】
     * - 预约不存在/状态异常/访客信息不完整：返回对应错误信息
     */
    @PostMapping("/manual-register")
    public ResultDTO<SecurityAccessRecordDTO> manualRegister(@RequestBody SecurityManualRegisterRequestDTO request,
                                                             @RequestAttribute("userId") Integer userId) {
        return ResultDTO.success(wxSecurityAccessService.manualRegister(request, userId));
    }

    /**
     * 手动签离（处理过期预约）
     *
     * 【接口说明】
     * 对已签到但未签离的预约执行手动签离。
     *
     * 【请求参数】
     * @param request 手动签离请求DTO，包含appointmentId（预约ID）
     * @param userId 安保人员用户ID（从JWT中获取）
     *
     * 【返回值】
     * @return 通行记录DTO，包含签离时间、停留时长等信息
     *
     * 【异常情况】
     * - 预约状态异常/预约不存在：返回对应错误信息
     */
    @PostMapping("/manual-signout")
    public ResultDTO<SecurityAccessRecordDTO> manualSignOut(@RequestBody SecurityManualSignOutRequestDTO request,
                                                            @RequestAttribute("userId") Integer userId) {
        return ResultDTO.success(wxSecurityAccessService.manualSignOutExpired(request.getAppointmentId(), userId));
    }

    /**
     * 查询通行记录列表
     *
     * 【接口说明】
     * 支持多条件筛选通行记录，并兼容前端字符串参数。
     *
     * 【请求参数】
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @param keyword 关键词（可选，搜索访客姓名、身份证号、手机号）
     * @param accessType 通行类型（可选：1-入场，2-离场，也接受字符串）
     * @param accessTypeStr 通行类型字符串（可选，兼容前端）
     * @param verifyMethod 核验方式（可选：1-扫码，2-手动，也接受字符串）
     * @param verifyMethodStr 核验方式字符串（可选，兼容前端）
     * @param emergencyOnly 是否仅紧急登记（可选，也接受字符串）
     * @param emergencyOnlyStr 是否仅紧急登记字符串（可选，兼容前端）
     *
     * 【返回值】
     * @return 通行记录DTO列表，包含访客信息、预约信息、通行时间、核验方式等
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/records")
    public ResultDTO<List<SecurityAccessRecordDTO>> records(@RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Integer accessType,
                                                             @RequestParam(required = false) String accessTypeStr,
                                                             @RequestParam(required = false) Integer verifyMethod,
                                                             @RequestParam(required = false) String verifyMethodStr,
                                                             @RequestParam(required = false) Boolean emergencyOnly,
                                                             @RequestParam(required = false) String emergencyOnlyStr) {
        // 处理字符串参数（兼容前端）
        Integer finalAccessType = accessType;
        if (finalAccessType == null) {
            finalAccessType = ParamParseUtil.parseIntegerParam(accessTypeStr);
        }
        
        Integer finalVerifyMethod = verifyMethod;
        if (finalVerifyMethod == null) {
            finalVerifyMethod = ParamParseUtil.parseIntegerParam(verifyMethodStr);
        }
        
        Boolean finalEmergencyOnly = emergencyOnly;
        if (finalEmergencyOnly == null) {
            finalEmergencyOnly = ParamParseUtil.parseBooleanParam(emergencyOnlyStr);
        }
        
        return ResultDTO.success(wxSecurityAccessService.listRecords(startDate, endDate, keyword, finalAccessType, finalVerifyMethod, finalEmergencyOnly));
    }

    /**
     * 【功能】获取通行记录详情
     *
     * 【接口地址】GET /api/wx/security/access/record-detail
     *
     * 【业务背景】
     * 安保人员点击某条通行记录，查看详细信息。
     *
     * 【实现逻辑】
     * 1. 接收通行记录ID
     * 2. 查询通行记录详情
     * 3. 返回详细信息
     *
     * 【参数说明】
     * @param logId 通行记录ID
     *
     * 【返回值】
     * @return 通行记录详情DTO，包含访客信息、预约信息、通行时间、核验方式、安保人员信息等
     *
     * 【异常情况】
     * @throws Exception 通行记录不存在
     *
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 返回信息包含敏感数据（如身份证号），需脱敏处理
     */
    @GetMapping("/record-detail")
    public ResultDTO<SecurityAccessRecordDTO> recordDetail(@RequestParam Long logId) {
        return ResultDTO.success(wxSecurityAccessService.getRecordDetail(logId));
    }

    /**
     * 【功能】获取通行统计
     *
     * 【接口地址】GET /api/wx/security/access/stats
     *
     * 【业务背景】
     * 安保人员查看通行统计信息，包括今日/本周/本月的通行人次、扫码次数、手动登记次数等。
     *
     * 【实现逻辑】
     * 1. 接收统计周期参数（day/week/month）
     * 2. 调用Service层查询统计数据
     * 3. 返回统计信息
     *
     * 【参数说明】
     * @param period 统计周期（可选：day-今日，week-本周，month-本月，默认day）
     *
     * 【返回值】
     * @return 统计DTO，包含总通行人次、扫码通行次数、手动登记次数、紧急登记次数、平均停留时长等
     *
     * 【注意事项】
     * - 统计数据实时查询数据库，不建议频繁调用
     * - 可考虑添加缓存优化性能
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     */
    @GetMapping("/stats")
    public ResultDTO<SecurityStatsDTO> stats(@RequestParam(required = false) String period) {
        return ResultDTO.success(wxSecurityAccessService.stats(period));
    }

    /**
     * 【功能】拉取告警列表
     *
     * 【接口地址】GET /api/wx/security/access/alerts
     *
     * 【业务背景】
     * 安保人员查看告警信息，包括待审核超时、签离超时、频繁访问等。
     * 告警信息通过WebSocket实时推送到前端，此接口用于主动拉取。
     *
     * 【实现逻辑】
     * 1. 调用Service层查询告警列表
     * 2. 返回告警信息
     *
     * 【参数说明】
     * - 无（用户ID和类型已在拦截器层校验）
     *
     * 【返回值】
     * @return 告警DTO列表，包含告警类型、告警内容、告警时间、处理状态等
     *
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 告警信息会实时推送到前端（通过WebSocket）
     * - 此接口用于前端主动拉取或WebSocket断开时的补偿
     * - 告警类型包括：待审核超时、签离超时、频繁访问、黑名单访客等
     */
    @GetMapping("/alerts")
    public ResultDTO<List<SecurityAlertDTO>> alerts() {
        return ResultDTO.success(wxSecurityAccessService.pollAlerts());
    }
}