package com.yury.trade.repository;

import com.yury.trade.entity.StockSymbol;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StockSymbolRepository extends CrudRepository<StockSymbol, String> {

    @Query("SELECT symbol FROM StockSymbol")
    List<String> findAllSymbols();

    @Query("SELECT symbol FROM StockSymbol WHERE enabled = 'TRUE'")
    List<String> findEnabledSymbols();

    @Query("SELECT symbol FROM StockSymbol WHERE last > 12 AND average_volume > 1000 AND type = 'stock'")
    List<String> findGoodSymbols();

/**
 @Query("SELECT s FROM Symbol s WHERE last > 12 AND average_volume > 1000 AND type = 'stock'")
 Collection<StockSymbol> findGoodSymbols();

 @Query("SELECT s FROM Symbol s WHERE type = 'option'")
 Collection<StockSymbol> findAllOptions();

 @Transactional
 @Modifying
 @Query("DELETE FROM Symbol WHERE type = 'option'")
 void deleteAllOptions();
 **/
}
