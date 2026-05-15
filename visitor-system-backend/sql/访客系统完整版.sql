-- =============================================
-- 基于微信小程序的应大访客预约与准入管理系统 - 完整数据库脚本
-- 包含：用户表、预约表、通行记录表、索引优化
-- =============================================

-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 用户表
-- 存储所有用户（访客、审批人、安保、管理员）
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(32) DEFAULT NULL COMMENT '微信openid（唯一）',
  `session_key` varchar(32) DEFAULT NULL COMMENT '微信session_key',
  `phone` varchar(16) DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(20) DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(128) DEFAULT NULL COMMENT '管理员登录密码（加密）',
  `user_type` tinyint NOT NULL DEFAULT '1' COMMENT '用户类型：1-访客，2-审批人，3-安保，4-管理员',
  `refresh_token` varchar(255) DEFAULT NULL COMMENT '刷新token',
  `refresh_token_expire_time` datetime DEFAULT NULL COMMENT '刷新token过期时间',
  `missed_count` int NOT NULL DEFAULT '0' COMMENT '累计爽约次数',
  `banned_until` datetime DEFAULT NULL COMMENT '禁止预约截止时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_user_type` (`user_type`),
  CHECK (`user_type` IN (1,2,3,4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 预约表
-- 存储所有访客预约记录
-- =============================================
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment` (
  `appointment_id` int NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `visitor_id` int NOT NULL COMMENT '访客ID（关联user表）',
  `approver_id` int DEFAULT NULL COMMENT '审批人ID（关联user表）',
  `visitor_name` varchar(20) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(16) NOT NULL COMMENT '访客手机号',
  `visitor_id_card` varchar(255) NOT NULL COMMENT '访客身份证号（加密）',
  `interviewee_name` varchar(20) DEFAULT NULL COMMENT '被访人姓名',
  `visit_reason` varchar(500) NOT NULL COMMENT '来访事由',
  `expected_start_time` datetime NOT NULL COMMENT '预计到达时间',
  `expected_end_time` datetime NOT NULL COMMENT '预计离开时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-预约成功，2-预约失败，3-已取消，4-已签到，5-已完成，6-已过期',
  `qr_code_url` longtext COMMENT '二维码图片URL',
  `qr_expire_time` datetime DEFAULT NULL COMMENT '二维码过期时间',
  `reject_reason` varchar(300) DEFAULT NULL COMMENT '拒绝原因',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间（审批通过/拒绝时记录）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_visitor_id` (`visitor_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CHECK (`status` IN (0,1,2,3,4,5,6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- =============================================
-- 3. 通行记录表
-- 存储所有签到/签离记录
-- =============================================
DROP TABLE IF EXISTS `access_log`;
CREATE TABLE `access_log` (
  `log_id` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `appointment_id` int DEFAULT NULL COMMENT '预约ID（可为空，表示手动登记）',
  `visitor_id` int DEFAULT NULL COMMENT '访客ID',
  `security_id` int NOT NULL COMMENT '核验人ID（安保）',
  `access_type` tinyint NOT NULL COMMENT '1-签到，2-签离',
  `access_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核验时间',
  `verify_method` tinyint NOT NULL DEFAULT '1' COMMENT '1-扫码，2-手动',
  `authorizer_id` int DEFAULT NULL COMMENT '紧急手动登记时的授权人ID（审批人）',
  PRIMARY KEY (`log_id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_visitor_id` (`visitor_id`),
  KEY `idx_security_id` (`security_id`),
  CHECK (`access_type` IN (1,2)),
  CHECK (`verify_method` IN (1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通行记录表';

-- =============================================
-- 4. 索引优化
-- 提升查询性能：避免全表扫描，优化常用查询场景
-- =============================================

-- appointment 表索引优化
-- 复合索引：按状态+时间查询（历史记录查询常用）
ALTER TABLE `appointment` ADD INDEX `idx_status_create_time` (`status`, `create_time`);

-- 复合索引：按审批人+状态查询（待审批列表常用）
ALTER TABLE `appointment` ADD INDEX `idx_approver_status` (`approver_id`, `status`);

-- 复合索引：按访客+状态查询（我的预约列表常用）
ALTER TABLE `appointment` ADD INDEX `idx_visitor_status_time` (`visitor_id`, `status`, `create_time`);

-- access_log 表索引优化
-- 时间索引：按访问时间查询（流量统计、记录查询常用）
ALTER TABLE `access_log` ADD INDEX `idx_access_time` (`access_time`);

-- 复合索引：按预约+访问类型查询（签到/签离判断常用）
ALTER TABLE `access_log` ADD INDEX `idx_appointment_type_time` (`appointment_id`, `access_type`, `access_time`);

-- =============================================
-- 5. 索引补充（定时任务/爽约判定/审批历史/紧急登记场景优化）
-- =============================================

-- 1) 状态 + 预计离开时间
-- 用途：expirePendingAppointments（每6小时过期扫描）、listLeaveTimeReached/listOvertimeStaying（每分钟告警扫描）
-- 效果：避免定时任务全表扫描
ALTER TABLE `appointment` ADD INDEX `idx_status_expected_end_time` (`status`, `expected_end_time`);

-- 2) 访客ID + 预计离开时间
-- 用途：findLatestRelevantBefore（查找上一次爽约记录）
-- 效果：精确查找访客的上一次爽约记录
ALTER TABLE `appointment` ADD INDEX `idx_visitor_expected_end_time` (`visitor_id`, `expected_end_time`);

-- 3) 审批人 + 处理时间 + 创建时间
-- 用途：findHistoryByApprover（审批人历史记录查询，按 process_time DESC, create_time DESC 排序）
-- 效果：避免 filesort，加速排序和分页
ALTER TABLE `appointment` ADD INDEX `idx_approver_process_time` (`approver_id`, `process_time`, `create_time`);

-- 4) 紧急登记查询
-- 用途：listEmergencyActive（安保端查询活跃的紧急手动登记）
-- 效果：避免 interviewee_name 全表扫描
ALTER TABLE `appointment` ADD INDEX `idx_interviewee_status` (`interviewee_name`, `status`, `create_time`);

-- =============================================
-- 完成提示
-- =============================================
SELECT '数据库初始化完成！' AS '提示';
SET FOREIGN_KEY_CHECKS = 1;
