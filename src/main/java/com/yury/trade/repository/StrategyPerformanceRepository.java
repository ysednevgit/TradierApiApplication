package com.yury.trade.repository;

import com.yury.trade.entity.StrategyPerformance;
import com.yury.trade.entity.StrategyPerformanceId;
import org.springframework.data.repository.CrudRepository;

public interface StrategyPerformanceRepository extends CrudRepository<StrategyPerformance, StrategyPerformanceId> {

}
