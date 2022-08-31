package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;

import java.util.ArrayList;
import java.util.List;

public class StrategyTester {

    private Strategy.StrategyType strategyType;

    public List<Strategy> getStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();
        // top strats

        //RATIO_DIAGONAL
        strategyType = Strategy.StrategyType.RATIO_DIAGONAL;

        strategies.add(getStrategy(new Leg(2, 55, 400), new Leg(-1, 85, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));
        strategies.add(getStrategy(new Leg(4, 30, 300), new Leg(-1, 85, 7), new Leg(1, 50, 300), new Leg(-4, 15, 300), Strategy.RollingStrategy.ROLL_SAME_STRIKE));
        strategies.add(getStrategy(new Leg(4, 80, 400), new Leg(-3, 85, 60), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));

/**


 //CALENDAR
 strategyType = Strategy.StrategyType.CALENDAR;

 //        strategies.add(getStrategy(new Leg(1, 50, 30), new Leg(-1, 0, 7), null, null, Strategy.RollingStrategy.NONE));
 strategies.add(getStrategy(new Leg(1, 30, 60), new Leg(-1, 0, 15), null, null, Strategy.RollingStrategy.NONE));
 strategies.add(getStrategy(new Leg(1, 35, 70), new Leg(-1, 0, 15), null, null, Strategy.RollingStrategy.NONE));
 **/
        //DOUBLE_CALENDAR
        strategyType = Strategy.StrategyType.DOUBLE_CALENDAR;
        strategies.add(getStrategy(new Leg(1, 30, 60), new Leg(-1, 0, 30), new Leg(1, 30, 60, OptionV2.OptionType.put), new Leg(-1, 0, 30, OptionV2.OptionType.put)));

        //STRADDLE
        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(1, 50, 7), new Leg(1, 50, 7, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 30, 14), new Leg(1, 30, 14, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 20, 14), new Leg(1, 20, 14, OptionV2.OptionType.put)));

        //SIMPLE
        strategyType = Strategy.StrategyType.SIMPLE;

        strategies.add(getStrategy(new Leg(1, 50, 14)));
        strategies.add(getStrategy(new Leg(1, 50, 14, OptionV2.OptionType.put)));

        strategies.add(getStrategy(new Leg(1, 30, 14)));
        strategies.add(getStrategy(new Leg(1, 30, 14, OptionV2.OptionType.put)));

        strategies.add(getStrategy(new Leg(1, 20, 7)));
        strategies.add(getStrategy(new Leg(1, 20, 7, OptionV2.OptionType.put)));

        strategies.add(getStrategy(new Leg(1, 20, 14)));
        strategies.add(getStrategy(new Leg(1, 20, 14, OptionV2.OptionType.put)));

        return strategies;
    }


    //usually for small tests
    public List<Strategy> getTestStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 50, 7), new Leg(1, 50, 7, OptionV2.OptionType.put)));

        strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put), null, null, null, Strategy.ExitStrategy._50_PERCENT_PROFIT));

        strategyType = Strategy.StrategyType.SIMPLE;
        strategies.add(getStrategy(new Leg(3, 20, 14, OptionV2.OptionType.put)));

        return strategies;
    }

    private Strategy getStrategy(Leg leg1) {
        return getStrategy(leg1, null, null, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2) {
        return getStrategy(leg1, leg2, null, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3) {
        return getStrategy(leg1, leg2, leg3, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3, Leg leg4) {
        return getStrategy(leg1, leg2, leg3, leg4, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3, Leg leg4, Strategy.RollingStrategy rollingStrategy) {
        return getStrategy(leg1, leg2, leg3, leg4, rollingStrategy, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3, Leg leg4, Strategy.RollingStrategy rollingStrategy, Strategy.ExitStrategy exitStrategy) {
        String leg1string = leg1.toString();

        String leg2string = leg2 != null ? leg2.toString() : null;
        String leg3string = leg3 != null ? leg3.toString() : null;
        String leg4string = leg4 != null ? leg4.toString() : null;

        Strategy strategy = new Strategy(leg1string + " " + leg2string + " " + leg3string + " " + leg4string, "", leg1string, leg2string, leg3string, leg4string);

        if (rollingStrategy != null) {
            strategy.setRollingStrategy(rollingStrategy);
        }

        if (exitStrategy != null) {
            strategy.setExitStrategy(exitStrategy);
        }

        if (strategyType != null) {
            strategy.setStrategyType(strategyType);
        }

        return strategy;
    }

    private class Leg {
        int coef;
        double delta;
        int days;
        OptionV2.OptionType optionType = OptionV2.OptionType.call;

        public Leg(int coef, double delta, int days) {
            this.coef = coef;
            this.delta = delta;
            this.days = days;
        }

        public Leg(int coef, double delta, int days, OptionV2.OptionType optionType) {
            this.coef = coef;
            this.delta = delta;
            this.days = days;
            this.optionType = optionType;
        }

        @Override
        public String toString() {
            return coef + " " + optionType.getDescription() + " " + delta + " " + days;
        }
    }
}
