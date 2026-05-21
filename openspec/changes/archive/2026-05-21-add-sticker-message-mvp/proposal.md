# add-sticker-message-mvp

## Why

基础 IM 体验需要表情/贴纸消息。后端只需要保存客户端选择的结构化贴纸信息，不应在 P2 引入表情商城或复杂资源管理。

## What Changes

- 新增贴纸消息类型和 `extra` 校验。
- 会话预览显示 `[表情]`。

## Non-Goals

- 不实现表情商城、付费表情、收藏同步或贴纸包管理。
- 不新增贴纸资源表。

## Linked Issues

- #36
- #39
