package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.*;
import com.huoli.trip.central.web.mapper.PassengerTemplateMapper;
import com.huoli.trip.central.web.service.CommonService;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.task.RecommendTask;
import com.huoli.trip.common.constant.*;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.entity.mpo.AddressInfo;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.recommend.RecommendBaseInfo;
import com.huoli.trip.common.entity.mpo.recommend.RecommendMPO;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotRuleMPO;
import com.huoli.trip.common.entity.po.PassengerTemplatePO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.BigDecimalUtil;
import com.huoli.trip.common.util.CommonUtils;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import com.huoli.trip.common.vo.request.central.*;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.goods.HotelScenicListReq;
import com.huoli.trip.common.vo.request.goods.ScenicTicketListReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;
import com.huoli.trip.common.vo.response.goods.*;
import com.huoli.trip.common.vo.response.recommend.RecommendResultV2;
import com.huoli.trip.common.vo.v2.BaseRefundRuleVO;
import com.huoli.trip.common.vo.v2.ScenicProductRefundRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.huoli.trip.central.web.constant.CentralConstants.RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX;
import static com.huoli.trip.central.web.constant.CentralConstants.RECOMMEND_LIST_POSITION_KEY_PREFIX;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Slf4j
@Service(timeout = 10000, group = "hltrip")
public class ProductServiceImpl implements ProductService {

    /**
     * 旅游类
     */
    private static final ImmutableList<Integer> TRIP_PRODUCTS = ImmutableList.of(ProductType.TRIP_FREE.getCode(),
            ProductType.TRIP_GROUP_PRIVATE.getCode(), ProductType.TRIP_GROUP.getCode(),
            ProductType.TRIP_GROUP_LOCAL.getCode(), ProductType.TRIP_GROUP_SEMI.getCode());

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductItemDao productItemDao;

    @Autowired
    private OrderFactory orderFactory;

    @Autowired
    private HodometerDao hodometerDao;

    @Autowired
    private RedisTemplate jedisTemplate;

    @Autowired
    private PriceDao priceDao;

    @Autowired
    private RecommendTask recommendTask;

    @Autowired
    private SupplierPolicyDao supplierPolicyDao;

    @Autowired
    private CommonService commonService;

    @Autowired
    private RecommendDao recommendDao;

    @Autowired
    private ScenicSpotProductDao scenicSpotProductDao;

    @Autowired
    private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

    @Autowired
    private ScenicSpotRuleDao scenicSpotRuleDao;

    @Autowired
    private GroupTourProductSetMealDao groupTourProductSetMealDao;

    @Autowired
    private HotelScenicSpotProductDao hotelScenicSpotProductDao;

    @Autowired
    private HotelScenicSpotProductSetMealDao hotelScenicSpotProductSetMealDao;

    @Autowired
    private GroupTourProductDao groupTourProductDao;

    @Autowired
    private ScenicSpotDao scenicSpotDao;

    @Autowired
    private PassengerTemplateMapper passengerTemplateMapper;

    @Override
    public BaseResponse<ProductPageResult> pageListForProduct(ProductPageRequest request) {
        ProductPageResult result = new ProductPageResult();
        List<Integer> types = ProductConverter.getTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        result.setProducts(products);
        StopWatch stopWatch = new StopWatch();
        for (Integer t : types) {
            int total = productDao.getPageListTotal(request.getCity(), t, request.getKeyWord());
            List<ProductPO> productPOs = productDao.getPageListProduct(request.getCity(), t, request.getKeyWord(), request.getPageIndex(), request.getPageSize());
            if (ListUtils.isNotEmpty(productPOs)) {
                products.addAll(convertToProducts(productPOs, total));
            }
        }
        log.info("耗时统计：{}", stopWatch.prettyPrint());
        if(ListUtils.isEmpty(products)){
            return BaseResponse.withFail(CentralError.NO_RESULT_PRODUCT_LIST_ERROR);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<ProductPageResult> pageList(ProductPageRequest request) {
        ProductPageResult result = new ProductPageResult();
        List<Integer> types = ProductConverter.getTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        result.setProducts(products);
        StopWatch stopWatch = new StopWatch();
        for (Integer t : types) {
            stopWatch.start("查总数type=" + t);
            long total = productDao.getPageListForItemTotal(request.getOriCity(), request.getCity(), t, request.getKeyWord(), request.getAppFrom());
            stopWatch.stop();
            stopWatch.start("查数据type=" + t);
            List<ProductItemPO> productItemPOs = productDao.getPageListForItem(request.getOriCity(), request.getCity(), t, request.getKeyWord(), request.getAppFrom(), request.getPageIndex(), request.getPageSize());
            stopWatch.stop();
            stopWatch.start("转换数据type=" + t);
            if (ListUtils.isNotEmpty(productItemPOs)) {
                products.addAll(convertToProductsByItem(productItemPOs, (int)total));
            }
            stopWatch.stop();
        }
        log.info("耗时统计：{}", stopWatch.prettyPrint());
        if(ListUtils.isEmpty(products)){
            return BaseResponse.withFail(CentralError.NO_RESULT_PRODUCT_LIST_ERROR);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request) {
        CategoryDetailResult result = new CategoryDetailResult();
        List<ProductPO> productPOs = productDao.getProductListByItemId(request.getProductItemId(), request.getSaleDate(), request.getAppFrom());
        convertToCategoryDetailResult(productPOs, result);
//        Map<String, List<Product>> products = result.getProducts().stream().collect(Collectors.groupingBy(p -> p.getPriceInfo().getSaleDate()));
//        List<String> dates = products.keySet().stream().sorted().collect(Collectors.toList());
//        result.setProducts(products.get(dates.get(0)));
        return BaseResponse.success(result);
    }

    @Override
    public BaseResponse<CategoryDetailResult> previewDetail(PreviewDetailRequest request) {
        CategoryDetailResult result = new CategoryDetailResult();
        ProductPO productPO = productDao.getPreviewDetail(request.getProductId());
        if(productPO == null){
            return BaseResponse.withFail(CentralError.PRICE_CALC_PRODUCT_NOT_FOUND_ERROR);
        }
        if(StringUtils.isBlank(productPO.getMainItemCode())){
            return BaseResponse.withFail(CentralError.NO_PRODUCT_MAIN_ITEM_NO_RELATED_ERROR);
        }
        convertToCategoryDetailResult(Lists.newArrayList(productPO), result);
        return BaseResponse.success(result);
    }

    @Override
    public BaseResponse<RecommendResult> recommendList(RecommendRequest request) {
        RecommendResult result = new RecommendResult();
        List<Integer> types = ProductConverter.getRecommendTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        result.setProducts(products);
        for (Integer t : types) {
            List<ProductPO> productPOs = null;
            // app主页推荐列表
            if (request.getPosition() == Constants.RECOMMEND_POSITION_MAIN ) {
                // 优先定位推荐，按坐标查低价
                if(request.getCoordinate() != null) {
                    productPOs = productDao.getNearRecommendResult(t, request.getCoordinate(), request.getRadius(), request.getPageSize());
                } else if(ListUtils.isNotEmpty(request.getProductCodes())){ // 按销量推荐
                    productPOs = productDao.getSalesRecommendList(request.getProductCodes());
                } else { // 最后用推荐标记查询
                    getFlagRecommend(products, t);
//                    productPOs = productDao.getFlagRecommendResult(t, request.getPageSize());
                }
            } else if(request.getPosition() == Constants.RECOMMEND_POSITION_TRIP_MAIN){  // 旅游首页推荐
                // 优先销量推荐
                if(ListUtils.isNotEmpty(request.getProductCodes())){
                    productPOs = productDao.getSalesRecommendList(request.getProductCodes());
                } else { // 按推荐标记推荐
                    getFlagRecommend(products, t);
//                    productPOs = productDao.getFlagRecommendResult(t, request.getPageSize());
                }
            } else {  // 其它根据城市和日期推荐
                if(StringUtils.isNotBlank(request.getCity()) && request.getSaleDate() != null){
                    productPOs = productDao.getByCityAndType(request.getCity(), request.getSaleDate(), t, request.getPageSize());
                }
            }
            if (ListUtils.isNotEmpty(productPOs)) {
                products.addAll(convertToProducts(productPOs, 0));
            }
            if(products.size() >= request.getPageSize()){
                break;
            }
        }
        if(ListUtils.isEmpty(products)){
            return BaseResponse.withFail(CentralError.NO_RESULT_RECOMMEND_LIST_ERROR);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<RecommendResult> recommendListV2(RecommendRequest request) {
        RecommendResult result = new RecommendResult();
        String key = String.join("_", RECOMMEND_LIST_POSITION_KEY_PREFIX, request.getPosition().toString());
        if(jedisTemplate.hasKey(key)){
            List<RecommendProduct> list = JSONArray.parseArray(jedisTemplate.opsForValue().get(key).toString(), RecommendProduct.class);
            log.info("{}缓存的推荐产品{}", key, JSON.toJSONString(list));
            result.setRecommendProducts(list);
            return BaseResponse.withSuccess(result);
        }
        recommendTask.refreshRecommendList(1);
        recommendTask.refreshRecommendListV2(1);
        ProductPageRequest productPageRequest = new ProductPageRequest();
        productPageRequest.setType(ProductType.FREE_TRIP.getCode());
        productPageRequest.setPageSize(4);
        BaseResponse<ProductPageResult> response = pageList(productPageRequest);
        if(response.getData() != null && response.getData().getProducts() != null){
            List<RecommendProduct> recommendProducts = response.getData().getProducts().stream().map(p -> {
                RecommendProduct recommendProduct = new RecommendProduct();
                recommendProduct.setPriceInfo(p.getPriceInfo());
                recommendProduct.setProductCode(p.getCode());
                recommendProduct.setProductName(p.getName());
                recommendProduct.setProductStatus(p.getStatus());
                recommendProduct.setProductType(p.getProductType());
                recommendProduct.setSupplierId(p.getSupplierId());
                recommendProduct.setSupplierName(p.getSupplierName());
                recommendProduct.setCity(p.getCity());
                recommendProduct.setMainImages(p.getMainItem().getMainImages());
                return recommendProduct;
            }).collect(Collectors.toList());
            result.setRecommendProducts(recommendProducts);
            return BaseResponse.withSuccess(result);
        }
        return BaseResponse.withFail(CentralError.NO_RESULT_RECOMMEND_LIST_ERROR);
    }

    /**
     * @author: wangying
     * @date 5/13/21 3:10 PM
     * 门票产品列表
     * [req]
     * @return {@link ScenicTicketListResult}
     * @throws
     */
    @Override
    public BaseResponse<ScenicTicketListResult> scenicTicketList(ScenicTicketListReq req) {
        List<ProductListMPO> productListMPOS = productDao.scenicTickets(req);
        int count = productDao.getScenicTicketTotal(req);
        ScenicTicketListResult result=new ScenicTicketListResult();
        if(count > req.getPageSize() * req.getPageIndex()){
            result.setMore(1);
        }
        List<ScenicTicketListItem> items = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(productListMPOS)){
            productListMPOS.stream().forEach(item -> {
                ScenicTicketListItem scenicTicketListItem = new ScenicTicketListItem();
                BeanUtils.copyProperties(item, scenicTicketListItem);
                //加价计算
                IncreasePrice increasePrice = increasePrice(item, req.getApp());
                // 设置价格
                scenicTicketListItem.setPrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                items.add(scenicTicketListItem);
            });
        }
        result.setItems(items);
        return BaseResponse.withSuccess(result);
    }

    /**
     * @author: wangying
     * @date 5/13/21 4:16 PM
     * 跟团游产品列表
     * [req]
     * @return {@link GroupTourListResult}
     * @throws
     */
    @Override
    public BaseResponse<GroupTourListResult> groupTourList(GroupTourListReq req) {
        List<ProductListMPO> productListMPOS = productDao.groupTourList(req);
        int count = productDao.groupTourListCount(req);
        GroupTourListResult result=new GroupTourListResult();
        if(count > req.getPageIndex() * req.getPageSize()){
            result.setMore(1);
        }
        List<GroupTourListItem> items = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(productListMPOS)){
            productListMPOS.stream().forEach(item -> {
                GroupTourListItem groupTourListItem = new GroupTourListItem();
                BeanUtils.copyProperties(item, groupTourListItem);
                //加价计算
                IncreasePrice increasePrice = increasePrice(item, req.getApp());
                // 设置价格
                groupTourListItem.setPrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                items.add(groupTourListItem);
            });
        }
        result.setItems(items);
        return BaseResponse.withSuccess(result);
    }

    /**
     * @author: wangying
     * @date 5/13/21 4:16 PM
     * 酒景产品列表
     * [req]
     * @return {@link HotelScenicListResult}
     * @throws
     */
    @Override
    public BaseResponse<HotelScenicListResult> hotelScenicList(HotelScenicListReq req) {
        List<ProductListMPO> productListMPOS = productDao.hotelScenicList(req);
        int count = productDao.hotelScenicListCount(req);
        HotelScenicListResult result=new HotelScenicListResult();
        if(count > req.getPageIndex() * req.getPageSize()){
            result.setMore(1);
        }
        List<HotelScenicListItem> items = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(productListMPOS)){
            productListMPOS.stream().forEach(item -> {
                HotelScenicListItem hotelScenicListItem = new HotelScenicListItem();
                BeanUtils.copyProperties(item, hotelScenicListItem);
                //加价计算
                IncreasePrice increasePrice = increasePrice(item, req.getApp());
                // 设置价格
                hotelScenicListItem.setPrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                items.add(hotelScenicListItem);
            });
        }
        result.setItems(items);
        return BaseResponse.withSuccess(result);
    }

    /**
     * @author: wangying
     * @date 5/13/21 4:21 PM
     * 组装参数调用加价计算
     * [productListMPO, app]
     * @return {@link IncreasePrice}
     * @throws
     */
    @Override
    public IncreasePrice increasePrice(ProductListMPO productListMPO, String app){
        IncreasePrice increasePrice = new IncreasePrice();
        increasePrice.setChannelCode(productListMPO.getChannel());
        increasePrice.setProductCode(productListMPO.getProductId());
        increasePrice.setProductCategory(productListMPO.getCategory());
        increasePrice.setAppSource(app);
        IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
        priceCalendar.setAdtSellPrice(productListMPO.getApiSellPrice());
        increasePrice.setPrices(Arrays.asList(priceCalendar));
        log.info("increasePrice:{}", JSONObject.toJSONString(increasePrice));
        commonService.increasePrice(increasePrice);
        return increasePrice;
    }

    @Override
    public BaseResponse<RecommendResultV2> recommendListV3(RecommendRequestV2 request) {
        String sysTag = "为你精选";
        RecommendResultV2 result = new RecommendResultV2();
        List<RecommendProductV2> products = Lists.newArrayList();
        List<RecommendMPO> recommendMPO = recommendDao.getList(request);
        List<RecommendBaseInfo> oriRecommendBaseInfos;
        if(ListUtils.isEmpty(recommendMPO)){
            // 如果带城市查不到就只按位置查
            List<RecommendMPO> recommendMPOs = recommendDao.getListByPosition(request);
            oriRecommendBaseInfos = recommendMPOs.stream().flatMap(r -> r.getRecommendBaseInfos().stream()).collect(Collectors.toList());
        } else {
            oriRecommendBaseInfos = recommendMPO.stream().flatMap(r -> r.getRecommendBaseInfos().stream()).collect(Collectors.toList());
        }
        // 只要参数有标签就处理，不考虑哪个位置；
        if(StringUtils.isNotBlank(request.getTag()) && StringUtils.equals(request.getTag(), sysTag)){
            oriRecommendBaseInfos.removeIf(r -> !StringUtils.equals(r.getTitle(), request.getTag()));
        }
        if(recommendMPO != null && ListUtils.isNotEmpty(oriRecommendBaseInfos)){
            oriRecommendBaseInfos = oriRecommendBaseInfos.stream().filter(rb -> rb.getApiSellPrice() != null).collect(Collectors.toList());
            List<RecommendBaseInfo> recommendBaseInfos;
            oriRecommendBaseInfos = resetRecommendBaseInfo(request.getAppSource(), oriRecommendBaseInfos);
            // 只有首页当地推荐需要补充逻辑
            if(request.getPosition() == 2){
                recommendBaseInfos = oriRecommendBaseInfos.stream().filter(rb -> {
                        boolean b = (StringUtils.equals(rb.getTitle(), request.getTag()) || StringUtils.equals(sysTag, request.getTag()));
                        // 用系统标签时就随便返回一些
                        if(StringUtils.equals(rb.getCategory(), "d_ss_ticket")){
                            return b && rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode();
                        } else {
                            b = b && rb.getProductStatus() == ProductStatus.STATUS_SELL.getCode();
                            if(ListUtils.isNotEmpty(rb.getAppSource())){
                                b = b && rb.getAppSource().contains(request.getAppSource());
                            }
                            return b;
                        }
                    }).collect(Collectors.toList());
                // 如果当前标签产品数量不够就用其它城市相同标签凑（这个理论上是能凑够的，因为之前获取标签的时候已经查过一遍了，但是不排除这段时间产品有变化导致数量不足的可能）
                if(recommendBaseInfos.size() < request.getPageSize() && !StringUtils.equals(sysTag, request.getTag())){
                    List<RecommendMPO> recommendMPOs = recommendDao.getListByTag(request.getPosition().toString(), Lists.newArrayList(request.getTag()));
                    List<RecommendBaseInfo> newRecommendBaseInfos = recommendMPOs.stream().filter(r ->
                            !StringUtils.equals(r.getCity(), request.getCity())).flatMap(r ->
                            r.getRecommendBaseInfos().stream()).filter(rb ->
                            StringUtils.equals(rb.getTitle(), request.getTag())
                                    && (StringUtils.equals(rb.getCategory(), "d_ss_ticket") ? rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode() :
                                    rb.getProductStatus() == ProductStatus.STATUS_SELL.getCode())).collect(Collectors.toList());
                    log.info("补充数据   {}", JSON.toJSONString(newRecommendBaseInfos));
                    newRecommendBaseInfos = resetRecommendBaseInfo(request.getAppSource(), newRecommendBaseInfos);
                    if(newRecommendBaseInfos.size() >= (request.getPageSize() - recommendBaseInfos.size())){
                        recommendBaseInfos.addAll(newRecommendBaseInfos);
                    }
                }
            }
            // 当地门票位置用主题过滤
            else if(request.getPosition() == 4){
                recommendBaseInfos = oriRecommendBaseInfos.stream().filter(rb ->
                        StringUtils.equals(rb.getSubjectCode(), request.getSubjectCode())
                                && rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode()).collect(Collectors.toList());
            }
            // 活动
            else if(request.getPosition() == 5){
                recommendBaseInfos = oriRecommendBaseInfos.stream().filter(rb -> StringUtils.equals(rb.getTitle(), request.getTag())).filter(rb ->
                        StringUtils.equals(rb.getCategory(), "d_ss_ticket") ? rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode() :
                                rb.getProductStatus() == ProductStatus.STATUS_SELL.getCode()).collect(Collectors.toList());
            }
            // 其它位置逻辑一样
            else {
                recommendBaseInfos = oriRecommendBaseInfos.stream().filter(rb ->
                        StringUtils.equals(rb.getCategory(), "d_ss_ticket") ? rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode() :
                                rb.getProductStatus() == ProductStatus.STATUS_SELL.getCode()).collect(Collectors.toList());
            }
            products = recommendBaseInfos.stream().map(rb ->
                    convertToRecommendProductV2(rb, request.getPosition().toString())).collect(Collectors.toList());
        }
        log.info("products:{}", products.size());
        if(products.size() > request.getPageSize()){
            products = products.subList(0, request.getPageSize());
            // 系统标签没有更多
            if(!StringUtils.equals(sysTag, request.getTag())){
                result.setMore(1);
            }
        }
        result.setProducts(products);
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<List<String>> recommendTags(RecommendRequestV2 request){
        List<RecommendMPO> recommendMPO = recommendDao.getList(request);
        List<String> tags = Lists.newArrayList();
        List<String> topTags = Lists.newArrayList();
        if(ListUtils.isEmpty(recommendMPO)){
            log.error("没有查到符合条件的标签。。");
            tags.add("为你精选");
            return BaseResponse.withSuccess(tags);
        }
        List<RecommendBaseInfo> baseInfos = recommendMPO.stream().flatMap(r ->
                r.getRecommendBaseInfos().stream()).filter(bi ->
                bi.getApiSellPrice() != null).collect(Collectors.toList());
        if(ListUtils.isNotEmpty(baseInfos)){
            baseInfos = resetRecommendBaseInfo(request.getAppSource(), baseInfos);
            Map<String, List<RecommendBaseInfo>> map = baseInfos.stream().collect(Collectors.groupingBy(RecommendBaseInfo::getTitle));
            List<String> shortTags = Lists.newArrayList();
            map.forEach((k, v) -> {
                if(v.size() >= request.getPageSize()){
                    // 包含晟和产品的标签要放到前面
                    if(v.stream().anyMatch(r -> StringUtils.equals(Constants.SUPPLIER_CODE_SHENGHE_TICKET, r.getChannel()))){
                        topTags.add(k);
                    } else {
                        tags.add(k);
                    }
                } else {
                    shortTags.add(k);
                }
            });
            // 如果当前城市的标签内容数量不够就用其它城市凑，够数就算；
            if(ListUtils.isNotEmpty(shortTags)){
                List<RecommendMPO> recommendMPOs = recommendDao.getListByTag(request.getPosition().toString(), shortTags);
                List<RecommendBaseInfo> newBaseInfos = recommendMPOs.stream().flatMap(r -> r.getRecommendBaseInfos().stream()).collect(Collectors.toList());
                newBaseInfos = newBaseInfos.stream().filter(nbi -> nbi.getApiSellPrice() != null).collect(Collectors.toList());
                newBaseInfos = resetRecommendBaseInfo(request.getAppSource(), newBaseInfos);
                Map<String, List<RecommendBaseInfo>> shortMap = newBaseInfos.stream().filter(rb ->
                        shortTags.contains(rb.getTitle())).collect(Collectors.groupingBy(RecommendBaseInfo::getTitle));
                shortMap.forEach((k, v) -> {
                    if(v.size() >= request.getPageSize()){
                        // 包含晟和产品的标签要放到前面
                        if(v.stream().anyMatch(r -> StringUtils.equals(Constants.SUPPLIER_CODE_SHENGHE_TICKET, r.getChannel()))){
                            topTags.add(k);
                        } else {
                            tags.add(k);
                        }
                    }
                });
            }
        }
        topTags.addAll(tags);
        // 实在没有就返回这个
        if(ListUtils.isEmpty(topTags)){
            log.error("东拼西凑也没有合适的标签。。");
            topTags.add("为你精选");
        }
        return BaseResponse.withSuccess(topTags);
    }

    @Override
    public BaseResponse<List<AddressInfo>> recommendCites(RecommendRequestV2 request){
        List<RecommendMPO> recommendMPOs = recommendDao.getCites(request);
        List<AddressInfo> addressInfos = recommendMPOs.stream().map(r -> {
            AddressInfo addressInfo = new AddressInfo();
            addressInfo.setCityCode(r.getCity());
            addressInfo.setCityName(r.getCityName());
            return addressInfo;
        }).collect(Collectors.toList());
        return BaseResponse.withSuccess(addressInfos);
    }

    @Override
    public BaseResponse<List<String>> recommendSubjects(RecommendRequestV2 request){
        List<RecommendMPO> recommendMPO = recommendDao.getList(request);
        if(ListUtils.isEmpty(recommendMPO)){
            // 如果带城市查不到就只按位置查
            recommendMPO = recommendDao.getListByPosition(request);
        }
        if(ListUtils.isEmpty(recommendMPO)){
            log.error("没有查到符合条件的主题。。");
            return BaseResponse.withSuccess();
        }
        List<RecommendBaseInfo> baseInfos = recommendMPO.stream().flatMap(r -> r.getRecommendBaseInfos().stream())
                .filter(rb -> StringUtils.isNotBlank(rb.getSubjectCode())
                        && rb.getPoiStatus() == ScenicSpotStatus.REVIEWED.getCode()
                        && rb.getApiSellPrice() != null).collect(Collectors.toList());
        List<String> subjects = Lists.newArrayList();
        if(ListUtils.isNotEmpty(baseInfos)){
            baseInfos = resetRecommendBaseInfo(request.getAppSource(), baseInfos);
            Map<String, List<RecommendBaseInfo>> map = baseInfos.stream().collect(Collectors.groupingBy(RecommendBaseInfo::getSubjectCode));
            map.forEach((k, v) -> {
                if(ListUtils.isNotEmpty(v)){
                    subjects.add(k);
                }
            });
        }
        return BaseResponse.withSuccess(subjects);
    }

    private List<RecommendBaseInfo> resetRecommendBaseInfo(String appSource, List<RecommendBaseInfo> oriRecommendBaseInfos){
        if(StringUtils.equals(appSource, "kssl")){
            List<RecommendBaseInfo> sort = Lists.newArrayList();
            // 凯撒商旅的晟和产品要排在前面
            sort.addAll(oriRecommendBaseInfos.stream().filter(rb -> StringUtils.equals(rb.getChannel(), "hllx_shenghe")).collect(Collectors.toList()));
            sort.addAll(oriRecommendBaseInfos.stream().filter(rb -> !StringUtils.equals(rb.getChannel(), "hllx_shenghe")).collect(Collectors.toList()));
            oriRecommendBaseInfos = sort;
        } else {
            // 凯撒商旅之外的来源不能卖晟和的产品
            oriRecommendBaseInfos.removeIf(rb -> StringUtils.equals(rb.getChannel(), "hllx_shenghe"));
        }
        return oriRecommendBaseInfos;
    }

    @Override
    public BaseResponse<ProductPriceCalendarResult> productPriceCalendar(ProductPriceReq productPriceReq) {
        try {
            ProductPriceCalendarResult result = new ProductPriceCalendarResult();

            final PricePO pricePo = productDao.getPricePos(productPriceReq.getProductCode());
            ProductPO productPO = productDao.getTripProductByCode(productPriceReq.getProductCode());
            // 提前预订天数
            Integer aheadDays = productPO.getBookAheadMin() == null ? null : (productPO.getBookAheadMin() / 60 / 24);
            if(null==pricePo || CollectionUtils.isEmpty(pricePo.getPriceInfos()))
                return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
            // 加价
            increasePrice(pricePo.getPriceInfos(), productPO.getSupplierId(), productPO.getCode(), productPO.getPrice());
            List<PriceInfo> priceInfos = Lists.newArrayList();
            for (PriceInfoPO entry : pricePo.getPriceInfos()) {
                String saleDate = CommonUtils.curDate.format(entry.getSaleDate());
                //过滤日期
                if(StringUtils.isNotBlank(productPriceReq.getStartDate())){
                    if(saleDate.compareTo(productPriceReq.getStartDate())<0)
                        continue;

                }
                // 日期小于今天的不返回
                if(entry.getSaleDate().getTime() < DateTimeUtil.trancateToDate(new Date()).getTime()){
                    continue;
                }
                // 兼容中石化
//                if(StringUtils.isNotBlank(productPriceReq.getEndDate())){
//                    if(saleDate.compareTo(productPriceReq.getEndDate())>0)
//                        continue;
//                }
                PriceInfo target = new PriceInfo();
                target.setSaleDate(saleDate);

                BeanUtils.copyProperties(entry, target);
                // 价格为空或者不是正数的过滤掉
                if(target.getSalePrice() == null || target.getSalePrice().floatValue()<=0)
                    continue;
                // 库存为空或者不是正数的过滤掉
                if(target.getStock() == null || target.getStock() <= 0){
                    continue;
                }
                // 预订的日期 - 今天 >= 提前预定天数  的才返回，小于预订天数内的不能订；
                if(aheadDays != null && DateTimeUtil.getDateDiffDays(entry.getSaleDate(), new Date()) < aheadDays ){
                    continue;
                }
                priceInfos.add(target);
//                log.info("这里的日期:" + CommonUtils.dateFormat.format(target.getSaleDate()));
            }
            ProductPO productPo = productDao.getTripProductByCode(productPriceReq.getProductCode());
            result.setPriceInfos(priceInfos);
            result.setBuyMaxNight(productPo.getBuyMaxNight());//购买晚数限制
            result.setBuyMinNight(productPo.getBuyMinNight());
            if (productPo.getRoom() != null&& CollectionUtils.isNotEmpty(productPo.getRoom().getRooms()))  {
                //设置基准晚数
                final Integer baseNum = productPo.getRoom().getRooms().get(0).getBaseNum();
                final Integer baseNight = productPo.getRoom().getRooms().get(0).getBaseNight();
                result.setBaseNum(baseNum);
                result.setBaseNight(baseNight);
            }
            if(TRIP_PRODUCTS.contains(productPo.getProductType())){
                result.setBaseNum(productPo.getTripDays());
            }

            return BaseResponse.success(result);
        } catch (Exception e) {
            log.error("productPriceCalendar价格日历报错:"+JSONObject.toJSONString(productPriceReq), e);
        }
        return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
    }

    @Override
    public BaseResponse<ProductPriceDetialResult> getPriceDetail(ProductPriceReq req) {

        try {
            //获取trip_product
            ProductPO productPo = productDao.getTripProductByCode(req.getProductCode());
            if(null==productPo )
                return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
            Product product = null;
            try {
                product=ProductConverter.convertToProduct(productPo, 0);
            } catch (Exception e) {
                log.info("",e);
            }
            if(null==product )
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            ProductPriceDetialResult result = new ProductPriceDetialResult();
            req.setSupplierProductId(product.getSupplierProductId());
            log.info("拿到的productInfo:"+JSONObject.toJSONString(product));
            log.info("信息:"+JSONObject.toJSONString(OrderFactory.orderManagerMap));

            String channelCode = productPo.getSupplierId();
         /*   if(channelCode.startsWith("hllx")){
                channelCode = "hllx";
            }*/
            log.info("渠道信息为：{}",channelCode);
            OrderManager orderManager = orderFactory.getOrderManager(channelCode);
            log.info("获取到的manager 是：{}",JSON.toJSONString(orderManager));
            if (orderManager == null) {
                return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
            }

            orderManager.refreshStockPrice(req);//这个方法会查最新价格,存mongo
            if (product.getMainItem() != null) {
                result.setMainItem(JSON.parseObject(JSON.toJSONString(product.getMainItem()), ProductItem.class));
                if(StringUtils.isBlank(result.getMainItem().getAppMainTitle())){
                    result.getMainItem().setAppMainTitle(product.getName());
                }
            }

            //处理product子item
            processProItem(product);
            result.setCode(product.getCode());
            result.setMainItemCode(product.getMainItemCode());
            result.setSupplierId(product.getSupplierId());
            result.setSupplierProductId(product.getSupplierProductId());
            result.setBookAheadMin(product.getBookAheadMin());
            result.setBuyMax(product.getBuyMax());
            result.setBuyMaxNight(product.getBuyMaxNight());
            result.setBuyMin(product.getBuyMin());
            result.setBuyMinNight(product.getBuyMinNight());
            result.setDelayType(product.getDelayType());
            result.setDescription(product.getDescription());
            if(StringUtils.isBlank(product.getDescription())){
                StringBuilder sb = new StringBuilder();
                if(StringUtils.isNotBlank(product.getBookDesc())){
                    sb.append("预定须知")
                            .append("<br>")
                            .append(product.getBookDesc())
                            .append("<br>");
                }
                if(StringUtils.isNotBlank(product.getIncludeDesc())){
                    sb.append("费用包含")
                            .append("<br>")
                            .append(product.getIncludeDesc())
                            .append("<br>");
                }
                if(StringUtils.isNotBlank(product.getExcludeDesc())){
                    sb.append("自理费用")
                            .append("<br>")
                            .append(product.getExcludeDesc())
                            .append("<br>");
                }
                if(StringUtils.isNotBlank(sb.toString())){
                    result.setDescription(sb.toString());
                }
            }
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
            result.setSupplierName(product.getSupplierName());
            result.setBookDesc(product.getBookDesc());
            result.setRemark(product.getRemark());
            result.setOperatorPhone(product.getOperatorPhone());
            result.setCityCode(product.getOriCityCode());
            result.setExtendParams(product.getExtendParams());
//  调用统一的价格计算并设值

            PriceCalcRequest priceCal=new PriceCalcRequest();
            priceCal.setQuantity(req.getCount());
            priceCal.setChdQuantity(req.getChdCount());
            if(StringUtils.isNotBlank(req.getStartDate()))
                priceCal.setStartDate(CommonUtils.curDate.parse(req.getStartDate()));
            if(StringUtils.isNotBlank(req.getEndDate()))
                priceCal.setEndDate(CommonUtils.curDate.parse(req.getEndDate()));
            priceCal.setProductCode(req.getProductCode());
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse = null;
            try {
                priceCalcResultBaseResponse= calcTotalPrice(priceCal);
            } catch (HlCentralException he) {
                return BaseResponse.fail(he.getCode(),he.getError(),he.getData());
            } catch (Exception e) {
                log.info("",e);
                return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
            }

            if(priceCalcResultBaseResponse.getCode()!=0){
                //抛出价格计算异常,如库存不足
                return BaseResponse.fail(priceCalcResultBaseResponse.getCode(),priceCalcResultBaseResponse.getMessage(),priceCalcResultBaseResponse.getData());
            }

            final PriceCalcResult priceCalData = priceCalcResultBaseResponse.getData();
            result.setSalePrice(priceCalData.getSalesTotal());
            result.setSettlePrice(priceCalData.getSettlesTotal());
            result.setStock(priceCalData.getMinStock());
            result.setChdSalePrice(priceCalData.getChdSalesPrice());
            result.setChdSettlePrice(priceCalData.getChdSettlePrice());
            result.setChdSalePriceTotal(priceCalData.getChdSalePriceTotal());
            result.setChdSettlePriceTotal(priceCalData.getChdSettlePriceTotal());
            result.setAdtSalePrice(priceCalData.getAdtSalesPrice());
            result.setAdtSettlePrice(priceCalData.getAdtSettlePrice());
            result.setAdtSalePriceTotal(priceCalData.getAdtSalePriceTotal());
            result.setAdtSettlePriceTotal(priceCalData.getAdtSettlePriceTotal());
            result.setStock(priceCalData.getStock());
            result.setRoomDiffPrice(priceCalData.getRoomDiffPrice());
            HodometerPO hodometerPO = hodometerDao.getHodometerByProductCode(req.getProductCode());
            if(hodometerPO != null){
                result.setHodometer(hodometerPO);
            }
            return BaseResponse.success(result);
        } catch (Exception e) {
            log.error("getPriceDetail报错:"+ JSONObject.toJSONString(req), e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }

    }

    @Override
    public BaseResponse<ProductPriceDetailResultV2> getPriceDetailV2(ProductPriceReq req) {
        try {
            String channel = null;
            ProductPriceDetailResultV2 result = new ProductPriceDetailResultV2();
            if(StringUtils.equals(req.getCategory(), "d_ss_ticket")) {
                ScenicSpotProductMPO productMPO = scenicSpotProductDao.getProductById(req.getProductCode());
                if (null == productMPO) {
                    return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
                }
                ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(req.getPackageCode());
                if (priceMPO == null) {
                    return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
                }
                ScenicSpotMPO scenicSpotMPO = scenicSpotDao.qyerySpotById(productMPO.getScenicSpotId());
                if(scenicSpotMPO == null){
                    return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
                }
                ScenicSpotRuleMPO ruleMPO = scenicSpotRuleDao.getRuleById(priceMPO.getScenicSpotRuleId());
                // 如果规则没有。下面很多信息都拿不到
                if (ruleMPO == null) {
                    return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
                }
                if (ListUtils.isNotEmpty(ruleMPO.getRefundRules())) {
                    result.setRefundRules(ruleMPO.getRefundRules().stream().map(rr -> {
                        ScenicProductRefundRule scenicProductRefundRule = new ScenicProductRefundRule();
                        scenicProductRefundRule.setDay(rr.getDay());
                        scenicProductRefundRule.setRefundRuleType(rr.getRefundRuleType());
                        scenicProductRefundRule.setDeductionType(rr.getDeductionType());
                        scenicProductRefundRule.setFee(rr.getFee());
                        scenicProductRefundRule.setHour(rr.getHour());
                        scenicProductRefundRule.setMinute(rr.getMinute());
                        return scenicProductRefundRule;
                    }).collect(Collectors.toList()));
                }
                result.setProductCode(productMPO.getId());
                result.setProductName(productMPO.getName());
                result.setScenicspotName(scenicSpotMPO.getName());
                result.setSupplierId(productMPO.getChannel());
                result.setSupplierProductId(productMPO.getSupplierProductId());
                result.setBookBeforeDay(productMPO.getScenicSpotProductTransaction().getBookBeforeDay());
                result.setBookBeforeTime(productMPO.getScenicSpotProductTransaction().getBookBeforeTime());
                result.setBuyMax(ruleMPO.getMaxCount());
                result.setBuyMin(1);
                result.setDescription(productMPO.getPcDescription());
                if(ListUtils.isNotEmpty(productMPO.getDescInfos())){
                    productMPO.getDescInfos().stream().filter(di ->
                            StringUtils.equals(di.getTitle(), "费用不包含")).findFirst().ifPresent(di ->
                            result.setExcludeDesc(di.getContent()));
                }
                result.setImages(productMPO.getImages());
                result.setIncludeDesc(ruleMPO.getFeeInclude());
                result.setRefundType(ruleMPO.getRefundCondition());
                result.setRefundDesc(ruleMPO.getRefundRuleDesc());
                result.setTicketInfos(ruleMPO.getTicketInfos());
                result.setTicketCardTypes(ruleMPO.getTicketCardTypes());
                result.setTravellerInfos(ruleMPO.getTravellerInfos());
                result.setTravellerTypes(ruleMPO.getTravellerTypes());
                if(ListUtils.isNotEmpty(result.getTravellerInfos())){
                    result.setPeopleLimit(1);
                } else {
                    result.setPeopleLimit(-1);
                }
                ProductPriceDetailResultV2.TicketInfo ticketInfo = new ProductPriceDetailResultV2.TicketInfo();
                ticketInfo.setCardType(ruleMPO.getCardType());
                ticketInfo.setChangeTicketAddress(ruleMPO.getChangeTicketAddress());
                ticketInfo.setInAddress(ruleMPO.getInAddress());
                ticketInfo.setInType(ruleMPO.getInType());
                ticketInfo.setVoucherType(ruleMPO.getVoucherType());
                result.setTicketInfo(ticketInfo);
                result.setRemark(ruleMPO.getSupplementDesc());
                result.setDepCityCode(scenicSpotMPO.getCityCode());
                result.setArrCityCode(scenicSpotMPO.getCityCode());
                result.setDescInfos(productMPO.getDescInfos());
                result.setExtendParams(productMPO.getExtendParams());
                result.setAddress(scenicSpotMPO.getAddress());
                channel = productMPO.getChannel();
            } else if(StringUtils.equals(req.getCategory(), "group_tour")){
                GroupTourProductSetMealMPO setMealMPO = groupTourProductSetMealDao.getSetMealById(req.getPackageCode());
                GroupTourProductMPO productMPO = groupTourProductDao.getProductById(req.getProductCode());
                if (null == setMealMPO) {
                    return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
                }
                PassengerTemplatePO passengerTemplatePO = passengerTemplateMapper.getPassengerTemplateById(productMPO.getTravelerTemplateId());
                if(passengerTemplatePO != null){
                    result.setPeopleLimit(passengerTemplatePO.getPeopleLimit());
                    if(StringUtils.isNotBlank(passengerTemplatePO.getPassengerInfo())){
                        result.setTravellerInfos(Arrays.stream(passengerTemplatePO.getPassengerInfo().split(",")).map(Integer::valueOf).collect(Collectors.toList()));
                    }
                    if(StringUtils.isNotBlank(passengerTemplatePO.getIdInfo())){
                        result.setTravellerTypes(Arrays.stream(passengerTemplatePO.getIdInfo().split(",")).map(Integer::valueOf).collect(Collectors.toList()));
                    }
                }
                result.setProductCode(productMPO.getId());
                result.setPackageCode(setMealMPO.getId());
                result.setProductName(productMPO.getProductName());
                result.setPackageName(setMealMPO.getName());
                result.setSupplierId(productMPO.getChannel());
                result.setSupplierProductId(productMPO.getSupplierProductId());
                result.setBookBeforeDay(productMPO.getGroupTourProductPayInfo().getBeforeBookDay());
                result.setDescription(productMPO.getComputerDesc());
                result.setExcludeDesc(setMealMPO.getCostExclude());
                result.setIncludeDesc(setMealMPO.getConstInclude());
                result.setImages(productMPO.getImages());
                result.setBookDesc(setMealMPO.getBookNotice());
                if(ListUtils.isNotEmpty(productMPO.getDepInfos())){
                    result.setDepCityCode(productMPO.getDepInfos().stream().filter(dep ->
                            StringUtils.isNotBlank(dep.getCityCode())).map(dep ->
                            dep.getCityCode()).collect(Collectors.joining(",")));
                }
                if(ListUtils.isNotEmpty(productMPO.getArrInfos())){
                    result.setArrCityCode(productMPO.getArrInfos().stream().filter(arr ->
                            StringUtils.isNotBlank(arr.getCityCode())).map(arr ->
                            arr.getCityCode()).collect(Collectors.joining(",")));
                }
                result.setGroupTourTripInfos(setMealMPO.getGroupTourTripInfos());
                if(ListUtils.isNotEmpty(productMPO.getGroupTourRefundRules())){
                    result.setRefundRuleVOs(productMPO.getGroupTourRefundRules().stream().map(grr -> {
                        BaseRefundRuleVO baseRefundRuleVO = new BaseRefundRuleVO();
                        baseRefundRuleVO.setBuyersFee(grr.getBuyersFee());
                        baseRefundRuleVO.setType(grr.getType());
                        baseRefundRuleVO.setMaxDay(grr.getMaxDay());
                        baseRefundRuleVO.setMinDay(grr.getMinDay());
                        baseRefundRuleVO.setSellerFee(grr.getSellerFee());
                        return baseRefundRuleVO;
                    }).collect(Collectors.toList()));
                }
                channel = productMPO.getChannel();
            } else if(StringUtils.equals(req.getCategory(), "hotel_scenicSpot")){
                HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicSpotProductSetMealDao.getSetMealById(req.getPackageCode());
                HotelScenicSpotProductMPO productMPO = hotelScenicSpotProductDao.getProductById(req.getProductCode());
                PassengerTemplatePO passengerTemplatePO = passengerTemplateMapper.getPassengerTemplateById(StringUtils.isNotBlank(productMPO.getTravellerTemplateId()) ? Integer.parseInt(productMPO.getTravellerTemplateId()) : 0);
                if(passengerTemplatePO != null){
                    result.setPeopleLimit(passengerTemplatePO.getPeopleLimit());
                    if(StringUtils.isNotBlank(passengerTemplatePO.getPassengerInfo())){
                        result.setTravellerInfos(Arrays.stream(passengerTemplatePO.getPassengerInfo().split(",")).map(Integer::valueOf).collect(Collectors.toList()));
                    }
                    if(StringUtils.isNotBlank(passengerTemplatePO.getIdInfo())){
                        result.setTravellerTypes(Arrays.stream(passengerTemplatePO.getIdInfo().split(",")).map(Integer::valueOf).collect(Collectors.toList()));
                    }
                }
                result.setDay(String.valueOf(productMPO.getDay()));
                result.setNight(String.valueOf(productMPO.getNight()));
                result.setProductCode(productMPO.getId());
                result.setPackageCode(setMealMPO.getId());
                result.setProductName(productMPO.getProductName());
                result.setPackageName(setMealMPO.getName());
                result.setSupplierId(productMPO.getChannel());
                result.setSupplierProductId(productMPO.getSupplierProductId());
                result.setBookBeforeDay(productMPO.getPayInfo().getBeforeBookDay());
                result.setBuyMax(setMealMPO.getBuyMax());
                result.setBuyMin(setMealMPO.getBuyMin());
                result.setDescription(productMPO.getComputerDesc());
                result.setExcludeDesc(productMPO.getPayInfo().getCostExclude());
                result.setImages(productMPO.getImages());
                result.setHotelElements(setMealMPO.getHotelElements());
                result.setOtherElements(setMealMPO.getOtherElements());
                result.setRestaurantElements(setMealMPO.getRestaurantElements());
                result.setSpaElements(setMealMPO.getSpaElements());
                result.setScenicSpotElements(setMealMPO.getScenicSpotElements());
                result.setSpecialActivityElements(setMealMPO.getSpecialActivityElements());
                result.setTrafficConnectionElements(setMealMPO.getTrafficConnectionElements());
                result.setBookDesc(productMPO.getPayInfo().getBookNotice());
                result.setRemark(setMealMPO.getMealDesc());
                if(ListUtils.isNotEmpty(productMPO.getAddressInfo())){
                    result.setArrCityCode(productMPO.getAddressInfo().stream().filter(arr ->
                            StringUtils.isNotBlank(arr.getCityCode())).map(arr ->
                            arr.getCityCode()).collect(Collectors.joining(",")));
                }
                if(ListUtils.isNotEmpty(productMPO.getRefundRules())){
                    result.setRefundRuleVOs(productMPO.getRefundRules().stream().map(grr -> {
                        BaseRefundRuleVO baseRefundRuleVO = new BaseRefundRuleVO();
                        baseRefundRuleVO.setBuyersFee(grr.getBuyersFee());
                        baseRefundRuleVO.setType(grr.getType());
                        baseRefundRuleVO.setMaxDay(grr.getMaxDay());
                        baseRefundRuleVO.setMinDay(grr.getMinDay());
                        baseRefundRuleVO.setSellerFee(grr.getSellerFee());
                        return baseRefundRuleVO;
                    }).collect(Collectors.toList()));
                }
                channel = productMPO.getChannel();
            }

            PriceCalcRequest priceCal = new PriceCalcRequest();
            priceCal.setQuantity(req.getCount());
            priceCal.setChdQuantity(req.getChdCount());
            if (StringUtils.isNotBlank(req.getStartDate())) {
                priceCal.setStartDate(DateTimeUtil.parseDate(req.getStartDate()));
            }
            if (StringUtils.isNotBlank(req.getEndDate())) {
                priceCal.setEndDate(DateTimeUtil.parseDate(req.getEndDate()));
            }
            priceCal.setProductCode(req.getProductCode());
            priceCal.setChannelCode(channel);
            priceCal.setCategory(req.getCategory());
            priceCal.setPackageCode(req.getPackageCode());
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse = null;
            try {
                priceCalcResultBaseResponse = calcTotalPriceV2(priceCal);
            } catch (HlCentralException he) {
                return BaseResponse.fail(he.getCode(), he.getError(), he.getData());
            } catch (Exception e) {
                log.error("计算价格异常", e);
                return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
            }

            if (priceCalcResultBaseResponse.getCode() != 0) {
                //抛出价格计算异常,如库存不足
                return BaseResponse.fail(priceCalcResultBaseResponse.getCode(), priceCalcResultBaseResponse.getMessage(), priceCalcResultBaseResponse.getData());
            }

            PriceCalcResult priceCalData = priceCalcResultBaseResponse.getData();
            result.setSalePrice(priceCalData.getSalesTotal());
            result.setSettlePrice(priceCalData.getSettlesTotal());
            result.setStock(priceCalData.getMinStock());
            result.setChdSalePrice(priceCalData.getChdSalesPrice());
            result.setChdSettlePrice(priceCalData.getChdSettlePrice());
            result.setChdSalePriceTotal(priceCalData.getChdSalePriceTotal());
            result.setChdSettlePriceTotal(priceCalData.getChdSettlePriceTotal());
            result.setAdtSalePrice(priceCalData.getAdtSalesPrice());
            result.setAdtSettlePrice(priceCalData.getAdtSettlePrice());
            result.setAdtSalePriceTotal(priceCalData.getAdtSalePriceTotal());
            result.setAdtSettlePriceTotal(priceCalData.getAdtSettlePriceTotal());
            result.setStock(priceCalData.getStock());
            result.setRoomDiffPrice(priceCalData.getRoomDiffPrice());
            return BaseResponse.success(result);
        } catch (Exception e) {
            log.error("getPriceDetail报错:"+ JSONObject.toJSONString(req), e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }
    }

    /**
     * 查询room,ticket,food里的item
     * @param product
     */
    private void processProItem(Product product) {

        try {
            if(product.getRoom()!=null && CollectionUtils.isNotEmpty(product.getRoom().getRooms())){

                for(ResourceRoom room: product.getRoom().getRooms()){
                    ProductItemPO itemPo = productItemDao.getByCode(room.getItemId());
                    ProductItem productItem = ProductConverter.convertToProductItem(itemPo);
                    room.setProductItem(productItem);
                }
            }
            if(product.getFood()!=null && CollectionUtils.isNotEmpty(product.getFood().getFoods())){

                for(ResourceFood food: product.getFood().getFoods()){
                    ProductItemPO itemPo = productItemDao.getByCode(food.getItemId());
                    ProductItem productItem = ProductConverter.convertToProductItem(itemPo);
                    food.setProductItem(productItem);
                }
            }
            if(product.getTicket()!=null && CollectionUtils.isNotEmpty(product.getTicket().getTickets())){

                for(ResourceTicket ticket: product.getTicket().getTickets()){
                    ProductItemPO itemPo = productItemDao.getByCode(ticket.getItemId());
                    ProductItem productItem = ProductConverter.convertToProductItem(itemPo);
                    ticket.setProductItem(productItem);
                }
            }

        } catch (Exception e) {
            log.info("",e);
        }

    }

    @Override
    public BaseResponse<List<ImageBase>> getImages(ImageRequest request) {
        if (StringUtils.isNotBlank(request.getProductCode())) {
            ProductPO productPO = productDao.getImagesByCode(request.getProductCode());
            if (productPO != null && productPO.getImages() != null) {
                return BaseResponse.withSuccess(productPO.getImages().stream().map(image ->
                        ProductConverter.convertToImageBase(image)).collect(Collectors.toList()));
            } else {
                log.error("未查询到产品图片，productCode = {}", request.getProductCode());
                return BaseResponse.withFail(CentralError.NO_RESULT_PRODUCT_IMAGE_LIST_ERROR);
            }
        } else if (StringUtils.isNotBlank(request.getProductItemCode())) {
            ProductItemPO productItemPO = productItemDao.getImagesByCode(request.getProductItemCode());
            if (productItemPO != null && ListUtils.isNotEmpty(productItemPO.getImages())) {
                return BaseResponse.withSuccess(productItemPO.getImages().stream().map(image ->
                        ProductConverter.convertToImageBase(image)).collect(Collectors.toList()));
            } else {
                log.error("未查询到项目图片，productItemCode = {}", request.getProductCode());
                return BaseResponse.withFail(CentralError.NO_RESULT_ITEM_IMAGE_LIST_ERROR);
            }
        }
        return BaseResponse.withFail(CentralError.ERROR_BAD_REQUEST);
    }

    @Override
    public BaseResponse<PriceCalcResult> calcTotalPriceV2(PriceCalcRequest request) {
        PriceCalcResult result = new PriceCalcResult();
        String supplierProductId = null;
        String channel = null;
        if(StringUtils.equals(request.getCategory(), "d_ss_ticket")){
            ScenicSpotProductMPO productMPO = scenicSpotProductDao.getProductById(request.getProductCode());
            supplierProductId = productMPO.getSupplierProductId();
            channel = productMPO.getChannel();
        } else if(StringUtils.equals(request.getCategory(), "group_tour")){
            GroupTourProductMPO productMPO = groupTourProductDao.getProductById(request.getProductCode());
            supplierProductId = productMPO.getSupplierProductId();
            channel = productMPO.getChannel();
        } else if(StringUtils.equals(request.getCategory(), "hotel_scenicSpot")) {
            HotelScenicSpotProductMPO productMPO = hotelScenicSpotProductDao.getProductById(request.getProductCode());
            supplierProductId = productMPO.getSupplierProductId();
            channel = productMPO.getChannel();
        }
        request.setChannelCode(channel);
        OrderManager orderManager = orderFactory.getOrderManager(channel);
        // 计算价格前先刷新
        orderManager.syncPriceV2(request.getProductCode(),
                supplierProductId,
                DateTimeUtil.formatDate(request.getStartDate()),
                request.getEndDate() == null ? null : DateTimeUtil.formatDate(request.getEndDate()),
                request.getTraceId());
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        int chdQuantity = request.getChdQuantity() == null ? 0 : request.getChdQuantity();
        if(StringUtils.equals(request.getCategory(), "d_ss_ticket")){
            ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(request.getPackageCode());
            IncreasePrice increasePrice = increasePrice(request, priceMPO.getSellPrice(), null, priceMPO.getStartDate());
            PriceInfo priceInfo = new PriceInfo();
            priceInfo.setSaleDate(priceMPO.getStartDate());
            priceInfo.setSalePrice(increasePrice.getPrices().get(0).getAdtSellPrice());
            priceInfo.setProductCode(priceMPO.getScenicSpotProductId());
            priceInfo.setSettlePrice(priceMPO.getSettlementPrice());
            priceInfo.setStock(priceMPO.getStock());
            checkPriceV2(Lists.newArrayList(priceInfo), DateTimeUtil.parseDate(priceMPO.getStartDate()), quantity, chdQuantity, result);
        } else if(StringUtils.equals(request.getCategory(), "group_tour")){
            GroupTourProductSetMealMPO setMealMPO = groupTourProductSetMealDao.getSetMealById(request.getPackageCode());
            List<PriceInfo> priceInfos = setMealMPO.getGroupTourPrices().stream().map(gp -> {
                IncreasePrice increasePrice = increasePrice(request, gp.getAdtSellPrice(), gp.getChdSellPrice(), gp.getDate());
                PriceInfo priceInfo = new PriceInfo();
                priceInfo.setSaleDate(gp.getDate());
                priceInfo.setSalePrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                priceInfo.setProductCode(setMealMPO.getId());
                priceInfo.setSettlePrice(gp.getAdtPrice());
                priceInfo.setStock(gp.getAdtStock());
                priceInfo.setChdSalePrice(increasePrice.getPrices().get(0).getChdSellPrice());
                priceInfo.setChdSettlePrice(gp.getChdPrice());
                priceInfo.setChdStock(gp.getChdStock());
                priceInfo.setRoomDiffPrice(gp.getDiffPrice());
                return priceInfo;
            }).collect(Collectors.toList());
            checkPriceV2(priceInfos, request.getStartDate(), quantity, chdQuantity, result);
        } else if(StringUtils.equals(request.getCategory(), "hotel_scenicSpot")){
//            HotelScenicSpotProductMPO productMPO = hotelScenicSpotProductDao.getProductById(request.getProductCode());
            HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicSpotProductSetMealDao.getSetMealById(request.getPackageCode());
            // 产品说不支持日期维度选多份产品，下面的逻辑就不用了。
//            // 晚数
//            int nightDiff = DateTimeUtil.getDateDiffDays(request.getEndDate(), request.getStartDate());
//            int baseNight = productMPO.getNight();
//            if (nightDiff % baseNight != 0) {
//                String msg = String.format("日期不符合购买标准，购买晚数应该为%s的整数倍，startDate=%s, endDate=%s",
//                        baseNight,
//                        DateTimeUtil.format(request.getStartDate(), DateTimeUtil.defaultDatePattern),
//                        DateTimeUtil.format(request.getEndDate(), DateTimeUtil.defaultDatePattern));
//                log.error(msg);
//                return BaseResponse.withFail(CentralError.PRICE_CALC_DATE_INVALID_ERROR.getCode(), msg);
//            }
//            // 日期维度的份数
//            int dayQuantity = nightDiff / baseNight;
//            // 总份数 = 日期维度的份数 * 购买数量的份数
//            int quantityTotal = dayQuantity * quantity;
//            if (quantityTotal > setMealMPO.getBuyMax() || quantityTotal < setMealMPO.getBuyMin()) {
//                String msg = String.format("数量不符合购买标准，最少购买%s份，最多购买%s份， quantity=%s", setMealMPO.getBuyMin(), setMealMPO.getBuyMax(), quantityTotal);
//                log.error(msg);
//                return BaseResponse.withFail(CentralError.PRICE_CALC_QUANTITY_LIMIT_ERROR.getCode(), msg);
//            }
            List<PriceInfo> priceInfos = setMealMPO.getPriceStocks().stream().map(ps -> {
                IncreasePrice increasePrice = increasePrice(request, ps.getAdtSellPrice(), ps.getChdSellPrice(), ps.getDate());
                PriceInfo priceInfo = new PriceInfo();
                priceInfo.setSaleDate(ps.getDate());
                priceInfo.setSalePrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                priceInfo.setProductCode(setMealMPO.getId());
                priceInfo.setSettlePrice(ps.getAdtPrice());
                priceInfo.setStock(ps.getAdtStock());
                priceInfo.setChdSalePrice(increasePrice.getPrices().get(0).getChdSellPrice());
                priceInfo.setChdSettlePrice(ps.getChdPrice());
                priceInfo.setChdStock(ps.getChdStock());
                priceInfo.setRoomDiffPrice(ps.getDiffPrice());
                return priceInfo;
            }).collect(Collectors.toList());
            checkPriceV2(priceInfos, request.getStartDate(), quantity, chdQuantity, result);
            // 不支持在日历中选多份，所以这里不需要了
//            for (int i = 0; i < dayQuantity; i++) {
//                // 日期维度中每份产品的起始日期 = 第一份起始日期 + 第n份 * 基准晚数
//                Date startDate = DateTimeUtil.addDay(request.getStartDate(), i * baseNight);
//                checkPriceV2(priceInfos, startDate, quantity, chdQuantity == null ? 0 : chdQuantity, result);
//            }
        }
        return BaseResponse.withSuccess(result);
    }

    private IncreasePrice increasePrice(PriceCalcRequest request, BigDecimal adtPrice, BigDecimal chdPrice, String date){
        IncreasePrice increasePrice = new IncreasePrice();
        increasePrice.setProductCode(request.getProductCode());
        increasePrice.setChannelCode(request.getChannelCode());
        increasePrice.setAppSource(request.getFrom());
        increasePrice.setProductCategory(request.getCategory());
        IncreasePriceCalendar calendar = new IncreasePriceCalendar();
        calendar.setAdtSellPrice(adtPrice);
        calendar.setChdSellPrice(chdPrice);
        calendar.setDate(date);
        increasePrice.setPrices(Lists.newArrayList(calendar));
        commonService.increasePrice(increasePrice);
        return increasePrice;
    }

    @Override
    public BaseResponse<PriceCalcResult> calcTotalPrice(PriceCalcRequest request) {
        PriceCalcResult result = new PriceCalcResult();
        ProductPO productPO = productDao.getTripProductByCode(request.getProductCode());
        if(productPO == null){
            return BaseResponse.withFail(CentralError.PRICE_CALC_PRODUCT_NOT_FOUND_ERROR);
        }
        // 计算价格前先刷新
        String channelCode = productPO.getSupplierId();
        OrderManager orderManager = orderFactory.getOrderManager(channelCode);
        orderManager.syncPrice(productPO.getCode(),
                productPO.getSupplierProductId(),
                DateTimeUtil.formatDate(request.getStartDate()),
                request.getEndDate() == null ? null : DateTimeUtil.formatDate(request.getEndDate()),
                request.getTraceId());
        PricePO pricePO = productDao.getPricePos(request.getProductCode());
        if(pricePO == null || ListUtils.isEmpty(pricePO.getPriceInfos())){
            return BaseResponse.withFail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        // 加价
        increasePrice(pricePO.getPriceInfos(), channelCode, productPO.getCode(), productPO.getPrice());

        int quantity = request.getQuantity();
        Integer chdQuantity = request.getChdQuantity();
        // 跟团游
        if(TRIP_PRODUCTS.contains(productPO.getProductType())){
            checkPrice(pricePO.getPriceInfos(), request.getStartDate(), quantity, chdQuantity == null ? 0 : chdQuantity, result);
        }
        // 含酒店
        else if(productPO.getProductType() == ProductType.FREE_TRIP.getCode()) {
            // 晚数
            int nightDiff = DateTimeUtil.getDateDiffDays(request.getEndDate(), request.getStartDate());
            int baseNight = productPO.getRoom().getRooms().get(0).getBaseNight();
            if (nightDiff % baseNight != 0) {
                String msg = String.format("日期不符合购买标准，购买晚数应该为%s的整数倍，startDate=%s, endDate=%s",
                        baseNight,
                        DateTimeUtil.format(request.getStartDate(), DateTimeUtil.defaultDatePattern),
                        DateTimeUtil.format(request.getEndDate(), DateTimeUtil.defaultDatePattern));
                log.error(msg);
                return BaseResponse.withFail(CentralError.PRICE_CALC_DATE_INVALID_ERROR.getCode(), msg);
            }
            // 日期维度的份数
            int dayQuantity = nightDiff / baseNight;
            // 总份数 = 日期维度的份数 * 购买数量的份数
            int quantityTotal = dayQuantity * quantity;
            if (quantityTotal > productPO.getBuyMax() || quantityTotal < productPO.getBuyMin()) {
                String msg = String.format("数量不符合购买标准，最少购买%s份，最多购买%s份， quantity=%s", productPO.getBuyMin(), productPO.getBuyMax(), quantityTotal);
                log.error(msg);
                return BaseResponse.withFail(CentralError.PRICE_CALC_QUANTITY_LIMIT_ERROR.getCode(), msg);
            }
            for (int i = 0; i < dayQuantity; i++) {
                // 日期维度中每份产品的起始日期 = 第一份起始日期 + 第n份 * 基准晚数
                Date startDate = DateTimeUtil.addDay(request.getStartDate(), i * baseNight);
                checkPrice(pricePO.getPriceInfos(), startDate, quantity, result);
            }
        } else { // 不含酒店
            checkPrice(pricePO.getPriceInfos(), request.getStartDate(), quantity, result);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public List<Product> getFlagRecommendProducts(Integer productType, int size){
        List<ProductPO> productPOs = productDao.getFlagRecommendResult_(productType, size);
        return productPOs.stream().map(p -> {
            Product product = new Product();
            BeanUtils.copyProperties(p, product);
            return product;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Product> getFlagRecommendProducts(int size){
        return getFlagRecommendProducts(null, size);
    }

    /**
     * 加价计算
     * @param priceInfos
     * @param channelCode
     * @param productCode
     */
    private void increasePrice(List<PriceInfoPO> priceInfos, String channelCode, String productCode, BigDecimal marketPrice){
        try {
            log.info("准备获取加价配置。。原始价格={}", JSON.toJSONString(priceInfos));
            SupplierPolicyPO supplierPolicy = supplierPolicyDao.getSupplierPolicyBySupplierId(channelCode);
            // 没有配置或者没有配置加价类型都不计算
            if(supplierPolicy != null && supplierPolicy.getPriceType() != null){
                log.info("获取到价格配置={}", JSONObject.toJSONString(supplierPolicy));
                // 如果配置了通用加价就用通用加价规则
                if(supplierPolicy.getPriceType() == Constants.SUPPLIER_POLICY_PRICE_COMMON){
                    supplierPolicy = supplierPolicyDao.getSupplierPolicyBySupplierId(Constants.SUPPLIER_CODE_COMMON);
                }
                ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");
                for (PriceInfoPO priceInfo : priceInfos) {
                    // 加价计算
                    if(priceInfo.getSettlePrice() != null){
                        BigDecimal newPrice = BigDecimal.valueOf((Double) se.eval(supplierPolicy.getPriceFormula().replace("price",
                                priceInfo.getSettlePrice().toPlainString()))).setScale(0, BigDecimal.ROUND_HALF_UP);
                        // 如果加价后价格超过门市价就用门市价
                        if(marketPrice != null && marketPrice.compareTo(newPrice) == 0){
                            priceInfo.setSalePrice(marketPrice);
                        } else {
                            priceInfo.setSalePrice(newPrice);
                        }
                    }
                    // 如果有儿童价也加价
                    if(priceInfo.getChdSettlePrice() != null){
                        String formula = supplierPolicy.getPriceFormula();
                        // 如果儿童单独配置了加价规则就用儿童的
                        if(StringUtils.isNotBlank(supplierPolicy.getChdPriceFormula())){
                            formula = supplierPolicy.getChdPriceFormula();
                        }
                        BigDecimal newPrice = BigDecimal.valueOf((Double) se.eval(formula.replace("price",
                                priceInfo.getChdSettlePrice().toPlainString()))).setScale(0, BigDecimal.ROUND_HALF_UP);;
                        // 如果加价后价格超过门市价就用门市价
                        if(marketPrice != null && marketPrice.compareTo(newPrice) == 0){
                            priceInfo.setChdSalePrice(marketPrice);
                        } else {
                            priceInfo.setChdSalePrice(newPrice);
                        }
                    }
                }
                log.info("加价完成，加价后价格={}", JSON.toJSONString(priceInfos));
            } else {
                log.info("没有获取到加价配置或者配置不完整，channel = {}", channelCode);
            }
        } catch (Exception e) {
            log.error("加价计算失败，不影响主流程，channel = {}, productCode = {}", channelCode, productCode, e);
        }
    }

    /**
     * 构建商品详情结果
     * @param productPOs
     * @param result
     */
    private void convertToCategoryDetailResult(List<ProductPO> productPOs, CategoryDetailResult result) {
        if (ListUtils.isEmpty(productPOs)) {
            log.info("没有查到商品详情");
            throw new HlCentralException(CentralError.NO_RESULT_DETAIL_LIST_ERROR);
        }
        result.setProducts(productPOs.stream().map(po -> {
            try {
                if(po.getPriceCalendar() != null && po.getPriceCalendar().getPriceInfos() != null){
                    increasePrice(Lists.newArrayList(po.getPriceCalendar().getPriceInfos()), po.getSupplierId(), po.getCode(),po.getPrice());
                }
                List<PriceInfoPO> priceInfos = Lists.newArrayList(po.getPriceCalendar().getPriceInfos());
                increasePrice(priceInfos, po.getSupplierId(), po.getCode(), null);
                po.getPriceCalendar().setPriceInfos(priceInfos.get(0));
                Product product = ProductConverter.convertToProduct(po, 0);
                // 设置主item，放在最外层，product里的去掉
                if (result.getMainItem() == null) {
                    ProductItem productItem = JSON.parseObject(JSON.toJSONString(product.getMainItem()), ProductItem.class);
                    if(productItem != null){
                        result.setMainItem(productItem);
                        if(StringUtils.isBlank(productItem.getAppMainTitle())){
                            productItem.setAppMainTitle(product.getName());
                        }
                        // todo 临时方案，后面有人审核这里要去掉
                        if(ListUtils.isEmpty(productItem.getFeatures())){
                            BackupProductItemPO backupProductItemPO = productItemDao.getBackupProductByCode(productItem.getCode());
                            if(backupProductItemPO != null){
                                ProductItemPO backupItem = JSON.parseObject(backupProductItemPO.getData(), ProductItemPO.class);
                                if(ListUtils.isNotEmpty(backupItem.getFeatures())){
                                    productItem.setFeatures(backupItem.getFeatures().stream().map(f -> {
                                        ItemFeature itemFeature = new ItemFeature();
                                        itemFeature.setDetail(f.getDetail());
                                        itemFeature.setName(f.getName());
                                        itemFeature.setType(f.getType());
                                        return itemFeature;
                                    }).collect(Collectors.toList()));
                                }
                            }
                        }
                        if(ListUtils.isNotEmpty(productItem.getImageDetails())){
                            if(ListUtils.isNotEmpty(productItem.getFeatures())){
                                productItem.getFeatures().removeIf(f -> f.getType() == 3);
                            }
                        }
                        if(ListUtils.isNotEmpty(productItem.getFeatures())){
                            productItem.getFeatures().forEach(f -> {
                                if(f.getType() == 1){
                                    f.setName("购买须知");
                                } else if(f.getType() == 2){
                                    f.setName("交通指南");
                                } else if(f.getType() == 3){
                                    f.setName("图文详情");
                                }else if(f.getType() == 4){
                                    f.setName("重要条款");
                                }else if(f.getType() == 5){
                                    f.setName("游玩须知");
                                }
                            });
                        }
                    }
                }
                product.setMainItem(null);
                if(ListUtils.isNotEmpty(product.getDescriptions())){
                    product.setDescription(null);
                }
                List<Description> bookDescList = Lists.newArrayList();
                if(StringUtils.isNotBlank(product.getRefundDesc())){
                    Description refundDesc = new Description();
                    refundDesc.setTitle("退改说明");
                    refundDesc.setContent(product.getRefundDesc());
                    bookDescList.add(refundDesc);
                }
                if(StringUtils.isNotBlank(product.getBookDesc())) {
                    Description bookDesc = new Description();
                    bookDesc.setTitle("预订须知");
                    bookDesc.setContent(product.getBookDesc());
                    bookDescList.add(bookDesc);
                }
                if(StringUtils.isNotBlank(product.getIncludeDesc())) {
                    Description feeInclude = new Description();
                    feeInclude.setTitle("费用包含");
                    feeInclude.setContent(product.getIncludeDesc());
                    bookDescList.add(feeInclude);
                }
                if(StringUtils.isNotBlank(product.getExcludeDesc())) {
                    Description feeExclude = new Description();
                    feeExclude.setTitle("自理费用");
                    feeExclude.setContent(product.getExcludeDesc());
                    bookDescList.add(feeExclude);
                }
                if(StringUtils.isNotBlank(product.getDiffPriceDesc())) {
                    Description feeExclude = new Description();
                    feeExclude.setTitle("差价说明");
                    feeExclude.setContent(product.getDiffPriceDesc());
                    bookDescList.add(feeExclude);
                }
                if(StringUtils.isNotBlank(product.getSuitDesc())) {
                    Description suitDesc = new Description();
                    suitDesc.setTitle("适用条件");
                    suitDesc.setContent(product.getSuitDesc());
                    bookDescList.add(suitDesc);
                }
                if(StringUtils.isNotBlank(product.getRemark())) {
                    Description remark = new Description();
                    remark.setTitle("其他说明");
                    remark.setContent(product.getRemark());
                    bookDescList.add(remark);
                }
                if(ListUtils.isNotEmpty(product.getBookDescList())){
                    bookDescList.addAll(product.getBookDescList());
                }
                product.setBookDescList(bookDescList);
                if(ListUtils.isNotEmpty(product.getBookNoticeList())){
                    product.getBookNoticeList().removeIf(b ->
                            StringUtils.isBlank(b.getContent()));
                }
                HodometerPO hodometerPO = hodometerDao.getHodometerByProductCode(po.getCode());
                if(hodometerPO != null){
                    product.setHodometer(hodometerPO);
                }
                return product;
            } catch (Exception e) {
                log.error("转换商品详情结果异常，po = {}", JSON.toJSONString(po), e);
                return null;
            }
        }).filter(po -> po != null).collect(Collectors.toList()));
    }

    /**
     * 构建商品列表
     * @param productPOs
     * @param total
     * @return
     */
    private List<Product> convertToProducts(List<ProductPO> productPOs, int total) {
        return productPOs.stream().map(po -> {
            try {
                if(po.getPriceCalendar() != null && po.getPriceCalendar().getPriceInfos() != null){
                    increasePrice(Lists.newArrayList(po.getPriceCalendar().getPriceInfos()), po.getSupplierId(), po.getCode(), po.getPrice());
                }
                return ProductConverter.convertToProduct(po, total);
            } catch (Exception e) {
                log.error("转换商品列表结果异常，po = {}", JSON.toJSONString(po), e);
                return null;
            }
        }).filter(po -> po != null).collect(Collectors.toList());
    }

    /**
     * 构建商品列表，根据item列表
     * @param productItemPOs
     * @param total
     * @return
     */
    private List<Product> convertToProductsByItem(List<ProductItemPO> productItemPOs, int total) {
        return productItemPOs.stream().map(po -> {
            try {
                if(po.getProduct() != null){
                    StopWatch stopWatch = new StopWatch();
                    if(po.getProduct() != null && po.getProduct().getPriceCalendar() != null
                            && po.getProduct().getPriceCalendar().getPriceInfos() != null){
                        stopWatch.start("计算加价");
                        increasePrice(Lists.newArrayList(po.getProduct().getPriceCalendar().getPriceInfos()),
                                po.getSupplierId(), po.getProduct().getCode(), po.getProduct().getPrice());
                        stopWatch.stop();
                    }
                    Product product = ProductConverter.convertToProductByItem(po, total);
//                    List<PriceSinglePO> prices = priceDao.selectByProductCode(po.getProduct().getCode(), 3);
//                    if(ListUtils.isNotEmpty(prices)){
//                        product.setGroupDates(prices.stream().map(p ->
//                                DateTimeUtil.format(p.getPriceInfos().getSaleDate(), "MM-dd")).collect(Collectors.toList()));
//                    }
                    stopWatch.start("设置团期");
                    PricePO pricePO = priceDao.selectPricePOByProductCode(po.getProduct().getCode());
                    if(pricePO != null && ListUtils.isNotEmpty(pricePO.getPriceInfos())) {
                        pricePO.getPriceInfos().removeIf(p ->
                                p.getSaleDate().getTime() < DateTimeUtil.trancateToDate(new Date()).getTime()
                                        || p.getStock() == null || p.getStock() <= 0
                                        || p.getSalePrice() == null || p.getSalePrice().compareTo(BigDecimal.valueOf(0)) < 1);
                        List<PriceInfoPO> newPriceInfos = pricePO.getPriceInfos().stream().sorted(Comparator.comparing(PriceInfoPO::getSaleDate)).collect(Collectors.toList());
                        if(newPriceInfos.size() > 3){
                            newPriceInfos = newPriceInfos.subList(0, 3);
                        }
                        List<PriceSinglePO> priceSinglePOs = newPriceInfos.stream().map(p -> {
                            PriceSinglePO priceSinglePO = new PriceSinglePO();
                            priceSinglePO.setPriceInfos(p);
                            return priceSinglePO;
                        }).collect(Collectors.toList());
                        product.setGroupDates(priceSinglePOs.stream().map(p ->
                                DateTimeUtil.format(p.getPriceInfos().getSaleDate(), "MM-dd")).collect(Collectors.toList()));
                    }
                    stopWatch.stop();
                    log.info("耗时统计：{}", stopWatch.prettyPrint());
                    return product;
                }
                return null;
            } catch (Exception e) {
                log.error("转换商品列表结果异常，po = {}", JSON.toJSONString(po), e);
                return null;
            }
        }).filter(po -> po != null).collect(Collectors.toList());
    }

    private void checkPrice(List<PriceInfoPO> priceInfoPOs, Date startDate,
                            int quantityTotal, PriceCalcResult result){
        checkPrice(priceInfoPOs, startDate, quantityTotal, 0, result);
    }
    /**
     * 检查价格
     * @param priceInfoPOs
     * @param startDate
     * @param quantityTotal
     * @param result
     */
    private void checkPrice(List<PriceInfoPO> priceInfoPOs, Date startDate,
                            int quantityTotal, int chdQuantityTotal, PriceCalcResult result){
        // 拿到当前日期价格信息
        PriceInfoPO priceInfoPO = priceInfoPOs.stream().filter(price -> price.getSaleDate().getTime() == startDate.getTime()).findFirst().orElse(null);
        String dateStr = DateTimeUtil.format(startDate, DateTimeUtil.defaultDatePattern);
        if (priceInfoPO == null) {
            String msg = String.format("%s的价格缺失", dateStr);
            log.error(msg);
            throw new HlCentralException(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR.getCode(), msg);
        }
        if (priceInfoPO.getStock() < (quantityTotal + chdQuantityTotal)) {
            String msg = String.format("库存不足，%s剩余库存=%s, 购买份数=%s", dateStr, priceInfoPO.getStock(), quantityTotal + chdQuantityTotal);
            log.error(msg);
            // 库存不足要返回具体库存
            result.setMinStock(priceInfoPO.getStock());
            throw new HlCentralException(CentralError.PRICE_CALC_STOCK_SHORT_ERROR.getCode(), msg, result);
        }
        double roomDiff = 0;
        if((quantityTotal) % 2 != 0 && priceInfoPO.getRoomDiffPrice() != null
                && priceInfoPO.getRoomDiffPrice().compareTo(BigDecimal.valueOf(0)) == 1){
            roomDiff = priceInfoPO.getRoomDiffPrice().doubleValue();
        }
        // 成人总价
        BigDecimal adtSalesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSalesTotal() == null ? 0d : result.getSalesTotal().doubleValue(),
                calcPrice(priceInfoPO.getSalePrice(), quantityTotal).doubleValue()));
        BigDecimal adtSettlesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSettlesTotal() == null ? 0d : result.getSettlesTotal().doubleValue(),
                calcPrice(priceInfoPO.getSettlePrice(), quantityTotal).doubleValue()));
        // 儿童总价
        BigDecimal chdSalesTotal = null;
        if(priceInfoPO.getChdSalePrice() != null){
            chdSalesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSalesTotal() == null ? 0d : result.getSalesTotal().doubleValue(),
                    calcPrice(priceInfoPO.getChdSalePrice(), chdQuantityTotal).doubleValue()));
        }
        BigDecimal chdSettlesTotal = null;
        if(priceInfoPO.getChdSettlePrice() != null){
            chdSettlesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSettlesTotal() == null ? 0d : result.getSettlesTotal().doubleValue(),
                    calcPrice(priceInfoPO.getChdSettlePrice(), chdQuantityTotal).doubleValue()));
        }
        result.setAdtSalePriceTotal(adtSalesTotal);
        result.setAdtSettlePriceTotal(adtSettlesTotal);
        result.setChdSalePriceTotal(chdSalesTotal);
        result.setChdSettlePriceTotal(chdSettlesTotal);
        // 总价
        result.setSalesTotal(BigDecimal.valueOf(BigDecimalUtil.add(adtSalesTotal.doubleValue(), chdSalesTotal == null ? 0d : chdSalesTotal.doubleValue(), roomDiff)));
        result.setSettlesTotal(BigDecimal.valueOf(BigDecimalUtil.add(adtSettlesTotal.doubleValue(), chdSettlesTotal == null ? 0d : chdSettlesTotal.doubleValue(), roomDiff)));
        result.setAdtSalesPrice(priceInfoPO.getSalePrice());
        result.setAdtSettlePrice(priceInfoPO.getSettlePrice());
        result.setChdSalesPrice(priceInfoPO.getChdSalePrice());
        result.setChdSettlePrice(priceInfoPO.getChdSettlePrice());
        result.setStock(priceInfoPO.getStock());
        result.setRoomDiffPrice(priceInfoPO.getRoomDiffPrice());
    }

    private void checkPriceV2(List<PriceInfo> priceInfos, Date startDate,
                            int quantityTotal, int chdQuantityTotal, PriceCalcResult result){
        // 拿到当前日期价格信息
        PriceInfo priceInfo = priceInfos.stream().filter(price ->
                DateTimeUtil.parseDate(price.getSaleDate()).getTime() == startDate.getTime()
                        && price.getSalePrice() != null).findFirst().orElse(null);
        String dateStr = DateTimeUtil.format(startDate, DateTimeUtil.defaultDatePattern);
        if (priceInfo == null) {
            String msg = String.format("%s的价格缺失", dateStr);
            log.error(msg);
            throw new HlCentralException(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR.getCode(), msg);
        }
        if (priceInfo.getStock() < (quantityTotal + chdQuantityTotal)) {
            String msg = String.format("库存不足，%s剩余库存=%s, 购买份数=%s", dateStr, priceInfo.getStock(), quantityTotal + chdQuantityTotal);
            log.error(msg);
            // 库存不足要返回具体库存
            result.setMinStock(priceInfo.getStock());
            throw new HlCentralException(CentralError.PRICE_CALC_STOCK_SHORT_ERROR.getCode(), msg, result);
        }
        double roomDiff = 0;
        if((quantityTotal) % 2 != 0 && priceInfo.getRoomDiffPrice() != null
                && priceInfo.getRoomDiffPrice().compareTo(BigDecimal.valueOf(0)) == 1){
            roomDiff = priceInfo.getRoomDiffPrice().doubleValue();
        }
        // 成人总价
        BigDecimal adtSalesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSalesTotal() == null ? 0d : result.getSalesTotal().doubleValue(),
                calcPrice(priceInfo.getSalePrice(), quantityTotal).doubleValue()));
        BigDecimal adtSettlesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSettlesTotal() == null ? 0d : result.getSettlesTotal().doubleValue(),
                calcPrice(priceInfo.getSettlePrice(), quantityTotal).doubleValue()));
        // 儿童总价
        BigDecimal chdSalesTotal = null;
        if(priceInfo.getChdSalePrice() != null){
            chdSalesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSalesTotal() == null ? 0d : result.getSalesTotal().doubleValue(),
                    calcPrice(priceInfo.getChdSalePrice(), chdQuantityTotal).doubleValue()));
        }
        BigDecimal chdSettlesTotal = null;
        if(priceInfo.getChdSettlePrice() != null){
            chdSettlesTotal = BigDecimal.valueOf(BigDecimalUtil.add(result.getSettlesTotal() == null ? 0d : result.getSettlesTotal().doubleValue(),
                    calcPrice(priceInfo.getChdSettlePrice(), chdQuantityTotal).doubleValue()));
        }
        result.setAdtSalePriceTotal(adtSalesTotal);
        result.setAdtSettlePriceTotal(adtSettlesTotal);
        result.setChdSalePriceTotal(chdSalesTotal);
        result.setChdSettlePriceTotal(chdSettlesTotal);
        // 总价
        result.setSalesTotal(BigDecimal.valueOf(BigDecimalUtil.add(adtSalesTotal.doubleValue(), chdSalesTotal == null ? 0d : chdSalesTotal.doubleValue(), roomDiff)));
        result.setSettlesTotal(BigDecimal.valueOf(BigDecimalUtil.add(adtSettlesTotal.doubleValue(), chdSettlesTotal == null ? 0d : chdSettlesTotal.doubleValue(), roomDiff)));
        result.setAdtSalesPrice(priceInfo.getSalePrice());
        result.setAdtSettlePrice(priceInfo.getSettlePrice());
        result.setChdSalesPrice(priceInfo.getChdSalePrice());
        result.setChdSettlePrice(priceInfo.getChdSettlePrice());
        result.setStock(priceInfo.getStock());
        result.setRoomDiffPrice(priceInfo.getRoomDiffPrice());
    }

    /**
     * 计算价格
     * @param price
     * @param quantity
     * @return
     */
    private BigDecimal calcPrice(BigDecimal price, int quantity) {
        BigDecimal total = BigDecimal.valueOf(BigDecimalUtil.mul(price.doubleValue(), quantity));
        return total;
    }

    /**
     * 获取缓存的推荐列表
     * @param products
     * @param type
     */
    private void getFlagRecommend(List<Product> products, Integer type){
        try {
            String key = String.join("", RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX, type.toString());
            if(jedisTemplate.hasKey(key)){
                List<Product> list = JSONArray.parseArray(jedisTemplate.opsForValue().get(key).toString(), Product.class);
                if(ListUtils.isNotEmpty(list)){
                    products.addAll(list);
                    return;
                }
            }
            if(ProductType.FREE_TRIP.getCode() == type.intValue()){
                recommendTask.refreshRecommendList(1);
                ProductPageRequest request = new ProductPageRequest();
                request.setType(type);
                request.setPageSize(4);
                BaseResponse<ProductPageResult> response = pageList(request);
                if(response.getData() != null && response.getData().getProducts() != null){
                    products.addAll(response.getData().getProducts());
                }
            }
        } catch (Exception e) {
            log.error("获取标记推荐列表异常", e);
        }
    }

    private RecommendProductV2 convertToRecommendProductV2(RecommendBaseInfo rb, String position){
        RecommendProductV2 recommendProduct = new RecommendProductV2();
        recommendProduct.setPoiId(rb.getPoiId());
        recommendProduct.setPoiName(rb.getPoiName());
        recommendProduct.setProductId(rb.getProductId());
        recommendProduct.setProductName(rb.getProductName());
        recommendProduct.setChannel(rb.getChannel());
        recommendProduct.setChannelName(rb.getChannelName());
        IncreasePrice increasePrice = new IncreasePrice();
        increasePrice.setProductCode(rb.getProductId());
        increasePrice.setChannelCode(rb.getChannel());
        IncreasePriceCalendar calendar = new IncreasePriceCalendar();
        calendar.setAdtSellPrice(rb.getApiSettlementPrice());
        increasePrice.setPrices(Lists.newArrayList(calendar));
        commonService.increasePrice(increasePrice);
        recommendProduct.setPrice(calendar.getAdtSellPrice());
        recommendProduct.setImage(rb.getMainImage());
        recommendProduct.setPosition(Integer.valueOf(position));
        recommendProduct.setCategory(rb.getCategory());
        recommendProduct.setBookDay(rb.getBookDay());
        recommendProduct.setRecommendDesc(rb.getRecommendDesc());
        recommendProduct.setSubTitle(rb.getSubTitle());
        recommendProduct.setTags(rb.getTags());
        recommendProduct.setSeq(rb.getSeq());
        return recommendProduct;
    }
}
