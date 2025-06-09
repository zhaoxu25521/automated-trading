# Netty WebSocket Client 使用示例

## 快速开始

### 1. 创建基本客户端

```java
// 创建WebSocket客户端
NettyClient<String> client = NettyClientFactory.createDefaultClient("ws://echo.websocket.org");

// 创建订阅管理器
DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);

// 添加消息处理器
subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
    @Override
    public boolean handle(String message, HandlerContext ctx) {
        System.out.println("Received: " + message);
        return true;
    }
});

// 创建连接管理器
DefaultConnectionManager connectionManager = new DefaultConnectionManager(
        "echo.websocket.org", 80, client);

// 启动连接
connectionManager.initConnection();

// 发送消息
client.send("Hello WebSocket!");

// 关闭连接(通常在应用退出时调用)
connectionManager.shutdown();
```

### 2. 使用SSL安全连接

```java
// 创建SSL客户端
NettyClient<String> client = NettyClientFactory.createDefaultClient("wss://echo.websocket.org");

// 其余代码与基本客户端相同...
```

### 3. 使用心跳机制

```java
// 创建带心跳的客户端(每5秒发送一次心跳)
NettyClient<String> client = NettyClientFactory.createClientWithHeartbeat(
        "ws://echo.websocket.org", 5, "HEARTBEAT");

// 处理心跳消息
subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
    @Override
    public boolean handle(String message, HandlerContext ctx) {
        if ("HEARTBEAT".equals(message)) {
            System.out.println("Heartbeat received");
        } else {
            System.out.println("Received: " + message);
        }
        return true;
    }
});
```

## 高级用法

### 主题订阅

```java
// 订阅特定主题
subscriptionManager.subscribe("market-data", new MessageHandler<String>() {
    @Override
    public boolean handle(String message, HandlerContext ctx) {
        System.out.println("Market data update: " + message);
        return true;
    }
});

// 取消订阅
subscriptionManager.unsubscribe("market-data", handler);
```

### 自定义重连策略

```java
// 自定义重试次数(5次)和间隔(2秒)
DefaultConnectionManager connectionManager = new DefaultConnectionManager(
        "echo.websocket.org", 80, client, 5, 2000);
```

## 最佳实践

1. **连接管理**:
   - 在应用启动时初始化连接
   - 在应用关闭时调用shutdown()
   - 监听网络状态变化触发重连

2. **错误处理**:
   - 为所有处理器添加异常捕获
   - 记录连接状态变化

3. **性能优化**:
   - 对于高频消息，考虑使用批处理
   - 避免在处理器中执行耗时操作

## 依赖配置

确保项目中包含以下依赖:

```xml
<!-- Netty -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.68.Final</version>
</dependency>

<!-- SLF4J -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.32</version>
</dependency>
```
