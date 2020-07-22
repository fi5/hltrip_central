package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 描述: <br> 供应商异常转义
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/14<br>
 */
@Slf4j
public class SupplierErrorMsgTransfer {
    public static BaseResponse buildMsg(String msg){
        switch (msg){
            case "请填写联系人手机号码" :
            case "请填写联系人姓名" :
            case "订单缺失出行人信息" :
            case "订单缺失身份证信息，至少需要一个身份证" :
            case "订单缺失身份证信息，至少需N个身份证" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_CONNECT_SUPPLIER);
                //不会出现这种情况
//            case "门票使用日期不在入住范围内" :
//            case "餐券使用日期不在入住范围内" :
//            case "餐券使用日期不在票范围内" :
//            case "同一类型资源产品使用时间必须相同" :
//                return BaseResponse.fail(CentralError.ERROR_DATE_ORDER);
            case "该产品不存在" :
            case "产品Id错误" :
                return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_SUPPLIER);
            case "该产品已下架" :
                return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_WITHDRAW_SUPPLIER);
            case "订单已存在" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_ISEXIST_SUPPLIER);
            case "价格不存在" :
            case "无价格信息" :
            case "价格不存在或者售罄" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_NO_PRICE);
            case "产品价格不得低于最低售价" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_PRICE_COMPARE);
            case "订单总价错误" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_TOTAL_PRICE);
            case "库存不足" :
                return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
            case "创建订单失败，购买数少于最小购买数" :
            case "创建订单失败，购买数超出最多购买数" :
            case "创建订单失败，入住晚数少于最小入住晚数" :
            case "创建订单失败，入住晚数超出最大入住晚数" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_CREATE_SUPPLIER);
            case "支付失败，该订单号不存在":
                return BaseResponse.fail(CentralError.ERROR_PAY_NO_ORDER);
            case "支付失败，对应支付流水号已存在":
                return BaseResponse.fail(CentralError.ERROR_ORDER_SERIALNUMBER_ISEXIST_SUPPLIER);
            case "订单已过期，不允许支付，请重新下单":
                return BaseResponse.fail(CentralError.ERROR_ORDER_EXPIRE_SUPPLIER);
            case "订单已支付":
                return BaseResponse.fail(CentralError.ERROR_ORDER_HASPAY_SUPPLIER);
            case "支付订单金额错误":
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_PRICE_SUPPLIER);
            case "支付失败，已超过该产品的可预订时间，请重新选择日期":
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_TIMEOUTBOOK_SUPPLIER);
            case "额度支付扣减出错":
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_AMOUNT_DEDUCTION_SUPPLIER);
            case "支付流水号为空":
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAYSERIALNUMBER_ISNULL);
            case "订单不存在":
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);
            case "订单号为空":
            case "合作商订单号为空":
                return BaseResponse.fail(CentralError.ERROR_ORDERNO_ISNULL);
            default:
                log.error("错误异常描述是 ：{}",msg);
                return BaseResponse.fail(9999,msg,null);
        }
    }
}
