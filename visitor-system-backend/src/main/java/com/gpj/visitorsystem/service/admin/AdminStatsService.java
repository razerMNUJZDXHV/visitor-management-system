package com.gpj.visitorsystem.service.admin;

import com.gpj.visitorsystem.dto.admin.AdminStatsDTO;
import com.gpj.visitorsystem.dto.wx.security.SecurityStatsDTO;
import com.gpj.visitorsystem.mapper.AccessLogMapper;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【业务模块】管理后台统计
 *
 * 【核心职责】
 * 1. 汇总管理端仪表盘统计数据（今日流量、预约状态分布等）
 * 2. 生成按日/周/月的时段流量趋势
 * 3. 计算高峰时段与紧急通行占比
 *
 * 【关键业务场景】
 * 仪表盘需要完整24小时的时段流量，即便无数据也要补0，
 * 避免前端图表出现断点或缺失。
 *
 * 【依赖说明】
 * - AccessLogMapper：通行与告警相关统计
 * - AppointmentMapper：预约状态与数量统计
 *
 * 【注意事项】
 * - days参数用于控制近几天流量统计范围
 * - 时段流量统一补齐24小时，前端无需二次处理
 */
@Service
public class AdminStatsService {

    @Autowired
    private AccessLogMapper accessLogMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;

    /**
     * 获取统计数据
     *
     * 【业务背景】
     * 管理端仪表盘需要汇总今日数据、预约状态分布与时段流量趋势。
     *
     * 【实现步骤】
     * 1. 查询今日实时数据与预约状态统计
     * 2. 计算每日流量趋势
     * 3. 构建今日/本周/本月/全时段流量
     * 4. 计算高峰时段与紧急通行占比
     *
     * 【参数说明】
     * @param days 每日流量统计天数
     *
     * 【返回值】
     * @return 管理端统计DTO
     *
     * 【异常情况】
     * @throws Exception 数据查询失败时抛出
     *
     * 【事务说明】
     * 无（只读查询）
     */
    public AdminStatsDTO getStats(int days) {
        AdminStatsDTO dto = new AdminStatsDTO();

        // 今日实时数据
        dto.setTodayFlow(accessLogMapper.countTodayFlow());
        dto.setTodaySignIn(accessLogMapper.countTodaySignIn());
        dto.setTodaySignOut(accessLogMapper.countTodaySignOut());
        dto.setTodayEmergency(accessLogMapper.countTodayEmergency());
        dto.setTodayAppointments(appointmentMapper.countTodayAppointments());

        // 预约状态统计
        long pending = appointmentMapper.countByStatus(0);
        long approved = appointmentMapper.countByStatus(1);
        long rejected = appointmentMapper.countByStatus(2);
        long canceled = appointmentMapper.countByStatus(3);
        long checkedIn = appointmentMapper.countByStatus(4);
        long completed = appointmentMapper.countByStatus(5);
        long expired = appointmentMapper.countByStatus(6);

        dto.setTotalAppointments(pending + approved + rejected + canceled + checkedIn + completed + expired);
        dto.setPendingCount(pending);
        dto.setApprovedCount(approved);
        dto.setRejectedCount(rejected);
        dto.setCanceledCount(canceled);
        dto.setCheckedInCount(checkedIn);
        dto.setCompletedCount(completed);
        dto.setExpiredCount(expired);

        // 每日流量趋势
        dto.setDailyFlow(accessLogMapper.countDailyFlow(Math.max(days - 1, 0)));

        // 今日时段流量
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        dto.setHourlyFlow(buildHourlyFlow(accessLogMapper.countFlowByHourRange(todayStart, todayStart.plusDays(1))));

        // 本周时段流量
        LocalDate now = LocalDate.now();
        int daysToMonday = now.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        LocalDateTime weekStart = now.minusDays(daysToMonday).atStartOfDay();
        dto.setWeeklyHourlyFlow(buildHourlyFlow(accessLogMapper.countFlowByHourRange(weekStart, weekStart.plusDays(7))));

        // 本月时段流量
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        dto.setMonthlyHourlyFlow(buildHourlyFlow(accessLogMapper.countFlowByHourRange(monthStart, currentMonth.plusMonths(1).atDay(1).atStartOfDay())));

        // 全部时段流量
        dto.setAllHourlyFlow(buildHourlyFlow(accessLogMapper.countFlowByHour(LocalDateTime.of(1970, 1, 1, 0, 0))));

        // 计算高峰时段
        String peakHour = "—";
        long peakValue = -1;
        for (SecurityStatsDTO.StatsPoint point : dto.getHourlyFlow()) {
            if (point.getValue() > peakValue) {
                peakValue = point.getValue();
                peakHour = point.getLabel();
            }
        }
        dto.setPeakHour(peakValue > 0 ? peakHour : "—");

        // 紧急通行统计
        long totalEmergency = accessLogMapper.countTotalEmergency();
        long totalFlow = accessLogMapper.countTotalFrom(LocalDateTime.of(1970, 1, 1, 0, 0));
        dto.setTotalEmergency(totalEmergency);
        dto.setTotalNonEmergency(Math.max(0, totalFlow - totalEmergency));

        return dto;
    }

    /**
     * 构建24小时时段流量数据（补全缺失时段）
     */
    private List<SecurityStatsDTO.StatsPoint> buildHourlyFlow(List<SecurityStatsDTO.StatsPoint> rawHourly) {
        Map<String, Long> hourMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourMap.put(String.format("%02d:00", h), 0L);
        }
        for (SecurityStatsDTO.StatsPoint point : rawHourly) {
            if (point.getLabel() != null) {
                hourMap.put(point.getLabel(), point.getValue() != null ? point.getValue() : 0L);
            }
        }

        List<SecurityStatsDTO.StatsPoint> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : hourMap.entrySet()) {
            SecurityStatsDTO.StatsPoint point = new SecurityStatsDTO.StatsPoint();
            point.setLabel(entry.getKey());
            point.setValue(entry.getValue());
            result.add(point);
        }
        return result;
    }
}
