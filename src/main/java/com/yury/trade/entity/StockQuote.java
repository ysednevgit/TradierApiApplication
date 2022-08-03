package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class StockQuote {

    @Id
    private String symbol;

    private Double last;

    private Long average_volume;
}
