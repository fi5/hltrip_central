package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.ListResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductItemDao productItemDao;

    public ListResult mainList(String city, Integer type, Integer listSize){
        ListResult listResult = new ListResult();
        List<Integer> types;
        // 不限需要查所有类型
        if(type == ProductType.UN_LIMIT.getCode()){
            types = Lists.newArrayList(ProductType.FREE_TRIP.getCode(), ProductType.RESTAURANT.getCode(), ProductType.SCENIC_TICKET.getCode(), ProductType.SCENIC_TICKET_PLUS.getCode());
        } else if (type == ProductType.SCENIC_TICKET_PLUS.getCode()){  // 门票加需要查门票和门票+
            types = Lists.newArrayList( ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(type);
        }
        for (Integer t : types) {
            List<ProductItemPO> productItems = productItemDao.selectByCityAndType(city, t, listSize);
            if(ListUtils.isEmpty(productItems)){
                continue;
            }
            List<ProductPO> products = productDao.getProductListByItemIds(productItems.stream().map(item -> item.getCode()).collect(Collectors.toList()));
            if(ListUtils.isNotEmpty(products)){
                Map<String, List<ProductPO>> map = products.stream().collect(Collectors.groupingBy(productPO -> productPO.getMainItemId()));
                BaseListProduct product = new BaseListProduct();
                List<BaseProduct> baseProducts = Lists.newArrayList();
                product.setPros(baseProducts);
                List<ProductItemPO> mores = productItemDao.selectByCityAndType(city, t, ++listSize);
                product.setMore(0);
                if(mores.size() > productItems.size()){
                    product.setMore(1);
                }
                if(t == ProductType.FREE_TRIP.getCode()){
                    listResult.setHotels(product);
                } else if(t == ProductType.SCENIC_TICKET.getCode() || t == ProductType.SCENIC_TICKET_PLUS.getCode()){
                    listResult.setTickets(product);
                } else {
                    listResult.setCates(product);
                }
                map.forEach((k, v) -> {
                    ProductItemPO item = productItems.stream().filter(i -> StringUtils.equals(i.getCode(), k)).findFirst().orElse(new ProductItemPO());
                    v.stream().min(Comparator.comparing(p -> p.getSalePrice())).ifPresent(p -> {
                        BaseProduct baseProduct = new BaseProduct();
                        baseProduct.setPrice(p.getSalePrice().doubleValue());
                        baseProduct.setProductItemId(item.getCode());
                        baseProduct.setProductName(p.getName());
                        baseProduct.setDesc(item.getDescription());
                        if(ListUtils.isNotEmpty(p.getImages())){
                            baseProduct.setImg(p.getImages().get(0).getUrl());
                        }
                        if(ListUtils.isNotEmpty(item.getTags())){
                            baseProduct.setTags(item.getTags().stream().map(tag -> {
                                Tag newTag = new Tag();
                                newTag.setName(tag);
                                return newTag;
                            }).collect(Collectors.toList()));
                        }
                        baseProducts.add(baseProduct);
                    });
                });
            }
        }
        return listResult;
    }

    @Override
    public BaseResponse<List<Product>> pageList(ProductPageRequest request){
        BaseResponse baseResponse = new BaseResponse();
        List<Integer> types;
        // 不限需要查所有类型
        if(request.getType() == ProductType.UN_LIMIT.getCode()){
            types = Lists.newArrayList(ProductType.FREE_TRIP.getCode(), ProductType.RESTAURANT.getCode(), ProductType.SCENIC_TICKET.getCode(), ProductType.SCENIC_TICKET_PLUS.getCode());
        } else if (request.getType() == ProductType.SCENIC_TICKET_PLUS.getCode()){  // 门票加需要查门票和门票+
            types = Lists.newArrayList(ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(request.getType());
        }
        List<Product> products = Lists.newArrayList();
        for (Integer t : types) {
            int total = productDao.getListTotal(request.getCity(), request.getType());
            List<ProductPO> productPOs = productDao.getPageList(request.getCity(), t, request.getPageIndex(), request.getPageSize());
             products.addAll(productPOs.stream().map(po -> {
                Product product = JSON.parseObject(JSON.toJSONString(po), Product.class);
                ProductItemPO productItemPO = productItemDao.selectByCode(product.getCode());
                ProductItem productItem = JSON.parseObject(JSON.toJSONString(productItemPO), ProductItem.class);
                product.setMainItem(productItem);
                product.setTotal(total);
                return product;
            }).collect(Collectors.toList()));
        }
        return baseResponse.withSuccess(products);
    }
}
