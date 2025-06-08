package com.trade.strategy.impl;

import com.trade.cache.OrderCache;
import com.trade.dto.Order;
import com.trade.dto.StrategyConfig;
import com.trade.strategy.TradeStrategy;
import com.trade.utils.OrderIdGenerator;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GridStrategy implements TradeStrategy {

    private final StrategyConfig config;
    private Status status;
    private ExecutorService executor;
    private String clientId;
    private final BigDecimal priceLower;
    private final BigDecimal priceUpper;
    private final BigDecimal gridSpacing;
    private final BigDecimal qtyPerGrid;
    private final BigDecimal maxPosition;
    private BigDecimal lastPrice = BigDecimal.ZERO;
//    private final OrderIdGenerator orderIdGenerator;

    public GridStrategy(StrategyConfig config, BigDecimal priceLower, BigDecimal priceUpper,
                        BigDecimal gridSpacing, BigDecimal qtyPerGrid, BigDecimal maxPosition) {
        this.config = config;
        this.status = Status.STOPPED;
        this.priceLower = priceLower;
        this.priceUpper = priceUpper;
        this.gridSpacing = gridSpacing;
        this.qtyPerGrid = qtyPerGrid;
        this.maxPosition = maxPosition;
        if (config.getAsync()) {
            this.executor = Executors.newSingleThreadExecutor();
        }
    }
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String name() {
        return "contract-hedging-grid";
    }

    @Override
    public String getKey() {
        return String.format("%s-%s-%s",config.getExchange(), name(), config.getSymbol());
    }

    @Override
    public StrategyConfig getConfig() {
        return config;
    }

    @Override
    public void start() {
        status = Status.RUNNING;
//        this.clientId = client.getActiveClientsCount() > 0 ? client.getClientChannels().keySet().iterator().next() : "client1";
//        client.subscribe(clientId, "{\"channel\":\"orders\",\"instId\":\"BTC-USDT-SWAP\"}");
//        client.subscribe(clientId, "{\"channel\":\"positions\",\"instId\":\"BTC-USDT-SWAP\"}");
        System.out.println("网格策略 " + getKey() + " 已启动，价格区间: [" + priceLower + ", " + priceUpper + "]");
        initializeGrid();
    }

    @Override
    public void stop() {
        status = Status.STOPPED;
        if (executor != null) {
            executor.shutdown();
        }
        System.out.println("网格策略 " + getKey() + " 已停止");
    }

    @Override
    public void priceChange(String symbol, BigDecimal price, Long ts) {
        if (status != Status.RUNNING) return;
//        Runnable task = () -> {
//            System.out.println("网格策略 [" + getId() + "] 处理消息: " + message);
//            if (message.contains("\"channel\":\"tickers\"")) {
//                BigDecimal price = extractPrice(message);
//                updateGrid(price);
//            } else if (message.contains("\"channel\":\"positions\"")) {
//                updatePosition(message);
//            }
//        };
//        if (config.isAsync() && executor != null) {
//            executor.submit(task);
//        } else {
//            task.run();
//        }
    }

    @Override
    public void handleTradeExecution(String orderId, String cliId, BigDecimal executedPrice, BigDecimal executedQty) {

    }

    private void initializeGrid() {
//        OrderCache orderCache = client.getStrategyManager().getOrderCache();
//        BigDecimal price = priceLower;
//        while (price.compareTo(priceUpper) <= 0) {
//            Order buyOpen = new Order(
//                    orderIdGenerator.generateOpenOrderId("buy", price), clientId, "BTC-USDT-SWAP",
//                    "buy", "open", null, price, qtyPerGrid, "new");
//            orderCache.compareAndCheckSupplement(buyOpen);
//            BigDecimal sellPrice = price.add(gridSpacing);
//            if (sellPrice.compareTo(priceUpper) <= 0) {
//                Order sellOpen = new Order(
//                        orderIdGenerator.generateOpenOrderId("sell", sellPrice), clientId, "BTC-USDT-SWAP",
//                        "sell", "open", null, sellPrice, qtyPerGrid, "new");
//                orderCache.compareAndCheckSupplement(sellOpen);
//            }
//            price = price.add(gridSpacing);
//        }
    }

    private void updateGrid(BigDecimal currentPrice) {
        if (lastPrice.compareTo(BigDecimal.ZERO) != 0 &&
                currentPrice.subtract(lastPrice).abs().compareTo(gridSpacing.divide(new BigDecimal("2"))) < 0) {
            return;
        }
        lastPrice = currentPrice.setScale(2, BigDecimal.ROUND_DOWN);
//        OrderCache orderCache = getOrderCache();
//        BigDecimal price = priceLower;
//        while (price.compareTo(priceUpper) <= 0) {
//            if (currentPrice.subtract(price).abs().compareTo(gridSpacing) < 0) {
//                Order buyOpen = new Order(
//                        orderIdGenerator.generateOpenOrderId("buy", price), clientId, "BTC-USDT-SWAP",
//                        "buy", "open", null, price, qtyPerGrid, "new");
//                orderCache.compareAndCheckSupplement(buyOpen);
//                BigDecimal sellPrice = price.add(gridSpacing);
//                if (sellPrice.compareTo(priceUpper) <= 0) {
//                    Order sellOpen = new Order(
//                            orderIdGenerator.generateOpenOrderId("sell", sellPrice), clientId, "BTC-USDT-SWAP",
//                            "sell", "open", null, sellPrice, qtyPerGrid, "new");
//                    orderCache.compareAndCheckSupplement(sellOpen);
//                }
//            }
//            price = price.add(gridSpacing);
//        }
    }
//    private OrderCache getOrderCache() {
//        return ((StrategyManager) NettySocketClient.getStrategyManager()).getOrderCache();
//    }
    private void updatePosition(String message) {
        BigDecimal qty = extractPositionQty(message);
        if (qty.abs().compareTo(maxPosition) > 0) {
            System.err.println("仓位超限: 当前=" + qty + ", 最大=" + maxPosition);
        }
    }

    private BigDecimal extractPrice(String message) {
        return new BigDecimal("50000.00"); // 模拟
    }

    private BigDecimal extractPositionQty(String message) {
        return BigDecimal.ZERO; // 模拟
    }
}
