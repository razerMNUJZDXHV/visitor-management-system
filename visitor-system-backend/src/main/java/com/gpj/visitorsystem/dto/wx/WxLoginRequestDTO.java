package com.gpj.visitorsystem.dto.wx;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 【业务模块】小程序登录请求DTO
 *
 * 【核心职责】
 * 1. 封装小程序端登录请求参数
 * 2. 支持旧版仅传code登录和新版一次性登录+手机号绑定
 * 3. 提供参数校验注解（@NotBlank等）
 *
 * 【关键业务场景】
 * 1. 旧版：前端调用wx.login获取code，仅传code完成登录
 * 2. 新版：前端同时传code、encryptedData、iv，一次性完成登录+手机号绑定
 * 3. 后端根据encryptedData和iv是否存在，判断走新版还是旧版逻辑
 *
 * 【依赖说明】
 * - 被WxCommonUserController.login接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - code为必填字段，由wx.login()获取，有效期5分钟
 * - encryptedData和iv为选填，传了则走新版一次性登录+绑定流程
 * - encryptedData为微信加密后的手机号数据，需用session_key解密
 * - iv为解密初始向量，必须与encryptedData配对使用
 */
@Data
public class WxLoginRequestDTO {
    @NotBlank(message = "code不能为空")
    private String code;     // wx.login()获取的临时code
    
    // 新版：传手机号加密数据，一次性完成登录+绑定
    private String encryptedData;  // 加密的手机号数据
    private String iv;             // 解密初始向量
}
