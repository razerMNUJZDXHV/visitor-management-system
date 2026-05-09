package com.gpj.visitorsystem.exception;

import com.gpj.visitorsystem.dto.ResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 【业务模块】全局异常处理器
 *
 * 【核心职责】
 * 1. 统一捕获和处理所有Controller抛出的异常
 * 2. 业务异常（BusinessException）返回具体错误码和提示
 * 3. 系统异常（Exception）返回500，不打乱用户，只记录日志
 * 4. 提供统一的错误响应格式，前端统一处理
 *
 * 【关键业务场景】
 * 1. 参数校验失败：返回400错误码和具体校验错误信息
 * 2. 业务逻辑错误（如预约时间冲突）：返回具体错误码和提示
 * 3. 权限不足：返回403无权限访问
 * 4. 系统异常（如数据库连接失败）：返回500，只记录日志不暴露细节
 * 5. 所有异常统一封装为ResultDTO格式返回
 *
 * 【依赖说明】
 * - 使用@RestControllerAdvice自动扫描所有Controller
 * - 捕获BusinessException：业务预期异常
 * - 捕获Exception：系统非预期异常
 *
 * 【注意事项】
 * - 业务异常返回具体错误码，前端根据code做不同处理
 * - 系统异常统一返回500，避免暴露内部细节（如SQL语句、堆栈）
 * - 系统异常只记录error日志，不返回给前端
 * - 参数校验异常（MethodArgumentNotValidException）需单独处理
 * - 异常处理顺序：先匹配具体异常类型，再匹配父类Exception
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * 业务异常是预期的（如参数校验失败、预约冲突），warn级别就够了
     */
    @ExceptionHandler(BusinessException.class)
    public ResultDTO<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        // 把异常里的code和message返回给前端
        return ResultDTO.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理系统异常
     * 系统异常是未预期的（如数据库挂了、空指针），error级别，要查日志
     */
    @ExceptionHandler(Exception.class)
    public ResultDTO<Void> handleException(Exception e) {
        log.error("系统异常", e);
        // 系统异常不给前端看详细错误，只说"系统繁忙"
        return ResultDTO.error(500, "系统繁忙，请稍后重试");
    }
}