package com.trade.dto;

import com.trade.enums.OrderEnums;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class Order {

    private final String key;
    private final String exchange;
    private final String orderId;      // 订单唯一标识符
    private final String clientId;     // 关联的客户端ID
    private final String symbol; // 交易对，如 BTC-USDT
    private final OrderEnums.OrderSide side;
    private final OrderEnums.Direction direction;
    private final String orderType;
    private final String linkedOrderId;// 关联订单ID（平仓单关联开仓单）
    private final BigDecimal price;
    private final BigDecimal quantity;
    private BigDecimal executedQty;
    private final int scale;
    private String status;
    private final Long createdAt;   // 创建时间
    private Long updatedAt;         // 最后更新时间
     // 订单状态：new/filled/canceled/rejected

    public Order(String key,
                 String exchange,
                 String orderId,
                 String clientId,
                 String symbol,
                 OrderEnums.OrderSide side,
                 OrderEnums.Direction direction,
                 String orderType,
                 String linkedOrderId,
                 BigDecimal price,
                 BigDecimal quantity,
                 int scale) {
        this.key = key;
        this.exchange = exchange;
        this.orderId = orderId;
        this.clientId = clientId;
        this.symbol = symbol;
        this.side = side;
        this.direction = direction;
        this.orderType = orderType;
        this.linkedOrderId = linkedOrderId;
        this.price = price.setScale(scale, RoundingMode.HALF_UP);
        this.quantity = quantity.setScale(scale, RoundingMode.HALF_UP);
        this.executedQty = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        this.scale = scale;
        this.status = "new";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public void updateExecutedQty(BigDecimal qty) {
        this.executedQty = this.executedQty.add(qty).setScale(scale, RoundingMode.HALF_UP);
    }

    public boolean isFullyFilled() {
        return quantity.subtract(executedQty).abs().compareTo(new BigDecimal("0.0001")) < 0;
    }

    @Override
    public String toString() {
        return "Order{key= "+key+", id=" + orderId + ", clientId=" + clientId + ", instId=" + symbol +
                ", side=" + side + ", type=" + direction + ", linkedId=" + linkedOrderId +
                ", price=" + price + ", qty=" + quantity + ", status=" + status + "}";
    }
}
