package com.gpj.visitorsystem.service.common;

import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.mapper.UserMapper;
import com.gpj.visitorsystem.util.AppointmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 【业务模块】用户通用服务
 *
 * 【核心职责】
 * 1. 提供用户基础查询与写入能力
 * 2. 封装Mapper，避免直接暴露持久层
 *
 * 【关键业务场景】
 * 用户查询时返回用户信息（不解封，解封由定时任务和显式方法处理）
 *
 * 【依赖说明】
 * - UserMapper：用户数据持久化
 *
 * 【注意事项】
 * - 解封逻辑不在findById中触发，避免副作用干扰其他业务
 * - 解封由AppointmentTask.autoUnbanExpiredUsers定时任务和显式checkAndUnban处理
 * - 管理端/小程序端均应通过本服务访问用户数据
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * 根据ID查询用户（只读，无副作用）
     *
     * 【业务逻辑】
     * 1. 查询用户信息
     * 2. 不触发自动解封（解封由独立方法处理）
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 用户信息（可能是封禁状态）
     */
    public User findById(Integer userId) {
        return userMapper.findById(userId);
    }

    /**
     * 检查并解封用户（如果封禁已过期）
     *
     * 【业务逻辑】
     * 1. 检查用户封禁状态
     * 2. 如果已过期，清除封禁与爽约次数，并更新数据库
     *
     * 【参数说明】
     * @param user 用户对象（会被修改）
     */
    public void checkAndUnban(User user) {
        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            if (user.getBannedUntil() != null && now.isAfter(user.getBannedUntil())) {
                user.setMissedCount(0);
                user.setBannedUntil(null);
                userMapper.update(user);
                logger.info("用户 {} 封禁已过期，执行解封", user.getUserId());
            }
        }
    }

    /**
     * 【实时处罚】检查并处理指定访客的所有已过期未处罚预约
     * 使用 REQUIRES_NEW 确保处罚独立于调用方事务提交，避免被回滚
     *
     * 【业务逻辑】
     * 1. 查询该访客所有 status=1（预约成功）且超过宽限期的预约
     * 2. 统一将这些预约标记为 status=6（已过期）
     * 3. 从已过期预约中筛选出无签到记录的（真正爽约）
     * 4. 按时间正序逐个调用 processNoShow 执行处罚
     *
     * 【触发时机】
     * - 访客发起新预约申请时（createAppointment）
     * - 定时任务兜底（processNoShowAppointments）
     *
     * 【参数说明】
     * @param visitorId 访客用户ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkNoShowAndPunishForUser(Integer visitorId) {
        List<Appointment> successAppointments = appointmentMapper.listByStatusAndVisitorId(1, visitorId);
        if (successAppointments == null || successAppointments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 第一步：统一将所有超时预约标记为已过期（status=6）
        List<Appointment> noShowCandidates = new ArrayList<>();
        for (Appointment appointment : successAppointments) {
            LocalDateTime graceExpireTime = AppointmentUtil.getGraceExpireTime(appointment);
            if (now.isAfter(graceExpireTime)) {
                // 标记为已过期
                appointmentMapper.updateStatus(appointment.getAppointmentId(), 6);
                appointment.setStatus(6);

                // 筛选出无签到记录的（真正爽约）
                int checkInCount = appointmentMapper.countAccessLogByAppointmentAndType(appointment.getAppointmentId(), 1);
                if (checkInCount == 0) {
                    noShowCandidates.add(appointment);
                } else {
                    logger.info("[实时处罚] 预约 {} 已有签到记录，标记过期但不处罚", appointment.getAppointmentId());
                }
            }
        }

        if (noShowCandidates.isEmpty()) {
            return;
        }

        // 第二步：按预计结束时间正序排序，确保先处理较早的预约，处罚回算才正确
        noShowCandidates.sort(Comparator.comparing(Appointment::getExpectedEndTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // 第三步：逐个执行处罚
        int punishedCount = 0;
        for (Appointment appointment : noShowCandidates) {
            try {
                processNoShow(appointment);
                punishedCount++;
            } catch (Exception e) {
                logger.error("[实时处罚] 预约 {} 处罚失败：{}", appointment.getAppointmentId(), e.getMessage(), e);
            }
        }

        if (punishedCount > 0) {
            logger.info("[实时处罚] 用户 {} 共处罚 {} 条爽约记录", visitorId, punishedCount);
        }
    }

    /**
     * 【核心处罚】对单个爽约预约执行处罚计算并更新用户表
     *
     * 【前置条件】
     * - 预约已标记为 status=6（已过期），由调用方统一处理
     * - 预约无签到记录（access_log 中 access_type=1 不存在）
     *
     * 【业务逻辑】
     * 1. 校验数据完整性
     * 2. 显式调用 checkAndUnban（处理封禁过期）
     * 3. 查找该访客上一次爽约记录（status=6 且无签到记录）
     * 4. 若两次爽约间隔在30天内，missedCount+1；否则重置为1
     * 5. missedCount达到2次，禁止预约3个月
     * 6. 强制检查 update 返回值，失败则抛异常触发回滚
     *
     * 【参数说明】
     * @param appointment 爽约的预约实体（status=6，无签到记录）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNoShow(Appointment appointment) {
        Long appointmentId = appointment.getAppointmentId();
        Long visitorId = appointment.getVisitorId();
        LocalDateTime currentEndTime = appointment.getExpectedEndTime();
        LocalDateTime now = LocalDateTime.now();

        logger.info("[爽约判定] 开始处理预约 {}，访客 {}，预计结束时间 {}", appointmentId, visitorId, currentEndTime);

        if (visitorId == null || currentEndTime == null) {
            logger.warn("[爽约判定] 预约 {} 数据异常，仅标记过期", appointmentId);
            appointmentMapper.updateStatus(appointmentId, 6);
            return;
        }

        User user = userMapper.findById(visitorId.intValue());
        if (user == null) {
            logger.warn("[爽约判定] 预约 {} 访客 {} 不存在，仅标记过期", appointmentId, visitorId);
            appointmentMapper.updateStatus(appointmentId, 6);
            return;
        }

        // 显式检查并解封
        checkAndUnban(user);

        logger.info("[爽约判定] 用户 {} 当前 missedCount={}, bannedUntil={}", user.getUserId(), user.getMissedCount(), user.getBannedUntil());

        // 查找该访客上一次爽约记录（status=6 且无签到记录）
        Appointment previous = appointmentMapper.findLatestRelevantBefore(visitorId, currentEndTime);
        logger.info("[爽约判定] 用户 {} 上一次爽约记录={}", user.getUserId(), previous != null ? previous.getAppointmentId() : "无");

        // 判断上一次爽约是否在一个月内
        boolean consecutiveNoShowWithinOneMonth = previous != null
                && previous.getExpectedEndTime() != null
                && !previous.getExpectedEndTime().isBefore(currentEndTime.minusMonths(1));
        logger.info("[爽约判定] 是否连续爽约(一个月内)={}", consecutiveNoShowWithinOneMonth);

        // 计算新的连续爽约次数
        int currentMissedCount = (user.getMissedCount() == null ? 0 : user.getMissedCount());
        int newMissedCount = consecutiveNoShowWithinOneMonth ? currentMissedCount + 1 : 1;
        user.setMissedCount(newMissedCount);
        logger.info("[爽约判定] 用户 {} 爽约次数: {} -> {}", user.getUserId(), currentMissedCount, newMissedCount);

        // 连续爽约2次，禁止预约3个月
        if (newMissedCount >= 2) {
            user.setBannedUntil(now.plusMonths(3));
            logger.info("[爽约判定] 用户 {} 连续爽约{}次，封禁3个月至 {}", user.getUserId(), newMissedCount, user.getBannedUntil());
        }

        // 强制检查更新结果，失败则抛异常触发事务回滚
        int updateRows = userMapper.update(user);
        if (updateRows == 0) {
            throw new RuntimeException("用户 " + user.getUserId() + " 更新失败，missedCount 未写入数据库！");
        } else {
            logger.info("[爽约判定] 用户 {} 更新成功({}行)，missedCount={}，bannedUntil={}",
                    user.getUserId(), updateRows, user.getMissedCount(), user.getBannedUntil());
        }

        // 注：预约状态 status=6（已过期）由调用方（checkNoShowAndPunishForUser 或定时任务）统一标记
        logger.info("[爽约判定] 预约 {} 处罚完成", appointmentId);
    }

    /**
     * 根据手机号查用户
     *
     * 【业务逻辑】
     * 用手机号查询用户，支持匹配管理员预添加账号。
     *
     * 【参数说明】
     * @param phone 手机号
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    /**
     * 根据openid查用户
     *
     * 【业务逻辑】
     * 小程序端使用openid判断用户是否已注册。
     *
     * 【参数说明】
     * @param openid 微信openid
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findByOpenid(String openid) {
        return userMapper.findByOpenid(openid);
    }

    /**
     * 新增用户
     *
     * 【业务逻辑】
     * 写入用户数据。
     *
     * 【参数说明】
     * @param user 用户实体
     *
     * 【返回值】
     * @return 影响行数
     */
    public int insert(User user) {
        return userMapper.insert(user);
    }

    /**
     * 更新用户
     *
     * 【业务逻辑】
     * 更新用户基础信息。
     *
     * 【参数说明】
     * @param user 用户实体
     *
     * 【返回值】
     * @return 影响行数
     */
    public int update(User user) {
        return userMapper.update(user);
    }

    /**
     * 查询封禁已过期的用户列表
     *
     * @param now 当前时间
     * @return 封禁已过期的用户列表
     */
    public List<User> findExpiredBans(LocalDateTime now) {
        return userMapper.listExpiredBans(now);
    }
}
