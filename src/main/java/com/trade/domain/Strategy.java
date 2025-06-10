package com.trade.domain;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("strategy")
public class Strategy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String exchange;
    private String params;
    private String status;
    private Long createdAt;
    private Long updatedAt;
}
