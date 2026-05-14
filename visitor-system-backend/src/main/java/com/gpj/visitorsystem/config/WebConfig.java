package com.gpj.visitorsystem.config;

import com.gpj.visitorsystem.interceptor.admin.AdminAuthInterceptor;
import com.gpj.visitorsystem.interceptor.wx.WxAuthInterceptor;
import com.gpj.visitorsystem.interceptor.wx.WxSecurityAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 【业务模块】Web MVC配置
 *
 * 【核心职责】
 * 1. 配置跨域（CORS），放行小程序端和Web管理端请求
 * 2. 配置接口拦截器，按角色分流鉴权
 * 3. 放行登录注册接口，确保用户能进入系统
 * 4. 配置拦截器执行顺序和拦截路径
 *
 * 【关键业务场景】
 * 1. 小程序端和Web管理端域名不同，需CORS放行预检请求
 * 2. /api/admin/**请求走AdminAuthInterceptor，校验管理员权限
 * 3. /api/wx/**请求走WxAuthInterceptor，校验小程序用户权限
 * 4. 登录、注册等接口放行，否则用户无法获取token
 * 5. OPTIONS预检请求放行，支持跨域POST/PUT/DELETE
 *
 * 【依赖说明】
 * - AdminAuthInterceptor：管理端接口鉴权
 * - WxAuthInterceptor：小程序端接口鉴权
 * - CorsFilter：跨域过滤器
 *
 * 【注意事项】
 * - 拦截器顺序：CorsFilter → WxAuthInterceptor/AdminAuthInterceptor
 * - 登录注册接口必须放行，否则形成死锁（没token进不来，进不来拿不到token）
 * - CORS配置需允许Authorization头，否则token传不过来
 * - 生产环境需限制allowedOrigins，不能设置为"*"
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    @NonNull
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    @NonNull
    private WxAuthInterceptor wxAuthInterceptor;

    @Autowired
    @NonNull
    private WxSecurityAuthInterceptor wxSecurityAuthInterceptor;

    /**
     * CORS跨域配置
     * 小程序和Web管理端端口不同，需要允许跨域携带Cookie和Token
     *
     * 【安全说明】
     * - 生产环境必须修改 allowedOrigins，不能使用"*"
     * - setAllowCredentials(true) 时，allowedOrigins 不能为"*"
     * - 这里使用 addAllowedOriginPattern 而非 addAllowedOrigin，支持通配符
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境请修改为具体域名，例如：
        // config.addAllowedOriginPattern("https://yourdomain.com");
        // config.addAllowedOriginPattern("https://yourdomain.com:8080");
        config.addAllowedOriginPattern("*"); // 开发环境临时允许所有来源
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // 允许携带凭证（Token/Cookie）
        config.setAllowCredentials(true);
        // 预检请求缓存1小时，减少OPTIONS请求
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 注册拦截器，按接口路径分流鉴权
     * 
     * 管理端接口：除注册/登录外全部拦截，Token无效直接401
     * 小程序接口：除登录外全部拦截，openid无效让用户重新登录
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 管理端拦截器：校验JWT Token
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/register", "/api/admin/login");

        // 小程序端拦截器：校验openid（排除安保端路径，由专用拦截器处理）
        registry.addInterceptor(wxAuthInterceptor)
                .addPathPatterns("/api/wx/**")
                .excludePathPatterns("/api/wx/user/login", "/api/wx/security/**");

        // 安保端专用拦截器：校验 Token + userType=3
        registry.addInterceptor(wxSecurityAuthInterceptor)
                .addPathPatterns("/api/wx/security/**");
    }
}