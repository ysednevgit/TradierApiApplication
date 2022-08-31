package com.yury.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;

@Entity
@Data
public class OptionV2 implements Serializable {

    @Transient
    private static DecimalFormat df2 = new DecimalFormat("###.##");

    @EmbeddedId
    private OptionV2Id optionV2Id;

    private String description;//"SPY Jan 20 2023 $190.00 Put"

    private String exch;//"Z"
    private Double last;//0.77
    private Double change;//0
    private Integer volume;//0
    private Integer open;
    private Double high;
    private Double low;
    private Double close;
    private Double bid;//0.76
    private Double ask;//0.78
    private Double mid_price;//0.77

    private String underlying;//"SPY"
    private Double strike;//190

    //greeks
    private Double delta;//-0.0039554099866707
    private Double gamma;//0.00008971700375536063
    private Double theta;//-0.0012644444007122117
    private Double vega;//0.009263385706564832
    private Double rho;//0.43574179665108764
    private Double bid_iv;//0.5193757913668888
    private Double mid_iv;//0.5205006117090346
    private Double ask_iv;//0.5216254320511804

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date greeks_updated_at;//"2022-07-21 20:00:08"
    private Double change_percentage;
    private Integer last_volume;
    private Long trade_date;//1658423566393
    private Double prevclose;//0.77
    private Integer open_interest;
    private Integer contract_size;//100

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date expiration_date;//"2023-01-20",
    private String expiration_type;//"standard"

    @Enumerated(EnumType.STRING)
    private OptionType option_type;

    private Integer days_left;

    @Override
    public String toString() {
        return optionV2Id.getSymbol() + " " + getDays_left() + " days left $" + df2.format(mid_price * contract_size) ;
    }

    public enum OptionType {
        call("C"),
        put("P");

        private String description;

        OptionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

}
