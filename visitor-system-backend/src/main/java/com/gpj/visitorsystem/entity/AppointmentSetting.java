package com.gpj.visitorsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 【业务模块】预约设置实体
 *
 * 【核心职责】
 * 1. 存储系统全局预约配置信息
 * 2. 管理访客端首页公告（notice）
 * 3. 控制每日预约人数上限（dailyLimit）
 * 4. 维护分时段预约规则列表（rules）
 *
 * 【关键业务场景】
 * 1. 管理员在后台配置公告信息，访客端首页实时展示
 * 2. 设置每日预约上限，防止访客超额影响校园秩序
 * 3. 配置分时段规则，如"工作日8:00-18:00开放预约"
 * 4. 临时关闭预约（如节假日、突发事件），访客端提示"预约暂未开放"
 *
 * 【依赖说明】
 * - 包含AppointmentTimeRule列表：定义允许预约的时间段规则
 * - 与AppointmentOpenStatus关联：控制预约通道总开关
 *
 * 【注意事项】
 * - 数据存储在JSON文件中，修改后立即生效
 * - dailyLimit为null或0表示不限制每日预约数量
 * - rules中的时间规则不能重叠，需校验合法性
 * - 设置变更无需重启服务，实时生效
 */
@Data
public class AppointmentSetting {
    private String notice;           // 系统公告，访客端首页展示
    private Integer dailyLimit;      // 每日预约上限，null表示不限制
    private LocalDateTime updatedTime;   // 最后修改时间
    private List<AppointmentTimeRule> rules = new ArrayList<>();  // 预约时间规则
}