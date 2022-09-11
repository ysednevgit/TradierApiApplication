package com.yury.trade.delegate;

import com.yury.trade.entity.StrategyPerformanceTotal;
import com.yury.trade.util.LineChartBuilder;
import com.yury.trade.util.LineChartDataset;
import com.yury.trade.util.Strategy;
import com.yury.trade.util.StrategyTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@Component
public class ChartDelegate {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private StrategyTester strategyTester = new StrategyTester();

    public void drawChart(final String symbol, final String startDateString, final boolean test) throws ParseException {

        List<StrategyPerformanceTotal> strategyPerformanceTotals = null;

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        strategyPerformanceTotals = getStrategyPerformanceTotals(symbol, startDate, getStrategies(test));

        List<LineChartDataset> lineChartDatasets = getLineChartDatasets(strategyPerformanceTotals);

        EventQueue.invokeLater(() -> {

            var ex = new LineChartBuilder(symbol, lineChartDatasets);
            ex.setVisible(true);
        });

    }

    private List<StrategyPerformanceTotal> getStrategyPerformanceTotals(String symbol, Date startDate, List<String> strategyDescriptions) {

        if (startDate != null && strategyDescriptions != null && strategyDescriptions.size() > 0) {
            return persistenceDelegate.getStrategyPerformanceTotalRepository().find(symbol, startDate, strategyDescriptions);
        }
        if (strategyDescriptions != null && strategyDescriptions.size() > 0) {
            return persistenceDelegate.getStrategyPerformanceTotalRepository().find(symbol, strategyDescriptions);
        }
        if (startDate != null) {
            return persistenceDelegate.getStrategyPerformanceTotalRepository().find(symbol, startDate);
        }
        return persistenceDelegate.getStrategyPerformanceTotalRepository().find(symbol);
    }

    private List<LineChartDataset> getLineChartDatasets(List<StrategyPerformanceTotal> strategyPerformanceTotals) {

        List<LineChartDataset> result = new ArrayList<>();

        for (StrategyPerformanceTotal strategyPerformanceTotal : strategyPerformanceTotals) {

            LineChartDataset dataset = getLineChartDataset(strategyPerformanceTotal);

            if (dataset.getData().size() > 0) {
                result.add(dataset);
            }
        }
        return result;
    }

    private LineChartDataset getLineChartDataset(StrategyPerformanceTotal strategyPerformanceTotal) {
        LineChartDataset lineChartDataset = new LineChartDataset();
        lineChartDataset.setName(strategyPerformanceTotal.getStrategyPerformanceId().getStrategyDescription() + " " + sdf.format(strategyPerformanceTotal.getStrategyPerformanceId().getStartDate()));

        Map<String, String> chartDataMap = new LinkedHashMap<>();

        Map<Integer, Integer> data = new LinkedHashMap<>();

        lineChartDataset.setData(data);

        if (strategyPerformanceTotal.getChartData() == null) {
            return lineChartDataset;
        }

        for (String part : strategyPerformanceTotal.getChartData().split("\n")) {
            String[] words = part.split(",");

            chartDataMap.put(words[0], words[1]);
        }

        int day = 1;

        for (Map.Entry<String, String> entry : chartDataMap.entrySet()) {

            data.put(day, Integer.parseInt(entry.getValue()));

            day++;
        }

        return lineChartDataset;
    }

    private List<String> getStrategies(boolean test) {

        List<String> strategiesDescriptions = new ArrayList<>();

        List<Strategy> strategies = test ? strategyTester.getTestStrategiesToTest() : new ArrayList<>();

        for (Strategy strategy : strategies) {
            strategiesDescriptions.add(strategy.toString());
        }
        return strategiesDescriptions;
    }

}
