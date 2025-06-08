package com.trade.utils;

import com.trade.dto.Order;

import java.util.Comparator;
import java.util.TreeSet;

public class ListUtils {
    // 定义 Comparator，按 price 升序排序
    private static final Comparator<Order> priceComparatorAsc = Comparator.comparing(Order::getPrice);

    private static final Comparator<Order> priceComparatorDesc = Comparator.comparing(Order::getPrice).reversed();

    public static TreeSet<Order> newTreeSetAsc() {
        return new TreeSet<>(priceComparatorAsc);
    }
    public static TreeSet<Order> newTreeSetDesc() {
        return new TreeSet<>(priceComparatorDesc);
    }

}
