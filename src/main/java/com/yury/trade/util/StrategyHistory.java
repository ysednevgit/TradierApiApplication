package com.yury.trade.util;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class StrategyHistory {

    private Strategy strategy;

    private Map<Date, Position> positionMap = new HashMap<>();

    public StrategyHistory(Strategy strategy) {
        this.strategy = strategy;
    }

    public void addData(final Date date, final Position position) {
        positionMap.put(date, position);
    }

}
