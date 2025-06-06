package com.trade.exchange;

import com.trade.enums.OrderEnums;

import java.math.BigDecimal;

public class BinanceFuturesApi implements ExchangeApi {
    public BigDecimal getMarketPrice(String symbol) throws Exception { return new BigDecimal("50000.12345678"); }
    public String placeOrder(String symbol, OrderEnums.OrderSide side, BigDecimal price, BigDecimal quantity) throws Exception {
        return "order_" + System.currentTimeMillis();
    }
    public void cancelOrder(String orderId) throws Exception { }
    public void setLeverage(String symbol, int leverage) throws Exception { }
    public BigDecimal getAvailableMargin() throws Exception { return new BigDecimal("10000.0"); }
    public BigDecimal getATR(String symbol, int period) throws Exception { return new BigDecimal("0.02"); }
    public BigDecimal getLiquidationPrice(String symbol, BigDecimal position) throws Exception {
        return new BigDecimal("45000.0");
    }
}