package com.gpj.visitorsystem.interceptor.admin;

import com.gpj.visitorsystem.interceptor.common.JwtAuthInterceptorSupport;
import com.gpj.visitorsystem.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 【业务模块】管理员接口拦截器
 *
 * 【核心职责】
 * 1. 拦截所有/api/admin/**请求，验证JWT Token有效性
 * 2. 校验用户角色是否为管理员（userType=4），非管理员返回403
 * 3. 验证通过后，将userId存入request属性供Controller使用
 * 4. 验证失败返回401/403响应
 *
 * 【关键业务场景】
 * 1. 管理员登录后获取token，后续请求携带在Authorization头中
 * 2. 拦截器验证token有效性，解析userId和userType
 * 3. 校验userType是否为4（管理员），非管理员返回403无权限
 * 4. 将userId存入request属性，Controller通过@RequestAttribute获取
 * 5. token无效/过期时返回401，前端跳转登录页
 *
 * 【依赖说明】
 * - 继承JwtAuthInterceptorSupport：共用Token验证逻辑
 * - 使用JwtUtils：验证和解析JWT Token
 * - 被WebConfig注册：配置拦截/api/admin/**路径
 *
 * 【注意事项】
 * - 登录注册接口必须放行（在WebConfig中配置excludePathPatterns）
 * - OPTIONS预检请求直接放行（CORS需要）
 * - 不仅验证token有效性，还校验userType=4（管理员权限）
 * - 非管理员返回403，与token无效的401区分
 * - 所有Controller可通过@RequestAttribute("userId")获取当前管理员ID
 */
@Component
public class AdminAuthInterceptor extends JwtAuthInterceptorSupport implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Claims claims = validateAndParseClaims(request, response, jwtUtils);
        if (claims == null) {
            return false;
        }

        Integer userType = claims.get("userType", Integer.class);
        if (userType == null || userType != 4) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问");
            return false;
        }

        // 将用户ID存入请求属性，供Controller使用
        request.setAttribute("userId", claims.get("userId", Integer.class));
        return true;
    }
}
