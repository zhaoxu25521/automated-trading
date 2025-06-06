package com.trade;

import com.trade.dto.Order;
import com.trade.dto.StrategyConfig;
import com.trade.enums.OrderEnums;
import com.trade.exchange.ExchangeApi;
import com.trade.strategy.impl.ContractHedgingGridStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ContractHedgingGridStrategyTest {
    private MockExchangeApi mockExchangeApi;
    private StrategyConfig config;
    private ContractHedgingGridStrategy strategy;

    @BeforeEach
    void setUp() {
        // 初始化配置
        config = new StrategyConfig(
                new BigDecimal("1.5"), // hedgeRatio
                new BigDecimal("0.01"), // gridInterval
                "BTCUSD_PERP", // symbol
                3, // gridCount
                8, // priceScale
                8, // quantityScale
                1000, // monitorIntervalMs
                500, // retryDelayMs
                10, // leverage
                new BigDecimal("10.0"), // maxPosition
                new BigDecimal("1000.0"), // minMargin
                new BigDecimal("1.0"), // baseQuantity
                new BigDecimal("0.01"), // riskPerTrade
                new BigDecimal("5000.0"), // stopLoss
                new BigDecimal("10000.0"), // takeProfit
                14, // atrPeriod
                new BigDecimal("1.5") // atrMultiplier
        );

        // 初始化模拟交易所API
        mockExchangeApi = new MockExchangeApi();
        strategy = new ContractHedgingGridStrategy(mockExchangeApi, config);
    }

    @AfterEach
    void tearDown() {
        strategy.stop();
    }

    @Test
    void testInitializeGrids() {
        // 设置模拟价格和ATR
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setATR("BTCUSD_PERP", new BigDecimal("0.02"));

        // 初始化网格
        strategy.start();

        // 验证网格订单
        assertEquals(3, mockExchangeApi.getOrders("BTCUSD_PERP").entrySet().stream()
                        .filter(e -> e.getValue().getSide() == OrderEnums.OrderSide.BUY).count(),
                "Should place 3 buy grid orders");
        assertEquals(3, mockExchangeApi.getOrders("BTCUSD_PERP").entrySet().stream()
                        .filter(e -> e.getValue().getSide() == OrderEnums.OrderSide.SELL).count(),
                "Should place 3 sell grid orders");

        // 验证网格价格
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("BUY_49999.97000000"),
                "Should have buy order at lower grid");
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("SELL_50000.03000000"),
                "Should have sell order at upper grid");
    }

    @Test
    void testHandleTradeExecution() throws Exception {
        // 设置初始状态
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setAvailableMargin(new BigDecimal("10000.0"));

        strategy.start();

        // 模拟买单成交
        String orderId = "BUY_50000.0";
        strategy.handleTradeExecution(orderId, null, new BigDecimal("50000.0"), new BigDecimal("0.5"));

        // 等待补单
        Thread.sleep(100);

        // 验证补卖单
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("SELL_50000.03000000"),
                "Should place sell order after buy execution");

        // 验证仓位更新
        assertEquals(new BigDecimal("0.50000000"),
                strategy.getClass().getDeclaredField("longPosition").get(strategy),
                "Long position should be updated");
    }

    @Test
    void testDynamicGridInterval() throws Exception {
        // 设置不同ATR值
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setATR("BTCUSD_PERP", new BigDecimal("0.02"));

        strategy.start();

        // 验证动态网格间隔 (ATR * atrMultiplier = 0.02 * 1.5 = 0.03)
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("BUY_49999.97000000"),
                "Should adjust grid interval based on ATR");
    }

    @Test
    void testStopLoss() throws Exception {
        // 设置初始状态
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setAvailableMargin(new BigDecimal("10000.0"));

        strategy.start();

        // 模拟买单成交
        strategy.handleTradeExecution("BUY_50000.0",null, new BigDecimal("50000.0"), new BigDecimal("1.0"));

        // 模拟大幅亏损
        strategy.getClass().getDeclaredField("totalPnl").set(strategy, new BigDecimal("-6000.0"));
        strategy.getClass().getDeclaredField("longPosition").set(strategy, new BigDecimal("1.0"));

        // 触发止损
        strategy.handleTradeExecution("BUY_50000.0",null , new BigDecimal("50000.0"), new BigDecimal("0.0"));

        // 等待平仓
        Thread.sleep(100);

        // 验证平仓订单
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("SELL_50000.00000000"),
                "Should place closing order on stop loss");
        assertFalse(strategy.getClass().getDeclaredField("isRunning").getBoolean(strategy),
                "Strategy should stop on stop loss");
    }

    @Test
    void testLiquidationRisk() throws Exception {
        // 设置初始状态
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setAvailableMargin(new BigDecimal("10000.0"));
        mockExchangeApi.setLiquidationPrice("BTCUSD_PERP", new BigDecimal("50000.0"));

        strategy.start();

        // 模拟价格接近强平线
        strategy.getClass().getDeclaredField("longPosition").set(strategy, new BigDecimal("1.0"));

        // 触发强平检查
        strategy.getClass().getDeclaredMethod("monitorPriceChanges").invoke(strategy);

        // 等待平仓
        Thread.sleep(100);

        // 验证平仓订单
        assertTrue(mockExchangeApi.getOrders("BTCUSD_PERP").containsKey("SELL_50000.00000000"),
                "Should place closing order on liquidation risk");
        assertFalse(strategy.getClass().getDeclaredField("isRunning").getBoolean(strategy),
                "Strategy should stop on liquidation risk");
    }

    @Test
    void testOrderQuantityCalculation() throws Exception {
        // 设置初始状态
        mockExchangeApi.setMarketPrice("BTCUSD_PERP", new BigDecimal("50000.0"));
        mockExchangeApi.setAvailableMargin(new BigDecimal("10000.0"));

        // 计算订单量
        BigDecimal qty = (BigDecimal) strategy.getClass()
                .getDeclaredMethod("calculateOrderQuantity", BigDecimal.class)
                .invoke(strategy, new BigDecimal("50000.0"));

        // 验证订单量 (10000 * 0.01 / 50000 * 10 = 0.02, min with baseQuantity 1.0)
        assertEquals(new BigDecimal("0.02000000"), qty, "Order quantity should respect risk per trade");
    }

    // 模拟交易所API
    private static class MockExchangeApi implements ExchangeApi {
        private final Map<String, BigDecimal> marketPrices = new HashMap<>();
        private final Map<String, BigDecimal> atrValues = new HashMap<>();
        private BigDecimal availableMargin = BigDecimal.ZERO;
        private final Map<String, Map<String, Order>> orders = new HashMap<>();
        private final Map<String, BigDecimal> liquidationPrices = new HashMap<>();

        public void setMarketPrice(String symbol, BigDecimal price) {
            marketPrices.put(symbol, price);
        }

        public void setATR(String symbol, BigDecimal atr) {
            atrValues.put(symbol, atr);
        }

        public void setAvailableMargin(BigDecimal margin) {
            availableMargin = margin;
        }

        public void setLiquidationPrice(String symbol, BigDecimal price) {
            liquidationPrices.put(symbol, price);
        }

        public Map<String, Order> getOrders(String symbol) {
            return orders.computeIfAbsent(symbol, k -> new HashMap<>());
        }

        @Override
        public BigDecimal getMarketPrice(String symbol) {
            return marketPrices.getOrDefault(symbol, BigDecimal.ZERO);
        }

        @Override
        public String placeOrder(String symbol, OrderEnums.OrderSide side, BigDecimal price, BigDecimal quantity) {
            String orderId = side + "_" + price.toString();
            orders.computeIfAbsent(symbol, k -> new HashMap<>())
                    .put(orderId, new Order(orderId, symbol, side, price, quantity, 8));
            return orderId;
        }

        @Override
        public void cancelOrder(String orderId) {
            orders.values().forEach(symbolOrders -> symbolOrders.remove(orderId));
        }

        @Override
        public void setLeverage(String symbol, int leverage) { }

        @Override
        public BigDecimal getAvailableMargin() {
            return availableMargin;
        }

        @Override
        public BigDecimal getATR(String symbol, int period) {
            return atrValues.getOrDefault(symbol, new BigDecimal("0.01"));
        }

        @Override
        public BigDecimal getLiquidationPrice(String symbol, BigDecimal position) {
            return liquidationPrices.getOrDefault(symbol, BigDecimal.ZERO);
        }
    }
}