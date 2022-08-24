package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Data
public class StrategyPerformanceTotal {

    @EmbeddedId
    private StrategyPerformanceId strategyPerformanceId;

    private Integer changeValue;
    private double avgChange;

    private double thetaTotal;

    private Integer maxDrawDownValue;

    private Double stockInitial;
    private Double stockLatest;
    private Double stockChange;

    private int runs;

    private String strategyType;
}
