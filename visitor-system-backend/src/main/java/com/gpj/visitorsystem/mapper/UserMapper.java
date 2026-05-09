package com.gpj.visitorsystem.mapper;

import com.gpj.visitorsystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 【业务模块】用户表Mapper
 *
 * 【核心职责】
 * 1. 用户数据的增删改查（CRUD）
 * 2. 小程序端用openid查用户，管理端用手机号/姓名/角色查用户
 * 3. 分页查询拆成listUsersPage（查数据）和countUsers（查总数）两次调用
 * 4. 支持按条件筛选和模糊搜索
 *
 * 【关键业务场景】
 * 1. 小程序登录：findByOpenid根据openid查用户，不存在则创建
 * 2. 手机号绑定：findByPhone根据手机号查用户，匹配预添加账号
 * 3. 管理端用户列表：listUsersPage分页查询，countUsers查总数
 * 4. 封禁状态查询：findById查询用户封禁状态和爽约次数
 * 5. 修改用户信息：update更新用户字段（手机号、密码、openid等）
 *
 * 【依赖说明】
 * - 对应数据库user表
 * - 被UserService使用：用户基础CRUD
 * - 被AdminUserService使用：管理端用户管理
 * - 被WxCommonUserService使用：小程序登录和绑定
 *
 * 【注意事项】
 * - openid字段有唯一索引（uk_openid），不能重复
 * - phone字段有唯一索引，不能重复
 * - 分页查询建议拆分count和list两个SQL，避免性能问题
 * - 修改手机号会清空openid/sessionKey，需重新登录
 * - 用户删除为逻辑删除还是物理删除，根据业务需求决定
 */
@Mapper
public interface UserMapper {

    // ==================== 小程序端方法 ====================

    /**
     * 根据openid查用户
     * 小程序登录后用openid查是否已有账号
     */
    User findByOpenid(@Param("openid") String openid);

    /**
     * 新增用户
     * 小程序登录时自动注册，或管理后台添加用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     * 绑定手机号、更新session_key等场景
     */
    int update(User user);

    // ==================== 管理端方法 ====================

    /**
     * 根据手机号查用户
     * 关键业务：小程序授权手机号时，用这个匹配管理员已添加的安保/审批人账号
     */
    User findByPhone(@Param("phone") String phone);

    /**
     * 根据ID查用户
     */
    User findById(@Param("userId") Integer userId);

    /**
     * 条件查询用户列表（不分页，导出用）
     */
    List<User> listUsers(@Param("phone") String phone, @Param("realName") String realName, @Param("userType") Integer userType);

    /**
     * 更新用户信息（管理端用）
     * 和update的区别：这个可能只更新部分字段
     */
    int updateUser(User user);

    /**
     * 删除用户
     * 注意：有关联预约记录的用户不能删
     */
    int deleteUserById(@Param("userId") Integer userId);

    // ==================== 分页查询 ====================

    /**
     * 分页查询用户列表
     * @param offset  偏移量（(page-1)*pageSize）
     * @param pageSize 每页条数
     */
    List<User> listUsersPage(
            @Param("phone") String phone,
            @Param("realName") String realName,
            @Param("userType") Integer userType,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计用户总数（用于分页计算）
     */
    Long countUsers(
            @Param("phone") String phone,
            @Param("realName") String realName,
            @Param("userType") Integer userType
    );
}