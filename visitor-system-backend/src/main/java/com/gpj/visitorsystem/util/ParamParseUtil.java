package com.gpj.visitorsystem.util;

/**
 * 【工具类】参数解析工具
 *
 * 【核心职责】
 * 1. 解析整型参数（兼容前端可能传入的字符串）
 * 2. 解析布尔型参数（兼容前端可能传入的字符串）
 *
 * 【关键业务场景】
 * 前端可能传入字符串类型的参数（如"1"、"true"、"null"、"undefined"），
 * 后端需要统一解析为Java类型。
 *
 * 【注意事项】
 * - 解析失败或参数为空时返回null
 * - 支持"true"/"false"、"1"/"0"、"null"、"undefined"等格式
 */
public class ParamParseUtil {

    /**
     * 解析整型参数（兼容前端可能传入的字符串）
     *
     * @param paramStr 参数字符串（可能为空或"null"）
     * @return 整型值（解析失败或参数为空时返回null）
     */
    public static Integer parseIntegerParam(String paramStr) {
        if (paramStr == null || "null".equalsIgnoreCase(paramStr.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(paramStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析布尔型参数（兼容前端可能传入的字符串）
     * 支持：true/1 -> true, false/0 -> false
     *
     * @param paramStr 参数字符串（可能为空、"null"、"undefined"）
     * @return 布尔值（解析失败或参数为空时返回null）
     */
    public static Boolean parseBooleanParam(String paramStr) {
        if (paramStr == null) {
            return null;
        }
        String trimmed = paramStr.trim();
        if ("true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
            return true;
        } else if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
            return false;
        }
        // 其他情况（"undefined"、"null"、空字符串）都保持 null
        return null;
    }
}
