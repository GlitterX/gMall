# 事件契约基线

## 统一事件信封

所有跨服务事件统一使用以下字段：

- `eventId`
- `eventType`
- `aggregateType`
- `aggregateId`
- `version`
- `occurredAt`
- `idempotencyKey`
- `payload`

## foundation -> trade

Topic：`foundation.events`

首轮已实现事件：

- `OrganizationActivated`
- `OrganizationFrozen`
- `SellerActivated`
- `SellerSuspended`
- `StorefrontActivated`
- `StorefrontFrozen`

`payload` 字段说明：

- `organizationId`
- `sellerId`
- `storefrontId`
- `status`
- `operatorId`
- `operationReason`

## trade -> downstream

Topic：`trade.events`

首轮已实现事件：

- `OrderSubmitted`
- `OrderRejected`

首轮预留但未实现事件：

- `PaymentSucceeded`
- `RefundCompleted`
- `SettlementTriggered`

`OrderRejected.payload` 字段说明：

- `businessIdempotencyKey`
- `buyerId`
- `reasonCode`
- `reasonMessage`

## 设计约束

- 事件名必须表达业务事实，不表达技术动作
- Producer 只负责至少一次投递，不承担下游一致性兜底
- Consumer 必须基于 `eventId` 或业务幂等键实现幂等
- 死信、重放、补偿是主链路的一部分，不属于“异常情况以后再补”
