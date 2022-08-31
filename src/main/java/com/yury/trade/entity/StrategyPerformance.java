package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
@Data
public class StrategyPerformance {

    @EmbeddedId
    private StrategyPerformanceId strategyPerformanceId;

    private Double change;
    private Integer changeValue;

    //position price at start
    private Double initialPrice;

    private double thetaTotal;

    private Double maxDrawDown;
    private Integer maxDrawDownValue;

    private Double stockLatest;
    private Double stockChange;

    private int daysRun;

    private String strategyType;

    private long index;

    @Lob
    private String data = "";

    @Lob
    private String chartData = "";

    public void addData(String data) {
        this.data += data + "\n";
    }

    public void addChartData(String chartData) {
        this.chartData += chartData + "\n";
    }
}
