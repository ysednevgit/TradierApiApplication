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
 //RATIO_DIAGONAL
 strategyType = Strategy.StrategyType.RATIO_DIAGONAL;

 strategies.add(getStrategy(new Leg(4, 30, 300), new Leg(-1, 85, 7), new Leg(1, 50, 300), new Leg(-4, 15, 300), Strategy.RollingStrategy.ROLL_SAME_STRIKE));
 strategies.add(getStrategy(new Leg(4, 80, 400), new Leg(-3, 85, 60), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));
 **/
/**
 //DOUBLE_CALENDAR
 strategyType = Strategy.StrategyType.DOUBLE_CALENDAR;
 strategies.add(getStrategy(new Leg(1, 30, 90), new Leg(-1, 0, 30), new Leg(1, 30, 90, OptionV2.OptionType.put), new Leg(-1, 0, 30, OptionV2.OptionType.put)));
 **/
        //STRADDLE
        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(1, 50, 10), new Leg(1, 50, 10, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 50, 10), new Leg(1, 50, 10, OptionV2.OptionType.put), null, null, null, Strategy.ProfitExitStrategy._50_PERCENT_PROFIT));
        strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put), null, null, null, Strategy.ProfitExitStrategy._50_PERCENT_PROFIT));

        //SIMPLE
        strategyType = Strategy.StrategyType.SIMPLE;
        strategies.add(getStrategy(new Leg(1, 20, 14, OptionV2.OptionType.put)));

        return strategies;
    }

    //usually for small tests
    public List<Strategy> getTestStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

//        strategyType = Strategy.StrategyType.DEBIT_SPREAD;
//        strategies.add(getStrategy(new Leg(4, 50, 8), new Leg(-4, 30, 8), new Leg(4, 50, 8, OptionV2.OptionType.put), new Leg(-4, 30, 8, OptionV2.OptionType.put)));

        //RATIO_DIAGONAL
        strategyType = Strategy.StrategyType.SIMPLE;

        strategies.add(getStrategy(new Leg(1, 40, 14)));
        strategies.add(getStrategy(new Leg(1, 30, 6)));

        //strategies.add(getStrategy(new Leg(2, 55, 6), new Leg(-1, 85, 6)));
        //strategies.add(getStrategy(new Leg(4, 80, 400), new Leg(-3, 85, 60), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));


        /**
         strategyType = Strategy.StrategyType.CUSTOM;
         strategies.add(getStrategy(new Leg(1, 10, 14), new Leg(-1, 10, 28),
         new Leg(1, 10, 14, OptionV2.OptionType.put), new Leg(-1, 10, 28, OptionV2.OptionType.put)));
         **/

        return strategies;
    }

    //for some custom algo testing
    public List<Strategy> getFlowStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        strategyType = Strategy.StrategyType.BOMB;
        Strategy strategy = getStrategy(new Leg(1, 20, 14), new Leg(1, 20, 14, OptionV2.OptionType.put),
                new Leg(-1, 20, 7), new Leg(-1, 20, 7, OptionV2.OptionType.put));
//        strategy.setBuyDays(Arrays.asList(1, 3, 5));

        strategy.setExitStrategy(Strategy.ExitStrategy.SHORT_STRIKE);

        strategies.add(strategy);

        return strategies;
    }


    //for some custom algo testing
    public List<Strategy> getCustomStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(6, 50, 200), new Leg(-3, 85, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));

        return strategies;
    }

    private Strategy getStrategy(Leg leg1) {
        return getStrategy(leg1, null, null, null);
    }

    protected Strategy getStrategy(Leg leg1, Leg leg2) {
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

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3, Leg leg4, Strategy.RollingStrategy rollingStrategy, Strategy.ProfitExitStrategy profitExitStrategy) {
        String leg1string = leg1.toString();

        String leg2string = leg2 != null ? leg2.toString() : null;
        String leg3string = leg3 != null ? leg3.toString() : null;
        String leg4string = leg4 != null ? leg4.toString() : null;

        StringBuilder nameBuilder = new StringBuilder(leg1.toString());

        appendLegToName(nameBuilder, leg2);
        appendLegToName(nameBuilder, leg3);
        appendLegToName(nameBuilder, leg4);

        Strategy strategy = new Strategy(nameBuilder.toString(), leg1string, leg2string, leg3string, leg4string);

        if (rollingStrategy != null) {
            strategy.setRollingStrategy(rollingStrategy);
        }

        if (profitExitStrategy != null) {
            strategy.setProfitExitStrategy(profitExitStrategy);
        }

        if (strategyType != null) {
            strategy.setStrategyType(strategyType);
        }

        return strategy;
    }

    private void appendLegToName(StringBuilder name, Leg leg) {
        if (leg != null) {
            name.append(" ").append(leg);
        }
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
