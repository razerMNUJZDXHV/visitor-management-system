-- =============================================
-- 应急管理大学访客预约系统 - 中期答辩演示数据
-- 用途：为中期答辩准备完整的演示数据
-- =============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 插入演示用户数据
-- 用户类型：1-访客，2-审批人，3-安保，4-管理员
-- =============================================

-- 清空现有数据（演示用）
DELETE FROM `access_log`;
DELETE FROM `appointment`;
DELETE FROM `user`;

-- 插入管理员用户（密码：admin123）
INSERT INTO `user` (`openid`, `phone`, `real_name`, `password`, `user_type`, `create_time`) VALUES
('admin_openid_001', '13800138000', '系统管理员', '$2a$10$8K1p/a0dL3LWbYQxYQxYQe8K1p/a0dL3LWbYQxYQxYQe8K1p/a0dL3', 4, NOW());

-- 插入审批人用户
INSERT INTO `user` (`openid`, `phone`, `real_name`, `user_type`, `create_time`) VALUES
('approver_openid_001', '13800138001', '张教授', 2, NOW()),
('approver_openid_002', '13800138002', '李主任', 2, NOW());

-- 插入安保人员
INSERT INTO `user` (`openid`, `phone`, `real_name`, `user_type`, `create_time`) VALUES
('security_openid_001', '13800138003', '王保安', 3, NOW()),
('security_openid_002', '13800138004', '赵安保', 3, NOW());

-- 插入访客用户
INSERT INTO `user` (`openid`, `phone`, `real_name`, `user_type`, `create_time`) VALUES
('visitor_openid_001', '13900139001', '张三', 1, NOW()),
('visitor_openid_002', '13900139002', '李四', 1, NOW()),
('visitor_openid_003', '13900139003', '王五', 1, NOW()),
('visitor_openid_004', '13900139004', '赵六', 1, NOW());

-- =============================================
-- 2. 插入演示预约数据
-- 预约状态：0-待审核，1-预约成功，2-预约失败，3-已取消，4-已签到，5-已完成，6-已过期
-- =============================================

-- 获取用户ID（假设自增ID从1开始）
-- 访客ID: 5,6,7,8
-- 审批人ID: 2,3
-- 安保ID: 4,5

-- 插入待审核预约（状态0）
INSERT INTO `appointment` 
(`visitor_id`, `approver_id`, `visitor_name`, `visitor_phone`, `visitor_id_card`, 
`interviewee_name`, `visit_reason`, `expected_start_time`, `expected_end_time`, 
`status`, `create_time`) 
VALUES
(5, 2, '张三', '13900139001', 'ENCRYPTED:张三身份证号', '张教授', '学术交流', 
DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY + 3 HOUR), 
0, NOW() - INTERVAL 30 MINUTE),
(6, 3, '李四', '13900139002', 'ENCRYPTED:李四身份证号', '李主任', '商务洽谈', 
DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY + 2 HOUR), 
0, NOW() - INTERVAL 1 HOUR);

-- 插入预约成功（状态1）
INSERT INTO `appointment` 
(`visitor_id`, `approver_id`, `visitor_name`, `visitor_phone`, `visitor_id_card`, 
`interviewee_name`, `visit_reason`, `expected_start_time`, `expected_end_time`, 
`status`, `process_time`, `qr_code_url`, `create_time`) 
VALUES
(5, 2, '张三', '13900139001', 'ENCRYPTED:张三身份证号', '张教授', '学术讲座', 
DATE_ADD(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), 
1, NOW() - INTERVAL 2 HOUR, 'https://example.com/qrcode/001.png', 
NOW() - INTERVAL 3 HOUR),
(7, 2, '王五', '13900139003', 'ENCRYPTED:王五身份证号', '张教授', '参观校园', 
DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY + 4 HOUR), 
1, NOW() - INTERVAL 4 HOUR, 'https://example.com/qrcode/002.png', 
NOW() - INTERVAL 5 HOUR);

-- 插入已签到（状态4）
INSERT INTO `appointment` 
(`visitor_id`, `approver_id`, `visitor_name`, `visitor_phone`, `visitor_id_card`, 
`interviewee_name`, `visit_reason`, `expected_start_time`, `expected_end_time`, 
`status`, `process_time`, `qr_code_url`, `create_time`) 
VALUES
(8, 3, '赵六', '13900139004', 'ENCRYPTED:赵六身份证号', '李主任', '面试', 
NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 2 HOUR, 
4, NOW() - INTERVAL 2 HOUR, 'https://example.com/qrcode/003.png', 
NOW() - INTERVAL 3 HOUR);

-- 插入已完成（状态5）
INSERT INTO `appointment` 
(`visitor_id`, `approver_id`, `visitor_name`, `visitor_phone`, `visitor_id_card`, 
`interviewee_name`, `visit_reason`, `expected_start_time`, `expected_end_time`, 
`status`, `process_time`, `create_time`) 
VALUES
(5, 2, '张三', '13900139001', 'ENCRYPTED:张三身份证号', '张教授', '项目讨论', 
NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 
5, NOW() - INTERVAL 2 DAY - INTERVAL 1 HOUR, 
NOW() - INTERVAL 3 DAY);

-- 插入已拒绝（状态2）
INSERT INTO `appointment` 
(`visitor_id`, `approver_id`, `visitor_name`, `visitor_phone`, `visitor_id_card`, 
`interviewee_name`, `visit_reason`, `expected_start_time`, `expected_end_time`, 
`status`, `reject_reason`, `process_time`, `create_time`) 
VALUES
(6, 3, '李四', '13900139002', 'ENCRYPTED:李四身份证号', '李主任', '私人拜访', 
DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY + 2 HOUR), 
2, '非工作时间不接受私人拜访', NOW() - INTERVAL 3 HOUR, 
NOW() - INTERVAL 4 HOUR);

-- =============================================
-- 3. 插入演示通行记录数据
-- 通行类型：1-签到，2-签离
-- 核验方式：1-扫码，2-手动
-- =============================================

-- 插入签到记录（对应已签到的预约）
INSERT INTO `access_log` 
(`appointment_id`, `visitor_id`, `security_id`, `access_type`, `access_time`, `verify_method`) 
VALUES
(3, 8, 4, 1, NOW() - INTERVAL 1 HOUR, 1);

-- 插入已完成预约的签到和签离记录
INSERT INTO `access_log` 
(`appointment_id`, `visitor_id`, `security_id`, `access_type`, `access_time`, `verify_method`) 
VALUES
(4, 5, 4, 1, NOW() - INTERVAL 2 DAY, 1),
(4, 5, 4, 2, NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 1);

-- 插入手动登记记录（无预约ID）
INSERT INTO `access_log` 
(`visitor_id`, `security_id`, `access_type`, `access_time`, `verify_method`, `authorizer_id`) 
VALUES
(7, 5, 1, NOW() - INTERVAL 30 MINUTE, 2, 2);

-- =============================================
-- 4. 更新预约设置（JSON文件）
-- =============================================

-- 注意：appointment_setting 存储在 data/appointment-setting.json
-- 请确保该文件包含以下配置：
-- {
--   "openTime": "09:00",
--   "closeTime": "18:00",
--   "maxAdvanceDays": 7,
--   "minAdvanceHours": 1,
--   "maxVisitHours": 24,
--   "dailyLimit": 100
-- }

-- =============================================
-- 完成提示
-- =============================================
SELECT '演示数据插入完成！' AS '提示';
SELECT '用户账号：' AS '说明', 
       '管理员: 13800138000' AS '账号',
       '审批人: 13800138001, 13800138002' AS '账号',
       '安保: 13800138003, 13800138004' AS '账号',
       '访客: 13900139001-13900139004' AS '账号';

SET FOREIGN_KEY_CHECKS = 1;
