## Why

P-005 富消息与媒体消息 PRD 已要求图片消息 `extra` 至少包含 `url/width/height/size`，文件消息 `extra` 至少包含 `url/name/size/ext`。当前 `ChatMessageHelper` 只检查字段存在且非 `null`，空 URL、空文件名、非正数大小、非数字宽高仍可能进入消息事实，影响端侧预览和文件展示稳定性。

本次变更只加固已有 `IMAGE` / `FILE` 类型的 `extra` 契约，不新增语音、视频、表情、转发或媒体处理链路。

## What Changes

- 图片消息：
  - `url` 必须非空。
  - `width`、`height`、`size` 必须为正数。
- 文件消息：
  - `url`、`name`、`ext` 必须非空。
  - `size` 必须为正数。
- 非法 `extra` 在发送前被拒绝，不保存消息、不触发推送。
- 会话预览继续保持 `[图片]` / `[文件]` 占位文案。

## Capabilities

### Modified Capabilities

- `chat-message`: 收紧图片/文件消息 `extra` 元数据校验。

## Impact

- 代码：
  - `ChatMessageHelper`
  - `ChatMessageServiceImpl` 发送链路回归测试
  - `ChatSessionServiceImpl` 会话预览回归测试
- 测试：
  - `ChatMessageHelperTest`
  - `ChatMessageServiceImplTest`
  - `ChatSessionServiceImplTest`
- 非目标：
  - 不新增 `VOICE`、`VIDEO`、表情、转发或合并转发。
  - 不做后端解析图片宽高、文件扩展名、文件大小或媒体转码。
