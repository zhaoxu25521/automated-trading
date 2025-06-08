package com.trade.strategy.impl;

import com.trade.dto.Order;
import com.trade.dto.StrategyConfig;
import com.trade.enums.OrderEnums.*;
import com.trade.exchange.ExchangeApi;
import com.trade.strategy.TradeStrategy;
import io.micrometer.observation.annotation.Observed;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContractHedgingGridStrategy implements TradeStrategy {
    private static final Logger LOGGER = Logger.getLogger(ContractHedgingGridStrategy.class.getName());

    // 订单和仓位状态管理
    private final ConcurrentHashMap<String, Order> activeOrders = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 交易所接口
    private final ExchangeApi exchangeApi;

    // 配置参数
    @Getter
    private final StrategyConfig config;

    // 网格价格和ATR缓存
    private final List<BigDecimal> buyGridPrices = new ArrayList<>();
    private final List<BigDecimal> sellGridPrices = new ArrayList<>();
    private BigDecimal atrValue = BigDecimal.ZERO;

    // 仓位和保证金
    private BigDecimal longPosition = BigDecimal.ZERO;
    private BigDecimal shortPosition = BigDecimal.ZERO;
    private BigDecimal availableMargin = BigDecimal.ZERO;
    private BigDecimal totalPnl = BigDecimal.ZERO;
    private Status status;
    public ContractHedgingGridStrategy(ExchangeApi exchangeApi, StrategyConfig config) {
        this.exchangeApi = exchangeApi;
        this.config = config;
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

    // 启动策略
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            LOGGER.info("Starting contract hedging grid strategy with config: " + config);
            try {
                // 设置杠杆
                exchangeApi.setLeverage(config.getSymbol(), config.getLeverage());
                // 初始化保证金和ATR
                updateMargin();
                updateATR();
                initializeGrids();
                status = Status.RUNNING;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start strategy: {0}", e.getMessage());
                stop();
            }
        }
    }

    // 停止策略
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            cancelAllOrders();
            status = Status.STOPPED;
            LOGGER.info("Contract hedging grid strategy stopped.");
        }
    }

    // 更新保证金
    private void updateMargin() {
        try {
            availableMargin = retryOperation(() -> exchangeApi.getAvailableMargin(), 3);
            if (availableMargin == null) {
                LOGGER.severe("Failed to fetch available margin");
                stop();
            } else {
                LOGGER.info("Available margin: " + availableMargin.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating margin: {0}", e.getMessage());
        }
    }

    // 更新ATR（平均真实波幅）
    private void updateATR() {
        try {
            BigDecimal atr = retryOperation(() -> exchangeApi.getATR(config.getSymbol(), config.getAtrPeriod()), 3);
            if (atr == null) {
                LOGGER.warning("Failed to fetch ATR, using default grid interval");
                atrValue = config.getGridInterval();
            } else {
                atrValue = atr.multiply(config.getAtrMultiplier())
                        .setScale(config.getPriceScale(), RoundingMode.HALF_UP);
                LOGGER.info("Updated ATR: " + atrValue.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating ATR: {0}", e.getMessage());
        }
    }

    // 初始化网格
    private void initializeGrids() {
        try {
            BigDecimal currentPrice = retryOperation(() -> exchangeApi.getMarketPrice(config.getSymbol()), 3);
            if (currentPrice == null) {
                LOGGER.severe("Failed to initialize grids: Invalid market price");
                return;
            }

            // 使用ATR动态调整网格间隔
            BigDecimal dynamicGridInterval = atrValue.max(config.getGridInterval());

            // 初始化买单网格（低于当前价格）
            buyGridPrices.clear();
            for (int i = -config.getGridCount(); i < 0; i++) {
                BigDecimal gridPrice = currentPrice.add(dynamicGridInterval.multiply(BigDecimal.valueOf(i)))
                        .setScale(config.getPriceScale(), RoundingMode.HALF_UP);
                buyGridPrices.add(gridPrice);
            }

            // 初始化卖单网格（高于当前价格）
            sellGridPrices.clear();
            for (int i = 1; i <= config.getGridCount(); i++) {
                BigDecimal gridPrice = currentPrice.add(dynamicGridInterval.multiply(BigDecimal.valueOf(i)))
                        .setScale(config.getPriceScale(), RoundingMode.HALF_UP);
                sellGridPrices.add(gridPrice);
            }

            // 放置初始网格订单
            placeGridOrders(currentPrice);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing grids: {0}", e.getMessage());
        }
    }

    // 放置网格订单
    private void placeGridOrders(BigDecimal currentPrice) {
        // 检查保证金和风险
        if (!checkMarginSufficient() || !checkRisk()) {
            LOGGER.severe("Insufficient margin or risk limit exceeded");
            return;
        }

        // 买单网格：低于当前价格挂买单（开多）
        for (BigDecimal gridPrice : buyGridPrices) {
            BigDecimal quantity = calculateOrderQuantity(gridPrice);
            if (checkPositionLimit(OrderSide.BUY, quantity)) {
                placeOrder(config.getSymbol(), OrderSide.BUY, gridPrice, quantity);
            }
        }

        // 卖单网格：高于当前价格挂卖单（开空）
        for (BigDecimal gridPrice : sellGridPrices) {
            BigDecimal quantity = calculateOrderQuantity(gridPrice)
                    .multiply(config.getHedgeRatio())
                    .setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
            if (checkPositionLimit(OrderSide.SELL, quantity)) {
                placeOrder(config.getSymbol(), OrderSide.SELL, gridPrice, quantity);
            }
        }
    }

    // 价格变化监控
    @Override
    public void priceChange(String symbol,BigDecimal currentPrice,Long ts) {
        if (!isRunning.get()){
            return;
        }

        try {
            updateMargin();
            updateATR();
            if (availableMargin.compareTo(config.getMinMargin()) < 0) {
                LOGGER.severe("Margin below minimum threshold: " + availableMargin.toString());
                stop();
                return;
            }

//            BigDecimal currentPrice = retryOperation(() -> exchangeApi.getMarketPrice(config.getSymbol()), 3);
//            if (currentPrice == null) {
//                LOGGER.warning("Failed to fetch market price after retries");
//                return;
//            }

            // 检查强平风险
            checkLiquidationRisk(currentPrice);

            // 检查是否需要更新网格
            if (checkGridUpdateNeeded(currentPrice)) {
                cancelAllOrders();
                initializeGrids();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in price monitoring: {0}", e.getMessage());
        }
    }

    // 成交处理接口
    public void handleTradeExecution(String orderId,String cliId, BigDecimal executedPrice, BigDecimal executedQty) {
        Order order = activeOrders.get(orderId);
        if (order == null) {
            LOGGER.warning("Unknown order executed: " + orderId);
            return;
        }

        try {
            // 更新订单状态和仓位
            order.updateExecutedQty(executedQty);
            updatePosition(order.getSide(), executedPrice, executedQty);

            if (order.isFullyFilled()) {
                activeOrders.remove(orderId);
                LOGGER.info(String.format("Order %s fully filled: %s@%s, Qty: %s",
                        orderId, order.getSymbol(), executedPrice.toString(), executedQty.toString()));

                // 检查止盈止损
                checkStopLossTakeProfit();

                // 补单逻辑
                if (order.getSide() == OrderSide.BUY) {
                    supplementSellOrder(order, executedPrice);
                } else if (order.getSide() == OrderSide.SELL) {
                    supplementBuyOrder(order, executedPrice);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling trade execution for order {0}: {1}",
                    new Object[]{orderId, e.getMessage()});
        }
    }

    // 补卖单（空头）
    private void supplementSellOrder(Order buyOrder, BigDecimal executedPrice) {
        try {
            BigDecimal sellPrice = executedPrice.multiply(BigDecimal.ONE.add(atrValue))
                    .setScale(config.getPriceScale(), RoundingMode.HALF_UP);
            BigDecimal sellQty = buyOrder.getQuantity().multiply(config.getHedgeRatio())
                    .setScale(config.getQuantityScale(), RoundingMode.HALF_UP);

            if (checkMarginSufficient() && checkPositionLimit(OrderSide.SELL, sellQty)) {
                placeOrder(config.getSymbol(), OrderSide.SELL, sellPrice, sellQty);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error supplementing sell order: {0}", e.getMessage());
        }
    }

    // 补买单（多头）
    private void supplementBuyOrder(Order sellOrder, BigDecimal executedPrice) {
        try {
            BigDecimal buyPrice = executedPrice.multiply(BigDecimal.ONE.subtract(atrValue))
                    .setScale(config.getPriceScale(), RoundingMode.HALF_UP);
            BigDecimal buyQty = sellOrder.getQuantity().divide(config.getHedgeRatio(),
                    config.getQuantityScale(), RoundingMode.HALF_UP);

            if (checkMarginSufficient() && checkPositionLimit(OrderSide.BUY, buyQty)) {
                placeOrder(config.getSymbol(), OrderSide.BUY, buyPrice, buyQty);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error supplementing buy order: {0}", e.getMessage());
        }
    }

    // 下单
    private void placeOrder(String symbol, OrderSide side, BigDecimal price, BigDecimal quantity) {
        try {
            String orderId = retryOperation(() -> exchangeApi.placeOrder(symbol, side, price, quantity), 3);
            if (orderId != null) {
//                activeOrders.put(orderId, new Order(orderId, symbol, side, price, quantity, config.getQuantityScale()));
                LOGGER.info(String.format("Placed order: %s %s@%s, Qty: %s",
                        side, symbol, price.toString(), quantity.toString()));
            } else {
                LOGGER.warning(String.format("Failed to place order: %s %s@%s, Qty: %s",
                        side, symbol, price.toString(), quantity.toString()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to place order {0} {1}@{2}: {3}",
                    new Object[]{side, symbol, price.toString(), e.getMessage()});
        }
    }

    // 更新仓位和盈亏
    private void updatePosition(OrderSide side, BigDecimal price, BigDecimal quantity) {
        BigDecimal pnl = BigDecimal.ZERO;
        if (side == OrderSide.BUY) {
            longPosition = longPosition.add(quantity).setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
        } else {
            shortPosition = shortPosition.add(quantity).setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
            pnl = price.multiply(quantity).negate(); // 卖单盈亏
        }
        totalPnl = totalPnl.add(pnl).setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
        LOGGER.info(String.format("Updated positions: Long=%s, Short=%s, PNL=%s",
                longPosition, shortPosition, totalPnl));
    }

    // 检查保证金是否足够
    private boolean checkMarginSufficient() {
        return availableMargin.compareTo(config.getMinMargin()) >= 0;
    }

    // 检查仓位限制
    private boolean checkPositionLimit(OrderSide side, BigDecimal quantity) {
        BigDecimal newLongPosition = longPosition;
        BigDecimal newShortPosition = shortPosition;

        if (side == OrderSide.BUY) {
            newLongPosition = newLongPosition.add(quantity);
        } else {
            newShortPosition = newShortPosition.add(quantity);
        }

        boolean withinLimit = newLongPosition.abs().compareTo(config.getMaxPosition()) <= 0 &&
                newShortPosition.abs().compareTo(config.getMaxPosition()) <= 0;
        if (!withinLimit) {
            LOGGER.warning(String.format("Position limit exceeded: Long=%s, Short=%s, Max=%s",
                    newLongPosition, newShortPosition, config.getMaxPosition()));
        }
        return withinLimit;
    }

    // 检查强平风险
    private void checkLiquidationRisk(BigDecimal currentPrice) {
        try {
            BigDecimal liquidationPrice = retryOperation(() ->
                    exchangeApi.getLiquidationPrice(config.getSymbol(), longPosition.subtract(shortPosition)), 3);

            if (liquidationPrice != null &&
                    (longPosition.compareTo(BigDecimal.ZERO) > 0 && currentPrice.compareTo(liquidationPrice) <= 0 ||
                            shortPosition.compareTo(BigDecimal.ZERO) > 0 && currentPrice.compareTo(liquidationPrice) >= 0)) {
                LOGGER.severe("Liquidation risk detected, stopping strategy");
                cancelAllOrders();
                if (longPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.SELL, currentPrice, longPosition.abs());
                }
                if (shortPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.BUY, currentPrice, shortPosition.abs());
                }
                stop();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking liquidation risk: {0}", e.getMessage());
        }
    }

    // 检查止盈止损
    private void checkStopLossTakeProfit() {
        if (totalPnl.compareTo(config.getTakeProfit()) >= 0) {
            LOGGER.info("Take profit triggered: PNL=" + totalPnl);
            cancelAllOrders();
            BigDecimal currentPrice = retryOperation(() -> exchangeApi.getMarketPrice(config.getSymbol()), 3);
            if (currentPrice != null) {
                if (longPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.SELL, currentPrice, longPosition.abs());
                }
                if (shortPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.BUY, currentPrice, shortPosition.abs());
                }
            }
            stop();
        } else if (totalPnl.compareTo(config.getStopLoss().negate()) <= 0) {
            LOGGER.severe("Stop loss triggered: PNL=" + totalPnl);
            cancelAllOrders();
            BigDecimal currentPrice = retryOperation(() -> exchangeApi.getMarketPrice(config.getSymbol()), 3);
            if (currentPrice != null) {
                if (longPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.SELL, currentPrice, longPosition.abs());
                }
                if (shortPosition.compareTo(BigDecimal.ZERO) > 0) {
                    placeClosingOrder(config.getSymbol(), OrderSide.BUY, currentPrice, shortPosition.abs());
                }
            }
            stop();
        }
    }

    // 平仓单
    private void placeClosingOrder(String symbol, OrderSide side, BigDecimal price, BigDecimal quantity) {
        if (price == null || quantity.compareTo(BigDecimal.ZERO) <= 0) return;
        placeOrder(symbol, side, price, quantity);
    }

    // 检查是否需要更新网格
    private boolean checkGridUpdateNeeded(BigDecimal currentPrice) {
        BigDecimal range = atrValue.multiply(BigDecimal.valueOf(config.getGridCount()));
        return currentPrice.compareTo(buyGridPrices.get(0).subtract(range)) < 0 ||
                currentPrice.compareTo(sellGridPrices.get(sellGridPrices.size() - 1).add(range)) > 0;
    }

    // 计算订单数量（资金管理）
    private BigDecimal calculateOrderQuantity(BigDecimal price) {
        BigDecimal riskAmount = availableMargin.multiply(config.getRiskPerTrade())
                .setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
        BigDecimal qty = riskAmount.divide(price, config.getQuantityScale(), RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(config.getLeverage()));
        return qty.min(config.getBaseQuantity()).setScale(config.getQuantityScale(), RoundingMode.HALF_UP);
    }

    // 检查总风险
    private boolean checkRisk() {
        return totalPnl.compareTo(config.getStopLoss().negate()) > 0;
    }

    // 撤所有订单
    private void cancelAllOrders() {
        activeOrders.keySet().forEach(orderId -> {
            try {
                exchangeApi.cancelOrder(orderId);
                LOGGER.info("Cancelled order: " + orderId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to cancel order {0}: {1}",
                        new Object[]{orderId, e.getMessage()});
            }
        });
        activeOrders.clear();
    }

    // 重试机制
    private <T> T retryOperation(ExchangeOperation<T> operation, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return operation.execute();
            } catch (Exception e) {
                attempts++;
                LOGGER.log(Level.WARNING, "Operation failed, attempt {0}/{1}: {2}",
                        new Object[]{attempts, maxRetries, e.getMessage()});
                if (attempts == maxRetries) {
                    return null;
                }
                try {
                    Thread.sleep(config.getRetryDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    // 交易所操作接口
    @FunctionalInterface
    interface ExchangeOperation<T> {
        T execute() throws Exception;
    }
}
