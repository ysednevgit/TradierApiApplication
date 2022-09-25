package com.yury.trade.util;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class LineChartDataset {

    private String name;
    private Map<Integer, Integer> data = new LinkedHashMap<>();

    public void add(Integer item) {
        data.put(data.size() + 1, item);
    }

}
