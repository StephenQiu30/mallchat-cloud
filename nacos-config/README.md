# Nacos 配置管理与导入工具

本目录作为 `mallchat-cloud` 的配置中心源，存放全系统的 Nacos 配置文件模板。通过标准化的配置管理，实现微服务环境的快速迁移与统一管理。

## 📁 目录结构

| 文件/目录                      | 说明                                    |
|:---------------------------|:--------------------------------------|
| `common-*.yml`             | 基础组件配置（MySQL, Redis, RabbitMQ, Web 等） |
| `common-secret.properties` | 敏感信息模板（密码、密钥、身份令牌等）                   |
| `*-prod.yml`               | 生产环境专用覆盖配置                            |
| `import-config.sh`         | 基于 Nacos OpenAPI 的自动化导入脚本             |

## 🚀 配置导入流程

### 1. 自动化导入 (推荐)

系统内置了基于 `curl` 的导入脚本，可以快速将本地配置同步至 Nacos 服务器：

```bash
# 1. 赋予执行权限
chmod +x import-config.sh
# 2. 执行导入 (确保 Nacos 已启动)
./import-config.sh
```

> [!TIP]
> 默认导入地址为 `localhost:8848`，如需修改请编辑脚本开头的 `NACOS_ADDR` 变量。

### 2. 手动导入

在 Nacos 控制台：`配置管理` -> `配置列表` -> `更多` -> `导入配置` -> 选择本目录下的相关文件。

## ⚠️ 重要说明

1. **敏感信息**: 导入前请务必修改 `common-secret.properties` 中的数据库用户名/密码、OSS 密钥等。
2. **命名空间**: 默认导入到 `public` 命名空间，如需区分环境请先在 Nacos 中创建命名空间并修改脚本。
3. **数据分组**: 统一使用 `DEFAULT_GROUP`，除特殊业务需求外不建议修改。
