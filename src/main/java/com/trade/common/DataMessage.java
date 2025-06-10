package com.trade.common;

import lombok.Data;

@Data
public class DataMessage<T> {
    private String exchange;
    private String channel;
    private T data;
}
