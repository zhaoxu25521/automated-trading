package com.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

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
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
