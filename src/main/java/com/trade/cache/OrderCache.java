package com.trade.cache;

import com.alibaba.fastjson.JSONObject;
import com.trade.dto.Order;
import com.trade.enums.ExchangeEnums;
import com.trade.enums.OrderEnums;
import com.trade.utils.ListUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 订单缓存，维护本地订单并支持比对与补单。
 */
public class OrderCache {
    private final Map<String, Order> ordersByClOrdId = new ConcurrentHashMap<>(); // 按 clOrdId 存储
    private final Map<String, String> clOrdIdByServerOrdId = new ConcurrentHashMap<>(); ; // 映射 serverOrdId 到 clOrdId
    private static final Map<String, TreeSet<Order>> openLongOrder = new ConcurrentHashMap<>();;
    // 预生成订单
    private static final Map<String, TreeSet<Order>> preOrderCache = new ConcurrentHashMap<>();
    // 挂单成功订单
    private static final Map<String, TreeSet<Order>> orderCache = new ConcurrentHashMap<>();
    static Order order1 = new Order("1",ExchangeEnums.BINANCE.name(), "ItemA","ItemA", "BTCUSDT", OrderEnums.OrderSide.BUY, OrderEnums.Direction.LONG,"limit","1",new BigDecimal("100.0"),new BigDecimal("0.01"),2);
    static Order order2 =new Order("1",ExchangeEnums.BINANCE.name(), "ItemB","ItemB", "BTCUSDT", OrderEnums.OrderSide.BUY,OrderEnums.Direction.LONG,"limit","1",new BigDecimal("100.01"),new BigDecimal("0.01"),2);
    static Order order3 =new Order("1",ExchangeEnums.BINANCE.name(), "ItemC","ItemC", "BTCUSDT", OrderEnums.OrderSide.BUY,OrderEnums.Direction.LONG,"limit","1",new BigDecimal("100.02"),new BigDecimal("0.01"),2);

    static {
        openLongOrder.put("1", ListUtils.newTreeSetDesc());
        // 向集合中添加元素，TreeSet 会自动排序
        openLongOrder.get("1").addAll(Arrays.asList(
                order1,order2,order3
        ));
    }


    public static void main(String[] args) {
        TreeSet<Order> orders = openLongOrder.get("1");
        System.out.println(orders.size());
        Order order = orders.first();
        order.setStatus("filled");
        orders.remove(order);
        System.out.println(JSONObject.toJSONString(order));
        System.out.println(JSONObject.toJSONString(openLongOrder));
        boolean add = orders.add(order1);
        System.out.println(add);

    }
    public static final String MAP_ORDER_KEY_FORMAT = "%s%s%s";
    public static String mapOrderKey(String key, OrderEnums.OrderSide orderSide, OrderEnums.Direction direction){
        return String.format(MAP_ORDER_KEY_FORMAT,key,orderSide, direction);
    }

    public static Boolean addPre(Order order){
        String key = mapOrderKey(order.getKey(),order.getSide(), order.getDirection());
        if (!preOrderCache.containsKey(key)){
            createPreCache(key, order.getSide(), order.getDirection());
        }
        return preOrderCache.get(key).add(order);
    }

    private static void createPreCache(String key, OrderEnums.OrderSide orderSide, OrderEnums.Direction direction){
        String mapOrderKey = mapOrderKey(key, orderSide, direction);
        switch (orderSide){
            case BUY ->{
                switch (direction){
                    // 开多
                    case LONG -> preOrderCache.put(mapOrderKey, ListUtils.newTreeSetDesc());
                    // 平空
                    case SHORT -> preOrderCache.put(mapOrderKey, ListUtils.newTreeSetDesc());
                }
            }case SELL -> {
                switch (direction){
                    // 平多
                    case LONG -> preOrderCache.put(mapOrderKey, ListUtils.newTreeSetAsc());
                    // 开空
                    case SHORT -> preOrderCache.put(mapOrderKey, ListUtils.newTreeSetAsc());
                }
            }
        }
    }

    // 挂单成功委托
    private static void createCache(String key, OrderEnums.OrderSide orderSide, OrderEnums.Direction direction){
        String mapOrderKey = mapOrderKey(key, orderSide, direction);
        switch (orderSide){
            case BUY ->{
                switch (direction){
                    // 开多
                    case LONG -> orderCache.put(mapOrderKey, ListUtils.newTreeSetDesc());
                    // 平空
                    case SHORT -> orderCache.put(mapOrderKey, ListUtils.newTreeSetDesc());
                }
            }case SELL -> {
                switch (direction){
                    // 平多
                    case LONG -> orderCache.put(mapOrderKey, ListUtils.newTreeSetAsc());
                    // 开空
                    case SHORT -> orderCache.put(mapOrderKey, ListUtils.newTreeSetAsc());
                }
            }
        }
    }


    public void addOrUpdateOrder(Order order) {
        ordersByClOrdId.put(order.getClientId(), order);
        if (order.getOrderId() != null) {
            clOrdIdByServerOrdId.put(order.getOrderId(), order.getClientId());
        }
        System.out.println("订单已缓存: " + order);
    }

    public void removeOrder(String clOrdId) {
        Order removed = ordersByClOrdId.remove(clOrdId);
        if (removed != null && removed.getOrderId() != null) {
            clOrdIdByServerOrdId.remove(removed.getOrderId());
        }
        if (removed != null) {
            System.out.println("订单已移除: " + removed);
        }
    }

    public Order getOrderByClOrdId(String clOrdId) {
        return ordersByClOrdId.get(clOrdId);
    }

    public Order getOrderByServerOrdId(String serverOrdId) {
        String clOrdId = clOrdIdByServerOrdId.get(serverOrdId);
        return clOrdId != null ? ordersByClOrdId.get(clOrdId) : null;
    }

    public boolean compareAndCheckSupplement(Order newOrder) {
        Order cachedOrder = ordersByClOrdId.get(newOrder.getClientId());
        if (cachedOrder == null) {
            System.out.println("订单不存在，需补单: " + newOrder);
            placeOrder(newOrder);
            return true;
        } else if (!cachedOrder.getStatus().equals(newOrder.getStatus()) &&
                !cachedOrder.getStatus().equals("filled") &&
                !cachedOrder.getStatus().equals("canceled")) {
            System.out.println("订单状态异常，需补单: 缓存=" + cachedOrder + ", 新订单=" + newOrder);
            placeOrder(newOrder);
            return true;
        } else {
            System.out.println("订单已存在，无需补单: " + newOrder);
            return false;
        }
    }

    private void placeOrder(Order order) {
//        String clientId = order.getClientId();
//        String posSide = order.getSide().equals("buy") ?
//                (order.getType().equals("open") ? "long" : "short") :
//                (order.getType().equals("open") ? "short" : "long");
//        String orderMsg = String.format(
//                "{\"op\":\"order\",\"args\":[{\"instId\":\"%s\",\"side\":\"%s\",\"posSide\":\"%s\",\"ordType\":\"limit\",\"px\":\"%s\",\"sz\":\"%s\",\"clOrdId\":\"%s\"}]}",
//                order.getInstrumentId(), order.getSide(), posSide,
//                order.getPrice().setScale(2, BigDecimal.ROUND_DOWN).toString(),
//                order.getQuantity().setScale(4, BigDecimal.ROUND_DOWN).toString(),
//                order.getClOrdId());
//        socketClient.sendMessage(clientId, orderMsg);
//        addOrUpdateOrder(order);
//        if (placeOrderCallback != null) {
//            placeOrderCallback.accept(order);
//        }
//        System.out.println("已发送订单请求: " + orderMsg);
    }

    public void handleOrderUpdate(String message) {
        if (message.contains("\"channel\":\"orders\"")) {
            String clOrdId = extractClOrdId(message);
            String serverOrdId = extractServerOrderId(message);
            String status = extractStatus(message);
            Order order = getOrderByClOrdId(clOrdId);
            if (order == null && serverOrdId != null) {
                order = getOrderByServerOrdId(serverOrdId);
            }
            if (order != null) {
//                order.setServerOrderId(serverOrdId);
//                order.setStatus(status);
//                addOrUpdateOrder(order);
//                System.out.println("订单状态更新: " + order);
//                if (order.getType().equals("open") && status.equals("filled")) {
//                    generateCloseOrder(order);
//                }
            } else {
                System.err.println("未找到订单: clOrdId=" + clOrdId + ", serverOrdId=" + serverOrdId);
            }
        }
    }

    private void generateCloseOrder(Order openOrder) {
        String closeSide = openOrder.getSide().equals("buy") ? "sell" : "buy";
//        String closeClOrdId = new OrderIdGenerator(openOrder.getClOrdId().split("-")[0])
//                .generateCloseOrderId(openOrder.getClOrdId());
//        BigDecimal priceFactor = closeSide.equals("buy") ?
//                new BigDecimal("1.01") : new BigDecimal("0.99");
//        BigDecimal closePrice = openOrder.getPrice().multiply(priceFactor)
//                .setScale(2, BigDecimal.ROUND_DOWN);
//        Order closeOrder = new Order(
//                closeClOrdId, openOrder.getClientId(), openOrder.getSymbol(),
//                closeSide, "close", openOrder.getClientId(),
//                closePrice, openOrder.getQuantity(), "new");
//        compareAndCheckSupplement(closeOrder);
    }

    private String extractClOrdId(String message) {
        return message.contains("\"clOrdId\"") ? message.split("\"clOrdId\":\"")[1].split("\"")[0] : null;
    }

    private String extractServerOrderId(String message) {
        return message.contains("\"ordId\"") ? message.split("\"ordId\":\"")[1].split("\"")[0] : null;
    }

    private String extractStatus(String message) {
        return message.contains("\"state\"") ? message.split("\"state\":\"")[1].split("\"")[0] : "unknown";
    }
}