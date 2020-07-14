package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.OrderStatus;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
public class OrderInfoTranser {

	public static int genCommonOrderStatus(int orderStatus, int type) {
		int rtnStatus=0;
		if(type==1){//要出发转换
//			0	待支付：创建订单成功，合作方尚未付款。
//	10	待确认：支付订单成功，要出发确认流程中
//	11	待确认（申请取消）：合作方申请取消，要出发在审核状态
//	12	[全网预售特有]预约出行：待二次预约
//	13	[全网预售特有]立即补款：待二次预约补款
//	20	待出行：要出发已确认订单，客人可出行消费
//	30	已消费：客人已消费订单
//	40	已取消：订单已取消
			switch (orderStatus) {
				case 0:
					rtnStatus = OrderStatus.TO_BE_PAID.getCode();//1
					break;
				case 10:
					rtnStatus=10;
					break;
				case 11:
					rtnStatus = 10;
					break;
				case 12:
					rtnStatus=11;
					break;
				case 13:
					rtnStatus = 12;
					break;
				case 20:
					rtnStatus=20;
					break;
				case 30:
					rtnStatus = 30;
					break;
				case 40:
					rtnStatus=40;
					break;
				default:
					break;
			}
		}
		return rtnStatus;
	}

	public static CentralError findCentralError(String msg){
		CentralError rtnError=null;
		for (CentralError entry : CentralError.values()){
			if(entry.getError().equals(msg)){
				rtnError=entry;
				break;
			}
		}
		if(null==rtnError)
			rtnError=CentralError.ERROR_UNKNOWN;

		return rtnError;
	}
}
