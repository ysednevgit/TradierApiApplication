package com.yury.trade.repository;

import com.yury.trade.entity.StockHistoryId;
import com.yury.trade.entity.VolatilityInfo;
import org.springframework.data.repository.CrudRepository;

public interface VolatilityInfoRepository extends CrudRepository<VolatilityInfo, StockHistoryId> {

}