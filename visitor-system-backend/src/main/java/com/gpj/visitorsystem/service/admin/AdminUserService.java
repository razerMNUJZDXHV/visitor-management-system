package com.gpj.visitorsystem.service.admin;

import com.gpj.visitorsystem.dto.PageResultDTO;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.mapper.UserMapper;
import com.gpj.visitorsystem.service.common.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【业务模块】管理后台用户管理
 *
 * 【核心职责】
 * 1. 管理员注册与登录校验
 * 2. 管理端用户增删改查与分页
 * 3. 管理员账号密码加密与权限校验
 *
 * 【关键业务场景】
 * 管理员在后台预添加审批人/安保账号（仅手机号/姓名），
 * 小程序端首次登录时通过手机号匹配绑定openid，避免重复用户。
 *
 * 【依赖说明】
 * - UserService：通用用户查询与封禁清理逻辑
 * - UserMapper：用户管理持久化与分页查询
 * - PasswordEncoder：管理员密码加密与校验
 *
 * 【注意事项】
 * - 管理员userType=4，仅管理员可登录后台
 * - 修改手机号会清空openid/sessionKey，需重新登录小程序
 * - 注册、创建、更新、删除涉及多表/多步骤时需加事务
 */
@Service
public class AdminUserService {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;

    /**
     * 管理员注册
     *
     * 【业务背景】
     * 管理端新增管理员账号，需要校验手机号唯一并加密密码。
     *
     * 【实现步骤】
     * 1. 校验手机号是否已注册
     * 2. 构建管理员用户（userType=4）
     * 3. 加密密码并保存
     *
     * 【参数说明】
     * @param phone 手机号
     * @param realName 真实姓名
     * @param rawPassword 原始密码
     *
     * 【返回值】
     * @return 新增管理员用户
     *
     * 【异常情况】
     * @throws BusinessException 手机号已注册
     *
     * 【事务说明】
     * 使用@Transactional保证写入原子性
     */
    @Transactional
    public User adminRegister(String phone, String realName, String rawPassword) {
        logger.info("开始注册管理员，手机号：{}", phone);

        if (userService.findByPhone(phone) != null) {
            throw new BusinessException(400, "手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setRealName(realName);
        user.setUserType(4);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userService.insert(user);

        logger.info("管理员注册成功，手机号：{}", phone);
        return user;
    }

    /**
     * 管理员登录
     *
     * 【业务背景】
     * 管理员通过手机号与密码登录后台。
     *
     * 【实现步骤】
     * 1. 根据手机号查询用户
     * 2. 校验用户类型为管理员
     * 3. 校验密码是否匹配
     *
     * 【参数说明】
     * @param phone 手机号
     * @param rawPassword 原始密码
     *
     * 【返回值】
     * @return 管理员用户信息
     *
     * 【异常情况】
     * @throws BusinessException 手机号或密码错误
     *
     * 【事务说明】
     * 无事务（只读查询）
     */
    public User adminLogin(String phone, String rawPassword) {
        User user = userService.findByPhone(phone);
        if (user == null || user.getUserType() != 4) {
            throw new BusinessException(400, "手机号或密码错误");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(400, "手机号或密码错误");
        }

        logger.info("管理员登录成功，userId: {}", user.getUserId());
        return user;
    }

    /**
     * 管理员创建用户（可创建各类用户）
     *
     * 【业务背景】
     * 管理端统一创建访客、审批人、安保、管理员账号。
     *
     * 【实现步骤】
     * 1. 校验手机号唯一性
     * 2. 管理员账号加密密码，其它角色清空密码
     * 3. 写入用户表
     *
     * 【参数说明】
     * @param user 用户实体
     *
     * 【返回值】
     * @return 创建后的用户
     *
     * 【异常情况】
     * @throws BusinessException 手机号已被注册或管理员密码为空
     *
     * 【事务说明】
     * 使用@Transactional保证写入原子性
     */
    @Transactional
    public User createUser(User user) {
        logger.info("创建用户，phone: {}, type: {}", user.getPhone(), user.getUserType());

        if (userService.findByPhone(user.getPhone()) != null) {
            throw new BusinessException(400, "手机号已被注册");
        }

        if (user.getUserType() == 4) {
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                throw new BusinessException(400, "管理员密码不能为空");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        userService.insert(user);
        logger.info("用户创建成功，userId: {}", user.getUserId());
        return user;
    }

    /**
     * 查询用户列表（不分页）
     *
     * 【业务逻辑】
     * 1. 按手机号/姓名/用户类型筛选
     * 2. 返回匹配列表
     *
     * 【参数说明】
     * @param phone 手机号（可选）
     * @param realName 真实姓名（可选）
     * @param userType 用户类型（可选）
     *
     * 【返回值】
     * @return 用户列表
     */
    public List<User> listUsers(String phone, String realName, Integer userType) {
        return userMapper.listUsers(phone, realName, userType);
    }

    /**
     * 更新用户信息
     *
     * 【业务背景】
     * 管理端修改用户手机号、姓名、角色或密码。
     *
     * 【实现步骤】
     * 1. 校验用户是否存在
     * 2. 手机号变更时校验唯一性并清空openid/sessionKey
     * 3. 密码非空则加密，否则不更新
     * 4. 更新用户信息并返回最新数据
     *
     * 【参数说明】
     * @param user 用户实体（包含待更新字段）
     *
     * 【返回值】
     * @return 更新后的用户
     *
     * 【异常情况】
     * @throws BusinessException 用户不存在或手机号已被注册
     *
     * 【事务说明】
     * 使用@Transactional保证更新原子性
     */
    @Transactional
    public User updateUser(User user) {
        logger.info("修改用户，userId: {}", user.getUserId());
        User oldUser = userService.findById(user.getUserId());
        if (oldUser == null) {
            throw new BusinessException(400, "用户不存在");
        }

        if (user.getPhone() != null && !user.getPhone().equals(oldUser.getPhone())) {
            if (userService.findByPhone(user.getPhone()) != null) {
                throw new BusinessException(400, "新手机号已被注册");
            }
            user.setOpenid(null);
            user.setSessionKey(null);
        }

        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        userMapper.updateUser(user);
        return userService.findById(user.getUserId());
    }

    /**
     * 删除用户
     *
     * 【业务背景】
     * 管理端删除用户，删除前需校验存在性。
     *
     * 【实现步骤】
     * 1. 校验用户是否存在
     * 2. 删除用户记录
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 无
     *
     * 【异常情况】
     * @throws BusinessException 用户不存在
     *
     * 【事务说明】
     * 使用@Transactional保证删除原子性
     */
    @Transactional
    public void deleteUserById(Integer userId) {
        logger.info("删除用户，userId: {}", userId);
        if (userService.findById(userId) == null) {
            throw new BusinessException(400, "用户不存在");
        }
        userMapper.deleteUserById(userId);
    }

    /**
     * 分页查询用户
     *
     * 【业务逻辑】
     * 1. 计算分页偏移量
     * 2. 查询分页数据与总数
     * 3. 组装分页结果
     *
     * 【参数说明】
     * @param phone 手机号（可选）
     * @param realName 真实姓名（可选）
     * @param userType 用户类型（可选）
     * @param bannedStatus 封禁状态（可选，0-正常 1-封禁中）
     * @param pageNum 页码
     * @param pageSize 每页条数
     *
     * 【返回值】
     * @return 分页结果
     */
    public PageResultDTO<User> listUsersPage(String phone, String realName, Integer userType,
                                             Integer bannedStatus,
                                             Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        LocalDateTime now = LocalDateTime.now();
        List<User> list = userMapper.listUsersPage(phone, realName, userType, bannedStatus, now, offset, pageSize);
        Long total = userMapper.countUsers(phone, realName, userType, bannedStatus, now);
        return PageResultDTO.of(list, total);
    }

    /**
     * 统计管理员数量
     *
     * 【业务逻辑】
     * 统计userType=4的用户数量。
     *
     * 【参数说明】
     * 无
     *
     * 【返回值】
     * @return 管理员数量
     */
    public Long countAdminUsers() {
        return userMapper.countUsers(null, null, 4, null, LocalDateTime.now());
    }

    /**
     * 根据ID查询用户
     *
     * 【业务逻辑】
     * 调用通用用户服务查询并返回用户信息。
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【返回值】
     * @return 用户信息
     */
    public User findById(Integer userId) {
        return userService.findById(userId);
    }

    /**
     * 手动解除访客封禁
     *
     * 【业务逻辑】
     * 仅允许解除访客(userType=1)的封禁，清空 banned_until 和 missed_count。
     *
     * 【参数说明】
     * @param userId 用户ID
     *
     * 【异常情况】
     * @throws BusinessException 用户不存在、非访客角色、未处于封禁状态
     */
    @Transactional
    public void unbanUser(Integer userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getUserType() == null || user.getUserType() != 1) {
            throw new BusinessException("仅访客角色支持手动解封");
        }
        if (user.getBannedUntil() == null) {
            throw new BusinessException("该用户未处于封禁状态");
        }
        user.setBannedUntil(null);
        user.setMissedCount(0);
        userMapper.update(user);
        logger.info("管理员手动解封访客 userId={}", userId);
    }
}
