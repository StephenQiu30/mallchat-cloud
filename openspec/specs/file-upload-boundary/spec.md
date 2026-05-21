# file-upload-boundary Specification

## Purpose
TBD - created by archiving change harden-file-upload-boundary. Update Purpose after archive.
## Requirements
### Requirement: File upload accepts only safe MVP file boundaries
The system SHALL reject unsafe or unsupported uploaded files before object storage upload.

#### Scenario: Empty or oversized file is uploaded
- **WHEN** a file is empty or exceeds the business maximum size
- **THEN** the upload SHALL fail with a parameter error

#### Scenario: Dangerous file name is uploaded
- **WHEN** a file name contains path separators, control characters, is blank, or is too long
- **THEN** the upload SHALL fail with a parameter error

#### Scenario: Image business upload has forged content
- **WHEN** `user_avatar` or `chat_image` is uploaded with an image suffix but non-image bytes
- **THEN** the upload SHALL fail with a parameter error

#### Scenario: Unsupported file type is uploaded
- **WHEN** the suffix or content type is not in the business whitelist
- **THEN** the upload SHALL fail with a parameter error

### Requirement: 语音上传应使用独立 chat_voice 边界
The system SHALL validate voice uploads using a dedicated `chat_voice` business type.

#### Scenario: 合法语音文件上传
- **WHEN** a user uploads a supported voice file through `chat_voice`
- **THEN** the upload boundary accepts the file before storage

#### Scenario: 伪造或不支持的语音文件上传
- **WHEN** a user uploads a forged or unsupported voice file through `chat_voice`
- **THEN** the upload boundary rejects the file with a parameter error

### Requirement: 视频上传应使用独立 chat_video 边界
The system SHALL validate video uploads using a dedicated `chat_video` business type.

#### Scenario: 合法视频文件上传
- **WHEN** a user uploads a supported video file through `chat_video`
- **THEN** the upload boundary accepts the file before storage

#### Scenario: 伪造或不支持的视频文件上传
- **WHEN** a user uploads a forged or unsupported video file through `chat_video`
- **THEN** the upload boundary rejects the file with a parameter error

