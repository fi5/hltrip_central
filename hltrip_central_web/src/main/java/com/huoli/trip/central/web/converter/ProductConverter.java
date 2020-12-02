package com.huoli.trip.central.web.converter;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/7<br>
 */
@Slf4j
public class ProductConverter {

    /**
     * 转成product vo
     * @param productPOs
     * @param total
     * @return
     */
    public static List<Product> convertToProducts(List<ProductPO> productPOs, int total) {
        return productPOs.stream().map(po -> {
            try {
                return ProductConverter.convertToProduct(po, total);
            } catch (Exception e) {
                log.error("转换商品列表结果异常，po = {}", JSON.toJSONString(po), e);
                return null;
            }
        }).filter(po -> po != null).collect(Collectors.toList());
    }

    /**
     * 转成product vo
     * @param po
     * @return
     */
    public static Product convertToProduct(ProductPO po){
        return convertToProduct(po, 0);
    }

    /**
     * 转成product vo
     * @param po
     * @param total
     * @return
     */
    public static Product convertToProduct(ProductPO po, int total){
        Product product = JSON.parseObject(JSON.toJSONString(po), Product.class);
        if(po.getValidTime() != null){
            product.setValidTime(DateTimeUtil.formatDate(po.getValidTime(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getInvalidTime() != null){
            product.setInvalidTime(DateTimeUtil.formatDate(po.getInvalidTime(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getDisplayStart() != null){
            product.setDisplayStart(DateTimeUtil.formatDate(po.getDisplayStart(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getDisplayEnd() != null){
            product.setDisplayEnd(DateTimeUtil.formatDate(po.getDisplayEnd(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getPreSaleStart() != null){
            product.setPreSaleStart(DateTimeUtil.formatDate(po.getPreSaleStart(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getPreSaleEnd() != null){
            product.setPreSaleEnd(DateTimeUtil.formatDate(po.getPreSaleEnd(), DateTimeUtil.YYYYMMDD));
        }
        if(po.getRoom() != null && ListUtils.isNotEmpty(po.getRoom().getRooms())){
            po.getRoom().getRooms().stream().map(roomInfoPO -> convertToResourceRoom(roomInfoPO)).collect(Collectors.toList());
        }
        if(po.getTicket() != null && ListUtils.isNotEmpty(po.getTicket().getTickets())){
            po.getTicket().getTickets().stream().map(ticketInfoPO -> convertToResourceTicket(ticketInfoPO)).collect(Collectors.toList());
        }
        if(po.getFood() != null && ListUtils.isNotEmpty(po.getFood().getFoods())){
            po.getFood().getFoods().stream().map(foodInfoPO -> convertToResourceFood(foodInfoPO)).collect(Collectors.toList());
        }
        product.setMainItem(convertToProductItem(po.getMainItem()));
        product.setTotal(total);
        product.setPriceInfo(convertToPriceInfo(po.getPriceCalendar()));
        if(product.getPriceInfo() != null){
            product.setSalePrice(product.getPriceInfo().getSalePrice());
        }
        return product;
    }

    /**
     * 把item列表对象转成product
     * @param po
     * @param total
     * @return
     */
    public static Product convertToProductByItem(ProductItemPO po, int total){
        Product product = JSON.parseObject(JSON.toJSONString(po.getProduct()), Product.class);
        product.setMainItem(convertToProductItem(po));
        product.setTotal(total);
        product.setPriceInfo(convertToPriceInfo(po.getProduct().getPriceCalendar()));
        product.setOriCity(po.getOriCity());
        product.setCity(po.getCity());
        if(product.getPriceInfo() != null){
            product.setSalePrice(product.getPriceInfo().getSalePrice());
        }
        if(ListUtils.isEmpty(product.getImages()) && ListUtils.isNotEmpty(po.getMainImages())){
            product.setImages(po.getMainImages().stream().map(i -> convertToImageBase(i)).collect(Collectors.toList()));
        }
        return product;
    }

    /**
     * 转成 ResourceRoom vo
     * @param roomInfoPO
     * @return
     */
    public static ResourceRoom convertToResourceRoom(RoomInfoPO roomInfoPO){
        ResourceRoom resourceRoom = JSON.parseObject(JSON.toJSONString(roomInfoPO), ResourceRoom.class);
        return resourceRoom;
    }

    /**
     * 转成 ResourceTicket vo
     * @param ticketInfoPO
     * @return
     */
    public static ResourceTicket convertToResourceTicket(TicketInfoPO ticketInfoPO){
        ResourceTicket resourceTicket = JSON.parseObject(JSON.toJSONString(ticketInfoPO), ResourceTicket.class);
        return resourceTicket;
    }

    /**
     * 转成 ResourceFood vo
     * @param foodInfoPO
     * @return
     */
    public static ResourceFood convertToResourceFood(FoodInfoPO foodInfoPO){
        ResourceFood resourceFood = JSON.parseObject(JSON.toJSONString(foodInfoPO), ResourceFood.class);
        return resourceFood;
    }

    /**
     * 转成ProductItem vo
     * @param productItemPO
     * @return
     */
    public static ProductItem convertToProductItem(ProductItemPO productItemPO){
        if(productItemPO == null){
            return null;
        }
        ProductItem productItem = JSON.parseObject(JSON.toJSONString(productItemPO), ProductItem.class);
        Double[] coordinateArr = productItemPO.getItemCoordinate();
        if(coordinateArr != null && coordinateArr.length == 2){
            Coordinate coordinate = new Coordinate();
            coordinate.setLongitude(coordinateArr[0]);
            coordinate.setLatitude(coordinateArr[1]);
            productItem.setCoordinate(coordinate);
        }
        return productItem;
    }

    /**
     * 转成 ImageBase vo
     * @param imageBasePO
     * @return
     */
    public static ImageBase convertToImageBase(ImageBasePO imageBasePO){
        ImageBase imageBase = new ImageBase();
        imageBase.setDesc(imageBasePO.getDesc());
        imageBase.setUrl(imageBasePO.getUrl());
        return imageBase;
    }

    /**
     * 转成PriceInfo vo
     * @param priceSinglePO
     * @return
     */
    public static PriceInfo convertToPriceInfo(PriceSinglePO priceSinglePO){
        if(priceSinglePO ==null || priceSinglePO.getPriceInfos() == null){
            return null;
        }
        PriceInfo priceInfo = new PriceInfo();
        PriceInfoPO priceInfoPO = priceSinglePO.getPriceInfos();
        priceInfo.setPriceType(priceInfoPO.getPriceType());
        priceInfo.setProductCode(priceSinglePO.getProductCode());
        priceInfo.setSaleDate(DateTimeUtil.formatDate(priceInfoPO.getSaleDate(), DateTimeUtil.YYYYMMDD));
        priceInfo.setSalePrice(priceInfoPO.getSalePrice());
        priceInfo.setSettlePrice(priceInfoPO.getSettlePrice());
        priceInfo.setStock(priceInfoPO.getStock());
        priceInfo.setSupplierPriceId(priceSinglePO.getSupplierProductId());
        return priceInfo;
    }

    /**
     * 根据前台类型转换对应数据库类型
     * @param type
     * @return
     */
    public static List<Integer> getTypes(int type) {
        List<Integer> types;
        // 不限需要查所有类型
        if (type == ProductType.UN_LIMIT.getCode()) {
            types = Arrays.asList(ProductType.values()).stream().map(t -> t.getCode()).filter(t ->
                    t != ProductType.UN_LIMIT.getCode()).collect(Collectors.toList());
        } else if (type == ProductType.SCENIC_TICKET_PLUS.getCode()) {  // 门票加需要查门票和门票+
            types = Lists.newArrayList(ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(type);
        }
        return types;
    }

    /**
     * 推荐列表查询类型
     * @param type
     * @return
     */
    public static List<Integer> getRecommendTypes(int type) {
        List<Integer> types;
        List<Integer> all = Arrays.asList(ProductType.values()).stream().map(t -> t.getCode()).collect(Collectors.toList());
        // 门票+转成两个类型
        if (type == ProductType.SCENIC_TICKET_PLUS.getCode()) {  // 门票加需要查门票和门票+
            types = Lists.newArrayList(ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(type);
        }
        // 合并去重，优先按传进来的查
        types.addAll(all);
        types = types.stream().filter(t -> t != ProductType.UN_LIMIT.getCode()).distinct().collect(Collectors.toList());
        return types;
    }

    /**
     * 把product type转成productitem type
     * @param productType
     * @return
     */
    public static int getItemType(int productType){
        int itemType;
        if(productType == ProductType.SCENIC_TICKET_PLUS.getCode() || productType == ProductType.SCENIC_TICKET.getCode()){
            itemType = Constants.PRODUCT_ITEM_TYPE_TICKET;
        } else if(productType == ProductType.RESTAURANT.getCode()){
            itemType = Constants.PRODUCT_ITEM_TYPE_FOOD;
        } else {
            itemType = Constants.PRODUCT_ITEM_TYPE_HOTEL;
        }
        return itemType;
    }
}
