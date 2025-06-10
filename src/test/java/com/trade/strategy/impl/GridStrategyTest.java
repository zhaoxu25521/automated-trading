//package com.trade.strategy.impl;
//
//import com.trade.domain.Strategy;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.test.context.TestPropertySource;
//
//import java.math.BigDecimal;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//
//@TestPropertySource("classpath:application-test.properties")
//
//class GridStrategyTest {
//    private GridStrategy gridStrategy;
//    private Strategy mockStrategy;
//
//    @BeforeEach
//    void setUp() {
//        mockStrategy = Mockito.mock(Strategy.class);
//        when(mockStrategy.getName()).thenReturn("TestGridStrategy");
//        when(mockStrategy.getParams()).thenReturn("{}");
//
//        gridStrategy = new GridStrategy();
//        gridStrategy.init(mockStrategy);
//    }
//
//    @Test
//    void testInitGridOrdersWithHedge() {
//        gridStrategy.onStart();
//
//        // 验证初始化了双向网格订单
//        assertFalse(gridStrategy.getLongOrders().isEmpty());
//        assertFalse(gridStrategy.getShortOrders().isEmpty());
//
//        // 验证做多网格价格递增
//        for (int i = 1; i < gridStrategy.getLongOrders().size(); i++) {
//            BigDecimal prev = gridStrategy.getLongOrders().get(i-1).getPrice();
//            BigDecimal curr = gridStrategy.getLongOrders().get(i).getPrice();
//            assertTrue(curr.compareTo(prev) > 0, "Long grid prices should increase");
//        }
//
//        // 验证做空网格价格递减
//        for (int i = 1; i < gridStrategy.getShortOrders().size(); i++) {
//            BigDecimal prev = gridStrategy.getShortOrders().get(i-1).getPrice();
//            BigDecimal curr = gridStrategy.getShortOrders().get(i).getPrice();
//            assertTrue(curr.compareTo(prev) < 0, "Short grid prices should decrease");
//        }
//    }
//
//    @Test
//    void testDynamicSpreadAdjustment() {
//        gridStrategy.onStart();
//        BigDecimal initialSpread = gridStrategy.getConfig().getBaseSpread();
//
//        // 模拟5个订单成交
////        for (int i = 0; i < 5; i++) {
////            gridStrategy.onOrderFilled(new Order());
////        }
//
//        // 验证价差已调整
//        BigDecimal newSpread = gridStrategy.getConfig().getCurrentSpread();
//        assertEquals(0, initialSpread.multiply(new BigDecimal("2")).compareTo(newSpread),
//                   "Spread should double after 5 filled orders");
//    }
//
//    @Test
//    void testPositionRatioControl() {
//        gridStrategy.onStart();
//
//        // 模拟净多头
//        gridStrategy.longPosition = new BigDecimal("2");
//        gridStrategy.shortPosition = new BigDecimal("1");
//        gridStrategy.executeHedgeOrders();
//
//        // 验证增加了空单对冲
//        assertTrue(gridStrategy.shortPosition.compareTo(new BigDecimal("1")) > 0,
//                  "Should increase short position when net long");
//
//        // 模拟净空头
//        gridStrategy.longPosition = new BigDecimal("1");
//        gridStrategy.shortPosition = new BigDecimal("2");
//        gridStrategy.executeHedgeOrders();
//
//        // 验证增加了多单对冲
//        assertTrue(gridStrategy.longPosition.compareTo(new BigDecimal("1")) > 0,
//                  "Should increase long position when net short");
//    }
//
//    @Test
//    void testExecuteHedgeOrders() {
//        gridStrategy.onStart();
//
//        // 测试净多头情况下的对冲
//        gridStrategy.longPosition = new BigDecimal("2");
//        gridStrategy.shortPosition = new BigDecimal("1");
//        gridStrategy.executeHedgeOrders();
//
//        // 验证增加了空单对冲
//        assertTrue(gridStrategy.shortPosition.compareTo(new BigDecimal("1")) > 0,
//                 "Should increase short position when net long");
//
//        // 测试净空头情况下的对冲
//        gridStrategy.longPosition = new BigDecimal("1");
//        gridStrategy.shortPosition = new BigDecimal("2");
//        gridStrategy.executeHedgeOrders();
//
//        // 验证增加了多单对冲
//        assertTrue(gridStrategy.longPosition.compareTo(new BigDecimal("1")) > 0,
//                 "Should increase long position when net short");
//
//        // 测试仓位限制
//        gridStrategy.longPosition = gridStrategy.getConfig().getMaxPosition().add(BigDecimal.ONE);
//        gridStrategy.shortPosition = BigDecimal.ZERO;
//        int initialLongOrders = gridStrategy.getLongOrders().size();
//        gridStrategy.executeHedgeOrders();
//
//        // 验证没有执行超出限制的对冲
//        assertEquals(initialLongOrders, gridStrategy.getLongOrders().size(),
//                   "Should not execute orders beyond position limit");
//    }
//
//    @Test
//    void testHedgePositionMonitoring() {
//        gridStrategy.onStart();
//        gridStrategy.executeHedgeOrders();
//
//        // 模拟亏损超过阈值
//        gridStrategy.getHedgePosition().multiply(new BigDecimal("-1.1"));
//        gridStrategy.monitorHedgePosition();
//
//        // 验证风控逻辑执行
//        assertTrue(gridStrategy.getHedgePosition().compareTo(BigDecimal.ZERO) == 0);
//    }
//
//    @Test
//    void testCloseAllHedgePositions() {
//        gridStrategy.onStart();
//        gridStrategy.executeHedgeOrders();
//        gridStrategy.closeAllHedgePositions();
//
//        // 验证仓位已清零
//        assertEquals(0, gridStrategy.getHedgePosition().compareTo(BigDecimal.ZERO));
//    }
//
//    @Test
//    void testConfigParsing() {
//        gridStrategy.onStart();
//
//        // 验证配置参数是否正确解析
//        assertEquals(5, gridStrategy.getConfig().getGridCount());
//        assertEquals(0.5, gridStrategy.getConfig().getHedgeRatio());
//        assertEquals(0.1, gridStrategy.getConfig().getStopLoss());
//    }
//
//    @Test
//    void testHedgeRatioCalculation() {
//        gridStrategy.onStart();
//
//        // 验证对冲订单数量是否正确
//        assertEquals(gridStrategy.getLongOrders().size() * 0.5,
//                     gridStrategy.getShortOrders().size());
//    }
//
//    @Test
//    void testToDecimalMethods() {
//        // 测试double转BigDecimal
//        BigDecimal decimalValue = gridStrategy.toDecimal(1.23456789);
//        assertEquals(new BigDecimal("1.23456789"), decimalValue);
//
//        // 测试String转BigDecimal
//        BigDecimal stringValue = gridStrategy.toDecimal("0.98765432");
//        assertEquals(new BigDecimal("0.98765432"), stringValue);
//
//        // 测试精度控制
//        BigDecimal roundedValue = gridStrategy.toDecimal(1.23456789, 4);
//        assertEquals(new BigDecimal("1.2346"), roundedValue);
//    }
//
//    @Test
//    void testPrecisionInCalculations() {
//        gridStrategy.onStart();
//        gridStrategy.executeHedgeOrders();
//
//        // 验证计算精度
//        BigDecimal position = gridStrategy.getHedgePosition();
//        assertTrue(position.scale() >= 8, "Position should maintain high precision");
//    }
//}
