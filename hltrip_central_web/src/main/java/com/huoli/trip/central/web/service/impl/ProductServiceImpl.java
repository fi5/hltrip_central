package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.PriceInfoPO;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.CommonUtils;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import com.huoli.trip.common.vo.request.central.CategoryDetailRequest;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.request.central.RecommendRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.CategoryDetailResult;
import com.huoli.trip.common.vo.response.central.ProductPageResult;
import com.huoli.trip.common.vo.response.central.RecommendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Slf4j
@Service(timeout = 10000,group = "hltrip")
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductItemDao productItemDao;

    @Override
    public BaseResponse<ProductPageResult> pageList(ProductPageRequest request){
        ProductPageResult result = new ProductPageResult();
        List<Integer> types = getTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        for (Integer t : types) {
            int total = productDao.getListTotal(request.getCity(), t);
            List<ProductPO> productPOs = productDao.getPageList(request.getCity(), t, request.getPageIndex(), request.getPageSize());
            if(ListUtils.isNotEmpty(productPOs)){
                products.addAll(convertToProducts(productPOs, total));
            }
        }
        result.setProducts(products);
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request){
        CategoryDetailResult result = new CategoryDetailResult();
        List<ProductPO> productPOs = productDao.getProductListByItemId(request.getProductItemId());
        result.setProducts(convertToProducts(productPOs, 0));
        return BaseResponse.success(result);
    }

    private List<Product> convertToProducts(List<ProductPO> productPOs, int total){
        return productPOs.stream().map(po -> {
            Product product = JSON.parseObject(JSON.toJSONString(po), Product.class);
            ProductItem productItem = JSON.parseObject(JSON.toJSONString(po.getMainItem()), ProductItem.class);
            Double[] coordinateArr = po.getMainItem().getItemCoordinate();
            if(coordinateArr != null && coordinateArr.length == 2){
                Coordinate coordinate = new Coordinate();
                coordinate.setLongitude(coordinateArr[0]);
                coordinate.setLatitude(coordinateArr[1]);
                productItem.setItemCoordinate(coordinate);
            }
            product.setTotal(total);
            return product;
        }).collect(Collectors.toList());
    }

    @Override
    public BaseResponse<RecommendResult> recommendList(RecommendRequest request){
        RecommendResult result = new RecommendResult();
        List<Integer> types = getTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        for (Integer t : types) {
            List<ProductPO> productPOs;
            if(request.getPosition() == Constants.RECOMMEND_POSITION_MAIN){
                productPOs = productDao.getCoordinateRecommendList(request.getCoordinate(), request.getRadius(), t, request.getPageSize());
            } else {
                productPOs =productDao.getCityRecommendList(request.getCity(), t, request.getPageSize());
            }
            if(ListUtils.isNotEmpty(productPOs)){
                products.addAll(convertToProducts(productPOs, 0));
            }
        }
        result.setProducts(products);
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<List<PriceInfo>> productPriceCalendar(ProductPriceReq productPriceReq) {
        try {

            final PricePO pricePo = productDao.getPricePos(productPriceReq.getProductCode());
            List<PriceInfo> priceInfos = Lists.newArrayList();
            for(PriceInfoPO entry: pricePo.getPriceInfos()){
                PriceInfo target=new PriceInfo();
                BeanUtils.copyProperties(entry,target);
                priceInfos.add(target);
                log.info("这里的日期:"+ CommonUtils.dateFormat.format(target.getSaleDate()));
            }
            return BaseResponse.success(priceInfos);
        } catch (Exception e) {
        	log.info("",e);
        }
        return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
    }

    private List<Integer> getTypes(int type){
        List<Integer> types;
        // 不限需要查所有类型
        if(type == ProductType.UN_LIMIT.getCode()){
            types = Lists.newArrayList(ProductType.FREE_TRIP.getCode(), ProductType.RESTAURANT.getCode(), ProductType.SCENIC_TICKET.getCode(), ProductType.SCENIC_TICKET_PLUS.getCode());
        } else if (type == ProductType.SCENIC_TICKET_PLUS.getCode()){  // 门票加需要查门票和门票+
            types = Lists.newArrayList(ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(type);
        }
        return types;
    }
}
