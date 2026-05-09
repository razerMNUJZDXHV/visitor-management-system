package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.wx.security.SecurityAccessRecordDTO;
import com.gpj.visitorsystem.service.wx.security.WxSecurityAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 【业务模块】管理后台通行记录
 *
 * 【核心职责】
 * 1. 查询通行记录列表
 * 2. 查询通行记录详情
 * 3. 删除通行记录（单条/批量）
 *
 * 【关键业务场景】
 * 通行记录涉及身份证号展示，需在Service层统一解密后返回。
 *
 * 【依赖说明】
 * - WxSecurityAccessService：通行记录查询与删除
 *
 * 【注意事项】
 * - 删除仅允许关联预约状态为已完成的记录
 * - 支持多条件筛选查询
 */
@RestController
@RequestMapping("/api/admin/access")
public class AdminAccessController {

    @Autowired
    private WxSecurityAccessService wxSecurityAccessService;

    /**
     * 查询通行记录列表
     *
     * 【接口说明】
     * 支持多条件筛选通行记录，返回列表数据。
     *
     * 【请求参数】
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param keyword 关键词：访客姓名/手机号/安保姓名/授权审批人姓名（可选）
     * @param accessType 通行类型：1-签到，2-签离（可选）
     * @param verifyMethod 核验方式：1-扫码，2-手动（可选）
     * @param emergencyOnly 是否仅查询紧急登记（可选）
     *
     * 【返回值】
     * @return 通行记录DTO列表
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/records")
    public ResultDTO<List<SecurityAccessRecordDTO>> listRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer accessType,
            @RequestParam(required = false) Integer verifyMethod,
            @RequestParam(required = false) Boolean emergencyOnly) {
        List<SecurityAccessRecordDTO> records = wxSecurityAccessService.listRecordsForAdmin(
                startDate, endDate, keyword, accessType, verifyMethod, emergencyOnly);
        return ResultDTO.success(records);
    }

    /**
     * 查询通行记录详情
     *
     * 【接口说明】
     * 根据通行记录ID查询详情。
     *
     * 【请求参数】
     * @param logId 通行记录ID
     *
     * 【返回值】
     * @return 通行记录详情DTO，身份证号已解密
     *
     * 【异常情况】
     * - 记录不存在：返回“记录不存在”
     */
    @GetMapping("/record-detail")
    public ResultDTO<SecurityAccessRecordDTO> getRecordDetail(
            @RequestParam Long logId) {
        SecurityAccessRecordDTO detail = wxSecurityAccessService.getRecordDetailForAdmin(logId);
        return ResultDTO.success(detail);
    }

    /**
     * 删除通行记录
     *
     * 【接口说明】
     * 仅允许删除关联预约状态为已完成的通行记录。
     *
     * 【请求参数】
     * @param logId 通行记录ID
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 记录不存在/状态不允许删除：返回对应错误信息
     */
    @DeleteMapping("/delete")
    public ResultDTO<Void> delete(@RequestParam Long logId) {
        wxSecurityAccessService.deleteAccessRecord(logId);
        return ResultDTO.success(null);
    }

    /**
     * 批量删除通行记录
     *
     * 【接口说明】
     * 批量删除通行记录，需全部满足可删除条件。
     *
     * 【请求参数】
     * @param request 请求体，包含logIds（通行记录ID列表）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 记录不存在/存在不可删除记录：返回对应错误信息
     */
    @DeleteMapping("/batch-delete")
    public ResultDTO<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> logIds = request.get("logIds");
        wxSecurityAccessService.batchDeleteAccessRecords(logIds);
        return ResultDTO.success(null);
    }
}
