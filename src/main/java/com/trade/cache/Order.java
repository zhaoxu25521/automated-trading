package com.trade.cache;

import com.trade.common.ExchangeEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 开平仓模式下，side和posSide需要进行组合
 * 开多：买入开多（side 填写 buy； posSide 填写 long ）
 * 开空：卖出开空（side 填写 sell； posSide 填写 short ）
 * 平多：卖出平多（side 填写 sell；posSide 填写 long ）
 * 平空：买入平空（side 填写 buy； posSide 填写 short ）
 */
@Getter
public class Order {
    private String cliId;
    private String symbol;
    private ExchangeEnums.OrderSide side;
    private ExchangeEnums.Direction direction;
    private ExchangeEnums.OrderType type;
    private BigDecimal amount;
    private BigDecimal price;
    private BigDecimal execAmount;
    // INIT 初始化订单； NEW 新订单- 挂单成功； PART_FILLED 部分成交； FILLED 全部成交； CANCELED 已撤销； REJECTED 已拒绝；
    private String status;
    private String closeCli;
    private Long ts;
    public Order(String cliId,
                 String symbol,
                 ExchangeEnums.OrderSide side,
                 ExchangeEnums.Direction direction,
                 ExchangeEnums.OrderType type,
                 BigDecimal amount,
                 BigDecimal price) {
        this.symbol = symbol;
        this.side = side;
        this.direction = direction;
        this.type = type;
        this.cliId = cliId;
        this.amount = amount;
        this.price = price;
        this.status = "INIT";
        this.ts = System.currentTimeMillis();
    }

    public void updaet(String status,BigDecimal execAmount){
        this.status = status;
        this.execAmount = execAmount;
        this.ts = System.currentTimeMillis();
    }
    public void updaet(String status){
        this.status = status;
        this.ts = System.currentTimeMillis();

    }
}
