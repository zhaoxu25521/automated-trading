package com.trade.strategy;

import java.math.BigDecimal;

public interface TradeStrategy {

    String name();

    void start();

    void stop();
    void priceChange(String symbol, BigDecimal price,Long ts);

    void handleTradeExecution(String orderId,String cliId, BigDecimal executedPrice, BigDecimal executedQty);
}
