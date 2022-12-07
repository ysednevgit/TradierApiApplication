package com.yury.trade.repository;

import com.yury.trade.entity.StockQuote;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StockQuoteRepository extends CrudRepository<StockQuote, String> {

    @Query("SELECT symbol FROM StockQuote WHERE last > 30 AND average_volume > 1000000")
    List<String> findGoodSymbols();

}
