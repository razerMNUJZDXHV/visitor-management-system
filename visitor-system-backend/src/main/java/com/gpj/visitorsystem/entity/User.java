package com.gpj.visitorsystem.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 【业务模块】用户实体
 *
 * 【核心职责】
 * 1. 存储系统所有角色的基础信息（访客、审批人、安保、管理员）
 * 2. 通过userType字段区分不同角色权限
 * 3. 管理用户封禁状态与爽约次数
 * 4. 维护JWT刷新token及其过期时间
 *
 * 【关键业务场景】
 * 1. 访客在小程序端注册，自动创建userType=1的记录
 * 2. 审批人/安保由管理员在Web后台预添加（只有手机号，无openid）
 * 3. 用户首次小程序登录时，通过手机号匹配已有账号并绑定openid，避免重复用户
 * 4. 连续爽约达到阈值后自动封禁，封禁期禁止预约
 *
 * 【依赖说明】
 * - 与Appointment关联：访客通过userId关联预约记录
 * - 与AccessLog关联：安保通过securityId关联通行记录
 *
 * 【注意事项】
 * - userType说明：1-访客 2-审批人 3-安保 4-管理员
 * - 密码字段使用@JsonProperty(writeOnly)避免序列化泄露
 * - session_key用于解密微信手机号，需定期更新
 * - bannedUntil为null表示未封禁，非null且在当前时间之后表示封禁中
 */
@Data
public class User {
    private Integer userId;
    private String openid;       // 微信openid，小程序登录后填充
    private String sessionKey;   // 微信session_key，解密手机号用
    private String phone;        // 手机号，授权后填充

    // 密码只写不读，避免JSON序列化返回给前端
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;     // 管理端登录密码（BCrypt加密）

    private String realName;     // 真实姓名，审批人/安保/管理员必填
    private Integer userType;    // 1-访客 2-审批人 3-安保 4-管理员
    private LocalDateTime createTime;
    private Integer missedCount;    // 累计爽约次数，达到阈值封禁
    private LocalDateTime bannedUntil;   // 封禁截止时间，null表示未封禁
    private String refreshToken;          // JWT刷新token
    private LocalDateTime refreshTokenExpireTime;  // 刷新token过期时间
}