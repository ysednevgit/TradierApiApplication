package com.yury.trade.delegate;

import com.yury.trade.repository.OptionRepository;
import com.yury.trade.repository.StatsRepository;
import com.yury.trade.repository.StockHistoryRepository;
import com.yury.trade.repository.StockSymbolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PersistenceDelegate {

    @Autowired
    private StockSymbolRepository stockSymbolRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private StatsRepository statsRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    public StockSymbolRepository getStockSymbolRepository() {
        return stockSymbolRepository;
    }

    public OptionRepository getOptionRepository() {
        return optionRepository;
    }

    public StatsRepository getStatsRepository() {
        return statsRepository;
    }

    public StockHistoryRepository getStockHistoryRepository() {
        return stockHistoryRepository;
    }

}
