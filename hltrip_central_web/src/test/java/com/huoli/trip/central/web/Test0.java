package com.huoli.trip.central.web;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.DateUtils;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.PriceInfoPO;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.vo.Product;
import com.huoli.trip.common.vo.request.central.CategoryDetailRequest;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
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
@SpringBootTest
@Slf4j
public class Test0 {
    @Autowired
    private ProductDao productDao;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductService productService;

    @Test
    public void test(){
        List<String> ids = Lists.newArrayList("11", "22","33","44","55","66","77");
//        List<ProductPO> list = getProductListByItemIdsPage(ids, 1, 3);
//        List<ProductPO> list = getProductList();
//        List<ProductPO> list = getGroup();
//        List<ProductPO> list = getPageList("", 1, 1, 60);
//        List<ProductPO> list = productDao.getProductListByItemId("yaochufa_29439");
        CategoryDetailRequest request = new CategoryDetailRequest();
        request.setProductItemId("yaochufa_29439");
        ProductPageRequest request1 = new ProductPageRequest();
        request1.setCity("北京市");
        request1.setType(0);
//        log.info("结果 = {}", JSON.toJSONString(productService.pageList(request1)));
        log.info("结果 = {}", JSON.toJSONString(productService.categoryDetail(request)));

    }

    public List<ProductPO> getPageList(String city, Integer type, int page, int size){
        MatchOperation matchOperation = Aggregation.match(Criteria.where("productType").is(type).and("city").is(city));
        GroupOperation groupOperation = Aggregation.group("mainItemCode").first("mainItemCode").as("mainItemCode");
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        long rows = (page - 1) * size;
        Aggregation aggregation = Aggregation.newAggregation(
//                matchOperation,
                groupOperation.min("salePrice").as("salePrice"),
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    public List<ProductPO> getGroup(){
        Criteria criteria = Criteria.where("code").in("yaochufa_247533_1724328", "yaochufa_247533_266960");
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        GroupOperation groupOperation = Aggregation.group("code").first("code").as("code")
                .first("salePrice").as("salePrice")
                .first("mainItem").as("mainItem");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                groupOperation.min("salePrice").as("salePrice"),
                Aggregation.project(ProductPO.class).andExclude("_id")
                );
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    public List<ProductPO> getProductList(){
//
        Criteria criteria = Criteria.where("code").in("yaochufa_247533_1724328", "yaochufa_247533_266960");
        return mongoTemplate.find(new Query(criteria), ProductPO.class);
    }

    public List<ProductPO> getProductListByItemIdsPage(List<String> itemIds, int page, int size){
//        NearQuery nearQuery = NearQuery.near(116.481533, 39.996504, Metrics.KILOMETERS).maxDistance(new Distance(100000, Metrics.KILOMETERS));
//        System.out.println("打印。。" + JSON.toJSONString(mongoTemplate.geoNear(nearQuery, ProductItemPO.class).getContent()));
//        return null;
//        Point point = new Point(116.481533, 39.996504);
//        Sphere sphere = new Sphere(point, new Distance(100, Metrics.KILOMETERS));
//        Query query = new Query(Criteria.where("itemCoordinate").within(sphere));
//        return mongoTemplate.find(query, ProductItemPO.class);
//
        Criteria criteria = Criteria.where("itype").in(itemIds);
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        GroupOperation groupOperation = Aggregation.group("mainItemId").first("mainItemId").as("mainItemId")
                .first("salePrice").as("salePrice")
                .first("mainItemId").as("mainItemCode")
                .first("mainItem").as("mainItem")
                .last("itype").as("itype");
        long rows = (page - 1) * size;
        Aggregation aggregation1 = Aggregation.newAggregation(Aggregation.match(criteria),
                groupOperation.count().as("total"),
                Aggregation.project(ProductPO.class).andExclude("_id"));
        AggregationResults<ProductPO> outputType1 = mongoTemplate.aggregate(aggregation1, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        System.out.println("total============ " + outputType1.getMappedResults().size());
        System.out.println(JSON.toJSONString(outputType1.getMappedResults()));
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                groupOperation.min("salePrice").as("salePrice"),
                sortOperation,
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

//    @Test
    public void testPriceInfos() throws Exception {
        String productCode = "yaochufa_247533_266960";
        String begin = "2020-07-03";
        String end = "2020-07-05";
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        Date date1 = DateUtils.parseTimeStringToDate2(begin);
        Date date2 = DateUtils.parseTimeStringToDate2(end);
//        Document queryObject = new Document();
//        queryObject.put("productCode", productCode);
        PricePO pricePos = productDao.getPricePos(productCode);
        if(pricePos!=null){
            List<PriceInfoPO> priceInfos = pricePos.getPriceInfos();
            System.out.println(priceInfos);
            List<PriceInfoPO> collect = priceInfos.stream().filter(s -> (s.getSaleDate().compareTo(date1)==0||s.getSaleDate().compareTo(date1)==1)
                    &&(s.getSaleDate().compareTo(date2)==0||s.getSaleDate().compareTo(date2)==-1)).collect(Collectors.toList());
            System.out.println(collect);
        }
//        Document fieldsObject = new Document();
//        fieldsObject.put("productCode", false);
//        fieldsObject.put("priceInfos", true);
//
//        Query query = new BasicQuery(queryObject, fieldsObject);

//        PricePO pricePO = mongoTemplate.findOne(query, PricePO.class);
//        System.out.println(pricePos);
//        List<PriceInfoPO> comments = Optional.of(pricePO).orElseThrow(Exception::new).getPriceInfos();
//        System.out.println(comments);
    }

}
