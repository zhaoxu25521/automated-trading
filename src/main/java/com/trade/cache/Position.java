package com.trade.cache;

import com.trade.common.ExchangeEnums;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Position {
    // 预挂单金额
//    private BigDecimal preOpenAmount;
//    // 已挂单金额
//    private BigDecimal entrustOpenAmount;
    private TreeSet<Order> longOrders;
    private TreeSet<Order> shortOrders;

    private TreeSet<Order> closeLongOrders;
    private TreeSet<Order> closeShortOrders;
    // 缓存cliId 和订单映射，方便更新
    private transient Map<String, Order> orderMap;


    public Position() {
        this.shortOrders = new TreeSet<>(Comparator.comparing(Order::getPrice));
        this.longOrders = new TreeSet<>(Comparator.comparing(Order::getPrice).reversed());

        this.closeLongOrders = new TreeSet<>(Comparator.comparing(Order::getPrice));
        this.closeShortOrders = new TreeSet<>(Comparator.comparing(Order::getPrice).reversed());

        orderMap = new ConcurrentHashMap<>();
//        this.preAmount = BigDecimal.ZERO;
//        this.entrustAmount = BigDecimal.ZERO;
    }

    /**
     * 新增预挂单
     *
     * @param order
     * @return
     */
    public boolean add(Order order) {
        if (orderMap.containsKey(order.getCliId())) {
            return false;
        }
        orderMap.put(order.getCliId(), order);
//        this.preAmount = this.preAmount.add(order.getAmount());
        if (order.getDirection() == ExchangeEnums.Direction.LONG && order.getSide() == ExchangeEnums.OrderSide.BUY) {
            return this.longOrders.add(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.SHORT && order.getSide() == ExchangeEnums.OrderSide.SELL) {
            return this.shortOrders.add(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.LONG && order.getSide() == ExchangeEnums.OrderSide.SELL) {
            return this.closeLongOrders.add(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.SHORT && order.getSide() == ExchangeEnums.OrderSide.BUY) {
            return this.closeShortOrders.add(order);
        }
        return false;
    }

    public boolean remove(String cliId) {
        if (!orderMap.containsKey(cliId)) {
            return false;
        }
        Order order = orderMap.get(cliId);
        orderMap.remove(cliId);
        if (order.getDirection() == ExchangeEnums.Direction.LONG && order.getSide() == ExchangeEnums.OrderSide.BUY) {
            return this.longOrders.remove(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.SHORT && order.getSide() == ExchangeEnums.OrderSide.SELL) {
            return this.shortOrders.remove(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.LONG && order.getSide() == ExchangeEnums.OrderSide.SELL) {
            return this.closeLongOrders.remove(order);
        } else if (order.getDirection() == ExchangeEnums.Direction.SHORT && order.getSide() == ExchangeEnums.OrderSide.BUY) {
            return this.closeShortOrders.remove(order);
        }
        return false;
    }

    public boolean update(String cliId, String status) {
        if (!orderMap.containsKey(cliId)) {
            return false;
        }
        Order order = orderMap.get(cliId);
        order.updaet(status);
        return true;
    }

    public void update(String cliId, BigDecimal execAmount, String status) {
        Order order = orderMap.get(cliId);
        if (order == null) {
            return;
        }
        order.updaet(status,execAmount);
    }

    public void clear() {
        orderMap.clear();
        longOrders.clear();
        shortOrders.clear();
    }
}
