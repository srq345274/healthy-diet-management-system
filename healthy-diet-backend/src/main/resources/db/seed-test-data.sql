-- =============================================================================
-- 联调测试数据（Navicat / mysql 客户端执行）
-- 库名：healthy_diet（与 application.yml 一致）
--
-- 登录账号：用户名 testdemo   密码 Test123456
-- （密码哈希由 BCrypt 生成，与 Spring PasswordEncoder 兼容）
--
-- 说明：
--   1. 会先清理「测试用户」名下的业务数据再重写，便于重复执行。
--   2. 打卡日期基于 CURDATE() 往前连续 7 天，首页连续天数、勋章可直接看到效果。
--   3. 饮食记录覆盖最近约 14 天；其中「3 天前」单日碳水偏高，便于「营养风险预警」接口有数据。
-- =============================================================================

USE `healthy_diet`;

SET NAMES utf8mb4;

-- 测试用户（不存在则插入）
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `created_at`)
SELECT 'testdemo',
       '$2b$10$z8o3Y6lL/UNwF3I48s5MKeUgZhRxSWYJaubcAU2U1RyZwufveWXWi',
       '测试用户',
       NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'testdemo');

SET @u := (SELECT `id` FROM `sys_user` WHERE `username` = 'testdemo' LIMIT 1);

-- 清理该用户旧数据（顺序满足外键）
DELETE FROM `user_badge` WHERE `user_id` = @u;
DELETE FROM `daily_check_in` WHERE `user_id` = @u;
DELETE FROM `diet_record` WHERE `user_id` = @u;
DELETE FROM `health_metric_log` WHERE `user_id` = @u;
DELETE FROM `shopping_list` WHERE `user_id` = @u;
DELETE FROM `meal_plan` WHERE `user_id` = @u;
DELETE FROM `user_favorite_recipe` WHERE `user_id` = @u;
DELETE FROM `user_health_profile` WHERE `user_id` = @u;

-- ---------------------------------------------------------------------------
-- 健康档案（每日热量目标 1800，便于饮食汇总「超标」提示）
-- ---------------------------------------------------------------------------
INSERT INTO `user_health_profile` (
  `user_id`, `height_cm`, `weight_kg`, `age`, `gender`,
  `medical_history`, `allergies`, `dietary_restrictions`,
  `health_goals`, `taste_preferences`, `avoid_ingredients`, `cooking_constraints`,
  `daily_calorie_target`, `updated_at`
) VALUES (
  @u, 172.00, 68.50, 28, 'MALE',
  NULL,
  '花生',
  '少油炸',
  '减脂、稳定血糖',
  '清淡、中餐',
  '香菜',
  '寝室仅有电煮锅',
  1800,
  NOW(6)
);

-- ---------------------------------------------------------------------------
-- 体重日志（统计页「体重趋势」）
-- ---------------------------------------------------------------------------
INSERT INTO `health_metric_log` (`user_id`, `metric_type`, `value`, `unit`, `recorded_at`) VALUES
(@u, 'WEIGHT', 69.20, 'kg', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(@u, 'WEIGHT', 68.90, 'kg', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(@u, 'WEIGHT', 68.60, 'kg', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(@u, 'WEIGHT', 68.40, 'kg', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(@u, 'WEIGHT', 68.50, 'kg', NOW());

-- ---------------------------------------------------------------------------
-- 每日打卡（连续 7 天到今天 → streak=7，配合下面勋章）
-- ---------------------------------------------------------------------------
INSERT INTO `daily_check_in` (`user_id`, `checkin_date`, `weight_kg`, `water_ml`, `diet_compliance_percent`, `created_at`)
VALUES
(@u, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 68.90, 1750, 78, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 68.85, 1600, 80, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 68.80, 1900, 75, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 68.70, 1800, 82, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 68.65, 1700, 85, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 68.55, 1850, 88, NOW(6)),
(@u, CURDATE(),                              68.50, 2000, 90, NOW(6));

-- 勋章（直接插入，避免再走打卡接口）
INSERT INTO `user_badge` (`user_id`, `badge_code`, `earned_at`) VALUES
(@u, 'FIRST_CHECKIN', NOW(6)),
(@u, 'STREAK_7', NOW(6));

-- ---------------------------------------------------------------------------
-- 饮食记录：平日正常 + 「3 天前」高碳水预警 + 若干天热量明细
-- NutritionRiskService：碳水日合计 > 400g 触发 HIGH_CARB
-- ---------------------------------------------------------------------------
INSERT INTO `diet_record` (
  `user_id`, `record_date`, `meal_type`, `source`,
  `dish_name`, `note`, `calories`, `protein_g`, `fat_g`, `carb_g`, `sodium_mg`,
  `created_at`
) VALUES
-- 普通日（昨天）
(@u, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'BREAKFAST', 'MANUAL', '燕麦牛奶', NULL, 320.00, 12.00, 8.00, 48.00, 120.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'LUNCH',     'MANUAL', '番茄鸡蛋面', NULL, 520.00, 18.00, 14.00, 72.00, 980.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'DINNER',    'MANUAL', '清蒸鱼套餐', NULL, 480.00, 35.00, 12.00, 38.00, 620.00, NOW(6)),
-- 高碳水日（3 天前，全天碳水合计 > 400g → 营养风险 HIGH_CARB）
(@u, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'BREAKFAST', 'MANUAL', '油条豆浆', '演示：偏高碳水', 480.00, 8.00, 22.00, 130.00, 380.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'LUNCH',     'MANUAL', '炒饭', NULL, 640.00, 14.00, 18.00, 155.00, 850.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'DINNER',    'MANUAL', '红糖馒头配菜', NULL, 520.00, 12.00, 10.00, 135.00, 420.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'SNACK',     'MANUAL', '奶茶', NULL, 290.00, 4.00, 10.00, 45.00, 90.00, NOW(6)),
-- 高钠日（4 天前，全天钠合计 > 2300mg → HIGH_SODIUM）
(@u, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'LUNCH',     'MANUAL', '咸菜肉丝饭', '演示：偏高钠', 560.00, 20.00, 18.00, 68.00, 1400.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'DINNER',    'MANUAL', '泡面火腿肠', NULL, 620.00, 15.00, 26.00, 72.00, 1150.00, NOW(6)),
-- 高脂肪日（6 天前，全天脂肪合计 > 80g → HIGH_FAT）
(@u, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'LUNCH',     'MANUAL', '炸鸡套餐', '演示：偏高脂肪', 920.00, 35.00, 52.00, 45.00, 680.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'DINNER',    'MANUAL', '红烧肉盖饭', NULL, 780.00, 28.00, 38.00, 62.00, 520.00, NOW(6)),
-- 再往前几天，便于「近14天」曲线
(@u, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'LUNCH', 'MANUAL', '鸡胸肉沙拉', NULL, 410.00, 38.00, 12.00, 28.00, 280.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'DINNER', 'MANUAL', '家常炒菜', NULL, 580.00, 26.00, 22.00, 48.00, 880.00, NOW(6)),
(@u, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'BREAKFAST', 'MANUAL', '全麦面包鸡蛋', NULL, 350.00, 15.00, 11.00, 45.00, 400.00, NOW(6));

-- ---------------------------------------------------------------------------
-- 配餐方案 JSON（购物清单「一键生成」会解析其中的菜名字符串）
-- ---------------------------------------------------------------------------
INSERT INTO `meal_plan` (`user_id`, `plan_name`, `plan_type`, `start_date`, `end_date`, `content_json`, `status`, `created_at`)
VALUES (
  @u,
  '演示周计划',
  'WEEKLY',
  DATE_SUB(CURDATE(), INTERVAL 7 DAY),
  DATE_ADD(CURDATE(), INTERVAL 7 DAY),
  '{"days":[{"date":"demo","meals":{"BREAKFAST":["小米粥","水煮蛋","凉拌黄瓜"],"LUNCH":["清蒸鲈鱼","米饭","蒜蓉西兰花"],"DINNER":["番茄豆腐汤","冬瓜虾仁"]}}]}',
  'ACTIVE',
  NOW(6)
);

SET @plan_id := LAST_INSERT_ID();

INSERT INTO `shopping_list` (`user_id`, `meal_plan_id`, `title`, `items_json`, `created_at`)
VALUES (
  @u,
  @plan_id,
  '演示：已从方案生成一次',
  '[{"name":"小米粥","qty":"适量"},{"name":"鲈鱼","qty":"1条"},{"name":"西兰花","qty":"300g"}]',
  NOW(6)
);

-- ---------------------------------------------------------------------------
-- 收藏菜谱（需库里已有 recipe.id；若无请先执行 schema-full 末尾示例 INSERT）
-- ---------------------------------------------------------------------------
INSERT INTO `user_favorite_recipe` (`user_id`, `recipe_id`, `created_at`)
SELECT @u, r.`id`, NOW(6)
FROM `recipe` r
WHERE r.`name` = '清蒸西兰花鸡胸肉'
LIMIT 1;

-- 完成提示
SELECT CONCAT('测试用户已就绪：username=testdemo  password=Test123456  user_id=', @u) AS hint;
