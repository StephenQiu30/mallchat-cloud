# sensitive-operation-audit Specification

## Purpose
TBD - created by archiving change harden-sensitive-operation-audit. Update Purpose after archive.
## Requirements
### Requirement: Sensitive operation audit masks secrets
The operation audit system SHALL record sensitive IM operations without storing raw secret request fields.

#### Scenario: Sensitive request parameters are masked
- **WHEN** an audited operation receives request parameters containing token, password, secret, or code fields
- **THEN** the audit request parameters SHALL mask those field values
- **AND** the audit log SHALL keep non-sensitive operation metadata such as module, action, path, operator, and success status

#### Scenario: Failed sensitive operation is audited
- **WHEN** an audited operation throws an exception
- **THEN** the audit system SHALL record the failed status and error message
- **AND** the original exception SHALL still be propagated to the caller

#### Scenario: Chat service sensitive operation is persisted
- **WHEN** chat-service records an audited friend, group, message, or moment operation
- **THEN** the service SHALL forward the operation context to mallchat-log-service
- **AND** a logging failure SHALL NOT interrupt the original chat business flow

#### Scenario: Audit log can be traced by business id
- **WHEN** an audited operation request contains a business identifier such as `roomId`, `messageId`, `momentId`, `applyId`, or `bizId`
- **THEN** the audit context SHALL preserve the identifier as `bizId`
- **AND** a specific business identifier SHALL take precedence over a generic `id`
- **AND** mallchat-log-service SHALL support filtering operation logs by `bizId`

#### Scenario: Masking fallback does not leak raw secrets
- **WHEN** request parameter serialization or masking cannot safely inspect an argument
- **THEN** the audit request parameters SHALL record a safe placeholder with the argument type
- **AND** the audit request parameters SHALL NOT call the original argument `toString()` value

#### Scenario: File upload operation records audit facts
- **WHEN** a file upload succeeds or fails
- **THEN** file-service SHALL send a file upload record to mallchat-log-service
- **AND** the record SHALL include user id, business type, file metadata, client IP, storage type, status, and error message when failed
- **AND** the asynchronous recorder SHALL use immutable file metadata captured before leaving the request thread

#### Scenario: Audit recording is asynchronous for business services
- **WHEN** chat-service or file-service records operation or upload audit facts
- **THEN** audit recording SHALL be enabled for asynchronous execution
- **AND** audit service failures SHALL NOT interrupt the original business flow

