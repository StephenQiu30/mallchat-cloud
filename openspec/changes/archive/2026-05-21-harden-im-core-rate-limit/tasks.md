## 1. OpenSpec

- [x] 1.1 创建 `harden-im-core-rate-limit` change。
- [x] 1.2 明确 IM 核心写接口专用限流的最小范围：消息发送、好友申请、动态发布、文件上传。
- [x] 1.3 运行 `openspec validate harden-im-core-rate-limit --strict`。

## 2. TDD

- [x] 2.1 先补 Gateway 配置测试，覆盖 `/api/chat/message/send`、`/api/chat/friend/apply/add`、`/api/chat/moment/publish`、`/api/file/upload` 专用路由。
- [x] 2.2 先补 Gateway 配置测试，覆盖专用路由优先于 `/api/chat/**` 通用路由。
- [x] 2.3 运行目标测试确认红灯来自缺失专用路由。
- [x] 2.4 补充 Gateway 行为测试，覆盖四个核心写接口在限流允许时放行、拒绝时返回 429。

## 3. Implementation

- [x] 3.1 最小修改 `application.yml`，增加核心写接口专用限流路由。
- [x] 3.2 保持通用聊天路由继续覆盖其他 `/api/chat/**` 接口。
- [x] 3.3 保持通用文件路由继续覆盖其他 `/api/file/**` 接口。

## 4. Validation

- [x] 4.1 运行 Gateway 目标测试。
- [x] 4.2 运行 Gateway 模块测试。
- [x] 4.3 运行 OpenSpec strict 校验。
- [x] 4.4 更新 GitHub Issue #8 和 Epic 验收记录。
