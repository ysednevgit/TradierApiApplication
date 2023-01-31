package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StrategyTester {

    private Strategy.StrategyType strategyType;

    public List<Strategy> getStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();
        // top strats

        //RATIO_DIAGONAL
        strategyType = Strategy.StrategyType.RATIO_DIAGONAL;

        strategies.add(getStrategy(new Leg(4, 55, 300), new Leg(-2, 80, 8), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));
/**
 //DOUBLE_CALENDAR
 strategyType = Strategy.StrategyType.DOUBLE_CALENDAR;
 strategies.add(getStrategy(new Leg(1, 30, 90), new Leg(-1, 0, 30), new Leg(1, 30, 90, OptionV2.OptionType.put), new Leg(-1, 0, 30, OptionV2.OptionType.put)));
 **/

/**
 //STRADDLE
 strategyType = Strategy.StrategyType.STRADDLE;
 strategies.add(getStrategy(new Leg(1, 50, 10), new Leg(1, 50, 10, OptionV2.OptionType.put)));
 strategies.add(getStrategy(new Leg(1, 50, 10), new Leg(1, 50, 10, OptionV2.OptionType.put), null, null, null, Strategy.ProfitExitStrategy._50_PERCENT_PROFIT));
 strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put)));
 strategies.add(getStrategy(new Leg(1, 50, 14), new Leg(1, 50, 14, OptionV2.OptionType.put), null, null, null, Strategy.ProfitExitStrategy._50_PERCENT_PROFIT));

 //SIMPLE
 strategyType = Strategy.StrategyType.SIMPLE;
 strategies.add(getStrategy(new Leg(1, 20, 14, OptionV2.OptionType.put)));
 **/
        return strategies;
    }

    //usually for small tests
    public List<Strategy> getTestStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        int days = 60;
        int coeff = 1;
        int delta = 15;

        strategyType = Strategy.StrategyType.CUSTOM;
        strategies.add(getStrategy(new Leg(coeff, delta, days), new Leg(coeff, delta, days, OptionV2.OptionType.put)));


//        strategies.add(getStrategy(new Leg(4, 20, 500, OptionV2.OptionType.put), new Leg(-4, 20, 60, OptionV2.OptionType.put),null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));;

//        strategyType = Strategy.StrategyType.DEBIT_SPREAD;
//        strategies.add(getStrategy(new Leg(4, 50, 8), new Leg(-4, 30, 8), new Leg(4, 50, 8, OptionV2.OptionType.put), new Leg(-4, 30, 8, OptionV2.OptionType.put)));

//        strategyType = Strategy.StrategyType.SIMPLE;
//        strategies.add(getStrategy(new Leg(1, 20, 14, OptionV2.OptionType.put)));

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

        List<List<Integer>> buyDaysLists = new ArrayList<>();
//        buyDaysLists.add(Arrays.asList(1));
//        buyDaysLists.add(Arrays.asList(3));
        buyDaysLists.add(Arrays.asList(5));

        for (List<Integer> buyDays : buyDaysLists) {
            strategyType = Strategy.StrategyType.CUSTOM;

//            Strategy strategy1 = getStrategy(new Leg(1, 50, 8), new Leg(2, 50, 8, OptionV2.OptionType.put), new Leg(-5, 10, 8, OptionV2.OptionType.put));
//            Strategy strategy2 = getStrategy(new Leg(2, 30, 3, OptionV2.OptionType.put), new Leg(-5, 12, 3, OptionV2.OptionType.put));
//            Strategy strategy1 = getStrategy(new Leg(2, 35, 8, OptionV2.OptionType.put), new Leg(-5, 17, 8, OptionV2.OptionType.put));

            Strategy strategy1 = getStrategy(new Leg(1, 50, 21), new Leg(-1, 80, 8));
//            strategy1.setBuyDays(buyDays);
            strategies.add(strategy1);

        }

        //        strategies.add(getStrategy(new Leg(2, 25, 8)));
//        strategies.add(getStrategy(new Leg(1, 50, 8), new Leg(1, 50, 8, OptionV2.OptionType.put)));

//        Strategy strategy2 = getStrategy(new Leg(-2, 27, 8, OptionV2.OptionType.put));
//        strategy2.setExitStrategy(Strategy.ExitStrategy.SHORT_STRIKE);
//        strategies.add(strategy2);


//        strategies.add(strategy1);

//        delta = 30;
//        strategies.add(getStrategy(new Leg(coeff, delta, days), new Leg(coeff, delta, days, OptionV2.OptionType.put)));


/**
 strategies.add(getStrategy(new Leg(12, 33, 300), new Leg(-4, 80, 6)));


 strategyType = Strategy.StrategyType.STRADDLE;
 strategies.add(getStrategy(new Leg(1, 50, 6), new Leg(1, 50, 6, OptionV2.OptionType.put)));
 **/
        return strategies;
    }

    void addStrategy(List<Strategy> strategies, Strategy strategy, List<Integer> buyDays) {
        strategy.setBuyDays(buyDays);
        strategies.add(strategy);
    }

    //for some custom algo testing
    public List<Strategy> getCustomStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        strategyType = Strategy.StrategyType.STRADDLE;
        strategies.add(getStrategy(new Leg(6, 50, 200), new Leg(-3, 85, 7), null, null, Strategy.RollingStrategy.ROLL_SAME_STRIKE));

        return strategies;
    }

    private Strategy.RollingStrategy getRollingStrategy(Strategy.RollingStrategy rollingStrategy, int minDays) {
        rollingStrategy.setMinDays(minDays);
        return rollingStrategy;
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
        return getStrategy(leg1, leg2, leg3, leg4, rollingStrategy, profitExitStrategy, null);
    }

    private Strategy getStrategy(Leg leg1, Leg leg2, Leg leg3, Leg leg4, Strategy.RollingStrategy rollingStrategy, Strategy.ProfitExitStrategy profitExitStrategy, Strategy.ExitStrategy exitStrategy) {
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

        if (exitStrategy != null) {
            strategy.setExitStrategy(exitStrategy);
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
