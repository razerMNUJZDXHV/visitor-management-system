package com.gpj.visitorsystem.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 【工具类】Excel导出工具
 *
 * 【核心职责】
 * 1. 创建Excel表头样式
 * 2. 创建Excel数据样式
 * 3. 创建Excel单元格（支持null、数字、字符串）
 * 4. 设置HTTP响应头（用于文件下载）
 * 5. 自动调整列宽
 *
 * 【关键业务场景】
 * 管理端导出预约审批记录和通行记录为Excel文件。
 *
 * 【依赖说明】
 * - Apache POI：Excel文件创建与样式
 * - HttpServletResponse：HTTP响应输出
 *
 * 【注意事项】
 * - 所有方法均为静态方法，可直接通过类名调用
 * - 单元格值为null时显示为"—"
 * - 列宽自动调整，最小宽度3000
 */
public class ExcelExportUtil {

    /**
     * 创建Excel表头样式
     *
     * 【业务逻辑】
     * 1. 设置字体为粗体、11号
     * 2. 设置背景色为25%灰色
     * 3. 设置细边框和居中对齐
     *
     * 【参数说明】
     * @param workbook Excel工作簿对象
     *
     * 【返回值】
     * @return 表头样式
     */
    public static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * 创建Excel数据样式
     *
     * 【业务逻辑】
     * 1. 设置细边框
     * 2. 不设置字体和对齐方式（使用默认）
     *
     * 【参数说明】
     * @param workbook Excel工作簿对象
     *
     * 【返回值】
     * @return 数据样式
     */
    public static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建Excel单元格
     *
     * 【业务逻辑】
     * 1. 创建单元格并设置样式
     * 2. 根据值类型设置单元格值（null→"—"、Number→数字、其他→字符串）
     *
     * 【参数说明】
     * @param row 行对象
     * @param col 列索引
     * @param style 单元格样式
     * @param value 单元格值（可为null）
     */
    public static void createCell(Row row, int col, CellStyle style, Object value) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setCellValue("—");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    /**
     * 设置HTTP响应头（用于Excel文件下载）
     *
     * 【业务逻辑】
     * 1. 设置Content-Type为Excel文件类型
     * 2. 设置字符编码为UTF-8
     * 3. 设置Content-Disposition为附件下载，文件名URL编码
     *
     * 【参数说明】
     * @param response HTTP响应对象
     * @param filename 文件名（如"预约审批记录.xlsx"）
     *
     * 【异常情况】
     * @throws IOException URL编码失败
     */
    public static void setResponseHeaders(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    /**
     * 自动调整列宽
     *
     * 【业务逻辑】
     * 1. 遍历所有列，调用autoSizeColumn自动调整
     * 2. 如果调整后宽度小于2500，设置为3000
     *
     * 【参数说明】
     * @param sheet Excel工作表对象
     * @param columnCount 列数
     */
    public static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 2500) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }

    /**
     * 写入Excel文件到HTTP响应输出流
     *
     * 【业务逻辑】
     * 1. 设置响应头
     * 2. 获取响应输出流
     * 3. 将Workbook写入输出流
     * 4. 关闭Workbook
     *
     * 【参数说明】
     * @param workbook Excel工作簿对象
     * @param response HTTP响应对象
     * @param filename 文件名（如"预约审批记录.xlsx"）
     *
     * 【异常情况】
     * @throws IOException 写入失败
     */
    public static void writeWorkbook(Workbook workbook, HttpServletResponse response, String filename) throws IOException {
        setResponseHeaders(response, filename);
        try (OutputStream out = response.getOutputStream()) {
            workbook.write(out);
        }
        workbook.close();
    }
}
