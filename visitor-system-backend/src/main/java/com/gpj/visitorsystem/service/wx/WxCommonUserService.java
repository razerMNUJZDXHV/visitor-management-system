package com.gpj.visitorsystem.service.wx;

import com.alibaba.fastjson.JSONObject;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.service.common.UserService;
import com.gpj.visitorsystem.util.JwtUtils;
import com.gpj.visitorsystem.util.WxDecryptUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【业务模块】小程序通用用户服务
 *
 * 【核心职责】
 * 1. 处理小程序端微信登录（新用户注册、老用户更新session_key）
 * 2. 处理手机号授权与绑定（含管理员预置账号绑定）
 * 3. 查询用户禁止预约状态与刷新Token
 *
 * 【关键业务场景】
 * 安保/审批人账号由管理员后台预添加（仅手机号、无openid），
 * 用户首次小程序登录需通过手机号匹配已有账号并绑定openid，避免重复用户。
 *
 * 【依赖说明】
 * - UserService：用户基础CRUD与封禁清理
 * - UserMapper：用户查询与更新
 * - WxDecryptUtil：微信手机号解密
 * - JwtUtils：JWT Token生成与校验
 *
 * 【注意事项】
 * - 管理员（userType=4）禁止小程序登录
 * - session_key使用ConcurrentHashMap缓存，避免频繁调用微信接口
 * - 关键更新操作需加@Transactional保证原子性
 */
@Service
public class WxCommonUserService {
    private static final Logger logger = LoggerFactory.getLogger(WxCommonUserService.class);
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtils jwtUtils;

    private static final Map<String, String> SESSION_KEY_MAP = new ConcurrentHashMap<>();
    private static final Object REFRESH_TOKEN_LOCK = new Object();

    /**
     * 小程序微信登录（旧版，仅获取openid）
     *
     * 【业务背景】
     * 旧版登录仅获取openid，需兼容管理员预添加账号的手机号绑定场景。
     *
     * 【实现步骤】
     * 1. 调用微信jscode2session接口获取openid与session_key
     * 2. 根据openid查询用户，未找到则创建临时访客
     * 3. 缓存session_key用于后续手机号解密
     *
     * 【参数说明】
     * @param code 微信登录返回的code
     * @param appid 小程序appid
     * @param secret 小程序secret
     *
     * 【返回值】
     * @return 登录后的用户信息
     *
     * 【异常情况】
     * @throws Exception 微信接口调用失败、code过期或管理员账号登录
     *
     * 【事务说明】
     * 使用@Transactional保证用户创建与更新原子性
     */
    @Transactional
    public User wxLogin(String code, String appid, String secret) throws Exception {
        JSONObject json = fetchWeChatSession(code, appid, secret);
        String openid = json.getString("openid");
        String sessionKey = json.getString("session_key");
        logger.info("微信登录成功，openid: {}, sessionKey: {}", openid, sessionKey);

        User user = userService.findByOpenid(openid);
        if (user == null) {
            logger.info("openid {} 未找到已有用户，创建新访客", openid);
            user = new User();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            user.setPhone(null);
            user.setRealName(null);
            user.setUserType(1);
            userService.insert(user);
            logger.info("新访客自动注册成功，userId: {}", user.getUserId());
        } else if (user.getUserType() == 4) {
            throw new RuntimeException("管理员请使用 Web 端登录");
        } else {
            user.setSessionKey(sessionKey);
            userService.update(user);
        }

        SESSION_KEY_MAP.put(openid, sessionKey);
        return user;
    }

    /**
     * 调用微信 jscode2session 接口，获取 openid 和 session_key
     *
     * 【业务背景】
     * wxLogin() 和 wxLoginWithPhone() 都需要调用此接口，
     * 提取为公共方法避免重复代码。
     *
     * 【实现逻辑】
     * 1. 构造请求 URL（appid + secret + code）
     * 2. 发送 HTTP GET 请求
     * 3. 解析响应 JSON，校验错误码
     * 4. 返回包含 openid 和 session_key 的 JSONObject
     *
     * 【参数说明】
     * @param code 微信登录返回的 code
     * @param appid 小程序 appid
     * @param secret 小程序 secret
     *
     * 【返回值】
     * @return 微信接口返回的 JSON 对象（含 openid、session_key）
     *
     * 【异常情况】
     * @throws IOException 网络请求失败
     * @throws RuntimeException 微信返回错误码
     */
    private JSONObject fetchWeChatSession(String code, String appid, String secret) throws IOException {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
                + "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String result = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject json = JSONObject.parseObject(result);
                if (json.containsKey("errcode")) {
                    throw new RuntimeException("微信登录失败: " + json.getString("errmsg"));
                }
                return json;
            }
        }
    }

    /**
     * 小程序微信登录（新版，一次性完成登录+手机号绑定）
     *
     * 【业务背景】
     * 新版登录合并openid获取与手机号绑定，减少步骤并避免数据不一致。
     *
     * 【实现步骤】
     * 1. 调用jscode2session获取openid与session_key
     * 2. 解密手机号并按手机号查询用户
     * 3. 已存在用户更新openid/session_key，否则创建新访客
     * 4. 缓存session_key并返回用户信息
     *
     * 【参数说明】
     * @param code 微信登录返回的code
     * @param encryptedData 手机号加密数据
     * @param iv 加密初始向量
     * @param appid 小程序appid
     * @param secret 小程序secret
     *
     * 【返回值】
     * @return 登录后的用户信息
     *
     * 【异常情况】
     * @throws Exception 微信接口调用失败、解密失败或管理员账号登录
     *
     * 【事务说明】
     * 使用@Transactional保证用户创建与更新原子性
     */
    @Transactional
    public User wxLoginWithPhone(String code, String encryptedData, String iv, String appid, String secret) throws Exception {
        // 1. 获取微信 openid 和 sessionKey（复用通用方法）
        JSONObject json = fetchWeChatSession(code, appid, secret);
        String openid = json.getString("openid");
        String sessionKey = json.getString("session_key");

        // 2. 解密手机号
        String decrypted = WxDecryptUtil.decryptPhoneNumber(sessionKey, encryptedData, iv);
        JSONObject phoneJson = JSONObject.parseObject(decrypted);
        String phoneNumber = phoneJson.getString("phoneNumber");
        logger.info("解密手机号成功，phoneNumber: {}", phoneNumber);

        // 3. 根据手机号查找用户（管理员可能已添加）
        User user = userService.findByPhone(phoneNumber);
        
        if (user != null) {
            // 已存在该手机号的用户（如安保、审批人），直接更新 openid 和 sessionKey
            logger.info("手机号 {} 已存在，更新用户 userId: {} 的 openid 和 sessionKey", phoneNumber, user.getUserId());
            
            if (user.getUserType() == 4) {
                throw new RuntimeException("管理员请使用 Web 端登录");
            }
            
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            userService.update(user);
            
            SESSION_KEY_MAP.put(openid, sessionKey);
            return user;
        } else {
            // 不存在该手机号的用户，创建新访客
            logger.info("手机号 {} 不存在，创建新访客", phoneNumber);
            
            user = new User();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            user.setPhone(phoneNumber);
            user.setRealName(null);
            user.setUserType(1);
            userService.insert(user);
            logger.info("新访客自动注册成功，userId: {}", user.getUserId());
            
            SESSION_KEY_MAP.put(openid, sessionKey);
            return user;
        }
    }

    /**
     * 生成并保存refresh token
     *
     * 【业务背景】
     * access token有效期短，需使用refresh token保持登录状态。
     *
     * 【实现步骤】
     * 1. 生成refresh token与过期时间
     * 2. 更新用户表refreshToken与过期时间
     * 3. 返回refresh token
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return refresh token字符串
     *
     * 【异常情况】
     * @throws Exception 数据库更新失败
     *
     * 【事务说明】
     * 使用@Transactional保证更新原子性
     */
    @Transactional
    public String generateAndSaveRefreshToken(Integer userId) {
        String refreshToken = jwtUtils.generateRefreshToken(userId);
        LocalDateTime expireTime = LocalDateTime.now().plusDays(7);
        
        User user = new User();
        user.setUserId(userId);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpireTime(expireTime);
        userService.update(user);
        
        logger.info("生成 refresh token，userId: {}, expireTime: {}", userId, expireTime);
        return refreshToken;
    }

    /**
     * 用refresh token换新的access token
     *
     * 【业务背景】
     * refresh token用于延长登录状态，需防并发刷新导致失效。
     *
     * 【实现步骤】
     * 1. 校验refresh token格式
     * 2. 解析userId并加锁避免并发刷新
     * 3. 校验数据库中的refresh token与过期时间
     * 4. 生成新的access/refresh token并更新数据库
     *
     * 【参数说明】
     * @param refreshToken 旧的refresh token
     *
     * 【返回值】
     * @return 新的accessToken与refreshToken
     *
     * 【异常情况】
     * @throws RuntimeException refresh token无效、已过期、不匹配或用户不存在
     *
     * 【事务说明】
     * 使用@Transactional保证更新原子性
     */
    @Transactional
    public Map<String, String> refreshAccessToken(String refreshToken) {
        // 1. 验证 refresh token 格式
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("refresh token 无效或已过期");
        }
        
        // 2. 从 token 获取 userId
        Integer userId = jwtUtils.getUserIdFromToken(refreshToken);
        
        // 3. 使用同步锁防止并发刷新导致 token 不一致
        synchronized (REFRESH_TOKEN_LOCK) {
            // 4. 查询用户并验证 refresh token
            User user = userService.findById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            if (!refreshToken.equals(user.getRefreshToken())) {
                throw new RuntimeException("refresh token 不匹配，可能已在其他设备刷新");
            }
            
            if (user.getRefreshTokenExpireTime() != null && 
                LocalDateTime.now().isAfter(user.getRefreshTokenExpireTime())) {
                throw new RuntimeException("refresh token 已过期，请重新登录");
            }
            
            // 5. 生成新的 access token 和 refresh token
            String newAccessToken = jwtUtils.generateToken(user.getUserId(), user.getUserType());
            String newRefreshToken = jwtUtils.generateRefreshToken(user.getUserId());
            LocalDateTime newExpireTime = LocalDateTime.now().plusDays(7);
            
            // 6. 更新数据库
            user.setRefreshToken(newRefreshToken);
            user.setRefreshTokenExpireTime(newExpireTime);
            userService.update(user);
            
            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            
            logger.info("刷新 token 成功，userId: {}", userId);
            return result;
        }
    }

    /**
     * 获取用户封禁状态
     *
     * 【业务逻辑】
     * 1. 查询用户信息
     * 2. 判断是否仍在封禁期
     * 3. 返回封禁截止时间与爽约次数
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 包含bannedUntil与missedCount的Map
     */
    public Map<String, Object> getBanStatus(Integer userId) {
        User user = userService.findById(userId);
        Map<String, Object> data = new HashMap<>();
        
        if (user == null) {
            data.put("bannedUntil", null);
            data.put("missedCount", 0);
            return data;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (user.getBannedUntil() != null && now.isBefore(user.getBannedUntil())) {
            data.put("bannedUntil", user.getBannedUntil().toString());
        } else {
            data.put("bannedUntil", null);
        }
        data.put("missedCount", user.getMissedCount() != null ? user.getMissedCount() : 0);
        return data;
    }
}