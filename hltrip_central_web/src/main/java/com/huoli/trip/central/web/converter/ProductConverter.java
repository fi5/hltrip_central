package com.huoli.trip.central.web.converter;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;

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
public class ProductConverter {

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
        product.setMainItem(convertToProductItem(po.getMainItem()));
        product.setTotal(total);
        product.setPriceInfo(convertToPriceInfo(po.getPriceCalendar()));
        if(product.getPriceInfo() != null){
            product.setSalePrice(product.getPriceInfo().getSalePrice());
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
        PriceInfo priceInfo = new PriceInfo();
        priceInfo.setPriceType(priceSinglePO.getPriceInfos().getPriceType());
        priceInfo.setProductCode(priceSinglePO.getProductCode());
        priceInfo.setSaleDate(DateTimeUtil.formatDate(priceSinglePO.getPriceInfos().getSaleDate(), DateTimeUtil.YYYYMMDD));
        priceInfo.setSalePrice(priceSinglePO.getPriceInfos().getSalePrice());
        priceInfo.setSettlePrice(priceSinglePO.getPriceInfos().getSettlePrice());
        priceInfo.setStock(priceSinglePO.getPriceInfos().getStock());
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
            types = Lists.newArrayList(ProductType.FREE_TRIP.getCode(), ProductType.RESTAURANT.getCode(), ProductType.SCENIC_TICKET.getCode(), ProductType.SCENIC_TICKET_PLUS.getCode());
        } else if (type == ProductType.SCENIC_TICKET_PLUS.getCode()) {  // 门票加需要查门票和门票+
            types = Lists.newArrayList(ProductType.SCENIC_TICKET_PLUS.getCode(), ProductType.SCENIC_TICKET.getCode());
        } else {  // 其它类型就按传进来的查
            types = Lists.newArrayList(type);
        }
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
