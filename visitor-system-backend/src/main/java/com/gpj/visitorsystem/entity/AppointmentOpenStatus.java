package com.gpj.visitorsystem.entity;

import lombok.Data;

/**
 * 【业务模块】预约开关状态
 *
 * 【核心职责】
 * 1. 控制整个预约通道的总开关
 * 2. 提供关闭时的提示信息给访客端展示
 * 3. 供访客端在创建预约前查询校验
 *
 * 【关键业务场景】
 * 1. 管理员在后台临时关闭预约（如节假日、突发事件、系统维护）
 * 2. 访客端首页查询此状态，若关闭则提示"预约暂未开放"及原因
 * 3. 创建预约接口前置校验，关闭时直接拒绝创建
 * 4. 与AppointmentSetting中的规则校验配合使用
 *
 * 【依赖说明】
 * - 与AppointmentSetting关联：共同决定预约是否开放
 *
 * 【注意事项】
 * - open=true时预约通道开放，访客可正常预约
 * - open=false时预约通道关闭，访客端显示message提示
 * - message字段在关闭时必填，用于告知访客关闭原因
 * - 该状态独立于AppointmentSetting中的时间规则，优先级更高
 */
@Data
public class AppointmentOpenStatus {
    private Boolean open;     // 是否开放预约
    private String message;   // 关闭时的提示信息
}