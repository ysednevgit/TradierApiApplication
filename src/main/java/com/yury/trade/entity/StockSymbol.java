package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class StockSymbol {

    @Id
    private String symbol;

    private String exchange;

    private String description;

    private String root_symbol;

    private boolean enabled;

}
