package com.yury.trade.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Strategy {

    private String name;

    private String description;

    private boolean shouldRoll = true;

    private RollingStrategy rollingStrategy = RollingStrategy.ROLL_SAME_STRIKE;

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

        String rs = RollingStrategy.ROLL_SAME_DELTA.equals(rollingStrategy) ? rollingStrategy.name() : "";

        return "Strategy{ " + name + " " + rs + "}";
    }

    public enum RollingStrategy {
        ROLL_SAME_STRIKE,
        ROLL_SAME_DELTA
    }
}


