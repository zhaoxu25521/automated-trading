package com.trade.cache;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
//@Component
public class CacheManager {
    // {策略唯一编号: {交易对: {方向: 仓位}}}
    private static Map<String, Position> CACHE = new ConcurrentHashMap<>();
    private static Map<String,Order> ORDERS = new ConcurrentHashMap<>();
    public boolean init(String key){
        log.info("{}",CACHE.containsKey(key));
        if(!CACHE.containsKey(key)){
            CACHE.put(key,new Position());
        }
        return true;
    }


    public boolean add(String strategyId, Order order){
        if(!CACHE.containsKey(strategyId)){
            return false;
        }
        return CACHE.get(strategyId).add(order);
    }


    public boolean update(String strategyId,String cliId,String status){
        return CACHE.get(strategyId).update(cliId,status);
    }



    public void print(){
        log.info("{}", JSONObject.toJSONString(CACHE));
    }
}
