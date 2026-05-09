package com.gpj.visitorsystem.exception;

/**
 * 【业务模块】自定义业务异常
 *
 * 【核心职责】
 * 1. 封装业务逻辑错误为异常对象
 * 2. 携带状态码（code），前端根据code做不同处理
 * 3. 继承RuntimeException，无需显式声明throws
 * 4. 被全局异常处理器捕获，统一返回给前端
 *
 * 【关键业务场景】
 * 1. 预约时间冲突：抛出BusinessException(500, "预约时间冲突")
 * 2. 爽约次数超限：抛出BusinessException(500, "爽约次数超限，已被封禁")
 * 3. 参数校验失败：抛出BusinessException(400, "参数错误")
 * 4. 权限不足：抛出BusinessException(403, "无权访问")
 * 5. 全局异常处理器捕获后，封装为ResultDTO返回
 *
 * 【依赖说明】
 * - 被GlobalExceptionHandler捕获：统一处理业务异常
 * - 被各Service层抛出：标识业务逻辑错误
 *
 * 【注意事项】
 * - code为状态码，前端根据code判断错误类型
 *   - 400：参数错误
 *   - 403：无权限
 *   - 404：资源不存在
 *   - 500：业务逻辑错误
 * - message为错误提示，前端直接展示给用户
 * - 与RuntimeException的区别：携带code，前端可区分处理
 * - 抛出后无需在方法上声明throws，代码更简洁
 */
public class BusinessException extends RuntimeException {

    private final int code;  // 状态码，默认400

    /**
     * 构造业务异常
     * @param code 状态码（如400-参数错误，403-无权限）
     * @param message 错误提示
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（默认code=400）
     */
    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}