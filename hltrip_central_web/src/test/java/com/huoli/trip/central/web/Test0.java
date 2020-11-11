package com.huoli.trip.central.web;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.dao.ChannelDao;
import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.PriceInfoPO;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.vo.Coordinate;
import com.huoli.trip.common.vo.request.central.*;
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
    private PriceDao priceDao;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductService productService;
    @Autowired
    private ChannelDao channelDao;

//    @Test
    public void test(){
        List<String> ids = Lists.newArrayList("11", "22","33","44","55","66","77");
        List<ProductPO> list = getProductListByItemIdsPage(ids, 1, 10);
//        List<ProductPO> list = getProductList();
//        List<ProductPO> list = getGroup();
//        List<ProductPO> list = getPageList("", 1, 1, 60);
//        List<ProductPO> list = productDao.getProductListByItemId("yaochufa_29439");
//        CategoryDetailRequest request = new CategoryDetailRequest();
//        request.setProductItemId("yaochufa_29439");
//        request.setSaleDate(DateTimeUtil.trancateToDate(new Date()));
//        ProductPageRequest request1 = new ProductPageRequest();
//        request1.setCity("北京市");
//        request1.setType(0);
//        log.info("结果 = {}", JSON.toJSONString(productService.pageList(request1)));
//        log.info("结果 = {}", JSON.toJSONString(productService.categoryDetail(request)));
        log.info("结果 = {}", JSON.toJSONString(list));
    }

//    @Test
    public void test1(){
        ImageRequest request = new ImageRequest();
//        request.setProductCode("yaochufa_247533_1724328");
        request.setProductItemCode("yaochufa_29439");
        log.info("================================={}", JSON.toJSONString(productService.getImages(request)));
    }

//    @Test
    public void test2(){
        RecommendRequest request = new RecommendRequest();
        request.setPosition(1);
        request.setCity("北京市");
        Coordinate coordinate = new Coordinate();
        coordinate.setLatitude(40d);
        coordinate.setLongitude(116d);
        request.setCoordinate(coordinate);
        log.info("================================={}", JSON.toJSONString(productService.recommendList(request)));
    }

//    @Test
    public void test3(){
        ProductPageRequest request = new ProductPageRequest();
        request.setType(0);
//        request.setCity("广州");
        request.setPageSize(10);
//        request.setKeyWord("SIP单票（EB）测试");
        request.setType(1);
        log.info("================================={}", JSON.toJSONString(productService.pageList(request)));
    }

//    @Test
    public void test4(){
//        Document document = priceDao.selectByProductCode("yaochufa_247533_1724328");
//        PricePO pricePO = JSON.parseObject(, PricePO.class);
        log.info("xxxxxxxxxxxxxxxxxxxxxxxxxx======={}", JSON.toJSONString(priceDao.selectByProductCode("yaochufa_247533_1724328").getId()));
    }

//    @Test
    public void test5(){
        PriceCalcRequest request = new PriceCalcRequest();
        request.setStartDate(DateTimeUtil.parseDate("2020-08-31 00:00:00"));
        request.setEndDate(DateTimeUtil.parseDate("2020-08-31 00:00:00"));
        request.setProductCode("yaochufa_904834_2095377");
        request.setQuantity(1);
        request.setTraceId("01c6b55d2d061000");
        log.info("xxxxxxxxxxxxxxxxxxxxxxxxxx======= {}", JSON.toJSONString(productService.calcTotalPrice(request)));
//        Set<String> zoneIds = ZoneId.getAvailableZoneIds();
//        for  (String  zoneId: zoneIds) {
//            System.out.println(zoneId);
//        }

    }

    @Test
    public void test6(){
//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getPageList("北京市", 2, null, 1, 10)));
//
//        log.info("-----------------------------------  {}", productDao.getPageListTotal("北京市", 2, null));

//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getPageList("北京市", 2, null, 1, 10)));

//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getProductListByItemId("yaochufa_29439", DateTimeUtil.trancateToDate(new Date()))));
//
//        Coordinate coordinate = new Coordinate();
//        coordinate.setLongitude(116.481533);
//        coordinate.setLatitude(39.996504);
//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getLowPriceRecommendResult(2, coordinate, 100d, 10)));

//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getSalesRecommendList(
//                Lists.newArrayList("yaochufa_247533_266960",
//                        "yaochufa_247533_597563",
//                        "yaochufa_247533_1724318",
//                        "yaochufa_247533_1724328"))));

        RecommendRequest recommendRequest = new RecommendRequest();
        recommendRequest.setType(1);
        recommendRequest.setPosition(1);
//        try {
//            Thread.sleep(1000 * 60);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        log.info("-----------------------------------  {}", JSON.toJSONString(productService.recommendList(recommendRequest)));

//        log.info("-----------------------------------  {}", JSON.toJSONString(productDao.getByCityAndType("北京市", DateTimeUtil.trancateToDate(new Date()), 2, 10)));
    }

//    @Test
    public void test7(){
        log.info("=========  {}", JSON.toJSONString(mongoTemplate.findOne(new Query(Criteria.where("productCode").is("yaochufa_247533_597563")), PricePO.class)));
    }

//    @Test
    public void test8(){
        ProductPageRequest request = new ProductPageRequest();
        request.setPageSize(6);
        request.setCity("三亚");
//        request.setOriCity("北京");
        request.setPageIndex(1);
        request.setType(0);
        productService.pageList(request );
    }

//    @Test
    public void test9(){
        Criteria criteria = new Criteria();
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("supplierId").and("supplierId").substring(0, 3).as("s1")
                .and("supplierId").substring(3, 3).as("s2"),
                Aggregation.match(criteria.and("s2").is("chu").and("s1").is("yao"))
                );
        AggregationResults<Protest> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, Protest.class);
        log.info(JSON.toJSONString(outputType.getMappedResults().get(0)));
    }

//    @Test
    public void test10(){
        CategoryDetailRequest request = new CategoryDetailRequest();
        request.setProductItemId("yaochufa_sirofusk");
        request.setSaleDate(new Date(1602432000000L));
        log.info(JSON.toJSONString(productService.categoryDetail(request)));
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
                .first("code").as("code")
                .first("count").as("count")
                .last("itype").as("itype");
        long rows = (page - 1) * size;
        Aggregation aggregation1 = Aggregation.newAggregation(Aggregation.match(criteria),
                groupOperation.count().as("total"),
                Aggregation.project(ProductPO.class).andExclude("_id"));
//        AggregationResults<ProductPO> outputType1 = mongoTemplate.aggregate(aggregation1, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
//        System.out.println("total============ " + outputType1.getMappedResults().size());
//        System.out.println(JSON.toJSONString(outputType1.getMappedResults()));
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        CountOperation countOperation = Aggregation.count().as("count");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                sortOperation,
                groupOperation.min("salePrice").as("salePrice"),
                sortOperation,
                Aggregation.project(ProductPO.class).andExclude("_id"),
                countOperation,
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
        Date date1 = DateTimeUtil.parseDate(begin);
        Date date2 = DateTimeUtil.parseDate(end);
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

//    @Test
//    public void testHandleTimeZoneOutput() {
//        String date = "2020-07-17 00:00:00.000";
//        Date saleDate = MongoDateUtils.handleTimezoneOutput((DateTimeUtil.parseDate(date,DateTimeUtil.YYYYMMDD)));
//        System.out.println("时区转换后的时间"+saleDate);
//    }
//    @Test
//    public void testChannelList(){
//        List<Channel> channels = channelDao.queryChannelList();
//        for (Channel channel:channels){
//            System.out.println("看看结果集"+channel.getName());
//        }
//    }

}
