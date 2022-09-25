package com.yury.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.util.Date;

@Entity
@Data
public class FlowPerformance {

    @Id
    private String name;

    private Integer changeValue;

    private String symbol;//SPY

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;//2022-01-20

    @Lob
    private String chartData = "";

    private Integer maxDrawDown;

    private Double stockChange;

    private Integer up_chance;

    private Integer down_chance;

    private Integer avg_up_move;

    private Integer avg_down_move;

    private Integer median_up_move;

    private Integer median_down_move;

}
