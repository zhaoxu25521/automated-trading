package com.trade.dto;

import com.trade.enums.OrderEnums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Order {

    private final String orderId;
    private final String symbol;
    private final OrderEnums.OrderSide side;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private BigDecimal executedQty;
    private final int scale;

    public Order(String orderId, String symbol, OrderEnums.OrderSide side, BigDecimal price, BigDecimal quantity, int scale) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price.setScale(scale, RoundingMode.HALF_UP);
        this.quantity = quantity.setScale(scale, RoundingMode.HALF_UP);
        this.executedQty = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        this.scale = scale;
    }

    public void updateExecutedQty(BigDecimal qty) {
        this.executedQty = this.executedQty.add(qty).setScale(scale, RoundingMode.HALF_UP);
    }

    public boolean isFullyFilled() {
        return quantity.subtract(executedQty).abs().compareTo(new BigDecimal("0.0001")) < 0;
    }

    public String getSymbol() { return symbol; }
    public OrderEnums.OrderSide getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
}
