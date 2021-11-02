package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.mpo.ScenicProductSortMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotRuleMPO;

import java.util.List;


public interface ScenicProductSortDao {
    List<ScenicProductSortMPO> queryScenicProductSortByScenicId(String scenicId);
}
