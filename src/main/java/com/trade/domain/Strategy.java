package com.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("strategy")
@ToString
public class Strategy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long exchangeId;
    private String params;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
