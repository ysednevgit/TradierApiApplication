package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class Stats {

    @Id
    String symbol;

    Double callRatio;
    Double putRatio;

    Double lowIvCall;
    Double highIvCall;

    Double lowIvPut;
    Double highIvPut;

    String lowIvCallSymbol;
    String highIvCallSymbol;

    String lowIvPutSymbol;
    String highIvPutSymbol;

    Date updated;

}
