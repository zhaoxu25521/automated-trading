package com.trade.exchange;

import com.trade.enums.ExchangeEnums;
import com.trade.enums.OrderEnums;

import java.math.BigDecimal;

public interface ExchangeApi {
    ExchangeEnums name();
    BigDecimal getMarketPrice(String symbol) throws Exception;
    String placeOrder(String symbol, OrderEnums.OrderSide side, BigDecimal price, BigDecimal quantity) throws Exception;
    void cancelOrder(String orderId) throws Exception;
    void setLeverage(String symbol, int leverage) throws Exception;
    BigDecimal getAvailableMargin() throws Exception;
    BigDecimal getATR(String symbol, int period) throws Exception;
    BigDecimal getLiquidationPrice(String symbol, BigDecimal position) throws Exception;
}
