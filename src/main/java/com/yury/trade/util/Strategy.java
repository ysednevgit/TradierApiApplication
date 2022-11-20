package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Strategy {

    private String name;

    private String description;

    private RollingStrategy rollingStrategy = RollingStrategy.NONE;

    private ProfitExitStrategy profitExitStrategy = ProfitExitStrategy.NONE;

    private ExitStrategy exitStrategy = ExitStrategy.NONE;

    private StrategyType strategyType = StrategyType.CUSTOM;

    private int changeStartDateByDays = 0;

    private int daysWhenExit = 0;

    private List<Integer> buyDays = new ArrayList<>();

    private List<Integer> sellDays = new ArrayList<>();

    //list of 1 C 20 300  - means buy 1 call 20 delta with 300 days out
    private List<String> legs = new ArrayList<>();

    public Strategy(String name, String leg1, String leg2, String leg3, String leg4) {
        this.name = name;

        addLeg(leg1);
        addLeg(leg2);
        addLeg(leg3);
        addLeg(leg4);
    }

    public static int getCoeff(String leg) {
        return Integer.parseInt(leg.split(" ")[0]);
    }

    public static double getDelta(String leg) {
        return Double.parseDouble(leg.split(" ")[2]);
    }

    public static int getDaysToExpiry(String leg) {
        return Integer.parseInt(leg.split(" ")[3]);
    }

    public static OptionV2.OptionType getOptionType(String leg) {
        return "C".equals(leg.split(" ")[1]) ? OptionV2.OptionType.call : OptionV2.OptionType.put;
    }

    private void addLeg(String leg) {
        if (leg != null) {
            legs.add(leg);
        }
    }

    @Override
    public String toString() {

        String rs = !RollingStrategy.NONE.equals(rollingStrategy) ? rollingStrategy + " " : "";
        String pes = !ProfitExitStrategy.NONE.equals(profitExitStrategy) ? profitExitStrategy.name() : "";
        String es = !ExitStrategy.NONE.equals(exitStrategy) ? exitStrategy.name() : "";
        String des = description != null ? description : "";
        String bd = buyDays.size() > 0 ? " " + buyDays : "";


        return "Strategy{" + name + " " + des + rs + es + pes + bd + "}";
    }

    public enum RollingStrategy {
        ROLL_SAME_STRIKE,
        ROLL_SAME_DELTA,
        CUSTOM,
        WHEN_ITM,
        NONE;

        private int minDays = 0;

        public int getMinDays() {
            return minDays;
        }

        public void setMinDays(int minDays) {
            this.minDays = minDays;
        }

        @Override
        public String toString() {
            String minDaysStr = getMinDays() > 0 ? "(" + getMinDays() + ")" : "";

            return name() + minDaysStr;
        }
    }

    public enum ProfitExitStrategy {
        _10_PERCENT_PROFIT,
        _20_PERCENT_PROFIT,
        _35_PERCENT_PROFIT,
        _50_PERCENT_PROFIT,
        _75_PERCENT_PROFIT,
        NONE
    }

    public enum ExitStrategy {
        SHORT_STRIKE,
        LONG_STRIKE,
        NONE
    }

    public enum StrategyType {
        RATIO_DIAGONAL,
        DIAGONAL,
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