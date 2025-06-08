package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Position {
    // 策略
    private String exchange;
    private final String instrumentId; // 交易对，如 BTC-USDT-SWAP
    private final String direction;    // 持仓方向：long/short
    private BigDecimal quantity;           // 持仓数量（正：多头，负：空头）
    private BigDecimal avgPrice;           // 平均持仓价格
    private Long updatedAt;         // 最后更新时间

    public Position(String instrumentId, String direction, BigDecimal quantity, BigDecimal avgPrice) {
        this.instrumentId = instrumentId;
        this.direction = direction;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Position{instId=" + instrumentId + ", dir=" + direction + ", qty=" + quantity + ", avgPx=" + avgPrice + "}";
    }
}
