package com.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


/**
 * passphrase Zx1qaz@WSX
 * api c787ebc3-6e76-4ae8-8707-b30026068321
 * se  9DBBDB72CEAE15F5EA25AFB73B7890A1
 * wss://wspap.okx.com:8443/ws/v5/public
 * 模拟盘的请求的header里面需要添加 "x-simulated-trading: 1"。
 */
@SpringBootApplication
@MapperScan("com.trade.mapper")
@EnableCaching
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class,args);
    }
}
