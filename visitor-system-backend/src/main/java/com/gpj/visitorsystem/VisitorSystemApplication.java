package com.gpj.visitorsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 【业务模块】访客预约与准入管理系统启动类
 *
 * 【核心职责】
 * 1. Spring Boot应用入口，负责启动整个系统
 * 2. @MapperScan自动扫描Mapper接口，无需每个Mapper加@Mapper注解
 * 3. @EnableScheduling开启定时任务（过期预约处理、爽约判定等）
 * 4. @SpringBootApplication启用自动配置、组件扫描
 *
 * 【关键业务场景】
 * 1. 系统启动时自动加载所有配置和Bean
 * 2. 定时任务自动执行：扫描过期预约、判定爽约、触发告警等
 * 3. MyBatis Mapper接口自动扫描并注册到Spring容器
 * 4. 嵌入式Tomcat启动，监听配置的端口号
 *
 * 【依赖说明】
 * - @MapperScan("com.gpj.visitorsystem.mapper")：扫描Mapper接口
 * - @EnableScheduling：开启Spring定时任务调度
 * - @SpringBootApplication：组合注解，启用自动配置
 *
 * 【注意事项】
 * - main方法为系统入口，直接运行即可启动
 * - 定时任务默认使用单线程调度器，如有性能需求需配置线程池
 * - 开发环境建议开启debug日志，生产环境建议关闭
 * - 启动失败常见原因：数据库连接失败、端口被占用、配置错误
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.gpj.visitorsystem.mapper")
public class VisitorSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisitorSystemApplication.class, args);
    }

}
 