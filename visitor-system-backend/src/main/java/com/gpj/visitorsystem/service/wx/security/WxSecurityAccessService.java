package com.gpj.visitorsystem.service.wx.security;

import com.gpj.visitorsystem.dto.wx.security.*;
import com.gpj.visitorsystem.entity.AccessLog;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.mapper.AccessLogMapper;
import com.gpj.visitorsystem.mapper.AppointmentMapper;
import com.gpj.visitorsystem.service.common.UserService;
import com.gpj.visitorsystem.util.AppointmentUtil;
import com.gpj.visitorsystem.util.AesEncryptUtil;
import com.gpj.visitorsystem.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 【业务模块】安保端通行管理
 *
 * 【核心职责】
 * 1. 扫码核验与确认签到/签离
 * 2. 手动登记与手动签离处理
 * 3. 通行记录查询与统计
 * 4. 定时扫描与推送通行告警
 *
 * 【关键业务场景】
 * 1. 扫码核验解析JWT Token并按预约状态决定签到/签离流程
 * 2. 紧急登记无需预约，需完整访客信息并标记紧急通行
 * 3. 超过预计离开时间+30分钟未签离触发告警与人工处理
 *
 * 【依赖说明】
 * - AccessLogMapper：通行记录写入与统计
 * - AppointmentMapper：预约状态查询与更新
 * - UserService：用户状态与封禁处理
 * - JwtUtils：二维码Token校验与解析
 * - AesEncryptUtil：身份证号解密
 * - SecurityAlertCenter：告警发布与清理
 *
 * 【注意事项】
 * - 所有安保接口权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
 * - 扫码核验为预检，写库需走确认接口
 * - 关键状态更新方法需加@Transactional保证一致性
 */
@Service
public class WxSecurityAccessService {

    /** 紧急登记告警类型 */
    private static final String EMERGENCY_ALERT_TYPE = "EMERGENCY_REGISTERED";

    @Autowired
    private AccessLogMapper accessLogMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    @Autowired
    private SecurityAlertCenter securityAlertCenter;

    /**
     * 【功能】扫码核验（安保端核心功能）
     * 
     * 【业务背景】
     * 访客到门口后，出示小程序里的二维码（其实是JWT Token），
     * 安保人员用小程序扫码，后端要校验这个Token是否合法，
     * 然后根据预约状态判断是「签到」还是「签离」，
     * 最后返回给安保人员确认。
     * 
     * 【实现逻辑】
     * 1. 参数校验：qrToken不能为空，空的话直接报错
     * 2. 校验JWT Token有效性：
     *    - 用JwtUtils.validateAppointmentToken()校验
     *    - 如果无效，推送告警（INVALID_QR），返回失败
     * 3. 解析Token，拿到appointmentId，查预约表
     *    - 如果预约不存在，推送告警（APPOINTMENT_NOT_FOUND），返回失败
     * 4. 根据预约状态分支处理：
     *    - status=1（预约成功）：
     *        * 未到预约起始时间 → 失败（TIME_NOT_ARRIVED）
     *        * 超过宽限期（预计离开时间+30分钟） → 失败（QR_EXPIRED）
     *        * 已签到 → 失败（ALREADY_SIGNED_IN）
     *        * 都通过了 → 返回待确认（pendingConfirm）
     *    - status=4（已签到）：
     *        * 超过宽限期 → 失败（QR_EXPIRED）
     *        * 已签离 → 失败（ALREADY_SIGNED_OUT）
     *        * 都通过了 → 返回待确认（pendingConfirm）
     *    - status=6（已过期）：
     *        * 已签到但未签离 → 失败（QR_EXPIRED），提示手动签离
     *    - 其他状态 → 失败（STATUS_INVALID）
     * 5. 返回核验结果（通过/失败/需确认）
     * 
     * 【参数说明】
     * @param qrToken 二维码里的JWT Token，访客小程序生成
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityScanVerifyResponseDTO，包含：
     *         - passed：是否通过
     *         - pendingConfirm：是否需要安保确认
     *         - alertType：告警类型
     *         - alertMessage：告警消息
     *         - 访客信息（姓名、手机号、身份证号等）
     * 
     * 【异常情况】
     * @throws BusinessException Token无效、预约不存在等
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 二维码（JWT Token）有效期2小时，过期后扫码会失败
     * - 宽限期是30分钟（QR_GRACE_MINUTES），超过后不能自动签到/签离
     * - 这里加了@Transactional，保证数据一致性
     * - 扫码核验只是"预检"，确认要调confirmScanAction()接口
     */
    @Transactional
    public SecurityScanVerifyResponseDTO scanVerify(String qrToken, Integer securityUserId) {
        // 1. 参数校验
        if (qrToken == null || qrToken.trim().isEmpty()) {
            throw new BusinessException("二维码内容为空");
        }

        // 3. 校验JWT token有效性
        if (!jwtUtils.validateAppointmentToken(qrToken)) {
            securityAlertCenter.publish("INVALID_QR", "二维码无效或已过期", null, null);
            SecurityScanVerifyResponseDTO fail = new SecurityScanVerifyResponseDTO();
            fail.setPassed(false);
            fail.setAlertType("INVALID_QR");
            fail.setAlertMessage("二维码无效或已过期");
            return fail;
        }

        // 4. 解析token，获取预约ID
        Claims claims = jwtUtils.parseToken(qrToken);
        Long appointmentId = claims.get("appointmentId", Long.class);
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            securityAlertCenter.publish("APPOINTMENT_NOT_FOUND", "未找到对应预约记录", appointmentId, null);
            SecurityScanVerifyResponseDTO fail = new SecurityScanVerifyResponseDTO();
            fail.setPassed(false);
            fail.setAlertType("APPOINTMENT_NOT_FOUND");
            fail.setAlertMessage("未找到对应预约记录");
            return fail;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime graceExpireTime = AppointmentUtil.getGraceExpireTime(appointment);
        String idCard = AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil);

        // 5. 根据预约状态分支处理
        // 5.1 预约成功（status=1）：核验签到
        if (appointment.getStatus() == 1) {
            // 未到预约起始时间
            if (now.isBefore(appointment.getExpectedStartTime())) {
                return fail("TIME_NOT_ARRIVED", "尚未到达预约起始时间", appointment, idCard);
            }
            // 已超过宽限过期时间
            if (now.isAfter(graceExpireTime)) {
                return fail("QR_EXPIRED", "通行码已过期，请安保手动处理", appointment, idCard);
            }
            // 已签到
            if (accessLogMapper.countByAppointmentAndType(appointmentId, 1) > 0) {
                return fail("ALREADY_SIGNED_IN", "该访客已签到", appointment, idCard);
            }

            // 返回待确认状态（前端弹出确认框）
            return pendingConfirm(appointment, idCard, "SIGN_IN", "请现场核验访客身份，确认无误后点击通过签到");
        }

        // 5.2 已签到（status=4）：核验签离
        if (appointment.getStatus() == 4) {
            // 已超过宽限过期时间
            if (now.isAfter(graceExpireTime)) {
                return fail("QR_EXPIRED", "访客滞留已超30分钟宽限，请安保手动签离", appointment, idCard);
            }
            // 已签离
            if (accessLogMapper.countByAppointmentAndType(appointmentId, 2) > 0) {
                return fail("ALREADY_SIGNED_OUT", "该访客已签离", appointment, idCard);
            }
            // 返回待确认状态
            return pendingConfirm(appointment, idCard, "SIGN_OUT", "请现场核验访客身份，确认无误后点击通过签离");
        }

        // 5.3 已过期（status=6）：提示需手动签离
        if (appointment.getStatus() == 6) {
            int signInCount = accessLogMapper.countByAppointmentAndType(appointmentId, 1);
            int signOutCount = accessLogMapper.countByAppointmentAndType(appointmentId, 2);
            if (signInCount > 0 && signOutCount == 0) {
                return fail("QR_EXPIRED", "访客滞留已超30分钟宽限，请安保手动签离", appointment, idCard);
            }
        }

        // 5.4 其他状态：不支持扫码通行
        return fail("STATUS_INVALID", "当前预约状态不支持扫码通行", appointment, idCard);
    }

    /**
     * 【功能】确认扫码操作（签到/签离）
     * 
     * 【业务背景】
     * 安保人员扫码核验通过后，需要点击"通过"确认通行。
     * 根据预约状态自动判断是签到还是签离操作。
     * 这里是真正写数据库的地方，扫码核验（scanVerify）只是预检。
     * 
     * 【实现逻辑】
     * 1. 参数校验：appointmentId不能为空，空的话直接报错
     * 2. 查预约表，预约不存在直接抛异常
     * 3. 根据预约状态分支处理：
     *    - status=1（预约成功）：执行签到
     *        * 未到预约起始时间 → 抛异常
     *        * 超过宽限期 → 抛异常
     *        * 已签到 → 抛异常
     *        * 都通过了 → 插入access_log（access_type=1），更新预约状态为4
     *    - status=4（已签到）：执行签离
     *        * 超过宽限期 → 抛异常
     *        * 无签到记录 → 抛异常
     *        * 已签离 → 抛异常
     *        * 都通过了 → 插入access_log（access_type=2），更新预约状态为5，清除告警
     * 4. 返回操作结果DTO
     * 
     * 【参数说明】
     * @param appointmentId 预约ID，从前端传过来
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityScanVerifyResponseDTO，包含操作类型、操作时间、通行记录ID等
     * 
     * 【异常情况】
     * @throws BusinessException 预约不存在、状态不允许、未到时间、已过期等
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 签到后状态变为4，签离后状态变为5
     * - 签离成功后会清除该预约的告警（securityAlertCenter.clearByAppointmentId）
     * - 这里加了@Transactional，保证数据一致性
     * - 这里是真正写数据库的地方，扫码核验（scanVerify）只是预检
     */
    @Transactional
    public SecurityScanVerifyResponseDTO confirmScanAction(Long appointmentId, Integer securityUserId) {
        // 1. 参数校验
        if (appointmentId == null) {
            throw new BusinessException("缺少预约信息，无法确认通行");
        }

        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }

        String idCard = AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil);
        LocalDateTime now = LocalDateTime.now();

        // 3.1 预约成功（status=1）：执行签到
        if (appointment.getStatus() == 1) {
            // 未到预约起始时间
            if (now.isBefore(appointment.getExpectedStartTime())) {
                throw new BusinessException("尚未到达预约起始时间");
            }
            // 通行码已过期
            if (now.isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
                throw new BusinessException("通行码已过期，请安保手动处理");
            }
            // 已签到
            if (accessLogMapper.countByAppointmentAndType(appointmentId, 1) > 0) {
                throw new BusinessException("该访客已签到");
            }

            // 插入签到记录
            AccessLog log = new AccessLog();
            log.setAppointmentId(appointmentId);
            log.setVisitorId(appointment.getVisitorId());
            log.setSecurityId(securityUserId.longValue());
            log.setAccessType(1);  // 1-签到
            log.setAccessTime(now);
            log.setVerifyMethod(1);  // 1-扫码
            accessLogMapper.insert(log);
            // 更新预约状态为 4（已签到）
            appointmentMapper.updateStatus(appointmentId, 4);

            return success("SIGN_IN", appointment, idCard, log);
        }

        // 3.2 已签到（status=4）：执行签离
        if (appointment.getStatus() == 4) {
            // 访客滞留已超30分钟宽限
            if (now.isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
                throw new BusinessException("访客滞留已超30分钟宽限，请安保手动签离");
            }
            // 无签到记录
            if (accessLogMapper.countByAppointmentAndType(appointmentId, 1) == 0) {
                throw new BusinessException("该预约无签到记录，无法签离");
            }
            // 已签离
            if (accessLogMapper.countByAppointmentAndType(appointmentId, 2) > 0) {
                throw new BusinessException("该访客已签离");
            }

            // 插入签离记录
            AccessLog log = new AccessLog();
            log.setAppointmentId(appointmentId);
            log.setVisitorId(appointment.getVisitorId());
            log.setSecurityId(securityUserId.longValue());
            log.setAccessType(2);  // 2-签离
            log.setAccessTime(now);
            log.setVerifyMethod(1);  // 1-扫码
            accessLogMapper.insert(log);
            // 更新预约状态为 5（已完成）
            appointmentMapper.updateStatus(appointmentId, 5);
            // 清除该预约的告警
            securityAlertCenter.clearByAppointmentId(appointmentId);

            return success("SIGN_OUT", appointment, idCard, log);
        }

        throw new BusinessException("当前预约状态不支持扫码确认");
    }

    /**
     * 【功能】手动登记（关联预约/紧急登记）
     * 
     * 【业务背景】
     * 访客二维码过期、手机没电、或者没预约（紧急登记），
     * 安保人员可以手动登记通行。
     * 分两种场景：关联预约登记、紧急登记（需审批人授权）。
     * 
     * 【实现逻辑】
     * 1. 参数校验：
     *    - accessType必须是1（签到）或2（签离）
     *    - scenario必须是1（关联预约）或2（紧急登记）
     * 2. 根据场景分支处理：
     *    - 场景1（关联预约）：调manualForAppointment()
     *    - 场景2（紧急登记）：调manualEmergency()
     * 3. 返回通行记录DTO
     * 
     * 【参数说明】
     * @param request 手动登记请求DTO，包含：
     *         - scenario：场景（1-关联预约，2-紧急登记）
     *         - appointmentId：预约ID（场景1必填）
     *         - accessType：通行类型（1-签到，2-签离）
     *         - authorizerId：授权审批人ID（场景2必填）
     *         - visitorName：访客姓名（场景2必填）
     *         - visitorPhone：访客手机号（场景2必填）
     *         - visitorIdCard：访客身份证号（场景2必填）
     *         - visitReason：来访事由（场景2必填）
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityAccessRecordDTO，包含通行记录详情
     * 
     * 【异常情况】
     * @throws BusinessException 预约不存在、状态不允许、缺少授权等
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 紧急登记需要审批人授权（authorizerId），不然不让登
     * - 紧急登记会推送告警通知管理员
     * - 这里加了@Transactional，保证数据一致性
     */
    @Transactional
    public SecurityAccessRecordDTO manualRegister(SecurityManualRegisterRequestDTO request, Integer securityUserId) {
        // 1. 参数校验
        if (request.getAccessType() == null || (request.getAccessType() != 1 && request.getAccessType() != 2)) {
            throw new BusinessException("登记类型仅支持签到/签离");
        }
        if (request.getScenario() == null || (request.getScenario() != 1 && request.getScenario() != 2)) {
            throw new BusinessException("登记场景不正确");
        }

        // 3. 根据场景分支处理
        if (request.getScenario() == 1) {
            // 场景1：关联预约登记
            return manualForAppointment(request, securityUserId);
        }
        // 场景2：紧急登记
        return manualEmergency(request, securityUserId);
    }

    /**
     * 【功能】手动签离（过期预约）
     * 
     * 【业务背景】
     * 访客超过30分钟宽限期仍未签离，安保人员需要手动签离。
     * 紧急登记的预约不受宽限期限制，随时可以签离。
     * 
     * 【实现逻辑】
     * 1. 参数校验：appointmentId不能为空，空的话直接报错
     * 2. 查预约表，预约不存在直接抛异常
     * 3. 检查是否为紧急登记（isEmergencyAppointment()）
     * 4. 状态校验：必须为4（已签到）或6（已过期），不然不让签离
     * 5. 签到记录校验：必须有签到记录（accessLogMapper.countByAppointmentAndType(appointmentId, 1) > 0）
     * 6. 已签离校验：必须未签离（accessLogMapper.countByAppointmentAndType(appointmentId, 2) == 0）
     * 7. 时间校验（非紧急登记）：必须超过宽限期（now.isAfter(getGraceExpireTime(appointment))）
     * 8. 插入签离记录（access_type=2，verify_method=2）
     * 9. 更新预约状态为5（已完成），清除告警
     * 
     * 【参数说明】
     * @param appointmentId 预约ID，从前端传过来
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityAccessRecordDTO，包含签离时间、停留时长等
     * 
     * 【异常情况】
     * @throws BusinessException 预约不存在、状态不允许、无签到记录、已签离、未超过宽限期
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 紧急登记不受宽限期限制（getGraceExpireTime()返回100年后）
     * - 签离后状态变为5，会清除告警（securityAlertCenter.clearByAppointmentId）
     * - 这里加了@Transactional，保证数据一致性
     * - 紧急登记需记录授权审批人ID（log.setAuthorizerId）
     */
    @Transactional
    public SecurityAccessRecordDTO manualSignOutExpired(Long appointmentId, Integer securityUserId) {
        // 1. 参数校验
        if (appointmentId == null) {
            throw new BusinessException("请提供预约ID");
        }

        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }

        // 3. 检查是否为紧急登记
        boolean emergencyAppointment = AppointmentUtil.isEmergencyAppointment(appointment);
        
        // 4. 状态校验
        if (appointment.getStatus() != 4 && appointment.getStatus() != 6) {
            throw new BusinessException("当前预约状态不支持手动签离");
        }

        // 5. 签到记录校验
        if (accessLogMapper.countByAppointmentAndType(appointmentId, 1) == 0) {
            throw new BusinessException("该预约无签到记录，无法手动签离");
        }
        // 6. 已签离校验
        if (accessLogMapper.countByAppointmentAndType(appointmentId, 2) > 0) {
            throw new BusinessException("该访客已签离");
        }

        // 7. 时间校验（非紧急登记必须超过宽限期）
        LocalDateTime now = LocalDateTime.now();
        if (!emergencyAppointment && !now.isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
            throw new BusinessException("未超过预计离开时间30分钟宽限，暂不支持手动签离");
        }

        // 8. 插入签离记录
        AccessLog log = new AccessLog();
        log.setAppointmentId(appointmentId);
        log.setVisitorId(appointment.getVisitorId());
        log.setSecurityId(securityUserId.longValue());
        log.setAccessType(2);  // 2-签离
        log.setAccessTime(now);
        log.setVerifyMethod(2);  // 2-手动
        // 紧急登记需记录授权审批人ID
        if (emergencyAppointment) {
            log.setAuthorizerId(appointment.getApproverId());
        }
        accessLogMapper.insert(log);

        // 9. 更新预约状态为 5（已完成），清除告警
        appointmentMapper.updateStatus(appointmentId, 5);
        securityAlertCenter.clearByAppointmentId(appointmentId);
        return accessLogMapper.findRecordDetail(log.getLogId());
    }

    /**
     * 【功能】查询通行记录列表
     * 
     * 【业务背景】
     * 安保人员查看通行记录，支持多条件筛选。
     * 数据可能来自预约（有visitorName）或手动登记（有visitorNameAlt），需要合并展示。
     * 
     * 【实现逻辑】
     * 1. 查询记录（支持多条件筛选）：
     *    - 调accessLogMapper.listRecords()
     *    - 支持按日期范围、关键词、通行类型、核验方式、是否紧急登记筛选
     * 2. 数据合并和解密：
     *    - visitorName为空时使用visitorNameAlt
     *    - visitorPhone为空时使用visitorPhoneAlt
     *    - visitorIdCard需要解密（失败则显示为***）
     * 3. 返回通行记录DTO列表
     * 
     * 【参数说明】
     * @param startDate 开始日期（yyyy-MM-dd，可选），为空则查所有
     * @param endDate 结束日期（yyyy-MM-dd，可选），为空则查所有
     * @param keyword 关键词（访客姓名/手机号，可选），模糊匹配
     * @param accessType 通行类型（1-签到 2-签离，可选），为空则查所有
     * @param verifyMethod 核验方式（1-扫码 2-手动，可选），为空则查所有
     * @param emergencyOnly 是否仅显示紧急登记（可选），true则只查紧急登记
     *
     * 【返回值】
     * @return List<SecurityAccessRecordDTO>，数据已合并和解密
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 数据可能来自两张表，需要合并展示
     * - 身份证号已解密，可直接展示（失败则显示为***）
     */
    public List<SecurityAccessRecordDTO> listRecords(String startDate, String endDate,
                                                     String keyword, Integer accessType,
                                                     Integer verifyMethod, Boolean emergencyOnly) {        
        // 1. 查询记录
        List<SecurityAccessRecordDTO> list = accessLogMapper.listRecords(startDate, endDate, keyword, accessType, verifyMethod, emergencyOnly);
        
        // 2. 数据合并和解密
        for (SecurityAccessRecordDTO item : list) {
            AppointmentUtil.mergeRecordData(item, aesEncryptUtil);
        }
        return list;
    }

    /**
     * 【功能】获取通行记录详情
     * 
     * 【业务背景】
     * 安保人员查看某条通行记录的详细信息。
     * 需要对数据合并（visitorName/visitorNameAlt）和解密（visitorIdCard）。
     * 
     * 【实现逻辑】
     * 1. 查询详情：调accessLogMapper.findRecordDetail()
     * 2. 数据合并（同listRecords逻辑）：
     *    - visitorName为空时使用visitorNameAlt
     *    - visitorPhone为空时使用visitorPhoneAlt
     * 3. 解密身份证号（失败则显示为***）
     * 4. 返回通行记录详情DTO
     *
     * 【参数说明】
     * @param logId 通行记录ID，从前端传过来
     *
     * 【返回值】
     * @return SecurityAccessRecordDTO，数据已合并和解密
     * 
     * 【异常情况】
     * @throws BusinessException 记录不存在
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 这里没有加@Transactional，因为只是查询，不涉及数据修改
     * - 身份证号解密失败会显示为***，不会抛异常
     */
    public SecurityAccessRecordDTO getRecordDetail(Long logId) {
        // 1. 查询详情
        SecurityAccessRecordDTO detail = accessLogMapper.findRecordDetail(logId);
        if (detail == null) {
            throw new BusinessException("记录不存在");
        }
        
        // 2. 数据合并和解密
        AppointmentUtil.mergeRecordData(detail, aesEncryptUtil);
        return detail;
    }

    /**
     * 【功能】获取通行统计信息
     * 
     * 【业务背景】
     * 安保人员在小程序端查看通行统计图表。
     * 支持三种统计周期：今日、近7日、近30日。
     * 
     * 【实现逻辑】
     * 1. 权限校验：必须是安保人员（userType=3），不然不让用
     * 2. 确定统计周期：
     *    - period为空或为null → 默认day
     *    - "week" → 近7日（start = 今天-6天）
     *    - "month" → 近30日（start = 今天-29天）
     *    - 其他 → 今日（start = 今天0点）
     * 3. 查询统计数据：
     *    - 调accessLogMapper.countFlowByDate(start) → 每日通行量
     *    - 调accessLogMapper.countFlowByHour(start) → 每小时通行量
     *    - 调accessLogMapper.countTotalFrom(start) → 总通行量
     * 4. 计算高峰时段（hourPoints中value最大的）：
     *    - 遍历hourPoints，找value最大的那个
     *    - 如果没找到，peakHour设为"-"
     * 5. 组装返回结果（SecurityStatsDTO）
     * 
     * 【参数说明】
     * @param period 统计周期（day/week/month，可选），为空则默认day
     *
     * 【返回值】
     * @return SecurityStatsDTO，包含：
     *         - period：统计周期
     *         - totalFlow：总通行量
     *         - peakHour：高峰时段
     *         - flowPoints：每日通行量（给折线图用）
     *         - hourPoints：每小时通行量（给柱状图用）
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 高峰时段是根据hourPoints计算的，可能不是绝对准确
     * - period不合法时默认使用day
     * - 这里没有加@Transactional，因为只是查询，不涉及数据修改
     */
    public SecurityStatsDTO stats(String period) {
        // 1. 确定统计周期
        String usePeriod = (period == null || period.isBlank()) ? "day" : period;
        LocalDateTime start;
        if ("week".equals(usePeriod)) {
            start = LocalDate.now().minusDays(6).atStartOfDay();  // 近7日
        } else if ("month".equals(usePeriod)) {
            start = LocalDate.now().minusDays(29).atStartOfDay();  // 近30日
        } else {
            start = LocalDate.now().atStartOfDay();  // 今日
            usePeriod = "day";
        }

        // 3. 查询统计数据
        List<SecurityStatsDTO.StatsPoint> flowPoints = accessLogMapper.countFlowByDate(start);  // 每日通行量
        List<SecurityStatsDTO.StatsPoint> hourPoints = accessLogMapper.countFlowByHour(start);  // 每小时通行量
        Long total = accessLogMapper.countTotalFrom(start);  // 总通行量

        // 4. 计算高峰时段
        String peakHour = "-";
        long peakValue = -1;
        for (SecurityStatsDTO.StatsPoint point : hourPoints) {
            if (point.getValue() != null && point.getValue() > peakValue) {
                peakValue = point.getValue();
                peakHour = point.getLabel();
            }
        }

        // 5. 组装返回结果
        SecurityStatsDTO dto = new SecurityStatsDTO();
        dto.setPeriod(usePeriod);
        dto.setTotalFlow(total == null ? 0L : total);
        dto.setPeakHour(peakHour);
        dto.setFlowPoints(flowPoints);
        dto.setHourPoints(hourPoints);
        return dto;
    }

    /**
     * 【功能】拉取告警列表
     * 
     * 【业务背景】
     * 安保人员需要实时查看告警信息。
     * 告警来自两部分：缓存的告警、活跃的紧急登记。
     * 
     * 【实现逻辑】
     * 1. 权限校验：必须是安保人员（userType=3），不然不让用
     * 2. 从告警中心拉取缓存的告警（只读取不清空）：
     *    - 调securityAlertCenter.peek()
     * 3. 查询活跃的紧急登记：
     *    - 调appointmentMapper.listEmergencyActive()
     * 4. 合并去重（按alertType+appointmentId）：
     *    - 用LinkedHashMap，key是alertType+"-"+appointmentId
     *    - 如果已经存在，就不重复加
     * 5. 按发生时间倒序返回：
     *    - 转成List，用Comparator.comparing().reversed()排序
     * 
     * 【参数说明】
     * - 无（用户ID和类型已在拦截器层校验）
     *
     * 【返回值】
     * @return List<SecurityAlertDTO>，按发生时间倒序
     * 
     * 【注意事项】
     * - 权限校验已在拦截器层完成（WxSecurityAuthInterceptor）
     * - 告警中心是缓存的，拉取后不清空（peek()方法）
     * - 活跃的紧急登记也需要作为告警展示
     * - 这里没有加@Transactional，因为只是查询，不涉及数据修改
     */
    public List<SecurityAlertDTO> pollAlerts() {
        // 1. 从告警中心拉取缓存的告警（只读取不清空）
        List<SecurityAlertDTO> cachedAlerts = securityAlertCenter.peek();
        
        // 3. 查询活跃的紧急登记
        List<Appointment> emergencyList = appointmentMapper.listEmergencyActive();

        // 4. 合并去重（按alertType+appointmentId）
        Map<String, SecurityAlertDTO> merged = new java.util.LinkedHashMap<>();
        for (SecurityAlertDTO alert : cachedAlerts) {
            if (alert == null) {
                continue;
            }
            String key = alert.getAlertType() + "-" + (alert.getAppointmentId() == null ? "none" : alert.getAppointmentId());
            merged.put(key, alert);
        }

        // 5. 添加紧急登记告警（如果不存在）
        for (Appointment appointment : emergencyList) {
            String key = EMERGENCY_ALERT_TYPE + "-" + appointment.getAppointmentId();
            if (merged.containsKey(key)) {
                continue;
            }
            SecurityAlertDTO emergencyAlert = buildEmergencyAlert(appointment);
            merged.put(key, emergencyAlert);
        }

        // 6. 按发生时间倒序
        List<SecurityAlertDTO> result = new java.util.ArrayList<>(merged.values());
        result.sort(java.util.Comparator.comparing(SecurityAlertDTO::getOccurredAt).reversed());
        return result;
    }

    /**
     * 【功能】定时扫描超时告警
     * 
     * 【业务背景】
     * 访客到了预计离开时间还没签离，安保人员可能不知道，
     * 所以用定时任务每分钟扫描一次，发现有超时的就推告警给安保端。
     * 
     * 【实现逻辑】
     * 1. 计算当前时间和宽限期起始时间（now - 30分钟）
     * 2. 扫描已到离开时间但未签离的预约（未超过宽限期）：
     *    - 调appointmentMapper.listLeaveTimeReached()
     *    - 计算超过离开时间多少分钟
     *    - 先清除旧告警，再发布LEAVE_TIME_REACHED告警
     * 3. 扫描超时滞留的预约（已超过宽限期）：
     *    - 调appointmentMapper.listOvertimeStaying()
     *    - 先清除旧告警，再更新状态为6（已过期）
     *    - 计算超过宽限过期时间多少分钟
     *    - 发布OVERTIME_EXPIRED告警
     * 
     * 【注意事项】
     * - 定时任务每分钟执行一次（@Scheduled(cron = "0 * * * * *")）
     * - 告警会推送到安保端（通过WebSocket）
     * - 已过期（status=6）的预约，安保可以手动签离
     */
    @Scheduled(cron = "0 * * * * *")
    public void scanOvertimeAlerts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime graceStart = now.minusMinutes(AppointmentUtil.QR_GRACE_MINUTES);  // 宽限期起始时间

        // 1. 扫描已到离开时间但未签离的预约（未超过宽限期）
        List<Appointment> reachedList = appointmentMapper.listLeaveTimeReached(now, graceStart);
        for (Appointment appointment : reachedList) {
            long minutes = Duration.between(appointment.getExpectedEndTime(), now).toMinutes();  // 超过离开时间多少分钟
            String idCard = AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil);
            LocalDateTime signInTime = findSignInTime(appointment.getAppointmentId());
            // 先清除该预约的旧告警，再发布新告警（避免重复累积）
            securityAlertCenter.clearByAppointmentId(appointment.getAppointmentId());
            // 发布 LEAVE_TIME_REACHED 告警
            securityAlertCenter.publish(
                    "LEAVE_TIME_REACHED",
                    "访客已到预计离开时间，仍未签离，请尽快提醒",
                    appointment,
                    false,  // canManualSignOut = false（未超过宽限期，不能手动签离）
                (int) Math.max(minutes, 0),
                idCard,
                appointment.getVisitReason(),
                signInTime
            );
        }

        // 2. 扫描超时滞留的预约（已超过宽限期）
        List<Appointment> overtimeList = appointmentMapper.listOvertimeStaying(graceStart);
        for (Appointment appointment : overtimeList) {
            // 清除旧告警（包括LEAVE_TIME_REACHED），发布新告警
            securityAlertCenter.clearByAppointmentId(appointment.getAppointmentId());
            // 更新状态为 6（已过期）
            if (appointment.getStatus() != null && appointment.getStatus() != 6) {
                appointmentMapper.updateStatus(appointment.getAppointmentId(), 6);
                appointment.setStatus(6);
            }
            long minutes = Duration.between(AppointmentUtil.getGraceExpireTime(appointment), now).toMinutes();  // 超过宽限过期时间多少分钟
            String idCard = AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil);
            LocalDateTime signInTime = findSignInTime(appointment.getAppointmentId());
            // 发布 OVERTIME_EXPIRED 告警
            securityAlertCenter.publish(
                    "OVERTIME_EXPIRED",
                    "访客滞留超时已过30分钟宽限，请安保手动签离",
                    appointment,
                    true,  // canManualSignOut = true（可以手动签离）
                    (int) Math.max(minutes, 0),
                    idCard,
                    appointment.getVisitReason(),
                    signInTime
            );
        }
    }

    /**
     * 构建失败的扫码核验响应
     * 
     * 【业务背景】
     * 扫码核验失败时，需要构建统一的失败响应。
     * 这个方法会发布告警，并填充访客信息。
     * 
     * 【实现逻辑】
     * 1. 判断是否可以手动签离（仅QR_EXPIRED且status=4或6时）
     * 2. 发布告警（securityAlertCenter.publish）
     * 3. 构建失败响应DTO：
     *    - passed = false
     *    - pendingConfirm = false
     *    - 设置alertType和alertMessage
     * 4. 如果appointment不为null，填充访客信息
     * 5. 返回失败响应DTO
     * 
     * 【参数说明】
     * @param type 告警类型（如：INVALID_QR、TIME_NOT_ARRIVED、QR_EXPIRED等）
     * @param msg 提示信息（展示给安保人员）
     * @param appointment 预约实体（可为null，为null时不填充访客信息）
     * @param idCard 解密后的身份证号（可为null）
     * 
     * 【返回值】
     * @return SecurityScanVerifyResponseDTO，包含失败信息和告警类型
     * 
     * 【注意事项】
     * - 这个方法会发布告警，告警会推送到安保端
     * - 如果appointment为null，不会填充访客信息
     * - canManualSignOut的判断逻辑：type是QR_EXPIRED且status是4或6
     */
    private SecurityScanVerifyResponseDTO fail(String type, String msg, Appointment appointment, String idCard) {
        // 判断是否可以手动签离（仅QR_EXPIRED且status=4或6时）
        boolean canManualSignOut = appointment != null
                && "QR_EXPIRED".equals(type)
                && (appointment.getStatus() != null && (appointment.getStatus() == 4 || appointment.getStatus() == 6));
        // 发布告警
        if (appointment != null) {
            securityAlertCenter.publish(type, msg, appointment, canManualSignOut, null,
                    idCard, appointment.getVisitReason(), findSignInTime(appointment.getAppointmentId()));
        } else {
            securityAlertCenter.publish(type, msg, null, null);
        }
        // 构建失败响应
        SecurityScanVerifyResponseDTO fail = new SecurityScanVerifyResponseDTO();
        fail.setPassed(false);
        fail.setPendingConfirm(false);
        fail.setAlertType(type);
        fail.setAlertMessage(msg);
        if (appointment != null) {
            fillVisitor(fail, appointment, idCard);
        }
        return fail;
    }

    /**
     * 构建待确认的扫码核验响应
     * 
     * 【业务背景】
     * 扫码核验通过后，需要安保人员现场确认（比如看下访客身份证）。
     * 这个方法就是构建"待确认"的响应，前端会弹个确认框。
     * 
     * 【实现逻辑】
     * 1. 创建SecurityScanVerifyResponseDTO对象
     * 2. 设置passed=true，pendingConfirm=true
     * 3. 设置actionType和alertMessage
     * 4. 调用fillVisitor()填充访客信息
     * 5. 返回DTO
     * 
     * 【参数说明】
     * @param appointment 预约实体，用来填充访客信息
     * @param idCard 解密后的身份证号，用来填充访客信息
     * @param actionType 操作类型，是"SIGN_IN"还是"SIGN_OUT"
     * @param message 提示信息，展示给安保人员看
     * 
     * 【返回值】
     * @return SecurityScanVerifyResponseDTO，前端根据这个弹确认框
     * 
     * 【注意事项】
     * - 这个方法只是构建响应，不写数据库
     * - 安保人员点"确认"后，才会调confirmScanAction()
     */
    private SecurityScanVerifyResponseDTO pendingConfirm(Appointment appointment, String idCard, String actionType, String message) {
        SecurityScanVerifyResponseDTO pending = new SecurityScanVerifyResponseDTO();
        pending.setPassed(true);
        pending.setPendingConfirm(true);  // 需要确认
        pending.setActionType(actionType);
        pending.setAlertMessage(message);
        fillVisitor(pending, appointment, idCard);
        return pending;
    }

    /**
     * 构建成功的扫码核验响应
     * 
     * 【业务背景】
     * 扫码核验并确认通过后，需要构建成功的响应DTO。
     * 这个方法会填充访客信息和通行记录信息。
     * 
     * 【实现逻辑】
     * 1. 创建SecurityScanVerifyResponseDTO对象
     * 2. 设置passed=true，pendingConfirm=false
     * 3. 设置actionType、accessType、verifyMethod、accessTime
     * 4. 填充访客信息（appointmentId、visitorId、visitorName等）
     * 5. 返回DTO
     * 
     * 【参数说明】
     * @param action 操作类型，是"SIGN_IN"还是"SIGN_OUT"
     * @param appointment 预约实体，用来填充访客信息
     * @param idCard 解密后的身份证号，用来填充访客信息
     * @param log 通行记录，用来填充操作时间和类型
     * 
     * 【返回值】
     * @return SecurityScanVerifyResponseDTO，前端根据这个显示成功页面
     * 
     * 【注意事项】
     * - 这个方法只是构建响应，不写数据库（写数据库在confirmScanAction()里）
     * - 身份证号已经解密，可以直接展示
     */
    private SecurityScanVerifyResponseDTO success(String action, Appointment appointment, String idCard, AccessLog log) {
        SecurityScanVerifyResponseDTO ok = new SecurityScanVerifyResponseDTO();
        ok.setPassed(true);
        ok.setPendingConfirm(false);
        ok.setActionType(action);
        ok.setAppointmentId(appointment.getAppointmentId());
        ok.setVisitorId(appointment.getVisitorId());
        ok.setVisitorName(appointment.getVisitorName());
        ok.setVisitorPhone(appointment.getVisitorPhone());
        ok.setVisitorIdCard(idCard);
        ok.setAccessType(log.getAccessType());
        ok.setVerifyMethod(log.getVerifyMethod());
        ok.setAccessTime(log.getAccessTime());
        return ok;
    }

    /**
     * 填充访客信息到响应DTO
     * 
     * 【业务背景】
     * 扫码核验成功或失败时，需要把访客信息填充到响应DTO里。
     * 这个方法就是干这个的，避免重复代码。
     * 
     * 【实现逻辑】
     * 1. 设置appointmentId、visitorId、visitorName
     * 2. 设置visitorPhone、visitorIdCard
     * 
     * 【参数说明】
     * @param dto 响应DTO，要填充的对象
     * @param appointment 预约实体，访客信息从这里取
     * @param idCard 解密后的身份证号，直接设置
     * 
     * 【注意事项】
     * - 这个方法只是填充数据，不写数据库
     * - 身份证号已经解密，可以直接展示
     */
    private void fillVisitor(SecurityScanVerifyResponseDTO dto, Appointment appointment, String idCard) {
        dto.setAppointmentId(appointment.getAppointmentId());
        dto.setVisitorId(appointment.getVisitorId());
        dto.setVisitorName(appointment.getVisitorName());
        dto.setVisitorPhone(appointment.getVisitorPhone());
        dto.setVisitorIdCard(idCard);
    }

    /**
     * 【功能】关联预约的手动登记
     * 
     * 【业务背景】
     * 访客二维码过期、手机没电、或者不想用二维码，
     * 安保人员可以手动输入预约ID，完成签到/签离。
     * 这里要核验身份证（如果提供了），保证"人证合一"。
     * 
     * 【实现逻辑】
     * 1. 参数校验：预约ID不能为空，空的话直接报错
     * 2. 查预约表，预约不存在直接抛异常
     * 3. 身份证核验（如果提供了）：
     *    - 解密预约里的身份证号
     *    - 和前端传过来的身份证号比对，不一样就报错
     * 4. 根据accessType分支处理：
     *    - 1（签到）：
     *        * 状态校验：必须是1（预约成功），不然不让签
     *        * 插入access_log（access_type=1，verify_method=2）
     *        * 更新预约状态为4
     *    - 2（签离）：
     *        * 状态校验：必须是4（已签到）或6（已过期），不然不让签
     *        * 签到记录校验：必须有签到记录，不然不让签
     *        * 已签离校验：必须未签离，不然不让签
     *        * 时间校验（非紧急登记）：必须超过宽限期，不然不让签
     *        * 插入access_log（access_type=2，verify_method=2）
     *        * 更新预约状态为5，清除告警
     * 5. 返回通行记录详情
     * 
     * 【参数说明】
     * @param request 手动登记请求DTO，包含：
     *         - appointmentId：预约ID（必填）
     *         - accessType：通行类型（1-签到，2-签离）
     *         - visitorIdCard：身份证号（可选，核验用）
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityAccessRecordDTO，包含通行记录详情
     * 
     * 【异常情况】
     * @throws BusinessException 预约不存在、状态不允许、身份证核验不通过等
     * 
     * 【注意事项】
     * - 身份证核验是可选的，不提供就不核验
     * - 签离需要超过宽限期（紧急登记除外）
     * - 这里加了@Transactional，保证数据一致性
     */
    private SecurityAccessRecordDTO manualForAppointment(SecurityManualRegisterRequestDTO request, Integer securityUserId) {
        // 1. 参数校验
        if (request.getAppointmentId() == null) {
            throw new BusinessException("请填写关联预约ID");
        }

        Appointment appointment = appointmentMapper.findById(request.getAppointmentId());
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }

        // 2. 身份证核验（如果提供了）
        String decryptedId = AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil);
        if (request.getVisitorIdCard() != null && !request.getVisitorIdCard().isBlank() && !request.getVisitorIdCard().equals(decryptedId)) {
            throw new BusinessException("身份证核验不通过");
        }

        // 3. 签离时的额外校验
        if (request.getAccessType() == 2) {
            if (appointment.getStatus() != 4 && appointment.getStatus() != 6) {
                throw new BusinessException("仅已签到或已过期访客可手动签离");
            }
            if (accessLogMapper.countByAppointmentAndType(appointment.getAppointmentId(), 1) == 0) {
                throw new BusinessException("该预约无签到记录，无法手动签离");
            }
            if (accessLogMapper.countByAppointmentAndType(appointment.getAppointmentId(), 2) > 0) {
                throw new BusinessException("该访客已签离");
            }
            if (!LocalDateTime.now().isAfter(AppointmentUtil.getGraceExpireTime(appointment))) {
                throw new BusinessException("未超过预计离开时间30分钟宽限，暂不支持手动签离");
            }
        }

        // 4. 插入通行记录
        AccessLog log = new AccessLog();
        log.setAppointmentId(appointment.getAppointmentId());
        log.setVisitorId(appointment.getVisitorId());
        log.setSecurityId(securityUserId.longValue());
        log.setAccessType(request.getAccessType());
        log.setAccessTime(LocalDateTime.now());
        log.setVerifyMethod(2);  // 2-手动
        accessLogMapper.insert(log);

        // 5. 更新预约状态
        if (request.getAccessType() == 1) {
            // 签到：更新status=4
            appointmentMapper.updateStatus(appointment.getAppointmentId(), 4);
        } else {
            // 签离：更新status=5，清除告警
            appointmentMapper.updateStatus(appointment.getAppointmentId(), 5);
            securityAlertCenter.clearByAppointmentId(appointment.getAppointmentId());
        }

        return accessLogMapper.findRecordDetail(log.getLogId());
    }

    /**
     * 【功能】紧急手动登记
     * 
     * 【业务背景】
     * 访客没预约，但急需入校（比如领导临时到访），
     * 安保人员可以手动登记，但需要审批人授权。
     * 这个场景叫"紧急登记"，预约表里会写一条特殊记录（interviewee_name='紧急手动登记'）。
     * 
     * 【实现逻辑】
     * 1. 校验授权审批人：
     *    - 调userService.findById()
     *    - 必须是userType=2（审批人）或4（管理员）
     * 2. 校验访客信息：
     *    - 手机号：必填，11位，正则校验
     *    - 姓名：必填
     *    - 身份证号：必填，18位，正则校验
     *    - 来访事由：必填，最多500字
     * 3. 创建或更新访客用户：
     *    - 调userService.findByPhone()
     *    - 查到了就更新realName（如果为空）
     *    - 查不到就新建（userType=1）
     * 4. 创建紧急预约记录：
     *    - 调appointmentMapper.insert()
     *    - interviewee_name设为"紧急手动登记"（标记用）
     *    - 时间设为当前时间（占位用，不参与过期判定）
     *    - 身份证号要加密（aesEncryptUtil.encrypt()）
     * 5. 设置审批人为授权审批人，更新状态为4（已签到）
     * 6. 插入签到记录（access_type=1，verify_method=2）
     * 7. 记录授权审批人ID（log.setAuthorizerId()）
     * 8. 发布紧急登记告警（securityAlertCenter.publish()）
     * 9. 返回记录详情
     * 
     * 【参数说明】
     * @param request 手动登记请求DTO，包含：
     *         - authorizerId：授权审批人ID（必填）
     *         - visitorPhone：访客手机号（必填）
     *         - visitorName：访客姓名（必填）
     *         - visitorIdCard：访客身份证号（必填）
     *         - visitReason：来访事由（必填）
     * @param securityUserId 安保人员用户ID，从JWT里取
     * 
     * 【返回值】
     * @return SecurityAccessRecordDTO，包含通行记录详情
     * 
     * 【异常情况】
     * @throws BusinessException 授权审批人不存在、访客信息不合法等
     * 
     * 【注意事项】
     * - 紧急登记不受宽限期限制（getGraceExpireTime()返回100年后）
     * - 紧急登记必须授权审批人同意，不然不让登
     * - 身份证号要加密存储（aesEncryptUtil.encrypt()）
     * - 这里加了@Transactional，保证数据一致性
     * - 紧急登记会推送告警通知管理员
     */
    private SecurityAccessRecordDTO manualEmergency(SecurityManualRegisterRequestDTO request, Integer securityUserId) {
        // 1. 校验授权审批人
        if (request.getAuthorizerId() == null) {
            throw new BusinessException("紧急登记必须填写授权审批人ID");
        }

        User authorizer = userService.findById(request.getAuthorizerId().intValue());
        if (authorizer == null || (authorizer.getUserType() != 2 && authorizer.getUserType() != 4)) {
            throw new BusinessException("授权审批人不存在或无审批权限");
        }

        // 2. 紧急登记仅支持签到
        if (request.getAccessType() != 1) {
            throw new BusinessException("无预约紧急登记仅支持手动签到");
        }

        // 3. 校验访客信息
        String visitorPhone = AppointmentUtil.normalizeText(request.getVisitorPhone());
        String visitorName = AppointmentUtil.normalizeText(request.getVisitorName());
        String visitorIdCard = AppointmentUtil.normalizeText(request.getVisitorIdCard());
        String visitReason = AppointmentUtil.normalizeText(request.getVisitReason());

        if (visitorPhone == null) {
            throw new BusinessException("请填写访客手机号");
        }
        AppointmentUtil.validatePhone(visitorPhone);
        if (visitorName == null) {
            throw new BusinessException("请填写访客姓名");
        }
        if (visitorIdCard == null) {
            throw new BusinessException("请填写身份证号码");
        }
        AppointmentUtil.validateIdCard(visitorIdCard);
        if (visitReason == null) {
            throw new BusinessException("请填写来访事由");
        }
        if (visitReason.length() > 500) {
            throw new BusinessException("来访事由最多500字");
        }

        // 4. 创建或更新访客用户
        User visitor = userService.findByPhone(visitorPhone);
        if (visitor == null) {
            visitor = new User();
            visitor.setPhone(visitorPhone);
            visitor.setRealName(visitorName);
            visitor.setUserType(1);  // 访客类型
            userService.insert(visitor);
        } else {
            if (visitor.getUserType() != 1) {
                throw new BusinessException("该手机号已绑定非访客账号，请更换手机号");
            }
            if (visitor.getRealName() == null || visitor.getRealName().isBlank()) {
                visitor.setRealName(visitorName);
                userService.update(visitor);
            }
        }

        // 5. 创建紧急预约记录
        Appointment emergencyAppointment = new Appointment();
        LocalDateTime signInTime = LocalDateTime.now();
        emergencyAppointment.setVisitorId(visitor.getUserId().longValue());
        emergencyAppointment.setVisitorName(visitorName);
        emergencyAppointment.setVisitorPhone(visitorPhone);
        emergencyAppointment.setIntervieweeName(AppointmentUtil.EMERGENCY_INTERVIEWEE_NAME);  // 标记为紧急登记
        emergencyAppointment.setVisitReason(visitReason);
        // 数据库字段非空，紧急登记写入占位时间；业务上通过 isEmergencyAppointment 忽略过期判定
        emergencyAppointment.setExpectedStartTime(signInTime);
        emergencyAppointment.setExpectedEndTime(signInTime);
        String formattedIdCard = AppointmentUtil.formatIdCard(visitorIdCard);
        String encryptedIdCard = AppointmentUtil.encryptIdCard(formattedIdCard, aesEncryptUtil);
        emergencyAppointment.setVisitorIdCard(encryptedIdCard);
        appointmentMapper.insert(emergencyAppointment);
        // 设置审批人为授权审批人
        appointmentMapper.updateApprover(emergencyAppointment.getAppointmentId(), request.getAuthorizerId().intValue());
        appointmentMapper.updateProcessTime(emergencyAppointment.getAppointmentId(), signInTime);
        appointmentMapper.updateStatus(emergencyAppointment.getAppointmentId(), 4);  // 状态=4（已签到）
        emergencyAppointment.setStatus(4);
        emergencyAppointment.setApproverId(request.getAuthorizerId());

        // 6. 插入签到记录
        AccessLog log = new AccessLog();
        log.setAppointmentId(emergencyAppointment.getAppointmentId());
        log.setVisitorId(visitor.getUserId().longValue());
        log.setSecurityId(securityUserId.longValue());
        log.setAccessType(1);  // 1-签到
        log.setAccessTime(signInTime);
        log.setVerifyMethod(2);  // 2-手动
        log.setAuthorizerId(request.getAuthorizerId());  // 记录授权审批人
        accessLogMapper.insert(log);

        // 7. 发布紧急登记告警
        String authorizerName = authorizer.getRealName() == null || authorizer.getRealName().isBlank()
            ? String.valueOf(authorizer.getUserId())
            : authorizer.getRealName();
        String emergencyMessage = "紧急登记访客已手动签到，请离校时手动签离"
            + "（手机号：" + visitorPhone + "，授权人：" + authorizerName + "，来访事由：" + visitReason + "）";
        securityAlertCenter.publish(
            EMERGENCY_ALERT_TYPE,
            emergencyMessage,
            emergencyAppointment,
            true,  // canManualSignOut = true
            null,
            formattedIdCard,
            visitReason,
            signInTime
        );

        // 8. 返回记录详情
        SecurityAccessRecordDTO dto = accessLogMapper.findRecordDetail(log.getLogId());
        if (dto != null) {
            dto.setVisitorName(visitorName);
            dto.setVisitorPhone(visitorPhone);
            dto.setVisitReason(visitReason);
            dto.setEmergency(true);  // 标记为紧急登记
        }
        return dto;
    }

    /**
     * 查找预约的首次签到时间
     * 
     * 【业务背景】
     * 计算停留时长、判断超时，都需要首次签到时间。
     * 这个方法就是查这个的，避免重复代码。
     * 
     * 【实现逻辑】
     * 1. 如果appointmentId为null，返回null
     * 2. 调accessLogMapper.findFirstAccessTimeByAppointmentAndType()
     *    - 查access_type=1（签到）的第一条记录
     *    - 按access_time升序，取第一条
     * 3. 返回首次签到时间（LocalDateTime），没找到返回null
     * 
     * 【参数说明】
     * @param appointmentId 预约ID，从Appointment对象里取
     * 
     * 【返回值】
     * @return 首次签到时间（LocalDateTime），没找到返回null
     * 
     * 【注意事项】
     * - 这个方法只是查询，不写数据库
     * - 返回null的话，上层计算停留时长要兜底
     */
    private LocalDateTime findSignInTime(Long appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        return accessLogMapper.findFirstAccessTimeByAppointmentAndType(appointmentId, 1);  // 1-签到
    }

    /**
     * 构建紧急登记告警DTO
     * 
     * 【业务背景】
     * 紧急登记的预约，要作为告警展示给安保人员。
     * 这个方法就是构建这个告警DTO的，避免重复代码。
     * 
     * 【实现逻辑】
     * 1. 创建SecurityAlertDTO对象
     * 2. 设置alertType="EMERGENCY_REGISTERED"
     * 3. 设置message（固定文案）
     * 4. 设置appointmentId、visitorName、visitorPhone、visitorIdCard
     * 5. 设置visitReason、signInTime、canManualSignOut、appointmentStatus
     * 6. 设置occurredAt（发生时间，用signInTime兜底）
     * 7. 返回DTO
     * 
     * 【参数说明】
     * @param appointment 紧急登记预约实体，从数据库查出来的
     * 
     * 【返回值】
     * @return SecurityAlertDTO，用来展示在告警列表里
     * 
     * 【注意事项】
     * - 这个方法只是构建DTO，不写数据库
     * - canManualSignOut设为true，紧急登记随时可以签离
     * - 身份证号已经解密，可以直接展示
     */
    private SecurityAlertDTO buildEmergencyAlert(Appointment appointment) {
        SecurityAlertDTO dto = new SecurityAlertDTO();
        LocalDateTime signInTime = findSignInTime(appointment.getAppointmentId());
        dto.setAlertType(EMERGENCY_ALERT_TYPE);  // "EMERGENCY_REGISTERED"
        dto.setMessage("紧急登记访客已手动签到，请离校时手动签离");
        dto.setAppointmentId(appointment.getAppointmentId());
        dto.setVisitorName(appointment.getVisitorName());
        dto.setVisitorPhone(appointment.getVisitorPhone());
        dto.setVisitorIdCard(AppointmentUtil.decryptIdCard(appointment.getVisitorIdCard(), aesEncryptUtil));
        dto.setVisitReason(appointment.getVisitReason());
        dto.setSignInTime(signInTime);
        dto.setCanManualSignOut(true);  // 可以手动签离
        dto.setAppointmentStatus(appointment.getStatus());
        dto.setOccurredAt(signInTime != null ? signInTime : LocalDateTime.now());
        return dto;
    }

    /**
     * 【功能】管理员查询通行记录列表（无需权限校验）
     * 
     * 【业务背景】
     * 管理员在Web后台查看通行记录，支持多条件筛选。
     * 这个方法不用校验权限（管理员已经登录了）。
     * 
     * 【实现逻辑】
     * 1. 调accessLogMapper.listRecords()
     * 2. 遍历结果，调mergeRecordData()合并数据
     * 3. 返回通行记录DTO列表
     * 
     * 【参数说明】
     * @param startDate 开始日期（yyyy-MM-dd，可选），为空则查所有
     * @param endDate 结束日期（yyyy-MM-dd，可选），为空则查所有
     * @param keyword 关键词（访客姓名/手机号/安保姓名，可选），模糊匹配
     * @param accessType 通行类型（1-签到，2-签离，可选），为空则查所有
     * @param verifyMethod 核验方式（1-扫码，2-手动，可选），为空则查所有
     * @param emergencyOnly 是否仅查询紧急登记（可选），true则只查紧急登记
     *
     * 【返回值】
     * @return List<SecurityAccessRecordDTO>，数据已合并和解密
     * 
     * 【注意事项】
     * - 这个方法不用校验权限（管理员已经登录了）
     * - 数据可能来自两张表，需要合并展示
     * - 身份证号已解密，可直接展示
     */
    public List<SecurityAccessRecordDTO> listRecordsForAdmin(String startDate, String endDate,
                                                           String keyword, Integer accessType,
                                                           Integer verifyMethod, Boolean emergencyOnly) {
        // 查询记录
        List<SecurityAccessRecordDTO> list = accessLogMapper.listRecords(startDate, endDate, keyword, accessType, verifyMethod, emergencyOnly);
        
        for (SecurityAccessRecordDTO item : list) {
            AppointmentUtil.mergeRecordData(item, aesEncryptUtil);
        }
        return list;
    }

    /**
     * 【功能】管理员查询通行记录详情（无需权限校验）
     * 
     * 【业务背景】
     * 管理员在Web后台查看通行记录详情。
     * 这个方法不用校验权限（管理员已经登录了）。
     * 
     * 【实现逻辑】
     * 1. 调accessLogMapper.findRecordDetail()
     * 2. 如果为null，抛异常
     * 3. 调mergeRecordData()合并数据
     * 4. 返回通行记录详情DTO
     * 
     * 【参数说明】
     * @param logId 通行记录ID，从前端传过来
     * 
     * 【返回值】
     * @return SecurityAccessRecordDTO，数据已合并和解密
     * 
     * 【异常情况】
     * @throws BusinessException 记录不存在
     * 
     * 【注意事项】
     * - 这个方法不用校验权限（管理员已经登录了）
     * - 身份证号解密失败会显示为***，不会抛异常
     */
    public SecurityAccessRecordDTO getRecordDetailForAdmin(Long logId) {
        SecurityAccessRecordDTO detail = accessLogMapper.findRecordDetail(logId);
        if (detail == null) {
            throw new BusinessException("记录不存在");
        }
        AppointmentUtil.mergeRecordData(detail, aesEncryptUtil);
        return detail;
    }

    /**
     * 【功能】删除通行记录
     * 
     * 【业务背景】
     * 管理员在Web后台删除通行记录，只能删关联预约已完成的。
     * 这个是硬删除，会真把数据删掉，要谨慎。
     * 
     * 【实现逻辑】
     * 1. 调accessLogMapper.findRecordDetail()查记录
     * 2. 如果为null，抛异常（记录不存在）
     * 3. 判断appointmentStatus是不是5（已完成），不是就抛异常
     * 4. 调accessLogMapper.deleteById()删除
     * 
     * 【参数说明】
     * @param logId 通行记录ID，从前端传过来
     * 
     * 【异常情况】
     * @throws BusinessException 记录不存在、不允许删除
     * 
     * 【注意事项】
     * - 只能删关联预约已完成的（appointmentStatus=5）
     * - 这个是硬删除，会真把数据删掉，要谨慎
     * - 这里加了@Transactional，保证数据一致性
     */
    @Transactional
    public void deleteAccessRecord(Long logId) {
        SecurityAccessRecordDTO record = accessLogMapper.findRecordDetail(logId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        if (!Integer.valueOf(5).equals(record.getAppointmentStatus())) {
            throw new BusinessException("仅允许删除关联预约已完成的通行记录");
        }
        accessLogMapper.deleteById(logId);
    }

    /**
     * 【功能】批量删除通行记录
     * 
     * 【业务背景】
     * 管理员在Web后台批量删除通行记录，只能删关联预约已完成的。
     * 这个是硬删除，会真把数据删掉，要谨慎。
     * 
     * 【实现逻辑】
     * 1. 参数校验：logIds不能为空，空的话直接报错
     * 2. 去重（logIds.stream().distinct().toList()）
     * 3. 遍历logIds，逐个校验：
     *    - 调accessLogMapper.findRecordDetail()查记录
     *    - 如果为null，抛异常（记录不存在）
     *    - 如果appointmentStatus不是5，抛异常（不允许删除）
     * 4. 调accessLogMapper.deleteByIds()批量删除
     * 
     * 【参数说明】
     * @param logIds 通行记录ID列表，从前端传过来
     * 
     * 【异常情况】
     * @throws BusinessException 记录不存在、不允许删除
     * 
     * 【注意事项】
     * - 只能删关联预约已完成的（appointmentStatus=5）
     * - 这个是硬删除，会真把数据删掉，要谨慎
     * - 这里加了@Transactional，保证数据一致性
     */
    @Transactional
    public void batchDeleteAccessRecords(List<Long> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        List<Long> distinctIds = logIds.stream().distinct().toList();
        for (Long id : distinctIds) {
            SecurityAccessRecordDTO record = accessLogMapper.findRecordDetail(id);
            if (record == null) {
                throw new BusinessException("记录不存在：" + id);
            }
            if (!Integer.valueOf(5).equals(record.getAppointmentStatus())) {
                throw new BusinessException("存在不允许删除的记录（ID：" + id + "），仅允许删除关联预约已完成的通行记录");
            }
        }
        accessLogMapper.deleteByIds(distinctIds);
    }
}