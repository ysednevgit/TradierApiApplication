package com.yury.trade.repository;

import com.yury.trade.entity.StockHistory;
import com.yury.trade.entity.StockHistoryId;
import org.springframework.data.repository.CrudRepository;

public interface StockHistoryRepository extends CrudRepository<StockHistory, StockHistoryId> {

}
