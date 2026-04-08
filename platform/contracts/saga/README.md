# Saga 基线

## 首轮 Saga

### `MerchantProvisionSaga`

归属：`foundation-service`

步骤：

1. 激活组织
2. 激活卖家
3. 激活店铺
4. 写入 Outbox 事件

失败策略：

- 事务内失败直接回滚
- 事务外失败通过补偿命令和事件重放收敛

### `SubmitOrderSaga`

归属：`trade-service`

步骤：

1. 校验经营资格投影
2. 按 `seller_of_record` 分组
3. 创建订单与子订单
4. 写入 `OrderSubmitted` Outbox

失败策略：

- 事务内失败直接回滚
- 不留下脏订单、不写半截事件

## 专家门禁

- `Marty Cagan / Melissa Perri`：是否仍服务于“双服务 + 单场景 + 单主链路”
- `Eric Evans`：上下文命名是否稳定，是否把流程对象误当领域对象
- `Martin Fowler`：基础设施是否留在 `infrastructure`，是否出现未来难拆耦合
- `Gregor Hohpe`：事件语义是否清晰，失败路径是否有协议
- `David Ferraiolo`：治理命令是否职责分离，是否存在越权风险
- `Martin Kleppmann`：是否可幂等、可重放、可补偿
- `Luca Pacioli`：是否预留后续账务事实，是否错误写入账务语义
- `Saltzer & Schroeder`：是否默认拒绝、最小权限、审计完整
