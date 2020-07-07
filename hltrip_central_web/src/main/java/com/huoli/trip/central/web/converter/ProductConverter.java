package com.huoli.trip.central.web.converter;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.common.entity.ImageBasePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.entity.RoomInfoPO;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.*;

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
        return product;
    }

    /**
     * 转成 ResourceRoom vo
     * @param roomInfoPO
     * @return
     */
    public static ResourceRoom convertToResourceRoom(RoomInfoPO roomInfoPO){
        ResourceRoom resourceRoom = JSON.parseObject(JSON.toJSONString(roomInfoPO), ResourceRoom.class);
        if(roomInfoPO.getEarliestTime() != null){
            resourceRoom.setEarliestTime(DateTimeUtil.formatDate(roomInfoPO.getEarliestTime(), DateTimeUtil.YYYYMMDD));
        }
        if(roomInfoPO.getLatestTime() != null){
            resourceRoom.setLatestTime(DateTimeUtil.formatDate(roomInfoPO.getLatestTime(), DateTimeUtil.YYYYMMDD));
        }
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
            productItem.setItemCoordinate(coordinate);
        }
        return productItem;
    }

    public static ImageBase convertToImageBase(ImageBasePO imageBasePO){
        ImageBase imageBase = new ImageBase();
        imageBase.setDesc(imageBasePO.getDesc());
        imageBase.setUrl(imageBasePO.getUrl());
        return imageBase;
    }
}
