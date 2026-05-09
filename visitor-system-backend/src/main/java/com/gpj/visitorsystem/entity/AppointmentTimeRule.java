package com.gpj.visitorsystem.entity;

import lombok.Data;

/**
 * 【业务模块】预约时间规则
 *
 * 【核心职责】
 * 1. 定义允许预约的日期范围和时段
 * 2. 控制特定日期段内是否开放预约
 * 3. 供AppointmentSetting组合成完整的预约规则列表
 *
 * 【关键业务场景】
 * 1. 管理员设置"2026-05-01至2026-05-07，每天08:00-18:00开放预约"
 * 2. 访客提交预约时，系统校验预计到达时间是否在开放时段内
 * 3. 可设置某些日期段关闭预约（如考试周、假期）
 * 4. 支持多个规则组合，覆盖不同日期段的不同时段
 *
 * 【依赖说明】
 * - 被AppointmentSetting包含：作为rules列表的元素
 *
 * 【注意事项】
 * - startDate/endDate格式：yyyy-MM-dd
 * - startTime/endTime格式：HH:mm
 * - open为false时该日期段完全关闭预约
 * - 多个规则之间不能有时间重叠，需校验
 * - 访客预约时间必须在某条open=true的规则范围内才算合法
 */
@Data
public class AppointmentTimeRule {
    private String startDate;   // 规则生效开始日期（yyyy-MM-dd）
    private String endDate;     // 规则生效结束日期（yyyy-MM-dd）
    private String startTime;   // 每日开放开始时间（HH:mm）
    private String endTime;     // 每日开放结束时间（HH:mm）
    private Boolean open;       // 是否开放预约
}
