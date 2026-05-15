package com.gpj.visitorsystem.task;

import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.service.common.UserService;
import com.gpj.visitorsystem.util.AppointmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 【定时任务】预约相关定时任务
 *
 * 【核心职责】
 * 1. 每分钟扫描过期预约，自动标记爽约并处罚
 * 2. 自动解封封禁已过期的用户
 */
@Component
public class AppointmentTask {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentTask.class);

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserService userService;

    /**
     * 【定时任务】每分钟扫描过期预约，自动处罚
     *
     * 【业务逻辑】
     * 1. 查询所有 status=1（预约成功）的预约
     * 2. 筛选出已超过宽限期的（expected_end_time + 30分钟 < now）
     * 3. 统一标记为 status=6（已过期）
     * 4. 筛选无签到记录的，逐个执行处罚
     *
     * 【执行频率】每分钟执行一次，最大延迟约1分钟
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public int processNoShowAppointments() {
        logger.debug("开始执行爽约判定任务...");

        List<Appointment> successAppointments = appointmentMapper.listByStatus(1);
        LocalDateTime now = LocalDateTime.now();

        // 第一步：标记已过期，筛选真正爽约的
        List<Appointment> noShowCandidates = new ArrayList<>();
        for (Appointment appointment : successAppointments) {
            if (now.isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
                appointmentMapper.updateStatus(appointment.getAppointmentId(), 6);
                appointment.setStatus(6);

                int checkInCount = appointmentMapper.countAccessLogByAppointmentAndType(
                        appointment.getAppointmentId(), 1);
                if (checkInCount == 0) {
                    noShowCandidates.add(appointment);
                } else {
                    logger.debug("[爽约判定] 预约 {} 已有签到记录，标记过期但不处罚",
                            appointment.getAppointmentId());
                }
            }
        }

        if (noShowCandidates.isEmpty()) {
            logger.debug("爽约判定任务完成，无需要处罚的爽约记录");
            return 0;
        }

        // 第二步：按时间正序排序，确保处罚回算正确
        noShowCandidates.sort(Comparator.comparing(Appointment::getExpectedEndTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // 第三步：逐个执行处罚
        int processedCount = 0;
        for (Appointment appointment : noShowCandidates) {
            try {
                userService.processNoShow(appointment);
                processedCount++;
            } catch (Exception e) {
                logger.error("[爽约判定] 预约 {} 处罚失败：{}",
                        appointment.getAppointmentId(), e.getMessage(), e);
            }
        }

        logger.info("爽约判定任务完成，共处罚 {} 条爽约记录", processedCount);
        return processedCount;
    }

    /**
     * 【定时任务】自动解封封禁已过期的用户
     *
     * 【执行频率】每分钟执行一次，确保封禁到期后立即解封
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public int autoUnbanExpiredUsers() {
        logger.debug("开始执行自动解封任务...");
        LocalDateTime now = LocalDateTime.now();

        List<User> expiredUsers = userService.findExpiredBans(now);
        if (expiredUsers == null || expiredUsers.isEmpty()) {
            logger.debug("自动解封任务完成，无需要解封的用户");
            return 0;
        }

        int unbanCount = 0;
        for (User user : expiredUsers) {
            logger.info("[自动解封] 用户 {} 封禁已过期({})，执行解封",
                    user.getUserId(), user.getBannedUntil());
            user.setMissedCount(0);
            user.setBannedUntil(null);
            int rows = userService.update(user);
            if (rows > 0) {
                unbanCount++;
                logger.info("[自动解封] 用户 {} 解封成功", user.getUserId());
            } else {
                logger.error("[自动解封] 用户 {} 解封失败，数据库未更新", user.getUserId());
            }
        }

        logger.info("自动解封任务完成，共解封 {} 位用户", unbanCount);
        return unbanCount;
    }
}
