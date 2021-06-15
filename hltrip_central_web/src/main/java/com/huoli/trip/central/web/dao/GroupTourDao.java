package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;

import java.util.List;

/**
 * @author zhouwenbin
 * @version 1.0
 * @date 2021/5/9
 */
public interface GroupTourDao {

    GroupTourProductMPO queryTourProduct(String groupTourId);

    List<GroupTourProductSetMealMPO> queryProductSetMealByProductId(String productId, List<String> depCodes, String packageId, String date);

    GroupTourProductSetMealMPO queryGroupSetMealBySetId(String setMealId);

}
