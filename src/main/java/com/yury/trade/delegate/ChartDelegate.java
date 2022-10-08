package com.yury.trade.delegate;

import com.yury.trade.entity.FlowPerformance;
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

    @Autowired
    private FlowDelegate flowDelegate;

    private StrategyTester strategyTester = new StrategyTester();

    public void drawFlowChart(final String symbol, String startDateString, String endDateString, boolean combo) throws ParseException {

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;
        Date endDate = endDateString != null ? sdf.parse(endDateString) : null;

        List<FlowPerformance> flowPerformances = null;

        if (symbol != null && endDate != null) {
            flowPerformances = persistenceDelegate.getFlowPerformanceRepository().findByParams(symbol, startDate, endDate);
        } else if (symbol != null) {
            flowPerformances = persistenceDelegate.getFlowPerformanceRepository().findByParams(symbol, startDate);
        } else {
            flowPerformances = persistenceDelegate.getFlowPerformanceRepository().findByStartDate(startDate);
        }

        if (combo) {
            FlowPerformance flowPerformanceCombo = getCombo(flowPerformances);
            flowDelegate.addInfoToFlowPerformance(flowPerformanceCombo);

            flowPerformanceCombo.setUpdated(new Date());
            flowPerformanceCombo.setSymbol(symbol);
            flowPerformanceCombo.setStartDate(startDate);
            flowPerformanceCombo.setEndDate(endDate);

            endDateString = endDateString == null ? "" : " " + endDateString;

            flowPerformanceCombo.setName("Combo " + symbol + " " + startDateString + endDateString);

            persistenceDelegate.getFlowPerformanceRepository().save(flowPerformanceCombo);

            flowPerformances.add(flowPerformanceCombo);
        }

        List<LineChartDataset> lineChartDatasets = getLineChartDatasets(flowPerformances);

        EventQueue.invokeLater(() -> {

            var ex = new LineChartBuilder(symbol, lineChartDatasets);
            ex.setVisible(true);
        });

    }

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

    public void drawChart(final String symbol, final List<LineChartDataset> lineChartDatasets) {

        EventQueue.invokeLater(() -> {

            var ex = new LineChartBuilder(symbol, lineChartDatasets);
            ex.setVisible(true);
        });
    }

    private FlowPerformance getCombo(List<FlowPerformance> flowPerformances) {

        FlowPerformance combo = new FlowPerformance();

        List<Integer> chartData = new ArrayList<>();

        for (FlowPerformance flowPerformance : flowPerformances) {

            String[] words = flowPerformance.getChartData().split(",");

            for (int i = 0; i < words.length; i++) {

                Integer wordInt = Integer.parseInt(words[i]);

                if (chartData.size() < words.length) {
                    chartData.add(wordInt);
                } else {
                    chartData.set(i, wordInt + chartData.get(i));
                }
            }
        }

        String comboChartData = chartData.toString().replace("[", "").replace("]", "").replace(" ", "");

        int changeValue = Integer.parseInt(comboChartData.substring(comboChartData.lastIndexOf(",") + 1));

        combo.setChangeValue(changeValue);

        combo.setChartData(comboChartData);

        return combo;
    }

    private List<StrategyPerformanceTotal> getStrategyPerformanceTotals(String symbol, Date
            startDate, List<String> strategyDescriptions) {

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

    private List<LineChartDataset> getLineChartDatasets(List objects) {

        List<LineChartDataset> result = new ArrayList<>();

        for (Object object : objects) {

            LineChartDataset dataset = null;

            if (object instanceof StrategyPerformanceTotal) {
                dataset = getLineChartDataset((StrategyPerformanceTotal) object);
            } else {
                dataset = getLineChartDataset((FlowPerformance) object);
            }

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

    private LineChartDataset getLineChartDataset(FlowPerformance flowPerformance) {

        LineChartDataset lineChartDataset = new LineChartDataset();
        lineChartDataset.setName(flowPerformance.getName());

        Map<Integer, Integer> data = new LinkedHashMap<>();

        String[] words = flowPerformance.getChartData().split(",");

        int day = 1;

        for (String word : words) {

            data.put(day, Integer.parseInt(word));

            day++;
        }

        lineChartDataset.setData(data);

        return lineChartDataset;
    }
}
