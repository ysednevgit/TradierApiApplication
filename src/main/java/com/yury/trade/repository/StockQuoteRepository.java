package com.yury.trade.repository;

import com.yury.trade.entity.StockQuote;
import org.springframework.data.repository.CrudRepository;

public interface StockQuoteRepository extends CrudRepository<StockQuote, String> {
}
