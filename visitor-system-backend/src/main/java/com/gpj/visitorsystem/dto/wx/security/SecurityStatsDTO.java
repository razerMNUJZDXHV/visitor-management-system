package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

import java.util.List;

/**
 * 【业务模块】安保端统计DTO
 *
 * 【核心职责】
 * 1. 封装安保端首页展示的统计数据
 * 2. 提供流量趋势图数据点（flowPoints、hourPoints）
 * 3. 汇总总流量、高峰时段等关键指标
 *
 * 【关键业务场景】
 * 1. 安保端首页加载时获取统计数据展示
 * 2. flowPoints按日期展示流量变化趋势
 * 3. hourPoints按时段展示当日流量分布
 * 4. peakHour标识当日人流高峰时段
 *
 * 【依赖说明】
 * - 由WxSecurityAccessService统计组装
 * - 内嵌StatsPoint静态类：定义图表数据点格式
 *
 * 【注意事项】
 * - period标识统计周期（如"今日"、"本周"）
 * - flowPoints的label为日期或时段标签，value为对应流量值
 * - hourPoints的label为小时标签（如"09:00"），value为对应流量
 * - StatsPoint为内嵌静态类，供前端图表组件直接使用
 */
@Data
public class SecurityStatsDTO {
    private String period;              // 统计周期
    private long totalFlow;            // 总流量
    private String peakHour;           // 高峰时段
    private List<StatsPoint> flowPoints;  // 流量数据点
    private List<StatsPoint> hourPoints;  // 时段流量数据点

    /**
     * 统计数据点
     * label是时段标签（如"09:00"），value是流量值
     */
    @Data
    public static class StatsPoint {
        private String label;  // 时段标签
        private Long value;   // 流量值
    }
}