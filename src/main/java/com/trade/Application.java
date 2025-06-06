package com.trade;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * passphrase Zx1qaz@WSX
 * api c787ebc3-6e76-4ae8-8707-b30026068321
 * se  9DBBDB72CEAE15F5EA25AFB73B7890A1
 *
 * 模拟盘的请求的header里面需要添加 "x-simulated-trading: 1"。
 */
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }
} 