package com.gpj.visitorsystem.dto;

import lombok.Data;
import java.util.List;

/**
 * 【业务模块】分页查询结果封装
 *
 * 【核心职责】
 * 1. 封装分页查询的返回数据（当前页数据+总条数）
 * 2. 前端根据list渲染表格，根据total计算总页数和页码
 * 3. 提供静态工厂方法快速构建分页结果
 *
 * 【关键业务场景】
 * 1. 管理端用户列表、预约列表、通行记录列表等分页查询
 * 2. 前端根据total计算总页数：Math.ceil(total / pageSize)
 * 3. 结合MyBatis分页插件，先查总数再查数据，封装到PageResultDTO返回
 *
 * 【依赖说明】
 * - 被各Service层分页查询方法返回
 * - 被Controller层封装到ResultDTO中返回给前端
 *
 * 【注意事项】
 * - list为当前页实际数据，可能为空列表（最后一页或无数据）
 * - total为总记录数，不是总页数，前端需自行计算
 * - 分页查询建议统一在Mapper层拆分count和list两个SQL
 */
@Data
public class PageResultDTO<T> {
    private List<T> list;   // 当前页数据
    private Long total;     // 总条数（用来算总页数）

    /**
     * 快速构建分页结果
     */
    public static <T> PageResultDTO<T> of(List<T> list, Long total) {
        PageResultDTO<T> result = new PageResultDTO<>();
        result.setList(list);
        result.setTotal(total);
        return result;
    }
}