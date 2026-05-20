---
layer: Acceptance
doc_no: "A-008"
audience:
  - Dev
  - QA
feature_area: message-media-extra-contract
purpose: "记录图片/文件消息 extra 契约加固的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-008-message-media-extra-contract-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-005-message-media-and-rich-types-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-message-media-extra-contract"
outputs:
  - "图片/文件消息 extra 契约加固验收结论"
triggers:
  - "harden-message-media-extra-contract 完成后验收"
downstream:
  - "../../openspec/specs/chat-message/spec.md"
---

# 图片/文件消息 extra 契约加固验收

## 1. 变更验收范围

- 图片消息 `extra.url` 必须非空。
- 图片消息 `extra.width`、`extra.height`、`extra.size` 必须为正数。
- 文件消息 `extra.url`、`extra.name`、`extra.ext` 必须非空。
- 文件消息 `extra.size` 必须为正数。
- 非法图片/文件消息在发送前拒绝，不保存消息、不触发房间推送。
- 会话预览继续保持 `[图片]` / `[文件]`，不改变端侧展示契约。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-message-media-extra-contract --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMessageHelperTest,ChatMessageServiceImplTest,ChatSessionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `6 failures`，非法媒体 extra 未被拒绝 | 通过 |
| 绿灯 | 同上 | `Tests run: 44, Failures: 0, Errors: 0` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1，建议补数值字符串、缺失字段和文件空 URL 用例 | 已补充 |
| 补充测试绿灯 | 同上 | `Tests run: 48, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 130, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |

## 3. 结论

本切片只收紧已有 `IMAGE` / `FILE` 消息的 `extra` 元数据契约，不新增消息类型、表结构、媒体解析、转码或上传链路。非法媒体消息会在 `ChatMessageHelper.validate` 阶段被拒绝，因此不会进入消息持久化或 MQ 推送。

## 4. 残余风险

1. 历史库中已存在的脏图片/文件消息不会被本次变更自动清洗。
2. 后端仍不解析图片宽高、文件扩展名或文件大小，端侧/文件服务仍需提供可信元数据。
3. 语音、视频、表情、转发和合并转发仍属于 P-005 后续独立切片。
