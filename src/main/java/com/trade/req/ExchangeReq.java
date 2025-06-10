package com.trade.req;

import lombok.Data;
import org.json.JSONObject;

@Data
public class ExchangeReq {


    private Long id;
    private String name;
    private JSONObject params;
    private String status;
    private Long createdAt;
    private Long updatedAt;
}
