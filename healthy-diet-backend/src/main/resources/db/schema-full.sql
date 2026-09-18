SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 一、创建数据库（可按需改库名）
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `healthy_diet`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `healthy_diet`;

-- ---------------------------------------------------------------------------
-- 二、删表（子表 → 父表，避免外键顺序问题）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_badge`;
DROP TABLE IF EXISTS `daily_check_in`;
DROP TABLE IF EXISTS `user_favorite_recipe`;
DROP TABLE IF EXISTS `diet_record`;
DROP TABLE IF EXISTS `health_metric_log`;
DROP TABLE IF EXISTS `shopping_list`;
DROP TABLE IF EXISTS `meal_plan`;
DROP TABLE IF EXISTS `user_health_profile`;
DROP TABLE IF EXISTS `recipe_ingredient`;
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `ingredient`;
DROP TABLE IF EXISTS `recipe`;
DROP TABLE IF EXISTS `sys_user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 三、建表
-- ---------------------------------------------------------------------------

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(128) NOT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(256) NOT NULL,
  `summary` TEXT,
  `content` LONGTEXT,
  `category` VARCHAR(32) DEFAULT NULL,
  `cover_image` VARCHAR(512) DEFAULT NULL,
  `published_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ingredient` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `calories_per100g` DECIMAL(19,2) DEFAULT NULL,
  `protein_per100g` DECIMAL(19,2) DEFAULT NULL,
  `fat_per100g` DECIMAL(19,2) DEFAULT NULL,
  `carb_per100g` DECIMAL(19,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recipe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `steps` TEXT,
  `servings` INT DEFAULT NULL,
  `calories_per_serving` DECIMAL(19,2) DEFAULT NULL,
  `protein_g` DECIMAL(19,2) DEFAULT NULL,
  `fat_g` DECIMAL(19,2) DEFAULT NULL,
  `carb_g` DECIMAL(19,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recipe_ingredient` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `recipe_id` BIGINT NOT NULL,
  `ingredient_id` BIGINT NOT NULL,
  `quantity_g` DECIMAL(19,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_ri_recipe` (`recipe_id`),
  KEY `fk_ri_ingredient` (`ingredient_id`),
  CONSTRAINT `fk_ri_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`),
  CONSTRAINT `fk_ri_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_health_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `height_cm` DECIMAL(19,2) DEFAULT NULL,
  `weight_kg` DECIMAL(19,2) DEFAULT NULL,
  `age` INT DEFAULT NULL,
  `gender` VARCHAR(8) DEFAULT NULL,
  `medical_history` TEXT,
  `allergies` TEXT,
  `dietary_restrictions` TEXT,
  `health_goals` TEXT,
  `taste_preferences` TEXT,
  `avoid_ingredients` TEXT,
  `cooking_constraints` TEXT,
  `daily_calorie_target` INT DEFAULT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_user` (`user_id`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meal_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `plan_name` VARCHAR(128) DEFAULT NULL,
  `plan_type` VARCHAR(16) NOT NULL,
  `start_date` DATE DEFAULT NULL,
  `end_date` DATE DEFAULT NULL,
  `content_json` LONGTEXT,
  `status` VARCHAR(16) DEFAULT NULL,
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_meal_plan_user` (`user_id`),
  CONSTRAINT `fk_meal_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shopping_list` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `meal_plan_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(128) DEFAULT NULL,
  `items_json` LONGTEXT,
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_sl_user` (`user_id`),
  KEY `fk_sl_meal_plan` (`meal_plan_id`),
  CONSTRAINT `fk_sl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_sl_meal_plan` FOREIGN KEY (`meal_plan_id`) REFERENCES `meal_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `health_metric_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `metric_type` VARCHAR(32) NOT NULL,
  `value` DECIMAL(19,2) DEFAULT NULL,
  `unit` VARCHAR(32) DEFAULT NULL,
  `recorded_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_hml_user` (`user_id`),
  CONSTRAINT `fk_hml_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `diet_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_date` DATE NOT NULL,
  `meal_type` VARCHAR(32) NOT NULL,
  `source` VARCHAR(32) NOT NULL,
  `image_url` VARCHAR(512) DEFAULT NULL,
  `dish_name` VARCHAR(256) DEFAULT NULL,
  `note` TEXT,
  `calories` DECIMAL(19,2) DEFAULT NULL,
  `protein_g` DECIMAL(19,2) DEFAULT NULL,
  `fat_g` DECIMAL(19,2) DEFAULT NULL,
  `carb_g` DECIMAL(19,2) DEFAULT NULL,
  `sodium_mg` DECIMAL(19,2) DEFAULT NULL,
  `recognition_raw_json` TEXT,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_dr_user` (`user_id`),
  KEY `idx_dr_user_date` (`user_id`, `record_date`),
  CONSTRAINT `fk_dr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_favorite_recipe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `recipe_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_recipe` (`user_id`, `recipe_id`),
  KEY `fk_ufr_recipe` (`recipe_id`),
  CONSTRAINT `fk_ufr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_ufr_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_check_in` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `checkin_date` DATE NOT NULL,
  `weight_kg` DECIMAL(19,2) DEFAULT NULL,
  `water_ml` INT DEFAULT NULL,
  `diet_compliance_percent` INT DEFAULT NULL,
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_user_date` (`user_id`, `checkin_date`),
  CONSTRAINT `fk_dci_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_badge` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `badge_code` VARCHAR(64) NOT NULL,
  `earned_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_badge_user_code` (`user_id`, `badge_code`),
  CONSTRAINT `fk_ub_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 四、可选示例数据（与 DataInitializer 类似，便于联调）
-- ---------------------------------------------------------------------------
INSERT INTO `article` (`title`, `summary`, `content`, `category`, `published_at`)
VALUES
('控糖饮食的常见误区', '并非不吃主食就能控糖。', '误区一：完全不吃碳水化合物。合理选择低 GI 主食并控制总量更重要。', 'MYTH', NOW(6)),
('春季清淡饮食建议', '少油少盐，多蔬菜。', '春季可多选用时令蔬菜，烹调以蒸、煮、焯为主。', 'SEASON', NOW(6)),
('如何阅读营养成分表', '关注每 100g 能量与钠含量。', '购买包装食品时注意能量与钠的 NRV%。', 'SCIENCE', NOW(6));

INSERT INTO `ingredient` (`name`, `calories_per100g`, `protein_per100g`, `fat_per100g`, `carb_per100g`)
VALUES
('鸡胸肉', 165.00, 31.00, 3.60, 0.00),
('糙米', 350.00, 7.40, 2.70, 77.00),
('西兰花', 34.00, 2.80, 0.40, 7.00);

INSERT INTO `recipe` (`name`, `description`, `steps`, `servings`, `calories_per_serving`, `protein_g`, `fat_g`, `carb_g`)
VALUES
('清蒸西兰花鸡胸肉', '低脂高蛋白搭配。', '1. 鸡胸肉切块腌制；2. 西兰花焯水；3. 一起蒸熟。', 1, 420.00, 45.00, 8.00, 35.00),
('糙米饭配时蔬', '复合碳水+纤维。', '糙米煮熟，搭配焯水青菜。', 1, 380.00, 12.00, 6.00, 72.00);

-- 结束
