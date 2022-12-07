package com.yury.trade.repository;

import com.yury.trade.entity.StockHistoryId;
import com.yury.trade.entity.VolatilityInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface VolatilityInfoRepository extends CrudRepository<VolatilityInfo, StockHistoryId> {

    @Query("SELECT s FROM VolatilityInfo s WHERE id.symbol = ?1 AND id.date >= ?2  ORDER BY id.date")
    List<VolatilityInfo> findByIdSymbolAndIdDate(String symbol, Date date);


}