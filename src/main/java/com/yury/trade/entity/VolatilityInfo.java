package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Data
@Entity
public class VolatilityInfo implements Comparable {

    @EmbeddedId
    private StockHistoryId id;

    //volatility
    private Double CC_vol;
    private Double OC_vol;
    private Double CO_vol;

    private Double OCbyCC_index;

    private Double stDeviation;

    //price change in $
    private Double CC_price_change;
    private Double OC_price_change;
    private Double CO_price_change;

    //price_change divide by stDeviation
    private Double CC_spike;
    private Double OC_spike;
    private Double CO_spike;

    @Override
    public int compareTo(Object o) {
        return OCbyCC_index.compareTo(((VolatilityInfo) o).getOCbyCC_index());
    }
}
