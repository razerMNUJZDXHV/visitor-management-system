package com.gpj.visitorsystem.util;

import com.gpj.visitorsystem.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 【通用模块】日期时间格式化工具类
 *
 * 【核心职责】
 * 1. 提供日期格式化的通用方法（yyyy-MM-dd）
 * 2. 提供时间格式化的通用方法（HH:mm）
 * 3. 提供日期时间格式化的通用方法（yyyy-MM-dd HH:mm）
 * 4. 提供获取当前日期字符串的通用方法
 * 5. 提供日期时间解析的通用方法
 *
 * 【关键业务场景】
 * 多个Service中存在重复的日期时间格式化逻辑，
 * 提取到此类中统一维护，避免代码重复。
 *
 * 【依赖说明】
 * - BusinessException：业务异常，解析失败时抛出
 *
 * 【注意事项】
 * - 本类为静态工具类，无需注入，直接通过类名调用
 * - 格式化方法对null值返回null，避免空指针异常
 * - 解析方法失败时抛出BusinessException，由调用方处理
 */
public class DateTimeUtil {

    /** 默认日期格式：yyyy-MM-dd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 默认时间格式：HH:mm */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 默认日期时间格式：yyyy-MM-dd HH:mm */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 格式化日期为字符串（使用默认格式 yyyy-MM-dd）
     *
     * 【业务背景】
     * 将LocalDate格式化为字符串，用于存储或传输。
     * 这个方法统一格式化逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 如果date为null，返回null
     * 2. 使用DATE_FORMATTER格式化
     *
     * 【参数说明】
     * @param date 日期
     *
     * 【返回值】
     * @return 格式化后的字符串，null 时返回 null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化时间为字符串（使用默认格式 HH:mm）
     *
     * 【业务背景】
     * 将LocalTime格式化为字符串，用于存储或传输。
     * 这个方法统一格式化逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 如果time为null，返回null
     * 2. 使用TIME_FORMATTER格式化
     *
     * 【参数说明】
     * @param time 时间
     *
     * 【返回值】
     * @return 格式化后的字符串，null 时返回 null
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * 格式化日期时间为字符串（使用默认格式 yyyy-MM-dd HH:mm）
     *
     * 【业务背景】
     * 将LocalDateTime格式化为字符串，用于存储或传输。
     * 这个方法统一格式化逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 如果dateTime为null，返回null
     * 2. 使用DATE_TIME_FORMATTER格式化
     *
     * 【参数说明】
     * @param dateTime 日期时间
     *
     * 【返回值】
     * @return 格式化后的字符串，null 时返回 null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取当前日期的字符串表示（格式：yyyy-MM-dd）
     *
     * 【业务背景】
     * 获取当前日期的字符串表示，用于查询、统计等场景。
     * 这个方法统一获取逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 获取当前日期
     * 2. 使用DATE_FORMATTER格式化
     *
     * 【参数说明】
     * 无
     *
     * 【返回值】
     * @return 当前日期的字符串表示（格式：yyyy-MM-dd）
     */
    public static String getCurrentDateStr() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 解析日期字符串为LocalDate（使用默认格式 yyyy-MM-dd）
     *
     * 【业务背景】
     * 将日期字符串解析为LocalDate，用于读取配置、参数解析等场景。
     * 这个方法统一解析逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 使用DATE_FORMATTER解析
     * 2. 解析失败抛出BusinessException
     *
     * 【参数说明】
     * @param dateStr 日期字符串（格式：yyyy-MM-dd）
     *
     * 【返回值】
     * @return 解析后的LocalDate
     *
     * 【异常情况】
     * @throws BusinessException 格式不正确时抛出
     */
    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception ex) {
            throw new BusinessException("日期格式不正确，请使用yyyy-MM-dd");
        }
    }

    /**
     * 解析时间字符串为LocalTime（使用默认格式 HH:mm）
     *
     * 【业务背景】
     * 将时间字符串解析为LocalTime，用于读取配置、参数解析等场景。
     * 这个方法统一解析逻辑，避免重复代码。
     *
     * 【实现逻辑】
     * 1. 使用TIME_FORMATTER解析
     * 2. 解析失败抛出BusinessException
     *
     * 【参数说明】
     * @param timeStr 时间字符串（格式：HH:mm）
     *
     * 【返回值】
     * @return 解析后的LocalTime
     *
     * 【异常情况】
     * @throws BusinessException 格式不正确时抛出
     */
    public static LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception ex) {
            throw new BusinessException("时间格式不正确，请使用HH:mm");
        }
    }
}
