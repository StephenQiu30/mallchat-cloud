## ADDED Requirements

### Requirement: 语音上传应使用独立 chat_voice 边界
The system SHALL validate voice uploads using a dedicated `chat_voice` business type.

#### Scenario: 合法语音文件上传
- **WHEN** a user uploads a supported voice file through `chat_voice`
- **THEN** the upload boundary accepts the file before storage

#### Scenario: 伪造或不支持的语音文件上传
- **WHEN** a user uploads a forged or unsupported voice file through `chat_voice`
- **THEN** the upload boundary rejects the file with a parameter error
