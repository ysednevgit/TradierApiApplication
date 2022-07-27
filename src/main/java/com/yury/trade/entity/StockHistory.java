package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Data
public class StockHistory {

    @EmbeddedId
    StockHistoryId stockHistoryId;

    Double open;
    Double high;
    Double low;
    Double close;
    Long volume;

}
