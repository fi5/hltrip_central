package com.huoli.trip.central.web.converter;

/**
 * 描述：desc<br> 业务实体适配层
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
public interface Converter<T,S,U,V> {
	public S convertRequestToSupplierRequest(T req);
	public V convertSupplierResponseToResponse(U supplierResponse);
}
