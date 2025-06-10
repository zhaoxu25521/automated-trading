package com.trade.strategy.impl;

import com.trade.cache.Order;
import com.trade.common.DataMessage;
import com.trade.common.ExchangeEnums.OrderSide;
import com.trade.strategy.AbstractStrategy;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 多空对冲网格策略
 */
@Slf4j
public class GridStrategy extends AbstractStrategy {
    @Getter
    private GridConfig config;

    @Override
    protected void onStart() {
        parseConfig();
        log.info("Grid strategy {} started with config: {}", strategy.getName(), config);
        // 初始化网格订单
        initGridOrders();
        
        // 启动对冲监控线程
        new Thread(() -> {
            while (running) {
                try {
                    executeHedgeOrders();
                    monitorHedgePosition();
                    Thread.sleep(1000); // 每秒检查一次
                } catch (InterruptedException e) {
                    log.error("Hedge monitor interrupted", e);
                }
            }
        }).start();
    }

    /**
     * 监控对冲仓位
     */
    private void monitorHedgePosition() {
        // 计算当前多空盈亏
//        BigDecimal longPnl = calculatePositionPnl(longPosition, true);
//        BigDecimal shortPnl = calculatePositionPnl(shortPosition, false);
//        BigDecimal totalPnl = longPnl.add(shortPnl);
//
//        // 如果总亏损超过阈值，触发风控
//        if (shouldCloseHedgePosition(totalPnl)) {
//            log.warn("Total PnL reached stop loss: {}", totalPnl);
//            closeAllHedgePositions();
//            return;
//        }
        
        // 检查多空比例失衡
        if (isPositionImbalanced()) {
            log.warn("Position imbalance detected: Long={}, Short={}", 
                    longPosition, shortPosition);
            rebalancePositions();
        }
        
//        log.debug("Current positions - Long: {} ({}), Short: {} ({}), Total PnL: {}",
//                longPosition, longPnl, shortPosition, shortPnl, totalPnl);
    }

    /**
     * 检查多空仓位是否失衡
     */
    private boolean isPositionImbalanced() {
        if (longPosition.compareTo(BigDecimal.ZERO) == 0 || 
            shortPosition.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        
        BigDecimal ratio = longPosition.divide(shortPosition, MC);
        BigDecimal targetRatio = config.getLongShortRatio();
        
        // 允许10%的偏差
        BigDecimal lowerBound = targetRatio.multiply(new BigDecimal("0.9"), MC);
        BigDecimal upperBound = targetRatio.multiply(new BigDecimal("1.1"), MC);
        
        return ratio.compareTo(lowerBound) < 0 || ratio.compareTo(upperBound) > 0;
    }

    /**
     * 重新平衡多空仓位
     */
    private void rebalancePositions() {
        BigDecimal netPosition = longPosition.subtract(shortPosition);
        if (netPosition.compareTo(BigDecimal.ZERO) > 0) {
            // 净多头 - 增加空单或减少多单
            executeRebalanceOrder(OrderSide.SELL, netPosition.abs());
        } else {
            // 净空头 - 增加多单或减少空单
            executeRebalanceOrder(OrderSide.BUY, netPosition.abs());
        }
    }

    /**
     * 执行再平衡订单
     */
    private void executeRebalanceOrder(OrderSide side, BigDecimal amount) {
        // 实际项目中应调用交易API
        log.info("Executing rebalance order: Side={}, Amount={}", side, amount);
    }

    /**
     * 平仓所有对冲头寸
     */
    private void closeAllHedgePositions() {
        log.info("Closing all hedge positions");
        // 实际项目中应调用交易API平仓
        longPosition = BigDecimal.ZERO;
        shortPosition = BigDecimal.ZERO;
        avgLongPrice = BigDecimal.ZERO;
        avgShortPrice = BigDecimal.ZERO;
    }

    @Override
    protected void onStop() {
        // 清理所有网格订单
        clearGridOrders();
        log.info("Grid strategy {} stopped", strategy.getName());
    }

    @Override
    protected void onParamsUpdate() {
        parseConfig();
        log.info("Grid strategy {} config updated: {}", strategy.getName(), config);
        // 根据新参数调整网格
        adjustGridOrders();
    }

    private void parseConfig() {
        // 解析JSON格式的策略参数
        // 示例参数结构:
        // {
        //   "symbol": "BTCUSDT",
        //   "gridSize": 10,
        //   "upperPrice": 50000,
        //   "lowerPrice": 40000,
        //   "orderAmount": 0.01,
        //   "spread": 0.001,
        //   "takeProfit": 0.05,
        //   "stopLoss": 0.1,
        //   "hedgeRatio": 1.0, // 对冲比例(1.0表示完全对冲)
        //   "maxHedgePosition": 10 // 最大对冲持仓量
        // }
        this.config = new GridConfig(strategy.getParams());
    }

    private List<Order> longOrders = new ArrayList<>();
    private List<Order> shortOrders = new ArrayList<>();
    private BigDecimal hedgePosition = BigDecimal.ZERO;

    // 测试用getter方法
    List<Order> getLongOrders() {
        return longOrders;
    }

    List<Order> getShortOrders() {
        return shortOrders;
    }

    BigDecimal getHedgePosition() {
        return hedgePosition;
    }

    // 使用BigDecimal进行精确计算
    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01");

    private BigDecimal calculateHedgePnl() {
        // 假设每个仓位盈利1%，使用固定精度计算
        return hedgePosition.multiply(ONE_PERCENT, MC);
    }

    private boolean shouldCloseHedgePosition(BigDecimal pnl) {
        BigDecimal stopLossAmount = config.getOrderAmount()
            .multiply(config.getStopLoss(), MC)
            .negate();
        return pnl.compareTo(stopLossAmount) < 0;
    }

    /**
     * 安全转换double到BigDecimal
     */
    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * 安全转换String到BigDecimal 
     */
    private static BigDecimal toDecimal(String value) {
        return new BigDecimal(value).setScale(8, RoundingMode.HALF_UP);
    }

    private void initGridOrders() {
        // 清空现有订单
        longOrders.clear();
        shortOrders.clear();
        
        // 计算网格步长(基于波动率或其他指标)
        BigDecimal priceStep = config.getBasePrice()
            .multiply(config.getBaseSpread(), MC);
        
        // 生成做多网格(向上)
        for (int i = 1; i <= config.getLongGridSize(); i++) {
            BigDecimal price = config.getBasePrice()
                .add(priceStep.multiply(toDecimal(i), MC));
            
            // 创建多头订单
//            Order longOrder = new Order(
//                config.getSymbol(),
//                OrderSide.BUY,
//                OrderType.LIMIT,
//                config.getOrderAmount(),
//                price.doubleValue()
//            );
//            longOrders.add(longOrder);
        }
        
        // 生成做空网格(向下)
        for (int i = 1; i <= config.getShortGridSize(); i++) {
            BigDecimal price = config.getBasePrice()
                .subtract(priceStep.multiply(toDecimal(i), MC));
            
            // 创建空头订单
//            Order shortOrder = new Order(
//                config.getSymbol(),
//                OrderSide.SELL,
//                OrderType.LIMIT,
//                config.getOrderAmount()
//                    .multiply(config.getLongShortRatio(), MC)
//                    .doubleValue(),
//                price.doubleValue()
//            );
//            shortOrders.add(shortOrder);
        }
        
        log.info("Initialized {} long and {} short grid orders with ratio {}", 
            longOrders.size(), shortOrders.size(), config.getLongShortRatio());
    }

    /**
     * 订单成交回调
     */
    @Override
    public void change(DataMessage dataMessage) {
//        // 统计成交订单数
//        filledOrderCount++;
//
//        // 检查是否需要调整价差
//        config.checkSpreadAdjustment(filledOrderCount);
        
        // 根据最新价差重新初始化网格
        initGridOrders();
    }

    private void clearGridOrders() {
        // 清理所有网格订单和对冲订单
        longOrders.clear();
        shortOrders.clear();
        hedgePosition = BigDecimal.ZERO;
        log.info("Cleared all grid and hedge orders");
    }

    private void adjustGridOrders() {
        // 根据新参数调整网格订单
        clearGridOrders();
        initGridOrders();
    }

    /**
     * 执行对冲订单
     */
    private BigDecimal longPosition = BigDecimal.ZERO;
    private BigDecimal shortPosition = BigDecimal.ZERO;
    private BigDecimal avgLongPrice = BigDecimal.ZERO;
    private BigDecimal avgShortPrice = BigDecimal.ZERO;

    private void executeHedgeOrders() {
        // 计算净仓位
        BigDecimal netPosition = longPosition.subtract(shortPosition);
        
        // 检查仓位限制
        if (netPosition.abs().compareTo(config.getMaxPosition()) >= 0) {
            log.warn("Position reached limit: {}", netPosition.toPlainString());
            return;
        }

        // 根据净仓位方向执行对冲
        if (netPosition.compareTo(BigDecimal.ZERO) > 0) {
            // 净多头 - 执行空单对冲
            for (Order shortOrder : shortOrders) {
                if (shouldExecuteHedge(shortOrder) && 
                    shortPosition.compareTo(
                        longPosition.multiply(config.getLongShortRatio(), MC)) < 0) {
                    executeOrder(shortOrder);
                    shortPosition = shortPosition.add(shortOrder.getAmount());
                    avgShortPrice = calculateAvgPrice(avgShortPrice, shortPosition, shortOrder);
                }
            }
        } else {
            // 净空头 - 执行多单对冲
            for (Order longOrder : longOrders) {
                if (shouldExecuteHedge(longOrder) && 
                    longPosition.compareTo(
                        shortPosition.multiply(config.getLongShortRatio(), MC)) < 0) {
                    executeOrder(longOrder);
                    longPosition = longPosition.add(longOrder.getAmount());
                    avgLongPrice = calculateAvgPrice(avgLongPrice, longPosition, longOrder);
                }
            }
        }
    }

    /**
     * 计算平均开仓价格
     */
    private BigDecimal calculateAvgPrice(BigDecimal currentAvg, BigDecimal position, Order order) {
        if (position.compareTo(BigDecimal.ZERO) == 0) {
            return order.getPrice();
        }
        BigDecimal newPosition = position.add(order.getAmount());
        return currentAvg.multiply(position, MC)
            .add(order.getPrice().multiply(order.getAmount(), MC))
            .divide(newPosition, MC);
    }

    /**
     * 检查是否应该执行对冲订单
     */
    private boolean shouldExecuteHedge(Order order) {
        // 实现对冲订单触发逻辑
        // 这里简化为总是执行
        return true;
    }

    /**
     * 执行订单
     */
    private void executeOrder(Order order) {
        // 实际项目中应调用交易API
        log.debug("Executing order: {}", order);
    }

    /**
     * 双向网格策略配置
     */
    @Data
    public class GridConfig {
        // 0 为接收到价格即触发 ,1 为则为价格触发
        private Integer triggerType;
        private BigDecimal tiggerPrice;
        private String symbol;
        // 做多网格格数
        private int longGridSize;
        // 做空网格格数
        private int shortGridSize;
        // 基准价格
        private BigDecimal basePrice;
        // 订单数量
        private BigDecimal orderAmount;
        // 止损
        private BigDecimal stopLoss;
        // 多空仓位比例
        private BigDecimal longShortRatio;
        // 最大持仓量
        private BigDecimal maxPosition;
        // 基础价差
        private BigDecimal baseSpread;
        // 价差乘数
        private BigDecimal spreadMultiplier;
        // 触发价差调整的订单数量
        private int spreadTriggerCount;
        // 当前价差倍数
        private int currentSpreadMultiplier = 1;

        public GridConfig(String jsonParams) {
            // 实际项目中应使用JSON解析库
            // 这里简化为硬编码示例
            this.symbol = "BTCUSDT";
            this.longGridSize = 10;
            this.shortGridSize = 10;
            this.basePrice = new BigDecimal("45000");
            this.orderAmount = new BigDecimal("0.01");
            this.stopLoss = new BigDecimal("0.1");
            this.longShortRatio = new BigDecimal("1.0");
            this.maxPosition = new BigDecimal("10.0");
            this.baseSpread = new BigDecimal("0.01");
            this.spreadMultiplier = new BigDecimal("2.0");
            this.spreadTriggerCount = 5;
        }

        /**
         * 获取当前价差(考虑动态调整)
         */
        public BigDecimal getCurrentSpread() {
            return baseSpread.multiply(
                spreadMultiplier.pow(currentSpreadMultiplier - 1));
        }

        /**
         * 检查是否需要调整价差
         */
        public void checkSpreadAdjustment(int filledOrderCount) {
            if (filledOrderCount >= spreadTriggerCount * currentSpreadMultiplier) {
                currentSpreadMultiplier++;
                log.info("Spread increased to {}x", currentSpreadMultiplier);
            }
        }
    }
}
