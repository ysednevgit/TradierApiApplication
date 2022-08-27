package com.yury.trade.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Strategy {

    private String name;

    private String description;

    private RollingStrategy rollingStrategy = RollingStrategy.NONE;

    private ExitStrategy exitStrategy = ExitStrategy.NONE;
    ;

    private StrategyType strategyType = StrategyType.CUSTOM;

    //list of 1 C 20 300  - means buy 1 call 20 delta with 300 days out
    private List<String> legs = new ArrayList<>();

    public Strategy(String name, String description, String leg1, String leg2, String leg3, String leg4) {
        this.name = name;
        this.description = description;

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
        String es = !ExitStrategy.NONE.equals(exitStrategy) ? exitStrategy.name() : "";

        return "Strategy{ " + name + " " + rs + es + "}";
    }

    public enum RollingStrategy {
        ROLL_SAME_STRIKE,
        ROLL_SAME_DELTA,
        NONE
    }

    public enum ExitStrategy {
        _10_PERCENT_PROFIT,
        _20_PERCENT_PROFIT,
        _50_PERCENT_PROFIT,
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
        DEBIT_SPREAD
    }

}