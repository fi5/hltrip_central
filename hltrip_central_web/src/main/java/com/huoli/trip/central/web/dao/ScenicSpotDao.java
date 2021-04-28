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

	ScenicSpotPayServiceMPO querySpotPayItem(String spotPayItemId);

	List<ScenicSpotProductMPO> querySpotProduct(String scenicSpotId);

	List<ScenicSpotProductPriceMPO> queryProductPrice(String scenicSpotProductId,String startDate,String endDate);

	List<ScenicSpotProductPriceMPO> queryProductPriceByProductId(String scenicSpotProductId);

	List<ScenicSpotProductPriceMPO> queryPriceByProductIdAndDate(String scenicSpotProductId,String startDate,String endDate);
	ScenicSpotRuleMPO queryRuleById(String ruleId);
}
