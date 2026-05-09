package com.gpj.visitorsystem.controller.admin;

import com.gpj.visitorsystem.dto.PageResultDTO;
import com.gpj.visitorsystem.dto.ResultDTO;
import com.gpj.visitorsystem.dto.admin.AdminCreateUserRequestDTO;
import com.gpj.visitorsystem.dto.admin.AdminUpdateUserRequestDTO;
import com.gpj.visitorsystem.entity.User;
import com.gpj.visitorsystem.exception.BusinessException;
import com.gpj.visitorsystem.service.admin.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【业务模块】管理后台用户管理接口
 *
 * 【核心职责】
 * 1. 创建/修改/删除用户
 * 2. 分页查询用户列表
 * 3. 统计管理员数量
 *
 * 【关键业务场景】
 * 管理员创建用户时需要区分角色，管理员必须设置密码，访客由小程序注册。
 *
 * 【依赖说明】
 * - AdminUserService：用户管理核心逻辑
 *
 * 【注意事项】
 * - 删除用户不允许删除自己
 * - 修改手机号会清空openid/sessionKey，需重新登录小程序
 * - 管理员数量统计用于仪表盘展示
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 管理员创建用户
     *
     * 【接口说明】
     * 管理端创建各类用户（访客/审批人/安保/管理员）。
     *
     * 【请求参数】
     * @param dto 创建用户请求DTO，包含phone、realName、userType、password等
     *
     * 【返回值】
     * @return 创建后的用户实体（含userId）
     *
     * 【异常情况】
     * - 手机号已注册/管理员密码为空：返回对应错误信息
     */
    @PostMapping("/create")
    public ResultDTO<User> createUser(@Valid @RequestBody AdminCreateUserRequestDTO dto) {
        User user = buildUserFromDto(new User(), dto);
        return ResultDTO.success(adminUserService.createUser(user));
    }

    /**
     * 管理员分页查询用户列表
     *
     * 【接口说明】
     * 支持按手机号、姓名、用户类型筛选用户列表。
     *
     * 【请求参数】
     * @param phone 手机号（可选）
     * @param realName 真实姓名（可选）
     * @param userType 用户类型（可选，1-访客 2-审批人 3-安保 4-管理员）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     *
     * 【返回值】
     * @return 分页结果，包含list和total
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/list")
    public ResultDTO<PageResultDTO<User>> listUsers(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer userType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        return ResultDTO.success(adminUserService.listUsersPage(
                phone, realName, userType, pageNum, pageSize));
    }

    /**
     * 管理员修改用户信息
     *
     * 【接口说明】
     * 修改用户手机号、姓名、角色与密码等信息。
     *
     * 【请求参数】
     * @param dto 修改用户请求DTO，包含userId、phone、realName、userType、password等
     * @param currentUserId 当前登录用户ID（从Token解析，通过@RequestAttribute传递）
     *
     * 【返回值】
     * @return 修改后的用户实体
     *
     * 【异常情况】
     * - 用户不存在/手机号已被注册：返回对应错误信息
     */
    @PutMapping("/update")
    public ResultDTO<User> updateUser(@Valid @RequestBody AdminUpdateUserRequestDTO dto,
                                      @RequestAttribute("userId") Integer currentUserId) {
        // 校验用户是否存在
        User existUser = adminUserService.findById(dto.getUserId());
        if (existUser == null) {
            throw new BusinessException(400, "用户不存在");
        }

        // 安全保护：不可删除/降级自己的管理员权限（若自己是唯一管理员）
        // 此逻辑由前端二次确认 + Service 层业务保护共同完成，这里仅做基础校验

        // DTO 转换为实体，只更新非空字段（基于已存在的用户）
        User user = buildUserFromDto(existUser, dto);
        user.setUserId(dto.getUserId());  // 确保 userId 正确

        return ResultDTO.success(adminUserService.updateUser(user));
    }

    /**
     * 管理员删除用户
     *
     * 【接口说明】
     * 删除指定用户（禁止删除自己）。
     *
     * 【请求参数】
     * @param userId 要删除的用户ID
     * @param currentUserId 当前登录用户ID（从Token解析）
     *
     * 【返回值】
     * @return 成功
     *
     * 【异常情况】
     * - 用户不存在/删除自己：返回对应错误信息
     */
    @DeleteMapping("/delete/{userId}")
    public ResultDTO<Void> deleteUser(@PathVariable Integer userId,
                                      @RequestAttribute("userId") Integer currentUserId) {
        // 不允许删除自己
        if (userId.equals(currentUserId)) {
            throw new BusinessException(400, "不可删除自己的账号");
        }

        adminUserService.deleteUserById(userId);
        return ResultDTO.success(null);
    }

    /**
     * 获取管理员总数
     *
     * 【接口说明】
     * 返回管理员数量，用于仪表盘展示。
     *
    * 【请求参数】
    * 无
     *
     * 【返回值】
     * @return 管理员总数
     *
     * 【异常情况】
     * - 无
     */
    @GetMapping("/admin/count")
    public ResultDTO<Long> getAdminCount() {
        return ResultDTO.success(adminUserService.countAdminUsers());
    }

    /**
     * 将 AdminCreateUserRequestDTO 的字段复制到 User 实体（null 值不覆盖）
     */
    private User buildUserFromDto(User user, AdminCreateUserRequestDTO dto) {
        return applyDtoFields(user,
                dto.getPhone(), dto.getRealName(), dto.getUserType(), dto.getPassword());
    }

    /**
     * 将 AdminUpdateUserRequestDTO 的字段复制到 User 实体（null 值不覆盖）
     */
    private User buildUserFromDto(User user, AdminUpdateUserRequestDTO dto) {
        return applyDtoFields(user,
                dto.getPhone(), dto.getRealName(), dto.getUserType(), dto.getPassword());
    }

    /**
     * 统一处理 DTO 字段填充（private 通用方法）
     * phone/realName/password 为空或空白时不设置
     * userType 为 null 时不设置
     */
    private User applyDtoFields(User user,
                                   String phone, String realName, Integer userType, String password) {
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
        }
        if (realName != null && !realName.trim().isEmpty()) {
            user.setRealName(realName.trim());
        }
        if (userType != null) {
            user.setUserType(userType);
        }
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(password.trim());
        }
        return user;
    }
}