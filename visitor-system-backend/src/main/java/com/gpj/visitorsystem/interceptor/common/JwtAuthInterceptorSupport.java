package com.gpj.visitorsystem.interceptor.common;

import com.gpj.visitorsystem.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 【业务模块】JWT认证拦截器基类
 *
 * 【核心职责】
 * 1. 提供统一的JWT Token验证和解析逻辑
 * 2. 提供统一的JSON格式错误响应写入方法
 * 3. 被WxAuthInterceptor和AdminAuthInterceptor继承使用
 * 4. 避免重复编写Token验证和错误处理代码
 *
 * 【关键业务场景】
 * 1. 从Authorization头中提取Bearer Token
 * 2. 使用JwtUtils验证token有效性并解析Claims
 * 3. 验证失败时自动写入401 JSON错误响应
 * 4. 子类调用validateAndParseClaims后，判断角色权限
 *
 * 【依赖说明】
 * - 被WxAuthInterceptor继承：小程序端拦截器
 * - 被AdminAuthInterceptor继承：管理端拦截器
 * - 使用JwtUtils：验证和解析JWT Token
 *
 * 【注意事项】
 * - validateAndParseClaims：从Header取Token、验证、解析，失败写401响应
 * - 验证成功返回Claims（包含userId、userType），失败返回null
 * - writeJsonError：统一写入JSON格式错误响应，前端好处理
 * - 子类应调用validateAndParseClaims后，再判断角色权限
 * - 错误响应格式与ResultDTO一致，code非200，msg包含错误信息
 */
public abstract class JwtAuthInterceptorSupport {

    /**
     * 验证请求中的JWT Token
     * 
     * 从Authorization头取Bearer Token，验证通过返回Claims，失败返回null（已经写了401响应）。
     * 子类调用这个方法，拿到Claims后判断角色权限。
     * 
     * @return 解析后的Claims（包含userId、userType），验证失败返回null
     */
    protected Claims validateAndParseClaims(HttpServletRequest request, HttpServletResponse response, JwtUtils jwtUtils) throws IOException {
        // 从Header取Authorization: Bearer xxx
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没带Token，返回401
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未提供认证信息");
            return null;
        }

        // 去掉"Bearer "前缀，拿到纯Token
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            // Token无效或过期，返回401
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "token无效或已过期");
            return null;
        }
        // 验证通过，解析Claims返回
        return jwtUtils.parseToken(token);
    }

    /**
     * 向客户端写入JSON格式的错误响应
     * 
     * 统一错误响应格式：{"code":xxx,"msg":"xxx"}
     * 前端统一用这个格式判断成功/失败。
     */
    protected void writeJsonError(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        // 简单拼接JSON，不用Jackson，避免依赖
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + message + "\"}");
    }
}
