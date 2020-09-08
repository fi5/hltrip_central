package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.web.converter.SupplierErrorMsgTransfer;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.CenterBookCheck;
import com.huoli.trip.supplier.api.HllxService;

import com.huoli.trip.supplier.self.hllx.vo.HllxBaseResult;
import com.huoli.trip.supplier.self.hllx.vo.HllxBookCheckReq;
import com.huoli.trip.supplier.self.hllx.vo.HllxBookCheckRes;
import com.huoli.trip.supplier.self.hllx.vo.HllxBookSaleInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;

import java.util.List;

public class HllxOrderManager extends OrderManager {
    @Reference(timeout = 10000, group = "hltrip", check = false)
    private HllxService hllxService;

    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_HLLX;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("hllx");
        return "hllx";
    }


    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        if(StringUtils.isEmpty(end)){
            end = begin;
        }

        /**
         * 开始日期大于结束日期
         */
        if(DateTimeUtil.parseDate(begin).after(DateTimeUtil.parseDate(begin))){
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_1);
        }
        /**
         * 时间跨度大于90天
         */
        /*if(this.isOutTime(DateTimeUtil.parseDate(begin),DateTimeUtil.parseDate(begin))){
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_2);
        }*/

        //开始组装供应商请求参数
        HllxBookCheckReq req1 = new HllxBookCheckReq();
        //转供应商productId
        //ycfBookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        req1.setBeginDate(begin);
        req1.setEndDate(end);
        HllxBookCheckRes hllxBookCheckRes;
        try {
            //供应商输出
            HllxBaseResult<HllxBookCheckRes> checkInfos = hllxService.getCheckInfos(req1);
            if(checkInfos!=null&&checkInfos.getStatusCode()==200){
                hllxBookCheckRes = checkInfos.getData();
                if(hllxBookCheckRes == null){
                    return SupplierErrorMsgTransfer.buildMsg(checkInfos.getMessage());//异常消息以供应商返回的
                }else{
                    CenterBookCheck  bookCheck = new CenterBookCheck();
                    List<HllxBookSaleInfo> saleInfos = hllxBookCheckRes.getSaleInfos();
                    if(ListUtils.isNotEmpty(saleInfos)){
                        return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
                    }
                    HllxBookSaleInfo hllxBookSaleInfo = saleInfos.get(0);
                    int stocks = hllxBookSaleInfo.getTotalStock();
                    if(req.getCount() > stocks){
                        return BaseResponse.withFail(CentralError.NOTENOUGH_STOCK_ERROR,bookCheck);
                    }
                    bookCheck.setSettlePrice(hllxBookSaleInfo.getPrice());
                    bookCheck.setSalePrice(hllxBookSaleInfo.getSalePrice());
                    bookCheck.setStock(stocks);
                    return BaseResponse.success(bookCheck);
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
            }
        }catch (HlCentralException e){
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
        }
    }
}
