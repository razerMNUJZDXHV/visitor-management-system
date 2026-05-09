package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.admin.AdminStatsDTO;
import com.gpj.visitorsystem.service.admin.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【业务模块】管理后台统计接口
 *
 * 【核心职责】
 * 1. 提供仪表盘统计数据接口
 * 2. 支持按天数调整统计范围
 *
 * 【关键业务场景】
 * 管理端首页需要统一的统计入口，按days参数切换展示范围。
 *
 * 【依赖说明】
 * - AdminStatsService：统计数据计算与汇总
 *
 * 【注意事项】
 * - days默认7天，前端可传入调整范围
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private AdminStatsService adminStatsService;

    /**
     * 获取统计数据
     *
     * 【接口说明】
     * 返回管理端仪表盘所需的统计数据。
     *
     * 【请求参数】
     * @param days 统计天数（默认7天）
     *
     * 【返回值】
     * @return 统计数据DTO，包含各类统计信息
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping
    public ResultDTO<AdminStatsDTO> stats(@RequestParam(defaultValue = "7") int days) {
        return ResultDTO.success(adminStatsService.getStats(days));
    }
}
