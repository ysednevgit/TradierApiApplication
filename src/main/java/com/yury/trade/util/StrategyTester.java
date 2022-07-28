package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.Strategy;

import java.util.ArrayList;
import java.util.List;

public class StrategyTester {

    public List<Strategy> getStrategiesToTest() {
        List<Strategy> strategies = new ArrayList<>();

        //Ratio diagonals
/**
 strategies.add(getStrategy(new Leg(6, 20, 400), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(4, 30, 400), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(2, 60, 400), new Leg(-1, 80, 7)));

 strategies.add(getStrategy(new Leg(6, 20, 300), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(4, 30, 300), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(2, 60, 300), new Leg(-1, 80, 7)));

 strategies.add(getStrategy(new Leg(6, 20, 200), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(4, 30, 200), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(2, 60, 200), new Leg(-1, 80, 7)));

 strategies.add(getStrategy(new Leg(6, 20, 100), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(4, 30, 100), new Leg(-1, 80, 7)));
 strategies.add(getStrategy(new Leg(2, 60, 100), new Leg(-1, 80, 7)));

 strategies.add(getStrategy(new Leg(3, 50, 120), new Leg(-2, 60, 30)));

 strategies.add(getStrategy(new Leg(4, 80, 300), new Leg(-3, 85, 30)));

        //straddle
        strategies.add(getStrategy(new Leg(-1, 50, 7), new Leg(-1, 50, 7, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(-1, 50, 10), new Leg(-1, 50, 10, OptionV2.OptionType.put)));
        strategies.add(getStrategy(new Leg(-1, 50, 15), new Leg(-1, 50, 15, OptionV2.OptionType.put)));

        strategies.add(getStrategy(new Leg(-1, 70, 10), new Leg(-1, 30, 10, OptionV2.OptionType.put), new Leg(-1, 30, 10), new Leg(-1, 70, 10, OptionV2.OptionType.put)));

**/
        //calendars
        strategies.add(getStrategy(new Leg(1, 50, 30), new Leg(-1, 0, 7)));

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
        String leg1string = leg1.toString();

        String leg2string = leg2 != null ? leg2.toString() : null;
        String leg3string = leg3 != null ? leg3.toString() : null;
        String leg4string = leg4 != null ? leg4.toString() : null;

        return new Strategy(leg1string + " " + leg2string + " " + leg3string + " " + leg4string, "", leg1string, leg2string, leg3string, leg4string);
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
