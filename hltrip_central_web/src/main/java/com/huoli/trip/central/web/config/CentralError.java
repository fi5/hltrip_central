package com.huoli.trip.central.web.config;

import lombok.Getter;

/**
 * 描述：中台错误配置<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
public enum CentralError {

    //100-999 服务器级别错误
    ERROR_BAD_REQUEST(400, "请求错误"),
    ERROR_SERVER_ERROR(500, "服务器内部错误"),

    //1000-1999 产品业务错误
    NO_RESULT_ERROR(1000, "无推荐产品"),

    //2000-2999 订单业务错误
    ERROR_NO_ORDER(2000, "查询订单信息不存在"),


    ERROR_UNKNOWN(9999, "未知错误");


    /**
     * 错误码
     */
    @Getter
    private int code;
    /**
     * 错误信息
     */
    @Getter
    private String error;

    CentralError(int code, String error) {
        this.code = code;
        this.error = error;
    }
}
