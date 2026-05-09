package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.service.admin.AdminExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 【业务模块】管理后台数据导出接口
 *
 * 【核心职责】
 * 1. 导出预约审批记录
 * 2. 导出通行记录
 * 3. 统一响应文件下载
 *
 * 【关键业务场景】
 * 导出为.xlsx文件，浏览器直接触发下载用于留档。
 *
 * 【依赖说明】
 * - AdminExportService：导出数据查询与Excel生成
 *
 * 【注意事项】
 * - 导出文件名固定，前端无需二次处理
 * - 身份证号在Service层解密后导出
 */
@RestController
@RequestMapping("/api/admin/export")
public class AdminExportController {

    @Autowired
    private AdminExportService adminExportService;

    /**
     * 导出预约记录
     *
     * 【接口说明】
     * 导出预约审批记录为Excel文件并写入HTTP响应。
     *
     * 【请求参数】
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param keyword 关键词（访客姓名/审批人姓名，可选）
     * @param status 状态筛选（可选）
     * @param searchType 搜索类型（create-按创建时间，process-按处理时间，默认process）
     * @param response HTTP响应，用于输出Excel文件
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * - 导出失败：返回文件输出异常信息
     */
    @GetMapping("/appointments")
    public void exportAppointments(@RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String searchType,
                                   HttpServletResponse response) throws IOException {
        adminExportService.exportAppointments(startDate, endDate, keyword, status, searchType, response);
    }

    /**
     * 导出通行记录
     *
     * 【接口说明】
     * 导出通行记录为Excel文件并写入HTTP响应。
     *
     * 【请求参数】
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param keyword 关键词（访客姓名/手机号，可选）
     * @param accessType 通行类型（1-签到 2-签离，可选）
     * @param verifyMethod 核验方式（1-扫码 2-手动，可选）
     * @param emergencyOnly 是否仅显示紧急登记（可选）
     * @param response HTTP响应，用于输出Excel文件
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * - 导出失败：返回文件输出异常信息
     */
    @GetMapping("/access-records")
    public void exportAccessRecords(@RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer accessType,
                                    @RequestParam(required = false) Integer verifyMethod,
                                    @RequestParam(required = false) Boolean emergencyOnly,
                                    HttpServletResponse response) throws IOException {
        adminExportService.exportAccessRecords(startDate, endDate, keyword, accessType, verifyMethod, emergencyOnly, response);
    }
}
