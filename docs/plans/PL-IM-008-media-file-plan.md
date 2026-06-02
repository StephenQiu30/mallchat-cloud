---
layer: Plan
doc_no: "PL-IM-008"
audience:
  - PM
  - Dev
  - QA
feature_area: file-media
purpose: "定义文件与媒体能力的分阶段执行计划，拆解 P0/P1/P2 子任务，明确依赖关系和交付顺序。"
canonical_path: "docs/plans/PL-IM-008-media-file-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-008-media-file-prd.md"
outputs:
  - "P0/P1/P2 子 Issue 清单与验收依据"
triggers:
  - "开始实现文件与媒体相关功能前"
  - "需要了解任务依赖和交付顺序时"
downstream:
  - "docs/acceptance/AC-IM-008-media-file-acceptance.md（待创建）"
  - "docs/design/D-IM-008-media-file-design.md（待创建）"
---

# PL-IM-008 文件与媒体 执行计划

## 1. 背景

本计划基于 `docs/prd/P-IM-008-media-file-prd.md`，将文件与媒体能力拆解为 P0（安全与基础）、P1（核心功能）、P2（增强能力）三个阶段，每个阶段产出独立的 Linear 子 Issue，遵循 TDD 红绿重构流程执行。

## 2. 目标

按优先级分阶段交付文件与媒体能力，确保：
- P0 阶段完成安全校验和审计基础，无安全债务。
- P1 阶段完成媒体消息和动态引用的端到端闭环。
- P2 阶段按需增强访问控制和运维能力。

## 3. 非目标

- 不在本计划中实现具体代码（代码实现由各子 Issue 的 TDD 流程驱动）。
- 不重新设计已有架构（沿用 Controller + Service + ServiceImpl + Mapper 风格）。
- 不引入新的存储后端或中间件。

## 4. 核心内容

### 4.1 阶段总览

```
P0 安全与基础 ──→ P1 核心功能 ──→ P2 增强能力
  │                 │                │
  ├─ 上传校验加固    ├─ 媒体消息闭环  ├─ 访问权限控制
  ├─ 审计能力补全    ├─ 消息引用标准化 ├─ 存储配额管理
  └─ 仓库守护规则    ├─ 动态媒体闭环  └─ 文件去重
                     └─ WebSocket 推送
```

### 4.2 P0：安全与基础（阻塞级）

P0 聚焦安全、权限和可观测性，是后续所有功能的前提。

| Issue 编号 | 标题 | 范围 | 验收标准 |
|-----------|------|------|----------|
| STE-133 | [P0] 文件上传校验加固 | 补充边界测试：超大文件、空文件、危险文件名、伪造 magic byte；确保所有 5 种 bizType 均有正向和负向测试 | `FileUploadValidatorTest` 覆盖所有 bizType 的正向/负向用例；`bash scripts/validate-repository.sh` 通过 |
| STE-134 | [P0] 上传审计能力补全 | 确保上传成功和失败均有审计记录；审计日志可按用户 ID 和时间范围查询；补充 `FileUploadRecordRecorder` 边界测试 | 审计记录写入 `file_upload_record` 表；查询接口支持 userId + 时间范围过滤；测试通过 |
| STE-135 | [P0] 文件服务仓库守护规则 | 补充 `FileUploadContractConsistencyTest`：验证接口 Content-Type、最大文件大小、bizType 枚举完整性 | 守护测试覆盖 multipart 配置、枚举值、返回类型；CI 可运行 |

### 4.3 P1：核心功能（价值级）

P1 聚焦媒体消息和动态引用的端到端闭环。

| Issue 编号 | 标题 | 范围 | 验收标准 |
|-----------|------|------|----------|
| STE-136 | [P1] 图片消息端到端闭环 | 完善 IMAGE 消息的 extra 校验（url/width/height/size）；补充发送和接收流程测试；确保 WebSocket 推送包含完整媒体元数据 | `ChatMessageHelper` IMAGE 校验测试通过；消息发送 → WebSocket 推送 → 接收方解析全链路测试通过 |
| STE-137 | [P1] 文件消息端到端闭环 | 完善 FILE 消息的 extra 校验（url/name/ext/size）；补充文件消息的发送、展示和下载流程测试 | FILE 消息 extra 校验测试通过；消息投递测试通过 |
| STE-138 | [P1] 语音消息端到端闭环 | 完善 VOICE 消息的 extra 校验（url/format/duration/size）；补充语音消息播放流程测试 | VOICE 消息 extra 校验测试通过；消息投递测试通过 |
| STE-139 | [P1] 视频消息端到端闭环 | 完善 VIDEO 消息的 extra 校验（url/format/duration/size/width/height）；补充视频消息播放流程测试 | VIDEO 消息 extra 校验测试通过；消息投递测试通过 |
| STE-140 | [P1] 消息引用（回复）媒体预览 | 确保回复媒体消息时，`replyMsg` 包含原消息的媒体预览信息；补充回复媒体消息的测试 | 回复 IMAGE/FILE/VOICE/VIDEO 消息时，接收方能看到原消息预览；测试通过 |
| STE-141 | [P1] 动态媒体引用闭环 | 完善动态发布时的媒体校验（URL 归属、类型一致性、数量限制）；补充动态删除时的媒体清理逻辑测试 | 动态发布最多 9 个媒体、URL 非空校验、媒体列表查询测试通过 |

### 4.4 P2：增强能力（体验级）

P2 聚焦访问控制和运维增强，按需实施。

| Issue 编号 | 标题 | 范围 | 验收标准 |
|-----------|------|------|----------|
| STE-142 | [P2] 文件访问权限控制 | 评估并实现基于会话成员的文件访问控制；非成员无法访问私聊媒体文件 | 访问控制测试覆盖：本人、会话成员、非成员三种角色；测试通过 |
| STE-143 | [P2] 文件去重（MD5） | 利用 `file_upload_record.md5` 字段实现上传去重；相同文件不重复存储 | 去重测试通过；相同文件上传返回已有 URL |
| STE-144 | [P2] 存储配额管理 | 按用户或会话维度限制上传总量；超限时返回明确错误 | 配额检查测试通过；超限返回 4xx 错误 |

### 4.5 依赖关系

```
STE-133 (P0 校验) ──→ STE-142 (P2 访问权限)
STE-134 (P0 审计) ──→ STE-143 (P2 文件去重)
                   ──→ STE-144 (P2 配额管理)
STE-136~139 (P1 媒体消息) ──→ STE-140 (P1 消息引用)
STE-135 (P0 守护) ──→ 独立，无下游依赖
STE-141 (P1 动态媒体) ──→ 独立，无下游依赖
```

- P0 三个 Issue 可并行执行，互不阻塞。
- P1 依赖 P0 完成（安全校验和审计是功能前提）。
- P1 内部各 Issue 可并行执行（不同消息类型互不依赖）。
- STE-88（消息引用）依赖 STE-84~87（各类型消息闭环）。
- P2 依赖 P1 完成。

### 4.6 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| P1 并行开发时 ChatMessageHelper 冲突 | 中 | 每种消息类型独立分支，合并前回归测试 |
| COS 签名 URL 方案不确定 | 低 | P2 阶段再评估，不阻塞 P0/P1 |
| 动态媒体删除与 COS 清理不一致 | 低 | 先保证 DB 一致性，COS 清理作为后续优化 |

## 5. 关联文档

### 5.1 输入文档

1. `docs/prd/P-IM-008-media-file-prd.md`

### 5.2 输出文档

1. `docs/plans/PL-IM-008-media-file-plan.md` — 本文档

### 5.3 下游文档

1. Linear 子 Issue（STE-81 ~ STE-92）
2. `docs/acceptance/AC-IM-008-media-file-acceptance.md`（待创建）

## 6. 验收门禁

- [ ] P0 子 Issue（STE-133、STE-134、STE-135）已创建并关联 Epic
- [ ] P1 子 Issue（STE-136 ~ STE-141）已创建并关联 Epic
- [ ] P2 子 Issue（STE-142、STE-143、STE-144）已创建并关联 Epic
- [ ] 每个子 Issue 包含明确的验收标准和 TDD 要求
- [ ] 依赖关系在 Issue 的 `blockedBy` 中正确表达

## 7. 风险与边界

见 4.6 节。

## 8. 待确认问题

1. STE-90（访问权限控制）的技术方案需要先完成 Design 文档评审。
2. STE-91（配额管理）的配额阈值需要产品侧确认。
3. STE-92（文件去重）是否需要在 P0 阶段提前实现？（当前 md5 字段已存在但未使用）

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-03 | StephenQiu30 | 0.1.0 | 初始化文档，拆解 P0/P1/P2 子任务 |
