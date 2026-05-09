package com.gpj.visitorsystem.dto.wx.security;

import lombok.Data;

/**
 * 【业务模块】安保扫码验证请求DTO
 *
 * 【核心职责】
 * 1. 封装安保扫码核验的请求参数
 * 2. 传递二维码中的JWT Token供后端解析
 * 3. 是安保端扫码通行流程的第一步
 *
 * 【关键业务场景】
 * 1. 安保使用小程序扫描访客出示的二维码
 * 2. 前端从二维码中提取JWT Token，调用此接口
 * 3. 后端解析Token获取预约ID、访客ID等信息
 * 4. 返回SecurityScanVerifyResponseDTO供安保确认
 *
 * 【依赖说明】
 * - 被WxSecurityAccessController.scanVerify接收
 *
 * 【注意事项】
 * - qrToken为二维码中的JWT Token，包含预约信息
 * - Token有效期为2小时，过期后需手动登记
 * - 解析失败可能原因：Token过期、格式错误、被篡改
 */
@Data
public class SecurityScanVerifyRequestDTO {
    private String qrToken;  // 二维码里的JWT Token
}