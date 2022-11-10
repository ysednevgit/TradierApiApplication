package com.yury.trade.repository;

import com.yury.trade.entity.StockHistory;
import com.yury.trade.entity.StockHistoryId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface StockHistoryRepository extends CrudRepository<StockHistory, StockHistoryId> {

    @Query("SELECT s FROM StockHistory s WHERE stockHistoryId.symbol = ?1 ORDER BY stockHistoryId.date")
    List<StockHistory> findByStockHistoryIdSymbol(String symbol);

    @Query("SELECT s FROM StockHistory s WHERE stockHistoryId.symbol = ?1 AND stockHistoryId.date >= ?2  ORDER BY stockHistoryId.date")
    List<StockHistory> findByStockHistoryIdSymbolAndDate(String symbol, Date date);

}
