package com.trade.socket.netty.handler.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.trade.socket.netty.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OkxHandler implements MessageHandler<String> {
    private static final String PONG = "pong";
    private static final String EVENT = "event";
    private static final String ARG = "arg";

    @Override
    public boolean handle(String message, HandlerContext ctx) {
        if(PONG.equalsIgnoreCase(message)){
            return true;
        }else {
            try {

                JSONObject jsonObject = JSONObject.parseObject(message);

                if(jsonObject.containsKey(EVENT)){
                    // 订阅事件
                    return true;
                }
                JSONObject arg = jsonObject.getJSONObject(ARG);
                JSONArray data = jsonObject.getJSONArray("data");
                JSONObject o = JSONObject.parse(data.get(data.size()-1).toString());


                log.info("channel : {} , instId : {} , price : {}", arg.getString("channel"),arg.getString("instId"),o.getBigDecimal("last").toPlainString());
            }catch (Exception e){
                log.info("error : {} , {}",message,e.getMessage());
            }
        }
        return true;
    }
}
