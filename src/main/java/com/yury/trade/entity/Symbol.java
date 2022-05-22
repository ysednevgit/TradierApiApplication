package com.yury.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
public class Symbol {

    @Id
    private String symbol;

    @Enumerated(EnumType.STRING)
    private Type type;

    private Double last;

    private Long average_volume;

    @Enumerated(EnumType.STRING)
    private OptionType option_type;

    private String exchange;

    private String description;

    private String root_symbol;

    private Integer open_interest;

    private Double greeks_iv;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date expiration_date;

    private Date updated;

    public enum Type {
        stock,
        etf,
        option
    }

    public enum OptionType {
        call,
        put
    }

}
