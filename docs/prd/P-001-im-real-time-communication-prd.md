---
layer: PRD
doc_no: "P-001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-real-time-communication
purpose: "定义 MallChat QQ-like IM 产品的 MVP 范围、跨端职责、验收边界和后续阶段。"
canonical_path: "docs/prd/P-001-im-real-time-communication-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../design/D-001-im-projects-reference.md"
  - "../../openspec/specs/chat-friend/spec.md"
  - "../../openspec/specs/chat-message/spec.md"
  - "../../openspec/specs/chat-online-status/spec.md"
  - "../../openspec/specs/chat-room-access/spec.md"
  - "../../openspec/specs/chat-session/spec.md"
outputs:
  - "MallChat IM MVP 产品范围"
  - "跨端职责矩阵"
  - "OpenSpec change: orchestrate-im-product-mvp"
triggers:
  - "新增 IM 核心功能"
  - "调整移动端消息、联系人、动态或聊天体验"
  - "需要判定功能属于 MVP 还是后续阶段"
downstream:
  - "../plans/PL-001-im-mvp-openspec-plan.md"
  - "../acceptance/A-002-im-mvp-research-acceptance.md"
---

# MallChat IM 实时通讯产品 PRD

## 1. 背景

MallChat 当前已经具备 Java 后端 chat 领域模型、小程序端 Taro 页面、UniApp/Flutter 多端雏形和管理端后台。用户希望产品能力参考 QQ 与主流 IM 系统，并根据 `demo.html` 还原消息、联系人、动态和聊天详情体验。

本 PRD 用于统一产品范围，避免各端在“IM 是什么、首版做什么、后续做什么”上分叉。

## 2. 产品目标

1. 以 QQ-like 社交通讯为方向，首版形成“好友关系 -> 会话列表 -> 聊天详情 -> 实时消息 -> 未读/已读反馈”的闭环。
2. 以 Taro 小程序端作为 `demo.html` 视觉还原主端，UniApp 与 Flutter 后续对齐同一设计语言。
3. 后端保持 `chat-*` 能力命名，继续围绕好友、房间、消息、会话、在线状态和通知演进。
4. 所有新增能力必须能通过 OpenSpec、CI 命令或页面状态验证。

```gherkin
Given 用户已登录并拥有好友或群聊
When 用户打开 MallChat 移动端
Then 系统应展示最近会话、未读数、好友在线状态和最新消息摘要
And 用户可以进入聊天详情发送文本或图片消息
```

## 3. 非目标

- 本阶段不实现端到端加密、复杂空间装扮、表情商城、直播聊天室和超大群体系。
- 本阶段不重写后端微服务架构，不新增与 `chat-*` 并行的数据层。
- 本阶段不要求 UniApp、Flutter、管理端一次性完成与 Taro 完全一致的视觉改造。
- 已读回执、消息编辑、语音通话、视频通话列为后续增强，不阻塞 MVP。

## 4. 核心用户故事

### 4.1 会话用户

作为普通用户，我希望打开应用后立即看到最近联系人、未读数量、@我或群聊提示，以便快速判断需要处理的消息。

验收标准：
- 会话列表展示头像、名称、最新消息、时间、未读数和置顶状态。
- 群聊和单聊具有可区分的视觉标识。
- 在线好友可显示在线状态。

### 4.2 联系人用户

作为普通用户，我希望查看好友、处理好友申请并进入私聊，以便完成从关系到聊天的闭环。

验收标准：
- 联系人页提供新朋友、群通知、搜索发现和好友分组。
- 点击好友必须进入真实私聊房间，不应把用户 ID 当作房间 ID 使用。
- 好友申请通过后能创建或复用稳定私聊房间。

### 4.3 聊天用户

作为聊天用户，我希望聊天详情页像常见 IM 一样支持左右气泡、时间分隔、输入栏、图片/文件入口和消息撤回，以便完成基础沟通。

验收标准：
- 自己消息右侧蓝色气泡，对方消息左侧白色气泡。
- 输入栏包含语音、文本输入、表情、图片/更多等高频入口。
- 新消息到达后刷新会话并上报已读边界。

### 4.4 运营/管理用户

作为运营或管理员，我希望管理后台能够逐步承接用户、群聊、消息、通知和日志管理，以便后续做内容治理和运营观察。

验收标准：
- 管理端后续任务不影响移动端 MVP 推进。
- 管理端以接口生成服务和 Ant Design Pro 表格/抽屉承接管理能力。

## 5. 职责矩阵

| 仓库 | 首版职责 | 本次是否执行代码 |
| --- | --- | --- |
| `mallchat-cloud` | chat 领域能力、WebSocket、消息/会话/好友/房间/通知接口 | 以 PRD/OpenSpec 编排为主 |
| `mallchat-taro` | `demo.html` 视觉还原主端、IM 主流程体验 | 是 |
| `mallchat-uniapp` | 对齐 Taro 体验的跨端备份实现 | 否，后续同步 |
| `mallchat_flutter` | 对齐移动端 IM 体验，兼顾原生质感 | 否，后续同步 |
| `mallchat-admin` | 后台管理、运营、审核、日志与通知管理 | 否，后续同步 |

## 6. 阶段划分

### MVP

1. 好友列表、好友申请、真实私聊房间。
2. 群聊创建、邀请、成员列表、退出/解散。
3. 会话列表、置顶、删除、未读数、最近消息。
4. 聊天详情、文本、图片、文件消息展示、撤回、已读上报。
5. 移动端消息、联系人、动态、聊天页统一 `demo.html` 风格。

### P1

1. 已读回执明细。
2. 消息引用、转发、更多文件类型。
3. 群公告、入群审核和群通知增强。
4. 管理端消息/群聊治理。

### P2

1. 语音消息、视频消息、音视频通话。
2. 空间动态发布、点赞、评论。
3. 多端离线缓存与漫游策略优化。

## 7. 首版验收门禁

- OpenSpec change `orchestrate-im-product-mvp` 和前端对应 change 均可通过 `openspec validate --strict`。
- Taro 端通过 `pnpm run build:weapp`。
- 后端通过 `mvn -B -DskipTests compile`。
- 文档必须记录本次调研、计划、设计和测试结论。

## 8. 风险与边界

- 当前 PRD 将 Taro 作为视觉主端，UniApp 和 Flutter 需后续按相同能力补齐，避免三端同时大改导致 CI 风险升高。
- WebSocket 真机链路、文件消息真实上传和跨端漫游需要运行环境配合，首轮以编译和静态链路为主。
- 已读回执细粒度成员列表属于增强能力，不纳入本次自动化执行。

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-19 | StephenQiu30 | 0.1.0 | 初始化 MallChat IM MVP PRD |
