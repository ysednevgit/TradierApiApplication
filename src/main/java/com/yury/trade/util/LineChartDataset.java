package com.yury.trade.util;

import lombok.Data;

import java.util.Map;

@Data
public class LineChartDataset {

    private String name;
    private Map<Integer, Integer> data;
}
