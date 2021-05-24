package com.huoli.trip.central.web.converter;

import com.aliyuncs.kms.transform.v20160120.ListResourceTagsResponseUnmarshaller;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.CreateOrderReq;
import com.huoli.trip.common.vo.response.order.CenterCreateOrderRes;
import com.huoli.trip.supplier.self.lvmama.vo.*;
import com.huoli.trip.supplier.self.lvmama.vo.request.CreateOrderRequest;
import com.huoli.trip.supplier.self.lvmama.vo.request.ValidateOrderRequest;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookGuest;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCreateOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCreateOrderRes;
import javafx.beans.binding.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.ref.ReferenceQueue;
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
        if(req==null){
            return null;
        }
        YcfCreateOrderReq ycfCreateOrderReq = new YcfCreateOrderReq();
        ycfCreateOrderReq.setCname(req.getCname());
        ycfCreateOrderReq.setCredential(req.getCredential());
        ycfCreateOrderReq.setCredentialType(req.getCredentialType());
        ycfCreateOrderReq.setEmail(req.getEmail());
        ycfCreateOrderReq.setEname(req.getEname());
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
        ycfCreateOrderReq.setTraceId(req.getTraceId());
        return ycfCreateOrderReq;
    }

    @Override
    public CenterCreateOrderRes convertSupplierResponseToResponse(YcfCreateOrderRes supplierResponse) {
        if(supplierResponse==null){
            return null;
        }
        CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
        createOrderRes.setOrderId(supplierResponse.getOrderId());
        createOrderRes.setOrderStatus(OrderInfoTranser.genCommonOrderStatus(supplierResponse.getOrderStatus(),1));
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

    public void convertLvmamaCreateOrderRequest(CreateOrderRequest request,CreateOrderReq req){
        Booker booker = new Booker(req.getcName(),req.getMobile(),req.geteName());
        request.setBooker(booker);
        //需要场次号
        OrderInfo orderInfo = new OrderInfo(null,String.valueOf(req.getSellAmount()),null);

        Product product = new Product();
        product.setGoodsId(Long.parseLong(req.getGoodsId()));
        product.setProductId(Long.parseLong(req.getProductId()));
        product.setQuantity(req.getQunatity());
        product.setSellPrice(Float.parseFloat(req.getSellPrice()));
        product.setVisitDate(req.getBeginDate());
        orderInfo.setProduct(product);
        final List<CreateOrderReq.BookGuest> guests = req.getGuests();
        if(ListUtils.isNotEmpty(guests)){
            List<Traveller> traveller = new ArrayList<>(guests.size());
            for(CreateOrderReq.BookGuest guest :guests){
                final String s = convertLvmamaCredentialsType(guest.getCredentialType());
                if(StringUtils.isEmpty(s)){
                    //抛出不支持的证件类型
                }
                Traveller traveller1 = new Traveller(guest.getCname(),guest.getMobile(),guest.getEname(),guest.getEmail(),guest.getCredential(),null,s);
                traveller.add(traveller1);
            }
            request.setTraveller(traveller);
        }
        request.setOrderInfo(orderInfo);
        //邮寄信息
       /* Recipient recipient = new Recipient();
        request.setRecipient(recipient);*/
    }

    public void convertLvmamaBookOrderRequest(ValidateOrderRequest request, BookCheckReq req){
        Booker booker = new Booker(req.getChinaName(),req.getMobile(),req.getEmail());
        request.setBooker(booker);
        //需要场次号
        OrderInfo orderInfo = new OrderInfo(null,String.valueOf(req.getSellAmount()),null);

        Product product = new Product();
        product.setGoodsId(Long.parseLong(req.getGoodsId()));
        product.setProductId(Long.parseLong(req.getProductId().split("_")[1]));
        product.setQuantity(req.getCount());
        product.setSellPrice(Float.parseFloat(req.getSellPrice()));
        product.setVisitDate(req.getBeginDate());
        orderInfo.setProduct(product);

        final List<CreateOrderReq.BookGuest> guests = req.getGuests();
        if(ListUtils.isNotEmpty(guests)){
            List<Traveller> traveller = new ArrayList<>(guests.size());
            for(CreateOrderReq.BookGuest guest :guests){
                final String s = convertLvmamaCredentialsType(guest.getCredentialType());
                if(StringUtils.isEmpty(s)){
                    //抛出不支持的证件类型
                }
                Traveller traveller1 = new Traveller(guest.getCname(),guest.getMobile(),guest.getEname(),guest.getEmail(),guest.getCredential(),null,s);
                traveller.add(traveller1);
            }
            request.setTraveller(traveller);
        }

        request.setOrderInfo(orderInfo);
        //邮寄信息
       /* Recipient recipient = new Recipient();
        request.setRecipient(recipient);*/
    }

    private String convertLvmamaCredentialsType(int type){
        switch (type){
            case 0:
                return "ID_CARD";
            case 1:
                return "HUZHAO";
            case 2:
                return "GANGAO";
            case 3:
                return "TAIBAO";
            default:
                return "";
        }
    }

    public int convertLvmamaOrderStatus(String status){
        switch (status){
            case "NORMAL": //下单成功 和 待支付都是待支付状态
            case "UNPAY":
                return OrderStatus.TO_BE_PAID.getCode();
            case "CANCEL":
                return OrderStatus.CANCELLED.getCode();
            case "PAYED":
                return OrderStatus.WAITING_TO_TRAVEL.getCode();
            case "PARTPAY":
            return OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode();
            default:
                return -1;
        }
    }
}
