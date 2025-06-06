package com.trade.strategy;

import com.trade.dto.StrategyConfig;

import java.math.BigDecimal;

public interface TradeStrategy {
    enum Status {
        RUNNING, STOPPED, ERROR
    }

    Status getStatus();
    void setStatus(Status status);
    String name();

    String getKey();

    void start();

    StrategyConfig getConfig();

    void stop();
    void priceChange(String symbol, BigDecimal price,Long ts);

    void handleTradeExecution(String orderId,String cliId, BigDecimal executedPrice, BigDecimal executedQty);
}
