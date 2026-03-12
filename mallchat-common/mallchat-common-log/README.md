# mallchat-common-log - 分布式日志采集组件

基于 AOP 的无侵入式操作日志记录模块，助力微服务生态实现全链路审计与业务行为追踪。

## 🌟 核心特性

- **🚀 零侵入记录**: 仅需一个 `@OperationLog` 注解即可自动采集请求路径、参数、执行时间及异常堆栈。
- **🧩 插件化处理**: 各微服务可通过实现 `OperationLogService` 接口，自由定义日志的落放策略（如异步写入
  `mallchat-log-service`）。
- **📊 维度丰富**: 自动采集当前登录用户、客户端 IP、模块名称及操作动作。
- **🛡️ 生产级可靠**: 基于 `@Async` 异步处理，全流程捕获异常，确保日志上报不影响核心业务吞吐。

## 🏗️ 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.stephen.cloud</groupId>
    <artifactId>mallchat-common-log</artifactId>
</dependency>
```

### 2. 使用方法

在 Controller 方法上标注：

```java
@PostMapping("/update")
@OperationLog(module = "订单中心", action = "更新订单状态")
public BaseResponse<Boolean> updateOrder(...) {
    // 业务逻辑
}
```

## 🛠️ 内部机制

- **切面拦截**: `OperationLogAspect` 统一拦截标注了注解的方法。
- **上下文感知**: 自动构建 `OperationLogContext`，并透传给具体的处理实现。

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
