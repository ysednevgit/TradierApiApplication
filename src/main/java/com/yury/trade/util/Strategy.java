package com.yury.trade.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Strategy {

    private String name;

    private String description;

    private RollingStrategy rollingStrategy = RollingStrategy.NONE;

    private ProfitExitStrategy profitExitStrategy = ProfitExitStrategy.NONE;

    private StrategyType strategyType = StrategyType.CUSTOM;

    private int changeStartDateByDays = 0;

    private int daysWhenExit = 0;

    //list of 1 C 20 300  - means buy 1 call 20 delta with 300 days out
    private List<String> legs = new ArrayList<>();

    public Strategy(String name, String leg1, String leg2, String leg3, String leg4) {
        this.name = name;

        addLeg(leg1);
        addLeg(leg2);
        addLeg(leg3);
        addLeg(leg4);
    }

    private void addLeg(String leg) {
        if (leg != null) {
            legs.add(leg);
        }
    }

    @Override
    public String toString() {

        String rs = !RollingStrategy.NONE.equals(rollingStrategy) ? rollingStrategy.name() + " " : "";
        String es = !ProfitExitStrategy.NONE.equals(profitExitStrategy) ? profitExitStrategy.name() : "";
        String des = description != null ? description : "";

        return "Strategy{ " + name + " " + des + rs + es + "}";
    }

    public enum RollingStrategy {
        ROLL_SAME_STRIKE,
        ROLL_SAME_DELTA,
        NONE
    }

    public enum ProfitExitStrategy {
        _10_PERCENT_PROFIT,
        _20_PERCENT_PROFIT,
        _35_PERCENT_PROFIT,
        _50_PERCENT_PROFIT,
        _75_PERCENT_PROFIT,
        NONE
    }

    public enum StrategyType {
        RATIO_DIAGONAL,
        CALENDAR,
        DOUBLE_CALENDAR,
        STRADDLE,
        CUSTOM,
        SIMPLE,
        RATIO,
        DEBIT_SPREAD,
        BOMB,
        IRON_CONDOR
    }

}