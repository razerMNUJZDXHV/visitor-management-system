package com.gpj.visitorsystem.util;

import com.gpj.visitorsystem.dto.admin.AdminAppointmentDetailDTO;
import com.gpj.visitorsystem.dto.wx.security.SecurityAccessRecordDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 【业务模块】预约通用工具类
 *
 * 【核心职责】
 * 1. 提供预约状态判断的通用方法（如过期判定）
 * 2. 提供时间格式化通用方法
 * 3. 提供预约状态文本转换
 * 4. 提供身份证号解密通用方法
 * 5. 提供通行记录数据合并通用方法
 *
 * 【关键业务场景】
 * 多个Service中存在重复的预约相关工具方法，
 * 提取到此类中统一维护，避免代码重复。
 *
 * 【依赖说明】
 * - AesEncryptUtil：身份证号解密
 *
 * 【注意事项】
 * - 本类为静态工具类，无需注入，直接通过类名调用
 * - 身份证号解密失败返回"***"，不抛异常
 */
public class AppointmentUtil {

    /** 二维码宽限期（分钟）：预计离开时间后30分钟内二维码仍有效 */
    public static final long QR_GRACE_MINUTES = 30L;

    /** 手机号正则：1[3-9]开头，共11位 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 身份证号正则：18位 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    /**
     * 校验手机号格式
     *
     * 【业务背景】
     * 手机号必须是11位，1[3-9]开头。
     * 这个方法用于手动登记、访客注册等场景。
     *
     * @param phone 手机号
     * @throws BusinessException 格式错误时抛出
     */
    public static void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("请输入有效的11位手机号");
        }
    }

    /**
     * 校验身份证号格式
     *
     * 【业务背景】
     * 身份证号必须是18位，最后一位可以是X。
     * 这个方法用于手动登记、访客预约等场景。
     *
     * @param idCard 身份证号
     * @throws BusinessException 格式错误时抛出
     */
    public static void validateIdCard(String idCard) {
        if (idCard == null || !ID_CARD_PATTERN.matcher(idCard).matches()) {
            throw new BusinessException("请输入有效的18位身份证号");
        }
    }

    /**
     * 格式化身份证号（去空格、转大写）
     *
     * 【业务背景】
     * 身份证号可能包含空格，最后一位可能是小写x。
     * 这个方法统一格式化，避免重复代码。
     *
     * @param idCard 原始身份证号
     * @return 格式化后的身份证号（去空格、转大写）
     */
    public static String formatIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        return idCard.trim().toUpperCase();
    }

    /**
     * 加密身份证号（包含异常处理）
     *
     * 【业务背景】
     * 身份证号需要AES加密后存储。
     * 这个方法统一加密逻辑和异常处理，避免重复代码。
     *
     * @param idCard 原始身份证号
     * @param aesEncryptUtil AES加密工具
     * @return 加密后的身份证号
     * @throws BusinessException 加密失败时抛出
     */
    public static String encryptIdCard(String idCard, AesEncryptUtil aesEncryptUtil) {
        try {
            return aesEncryptUtil.encrypt(idCard);
        } catch (Exception e) {
            throw new BusinessException("身份证号加密失败");
        }
    }

    /**
     * 标准化文本输入
     * 
     * 【业务背景】
     * 前端传过来的文本，可能有空格、空字符串，要统一处理。
     * 这个方法就是干这个的，避免重复代码。
     * 
     * 【实现逻辑】
     * 1. 如果value为null，返回null
     * 2. 去除首尾空格（trim()）
     * 3. 如果去除后是空字符串，返回null；否则返回去除后的字符串
     * 
     * 【参数说明】
     * @param value 原始文本，从前端传过来
     * 
     * 【返回值】
     * @return 标准化后的文本，空则返回null
     * 
     * 【注意事项】
     * - 这个方法只是处理字符串，不写数据库
     * - 返回null的话，上层会抛异常（因为必填字段不能为空）
     */
    public static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 判断待审批预约是否已过期
     *
     * 【业务背景】
     * 待审批预约（status=0）的过期条件是：
     * 当前时间已超过预计离开时间。
     *
     * @param expectedEndTime 预计离开时间
     * @param now 当前时间
     * @return 是否已过期
     */
    public static boolean isPendingExpired(LocalDateTime expectedEndTime, LocalDateTime now) {
        return expectedEndTime != null && !expectedEndTime.isAfter(now);
    }

    /**
     * 格式化日期时间为字符串（使用默认格式 yyyy-MM-dd HH:mm）
     * 
     * 【业务背景】
     * 导出等场景中，null值需要显示为"—"。
     * 这个方法统一处理null值，避免重复代码。
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串，null 时返回 "—"
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        String result = DateTimeUtil.formatDateTime(dateTime);
        return result == null ? "—" : result;
    }

    /**
     * 格式化日期时间为字符串（使用指定格式）
     *
     * @param dateTime 日期时间
     * @param formatter 格式器
     * @return 格式化后的字符串，null 时返回空字符串
     */
    public static String formatDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(formatter);
    }

    /**
     * 获取预约状态文本（简单版）
     *
     * 【业务背景】
     * 将状态码转换为中文描述，用于导出等场景。
     * status=6 统一返回"已过期"，不区分是否已处理。
     *
     * @param status 状态码
     * @return 状态中文描述
     */
    public static String getStatusText(Integer status) {
        if (status == null) {
            return "—";
        }
        switch (status) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            case 4: return "已签到";
            case 5: return "已完成";
            case 6: return "已过期";
            default: return "未知";
        }
    }

    /**
     * 获取预约状态文本（详细版，用于管理端审批列表）
     *
     * 【业务背景】
     * 管理端审批列表中，status=6 需要区分"已处理已过期"和"未处理已过期"。
     * 区分依据：processTime 或 approverId 不为空表示已处理。
     *
     * @param detail 预约详情DTO
     * @return 状态中文描述
     */
    public static String getStatusText(AdminAppointmentDetailDTO detail) {
        if (detail == null || detail.getStatus() == null) {
            return "未知";
        }
        int status = detail.getStatus();
        if (status == 6) {
            boolean processed = detail.getProcessTime() != null || detail.getApproverId() != null;
            return processed ? "已处理已过期" : "未处理已过期";
        }
        return getStatusText(status);
    }

    /**
     * 解密身份证号（字符串版）
     *
     * 【业务背景】
     * 对加密的身份证号进行解密，解密失败返回"***"。
     *
     * @param encrypted 加密后的身份证号
     * @param aesEncryptUtil AES加解密工具
     * @return 解密后的身份证号，或占位符
     */
    public static String decryptIdCard(String encrypted, AesEncryptUtil aesEncryptUtil) {
        if (encrypted == null || encrypted.isBlank()) {
            return "—";
        }
        try {
            return aesEncryptUtil.decrypt(encrypted);
        } catch (Exception ignored) {
            return "***";
        }
    }

    /**
     * 解密预约实体中的身份证号
     *
     * @param appointment 预约实体
     * @param aesEncryptUtil AES加解密工具
     */
    public static void decryptIdCard(Appointment appointment, AesEncryptUtil aesEncryptUtil) {
        if (appointment == null) {
            return;
        }
        try {
            if (appointment.getVisitorIdCard() != null) {
                appointment.setVisitorIdCard(aesEncryptUtil.decrypt(appointment.getVisitorIdCard()));
            }
        } catch (Exception ignored) {
            appointment.setVisitorIdCard("***");
        }
    }

    /**
     * 解密预约详情DTO中的身份证号
     *
     * @param detail 预约详情DTO
     * @param aesEncryptUtil AES加解密工具
     */
    public static void decryptIdCard(AdminAppointmentDetailDTO detail, AesEncryptUtil aesEncryptUtil) {
        if (detail == null) {
            return;
        }
        try {
            if (detail.getVisitorIdCard() != null) {
                detail.setVisitorIdCard(aesEncryptUtil.decrypt(detail.getVisitorIdCard()));
            }
        } catch (Exception ignored) {
            detail.setVisitorIdCard("***");
        }
    }

    /**
     * 合并通行记录数据（填充备用字段、解密身份证号）
     *
     * 【业务背景】
     * 通行记录中，visitorName/visitorPhone 可能为空（手动登记场景），
     * 需要用备用字段（visitorNameAlt/visitorPhoneAlt）填充。
     * 身份证号为AES加密，需要解密后展示。
     *
     * @param item 通行记录DTO
     * @param aesEncryptUtil AES加解密工具
     */
    public static void mergeRecordData(SecurityAccessRecordDTO item, AesEncryptUtil aesEncryptUtil) {
        if (item == null) {
            return;
        }
        if (item.getVisitorName() == null || item.getVisitorName().isEmpty()) {
            item.setVisitorName(item.getVisitorNameAlt());
        }
        if (item.getVisitorPhone() == null || item.getVisitorPhone().isEmpty()) {
            item.setVisitorPhone(item.getVisitorPhoneAlt());
        }
        if (item.getVisitorIdCard() != null && !item.getVisitorIdCard().isEmpty()) {
            item.setVisitorIdCard(decryptIdCard(item.getVisitorIdCard(), aesEncryptUtil));
        }
    }

    /** 紧急登记标识（interviewee_name字段的值） */
    public static final String EMERGENCY_INTERVIEWEE_NAME = "紧急手动登记";

    /**
     * 判断是否为紧急登记预约
     *
     * 【业务背景】
     * 紧急登记的预约有两种标记方式：
     * 1. intervieweeName = "紧急手动登记"（安保端手动登记）
     * 2. visitReason 以 "【紧急登记】" 开头（访客端紧急预约）
     * 这类预约永不过期，只能手动签离。
     *
     * @param appointment 预约实体
     * @return 是否为紧急登记
     */
    public static boolean isEmergencyAppointment(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        String intervieweeName = appointment.getIntervieweeName();
        String reason = appointment.getVisitReason();
        return (intervieweeName != null && EMERGENCY_INTERVIEWEE_NAME.equals(intervieweeName))
                || (reason != null && reason.startsWith("【紧急登记】"));
    }

    /**
     * 应用过期状态（修改传入的预约对象）
     *
     * 【业务背景】
     * 查询预约时，需要动态判断并应用过期状态：
     * 1. status=0（待审批）且 expectedEndTime 已过 → 设置为 6（已过期）
     * 2. status=1（已通过）或 status=4（已签到）且超过宽限期 → 设置为 6（已过期）
     *
     * 【实现逻辑】
     * 1. 如果appointment为null或status为null，直接返回
     * 2. 如果status=0，检查 expectedEndTime 是否已过
     * 3. 如果status=1或4，检查是否超过宽限期
     * 4. 满足条件则设置 status=6
     *
     * @param appointment 预约实体（会被修改）
     */
    public static void applyExpiredStatus(Appointment appointment) {
        if (appointment == null) {
            return;
        }
        Integer status = appointment.getStatus();
        if (status == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (status == 0) {
            LocalDateTime expectedEndTime = appointment.getExpectedEndTime();
            if (expectedEndTime != null && !expectedEndTime.isAfter(now)) {
                appointment.setStatus(6);
            }
            return;
        }
        if (status == 1 || status == 4) {
            if (now.isAfter(getGraceExpireTime(appointment))) {
                appointment.setStatus(6);
            }
        }
    }

    /**
     * 计算二维码宽限过期时间
     *
     * 【业务背景】
     * 普通预约：预计离开时间 + 30分钟宽限期
     * 紧急登记：永不过期（返回100年后）
     * 异常情况：返回当前时间（立即过期）
     *
     * @param appointment 预约实体
     * @return 宽限过期时间
     */
    public static LocalDateTime getGraceExpireTime(Appointment appointment) {
        if (appointment == null) {
            return LocalDateTime.now();
        }
        // 紧急登记：永不过期
        if (isEmergencyAppointment(appointment)) {
            return LocalDateTime.now().plusYears(100);
        }
        if (appointment.getExpectedEndTime() == null) {
            return LocalDateTime.now();
        }
        return appointment.getExpectedEndTime().plusMinutes(QR_GRACE_MINUTES);
    }
}
