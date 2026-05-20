## ADDED Requirements

### Requirement: RabbitMQ publish outcomes are observable
The system SHALL record RabbitMQ publish outcomes with enough business context to locate the message type and business id.

#### Scenario: Publish is accepted by RabbitTemplate
- **WHEN** `RabbitMqSender` sends a non-null payload
- **AND** RabbitTemplate accepts the send call
- **THEN** the system SHALL record a publish outcome tagged with `bizType` and result `accepted`
- **AND** the RabbitMQ message SHALL carry `bizType` and `bizId` in message headers

#### Scenario: Publish throws before accepted
- **WHEN** `RabbitMqSender` sends a non-null payload
- **AND** RabbitTemplate throws during publish
- **THEN** the system SHALL record a publish outcome tagged with `bizType` and result `failed`
- **AND** the original publish exception SHALL NOT be swallowed

#### Scenario: Publish payload is rejected before RabbitMQ
- **WHEN** `RabbitMqSender` receives a null payload
- **THEN** the system SHALL record a publish outcome tagged with `bizType` and result `rejected`
- **AND** RabbitTemplate SHALL NOT be invoked

#### Scenario: Broker confirm and return callbacks are observable
- **WHEN** RabbitMQ confirm or return callbacks are invoked
- **THEN** the system SHALL record the callback outcome tagged with the message `bizType`
- **AND** the callback log SHALL retain the message business id when available
