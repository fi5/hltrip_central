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
		int rtnStatus = 0;
		if (type == 1) {//要出发转换
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
					rtnStatus = 10;
					break;
				case 11:
					rtnStatus = OrderStatus.APPLYING_FOR_REFUND.getCode();//21
					break;
				case 12:
					rtnStatus = 11;
					break;
				case 13:
					rtnStatus = 12;
					break;
				case 20:
					rtnStatus = 20;
					break;
				case 30:
					rtnStatus = 30;
					break;
				case 40:
					rtnStatus = 40;
					break;
				default:
					rtnStatus = orderStatus;
					break;
			}
		} else if (type == 2) {
			return orderStatus;
		}
		return rtnStatus;
	}


	public static int genCommonOrderStringStatus(String orderStatus, int type) {
		int rtnStatus = 0;
		if (type == 1) {//要出发转换
//			0	待支付：创建订单成功，合作方尚未付款。
//	10	待确认：支付订单成功，要出发确认流程中
//	11	待确认（申请取消）：合作方申请取消，要出发在审核状态
//	12	[全网预售特有]预约出行：待二次预约
//	13	[全网预售特有]立即补款：待二次预约补款
//	20	待出行：要出发已确认订单，客人可出行消费
//	30	已消费：客人已消费订单
//	40	已取消：订单已取消
			switch (orderStatus) {
				case "0":
					rtnStatus = OrderStatus.TO_BE_PAID.getCode();//1
					break;
				case "10":
					rtnStatus = 10;
					break;
				case "11":
					rtnStatus = OrderStatus.APPLYING_FOR_REFUND.getCode();//21
					break;
				case "12":
					rtnStatus = 11;
					break;
				case "13":
					rtnStatus = 12;
					break;
				case "20":
					rtnStatus = 20;
					break;
				case "30":
					rtnStatus = 30;
					break;
				case "40":
					rtnStatus = 40;
					break;
				default:
					rtnStatus = Integer.parseInt(orderStatus);
					break;
			}
		} else if (type == 2) {
			return Integer.parseInt(orderStatus);
		} else if (type == 3) {
			/**
			 * 订单状态:

			 待确认：订单正在校验/占位；
			 待付款：订单在此状态下，同时满足“支付开关canPay=1.可支付”，则可以调用出票代扣接口进行付款；

			 出票中（已确认）：付款后出票中到此状态；
			 已完成：表示出票成功；
			 已取消：订单取消成功，或者退票成功，到此状态；
			 */
			switch (orderStatus) {
				case "待确认":
					rtnStatus = OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode();//0,"支付待确认"
					break;
				case "待付款":
					rtnStatus = OrderStatus.TO_BE_PAID.getCode();//1,"待支付",
					break;
				case "出票中":
				case "已确认":
					rtnStatus = OrderStatus.TO_BE_CONFIRMED.getCode();//待确认 10已经支付了的
					break;
				case "已完成":
					rtnStatus = OrderStatus.WAITING_TO_TRAVEL.getCode();//20,"待出行",
					break;
				case "申请退款中":
					rtnStatus = OrderStatus.APPLYING_FOR_REFUND.getCode();//21,"申请退款中",
					break;
				case "已消费":
					rtnStatus = OrderStatus.CONSUMED.getCode();//30已消费
					break;
				case "已取消":
					rtnStatus = OrderStatus.CANCELLED.getCode();//40
					break;
				case "已退款":
					rtnStatus = OrderStatus.REFUNDED.getCode();//50
					break;
				default:
					rtnStatus = Integer.parseInt(orderStatus);
					break;
			}
		} else if (type == 4) {
			/**
			 * 订单状态:

			 订单状态：待确认，待付款，已确认，已完成，已取消
			 */
			switch (orderStatus) {
				case "待确认":
					rtnStatus = OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode();//0,"支付待确认"
					break;
				case "待付款":
					rtnStatus = OrderStatus.TO_BE_PAID.getCode();//1,"待支付",
					break;
				case "已确认":
					rtnStatus = OrderStatus.WAITING_TO_TRAVEL.getCode();//待确认 10已经支付了的,这个应该转待出行,20,"待出行",
					break;
				case "出游中":
				case "出游归来":
					rtnStatus = OrderStatus.CONSUMED.getCode();//30已消费
					break;
				case "已完成":
					rtnStatus = OrderStatus.CONSUMED.getCode();//30已消费
					break;
				case "申请退款中":
					rtnStatus = OrderStatus.APPLYING_FOR_REFUND.getCode();//21,"申请退款中",
					break;
				case "已取消":
					rtnStatus = OrderStatus.CANCELLED.getCode();//40
					break;
				case "已退款":
					rtnStatus = OrderStatus.REFUNDED.getCode();//50
					break;
				default:
					rtnStatus = Integer.parseInt(orderStatus);
					break;
			}
		}else if (type == 5) {//驴妈妈
			/**
			 * 订单状态:

			 订单状态：待确认，待付款，已确认，已完成，已取消
			 */
			switch (orderStatus) {
				case "待确认":
					rtnStatus = OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode();//0,"支付待确认"
					break;
				case "待付款":
					rtnStatus = OrderStatus.TO_BE_PAID.getCode();//1,"待支付",
					break;
				case "已发送":
				case "未发送":
				case "未使用":
				case "待出行":
					rtnStatus = OrderStatus.WAITING_TO_TRAVEL.getCode();//待确认 10已经支付了的,这个应该转待出行,20,"待出行",
					break;
				case "已消费":
					rtnStatus = OrderStatus.CONSUMED.getCode();//30已消费
					break;
				case "申请退款中":
					rtnStatus = OrderStatus.APPLYING_FOR_REFUND.getCode();//21,"申请退款中",
					break;
				case "已取消":
					rtnStatus = OrderStatus.CANCELLED.getCode();//40
					break;
				case "已退款":
					rtnStatus = OrderStatus.REFUNDED.getCode();//50
					break;
				default:
					rtnStatus = Integer.parseInt(orderStatus);
					break;
			}
		}
		return rtnStatus;
	}

	public static CentralError findCentralError(String msg) {
		CentralError rtnError = null;
		if (msg.equals("重发凭证失败，该订单号不存在"))
			return CentralError.ERROR_NO_ORDER;
		for (CentralError entry : CentralError.values()) {
			if (entry.getError().equals(msg)) {
				rtnError = entry;
				break;
			}
		}
		if (null == rtnError)
			rtnError = CentralError.ERROR_UNKNOWN;

		return rtnError;
	}
}
