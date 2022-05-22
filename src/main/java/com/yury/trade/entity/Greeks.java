package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
public class Greeks {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private double delta;
    private double gamma;
    private double theta;
    private double vega;
    private double mid_iv;

}
