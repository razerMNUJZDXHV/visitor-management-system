package com.gpj.visitorsystem.dto.wx;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 【业务模块】小程序手机号绑定请求DTO
 *
 * 【核心职责】
 * 1. 封装旧版手机号绑定请求参数
 * 2. 提供参数校验注解确保必填字段不为空
 * 3. 支持管理员预添加账号的手机号匹配绑定
 *
 * 【关键业务场景】
 * 1. 旧版流程：用户先wxLogin登录获取openid，再调用此接口绑定手机号
 * 2. 新版流程已合并到login接口，此接口仅保留兼容旧版前端
 * 3. 绑定手机号时，若手机号已存在管理员预添加的账号，则合并openid
 *
 * 【依赖说明】
 * - 被WxCommonUserController.updatePhone接收
 * - 参数校验由Spring Validation自动处理
 *
 * 【注意事项】
 * - openid为必填，由微信登录返回
 * - encryptedData为微信加密的手机号数据，需用session_key解密
 * - iv为解密初始向量，必须与encryptedData配对使用
 * - 绑定失败可能原因：session_key过期、解密失败、用户不存在
 */
@Data
public class WxPhoneRequestDTO {
    @NotBlank(message = "openid不能为空")
    private String openid;          // 微信openid
    @NotBlank(message = "encryptedData不能为空")
    private String encryptedData;  // 加密的手机号数据
    @NotBlank(message = "iv不能为空")
    private String iv;               // 解密初始向量
}