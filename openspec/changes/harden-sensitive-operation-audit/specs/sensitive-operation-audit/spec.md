## ADDED Requirements

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
