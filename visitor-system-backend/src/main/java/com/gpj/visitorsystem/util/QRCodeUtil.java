package com.gpj.visitorsystem.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 【业务模块】二维码生成工具
 *
 * 【核心职责】
 * 1. 生成包含JWT Token的二维码图片
 * 2. 返回Base64格式，前端直接用在img标签中展示
 * 3. 使用Google ZXing库生成高质量二维码
 *
 * 【关键业务场景】
 * 1. 预约审批通过后，生成包含appointment token的二维码
 * 2. 访客在小程序端展示二维码给安保扫码
 * 3. 安保扫码解析Token，直接获取预约信息，无需查数据库
 * 4. 二维码大小300x300，平衡扫描成功率和加载速度
 *
 * 【依赖说明】
 * - 使用Google ZXing库生成二维码
 * - 被WxVisitorAppointmentService使用：生成预约二维码
 *
 * 【注意事项】
 * - content为二维码内容，通常是JWT Token字符串
 * - 返回data:image/png;base64,...格式，前端直接用于img src
 * - 300x300是合适的大小，太小扫不出来，太大加载慢
 * - 二维码图片格式为PNG，支持透明背景
 * - 生成的二维码有效期与Token有效期一致
 */
public class QRCodeUtil {

    /**
     * 生成Base64格式的二维码图片
     * 
     * content里放JWT Token，安保扫码后后端解析Token拿到预约信息。
     * 300x300是合适的大小，太小扫不出来，太大加载慢。
     * 
     * @param content 二维码内容（JWT Token）
     * @return data:image/png;base64,... 格式，前端直接用在img src
     */
    public static String generateBase64QRCode(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            // 生成二维码矩阵，300x300像素
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // 写成PNG格式到内存
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);
            byte[] bytes = bos.toByteArray();
            // 转Base64，加上data URI前缀，前端直接用在img src
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}