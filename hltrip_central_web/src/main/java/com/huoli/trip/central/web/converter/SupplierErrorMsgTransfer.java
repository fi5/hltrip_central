package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.vo.response.BaseResponse;

/**
 * 描述: <br> 供应商异常转义
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/14<br>
 */
public class SupplierErrorMsgTransfer {
    public static BaseResponse buildMsg(String msg){
        switch (msg){
            case "请填写联系人手机号码" :
            case "请填写联系人姓名" :
            case "无价格信息" :
            case "门票使用日期不在入住范围内" :
            case "餐券使用日期不在入住范围内" :
            case "餐券使用日期不在票范围内" :
            case "同一类型资源产品使用时间必须相同" :
            case "订单缺失出行人信息" :
            case "订单缺失身份证信息，至少需要一个身份证" :
            case "订单缺失身份证信息，至少需N个身份证" :
            case "创建订单失败，购买数少于最小购买数" :
            case "创建订单失败，购买数超出最多购买数" :
            case "创建订单失败，入住晚数少于最小入住晚数" :
            case "创建订单失败，入住晚数超出最大入住晚数" :
                return BaseResponse.fail(401,msg,null);
            case "该产品不存在" :
                return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_SUPPLIER);
            case "该产品已下架" :
                return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_WITHDRAW_SUPPLIER);
            case "订单已存在" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_ISEXIST_SUPPLIER);
            case "价格不存在" :
            case "产品价格不得低于最低售价" :
            case "价格不存在或者售罄" :
            case "订单总价错误" :
                return BaseResponse.fail(2014,msg,null);
            case "库存不足" :
                return BaseResponse.fail(CentralError.ERROR_ORDER_STOCK_SUPPLIER);
            default:break;
        }
       return BaseResponse.fail(CentralError.ERROR_BAD_REQUEST);
    }
}
