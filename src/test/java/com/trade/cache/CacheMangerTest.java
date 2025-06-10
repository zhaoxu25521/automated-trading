package com.trade.cache;

import com.trade.common.ExchangeEnums;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.trade.utils.RandomUtils.randomStringLang;

public class CacheMangerTest {

    @Test
    public void cacheTest(){
        CacheManager manager = new CacheManager();
        manager.init("gr-1");


        manager.add("gr-1",new Order("121","BTCUSDT",ExchangeEnums.OrderSide.BUY,ExchangeEnums.Direction.LONG,ExchangeEnums.OrderType.LIMIT,new BigDecimal("0.02"),new BigDecimal("95000")));
        manager.add("gr-1",new Order("122","BTCUSDT",ExchangeEnums.OrderSide.BUY,ExchangeEnums.Direction.LONG,ExchangeEnums.OrderType.LIMIT,new BigDecimal("0.01"),new BigDecimal("90000")));
        manager.add("gr-1",new Order("123","BTCUSDT",ExchangeEnums.OrderSide.BUY,ExchangeEnums.Direction.LONG,ExchangeEnums.OrderType.LIMIT,new BigDecimal("0.03"),new BigDecimal("92000")));

        manager.update("gr-1","121","SSSSSS");

        manager.print();
    }



    @Test
    public void cacheTest2() {
        Set<String> a = new HashSet<>();
        for (int i = 0; i < 1000000; i++){
            boolean add = a.add(randomStringLang(5));
            if(!add){
                System.out.println("重复字符串"+);
                break;
            }
        }
        System.out.println(a.size());
    }
}
