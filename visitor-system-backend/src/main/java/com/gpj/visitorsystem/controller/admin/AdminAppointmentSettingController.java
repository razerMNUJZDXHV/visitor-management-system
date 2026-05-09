package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.entity.AppointmentSetting;
import com.gpj.visitorsystem.service.common.AppointmentSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【业务模块】管理后台预约设置
 *
 * 【核心职责】
 * 1. 查询当前预约设置
 * 2. 更新预约规则与系统公告
 * 3. 提供前端预约设置数据
 *
 * 【关键业务场景】
 * 预约设置存储在JSON文件中，修改后立即生效，无需数据库迁移。
 *
 * 【依赖说明】
 * - AppointmentSettingService：预约设置读取与校验
 *
 * 【注意事项】
 * - rules中的时间规则不能重叠
 * - dailyLimit为0或null表示不限额
 */
@RestController
@RequestMapping("/api/admin/appointment-setting")
public class AdminAppointmentSettingController {

    @Autowired
    private AppointmentSettingService appointmentSettingService;

    /**
     * 获取预约设置
     *
     * 【接口说明】
     * 返回当前预约设置（公告、限额、时间规则）。
     *
    * 【请求参数】
    * 无
     *
     * 【返回值】
     * @return 预约设置实体，包含notice、dailyLimit、rules等
     *
     * 【异常情况】
     * - 读取失败：返回系统错误信息
     */
    @GetMapping
    public ResultDTO<AppointmentSetting> getSetting() {
        return ResultDTO.success(appointmentSettingService.getCurrentSetting());
    }

    /**
     * 更新预约设置
     *
     * 【接口说明】
     * 管理员调整预约规则与公告，保存后立即生效。
     *
     * 【请求参数】
     * @param setting 预约设置实体，包含notice、dailyLimit、rules等
     *
     * 【返回值】
     * @return 更新后的预约设置实体
     *
     * 【异常情况】
     * - 规则重叠/时间格式错误：返回对应校验错误信息
     */
    @PutMapping
    public ResultDTO<AppointmentSetting> updateSetting(@RequestBody AppointmentSetting setting) {
        return ResultDTO.success(appointmentSettingService.updateSetting(setting));
    }
}