# 加密货币合约策略交易系统详细设计文档（单体应用，优先 WebSocket 降级 REST）

## 1. 系统概述

### 1.1 目标
设计一个高性能、可扩展的单体加密货币合约策略交易系统，支持：
- 连接多个交易所（如 Binance、OKX、Bybit）。
- 支持多种交易对（如 BTC/USDT、ETH/USDT）和交易策略（如均线、网格）。
- 实时处理市场行情、订单、仓位和资金数据。
- **优先通过 WebSocket 订阅实时数据，降级使用 REST API 获取数据**，确保高实时性和可靠性。
- 使用领域驱动设计（DDD）架构，模块化设计，便于维护和后续扩展到分布式架构。
- 高并发处理能力，系统稳定运行。

### 1.2 技术栈
- **语言**: Java 17
- **架构**: 领域驱动设计（DDD）
- **缓存**: Redis 6.x
- **数据库**: MySQL 8.0,MyBatis-Plus
- **WebSocket**: Netty 4.1
- **REST Client**: OkHttp 4.10
- **框架**: Spring Boot 3.x
- **日志**: SLF4J + Logback
- **监控**: Prometheus + Grafana
- **测试**: JUnit 5, Mockito
- **容器化**: Docker（便于后续扩展到 Kubernetes）

### 1.3 系统架构图
```mermaid
graph TD
    A[表现层: REST API<br>Spring MVC + OkHttp] --> B[应用层: 应用服务<br>DTO、业务流程]
    B --> C[领域层: 实体、聚合根<br>交易所、账户、策略等]
    C --> D[基础设施层: MySQL、Redis<br>Netty、Prometheus]
```

### 1.4 模块划分
1. **交易所管理**：管理交易所信息（如 API 地址、支持的交易对）。
2. **交易所账户管理**：管理账户的 API Key 和 Secret Key。
3. **Socket 统一管理**：基于 Netty 封装 WebSocket，优先处理实时数据订阅。
4. **资金管理**：跟踪账户余额、保证金、冻结资金。
5. **仓位管理**：管理持仓信息（多仓/空仓、杠杆等）。
6. **订单管理**：处理订单创建、取消、状态更新。
7. **策略管理**：支持多种交易策略，动态加载和执行。

### 1.5 优先 WebSocket 降级 REST 策略
- **优先 WebSocket**：
  - WebSocket 提供低延迟、实时性高的数据推送，适合行情、订单、仓位、资金的实时更新。
  - 系统启动时自动订阅相关主题，保持长连接。
- **降级 REST**：
  - 检测 WebSocket 连接断开或数据异常（如长时间未收到更新）。
  - 自动切换到 REST API 轮询数据（如每 5 秒请求一次）。
  - REST 数据用于验证 WebSocket 数据的准确性（定时同步）。
- **切换机制**：
  - 通过 Redis 记录 WebSocket 连接状态（Key: `ws:{exchangeId}:status`）。
  - 使用 Spring Scheduler 定时检查连接状态，触发降级或恢复。
  - 降级时记录日志并触发 Prometheus 告警。

## 2. 模块详细设计

### 2.1 交易所管理
**功能**：
- 管理交易所信息（名称、API 地址、WebSocket 地址、支持的交易对、杠杆信息）。
- 提供交易所功能查询接口（如支持的合约类型、最大杠杆）。
- 支持动态添加新交易所。

**领域模型**：
- **Exchange（聚合根）**：
  - 属性：`id`, `name`, `restApiUrl`, `wsUrl`, `supportedSymbols`, `maxLeverage`, `status`
  - 方法：`getSupportedSymbols()`, `updateConfig()`, `validateApi()`
- **ExchangeConfig（值对象）**：存储 API 配置（如限频规则）。

**数据库表**：
```sql
CREATE TABLE exchange (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    rest_api_url VARCHAR(255) NOT NULL,
    ws_url VARCHAR(255) NOT NULL,
    supported_symbols JSON,
    max_leverage INT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**缓存**：
- Redis Key: `exchange:{name}:config`（TTL 24小时）。
- Redis Key: `exchange:{name}:symbols`（TTL 24小时）。

**实现细节**：
- 使用适配器模式（`ExchangeAdapter`）适配交易所 API 差异。
- 提供 WebSocket 和 REST 的统一接口，优先调用 WebSocket 方法。
- 单体架构下，适配器由 Spring 管理。

**示例代码**：
```java
public interface ExchangeAdapter {
    String getRestApiUrl();
    String getWsUrl();
    List<String> getSupportedSymbols();
    void validateApiKey(String apiKey, String secretKey);
    CompletableFuture<MarketData> getMarketData(String symbol); // WebSocket 优先
    CompletableFuture<MarketData> getMarketDataViaRest(String symbol); // REST 降级
}

@Component
public class BinanceAdapter implements ExchangeAdapter {
    @Autowired
    private WebSocketManager webSocketManager;
    @Autowired
    private OkHttpClient okHttpClient;

    @Override
    public CompletableFuture<MarketData> getMarketData(String symbol) {
        if (webSocketManager.isConnected("binance")) {
            return webSocketManager.getMarketData(symbol);
        }
        return getMarketDataViaRest(symbol); // 降级到 REST
    }

    @Override
    public CompletableFuture<MarketData> getMarketDataViaRest(String symbol) {
        Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/ticker/bookTicker?symbol=" + symbol)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try (Response response = okHttpClient.newCall(request).execute()) {
                return parseMarketData(response.body().string());
            } catch (IOException e) {
                throw new RuntimeException("REST API failed", e);
            }
        });
    }
}
```

### 2.2 交易所账户管理
**功能**：
- 管理用户在交易所的账户（API Key、Secret Key）。
- 支持多账户配置，加密存储敏感信息。
- 提供账户状态管理。

**领域 model**：
- **ExchangeAccount（聚合根）**：
  - 属性：`id`, `userId`, `exchangeId`, `apiKey`, `secretKey`, `status`, `createdAt`
  - 方法：`encryptApiKey()`, `decryptApiKey()`, `enable()`, `disable()`

**数据库表**：
```sql
CREATE TABLE exchange_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exchange_id BIGINT NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (exchange_id) REFERENCES exchange(id)
);
```

**缓存**：
- Redis Key: `account:{userId}:{exchangeId}:status`（TTL 1小时）。
- Redis Key: `account:{userId}:{exchangeId}:keys`（TTL 1小时）。

**实现细节**：
- AES-256 加密 API Key 和 Secret Key。
- Spring Security + JWT 验证用户身份。
- 账户状态检查优先通过 WebSocket 验证，降级使用 REST。

**示例代码**：
```java
@Service
public class ExchangeAccountService {
    @Autowired
    private ExchangeAccountRepository repository;
    @Autowired
    private EncryptionUtil encryptionUtil;
    @Autowired
    private ExchangeAdapter exchangeAdapter;

    public ExchangeAccount createAccount(Long userId, Long exchangeId, String apiKey, String secretKey) {
        String encryptedApiKey = encryptionUtil.encrypt(apiKey);
        String encryptedSecretKey = encryptionUtil.encrypt(secretKey);
        ExchangeAccount account = new ExchangeAccount(userId, exchangeId, encryptedApiKey, encryptedSecretKey);
        exchangeAdapter.validateApiKey(apiKey, secretKey); // 优先 WebSocket 验证
        return repository.save(account);
    }
}
```

### 2.3 Socket 统一管理
**功能**：
- 统一管理 WebSocket 连接，订阅行情、订单、仓位、资金数据。
- 支持自动重连、心跳机制、断线检测。
- 检测连接状态，触发 REST 降级。

**技术选型**：
- Netty 4.1 封装 WebSocket 客户端。
- 每个交易所一个连接池，支持多账户多主题订阅。

**领域模型**：
- **WebSocketManager**：
  - 方法：`connect()`, `subscribe()`, `unsubscribe()`, `handleMessage()`, `reconnect()`, `isConnected()`, `fallbackToRest()`
- **Subscription（值对象）**：存储订阅信息（主题、账户 ID、交易所 ID）。

**实现细节**：
- **Netty 配置**：
  - 使用 `NioEventLoopGroup` 处理 I/O。
  - 配置心跳（Ping 每 30 秒）。
  - 使用 `WebSocketClientHandshaker` 处理握手。
- **订阅逻辑**：
  - 主题格式：`{exchange}:{symbol}:{type}`（如 `binance:btcusdt@depth`）。
  - 消息解析为 `MarketEvent`、`OrderEvent`、`PositionEvent`、`BalanceEvent`。
- **降级机制**：
  - Redis 记录连接状态（Key: `ws:{exchangeId}:status`，值：`CONNECTED`/`DISCONNECTED`）。
  - 心跳失败或超时（60 秒无消息）标记为 `DISCONNECTED`，触发 REST 降级。
  - Spring Scheduler 每 10 秒检查状态，尝试重连。
- **消息分发**：
  - 使用 Spring `ApplicationEventPublisher` 分发事件到模块。
- Redis 存储订阅状态（Key: `ws:{exchangeId}:{accountId}:subscriptions`）。

**流程图**：
```mermaid
sequenceDiagram
    participant Client
    participant WebSocketManager
    participant Exchange
    participant Redis
    participant Service
    participant REST

    Client->>WebSocketManager: 订阅请求 (symbol, type)
    WebSocketManager->>Redis: 存储订阅信息
    WebSocketManager->>Exchange: 建立 WebSocket 连接
    Exchange-->>WebSocketManager: 推送消息
    WebSocketManager->>Service: 发布事件
    Service->>Redis: 更新缓存
    Service->>MySQL: 更新数据库
    Note over WebSocketManager: 检测断线
    WebSocketManager->>Redis: 标记 DISCONNECTED
    WebSocketManager->>REST: 降级请求数据
    REST-->>Service: 返回数据
```

**示例代码**：
```java
@Component
public class WebSocketManager {
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketManager(RedisTemplate<String, String> redisTemplate, ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    public void connect(String exchangeId, String wsUrl) throws URISyntaxException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new WebSocketClientInitializer(this::handleMessage));
        bootstrap.connect(new URI(wsUrl).getHost(), 443).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                connectionStatus.put(exchangeId, true);
                redisTemplate.opsForValue().set("ws:" + exchangeId + ":status", "CONNECTED");
            }
        });
    }

    public boolean isConnected(String exchangeId) {
        return connectionStatus.getOrDefault(exchangeId, false);
    }

    public void subscribe(String exchangeId, String accountId, String topic) {
        redisTemplate.opsForSet().add("ws:" + exchangeId + ":" + accountId + ":subscriptions", topic);
        if (isConnected(exchangeId)) {
            // 发送订阅请求
        } else {
            fallbackToRest(exchangeId, topic);
        }
    }

    private void handleMessage(WebSocketFrame frame) {
        String message = frame.text();
        MarketEvent event = parseMessage(message);
        eventPublisher.publishEvent(event);
    }

    private void fallbackToRest(String exchangeId, String topic) {
        log.warn("WebSocket disconnected for {}, falling back to REST", exchangeId);
        redisTemplate.opsForValue().set("ws:" + exchangeId + ":status", "DISCONNECTED");
        // 触发 REST 轮询（通过 ApplicationEvent 或直接调用服务）
        eventPublisher.publishEvent(new FallbackEvent(exchangeId, topic));
    }

    @Scheduled(fixedRate = 10000) // 每 10 秒检查
    public void checkConnections() {
        connectionStatus.forEach((exchangeId, status) -> {
            if (!status) {
                log.info("Attempting to reconnect to {}", exchangeId);
                // 尝试重连
            }
        });
    }
}
```

### 2.4 资金管理
**功能**：
- 跟踪账户余额、可用保证金、冻结资金。
- 优先通过 WebSocket 更新，降级使用 REST。

**领域模型**：
- **AccountBalance（聚合根）**：
  - 属性：`id`, `accountId`, `currency`, `totalBalance`, `availableBalance`, `frozenBalance`, `updatedAt`
  - 方法：`updateBalance()`, `freeze()`, `unfreeze()`

**数据库表**：
```sql
CREATE TABLE account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    total_balance DECIMAL(18,8) DEFAULT 0,
    available_balance DECIMAL(18,8) DEFAULT 0,
    frozen_balance DECIMAL(18,8) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES exchange_account(id),
    UNIQUE (account_id, currency)
);
```

**缓存**：
- Redis Key: `balance:{accountId}:{currency}`（TTL 10 分钟）。

**实现细节**：
- **WebSocket 更新**：订阅资金变化（如 `binance:account@update`）。
- **REST 降级**：WebSocket 断连时，每 5 秒通过 REST API 获取余额（`GET /account/balance`）。
- **一致性**：Redis 分布式锁（Key: `lock:balance:{accountId}`）防止并发更新。
- **验证**：REST 数据用于校验 WebSocket 数据（每 5 分钟同步一次）。
- Spring 事务确保数据库更新。

**示例代码**：
```java
@Service
public class BalanceService {
    @Autowired
    private AccountBalanceRepository repository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private OkHttpClient okHttpClient;
    @Autowired
    private WebSocketManager webSocketManager;

    @Transactional
    public void updateBalance(Long accountId, String currency, BigDecimal total, BigDecimal available) {
        String lockKey = "lock:balance:" + accountId;
        try (RedisLock lock = new RedisLock(redisTemplate, lockKey)) {
            if (lock.acquire()) {
                AccountBalance balance = repository.findByAccountIdAndCurrency(accountId, currency)
                        .orElse(new AccountBalance(accountId, currency));
                balance.update(total, available);
                repository.save(balance);
                redisTemplate.opsForValue().set("balance:" + accountId + ":" + currency, balance.toJson(), 10, TimeUnit.MINUTES);
            }
        }
    }

    @EventListener
    public void handleBalanceEvent(BalanceEvent event) {
        updateBalance(event.getAccountId(), event.getCurrency(), event.getTotal(), event.getAvailable());
    }

    @EventListener
    public void handleFallbackEvent(FallbackEvent event) {
        if (event.getTopic().contains("account@update")) {
            fetchBalanceViaRest(event.getExchangeId());
        }
    }

    private void fetchBalanceViaRest(String exchangeId) {
        ExchangeAccount account = exchangeAccountRepository.findByExchangeId(exchangeId).get(0);
        Request request = new Request.Builder()
                .url(account.getExchange().getRestApiUrl() + "/account/balance")
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            BalanceData balanceData = parseBalanceData(response.body().string());
            updateBalance(account.getId(), balanceData.getCurrency(), balanceData.getTotal(), balanceData.getAvailable());
        } catch (IOException e) {
            log.error("REST balance fetch failed for exchange: {}", exchangeId, e);
        }
    }
}
```

### 2.5 仓位管理
**功能**：
- 管理持仓（多仓/空仓、杠杆、数量、开仓价格、未实现盈亏）。
- 优先通过 WebSocket 更新，降级使用 REST。

**领域模型**：
- **Position（聚合根）**：
  - 属性：`id`, `accountId`, `symbol`, `side`, `quantity`, `leverage`, `entryPrice`, `unrealizedPnl`, `updatedAt`
  - 方法：`updatePosition()`, `closePosition()`

**数据库表**：
```sql
CREATE TABLE position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side ENUM('LONG', 'SHORT') NOT NULL,
    quantity DECIMAL(18,8) DEFAULT 0,
    leverage INT DEFAULT 1,
    entry_price DECIMAL(18,8) DEFAULT 0,
    unrealized_pnl DECIMAL(18,8) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES exchange_account(id),
    UNIQUE (account_id, symbol, side)
);
```

**缓存**：
- Redis Key: `position:{accountId}:{symbol}:{side}`（TTL 10 分钟）。

**实现细节**：
- **WebSocket 更新**：订阅仓位变化（如 `binance:position@update`）。
- **REST 降级**：WebSocket 断连时，每 5 秒通过 REST API 获取仓位（`GET /position`）。
- **一致性**：Redis 分布式锁（Key: `lock:position:{accountId}:{symbol}`）。
- **验证**：REST 数据校验 WebSocket 数据（每 5 分钟）。

**示例代码**：
```java
@Service
public class PositionService {
    @Autowired
    private PositionRepository repository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private OkHttpClient okHttpClient;

    @Transactional
    public void updatePosition(Long accountId, String symbol, String side, BigDecimal quantity, BigDecimal entryPrice) {
        String lockKey = "lock:position:" + accountId + ":" + symbol;
        try (RedisLock lock = new RedisLock(redisTemplate, lockKey)) {
            if (lock.acquire()) {
                Position position = repository.findByAccountIdAndSymbolAndSide(accountId, symbol, side)
                        .orElse(new Position(accountId, symbol, side));
                position.update(quantity, entryPrice);
                repository.save(position);
                redisTemplate.opsForValue().set("position:" + accountId + ":" + symbol + ":" + side, position.toJson(), 10, TimeUnit.MINUTES);
            }
        }
    }

    @EventListener
    public void handlePositionEvent(PositionEvent event) {
        updatePosition(event.getAccountId(), event.getSymbol(), event.getSide(), event.getQuantity(), event.getEntryPrice());
    }

    @EventListener
    public void handleFallbackEvent(FallbackEvent event) {
        if (event.getTopic().contains("position@update")) {
            fetchPositionViaRest(event.getExchangeId());
        }
    }

    private void fetchPositionViaRest(String exchangeId) {
        // 类似 BalanceService 的 REST 实现
    }
}
```

### 2.6 订单管理
**功能**：
- 创建、取消、查询订单。
- 优先通过 WebSocket 更新订单状态，降级使用 REST。

**领域模型**：
- **Order（聚合根）**：
  - 属性：`id`, `accountId`, `symbol`, `side`, `type`, `quantity`, `price`, `status`, `filledQuantity`, `createdAt`, `updatedAt`
  - 方法：`placeOrder()`, `cancelOrder()`, `updateStatus()`

**数据库表**：
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side ENUM('BUY', 'SELL') NOT NULL,
    type ENUM('LIMIT', 'MARKET') NOT NULL,
    quantity DECIMAL(18,8) NOT NULL,
    price DECIMAL(18,8),
    status ENUM('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELED') NOT NULL,
    filled_quantity DECIMAL(18,8) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES exchange_account(id)
);
```

**缓存**：
- Redis Key: `order:{accountId}:{orderId}`（TTL 1 小时）。

**实现细节**：
- **WebSocket 更新**：订阅订单变化（如 `binance:order@update`）。
- **REST 降级**：WebSocket 断连时，每 5 秒查询订单状态（`GET /order`）。
- **下单/撤单**：优先通过 REST API 执行（WebSocket 通常不支持下单）。
- **一致性**：Redis 分布式锁（Key: `lock:order:{accountId}:{orderId}`）。

**示例代码**：
```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository repository;
    @Autowired
    private OkHttpClient okHttpClient;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Order placeOrder(Long accountId, String symbol, String side, String type, BigDecimal quantity, BigDecimal price) {
        Order order = new Order(accountId, symbol, side, type, quantity, price);
        Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/order")
                .post(RequestBody.create(order.toJson(), MediaType.get("application/json")))
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            order.setStatus("NEW");
            repository.save(order);
            redisTemplate.opsForValue().set("order:" + accountId + ":" + order.getId(), order.toJson(), 1, TimeUnit.HOURS);
            return order;
        } catch (IOException e) {
            throw new RuntimeException("Failed to place order", e);
        }
    }

    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        String lockKey = "lock:order:" + event.getAccountId() + ":" + event.getOrderId();
        try (RedisLock lock = new RedisLock(redisTemplate, lockKey)) {
            if (lock.acquire()) {
                Order order = repository.findById(event.getOrderId()).orElseThrow();
                order.updateStatus(event.getStatus(), event.getFilledQuantity());
                repository.save(order);
                redisTemplate.opsForValue().set("order:" + event.getAccountId() + ":" + event.getOrderId(), order.toJson(), 1, TimeUnit.HOURS);
            }
        }
    }

    @EventListener
    public void handleFallbackEvent(FallbackEvent event) {
        if (event.getTopic().contains("order@update")) {
            fetchOrderViaRest(event.getExchangeId());
        }
    }

    private void fetchOrderViaRest(String exchangeId) {
        // 类似 REST 实现
    }
}
```

### 2.7 策略管理
**功能**：
- 支持多种策略（如均线、网格）。
- 动态加载策略，基于行情触发交易。
- 优先使用 WebSocket 获取行情，降级使用 REST。

**领域模型**：
- **Strategy（聚合根）**：
  - 属性：`id`, `name`, `accountId`, `symbol`, `parameters`, `status`, `updatedAt`
  - 方法：`start()`, `stop()`, `execute()`

**数据库表**：
```sql
CREATE TABLE strategy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    parameters JSON,
    status ENUM('RUNNING', 'STOPPED') DEFAULT 'STOPPED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES exchange_account(id)
);
```

**缓存**：
- Redis Key: `strategy:{strategyId}:status`（TTL 无）。
- Redis Key: `strategy:{strategyId}:parameters`（TTL 1 小时）。

**实现细节**：
- **WebSocket 行情**：订阅行情数据（如 `binance:btcusdt@tick`）。
- **REST 降级**：WebSocket 断连时，每 5 秒通过 REST 获取行情（`GET /ticker`）。
- 策略通过 `ApplicationEvent` 接收行情，触发订单。

**示例代码**：
```java
public interface TradingStrategy {
    void execute(MarketData data, StrategyContext context);
}

@Component
public class MovingAverageStrategy implements TradingStrategy {
    @Autowired
    private OrderService orderService;

    @Override
    public void execute(MarketData data, StrategyContext context) {
        int shortPeriod = context.getParameters().getInt("shortPeriod");
        int longPeriod = context.getParameters().getInt("longPeriod");
        double shortMA = calculateMA(data.getPrices(), shortPeriod);
        double longMA = calculateMA(data.getPrices(), longPeriod);
        if (shortMA > longMA) {
            orderService.placeOrder(context.getAccountId(), data.getSymbol(), "BUY", "MARKET", BigDecimal.ONE, data.getLastPrice());
        } else if (shortMA < longMA) {
            orderService.placeOrder(context.getAccountId(), data.getSymbol(), "SELL", "MARKET", BigDecimal.ONE, data.getLastPrice());
        }
    }

    private double calculateMA(List<Double> prices, int period) {
        return prices.stream().limit(period).mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}

@Service
public class StrategyService {
    @Autowired
    private StrategyRepository repository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ExchangeAdapter exchangeAdapter;

    @EventListener
    public void handleMarketDataEvent(MarketDataEvent event) {
        List<Strategy> strategies = repository.findBySymbolAndStatus(event.getSymbol(), "RUNNING");
        strategies.forEach(strategy -> {
            TradingStrategy tradingStrategy = loadStrategy(strategy.getName());
            tradingStrategy.execute(event.getMarketData(), new StrategyContext(strategy));
        });
    }

    @EventListener
    public void handleFallbackEvent(FallbackEvent event) {
        if (event.getTopic().contains("@ticker")) {
            exchangeAdapter.getMarketDataViaRest(event.getTopic().split(":")[1])
                .thenAccept(data -> eventPublisher.publishEvent(new MarketDataEvent(data)));
        }
    }
}
```

## 3. 数据更新机制

### 3.1 WebSocket 数据更新
**流程**：
1. 系统启动，订阅行情、订单、仓位、资金数据。
2. Netty 接收消息，解析为事件（`MarketEvent` 等）。
3. 通过 `ApplicationEventPublisher` 分发到模块。
4. 模块更新 Redis 和 MySQL。

**一致性**：
- Redis 分布式锁防止并发更新。
- Spring 事务确保数据库原子性。

### 3.2 REST 降级更新
**触发条件**：
- WebSocket 断连（`ws:{exchangeId}:status` 为 `DISCONNECTED`）。
- WebSocket 数据异常（如 60 秒无更新）。
- 定时验证（每 5 分钟通过 REST 同步）。

**流程**：
1. WebSocketManager 检测断线，发布 `FallbackEvent`。
2. 模块通过 REST API 获取数据（OkHttp 请求）。
3. 更新 Redis 和 MySQL。
4. 记录降级日志，触发告警。

**流程图**：
```mermaid
sequenceDiagram
    participant WebSocketManager
    participant Redis
    participant Service
    participant REST
    participant MySQL

    WebSocketManager->>Redis: 检查连接状态
    Redis-->>WebSocketManager: DISCONNECTED
    WebSocketManager->>Service: 发布 FallbackEvent
    Service->>REST: 请求数据
    REST-->>Service: 数据
    Service->>Redis: 更新缓存
    Service->>MySQL: 更新数据库
```

### 3.3 定时同步
- **目的**：校验 WebSocket 数据，确保一致性。
- **频率**：每 5 分钟通过 REST 同步资金、仓位、订单。
- **实现**：Spring Scheduler 调用 REST 任务。

**示例代码**：
```java
@Component
public class DataSyncTask {
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private OrderService orderService;

    @Scheduled(fixedRate = 300000) // 每 5 分钟
    public void syncData() {
        exchangeAccountRepository.findByStatus("ACTIVE").forEach(account -> {
            balanceService.fetchBalanceViaRest(account.getExchangeId());
            positionService.fetchPositionViaRest(account.getExchangeId());
            orderService.fetchOrderViaRest(account.getExchangeId());
        });
    }
}
```

## 4. 非功能性设计

### 4.1 安全性
- AES-256 加密 API Key。
- Spring Security + JWT 认证。
- TLS 1.2 保护 REST 和 WebSocket。
- 参数化查询防 SQL 注入。

### 4.2 高性能
- Redis 缓存热点数据。
- Netty 异步处理 WebSocket。
- OkHttp 连接池优化 REST 请求。
- 数据库索引优化。

### 4.3 可扩展性
- DDD 模块化设计。
- 适配器模式支持新交易所。
- 预留 Kafka 接口，方便扩展为微服务。

### 4.4 监控与日志
- Prometheus + Grafana 监控连接状态、降级次数。
- Logback 记录结构化日志。
- Prometheus Alertmanager 告警。

## 5. 数据库表结构
（与前述文档一致，略）

## 6. API 示例
（与前述文档一致，略）

## 7. 开发与部署

### 7.1 开发计划
- **阶段 1（4 周）**：搭建 DDD 框架，交易所和账户管理。
- **阶段 2（6 周）**：WebSocket 管理（含降级逻辑）、资金、仓位、订单模块。
- **阶段 3（4 周）**：策略模块（2 种策略），性能优化。
- **阶段 4（2 周）**：部署，配置监控，编写文档。

### 7.2 部署架构
- Docker 容器化单体应用。
- MySQL 和 Redis 单实例。
- Prometheus + Grafana 监控。

### 7.3 后续扩展
- 引入 Kafka 拆分为微服务。
- Kubernetes 编排。
- 添加回测模块。

## 8. 风险与挑战
1. **WebSocket 断连**：
   - **措施**：自动重连，REST 降级。
2. **数据一致性**：
   - **措施**：REST 定时同步，Redis 锁。
3. **交易所 API 限频**：
   - **措施**：OkHttp 限流，Redis 记录请求频率。
4. **单体瓶颈**：
   - **措施**：模块化设计，预留分布式扩展。

## 9. 附录
（与前述文档一致，略）

---

本设计文档细化了“优先 WebSocket、降级 REST”的实现，新增了降级机制、流程图和代码示例。系统保持单体架构，模块化设计便于后续扩展为分布式。如需进一步细化（如特定交易所适配器、策略实现），请告知！