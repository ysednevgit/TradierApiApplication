package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;

import java.util.ArrayList;
import java.util.List;

public class StrategyTester {

    private Strategy.StrategyType strategyType;

    public List<Strategy> getStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();
        // top strats
/**
 1.36	Strategy{ 1 C 30.0 60 null 2 P 30.0 60 -2 P 0.0 30 }	CUSTOM
 1.25	Strategy{ 6 C 20.0 300 -1 C 80.0 7 null null ROLL_SAME_DELTA}	RATIO_DIAGONAL
 1.23	Strategy{ 1 C 20.0 60 -1 C 0.0 30 1 P 20.0 60 -1 P 0.0 30 }	DOUBLE_CALENDAR
 **/
        //RATIO_DIAGONAL
        strategyType = Strategy.StrategyType.RATIO_DIAGONAL;

        strategies.add(getStrategy(new Leg(1, 70, 400), new Leg(-1, 30, 21), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(2, 60, 400), new Leg(-1, 80, 7)));
        strategies.add(getStrategy(new Leg(2, 60, 400), new Leg(-1, 80, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(6, 20, 500), new Leg(-1, 80, 7)));
        strategies.add(getStrategy(new Leg(6, 20, 500), new Leg(-1, 80, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(3, 40, 300), new Leg(-1, 80, 7)));
        strategies.add(getStrategy(new Leg(3, 40, 300), new Leg(-1, 80, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(4, 30, 300), new Leg(-1, 80, 7)));
        strategies.add(getStrategy(new Leg(4, 30, 300), new Leg(-1, 80, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(6, 20, 300), new Leg(-1, 80, 7)));
        strategies.add(getStrategy(new Leg(6, 20, 300), new Leg(-1, 80, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));
        strategies.add(getStrategy(new Leg(6, 20, 300), new Leg(-1, 80, 14), null, null, Strategy.RollingStrategy.ROLL_SAME_DELTA));

        strategies.add(getStrategy(new Leg(4, 80, 120), new Leg(-3, 85, 14), null, null));
        strategies.add(getStrategy(new Leg(4, 80, 400), new Leg(-3, 85, 60), null, null));

        //CALENDAR
        strategyType = Strategy.StrategyType.CALENDAR;

        strategies.add(getStrategy(new Leg(1, 50, 30), new Leg(-1, 0, 7), null, null, Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 30, 60), new Leg(-1, 0, 15), null, null, Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 35, 70), new Leg(-1, 0, 15), null, null, Strategy.RollingStrategy.NONE));

        //STRADDLE
        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(-1, 50, 7), new Leg(-1, 50, 7, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(-1, 50, 15), new Leg(-1, 50, 15, OptionV2.OptionType.put)));

        //DOUBLE_CALENDAR
        strategyType = Strategy.StrategyType.DOUBLE_CALENDAR;
        strategies.add(getStrategy(new Leg(1, 20, 60), new Leg(-1, 0, 30), new Leg(1, 20, 60, OptionV2.OptionType.put), new Leg(-1, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 30, 60), new Leg(-1, 0, 15), new Leg(1, 30, 60, OptionV2.OptionType.put), new Leg(-1, 0, 15, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 30, 200), new Leg(-1, 0, 30), new Leg(1, 30, 200, OptionV2.OptionType.put), new Leg(-1, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));

        //CUSTOM
        strategyType = Strategy.StrategyType.CUSTOM;
        strategies.add(getStrategy(new Leg(1, 50, 60), null, new Leg(2, 30, 60, OptionV2.OptionType.put), new Leg(-2, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 50, 60), null, new Leg(3, 30, 60, OptionV2.OptionType.put), new Leg(-3, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 70, 60), null, new Leg(2, 30, 60, OptionV2.OptionType.put), new Leg(-2, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));
        strategies.add(getStrategy(new Leg(1, 70, 60), null, new Leg(3, 30, 60, OptionV2.OptionType.put), new Leg(-3, 0, 30, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));


        return strategies;
    }

    //usually for small tests
    public List<Strategy> getTestStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        //CALENDAR
        strategyType = Strategy.StrategyType.CUSTOM;

        strategies.add(getStrategy(new Leg(1, 50, 60), null, new Leg(2, 35, 60, OptionV2.OptionType.put), new Leg(-2, 0, 15, OptionV2.OptionType.put), Strategy.RollingStrategy.NONE));

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
