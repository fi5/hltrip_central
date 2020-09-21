package com.huoli.trip.central.web;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/14<br>
 */
@Data
public class Protest {

    /**
     * 产品id
     */
    @Id
    private ObjectId id;

    /**
     * 产品编码（唯一）
     */
    private String code;

    /**
     * 供应商产品id
     */
    private String s2;

    /**
     * 产品名称
     */
    private String s1;

    /**
     * 供应商id
     */
    private String supplierId;
}
