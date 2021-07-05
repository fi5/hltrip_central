package com.huoli.trip.central.web.converter;

import com.huoli.eagle.eye.core.util.StringUtil;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.util.BigDecimalUtil;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
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

    public void convertLvmamaCreateOrderRequest(CreateOrderRequest request, CreateOrderReq req, ScenicSpotProductMPO scenicSpotProductMPO, List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS){
        //需要场次号
        Integer adultNum = req.getAdultNum();
        Integer childNum = req.getChildNum();
        int count = 0;
        if(adultNum != null){
            count = adultNum;
        }
        if(childNum != null) {
            count = count +childNum.intValue();
        }
        String sellPrice = req.getSellPrice();
        BigDecimal multiply = new BigDecimal(sellPrice).multiply(new BigDecimal(count));
        OrderInfo orderInfo = new OrderInfo(req.getPartnerOrderId(),String.valueOf(multiply),null);

        Booker booker = new Booker(req.getcName(),req.getMobile(),req.geteName());
        orderInfo.setBooker(booker);

        Product product = new Product();
        product.setQuantity(req.getQunatity());
        product.setVisitDate(req.getBeginDate());
        //2021-05-31 goodsid和productId从mongo拿
        if(scenicSpotProductMPO != null){
            product.setGoodsId(Long.valueOf(scenicSpotProductMPO.getSupplierProductId()));
            product.setProductId(Long.valueOf(scenicSpotProductMPO.getExtendParams().get("productId")));
        }else{
            product.setGoodsId(Long.parseLong(req.getGoodsId()));
            product.setProductId(Long.parseLong(req.getProductId()));
        }
        if(!CollectionUtils.isEmpty(scenicSpotProductPriceMPOS)){
            product.setSellPrice(scenicSpotProductPriceMPOS.get(0).getSettlementPrice().floatValue());
        }else{
            product.setSellPrice(Float.parseFloat(req.getSellPrice()));
        }
        orderInfo.setProduct(product);
        final List<CreateOrderReq.BookGuest> guests = req.getGuests();
        if(ListUtils.isNotEmpty(guests)){

           // Travellers travellers = new Travellers();
            List<Traveller> travellers = new ArrayList<>(guests.size());
            //travellers.setTraveller(traveller);
            for(CreateOrderReq.BookGuest guest :guests){
                String credentialType = null;
                if(StringUtil.isNotEmpty(guest.getCredential())){
                    credentialType= convertLvmamaCredentialsType(guest.getCredentialType());
                }
                Traveller traveller1 = new Traveller(guest.getCname(),guest.getMobile(),guest.getEname(),guest.getEmail(),guest.getCredential(),null,credentialType);
                travellers.add(traveller1);
            }
            orderInfo.setTravellers(travellers);
        }
        request.setOrderInfo(orderInfo);
        //邮寄信息
       /* Recipient recipient = new Recipient();
        request.setRecipient(recipient);*/
    }

    public void convertLvmamaBookOrderRequest(ValidateOrderRequest request, BookCheckReq req, ScenicSpotProductMPO scenicSpotProductMPO, List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS){
        //需要场次号
        int count = req.getCount()+req.getChdCount();
        String sellPrice = req.getSellPrice();
        BigDecimal multiply = new BigDecimal(sellPrice).multiply(new BigDecimal(count));
        OrderInfo orderInfo = new OrderInfo(null,String.valueOf(multiply),null);

        Booker booker = new Booker(req.getChinaName(),req.getMobile(),req.getEmail());
        orderInfo.setBooker(booker);

        Product product = new Product();
        product.setQuantity(req.getCount());
        product.setVisitDate(req.getBeginDate());
        //2021-05-31 goodsid和productId从mongo拿
        if(scenicSpotProductMPO != null){
            product.setGoodsId(Long.valueOf(scenicSpotProductMPO.getSupplierProductId()));
            product.setProductId(Long.valueOf(scenicSpotProductMPO.getExtendParams().get("productId")));
        }else{
            product.setGoodsId(Long.parseLong(req.getGoodsId()));
            product.setProductId(Long.parseLong(req.getProductId()));
        }
        if(!CollectionUtils.isEmpty(scenicSpotProductPriceMPOS)){
            product.setSellPrice(scenicSpotProductPriceMPOS.get(0).getSettlementPrice().floatValue());
        }else{
            product.setSellPrice(Float.parseFloat(req.getSellPrice()));
        }
        orderInfo.setProduct(product);

        final List<CreateOrderReq.BookGuest> guests = req.getGuests();
        if(ListUtils.isNotEmpty(guests)){
            //Travellers travellers = new Travellers();
            List<Traveller> traveller = new ArrayList<>(guests.size());
            //travellers.setTraveller(traveller);
            for(CreateOrderReq.BookGuest guest :guests){
                String credentialType = null;
                if(StringUtil.isNotEmpty(guest.getCredential())){
                    credentialType= convertLvmamaCredentialsType(guest.getCredentialType());
                }

                Traveller traveller1 = new Traveller(guest.getCname(),guest.getMobile(),guest.getEname(),guest.getEmail(),guest.getCredential(),null,credentialType);
                traveller.add(traveller1);
            }

            orderInfo.setTravellers(traveller);
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
