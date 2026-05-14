package com.gpj.visitorsystem.interceptor.wx;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 【业务模块】安保端接口专用拦截器
 *
 * 【核心职责】
 * 1. 继承 WxAuthInterceptor，复用 JWT Token 验证和用户信息解析
 * 2. 额外校验 userType=3（只有安保人员可以访问安保端接口）
 * 3. 校验失败返回 403 无权限响应
 *
 * 【关键业务场景】
 * 1. 安保人员登录后携带 Token 访问 /api/wx/security/** 路径
 * 2. 父类先验证 Token 合法性，解析 userId 和 userType 存入 request
 * 3. 本类再校验 userType=3，不满足返回 403
 *
 * 【依赖说明】
 * - 继承 WxAuthInterceptor：复用小程序端 Token 验证逻辑
 * - 被 WebConfig 注册到 /api/wx/security/**
 *
 * 【注意事项】
 * - 只处理安保端接口，不影响其他小程序接口
 * - Token 无效/过期时父类已写 401 响应，本类无需重复处理
 * - userType≠3 时返回 403，前端跳转无权限页面
 */
@Component
public class WxSecurityAuthInterceptor extends WxAuthInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 1. 先调用父类验证 Token（验证失败直接返回 false，父类已写 401 响应）
        if (!super.preHandle(request, response, handler)) {
            return false;
        }

        // 2. 额外校验 userType=3（只有安保人员可以访问安保端接口）
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 3) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问安保端接口");
            return false;
        }

        return true;
    }
}
