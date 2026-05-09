package com.gpj.visitorsystem.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 【业务模块】JWT工具类
 *
 * 【核心职责】
 * 1. 生成和验证access token（短期有效，默认2小时）- 用于接口鉴权
 * 2. 生成和验证refresh token（长期有效，默认7天）- 用于刷新access token
 * 3. 生成和验证appointment token（短期有效）- 用于预约二维码
 * 4. 从Token中解析userId、userType等关键信息
 *
 * 【关键业务场景】
 * 1. 用户登录后生成access token和refresh token返回给前端
 * 2. 前端每次请求在Authorization头携带access token，后端验证
 * 3. access token过期后，前端用refresh token换取新的access token
 * 4. 审批通过后生成appointment token，嵌入二维码供安保扫码
 * 5. 安保扫码解析appointment token，直接获取预约信息，无需查数据库
 *
 * 【依赖说明】
 * - 被WxCommonUserService使用：生成和刷新token
 * - 被WxSecurityAccessService使用：解析二维码token
 * - 被拦截器使用：验证请求中的token
 *
 * 【注意事项】
 * - secret配置在application.yml的jwt.secret，生产环境必须修改默认值
 * - access token过期时间默认2小时（7200000毫秒）
 * - refresh token过期时间默认7天
 * - appointment token过期时间较短，与预约时间段绑定
 * - 二维码Token包含预约ID、访客ID等信息，安保扫码直接解析
 * - Token使用HS256算法签名，secret必须保密
 */
@Component
public class JwtUtils {
    @Value("${jwt.secret:mySecretKeyForJwtTokenGeneration2024}")
    private String secret;

    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问Token（短期，默认2小时）
     */
    public String generateToken(Integer userId, Integer userType) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新Token（长期，7天）
     */
    public String generateRefreshToken(Integer userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000L);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证刷新Token是否有效
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * 从Token中获取用户类型
     */
    public Integer getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userType", Integer.class);
    }

    /**
     * 生成预约二维码Token（有效期至预计离开时间+宽限期）
     */
    public String generateAppointmentToken(Long appointmentId, LocalDateTime expireTime) {
        Date now = new Date();
        Date expireDate = Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .claim("appointmentId", appointmentId)
                .claim("type", "appointment_qr")
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证预约二维码Token
     */
    public boolean validateAppointmentToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);
            return "appointment_qr".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 从预约Token中获取预约ID
     */
    public Long getAppointmentIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("appointmentId", Long.class);
    }
}