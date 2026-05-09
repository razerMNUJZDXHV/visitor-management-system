package com.gpj.visitorsystem.controller.wx.visitor;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.entity.Appointment;
import com.gpj.visitorsystem.entity.AppointmentOpenStatus;
import com.gpj.visitorsystem.entity.AppointmentSetting;
import com.gpj.visitorsystem.service.common.AppointmentSettingService;
import com.gpj.visitorsystem.service.wx.visitor.WxVisitorAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【业务模块】访客端预约接口
 *
 * 【核心职责】
 * 1. 创建/取消预约与查询预约记录
 * 2. 获取预约详情与生成通行二维码
 * 3. 查询预约设置与开放状态
 *
 * 【关键业务场景】
 * 所有接口需携带visitorId用于权限校验，Controller只做参数转发。
 *
 * 【依赖说明】
 * - WxVisitorAppointmentService：预约业务逻辑
 * - AppointmentSettingService：预约设置与开放状态
 *
 * 【注意事项】
 * - 预约创建前需检查开放状态与每日上限
 * - 二维码仅预约成功/已签到可生成
 */
@RestController
@RequestMapping("/api/wx/visitor")
public class WxVisitorAppointmentController {

    @Autowired
    private WxVisitorAppointmentService wxVisitorAppointmentService;

    @Autowired
    private AppointmentSettingService appointmentSettingService;

    /**
     * 创建预约
     *
     * 【接口说明】
     * 访客提交预约信息创建预约记录，状态为待审核。
     *
     * 【请求参数】
     * @param appointment 预约信息（JSON body），包含visitorName、visitorPhone等
     * @param visitorId 访客用户ID（query param）
     *
     * 【返回值】
     * @return 创建后的预约实体（含appointmentId）
     *
     * 【异常情况】
     * - 预约时间未开放/用户被封禁/每日上限已满：返回对应错误信息
     */
    @PostMapping("/appointment/create")
    public ResultDTO<Appointment> create(@RequestBody Appointment appointment,
                                         @RequestParam Integer visitorId) {
        appointment.setVisitorId(Long.valueOf(visitorId));
        Appointment created = wxVisitorAppointmentService.createAppointment(appointment);
        return ResultDTO.success(created);
    }

    /**
     * 查询我的预约列表
     *
     * 【接口说明】
     * 返回访客的预约记录列表并自动应用过期与爽约处理。
     *
     * 【请求参数】
     * @param visitorId 访客用户ID
     *
     * 【返回值】
     * @return 预约列表，身份证号已解密，状态已更新
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/appointment/list")
    public ResultDTO<List<Appointment>> listMyAppointments(@RequestParam Integer visitorId) {
        return ResultDTO.success(wxVisitorAppointmentService.listMyAppointments(visitorId));
    }

    /**
     * 查询预约详情
     *
     * 【接口说明】
     * 查询访客自己的预约详情，包含解密后的身份证号。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【返回值】
     * @return 预约详情，身份证号已解密
     *
     * 【异常情况】
     * - 预约不存在/无权限查看：返回对应错误信息
     */
    @GetMapping("/appointment/detail")
    public ResultDTO<Appointment> getDetail(@RequestParam Long appointmentId,
                                            @RequestParam Integer visitorId) {
        return ResultDTO.success(wxVisitorAppointmentService.getAppointmentDetail(appointmentId, visitorId));
    }

    /**
     * 取消预约
     *
     * 【接口说明】
     * 访客取消自己的预约，状态更新为已取消。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 预约不存在/无权取消/状态不允许：返回对应错误信息
     */
    @PostMapping("/appointment/cancel")
    public ResultDTO<Void> cancel(@RequestParam Long appointmentId,
                                  @RequestParam Integer visitorId) {
        wxVisitorAppointmentService.cancelAppointment(appointmentId, visitorId);
        return ResultDTO.success(null);
    }

    /**
     * 生成预约二维码
     *
     * 【接口说明】
     * 预约成功后生成JWT二维码供安保扫码通行。
     *
     * 【请求参数】
     * @param appointmentId 预约ID
     * @param visitorId 访客用户ID（用于权限校验）
     *
     * 【返回值】
     * @return Base64编码的二维码图片数据，可直接用于前端img标签
     *
     * 【异常情况】
     * - 预约不存在/无权操作/状态不允许/时间未到或已过期：返回对应错误信息
     */
    @GetMapping("/appointment/qrcode")
    public ResultDTO<String> generateQrCode(@RequestParam Long appointmentId,
                                            @RequestParam Integer visitorId) {
        return ResultDTO.success(wxVisitorAppointmentService.generateQrCode(appointmentId, visitorId));
    }

    /**
     * 获取预约设置
     *
     * 【接口说明】
     * 返回预约设置与规则信息，供前端展示。
     *
    * 【请求参数】
    * 无
     *
     * 【返回值】
     * @return 预约设置实体，包含开放状态、时间规则、每日上限等
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/appointment/setting")
    public ResultDTO<AppointmentSetting> getSetting() {
        return ResultDTO.success(appointmentSettingService.getCurrentSetting());
    }

    /**
     * 获取预约开放状态
     *
     * 【接口说明】
     * 返回当前预约是否开放及提示信息。
     *
    * 【请求参数】
    * 无
     *
     * 【返回值】
     * @return 预约开放状态，包含open（是否开放）和message（提示信息）
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/appointment/open-status")
    public ResultDTO<AppointmentOpenStatus> getOpenStatus() {
        return ResultDTO.success(appointmentSettingService.getCurrentOpenStatus());
    }

    /**
     * 获取每日预约人数上限状态
     *
     * 【接口说明】
     * 返回今日预约人数、上限与是否已达上限。
     *
    * 【请求参数】
    * 无
     *
     * 【返回值】
     * @return dailyLimit: 上限人数, todayCount: 今日已预约, reached: 是否已达上限
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/appointment/daily-limit-status")
    public ResultDTO<java.util.Map<String, Object>> getDailyLimitStatus() {
        return ResultDTO.success(wxVisitorAppointmentService.getDailyLimitStatus());
    }
}