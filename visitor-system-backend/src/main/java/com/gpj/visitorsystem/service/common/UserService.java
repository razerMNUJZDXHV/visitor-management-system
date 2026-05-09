package com.gpj.visitorsystem.service.common;

import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 【业务模块】用户通用服务
 *
 * 【核心职责】
 * 1. 提供用户基础查询与写入能力
 * 2. 统一处理封禁状态的自动解封
 * 3. 封装Mapper，避免直接暴露持久层
 *
 * 【关键业务场景】
 * 用户查询时自动判断封禁是否过期，过期即清除封禁与爽约次数，
 * 无需额外定时任务。
 *
 * 【依赖说明】
 * - UserMapper：用户数据持久化
 *
 * 【注意事项】
 * - 解封逻辑在findById中触发，需确保更新写库
 * - 管理端/小程序端均应通过本服务访问用户数据
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 根据ID查询用户
     *
     * 【业务逻辑】
     * 1. 查询用户信息
     * 2. 若封禁已过期则清除封禁状态与爽约次数
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findById(Integer userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            // 封禁已过期，自动解封
            if (user.getBannedUntil() != null && now.isAfter(user.getBannedUntil())) {
                user.setMissedCount(0);
                user.setBannedUntil(null);
                userMapper.update(user);  // 更新数据库
            }
        }
        return user;
    }

    /**
     * 根据手机号查用户
     *
     * 【业务逻辑】
     * 用手机号查询用户，支持匹配管理员预添加账号。
     *
     * 【参数说明】
     * @param phone 手机号
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    /**
     * 根据openid查用户
     *
     * 【业务逻辑】
     * 小程序端使用openid判断用户是否已注册。
     *
     * 【参数说明】
     * @param openid 微信openid
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findByOpenid(String openid) {
        return userMapper.findByOpenid(openid);
    }

    /**
     * 新增用户
     *
     * 【业务逻辑】
     * 写入用户数据。
     *
     * 【参数说明】
     * @param user 用户实体
     *
     * 【返回值】
     * @return 影响行数
     */
    public int insert(User user) {
        return userMapper.insert(user);
    }

    /**
     * 更新用户
     *
     * 【业务逻辑】
     * 更新用户基础信息。
     *
     * 【参数说明】
     * @param user 用户实体
     *
     * 【返回值】
     * @return 影响行数
     */
    public int update(User user) {
        return userMapper.update(user);
    }
}
