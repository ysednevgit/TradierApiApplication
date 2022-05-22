package com.yury.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Data
public class Option {

    @Id
    private String symbol;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date expirationDate;

    private int year;

    private int month;

    private int day;

    private String rootSymbol;

    @Enumerated(EnumType.STRING)
    private Symbol.OptionType type;

    private double strikePrice;

    private Date updated;

    @Transient
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYMMdd");

    public Option(String symbol) {
        this.symbol = symbol;

        this.rootSymbol = symbol.substring(0, symbol.length() - 18);

        symbol = symbol.replace(rootSymbol, "");

        this.year = Integer.parseInt(symbol.substring(0, 2));
        this.month = Integer.parseInt(symbol.substring(2, 4));
        this.day = Integer.parseInt(symbol.substring(4, 6));

        try {
            this.expirationDate = simpleDateFormat.parse(symbol.substring(0, 6));
        } catch (ParseException e) {
            System.out.println("Unable to get expirationDate from " + symbol.substring(0, 6) + " for " + this.symbol);
        }

        this.type = getType(symbol.charAt(6));

        this.strikePrice = Double.parseDouble(symbol.substring(7, 12) + "." + symbol.substring(12));
    }

    private Symbol.OptionType getType(char ch) {
        if (ch == 'C') {
            return Symbol.OptionType.call;
        } else if (ch == 'P') {
            return Symbol.OptionType.put;
        }
        return null;
    }

}
