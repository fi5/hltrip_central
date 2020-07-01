package com.huoli.trip.central.web.service.impl;

import com.google.common.collect.ImmutableList;
import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.Product;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
public class ProductServiceImpl {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductItemDao productItemDao;

    @Autowired
    private PriceDao priceDao;

    public static final ImmutableList<Integer> types = ImmutableList.of(0, 1, 2, 3, 4);

    public List<Product> productList(String city, Integer type, Integer mainPageSize){
        List<ProductItemPO> productItems = productItemDao.selectByCityAndType(city, type, mainPageSize);
        if(ListUtils.isNotEmpty(productItems)){
            List<ProductPO> products = productDao.getProductListByItemIds(productItems.stream().map(item -> item.getCode()).collect(Collectors.toList()));
            if(ListUtils.isNotEmpty(products)){
                Map<String, List<ProductPO>> map = products.stream().collect(Collectors.groupingBy(productPO -> productPO.getMainItemId()));

            }
        }


        return null;
    }
}
