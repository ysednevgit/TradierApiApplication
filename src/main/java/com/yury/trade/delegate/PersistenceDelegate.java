package com.yury.trade.delegate;

import com.yury.trade.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PersistenceDelegate {

    @Autowired
    private StockSymbolRepository stockSymbolRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private StockQuoteRepository stockQuoteRepository;

    @Autowired
    private StrategyPerformanceDataRepository strategyPerformanceDataRepository;

    @Autowired
    private StrategyPerformanceTotalRepository strategyPerformanceTotalRepository;

    @Autowired
    private StrategyPerformanceRepository strategyPerformanceRepository;

    public StockSymbolRepository getStockSymbolRepository() {
        return stockSymbolRepository;
    }

    public OptionRepository getOptionRepository() {
        return optionRepository;
    }

    public StockHistoryRepository getStockHistoryRepository() {
        return stockHistoryRepository;
    }

    public StrategyPerformanceRepository getStrategyPerformanceRepository() {
        return strategyPerformanceRepository;
    }

    public StockQuoteRepository getStockQuoteRepository() {
        return stockQuoteRepository;
    }

    public StrategyPerformanceDataRepository getStrategyPerformanceDataRepository() {
        return strategyPerformanceDataRepository;
    }

    public StrategyPerformanceTotalRepository getStrategyPerformanceTotalRepository() {
        return strategyPerformanceTotalRepository;
    }
}
