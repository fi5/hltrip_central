package com.huoli.trip.central.web.dao;


import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;

import java.util.List;

/**
 * @time   :2021/3/19
 * @comment:
 **/
public interface ScenicSpotDao {


	ScenicSpotMPO qyerySpotById(String scenicSpotId);

	List<ProductListMPO> querySpotProductBySpotIdAndDate(String scenicSpotId,String date);

	List<ScenicSpotProductMPO> querySpotProduct(String scenicSpotId, List<String> channelInfo);

	List<ScenicSpotProductPriceMPO> queryProductPriceByProductId(String scenicSpotProductId);

	List<ScenicSpotProductPriceMPO> queryPriceByProductIdAndDate(String scenicSpotProductId,String startDate,String endDate);

	ScenicSpotRuleMPO queryRuleById(String ruleId);
	ScenicSpotProductMPO querySpotProductById(String productId, List<String> channelInfo);
	ScenicSpotProductPriceMPO querySpotProductPriceById(String priceId);

    ScenicSpotProductBackupMPO queryBackInfoByProductId(String productId);

	/**
	 * 获取价格，产品id、规则id、票种确定唯一
	 * @param scenicSpotProductId
	 * @param startDate
	 * @param endDate
	 * @param ruleId
	 * @param ticketKind
	 * @return
	 */
	List<ScenicSpotProductPriceMPO> queryPrice(String scenicSpotProductId, String startDate, String endDate, String ruleId, String ticketKind);

    List<ScenicSpotProductPriceMPO> queryPriceByProductIds(List<String> productIds, String startDate, String endDate);

    List<ScenicSpotMPO> queryByKeyword(String keyword, int count);

	List<ScenicSpotMPO> queryScenicSpotByPoint(double longitude,double latitude);
}
