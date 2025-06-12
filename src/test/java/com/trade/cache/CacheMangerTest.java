package com.trade.cache;

import com.alibaba.fastjson2.JSONObject;
import com.trade.common.ExchangeEnums;
import com.trade.utils.SnowflakeIdGenerator;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.trade.utils.RandomUtils.*;

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
        long t = System.currentTimeMillis();
        String xxxxx = null;
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);
        Set<String> a = new HashSet<>();
        for (int i = 0; i < 10000; i++){
            xxxxx = String.valueOf(idGenerator.nextId());
            boolean x = a.add(xxxxx);
            System.out.println( xxxxx );
            if(!x ){
                System.out.println( xxxxx );
                break;
            }
        }
        System.out.println(a.size());
        System.out.println(System.currentTimeMillis() -t );
    }
}
