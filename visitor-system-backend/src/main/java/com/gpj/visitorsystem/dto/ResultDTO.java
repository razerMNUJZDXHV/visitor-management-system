package com.gpj.visitorsystem.dto;

import lombok.Data;

/**
 * 【业务模块】统一接口返回结果封装
 *
 * 【核心职责】
 * 1. 统一所有接口的返回格式，前端统一处理
 * 2. 封装状态码（code）、提示信息（msg）、业务数据（data）
 * 3. 提供便捷的静态工厂方法快速构建响应
 *
 * 【关键业务场景】
 * 1. Controller层所有接口返回ResultDTO，前端根据code判断成功/失败
 * 2. code=200表示成功，前端取data处理业务数据
 * 3. code非200表示失败，前端取msg展示错误信息给用户
 * 4. 支持泛型，不同接口返回不同的data类型
 *
 * 【依赖说明】
 * - 被所有Controller使用：统一返回格式
 *
 * 【注意事项】
 * - code=200固定为成功状态码，前端必须以此判断
 * - msg字段在失败时必填，用于前端弹窗提示
 * - data在失败时可为null，前端需做空值处理
 * - 提供success/error/of三个静态工厂方法，禁止直接new对象
 */
@Data
public class ResultDTO<T> {
    private Integer code;   // 状态码：200成功，其他失败
    private String msg;     // 提示信息
    private T data;        // 返回数据

    /**
     * 成功返回
     * 前端判断code===200就处理data
     */
    public static <T> ResultDTO<T> success(T data) {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    /**
     * 失败返回（默认500）
     * 用于业务异常，如"预约时间冲突"、"爽约次数超限"
     */
    public static <T> ResultDTO<T> error(String msg) {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    /**
     * 失败返回（自定义状态码）
     * 如401未授权、403无权限等
     */
    public static <T> ResultDTO<T> error(Integer code, String msg) {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}