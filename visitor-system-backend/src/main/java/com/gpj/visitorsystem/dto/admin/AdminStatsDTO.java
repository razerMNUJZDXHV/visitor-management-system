package com.gpj.visitorsystem.dto.admin;

import com.gpj.visitorsystem.dto.wx.security.SecurityStatsDTO;
import lombok.Data;

import java.util.List;

/**
 * 【业务模块】管理端统计DTO
 *
 * 【核心职责】
 * 1. 封装管理端仪表盘的所有统计数据
 * 2. 提供今日流量、预约状态分布等关键指标
 * 3. 提供时段流量趋势图数据
 * 4. 支持多维度统计分析
 *
 * 【关键业务场景】
 * 1. 管理端首页加载时获取统计数据展示
 * 2. 今日统计：总通行量、签到/签离人数、紧急通行次数
 * 3. 预约状态分布：待审核、已通过、已拒绝等各状态数量
 * 4. 时段流量图：按日/周/月展示流量变化趋势
 * 5. 高峰时段分析：识别人流高峰时段
 *
 * 【依赖说明】
 * - 由AdminStatsService统计组装
 * - 内嵌SecurityStatsDTO.StatsPoint：定义图表数据点格式
 *
 * 【注意事项】
 * - 今日统计按自然日计算（00:00-23:59）
 * - 预约状态统计为累计值，非当日值
 * - dailyFlow为近30天数据，用于画月度趋势图
 * - hourlyFlow为今日24小时数据，用于画当日分布图
 * - peakHour格式如"09:00-10:00"，标识人流最密集时段
 */
@Data
public class AdminStatsDTO {
    // ==================== 今日统计 ====================
    private long todayFlow;           // 今日总通行量
    private long todaySignIn;         // 今日签到人数
    private long todaySignOut;        // 今日签离人数
    private long todayEmergency;      // 今日紧急通行次数
    private long todayAppointments;   // 今日新增预约

    // ==================== 预约状态统计 ====================
    private long totalAppointments;   // 预约总数
    private long pendingCount;        // 待审核
    private long approvedCount;       // 已通过
    private long rejectedCount;       // 已拒绝
    private long canceledCount;       // 已取消
    private long checkedInCount;      // 已签到
    private long completedCount;      // 已完成
    private long expiredCount;        // 已过期

    // ==================== 紧急/非紧急统计 ====================
    private long totalEmergency;      // 总紧急通行次数
    private long totalNonEmergency;   // 总非紧急通行次数

    // ==================== 异常监控 ====================
    private long todayNoShow;            // 今日爽约数（status=6且无签到记录），保留统计供后续扩展
    private long overtimeStayingCount;   // 当前超时滞留人数（已签到未签离且超过宽限期）
    private long bannedUserCount;        // 当前被封禁用户数（仪表盘当前主要展示此指标）

    // ==================== 峰值和图表数据 ====================
    private String peakHour;          // 高峰时段（如"09:00-10:00"）
    private List<SecurityStatsDTO.StatsPoint> dailyFlow;       // 每日流量（近30天）
    private List<SecurityStatsDTO.StatsPoint> hourlyFlow;      // 今日时段流量
    private List<SecurityStatsDTO.StatsPoint> weeklyHourlyFlow;  // 本周时段流量
    private List<SecurityStatsDTO.StatsPoint> monthlyHourlyFlow; // 本月时段流量
    private List<SecurityStatsDTO.StatsPoint> allHourlyFlow;    // 全部时段流量
}
