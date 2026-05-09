package com.gpj.visitorsystem.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 【业务模块】AES加解密工具
 *
 * 【核心职责】
 * 1. 对敏感信息（如身份证号）进行AES加密存储
 * 2. 在需要展示时解密还原明文
 * 3. 保护用户隐私，防止数据库泄露导致敏感信息暴露
 *
 * 【关键业务场景】
 * 1. 访客提交预约时，身份证号用AES加密后存入数据库
 * 2. 安保核验时解密展示身份证号，核对访客身份
 * 3. 管理端导出预约记录时解密身份证号
 * 4. 其他场景（如列表查询）只展示密文或脱敏信息
 *
 * 【依赖说明】
 * - 使用Java标准javax.crypto包进行AES加密
 * - 被WxVisitorAppointmentService使用：加密存储身份证号
 * - 被WxSecurityAccessService使用：解密展示身份证号
 * - 被AdminExportService使用：解密后导出
 *
 * 【注意事项】
 * - key配置在application.yml的app.aes.key，必须是16/24/32字节
 * - 使用AES/ECB/PKCS5Padding算法，key长度决定加密强度
 * - 生产环境必须修改默认key，否则加密形同虚设
 * - 加密后的数据为Base64字符串，便于数据库存储
 * - 解密失败时返回null，调用方需做空值处理
 */
@Component
public class AesEncryptUtil {

    @Value("${app.aes.key}")
    private String key;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * AES加密
     * 
     * 身份证号入库前调用这个加密。
     * 空值直接返回，避免加密空字符串。
     * 
     * @param content 明文（如身份证号）
     * @return Base64编码的密文
     */
    public String encrypt(String content) throws Exception {
        if (content == null || content.trim().isEmpty()) {
            return content;  // 空值直接返回，不加密
        }
        // 用配置的key生成密钥
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(content.getBytes("UTF-8"));
        // 密文转Base64，方便存储到数据库
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES解密
     * 
     * 安保核验时调用这个解密身份证号。
     * 
     * @param encryptedContent Base64编码的密文
     * @return 明文
     */
    public String decrypt(String encryptedContent) throws Exception {
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            return encryptedContent;  // 空值直接返回
        }
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        // 先Base64解码，再解密
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedContent));
        return new String(decrypted, "UTF-8");
    }
}