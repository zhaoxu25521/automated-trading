package com.trade.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ExchangeConfig {
    private String exchangeName;
    private Map<String,String> parms;
}
