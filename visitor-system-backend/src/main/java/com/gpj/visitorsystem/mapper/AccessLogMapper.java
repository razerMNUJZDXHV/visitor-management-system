package com.gpj.visitorsystem.mapper;

import com.gpj.visitorsystem.dto.wx.security.SecurityAccessRecordDTO;
import com.gpj.visitorsystem.dto.wx.security.SecurityStatsDTO;
import com.gpj.visitorsystem.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【业务模块】通行记录表Mapper
 *
 * 【核心职责】
 * 1. 通行记录的增删改查（CRUD）
 * 2. 安保扫码/手动登记时写入记录
 * 3. 提供统计数据供安保端首页图表使用
 * 4. 支持管理端查询和导出通行记录
 * 5. 按时间范围、类型、关键词等条件筛选
 *
 * 【关键业务场景】
 * 1. 扫码签到/签离：insert插入通行记录（verifyMethod=1）
 * 2. 手动登记：insert插入通行记录（verifyMethod=2，带authorizerId）
 * 3. 安保端统计：countTodayFlow统计今日流量，countTodaySignIn统计签到人数
 * 4. 时段流量统计：listHourlyFlow按小时统计流量，用于画趋势图
 * 5. 管理端列表：listAccessLogsPage分页查询，countAccessLogs查总数
 * 6. 紧急通行统计：countEmergency统计紧急通行次数
 *
 * 【依赖说明】
 * - 对应数据库access_log表
 * - 被WxSecurityAccessService使用：写入通行记录
 * - 被AdminAccessService使用：管理端查询和统计
 * - 被AdminStatsService使用：仪表盘统计数据
 *
 * 【注意事项】
 * - accessType：1-签到 2-签离
 * - verifyMethod：1-扫码 2-手动登记
 * - 手动登记时authorizerId必填，扫码时为null
 * - 统计查询注意时间范围，避免全表扫描
 * - 管理端导出时需关联预约表获取访客信息
 * - 删除仅允许关联预约状态为已完成的记录
 */
@Mapper
public interface AccessLogMapper {

    /**
     * 插入通行记录
     * 签到/签离时调用
     */
    int insert(AccessLog accessLog);

    /**
     * 统计指定预约的指定类型通行记录数
     * 用来判断是否已经签到/签离，避免重复操作
     */
    int countByAppointmentAndType(@Param("appointmentId") Long appointmentId,
                                  @Param("accessType") Integer accessType);

    /**
     * 统计指定预约的签离记录数
     * 已签离的预约不能再签离
     */
    int countSignOutByAppointment(@Param("appointmentId") Long appointmentId);

    /**
     * 查指定预约的第一次通行时间
     * 用来计算实际停留时长
     */
    LocalDateTime findFirstAccessTimeByAppointmentAndType(@Param("appointmentId") Long appointmentId,
                                                          @Param("accessType") Integer accessType);

    // ==================== 查询通行记录 ====================

    /**
     * 查询通行记录（支持多条件筛选）
     * 安保端"通行记录"页面用这个
     * 
     * @param emergencyOnly 是否只查紧急预约的通行记录
     */
    List<SecurityAccessRecordDTO> listRecords(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate,
                                              @Param("keyword") String keyword,
                                              @Param("accessType") Integer accessType,
                                              @Param("verifyMethod") Integer verifyMethod,
                                              @Param("emergencyOnly") Boolean emergencyOnly);

    /**
     * 查通行记录详情
     */
    SecurityAccessRecordDTO findRecordDetail(@Param("logId") Long logId);

    /**
     * 删除通行记录（单条）
     */
    int deleteById(@Param("logId") Long logId);

    /**
     * 批量删除通行记录
     */
    int deleteByIds(@Param("logIds") List<Long> logIds);

    // ==================== 统计数据 ====================

    /**
     * 按日期统计流量（从start开始）
     * 返回每天的签到/签离人数，供图表展示
     */
    List<SecurityStatsDTO.StatsPoint> countFlowByDate(@Param("start") LocalDateTime start);

    /**
     * 按小时统计流量（今天）
     * 返回今天每个小时的流量，供今日趋势图展示
     */
    List<SecurityStatsDTO.StatsPoint> countFlowByHour(@Param("start") LocalDateTime start);

    /**
     * 按小时统计流量（指定时间范围）
     * 供自定义时间范围的图表展示
     */
    List<SecurityStatsDTO.StatsPoint> countFlowByHourRange(@Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);

    /**
     * 统计从指定时间开始的总流量
     */
    Long countTotalFrom(@Param("start") LocalDateTime start);

    // ==================== 今日统计 ====================

    /**
     * 统计今日总通行量（签到+签离）
     */
    long countTodayFlow();

    /**
     * 统计今日签到人数
     */
    long countTodaySignIn();

    /**
     * 统计今日签离人数
     */
    long countTodaySignOut();

    /**
     * 统计今日紧急预约通行量
     */
    long countTodayEmergency();

    /**
     * 统计历史总紧急预约通行量
     */
    long countTotalEmergency();

    // ==================== 每日流量统计 ====================

    /**
     * 统计最近days天的每日流量
     * 供安保端趋势图展示
     */
    List<SecurityStatsDTO.StatsPoint> countDailyFlow(@Param("days") int days);
}
