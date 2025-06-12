package com.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange")
public class Exchange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String params;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
