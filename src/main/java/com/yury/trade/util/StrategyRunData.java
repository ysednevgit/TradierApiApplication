package com.yury.trade.util;

import com.yury.trade.entity.StrategyPerformance;
import com.yury.trade.entity.StrategyPerformanceId;
import lombok.Data;

import java.util.*;

@Data
public class StrategyRunData {

    private Map<StrategyPerformanceId, StrategyPerformance> strategyPerformanceMap = new LinkedHashMap<>();

    private Strategy strategy;

    private Date startDate;

    private Double initialStockPrice;

}