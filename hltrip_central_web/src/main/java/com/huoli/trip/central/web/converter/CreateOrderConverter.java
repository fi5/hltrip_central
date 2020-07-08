package com.huoli.trip.central.web.converter;

import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.vo.request.CreateOrderReq;
import com.huoli.trip.common.vo.response.order.CenterCreateOrderRes;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookGuest;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCreateOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCreateOrderRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述: <br> 创建订单业务实体转换
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
@Component
@Slf4j
public class  CreateOrderConverter implements Converter<CreateOrderReq, YcfCreateOrderReq,YcfCreateOrderRes,CenterCreateOrderRes> {
    @Override
    public YcfCreateOrderReq convertRequestToSupplierRequest(CreateOrderReq req) {
        YcfCreateOrderReq ycfCreateOrderReq = new YcfCreateOrderReq();
        ycfCreateOrderReq.setCName(req.getCName());
        ycfCreateOrderReq.setCredential(req.getCredential());
        ycfCreateOrderReq.setCredentialType(req.getCredentialType());
        ycfCreateOrderReq.setEmail(req.getEmail());
        ycfCreateOrderReq.setEName(req.getEName());
        ycfCreateOrderReq.setGuests(converterGuestList(req.getGuests()));
        ycfCreateOrderReq.setMobile(req.getMobile());
        ycfCreateOrderReq.setPartnerOrderId(req.getPartnerOrderId());
        //产品编码转供应商需要的格式
        ycfCreateOrderReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        ycfCreateOrderReq.setProductName(req.getProductName());
        ycfCreateOrderReq.setQunatity(req.getQunatity());
        ycfCreateOrderReq.setRemark(req.getRemark());
        //总的销售价
        ycfCreateOrderReq.setSellAmount(req.getSellAmount());
        return ycfCreateOrderReq;
    }

    @Override
    public CenterCreateOrderRes convertSupplierResponseToResponse(YcfCreateOrderRes supplierResponse) {
        CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
        createOrderRes.setOrderId(supplierResponse.getOrderId());
        createOrderRes.setOrderStatus(supplierResponse.getOrderStatus());
        return createOrderRes;
    }

    /**
     * 转换联系人
     * @param bookGuest
     * @return
     */
    public YcfBookGuest converterGuest(CreateOrderReq.BookGuest bookGuest){
        if(bookGuest==null){
            return null;
        }
        YcfBookGuest ycfBookGuest = new YcfBookGuest();
        try{
            BeanUtils.copyProperties(bookGuest,ycfBookGuest);
        }catch (Exception e){
            log.error("创建订单联系人对象转换异常",e);
        }
        return ycfBookGuest;
    }
    /**
     * 转换联系人list
     * @param bookGuestList
     * @return
     */
    public List<YcfBookGuest> converterGuestList(List<CreateOrderReq.BookGuest> bookGuestList){
        if(CollectionUtils.isEmpty(bookGuestList)){
            return null;
        }
        List<YcfBookGuest> ycfBookGuests = new ArrayList<>();
        bookGuestList.forEach(bookGuest -> {
            ycfBookGuests.add(converterGuest(bookGuest));
        });
        return ycfBookGuests;
    }
}
