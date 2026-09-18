# AI 健康饮食管理系统

前后端分离：Spring Boot 2.7 + MySQL + uni-app（Vue2）。RESTful API，JWT 鉴权；百度菜品识别；豆包（火山方舟 OpenAPI）营养建议与配餐草稿。

## 你还需要的软件 / 信息

| 项目 | 说明 |
|------|------|
| **JDK 17（推荐）或 11** | 本仓库 `pom.xml` 已设为 **Java 17**。请在 IDEA **文件 → 项目结构 → Project** 将 **SDK** 选为 **Microsoft OpenJDK 17**（或你的 JDK 11），**不要选 JDK 23/24** 编译 Spring Boot 2.7，以免不兼容。 |
| **Maven** | 你当前使用 **IDEA 捆绑 Maven 3.9.9** 即可；命令行打包需本机已安装 `mvn` 并配置 PATH。 |
| **HBuilderX 或 @dcloudio/vue-cli-plugin-uni** | uni-app 工程推荐用 [HBuilderX](https://www.dcloud.io/hbuilderx.html) 打开 `healthy-diet-app` 运行到浏览器/模拟器；若用 CLI，请按 DCloud 文档安装 uni-app Vue2 编译环境。 |
| **百度智能云** | 开通「图像识别 / 菜品识别」，得到 API Key 与 Secret Key。 |
| **火山引擎方舟** | 创建推理接入点，得到 `endpoint`、`model`（如 ep-xxx）与 API Key。 |
| **云服务器（可选）** | 部署时需提供公网 IP/域名、HTTPS 证书（小程序必填 HTTPS）、MySQL 与 JDK 运行环境。 |

请在本机准备好：**MySQL 库名与账号密码**、**本机局域网 IP**（手机调试后端时把 `healthy-diet-app/utils/config.js` 里 `API_BASE` 改为 `http://你的IP:8080/api`）。

## 后端 `healthy-diet-backend`

1. 创建数据库（或执行 `src/main/resources/db/init.sql` 仅建库）：
   ```sql
   CREATE DATABASE healthy_diet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 修改 `src/main/resources/application.yml` 中的 `spring.datasource.username` / `password`。
3. 配置密钥（**切勿提交真实密钥到公开仓库**）：
   - `healthdiet.jwt.secret`：足够长的随机字符串。
   - 环境变量或 yml：`BAIDU_API_KEY`、`BAIDU_SECRET_KEY`、`DOUBAO_API_KEY`、`DOUBAO_ENDPOINT`、`DOUBAO_MODEL`。
4. IDEA 打开 `healthy-diet-backend` 目录，运行 `HealthyDietApplication`；或执行：
   ```bash
   mvn -f healthy-diet-backend/pom.xml spring-boot:run
   ```
5. 健康检查：`GET http://127.0.0.1:8080/api/health`

主要接口前缀：`/api`。公开接口：`/api/auth/*`、`/api/health`、`/api/articles/**`、`/api/recipes/**`、`/api/ingredients/**`；其余需在 Header 携带 `Authorization: Bearer <token>`。

## 前端 `healthy-diet-app`

1. 用 **HBuilderX** 打开 `healthy-diet-app` 目录，选择「运行 -> 运行到浏览器」或运行到微信开发者工具。
2. 修改 `utils/config.js` 中 `API_BASE` 指向你的后端（真机调试不要用 `127.0.0.1`，应改为电脑局域网 IP）。
3. H5 若使用 `manifest.json` 里已配置的 `devServer.proxy` 将 `/api` 代理到 `8080`，可将 `API_BASE` 改为 `'/api'`。

**说明：** 拍照识别在 App/小程序端使用 `FileSystemManager` 读图为 Base64；若 H5 运行异常，可改为先上传图片到自建存储再由后端识别（当前实现为客户端 Base64 直传后端）。

## 模块与接口对应关系

1. **用户健康档案**：`/api/profile`、`/api/health-metrics`
2. **AI 配餐与食谱**：`/api/recipes`、`/api/meal-plans`、`/api/ai/meal-plan-draft`、`/api/favorites/recipes`
3. **饮食记录与识别**：`/api/diet-records`、`/api/diet-records/from-photo`、`recognize-preview`
4. **每日打卡**：`/api/check-ins`
5. **数据统计**：`/api/statistics/*`
6. **营养知识**：`/api/articles`、`/api/ingredients/search`、`/api/ai/nutrition-advice`

首次启动会自动插入少量示例文章与菜谱（见 `DataInitializer`）。

## 许可证

示例项目，按课程/作业要求自行声明版权与许可证。
