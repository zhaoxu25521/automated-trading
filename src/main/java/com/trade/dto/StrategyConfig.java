package com.trade.dto;


import com.trade.enums.ExchangeEnums;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

/**
 * 构造函数，初始化策略配置。
 *
 * @param strategyId      策略唯一标识符，用于区分不同策略。
 *                        类型：String
 *                        示例："strategy1"
 *                        必填：是
 * @param subscribedTopics 策略订阅的 WebSocket 主题列表。
 *                         类型：Set<String>
 *                         示例：{"{\"channel\":\"tickers\",\"instId\":\"BTC-USDT\"}"}
 *                         默认：空集合
 * @param isAsync         是否异步处理消息。
 *                        类型：boolean
 *                        用途：true 表示消息处理在单独线程中执行，适合高吞吐量场景；false 表示同步处理，适合简单逻辑。
 *                        默认：false
 * @param priority        策略优先级，影响消息分发的顺序（较低值优先）。
 *                        类型：int
 *                        范围：0（最高）到 100（最低）
 *                        默认：50
 * @param autoRestart     是否在客户端重连后自动重启策略。
 *                        类型：boolean
 *                        用途：true 表示在 WebSocket 断线重连后自动恢复策略运行；false 需手动重启。
 *                        默认：true
 */
@Data
public class StrategyConfig {
    private final String exchange ;

    private Set<String> subscribedTopics;
    private final BigDecimal hedgeRatio;
    private final BigDecimal gridInterval;
    private final String symbol;
    private final int gridCount;
    private final int priceScale;
    private final int quantityScale;
    private final long monitorIntervalMs;
    private final long retryDelayMs;
    private final int leverage;
    private final BigDecimal maxPosition;
    private final BigDecimal minMargin;
    private final BigDecimal baseQuantity;
    private final BigDecimal riskPerTrade;
    private final BigDecimal stopLoss;
    private final BigDecimal takeProfit;
    private final int atrPeriod;
    private final BigDecimal atrMultiplier;
    private int priority;

    private Boolean isAutoRestart;
    private Boolean autoRestart;

    public StrategyConfig(String exchange,BigDecimal hedgeRatio, BigDecimal gridInterval, String symbol,
                          int gridCount, int priceScale, int quantityScale,
                          long monitorIntervalMs, long retryDelayMs, int leverage,
                          BigDecimal maxPosition, BigDecimal minMargin, BigDecimal baseQuantity,
                          BigDecimal riskPerTrade, BigDecimal stopLoss, BigDecimal takeProfit,
                          int atrPeriod, BigDecimal atrMultiplier,Set<String> subscribedTopics) {
        this.exchange = exchange;
        this.hedgeRatio = hedgeRatio.setScale(quantityScale, RoundingMode.HALF_UP);
        this.gridInterval = gridInterval.setScale(priceScale, RoundingMode.HALF_UP);
        this.symbol = symbol;
        this.gridCount = gridCount;
        this.priceScale = priceScale;
        this.quantityScale = quantityScale;
        this.monitorIntervalMs = monitorIntervalMs;
        this.retryDelayMs = retryDelayMs;
        this.leverage = leverage;
        this.maxPosition = maxPosition.setScale(quantityScale, RoundingMode.HALF_UP);
        this.minMargin = minMargin.setScale(quantityScale, RoundingMode.HALF_UP);
        this.baseQuantity = baseQuantity.setScale(quantityScale, RoundingMode.HALF_UP);
        this.riskPerTrade = riskPerTrade.setScale(quantityScale, RoundingMode.HALF_UP);
        this.stopLoss = stopLoss.setScale(quantityScale, RoundingMode.HALF_UP);
        this.takeProfit = takeProfit.setScale(quantityScale, RoundingMode.HALF_UP);
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier.setScale(quantityScale, RoundingMode.HALF_UP);

        this.subscribedTopics = subscribedTopics != null ? new HashSet<>(subscribedTopics) : new HashSet<>();
        this.priority = Math.max(0, Math.min(100, priority));
        this.autoRestart = autoRestart;
    }

    public BigDecimal getHedgeRatio() { return hedgeRatio; }
    public BigDecimal getGridInterval() { return gridInterval; }
    public String getSymbol() { return symbol; }
    public int getGridCount() { return gridCount; }
    public int getPriceScale() { return priceScale; }
    public int getQuantityScale() { return quantityScale; }
    public long getMonitorIntervalMs() { return monitorIntervalMs; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public int getLeverage() { return leverage; }
    public BigDecimal getMaxPosition() { return maxPosition; }
    public BigDecimal getMinMargin() { return minMargin; }
    public BigDecimal getBaseQuantity() { return baseQuantity; }
    public BigDecimal getRiskPerTrade() { return riskPerTrade; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public BigDecimal getTakeProfit() { return takeProfit; }
    public int getAtrPeriod() { return atrPeriod; }
    public BigDecimal getAtrMultiplier() { return atrMultiplier; }

    @Override
    public String toString() {
        return String.format("StrategyConfig{hedgeRatio=%s, gridInterval=%s, symbol=%s, " +
                        "gridCount=%d, priceScale=%d, quantityScale=%d, monitorIntervalMs=%d, retryDelayMs=%d, " +
                        "leverage=%d, maxPosition=%s, minMargin=%s, baseQuantity=%s, riskPerTrade=%s, " +
                        "stopLoss=%s, takeProfit=%s, atrPeriod=%d, atrMultiplier=%s}",
                hedgeRatio, gridInterval, symbol, gridCount, priceScale, quantityScale,
                monitorIntervalMs, retryDelayMs, leverage, maxPosition, minMargin, baseQuantity,
                riskPerTrade, stopLoss, takeProfit, atrPeriod, atrMultiplier);
    }
}
