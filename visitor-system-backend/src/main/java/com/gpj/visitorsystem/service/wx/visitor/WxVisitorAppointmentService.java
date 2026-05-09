package com.gpj.visitorsystem.service.wx.visitor;

import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.AppointmentSetting;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.service.common.AppointmentSettingService;
import com.gpj.visitorsystem.service.common.UserService;
import com.gpj.visitorsystem.util.AppointmentUtil;
import com.gpj.visitorsystem.util.AesEncryptUtil;
import com.gpj.visitorsystem.util.DateTimeUtil;
import com.gpj.visitorsystem.util.JwtUtils;
import com.gpj.visitorsystem.util.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【业务模块】访客预约服务
 *
 * 【核心职责】
 * 1. 处理访客端的预约创建、查询、取消
 * 2. 生成预约二维码（JWT Token + Base64二维码图片）
 * 3. 爽约判定与处罚（连续爽约2次封禁3个月）
 * 4. 查询预约设置和开放状态
 * 5. 身份证号AES加密存储保护隐私
 *
 * 【关键业务场景】
 * 1. 创建预约：访客填写信息提交，校验开放时间、每日上限、封禁状态
 * 2. 查询我的预约：访客查看历史记录和审批状态
 * 3. 查询预约详情：查看审批状态、二维码等详细信息
 * 4. 取消预约：未审批前可取消（status=3）
 * 5. 生成二维码：审批通过后生成JWT Token二维码供安保扫码
 * 6. 爽约判定：超过预计离开时间+30分钟宽限期未签到视为爽约
 *    连续爽约2次封禁3个月，禁止预约
 * 7. 身份证号加密：AES加密后入库，查询时解密展示
 *
 * 【依赖说明】
 * - AppointmentMapper：预约数据持久化
 * - AppointmentSettingService：预约设置校验
 * - UserService：用户查询和封禁状态
 * - AesEncryptUtil：身份证号加密
 * - JwtUtils：生成二维码Token
 * - QRCodeUtil：生成二维码图片
 *
 * 【注意事项】
 * - 创建预约前需校验：开放状态、每日上限、用户封禁状态
 * - 身份证号使用AES加密存储，只有查询详情和安保核验时解密
 * - 爽约判定在定时任务中执行，超过宽限期未签到自动标记
 * - 二维码Token有效期与预约时间段绑定，预计开始时间前失效
 * - 关键操作（创建、取消、爽约判定）需加@Transactional保证原子性
 */
@Service
public class WxVisitorAppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(WxVisitorAppointmentService.class);

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentSettingService appointmentSettingService;

    @Autowired
    private UserService userService;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 【功能】创建访客预约
     *
     * 【业务背景】
     * 访客在小程序端填写预约信息后提交，需要创建预约记录。
     * 创建前需要校验预约时间是否开放、用户是否被封禁、每日预约上限等。
     *
     * 【实现逻辑】
     * 1. 校验预计到达时间不能为空
     * 2. 调用AppointmentSettingService校验预约是否开放
     * 3. 校验每日预约人数上限（如果设置了）
     * 4. 校验用户存在性、封禁状态、用户类型（只有访客能预约）
     * 5. 更新用户真实姓名（如果提交了）
     * 6. 加密身份证号（AES加密存储）
     * 7. 保存预约记录（status=0 待审核）
     *
     * 【参数说明】
     * @param appointment 预约信息，包含visitorId、visitorName、visitorPhone、visitorIdCard等
     *
     * 【返回值】
     * @return 创建后的预约实体（含appointmentId，由MyBatis自动回填）
     *
     * 【异常情况】
     * @throws BusinessException 预约时间未开放、用户被封禁、每日上限已满等
     *
     * 【注意事项】
     * - 新创建的预约状态为0（待审核），需要审批人审批
     * - 身份证号使用AES加密后存储，查询时解密展示
     * - 这里加了@Transactional，保证数据一致性
     */
    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        // 1. 校验预计到达时间
        if (appointment.getExpectedStartTime() == null) {
            throw new BusinessException("预计到达时间不能为空");
        }
        // 2. 校验预约是否开放
        appointmentSettingService.assertAppointmentOpen(appointment.getExpectedStartTime());

        // 2.1 校验每日预约人数上限
        AppointmentSetting setting = appointmentSettingService.getCurrentSetting();
        Integer dailyLimit = setting.getDailyLimit();
        if (dailyLimit != null && dailyLimit > 0) {
            String today = DateTimeUtil.getCurrentDateStr();
            long todayCount = appointmentMapper.countByDate(today);
            if (todayCount >= dailyLimit) {
                throw new BusinessException("今日预约人数已达上限（" + dailyLimit + "人），请明天再试");
            }
        }

        // 3. 校验用户存在性和状态
        User user = userService.findById(appointment.getVisitorId().intValue());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 检查是否被禁止预约
        if (user.getBannedUntil() != null && LocalDateTime.now().isBefore(user.getBannedUntil())) {
            throw new BusinessException("您已被禁止预约至 " + user.getBannedUntil().toString().replace("T", " "));
        }
        // 检查用户类型（只有访客能预约）
        if (user.getUserType() != 1) {
            throw new BusinessException("只有访客才能申请预约");
        }

        // 4. 更新用户真实姓名（如果提交了）
        String visitorName = appointment.getVisitorName();
        if (visitorName != null && !visitorName.trim().isEmpty()) {
            user.setRealName(visitorName.trim());
            userService.update(user);
            logger.info("用户 {} 姓名更新为：{}", user.getUserId(), visitorName.trim());
        }

        // 5. 加密身份证号（AES加密存储）
        String encryptedIdCard = AppointmentUtil.encryptIdCard(
                appointment.getVisitorIdCard(), aesEncryptUtil);
        appointment.setVisitorIdCard(encryptedIdCard);

        // 6. 保存预约（status=0 待审核）
        appointment.setStatus(0);
        appointmentMapper.insert(appointment);
        return appointment;
    }

    /**
     * 【功能】查询我的预约列表
     *
     * 【业务背景】
     * 访客在小程序端查看自己的预约记录，需要展示预约详情。
     * 查询时需要对身份证号解密，并自动应用爽约处罚和过期状态。
     *
     * 【实现逻辑】
     * 1. 根据visitorId查询预约列表
     * 2. 遍历列表，对每个预约：
     *    - 解密身份证号（AES解密，失败则显示为***）
     *    - 检查并应用爽约处罚（checkAndApplyPenalty）
     *    - 应用过期状态（applyExpiredStatus）
     *
     * 【参数说明】
     * @param visitorId 访客用户ID
     *
     * 【返回值】
     * @return 预约列表，按创建时间倒序
     *
     * 【注意事项】
     * - 爽约处罚会在查询时自动应用，更新用户的missedCount和bannedUntil
     * - 过期的预约会自动更新status为6
     */
    public List<Appointment> listMyAppointments(Integer visitorId) {
        User user = userService.findById(visitorId);
        List<Appointment> list = appointmentMapper.listByVisitorId(visitorId);
        for (Appointment app : list) {
            // 解密身份证号（用于前端展示）
            AppointmentUtil.decryptIdCard(app, aesEncryptUtil);
            // 检查并应用爽约处罚
            checkAndApplyPenalty(user, app);
            // 应用过期状态（如果已过期，status改为6）
            AppointmentUtil.applyExpiredStatus(app);
        }
        return list;
    }

    /**
     * 【功能】获取预约详情
     *
     * 【业务背景】
     * 访客查看预约详情时，需要展示完整信息。
     * 查询时需要对身份证号解密，并自动应用爽约处罚和过期状态。
     *
     * 【实现逻辑】
     * 1. 根据appointmentId查询预约
     * 2. 权限校验：只能查看自己的预约
     * 3. 解密身份证号（AES解密，失败则显示为***）
     * 4. 检查并应用爽约处罚（checkAndApplyPenalty）
     * 5. 应用过期状态（applyExpiredStatus）
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【返回值】
     * @return 预约详情
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、无权查看他人预约
     */
    public Appointment getAppointmentDetail(Long appointmentId, Integer visitorId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        // 权限校验：只能查看自己的预约
        if (!appointment.getVisitorId().equals(visitorId.longValue())) {
            throw new BusinessException("无权查看他人预约");
        }
        // 解密身份证号
        AppointmentUtil.decryptIdCard(appointment, aesEncryptUtil);
        User user = userService.findById(visitorId);
        // 检查并应用爽约处罚
        checkAndApplyPenalty(user, appointment);
        // 应用过期状态
        AppointmentUtil.applyExpiredStatus(appointment);
        return appointment;
    }

    /**
     * 【功能】取消预约
     *
     * 【业务背景】
     * 访客在预约未开始或待审核时，可以主动取消预约。
     * 已签到、已完成或已过期的预约不可取消。
     *
     * 【实现逻辑】
     * 1. 根据appointmentId查询预约
     * 2. 权限校验：只能取消自己的预约
     * 3. 状态校验：已签到（4）、已完成（5）、已过期（6）不可取消
     * 4. 更新状态为3（已取消）
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、无权取消、状态不允许取消
     *
     * 【注意事项】
     * - 只有待审核（0）、预约成功（1）可以取消
     * - 取消后状态变为3，不会触发爽约处罚
     */
    @Transactional
    public void cancelAppointment(Long appointmentId, Integer visitorId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        // 权限校验
        if (!appointment.getVisitorId().equals(visitorId.longValue())) {
            throw new BusinessException("无权取消他人预约");
        }
        // 已签到不可取消
        if (appointment.getStatus() == 4) {
            throw new BusinessException("已签到的预约不可取消");
        }
        // 已完成或已过期不可取消
        if (appointment.getStatus() == 5 || appointment.getStatus() == 6) {
            throw new BusinessException("已完成或已过期的预约不可取消");
        }
        // 更新状态为 3（已取消）
        int rows = appointmentMapper.updateStatus(appointmentId, 3);
        if (rows == 0) {
            throw new BusinessException("取消失败");
        }
    }

    /**
     * 【功能】生成预约二维码
     *
     * 【业务背景】
     * 访客预约成功后，需要生成二维码用于安保端扫码核验。
     * 二维码包含JWT Token，Token中包含预约ID和过期时间。
     *
     * 【实现逻辑】
     * 1. 校验预约存在性和权限
     * 2. 校验预约状态（只有1-预约成功、4-已签到可以生成）
     * 3. 校验时间：未到预约时间不能生成，超过宽限期不能生成
     * 4. 生成JWT Token（包含appointmentId和expireTime）
     * 5. 生成Base64二维码图片
     * 6. 保存二维码信息到数据库
     *
     * 【参数说明】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【返回值】
     * @return Base64编码的二维码图片数据，可直接用于前端img标签
     *
     * 【异常情况】
     * @throws BusinessException 预约不存在、无权操作、状态不允许、时间未到或已过期
     *
     * 【注意事项】
     * - 二维码有效期 = 预计离开时间 + 30分钟宽限期
     * - 二维码内容是对JWT Token的Base64编码
     * - 安保端扫码后会解析Token获取appointmentId
     */
    @Transactional
    public String generateQrCode(Long appointmentId, Integer visitorId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null || !appointment.getVisitorId().equals(visitorId.longValue())) {
            throw new BusinessException("预约不存在或无权操作");
        }
        // 只有预约成功（1）或已签到（4）才能生成二维码
        if (appointment.getStatus() != 1 && appointment.getStatus() != 4) {
            throw new BusinessException("只有预约成功或已签到的记录才能生成二维码");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime qrExpireTime = AppointmentUtil.getGraceExpireTime(appointment);
        // 预约时间未到
        if (now.isBefore(appointment.getExpectedStartTime())) {
            throw new BusinessException("预约时间未到，暂无法生成通行码");
        }
        // 预约已过期
        if (now.isAfter(qrExpireTime)) {
            throw new BusinessException("预约已过期，通行码失效");
        }

        // 生成JWT token（包含预约ID和过期时间）
        String token = jwtUtils.generateAppointmentToken(appointmentId, qrExpireTime);
        // 生成Base64二维码
        String qrCodeUrl = QRCodeUtil.generateBase64QRCode(token);
        String expireTimeStr = qrExpireTime.toString();
        // 保存二维码信息
        appointmentMapper.updateQrCode(appointmentId, qrCodeUrl, expireTimeStr);
        return qrCodeUrl;
    }

    /**
     * 【功能】应用过期状态
     *
     * 【业务背景】
     * 预约在不同状态下，过期的判定逻辑不同。
     * 待审核的预约，到达预计离开时间即过期；
     * 预约成功或已签到的，需要加上30分钟宽限期。
     *
     * 【实现逻辑】
     * 1. 如果status=0（待审核），到达expectedEndTime即过期
     * 2. 如果status=1或4（预约成功/已签到），超过expectedEndTime+30分钟才过期
     * 3. 过期后将status更新为6
     *
     * 【参数说明】
     * @param appointment 预约实体
     *
     * 【注意事项】
     * - 此方法会直接修改传入的appointment对象的status
     * - 需要在查询预约时调用，确保状态是最新的
     */

    /**
     * 【功能】检查并应用爽约处罚
     *
     * 【业务背景】
     * 访客预约成功后，如果超过预计离开时间+30分钟宽限期仍未签到，
     * 视为爽约。连续两次爽约（间隔不超过1个月）将封禁预约权限3个月。
     *
     * 【实现逻辑】
     * 1. 只处理status=1（预约成功）的预约
     * 2. 判断是否超过宽限过期时间，未超过则不处理
     * 3. 检查是否存在更晚的爽约处理记录（避免重复处罚）
     * 4. 如果没有，查找上一次预约记录
     * 5. 如果上一次也是爽约且在一个月内，连续爽约次数=2，封禁3个月
     * 6. 否则，爽约次数=1，不封禁
     * 7. 更新用户爽约次数和封禁截止时间
     * 8. 将预约状态更新为6（已过期）
     *
     * 【参数说明】
     * @param user 用户实体
     * @param appointment 预约实体
     *
     * 【注意事项】
     * - 如果已经存在更晚的爽约处理记录，跳过处罚，避免重复
     * - 连续爽约的判定：上一次也是爽约状态，且间隔不超过1个月
     * - 封禁时间是3个月，从当前时间算起
     */
    private void checkAndApplyPenalty(User user, Appointment appointment) {
        // 只处理预约成功（1）的状态
        if (appointment.getStatus() != 1) {
            return;
        }
        // 如果未超过宽限过期时间，不处理
        if (!LocalDateTime.now().isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
            return;
        }

        Long visitorId = appointment.getVisitorId();
        LocalDateTime currentEndTime = appointment.getExpectedEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (visitorId == null || currentEndTime == null) {
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
     * 【功能】判断是否为爽约状态
     *
     * 【业务背景】
     * 预约成功（1）但没签到，超过宽限期后会变成已过期（6），
     * 这两种状态都视为爽约，用于连续爽约的判定。
     *
     * 【实现逻辑】
     * 判断status是否为1或6
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

    /**
     * 【功能】计算二维码宽限过期时间
     *
     * 【业务背景】
     * 二维码的有效期不是预计离开时间，而是预计离开时间+30分钟宽限期。
     * 这样访客如果稍微迟到，仍然可以正常通行。
     *
     * 【实现逻辑】
     * 1. 如果appointment或expectedEndTime为空，返回当前时间（立即过期）
     * 2. 否则返回expectedEndTime + 30分钟
     *
     * 【参数说明】
     * @param appointment 预约实体
     *
     * 【返回值】
     * @return 二维码宽限过期时间
     */
    /**
     * 【功能】获取每日预约人数上限状态
     *
     * 【业务背景】
     * 管理员可以设置每日预约人数上限，访客在创建预约前需要检查是否已达上限。
     * 这个方法返回今日已预约人数、上限、是否已达上限，供前端展示。
     *
     * 【实现逻辑】
     * 1. 从AppointmentSetting获取每日上限（dailyLimit）
     * 2. 查询今日已预约人数（countByDate）
     * 3. 组装结果：dailyLimit、todayCount、reached
     *
     * 【返回值】
     * @return 包含 dailyLimit、todayCount、reached 的 Map
     *
     * 【注意事项】
     * - dailyLimit为0或null时，表示不限制
     * - reached为true时，前端应禁止提交预约
     */
    public Map<String, Object> getDailyLimitStatus() {
        AppointmentSetting setting = appointmentSettingService.getCurrentSetting();
        int dailyLimit = setting.getDailyLimit() == null ? 0 : setting.getDailyLimit();

        String today = DateTimeUtil.getCurrentDateStr();
        long todayCount = appointmentMapper.countByDate(today);

        Map<String, Object> result = new HashMap<>();
        result.put("dailyLimit", dailyLimit);
        result.put("todayCount", (int) todayCount);
        result.put("reached", dailyLimit > 0 && todayCount >= dailyLimit);
        return result;
    }
}