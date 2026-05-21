---
layer: Acceptance
doc_no: "A-023"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-rich-message-forward
purpose: "记录 m8 富消息与单条转发 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-023-m8-rich-message-forward-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #36"
  - "GitHub Issue #37"
  - "GitHub Issue #38"
  - "GitHub Issue #39"
  - "GitHub Issue #40"
  - "GitHub Issue #41"
  - "openspec/changes/add-voice-message-mvp"
  - "openspec/changes/add-video-message-mvp"
  - "openspec/changes/add-sticker-message-mvp"
  - "openspec/changes/add-message-forward-mvp"
  - "openspec/changes/design-merged-forward-message"
outputs:
  - "m8-backend-rich-message-epic"
  - "语音消息 MVP"
  - "视频消息 MVP"
  - "表情贴纸消息 MVP"
  - "单条消息转发 MVP"
  - "合并转发后端骨架设计"
triggers:
  - "创建或更新 m8 PR"
  - "回归富消息与转发体验 Epic #36"
downstream:
  - "GitHub Epic #36"
---

# m8 富消息与转发体验验收

## 1. 验收范围

本次 m8 聚合消费 Epic #36 下的 #37、#38、#39、#40，并对 #41 只交付后端骨架设计。实现保持最小可用闭环：新增语音、视频、表情贴纸消息类型和 extra 校验；会话列表使用稳定占位预览；文件服务新增 `chat_voice` / `chat_video` 上传边界；单条转发复用现有 `sendMessage` 链路，不新增平行表和推送协议。

## 2. 结论

1. #37：语音消息接受 `url`、`format`、`duration`、`size`，会话预览为 `[语音]`，上传边界使用 `chat_voice`。
2. #38：视频消息接受 `url`、`format`、`duration`、`size`、`width`、`height`，会话预览为 `[视频]`，上传边界使用 `chat_video`。
3. #39：表情贴纸消息接受 `stickerId`、`name`、`url`，会话预览为 `[表情]`。
4. #40：单条转发校验来源房间可见、目标房间可发送、来源消息正常，然后复制 `type`、`content`、`extra` 到目标房间并复用普通发送链路；私聊目标房间必须确认当前用户是 `userLow` 或 `userHigh`。
5. #41：合并转发不在 m8 强行实现，已写入 design，后续 change 再做快照和详情页契约。

## 3. RED 证据

1. `FileUploadValidatorTest` 初次编译失败：`FileUploadBizEnum.CHAT_VOICE` 和 `CHAT_VIDEO` 不存在。
2. `ChatMessageHelperTest` 新增富消息测试要求 `ChatMessageTypeEnum.VOICE`、`VIDEO`、`STICKER` 和对应 extra 校验。
3. `ChatMessageServiceImplTest` 新增转发测试要求 `forwardMessage` 服务方法，并要求失败时不落库不推送。
4. 代码审查补充发现私聊目标房间权限漏洞后，`ChatMessageServiceImplTest` 新增私聊非成员普通发送和转发测试，初次失败：未抛出 `BusinessException`。

## 4. GREEN 命令

```bash
mvn -B -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-file-service -am -Dtest=ChatMessageHelperTest,ChatMessageServiceImplTest,ChatSessionServiceImplTest,FileUploadValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-file-service -am test
```

focused tests 通过 81 个测试；chat/file 相关模块扩展测试通过 209 个测试。

## 5. CI 补充

`.github/workflows/ci.yml` 的 backend focused tests 已把 `ChatMessageHelperTest` 纳入 chat-service 质量门禁，文件上传边界继续通过 `FileUploadValidatorTest` 覆盖。

## 6. 残余风险

1. m8 不包含端侧录音、录像、播放、贴纸选择器或上传 UI。
2. m8 不做语音转文字、视频转码、封面生成或内容审核。
3. 合并转发只保留设计骨架，避免在快照策略未稳定前扩大表结构。

## 7. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m8 富消息与转发体验验收 |
