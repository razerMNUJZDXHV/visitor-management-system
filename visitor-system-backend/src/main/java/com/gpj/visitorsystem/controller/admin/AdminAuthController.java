package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.admin.AdminLoginRequestDTO;
import com.gpj.visitorsystem.dto.admin.AdminLoginResponseDTO;
import com.gpj.visitorsystem.dto.admin.AdminRegisterRequestDTO;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.service.admin.AdminUserService;
import com.gpj.visitorsystem.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【业务模块】管理后台认证
 *
 * 【核心职责】
 * 1. 管理员注册
 * 2. 管理员登录
 * 3. 生成后台访问JWT Token
 *
 * 【关键业务场景】
 * 注册/登录接口必须放行拦截器，否则管理员无法进入系统。
 *
 * 【依赖说明】
 * - AdminUserService：管理员注册与登录逻辑
 * - JwtUtils：JWT Token生成
 *
 * 【注意事项】
 * - 登录成功后返回Token，前端需保存并在Header携带
 * - 管理员userType=4，仅管理员可登录后台
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 管理员注册
     *
     * 【接口说明】
     * 管理员账号注册入口，仅用于后台管理账号创建。
     *
     * 【请求参数】
     * @param request 注册请求DTO，包含phone、realName、password
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 手机号已注册：返回“手机号已注册”
     */
    @PostMapping("/register")
    public ResultDTO<Void> register(@Valid @RequestBody AdminRegisterRequestDTO request) {
        adminUserService.adminRegister(request.getPhone(), request.getRealName(), request.getPassword());
        return ResultDTO.success(null);
    }

    /**
     * 管理员登录
     *
     * 【接口说明】
     * 管理员账号登录，成功后返回后台访问Token。
     *
     * 【请求参数】
     * @param request 登录请求DTO，包含phone、password
     *
     * 【返回值】
     * @return 登录响应DTO，包含userId、phone、realName、userType、token
     *
     * 【异常情况】
     * - 手机号或密码错误：返回“手机号或密码错误”
     * - 非管理员账号：返回“手机号或密码错误”
     */
    @PostMapping("/login")
    public ResultDTO<AdminLoginResponseDTO> login(@Valid @RequestBody AdminLoginRequestDTO request) {
        User user = adminUserService.adminLogin(request.getPhone(), request.getPassword());

        AdminLoginResponseDTO response = new AdminLoginResponseDTO();
        response.setUserId(user.getUserId());
        response.setPhone(user.getPhone());
        response.setRealName(user.getRealName());
        response.setUserType(user.getUserType());
        response.setToken(jwtUtils.generateToken(user.getUserId(), user.getUserType()));

        return ResultDTO.success(response);
    }
}