package com.yury.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Date;

@Embeddable
@Data
public class StrategyPerformanceId implements Serializable {

    private String symbol;//SPY

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;//2022-01-20

    private String strategyDescription;//Strategy{ 4 C 30.0 300 -1 C 80.0 7 null null }

    @Override
    public String toString() {
        return symbol + " " + startDate + " " + strategyDescription;
    }
}
