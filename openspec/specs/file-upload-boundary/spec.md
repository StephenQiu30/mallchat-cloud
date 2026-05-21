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

