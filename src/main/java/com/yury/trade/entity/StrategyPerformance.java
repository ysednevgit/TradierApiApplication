package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Data
public class StrategyPerformance {

    @EmbeddedId
    private StrategyPerformanceId strategyPerformanceId;

    private Double change;
    private Integer changeValue;

    private Double thetaToPricePct;
    private Double thetaTotalToPricePct;
    private double thetaTotal;

    private Double maxDrawDown;
    private Integer maxDrawDownValue;

    private Double stockLatest;
    private Double stockChange;

    private int daysRun;

    private String strategyType;

    private long index;
}
