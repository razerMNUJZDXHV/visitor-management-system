package com.gpj.visitorsystem.interceptor.wx;

import com.gpj.visitorsystem.interceptor.common.JwtAuthInterceptorSupport;
import com.gpj.visitorsystem.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 【业务模块】小程序端接口拦截器
 *
 * 【核心职责】
 * 1. 拦截所有/api/wx/**请求（除登录注册接口），验证JWT Token有效性
 * 2. 验证通过后，将userId和userType存入request属性供Controller使用
 * 3. 验证失败返回401未授权响应
 * 4. 放行OPTIONS预检请求（CORS需要）
 *
 * 【关键业务场景】
 * 1. 小程序端登录后获取token，后续请求携带在Authorization头中
 * 2. 拦截器验证token有效性，解析userId和userType
 * 3. 将解析结果存入request属性，Controller通过@RequestAttribute获取
 * 4. token无效/过期时返回401，前端跳转登录页
 *
 * 【依赖说明】
 * - 继承JwtAuthInterceptorSupport：共用Token验证逻辑
 * - 使用JwtUtils：验证和解析JWT Token
 * - 被WebConfig注册：配置拦截/api/wx/**路径
 *
 * 【注意事项】
 * - 登录注册接口必须放行（在WebConfig中配置excludePathPatterns）
 * - OPTIONS预检请求直接放行（CORS需要）
 * - token从Authorization头中获取，格式为"Bearer {token}"
 * - 解析成功后将userId和userType存入request属性
 * - 所有Controller可通过@RequestAttribute("userId")获取当前用户ID
 */
@Component
public class WxAuthInterceptor extends JwtAuthInterceptorSupport implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS预检请求（CORS需要）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 验证Token，解析Claims
        Claims claims = validateAndParseClaims(request, response, jwtUtils);
        if (claims == null) {
            return false;  // Token无效，已经写了401响应
        }

        // 把用户信息存入request，Controller里直接getAttribute取
        Integer userId = claims.get("userId", Integer.class);
        Integer userType = claims.get("userType", Integer.class);
        request.setAttribute("userId", userId);
        request.setAttribute("userType", userType);
        return true;
    }
}
