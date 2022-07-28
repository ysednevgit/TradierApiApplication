package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class Strategy {


    @Id
    private String name;

    private String description;

    private String leg1; // 1 C 20 300  - means buy 1 call 20 delta with 300 days out
    private String leg2; // -1 C 20 300  - means sell 1 call 20 delta with 300 days out
    private String leg3;
    private String leg4;

    public Strategy(String name, String description, String leg1, String leg2, String leg3, String leg4) {
        this.name = name;
        this.description = description;
        this.leg1 = leg1;
        this.leg2 = leg2;
        this.leg3 = leg3;
        this.leg4 = leg4;
    }

    @Override
    public String toString() {
        return "Strategy{" +
                "name='" + name + '\'' +
                '}';
    }
}


