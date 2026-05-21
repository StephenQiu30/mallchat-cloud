## ADDED Requirements

### Requirement: 视频上传应使用独立 chat_video 边界
The system SHALL validate video uploads using a dedicated `chat_video` business type.

#### Scenario: 合法视频文件上传
- **WHEN** a user uploads a supported video file through `chat_video`
- **THEN** the upload boundary accepts the file before storage

#### Scenario: 伪造或不支持的视频文件上传
- **WHEN** a user uploads a forged or unsupported video file through `chat_video`
- **THEN** the upload boundary rejects the file with a parameter error
