package com.gpj.visitorsystem.dto.wx;

import lombok.Data;

/**
 * 【业务模块】小程序手机号绑定响应DTO
 *
 * 【核心职责】
 * 1. 封装手机号绑定成功后的返回数据
 * 2. 返回更新后的用户信息和新的JWT令牌
 * 3. 前端需更新本地存储的token和refreshToken
 *
 * 【关键业务场景】
 * 1. 旧版手机号绑定成功后返回，前端更新storage中的token
 * 2. 若绑定的是管理员预添加账号，返回该账号的userType（审批人/安保）
 * 3. 前端根据userType判断是否需要跳转不同首页
 *
 * 【依赖说明】
 * - 由WxCommonUserService.updatePhone组装
 * - 由WxCommonUserController封装到ResultDTO返回
 *
 * 【注意事项】
 * - 绑定成功后需重新生成token，旧的token失效
 * - userType可能变化（如从访客变为安保），前端需重新判断角色
 * - realName可能为null（管理员添加时未设置姓名）
 * - 前端应更新storage中的token和refreshToken
 */
@Data
public class WxPhoneResponseDTO {
    private Integer userId;
    private String openid;       // 微信openid
    private Integer userType;     // 1-访客 2-审批人 3-安保 4-管理员
    private String phone;         // 绑定后的手机号
    private String realName;     // 真实姓名（可能是管理员添加的账号的姓名）
    private String token;        // 新的access token
    private String refreshToken; // 新的refresh token
}
