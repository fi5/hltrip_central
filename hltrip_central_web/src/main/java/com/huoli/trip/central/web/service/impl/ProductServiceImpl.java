package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.*;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.task.RecommendTask;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.BigDecimalUtil;
import com.huoli.trip.common.util.CommonUtils;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import com.huoli.trip.common.vo.request.central.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.huoli.trip.central.web.constant.Constants.RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX;

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

    @Override
    public BaseResponse<ProductPageResult> pageListForProduct(ProductPageRequest request) {
        ProductPageResult result = new ProductPageResult();
        List<Integer> types = ProductConverter.getTypes(request.getType());
        List<Product> products = Lists.newArrayList();
        result.setProducts(products);
        for (Integer t : types) {
            int total = productDao.getPageListTotal(request.getCity(), t, request.getKeyWord());
            List<ProductPO> productPOs = productDao.getPageListProduct(request.getCity(), t, request.getKeyWord(), request.getPageIndex(), request.getPageSize());
            if (ListUtils.isNotEmpty(productPOs)) {
                products.addAll(convertToProducts(productPOs, total));
            }
        }
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
        for (Integer t : types) {
            long total = productDao.getPageListForItemTotal(request.getOriCity(), request.getCity(), t, request.getKeyWord(), request.getAppFrom());
            List<ProductItemPO> productItemPOs = productDao.getPageListForItem(request.getOriCity(), request.getCity(), t, request.getKeyWord(), request.getAppFrom(), request.getPageIndex(), request.getPageSize());
            if (ListUtils.isNotEmpty(productItemPOs)) {
                products.addAll(convertToProductsByItem(productItemPOs, (int)total));
            }
        }
        if(ListUtils.isEmpty(products)){
            return BaseResponse.withFail(CentralError.NO_RESULT_PRODUCT_LIST_ERROR);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request) {
        CategoryDetailResult result = new CategoryDetailResult();
        List<ProductPO> productPOs = productDao.getProductListByItemId(request.getProductItemId(), request.getSaleDate());
        convertToCategoryDetailResult(productPOs, result);
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
                if(StringUtils.isNotBlank(productPriceReq.getEndDate())){
                    if(saleDate.compareTo(productPriceReq.getEndDate())>0)
                        continue;
                }
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
                Product product = ProductConverter.convertToProduct(po, 0);
                // 设置主item，放在最外层，product里的去掉
                if (result.getMainItem() == null) {
                    ProductItem productItem = JSON.parseObject(JSON.toJSONString(product.getMainItem()), ProductItem.class);
                    if(productItem != null){
                        List<ImageBase> imageBases = productItem.getImageDetails();
                        List<ImageBase> imageBases1 =  productItem.getImages();
                        if(ListUtils.isNotEmpty(imageBases)){
                            if(imageBases1 == null){
                                productItem.setImages(imageBases);
                            }else{
                                imageBases1.addAll(imageBases);
                                productItem.setImages(imageBases1);
                            }
                        }
                        result.setMainItem(productItem);
                        if(StringUtils.isBlank(productItem.getAppMainTitle())){
                            productItem.setAppMainTitle(product.getName());
                        }
                        if(ListUtils.isNotEmpty(productItem.getImageDetails())){
                            if(ListUtils.isNotEmpty(productItem.getFeatures())){
                                productItem.getFeatures().removeIf(f -> f.getType() == 3);
                            }
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
                if(StringUtils.isNotBlank(product.getRefundDesc())) {
                    Description bookDesc = new Description();
                    bookDesc.setTitle("预订须知");
                    bookDesc.setContent(product.getBookDesc());
                    bookDescList.add(bookDesc);
                }
                if(StringUtils.isNotBlank(product.getRefundDesc())) {
                    Description feeInclude = new Description();
                    feeInclude.setTitle("费用包含");
                    feeInclude.setContent(product.getRefundDesc());
                    bookDescList.add(feeInclude);
                }
                if(StringUtils.isNotBlank(product.getRefundDesc())) {
                    Description feeExclude = new Description();
                    feeExclude.setTitle("自理费用");
                    feeExclude.setContent(product.getExcludeDesc());
                    bookDescList.add(feeExclude);
                }
                if(StringUtils.isNotBlank(product.getRefundDesc())) {
                    Description suitDesc = new Description();
                    suitDesc.setTitle("适用条件");
                    suitDesc.setContent(product.getSuitDesc());
                    bookDescList.add(suitDesc);
                }
                if(StringUtils.isNotBlank(product.getRefundDesc())) {
                    Description remark = new Description();
                    remark.setTitle("其他说明");
                    remark.setContent(product.getRemark());
                    bookDescList.add(remark);
                }
                bookDescList.addAll(product.getBookDescList());
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
                    if(po.getProduct() != null && po.getProduct().getPriceCalendar() != null
                            && po.getProduct().getPriceCalendar().getPriceInfos() != null){
                        increasePrice(Lists.newArrayList(po.getProduct().getPriceCalendar().getPriceInfos()),
                                po.getSupplierId(), po.getProduct().getCode(), po.getProduct().getPrice());
                    }
                    Product product = ProductConverter.convertToProductByItem(po, total);
                    List<PriceSinglePO> prices = priceDao.selectByProductCode(po.getProduct().getCode(), 3);
                    if(ListUtils.isNotEmpty(prices)){
                        product.setGroupDates(prices.stream().map(p ->
                                DateTimeUtil.format(p.getPriceInfos().getSaleDate(), "MM-dd")).collect(Collectors.toList()));
                    }
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
            String msg = String.format("库存不足，%s剩余库存=%s, 购买份数=%s", dateStr, priceInfoPO.getStock(), quantityTotal);
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
}
