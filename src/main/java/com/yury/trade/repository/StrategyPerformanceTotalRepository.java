package com.yury.trade.repository;

import com.yury.trade.entity.StrategyPerformanceId;
import com.yury.trade.entity.StrategyPerformanceTotal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface StrategyPerformanceTotalRepository extends CrudRepository<StrategyPerformanceTotal, StrategyPerformanceId> {

    @Query("SELECT s FROM StrategyPerformanceTotal s WHERE strategyPerformanceId.symbol = :symbol and strategyPerformanceId.strategyDescription IN (:strategyDescriptions) ORDER BY strategy_type, strategy_description")
    List<StrategyPerformanceTotal> find(String symbol, List<String> strategyDescriptions);

    @Query("SELECT s FROM StrategyPerformanceTotal s WHERE strategyPerformanceId.symbol = :symbol and strategyPerformanceId.startDate = :startDate and strategyPerformanceId.strategyDescription IN (:strategyDescriptions) ORDER BY strategy_type, strategy_description")
    List<StrategyPerformanceTotal> find(String symbol, Date startDate, List<String> strategyDescriptions);

    @Query("SELECT s FROM StrategyPerformanceTotal s WHERE strategyPerformanceId.symbol = :symbol ORDER BY strategy_type, strategy_description")
    List<StrategyPerformanceTotal> find(String symbol);

    @Query("SELECT s FROM StrategyPerformanceTotal s WHERE strategyPerformanceId.symbol = :symbol and strategyPerformanceId.startDate = :startDate ORDER BY strategy_type, strategy_description")
    List<StrategyPerformanceTotal> find(String symbol, Date startDate);

}