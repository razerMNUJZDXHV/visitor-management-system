package com.gpj.visitorsystem.service.admin;

import com.gpj.visitorsystem.dto.admin.AdminAppointmentDetailDTO;
import com.gpj.visitorsystem.dto.wx.security.SecurityAccessRecordDTO;
import com.gpj.visitorsystem.mapper.AccessLogMapper;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.util.AppointmentUtil;
import com.gpj.visitorsystem.util.AesEncryptUtil;
import com.gpj.visitorsystem.util.ExcelExportUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 【业务模块】管理后台数据导出
 *
 * 【核心职责】
 * 1. 导出预约审批记录为Excel
 * 2. 导出通行记录为Excel
 * 3. 导出前解密身份证号并格式化时间
 *
 * 【关键业务场景】
 * 管理端需要离线留存审批与通行记录，导出时需确保
 * 身份证号已解密、时间字段格式统一且可读。
 *
 * 【依赖说明】
 * - AppointmentMapper：预约审批记录查询
 * - AccessLogMapper：通行记录查询
 * - AesEncryptUtil：身份证号解密
 * - Apache POI：Excel文件创建与样式
 *
 * 【注意事项】
 * - 导出文件通过HTTP响应输出，需设置正确的响应头
 * - 身份证号解密失败显示为"—"，不阻断导出流程
 */
@Service
public class AdminExportService {

    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private AccessLogMapper accessLogMapper;
    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    /**
     * 导出预约审批记录
     *
     * 【业务背景】
     * 管理端导出预约审批记录为Excel文件，便于留档。
     *
     * 【实现步骤】
     * 1. 查询符合条件的审批记录
     * 2. 解密身份证号并格式化时间
     * 3. 构建Excel表头和数据行
     * 4. 写入HTTP响应输出流
     *
     * 【参数说明】
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param keyword 关键词（可选）
     * @param status 状态筛选（可选）
     * @param searchType 搜索类型（create/process）
     * @param response HTTP响应对象
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws IOException 响应输出失败
     *
     * 【事务说明】
     * 无（只读查询与输出）
     */
    public void exportAppointments(String startDate, String endDate,
                                    String keyword, Integer status, String searchType,
                                    HttpServletResponse response) throws IOException {
        List<AdminAppointmentDetailDTO> list = appointmentMapper.findAdminHistory(
                "all", null, keyword, startDate, endDate, status,
                searchType != null && !searchType.isEmpty() ? searchType : "create");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("预约审批记录");

        CellStyle headerStyle = ExcelExportUtil.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelExportUtil.createDataStyle(workbook);

        String[] headers = {
                "预约编号", "访客ID", "访客姓名", "访客手机号", "身份证号",
                "被访人", "来访事由", "预计到达时间", "预计离开时间", "状态",
                "审批人ID", "审批人姓名", "审批人手机号", "拒绝理由",
                "申请时间", "处理时间"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (AdminAppointmentDetailDTO item : list) {
            String idCard = AppointmentUtil.decryptIdCard(item.getVisitorIdCard(), aesEncryptUtil);
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getAppointmentId());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorId());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorPhone());
            ExcelExportUtil.createCell(row, col++, dataStyle, idCard);
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getIntervieweeName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitReason());
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getExpectedStartTime()));
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getExpectedEndTime()));
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.getStatusText(item.getStatus()));
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getApproverId());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getApproverName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getApproverPhone());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getRejectReason());
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getCreateTime()));
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getProcessTime()));
        }

        ExcelExportUtil.autoSizeColumns(sheet, headers.length);
        ExcelExportUtil.writeWorkbook(workbook, response, "预约审批记录.xlsx");
    }

    /**
     * 导出通行记录
     *
     * 【业务背景】
     * 管理端导出通行记录为Excel文件，便于审计与留档。
     *
     * 【实现步骤】
     * 1. 查询符合条件的通行记录
     * 2. 合并记录所需展示字段
     * 3. 构建Excel表头与数据行
     * 4. 写入HTTP响应输出流
     *
     * 【参数说明】
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param keyword 关键词（可选）
     * @param accessType 通行类型（可选）
     * @param verifyMethod 核验方式（可选）
     * @param emergencyOnly 是否仅紧急登记（可选）
     * @param response HTTP响应对象
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws IOException 响应输出失败
     *
     * 【事务说明】
     * 无（只读查询与输出）
     */
    public void exportAccessRecords(String startDate, String endDate,
                                     String keyword, Integer accessType,
                                     Integer verifyMethod, Boolean emergencyOnly,
                                     HttpServletResponse response) throws IOException {
        List<SecurityAccessRecordDTO> list = accessLogMapper.listRecords(
                startDate, endDate, keyword, accessType, verifyMethod, emergencyOnly);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("通行记录");

        CellStyle headerStyle = ExcelExportUtil.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelExportUtil.createDataStyle(workbook);

        String[] headers = {
                "记录编号", "访客姓名", "访客手机号", "身份证号",
                "安保姓名", "安保手机号", "核验方式", "通行类型",
                "紧急通行", "授权审批人", "审批人手机号",
                "来访事由", "预计到达时间", "预计离开时间", "通行时间"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SecurityAccessRecordDTO item : list) {
            AppointmentUtil.mergeRecordData(item, aesEncryptUtil);
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getLogId());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorPhone());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitorIdCard());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getSecurityName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getSecurityPhone());
            ExcelExportUtil.createCell(row, col++, dataStyle, getVerifyMethodText(item.getVerifyMethod()));
            ExcelExportUtil.createCell(row, col++, dataStyle, getAccessTypeText(item.getAccessType()));
            ExcelExportUtil.createCell(row, col++, dataStyle, Boolean.TRUE.equals(item.getEmergency()) ? "是" : "否");
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getAuthorizerName());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getAuthorizerPhone());
            ExcelExportUtil.createCell(row, col++, dataStyle, item.getVisitReason());
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getExpectedStartTime()));
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getExpectedEndTime()));
            ExcelExportUtil.createCell(row, col++, dataStyle, AppointmentUtil.formatDateTime(item.getAccessTime()));
        }

        ExcelExportUtil.autoSizeColumns(sheet, headers.length);
        ExcelExportUtil.writeWorkbook(workbook, response, "通行记录.xlsx");
    }

    private String getVerifyMethodText(Integer verifyMethod) {
        if (verifyMethod == null) {
            return "—";
        }
        return verifyMethod == 1 ? "扫码" : "手动";
    }

    private String getAccessTypeText(Integer accessType) {
        if (accessType == null) {
            return "—";
        }
        return accessType == 1 ? "签到" : "签离";
    }
}
