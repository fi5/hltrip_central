package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.util.CommonUtils;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import com.huoli.trip.common.vo.request.central.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.api.YcfSyncService;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfGetPriceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    @Autowired
    OrderFactory orderFactory;

    @Reference(group = "hltrip")
    private YcfSyncService ycfOrderService;

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
        convertToCategoryDetailResult(productPOs, result);
        return BaseResponse.success(result);
    }

    private void convertToCategoryDetailResult(List<ProductPO> productPOs, CategoryDetailResult result){
        if(ListUtils.isEmpty(productPOs)){
            log.info("没有查到商品详情");
            return;
        }
        result.setProducts(productPOs.stream().map(po -> {
            try {
                Product product = ProductConverter.convertToProduct(po, 0);
                if(result.getMainItem() == null){
                    result.setMainItem(JSON.parseObject(JSON.toJSONString(product.getMainItem()), ProductItem.class));
                }
                product.setMainItem(null);
                return product;
            } catch (Exception e){
                log.error("转换商品详情结果异常", e);
                return null;
            }
        }).filter(po -> po != null).collect(Collectors.toList()));
    }

    private List<Product> convertToProducts(List<ProductPO> productPOs, int total){
        return productPOs.stream().map(po -> ProductConverter.convertToProduct(po, total)).collect(Collectors.toList());
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
                productPOs = productDao.getCityRecommendList(request.getCity(), t, request.getPageSize());
            }
            if(ListUtils.isNotEmpty(productPOs)){
                products.addAll(convertToProducts(productPOs, 0));
            }
        }
        result.setProducts(products);
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<ProductPriceCalendarResult> productPriceCalendar(ProductPriceReq productPriceReq) {
        try {
            ProductPriceCalendarResult result=new ProductPriceCalendarResult();

            final PricePO pricePo = productDao.getPricePos(productPriceReq.getProductCode());
            List<PriceInfo> priceInfos = Lists.newArrayList();
            for(PriceInfoPO entry: pricePo.getPriceInfos()){
                PriceInfo target=new PriceInfo();
                BeanUtils.copyProperties(entry,target);
                priceInfos.add(target);
                log.info("这里的日期:"+ CommonUtils.dateFormat.format(target.getSaleDate()));
            }
            ProductPO productPo = productDao.getTripProductByCode(productPriceReq.getProductCode());
            result.setPriceInfos(priceInfos);
            result.setBuyMaxNight(productPo.getBuyMaxNight());
            result.setBuyMinNight(productPo.getBuyMinNight());
            if(productPo.getRoom()!=null){
                final Integer baseNum = productPo.getRoom().getRooms().get(0).getBaseNum();
                result.setBaseNum(baseNum);
            }

            return BaseResponse.success(result);
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

    @Override
    public BaseResponse<ProductPriceDetialResult> getPriceDetail(ProductPriceReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        if (orderManager == null) {
            return null;
        }

        try {
            ProductPO productPo = productDao.getTripProductByCode(req.getProductCode());
            final Product product = ProductConverter.convertToProduct(productPo, 0);
            ProductPriceDetialResult result =new ProductPriceDetialResult();
            req.setSupplierProductId(product.getSupplierProductId());

            YcfGetPriceRequest stockPriceReq=new YcfGetPriceRequest();
            stockPriceReq.setProductID(req.getSupplierProductId());
            stockPriceReq.setPartnerProductID(req.getProductCode());
            stockPriceReq.setStartDate(req.getStartDate());
            stockPriceReq.setEndDate(req.getEndDate());
            ycfOrderService.getPrice(stockPriceReq);//这个方法会查最新价格,存mongo

            result.setSupplierProductId(product.getSupplierProductId());
            result.setBookAheadMin(product.getBookAheadMin());
            result.setBuyMax(product.getBuyMax());
            result.setBuyMaxNight(product.getBuyMaxNight());
            result.setBuyMin(product.getBuyMin());
            result.setBuyMinNight(product.getBuyMinNight());
            result.setDelayType(product.getDelayType());
            result.setDescription(product.getDescription());
            result.setExcludeDesc(product.getExcludeDesc());
            result.setName(product.getName());
            result.setProductType(product.getProductType());
            result.setImages(product.getImages());
            result.setIncludeDesc(product.getIncludeDesc());
            result.setRefundType(product.getRefundType());
            result.setDelayType(product.getDelayType());
            result.setRefundAheadMin(product.getRefundAheadMin());
            result.setRefundDesc(product.getRefundDesc());
            result.setBookRules(product.getBookRules());
            result.setLimitRules(product.getLimitRules());
            result.setRoom(product.getRoom());
            result.setTicket(product.getTicket());
            result.setFood(product.getFood());
//  调用统一的价格计算并设值

            ca


            return BaseResponse.success(result);
        } catch (Exception e) {
        	log.info("",e);
            return null;
        }

    }

    @Override
    public List<ImageBase> getImages(ImageRequest request){
        if(StringUtils.isNotBlank(request.getProductCode())){
            ProductPO productPO = productDao.getImagesByCode(request.getProductCode());
            if(productPO != null && productPO.getImages() != null){
                return productPO.getImages().stream().map(image ->
                        ProductConverter.convertToImageBase(image)).collect(Collectors.toList());
            }
        } else if(StringUtils.isNotBlank(request.getProductItemCode())){
            ProductItemPO productItemPO = productItemDao.getImagesByCode(request.getProductItemCode());
            if(productItemPO != null && ListUtils.isNotEmpty(productItemPO.getImages())){
                return productItemPO.getImages().stream().map(image ->
                        ProductConverter.convertToImageBase(image)).collect(Collectors.toList());
            }
        }
        return null;
    }
}
