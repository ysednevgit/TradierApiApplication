package com.yury.trade.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class StrategyPerformanceData {

    @Id
    private long id;

    @Column(length = 10000)
    private String data = "";

    public void addData(String data) {
        this.data += data + "\n";
    }

}
