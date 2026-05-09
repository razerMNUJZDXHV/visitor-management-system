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
import java.util.List;

/**
 * 【定时任务】预约相关定时任务
 *
 * 【核心职责】
 * 1. 定时扫描过期预约，自动标记爽约
 * 2. 处理爽约处罚逻辑（连续爽约2次封禁3个月）
 *
 * 【关键业务场景】
 * 1. 每10分钟执行一次，扫描状态为"预约成功(1)"且超过宽限期的预约
 * 2. 自动判定爽约，更新用户爽约次数和封禁状态
 * 3. 将过期预约状态更新为"已过期(6)"
 *
 * 【注意事项】
 * - 使用@Scheduled定时执行，cron表达式：0 0/10 * * * ?（每10分钟）
 * - 使用@Transactional保证数据一致性
 * - 避免重复处罚：检查是否存在更晚的爽约处理记录
 */
@Component
public class AppointmentTask {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentTask.class);

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserService userService;

    /**
     * 【定时任务】处理爽约预约
     *
     * 【业务逻辑】
     * 1. 查询所有状态为"预约成功(1)"且超过宽限期的预约
     * 2. 遍历这些预约，逐个判定爽约并处罚
     * 3. 更新用户爽约次数，连续爽约2次则封禁3个月
     * 4. 将预约状态更新为"已过期(6)"
     *
     * 【执行频率】
     * 每10分钟执行一次
     *
     * 【注意事项】
     * - 使用@Transactional保证原子性
     * - 避免重复处罚：检查是否存在更晚的爽约处理记录
     */
    // 懒加载模式：用户查询列表时已自动处理爽约，定时任务仅做兜底
    // 每6小时执行一次，防止用户长期不登录导致数据未更新
    @Scheduled(cron = "0 0 0/6 * * ?")  // 每天0点、6点、12点、18点执行
    @Transactional
    public int processNoShowAppointments() {
        logger.info("开始执行爽约判定任务...");

        // 1. 查询所有状态为"预约成功(1)"的预约
        List<Appointment> successAppointments = appointmentMapper.listByStatus(1);

        int processedCount = 0;
        for (Appointment appointment : successAppointments) {
            // 2. 判断是否超过宽限过期时间
            if (LocalDateTime.now().isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
                processNoShow(appointment);
                processedCount++;
            }
        }

        logger.info("爽约判定任务执行完成，共处理 {} 条记录", processedCount);
        return processedCount;
    }

    /**
     * 【内部方法】处理单个爽约预约
     *
     * 【业务逻辑】
     * 1. 检查是否存在更晚的爽约处理记录（避免重复处罚）
     * 2. 查找上一次预约记录
     * 3. 如果上一次也是爽约且在一个月内，连续爽约次数+1
     * 4. 否则，连续爽约次数重置为1
     * 5. 更新用户爽约次数和封禁截止时间
     * 6. 将预约状态更新为6（已过期）
     *
     * 【参数说明】
     * @param appointment 爽约的预约实体
     */
    private void processNoShow(Appointment appointment) {
        Long visitorId = appointment.getVisitorId();
        LocalDateTime currentEndTime = appointment.getExpectedEndTime();
        LocalDateTime now = LocalDateTime.now();

        if (visitorId == null || currentEndTime == null) {
            return;
        }

        User user = userService.findById(visitorId.intValue());
        if (user == null) {
            return;
        }

        // 检查是否存在更晚的爽约处理记录（避免重复处罚）
        boolean hasLaterProcessedNoShow = appointmentMapper.countLaterNoShowProcessed(visitorId, currentEndTime) > 0;
        if (!hasLaterProcessedNoShow) {
            // 查找上一次预约记录
            Appointment previous = appointmentMapper.findLatestRelevantBefore(visitorId, currentEndTime);

            // 判断上一次是否也是爽约且在一个月内
            boolean consecutiveNoShowWithinOneMonth = previous != null
                    && isNoShowStatus(previous.getStatus())
                    && previous.getExpectedEndTime() != null
                    && !previous.getExpectedEndTime().isBefore(currentEndTime.minusMonths(1));

            // 计算新的连续爽约次数
            int currentMissedCount = (user.getMissedCount() == null ? 0 : user.getMissedCount());
            int newMissedCount = consecutiveNoShowWithinOneMonth ? currentMissedCount + 1 : 1;
            user.setMissedCount(newMissedCount);

            // 连续爽约2次，禁止预约3个月
            if (newMissedCount >= 2) {
                user.setBannedUntil(now.plusMonths(3));
            }

            userService.update(user);
            logger.info("用户 {} 爽约次数更新为 {}，禁止截止时间：{}", user.getUserId(), newMissedCount, user.getBannedUntil());
        } else {
            logger.info("预约 {} 存在更晚爽约处理记录，跳过用户处罚回算", appointment.getAppointmentId());
        }

        // 更新预约状态为已过期（6）
        appointmentMapper.updateStatus(appointment.getAppointmentId(), 6);
        appointment.setStatus(6);
    }

    /**
     * 【内部方法】判断是否为爽约状态
     *
     * 【业务逻辑】
     * 预约成功(1)但没签到，超过宽限期后会变成已过期(6)，
     * 这两种状态都视为爽约，用于连续爽约的判定。
     *
     * 【参数说明】
     * @param status 预约状态
     *
     * 【返回值】
     * @return 是否为爽约状态
     */
    private boolean isNoShowStatus(Integer status) {
        return status != null && (status == 1 || status == 6);
    }
}
