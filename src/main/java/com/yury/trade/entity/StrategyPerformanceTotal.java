package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
@Data
public class StrategyPerformanceTotal {

    @EmbeddedId
    private StrategyPerformanceId strategyPerformanceId;

    private Integer changeValue;

    //change on average
    private double avgChange;

    private Double change;

    private double thetaTotal;

    private Integer maxDrawDownValue;

    private Double stockInitial;
    private Double stockLatest;
    private Double stockChange;

    private int runs;
    private Integer wins;

    private String strategyType;

    private String dataIds;

    @Lob
    private String chartData = "";

}
