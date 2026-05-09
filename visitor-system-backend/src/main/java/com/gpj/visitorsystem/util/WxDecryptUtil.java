package com.gpj.visitorsystem.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 【业务模块】微信加密数据解密工具
 *
 * 【核心职责】
 * 1. 解密微信小程序返回的加密手机号数据
 * 2. 处理微信特有的URL安全Base64编码
 * 3. 使用AES-CBC算法解密，返回JSON格式手机号信息
 *
 * 【关键业务场景】
 * 1. 小程序登录时获取用户手机号，微信返回encryptedData和iv
 * 2. 后端用session_key解密，获取用户真实手机号
 * 3. 解密后的JSON包含phoneNumber、purePhoneNumber、countryCode等字段
 * 4. 手机号用于匹配管理员预添加的账号或创建新访客
 *
 * 【依赖说明】
 * - 被WxCommonUserService使用：解密手机号完成绑定
 * - 使用Java标准javax.crypto包进行AES解密
 *
 * 【注意事项】
 * - 微信返回的Base64是URL安全的（-_代替+/），需先替换为标准Base64
 * - session_key、encryptedData、iv三个参数缺一不可
 * - session_key有效期与微信登录session一致，过期需重新登录
 * - 解密失败可能原因：session_key错误、数据篡改、格式不正确
 * - 解密后的JSON格式固定，需用fastjson或jackson解析
 */
public class WxDecryptUtil {

    /**
     * 解密微信手机号数据
     * 
     * 踩过的坑：微信返回的是URL安全的Base64，Java默认不支持，必须先替换字符。
     * session_key、encryptedData、iv三个参数缺一不可，前端传过来的是什么就用什么。
     * 
     * @param sessionKey 微信session_key（URL安全的Base64）
     * @param encryptedData 加密的手机号数据（URL安全的Base64）
     * @param iv 初始向量（URL安全的Base64）
     * @return 解密后的JSON字符串，包含phoneNumber字段
     */
    public static String decryptPhoneNumber(String sessionKey, String encryptedData, String iv) throws Exception {
        // 微信用的是URL安全的Base64（-代替+，_代替/），Java不认，得先转回来
        String stdSessionKey = convertToStandardBase64(sessionKey);
        String stdEncryptedData = convertToStandardBase64(encryptedData);
        String stdIv = convertToStandardBase64(iv);

        // 解码Base64，拿到原始字节
        byte[] sessionKeyBytes = Base64.getDecoder().decode(stdSessionKey);
        byte[] encryptedDataBytes = Base64.getDecoder().decode(stdEncryptedData);
        byte[] ivBytes = Base64.getDecoder().decode(stdIv);

        // AES/CBC/PKCS5Padding 是微信用的加密模式
        SecretKeySpec keySpec = new SecretKeySpec(sessionKeyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedDataBytes);
        // 解密后是UTF-8编码的JSON字符串
        return new String(decryptedBytes, "UTF-8");
    }

    /**
     * 把微信的URL安全Base64转成标准Base64
     * 微信为了不让+和/出现在URL里，用-和_代替了
     */
    private static String convertToStandardBase64(String base64) {
        return base64.replace('-', '+').replace('_', '/');
    }
}