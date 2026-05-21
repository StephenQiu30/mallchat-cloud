# add-video-message-mvp

## Why

P2 富消息体验需要基础视频消息。当前后端没有视频消息类型和上传边界，端侧无法稳定发送短视频。

## What Changes

- 新增视频消息类型和 `extra` 校验。
- 会话预览显示 `[视频]`。
- 文件服务新增 `chat_video` 上传边界。

## Non-Goals

- 不做转码、封面自动生成、审核平台或播放器能力。
- 不引入新的消息表或对象存储适配。

## Linked Issues

- #36
- #38
