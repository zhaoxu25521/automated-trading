package com.trade.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class StrategyConfig {
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

    public StrategyConfig(BigDecimal hedgeRatio, BigDecimal gridInterval, String symbol,
                          int gridCount, int priceScale, int quantityScale,
                          long monitorIntervalMs, long retryDelayMs, int leverage,
                          BigDecimal maxPosition, BigDecimal minMargin, BigDecimal baseQuantity,
                          BigDecimal riskPerTrade, BigDecimal stopLoss, BigDecimal takeProfit,
                          int atrPeriod, BigDecimal atrMultiplier) {
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
