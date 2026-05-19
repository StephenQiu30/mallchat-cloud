---
layer: PRD
doc_no: "P-005"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: message-media-and-rich-types
purpose: "定义 MallChat 文本、图片、文件、语音、视频、表情、引用、转发等富消息类型的后端增强边界。"
canonical_path: "docs/prd/P-005-message-media-and-rich-types-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "openspec/specs/chat-message/spec.md"
outputs:
  - "富消息与媒体消息增强需求"
  - "后续 OpenSpec change: enhance-message-media-rich-types"
triggers:
  - "新增消息类型"
  - "调整图片、文件、语音、视频或表情消息"
downstream:
  - "docs/design/"
  - "docs/acceptance/"
---

# 富消息与媒体消息 PRD

## 1. 背景

当前消息枚举仅支持文本、图片、文件。消息表已经包含 `content`、`extra json`、`type`、`reply_msg_id` 和 `status`，`ChatMessageHelper` 对图片和文件 extra 有结构校验。文件服务支持 `chat_image` 和 `chat_file` 上传。

QQ-like IM 通常还需要语音、视频、表情、位置、名片、引用、转发、合并转发和消息收藏。当前后端只具备富消息扩展的基础槽位，尚未形成完整类型体系和媒体元数据规范。

## 2. 产品目标

1. MVP 稳定支持文本、图片、文件和撤回。
2. P1 支持引用回复、转发、表情和语音消息。
3. P2 支持视频、位置、名片、收藏和合并转发。

```gherkin
Given 用户已上传一张聊天图片
When 用户发送图片消息
Then 消息 extra 应包含 url、width、height、size
And 会话列表应显示图片预览占位
```

## 3. 非目标

- 不在首版实现音视频通话。
- 不在消息表中直接保存大文件内容。
- 不把不同类型消息都压成纯文本字符串。

## 4. 核心用户故事

### 4.1 图片和文件用户

作为聊天用户，我希望发送图片和文件后，对方能看到稳定的预览、名称和大小。

验收标准：
- 文件服务返回的信息能满足消息 extra 校验，或后端提供补齐媒体元数据的能力。
- 图片消息至少包含 URL、宽、高、大小。
- 文件消息至少包含 URL、名称、大小、扩展名。

### 4.2 引用和转发用户

作为聊天用户，我希望可以引用或转发已有消息，保留上下文。

验收标准：
- 引用消息必须属于同一房间。
- 转发消息必须校验用户对原房间和目标房间的访问权限。
- 被撤回消息在引用预览中应脱敏。

### 4.3 语音和表情用户

作为移动端用户，我希望可以发送语音和表情，让聊天更接近 QQ 体验。

验收标准：
- 语音消息 extra 至少包含 URL、时长、大小和格式。
- 表情消息应区分系统表情、自定义表情和普通图片。
- 会话预览对每种类型有明确占位文案。

## 5. 数据与权限边界

- `chat_message.type` 是消息类型主字段。
- `chat_message.extra` 存结构化元数据，不直接存业务大对象。
- 文件 URL 应来自文件服务或可信对象存储。
- 所有消息发送仍必须经过房间成员权限校验。

## 6. 首版验收门禁

- 每新增一种消息类型，必须新增 helper 校验测试和会话预览测试。
- 图片/文件消息需验证上传返回值与消息 extra 结构一致。
- 引用、转发必须覆盖权限和撤回边界。

## 7. 风险与边界

- 如果端侧自行拼 extra，容易出现字段不一致，需要定义统一 DTO。
- 视频、语音涉及转码、时长识别和存储成本，应后置。

## 8. 待确认问题

- 是否需要后端解析图片宽高和文件扩展名？
- 表情是否先作为图片消息处理，还是独立消息类型？
- 转发是否允许跨群、跨好友转发？

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-19 | StephenQiu30 | 0.1.0 | 初始化富消息与媒体消息 PRD |
