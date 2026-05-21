---
layer: Design
doc_no: "D-002"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: qq-like-im-mvp-architecture
purpose: "基于开源 IM 案例和 MallChat 当前后端事实，定义成熟但不过度设计的 QQ-like IM MVP 架构边界。"
canonical_path: "docs/design/D-002-qq-like-im-mvp-architecture.md"
status: draft
version: "0.1.1"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "docs/prd/P-007-qzone-like-moments-feed-prd.md"
  - "docs/design/D-001-im-projects-reference.md"
  - "https://docs.openim.io/guides/introduction/features"
  - "https://matrix.org/docs/matrix-concepts/rooms_and_events/"
  - "https://spec.matrix.org/latest/"
  - "https://docs.rocket.chat/docs/messages"
  - "https://docs.mattermost.com/end-user-guide/collaborate/communicate-with-messages.html"
  - "https://tailchat.msgbyte.com/"
outputs:
  - "QQ-like IM MVP 架构边界"
  - "动态 feed MVP 执行依据"
  - "OpenSpec change: add-moments-feed-mvp"
triggers:
  - "继续开发好友、群聊、动态、消息或通知能力"
  - "需要判断某个 IM 能力是否进入 MVP"
downstream:
  - "docs/prd/P-007-qzone-like-moments-feed-prd.md"
  - "openspec/changes/add-moments-feed-mvp"
---

# QQ-like IM MVP 架构设计

## 1. 背景

MallChat 已完成好友关系、私聊房间、群聊、消息、会话、WebSocket 运行契约、实时投递兜底、引用回复和已读统计等后端能力。新的产品目标是开发一款成熟但不过度设计的 QQ-like IM 系统，至少覆盖好友聊天、动态和群组。

本文档基于当前 MallChat 后端事实和开源 IM 案例，定义下一阶段最小闭环，避免把联邦、端到端加密、插件平台、音视频和复杂空间能力过早纳入后端。

## 2. 开源 IM 参考结论

| 项目 | 可借鉴点 | MallChat 采纳方式 | 暂不采纳 |
| --- | --- | --- | --- |
| OpenIM | 服务端与多端 SDK 分层，好友、群、会话、消息类型较全，私有化部署清晰 | 继续保持 `mallchat-cloud` 作为后端事实源，前端复用统一 API/WebSocket；动态与聊天消息分域 | 不引入完整 SDK 平台、不复制 Go 微服务拆分 |
| Matrix/Synapse | 房间与事件模型清晰，事件需要服务端校验，房间权限有层级 | 保持 `chat_room + chat_message` 事实模型，新增动态也按事实表建模并做权限校验 | 不做联邦、事件 DAG、跨 homeserver 一致性 |
| Rocket.Chat | 所有聊天发生在 room，支持 DM、群、文件、提及、通知、输入状态 | MallChat 的私聊/群聊继续统一走 room；文件消息、通知和会话刷新继续复用现有链路 | 不做企业协作频道、音视频、位置分享 |
| Mattermost | 消息、线程、未读、搜索、文件、通知是成熟聊天体验核心 | 继续推进引用、已读、文件和通知；动态评论先做一级评论，不把动态评论做成聊天线程 | 不做复杂线程收件箱、优先级消息、工作流集成 |
| Tailchat | 插件化适合长期扩展自定义面板和集成 | 当前只保留“可扩展边界”意识，动态 feed 独立建模 | 不引入插件系统、开放平台和二级工作区 |

## 3. MallChat MVP 能力边界

### 3.1 必须完成

1. 好友聊天：好友申请、发现、列表、删除、稳定私聊房间、私聊消息权限。
2. 群组：创建、邀请、成员列表、群资料更新、群主移除普通成员、退群、解散。
3. 消息：文本、图片、文件、引用回复、撤回、发送幂等、历史分页、重连补偿游标。
4. 实时：notification-service 承载 WebSocket，MQ 推送、会话刷新、在线状态、缓存缺失兜底。
5. 已读与未读：会话未读、读边界上报、消息级已读统计摘要。
6. 动态 feed：文字/图片发布、好友可见列表、删除自己的动态、点赞、评论、互动通知。其中 `add-moments-feed-mvp` 只完成发布/列表/删除基础切片，完整动态 MVP 还需要后续互动 change。
7. 通知：好友申请、好友通过、消息/会话、群治理、动态点赞评论均进入统一通知入口。

### 3.2 暂缓能力

- 端到端加密、联邦互通、跨服务事件 DAG。
- 语音/视频通话、语音消息、视频消息和直播聊天室。
- 空间装扮、访客记录、相册体系、公开广场、推荐流。
- 插件平台、开放 API 应用市场、企业频道治理。
- 动态评论二级回复、复杂可见性、内容审核后台。

## 4. 架构决策

### 4.1 动态必须独立于聊天消息

动态 feed 不写入 `chat_message`，也不产生 `chat_session` 未读。聊天消息是房间内实时通讯事实，动态是好友关系上的社交内容事实，两者生命周期、权限和查询模型不同。

建议新增事实模型：

1. `chat_moment`：动态主体，保存作者、正文、状态、统计数和发布时间。
2. `chat_moment_media`：动态媒体，保存图片 URL、宽高、大小和排序。
3. `chat_moment_like`：点赞事实，按 `moment_id + user_id` 唯一。
4. `chat_moment_comment`：一级评论事实，保存评论者、内容和状态。

### 4.2 权限先用好友关系

MVP 只做好友可见：

1. 作者本人始终可见自己的动态。
2. 互为好友的用户可在好友动态流看到对方未删除动态。
3. 删除好友后，后续好友流不再返回对方动态。
4. 点赞和评论必须先通过同一套好友可见校验。

### 4.3 通知只做展示入口

点赞和评论成功后可调用 notification-service 生成互动提醒，但通知不是动态事实来源。通知失败不能回滚已经成功的动态互动事实；失败应记录日志并可通过后续补偿或重试增强。

### 4.4 TDD 门禁

新增动态接口必须先写失败测试，再写最小实现。至少覆盖：

1. 发布：空正文且无图拒绝，超长正文拒绝，合法发布成功。
2. 列表：作者本人可见，好友可见，非好友不可见，删除后不可见。
3. 删除：作者可删，非作者拒绝，重复删除幂等。
4. 点赞：好友可点赞，非好友拒绝，重复点赞幂等或明确取消。
5. 评论：好友可评论，空内容拒绝，非好友拒绝。
6. 通知：互动通知失败不回滚主事实。

## 5. 执行顺序

1. `add-moments-feed-mvp`：动态主体、媒体、发布、好友流、删除。归档该 change 只代表动态基础切片完成。
2. `enhance-moments-interaction`：点赞、评论和互动通知。
3. `align-mobile-moments-page`：Taro 动态页接入真实后端。
4. `enhance-im-notification-center`：统一通知中心类型、跳转参数和未读展示。

## 6. 验收门禁

- OpenSpec change 通过 `openspec validate add-moments-feed-mvp --strict`。
- 新增后端代码遵循红绿测试，相关测试至少覆盖 `ChatMomentServiceImplTest`。
- chat-service 回归通过 `mvn -pl :mallchat-chat-service -am test`。
- OpenSpec 全量通过 `openspec validate --all --strict`。
- PRD、设计、OpenSpec 和 PR/Issue 验收结论可互相追踪。

## 7. 风险与边界

- 动态 feed 引入新表，若直接跳过 migration 或 SQL 说明，后续环境无法落库。
- 好友可见规则必须统一，不应在列表、详情、点赞、评论各自散落不同判断。
- 统计数可先同步更新；高并发计数、反作弊、审核和推荐流均不进入 MVP。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 初始化 QQ-like IM MVP 架构设计与开源案例参考 |
| 2026-05-21 | StephenQiu30 | 0.1.1 | 移除对一次性 Superpowers 过程计划的长期引用 |
