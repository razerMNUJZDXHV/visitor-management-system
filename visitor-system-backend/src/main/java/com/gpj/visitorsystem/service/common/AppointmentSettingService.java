package com.gpj.visitorsystem.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpj.visitorsystem.entity.AppointmentOpenStatus;
import com.gpj.visitorsystem.entity.AppointmentSetting;
import com.gpj.visitorsystem.entity.AppointmentTimeRule;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.util.DateTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 【业务模块】预约设置管理
 *
 * 【核心职责】
 * 1. 读取与保存预约设置（公告、限额、时间规则）
 * 2. 校验时间规则合法性与重叠
 * 3. 判断指定时间是否开放预约
 *
 * 【关键业务场景】
 * 预约设置存储在JSON文件中，后台修改后立即生效，避免数据库迁移。
 *
 * 【依赖说明】
 * - ObjectMapper：JSON读写
 * - AppointmentSetting/AppointmentTimeRule：设置实体
 *
 * 【注意事项】
 * - 文件读写需要同步，避免并发写入
 * - 规则重叠或日期时间格式不合法需抛出业务异常
 */
@Service
public class AppointmentSettingService {

    private static final String DEFAULT_NOTICE = "";
    // JSON文件存在项目根目录下的data/appointment-setting.json
    private static final String DATA_FILE_RELATIVE_PATH = "data/appointment-setting.json";

    private final ObjectMapper objectMapper;
    private final Path storagePath;  // JSON文件的路径

    /**
     * 构造预约设置服务
     *
     * 【业务逻辑】
     * 1. 注入ObjectMapper
     * 2. 初始化JSON存储路径
     *
     * 【参数说明】
     * @param objectMapper JSON序列化工具
     *
     * 【返回值】
     * @return 无
     */
    public AppointmentSettingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 存到项目运行目录下的data/appointment-setting.json
        this.storagePath = Paths.get(System.getProperty("user.dir"), DATA_FILE_RELATIVE_PATH);
    }
    
    /**
     * 获取当前预约设置
     *
     * 【业务逻辑】
     * 1. 读取JSON文件
     * 2. 文件不存在则创建默认配置
     * 3. 规范化配置并返回
     *
     * 【参数说明】
     * 无
     *
     * 【返回值】
     * @return 当前预约设置
     */
    public synchronized AppointmentSetting getCurrentSetting() {
        AppointmentSetting setting = readSettingFromFile();
        if (setting == null) {
            // 文件不存在，创建默认配置
            setting = createDefaultSetting();
            writeSettingToFile(setting);
            return setting;
        }
        // 规范化（校验规则、排序）
        return normalizeSetting(setting, false);
    }

    /**
     * 更新预约设置
     *
     * 【业务背景】
     * 管理端修改预约规则与公告后调用该方法保存。
     *
     * 【实现步骤】
     * 1. 校验并规范化配置
     * 2. 设置更新时间
     * 3. 写入JSON文件
     *
     * 【参数说明】
     * @param request 预约设置请求
     *
     * 【返回值】
     * @return 更新后的预约设置
     *
     * 【异常情况】
     * @throws BusinessException 规则重叠或格式校验失败
     *
     * 【事务说明】
     * 无（文件写入）
     */
    public synchronized AppointmentSetting updateSetting(AppointmentSetting request) {
        AppointmentSetting normalized = normalizeSetting(request, true);
        normalized.setUpdatedTime(LocalDateTime.now());
        writeSettingToFile(normalized);
        return normalized;
    }

    /**
     * 获取当前时刻的预约开放状态
     *
     * 【业务逻辑】
     * 1. 以当前时间计算开放状态
     * 2. 返回开放结果与提示信息
     *
     * 【参数说明】
     * 无
     *
     * 【返回值】
     * @return 预约开放状态
     */
    public AppointmentOpenStatus getCurrentOpenStatus() {
        return getOpenStatusByTime(LocalDateTime.now());
    }

    /**
     * 获取指定时刻的预约开放状态
     *
     * 【业务逻辑】
     * 1. 读取当前配置
     * 2. 判断指定时间是否开放
     * 3. 生成关闭提示信息
     *
     * 【参数说明】
     * @param targetTime 要查询的时间（通常是预约的预计到达时间）
     *
     * 【返回值】
     * @return 预约开放状态
     */
    public AppointmentOpenStatus getOpenStatusByTime(LocalDateTime targetTime) {
        AppointmentSetting setting = getCurrentSetting();
        AppointmentOpenStatus status = new AppointmentOpenStatus();
        boolean open = isOpenAt(setting, targetTime);
        status.setOpen(open);
        // 不开放时，生成提示信息（告诉用户哪个时段可以预约）
        status.setMessage(open ? "" : buildClosedMessage(setting, targetTime));
        return status;
    }

    /**
     * 断言预约是否开放，不开放则抛异常
     *
     * 【业务背景】
     * 访客提交预约时需校验开放时间，未开放直接阻断。
     *
     * 【实现步骤】
     * 1. 获取指定时间开放状态
     * 2. 未开放时抛出业务异常
     *
     * 【参数说明】
     * @param targetTime 预约时间
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 预约未开放
     *
     * 【事务说明】
     * 无
     */
    public void assertAppointmentOpen(LocalDateTime targetTime) {
        AppointmentOpenStatus status = getOpenStatusByTime(targetTime);
        if (!Boolean.TRUE.equals(status.getOpen())) {
            throw new BusinessException(status.getMessage());
        }
    }

    /**
     * 规范化设置数据（校验并格式化）
     * 
     * strict=true时，空字段直接抛异常；strict=false时，跳过空字段。
     * 会校验：日期格式、时间格式、日期先后、时间先后、规则是否重叠。
     */
    private AppointmentSetting normalizeSetting(AppointmentSetting raw, boolean strict) {
        AppointmentSetting normalized = new AppointmentSetting();
        
        // 处理公告文案（限制300字）
        String notice = raw == null ? null : raw.getNotice();
        if (notice != null && notice.trim().length() > 300) {
            throw new BusinessException("提示文案不能超过300个字符");
        }
        normalized.setNotice(notice != null ? notice.trim() : DEFAULT_NOTICE);

        // 每日限额（null表示不限制）
        Integer dailyLimit = raw == null ? null : raw.getDailyLimit();
        normalized.setDailyLimit(dailyLimit);

        // 处理时间规则
        List<AppointmentTimeRule> rules = new ArrayList<>();
        if (raw != null && raw.getRules() != null) {
            for (AppointmentTimeRule item : raw.getRules()) {
                if (item == null) {
                    continue;
                }
                // 校验必填字段
                if (!StringUtils.hasText(item.getStartDate())
                        || !StringUtils.hasText(item.getEndDate())
                        || !StringUtils.hasText(item.getStartTime())
                        || !StringUtils.hasText(item.getEndTime())) {
                    if (strict) {
                        throw new BusinessException("规则的日期和时间不能为空");
                    }
                    continue;  // 非严格模式，跳过这条规则
                }

                // 解析并校验日期时间
                LocalDate startDate = DateTimeUtil.parseDate(item.getStartDate());
                LocalDate endDate = DateTimeUtil.parseDate(item.getEndDate());
                if (endDate.isBefore(startDate)) {
                    throw new BusinessException("规则结束日期必须不早于开始日期");
                }

                LocalTime startTime = DateTimeUtil.parseTime(item.getStartTime());
                LocalTime endTime = DateTimeUtil.parseTime(item.getEndTime());
                if (!endTime.isAfter(startTime)) {
                    throw new BusinessException("规则结束时间必须晚于开始时间");
                }

                // 构建规范化后的规则对象
                AppointmentTimeRule rule = new AppointmentTimeRule();
                rule.setStartDate(DateTimeUtil.formatDate(startDate));
                rule.setEndDate(DateTimeUtil.formatDate(endDate));
                rule.setStartTime(DateTimeUtil.formatTime(startTime));
                rule.setEndTime(DateTimeUtil.formatTime(endTime));
                rule.setOpen(item.getOpen() == null ? Boolean.TRUE : item.getOpen());
                rules.add(rule);
            }
        }

        // 按开始日期和时间排序，然后校验是否重叠
        rules.sort(Comparator
                .comparing(AppointmentTimeRule::getStartDate)
                .thenComparing(AppointmentTimeRule::getStartTime));
        validateNoOverlap(rules);
        normalized.setRules(rules);

        // 更新时间
        LocalDateTime updatedTime = raw == null ? null : raw.getUpdatedTime();
        normalized.setUpdatedTime(updatedTime == null ? LocalDateTime.now() : updatedTime);
        return normalized;
    }

    // ==================== 私有方法：校验规则 ====================

    /**
     * 校验时间规则是否重叠
     * 任意两条规则的时间段有交集，就认为重叠
     */
    private void validateNoOverlap(List<AppointmentTimeRule> rules) {
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                if (hasOverlap(rules.get(i), rules.get(j))) {
                    throw new BusinessException("规则时间段存在重叠，请调整后再保存");
                }
            }
        }
    }

    /**
     * 判断两个时间规则是否重叠
     * 日期范围有交集，且时间范围也有交集，才算重叠
     */
    private boolean hasOverlap(AppointmentTimeRule a, AppointmentTimeRule b) {
        LocalDate aStart = DateTimeUtil.parseDate(a.getStartDate());
        LocalDate aEnd = DateTimeUtil.parseDate(a.getEndDate());
        LocalDate bStart = DateTimeUtil.parseDate(b.getStartDate());
        LocalDate bEnd = DateTimeUtil.parseDate(b.getEndDate());

        // 日期范围无交集，肯定不重叠
        if (aEnd.isBefore(bStart) || bEnd.isBefore(aStart)) {
            return false;
        }

        // 日期有交集，再比时间
        LocalTime aTimeStart = DateTimeUtil.parseTime(a.getStartTime());
        LocalTime aTimeEnd = DateTimeUtil.parseTime(a.getEndTime());
        LocalTime bTimeStart = DateTimeUtil.parseTime(b.getStartTime());
        LocalTime bTimeEnd = DateTimeUtil.parseTime(b.getEndTime());

        // 时间也有交集，才算重叠
        return !aTimeEnd.isBefore(bTimeStart) && !bTimeEnd.isBefore(aTimeStart);
    }

    /**
     * 判断指定时刻是否在开放时间内
     * 
     * 遍历所有规则，找到匹配的日期范围，再看时间是否在范围内。
     * 如果没有任何规则匹配该日期，默认返回true（开放）。
     */
    private boolean isOpenAt(AppointmentSetting setting, LocalDateTime targetTime) {
        List<AppointmentTimeRule> rules = setting.getRules();
        // 没有规则，默认全天开放
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        LocalDate targetDate = targetTime.toLocalDate();
        LocalTime currentTime = targetTime.toLocalTime();
        boolean hasRuleForDate = false;  // 是否有规则匹配这个日期

        for (AppointmentTimeRule rule : rules) {
            LocalDate ruleStart = DateTimeUtil.parseDate(rule.getStartDate());
            LocalDate ruleEnd = DateTimeUtil.parseDate(rule.getEndDate());

            // 日期不在规则范围内，跳过
            if (targetDate.isBefore(ruleStart) || targetDate.isAfter(ruleEnd)) {
                continue;
            }
            hasRuleForDate = true;

            // 时间在规则范围内，看这个规则是开放还是关闭
            LocalTime start = DateTimeUtil.parseTime(rule.getStartTime());
            LocalTime end = DateTimeUtil.parseTime(rule.getEndTime());
            if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                return Boolean.TRUE.equals(rule.getOpen());
            }
        }

        // 有规则但都不匹配当前时间，返回false（不开放）
        // 没有规则匹配这个日期，返回true（开放）
        return !hasRuleForDate;
    }

    /**
     * 构建关闭时的提示信息
     * 告诉用户哪些时段可以预约
     */
    private String buildClosedMessage(AppointmentSetting setting, LocalDateTime targetTime) {
        List<String> openWindows = new ArrayList<>();
        LocalDate targetDate = targetTime.toLocalDate();

        for (AppointmentTimeRule rule : setting.getRules()) {
            LocalDate ruleStart = DateTimeUtil.parseDate(rule.getStartDate());
            LocalDate ruleEnd = DateTimeUtil.parseDate(rule.getEndDate());

            if (targetDate.isBefore(ruleStart) || targetDate.isAfter(ruleEnd)) {
                continue;
            }
            // 收集这个日期范围内开放的时段
            if (Boolean.TRUE.equals(rule.getOpen())) {
                openWindows.add(rule.getStartTime() + "-" + rule.getEndTime());
            }
        }

        StringBuilder message = new StringBuilder();
        if (openWindows.isEmpty()) {
            message.append("当前时段预约未开放");
        } else {
            message.append("当前时段预约未开放，可预约时段：").append(String.join("、", openWindows));
        }

        // 附上系统公告
        if (StringUtils.hasText(setting.getNotice())) {
            message.append("：").append(setting.getNotice());
        }
        return message.toString();
    }

    // ==================== 私有方法：文件读写 ====================

    /**
     * 创建默认设置
     */
    private AppointmentSetting createDefaultSetting() {
        AppointmentSetting setting = new AppointmentSetting();
        setting.setNotice(DEFAULT_NOTICE);
        setting.setUpdatedTime(LocalDateTime.now());
        setting.setRules(new ArrayList<>());
        return setting;
    }

    /**
     * 从JSON文件读取设置
     */
    private AppointmentSetting readSettingFromFile() {
        try {
            if (!Files.exists(storagePath)) {
                return null;  // 文件不存在，返回null
            }
            return objectMapper.readValue(storagePath.toFile(), AppointmentSetting.class);
        } catch (Exception ex) {
            throw new BusinessException("读取预约设置失败，请检查配置文件");
        }
    }

    /**
     * 写入JSON文件
     * 用pretty printer格式化，方便人直接看文件
     */
    private void writeSettingToFile(AppointmentSetting setting) {
        try {
            Path parent = storagePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);  // 目录不存在就创建
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), setting);
        } catch (Exception ex) {
            throw new BusinessException("保存预约设置失败");
        }
    }
}
