package com.yury.trade.repository;

import com.yury.trade.entity.StrategyPerformanceId;
import com.yury.trade.entity.StrategyPerformanceTotal;
import org.springframework.data.repository.CrudRepository;

public interface StrategyPerformanceTotalRepository extends CrudRepository<StrategyPerformanceTotal, StrategyPerformanceId> {

}