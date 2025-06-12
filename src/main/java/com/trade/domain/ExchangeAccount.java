package com.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange_account")
public class ExchangeAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long exchangeId;
    private String apiKey;
    private String secretKey;
    private String passphrase;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
