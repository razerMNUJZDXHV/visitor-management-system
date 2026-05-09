package com.gpj.visitorsystem.controller.wx;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.wx.*;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.service.wx.WxCommonUserService;
import com.gpj.visitorsystem.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 【业务模块】小程序通用用户接口
 *
 * 【核心职责】
 * 1. 小程序登录与手机号绑定
 * 2. 刷新access token
 * 3. 查询用户封禁状态
 *
 * 【关键业务场景】
 * 新版登录支持一次性完成登录+手机号绑定，旧版仅传code仍兼容。
 *
 * 【依赖说明】
 * - WxCommonUserService：登录、绑定与封禁状态查询
 * - JwtUtils：JWT Token生成
 *
 * 【注意事项】
 * - 登录接口不走Token校验，其它接口需校验
 * - access token有效期2小时，refresh token有效期7天
 */
@RestController
@RequestMapping("/api/wx/user")
public class WxCommonUserController {
    @Autowired
    private WxCommonUserService wxCommonUserService;
    @Autowired
    private JwtUtils jwtUtils;

    @Value("${wechat.appid}")
    private String appid;
    @Value("${wechat.secret}")
    private String secret;


    /**
     * 小程序微信登录
     *
     * 【接口说明】
     * 前端调用wx.login获取code后调用此接口换取用户信息和token，
     * 若携带encryptedData和iv则一次性完成手机号绑定。
     *
     * 【请求参数】
     * @param request 登录请求DTO，包含code、encryptedData、iv等
     *
     * 【返回值】
     * @return 登录响应DTO，包含userId、openid、userType、token、refreshToken
     *
     * 【异常情况】
     * - code无效/过期：返回“微信登录失败”
     * - 管理员账号登录：返回“管理员请使用Web端登录”
     */
    @PostMapping("/login")
    public ResultDTO<WxLoginResponseDTO> login(@Valid @RequestBody WxLoginRequestDTO request) throws Exception {
        User user;
        
        // 如果提供了手机号加密数据，使用新版一次性登录
        if (request.getEncryptedData() != null && request.getIv() != null) {
            user = wxCommonUserService.wxLoginWithPhone(
                request.getCode(), 
                request.getEncryptedData(), 
                request.getIv(), 
                appid, 
                secret
            );
        } else {
            // 旧版：只传code（兼容旧版本前端）
            user = wxCommonUserService.wxLogin(request.getCode(), appid, secret);
        }
        
        // 生成 access token 和 refresh token
        String accessToken = jwtUtils.generateToken(user.getUserId(), user.getUserType());
        String refreshToken = wxCommonUserService.generateAndSaveRefreshToken(user.getUserId());
        
        WxLoginResponseDTO response = new WxLoginResponseDTO();
        response.setUserId(user.getUserId());
        response.setOpenid(user.getOpenid());
        response.setUserType(user.getUserType());
        response.setPhone(user.getPhone());
        response.setRealName(user.getRealName());
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        
        return ResultDTO.success(response);
    }

    /**
     * 刷新 access token
     *
     * 【接口说明】
     * access token过期后，使用refresh token换取新token。
     *
     * 【请求参数】
     * @param request 请求体，包含refreshToken
     *
     * 【返回值】
     * @return 包含accessToken和refreshToken的Map
     *
     * 【异常情况】
     * - refreshToken无效或过期：返回对应错误信息
     */
    @PostMapping("/refresh-token")
    public ResultDTO<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResultDTO.error("refresh token 不能为空");
            }
            
            Map<String, String> result = wxCommonUserService.refreshAccessToken(refreshToken);
            return ResultDTO.success(result);
        } catch (Exception e) {
            return ResultDTO.error(e.getMessage());
        }
    }

    /**
     * 获取用户禁止预约状态
     *
     * 【接口说明】
     * 前端在创建预约前调用此接口检查是否处于封禁期。
     *
     * 【请求参数】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 包含bannedUntil（封禁截止时间）和missedCount（爽约次数）的Map
     *
     * 【异常情况】
     * - 用户不存在：返回默认值（未封禁、爽约次数0）
     */
    @GetMapping("/ban-status")
    public ResultDTO<Map<String, Object>> getBanStatus(@RequestParam Integer userId) {
        return ResultDTO.success(wxCommonUserService.getBanStatus(userId));
    }
}
