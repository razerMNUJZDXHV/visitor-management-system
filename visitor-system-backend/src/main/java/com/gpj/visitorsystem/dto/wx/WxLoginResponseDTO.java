package com.gpj.visitorsystem.dto.wx;

import lombok.Data;

/**
 * 【业务模块】小程序登录响应DTO
 *
 * 【核心职责】
 * 1. 封装小程序登录成功后的返回数据
 * 2. 包含用户基本信息和JWT认证令牌
 * 3. 前端需持久化存储token和refreshToken
 *
 * 【关键业务场景】
 * 1. 登录成功后返回，前端保存到storage供后续请求使用
 * 2. 前端根据userType判断用户角色，跳转不同首页
 * 3. token用于后续接口鉴权（放在Authorization头中）
 * 4. refreshToken用于access token过期后刷新
 *
 * 【依赖说明】
 * - 由WxCommonUserService组装
 * - 由WxCommonUserController封装到ResultDTO返回
 *
 * 【注意事项】
 * - token为access token，有效期2小时，过期需用refreshToken刷新
 * - refreshToken有效期7天，过期需重新登录
 * - 前端应将token和refreshToken持久化存储（如storage）
 * - phone可能为null（旧版登录未绑定手机号时）
 * - userType：1-访客 2-审批人 3-安保 4-管理员
 */
@Data
public class WxLoginResponseDTO {
    private Integer userId;
    private String openid;       // 微信openid
    private Integer userType;     // 1-访客 2-审批人 3-安保 4-管理员
    private String phone;         // 手机号（绑定后才有）
    private String realName;      // 真实姓名
    private String token;         // access token，有效期2小时
    private String refreshToken;  // refresh token，有效期7天
}
