package com.imooc.sell.enums;

import lombok.Data;
import lombok.Getter;

//返回给前端提示的消息
@Getter
public enum ResultEnum {

    PRODUCT_NOT_EXIST(10,  "商品不存在"),
    PRODUCT_STOCK_ERROR(11, "库存不正确"),
    ORDER_NOT_EXIST(12,"订单不存在"),
    ORDERDETAIL_NOT_EXIST(13, "订单为空"),
    ;

    ResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private Integer code;

    private String message;


}
