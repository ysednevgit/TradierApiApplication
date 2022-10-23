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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;//2022-01-20

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date endDate;//2022-01-20

    private Integer changeValue;

    private Integer initialValue;

    private Double change;

    private String symbol;//SPY

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

    private Integer max_up_move;

    private Integer max_down_move;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date updated;//2022-01-20

}
