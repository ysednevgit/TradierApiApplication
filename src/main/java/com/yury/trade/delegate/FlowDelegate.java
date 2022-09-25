package com.yury.trade.delegate;

import com.yury.trade.entity.FlowPerformance;
import com.yury.trade.entity.OptionV2;
import com.yury.trade.util.FlowPosition;
import com.yury.trade.util.LineChartDataset;
import com.yury.trade.util.Strategy;
import com.yury.trade.util.SymbolWithDate;
import lombok.Data;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class FlowDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private StatsDelegate statsDelegate;

    @Autowired
    private ChartDelegate chartDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private static final int MAX_OPTIONS = 20;

    private Map<Date, Integer> balanceMap = new LinkedHashMap<>();

    private List<Spread> spreads = new ArrayList<>();

    public void getFlow(final String symbol, final String startDateString, boolean debug, boolean drawChart) throws ParseException {

        List<SymbolWithDate> symbolsWithMinDates = getSymbolsWithMinDates(symbol, startDateString);

        for (SymbolWithDate symbolWithDate : symbolsWithMinDates) {
            getFlow(symbolWithDate, debug, drawChart);
        }

        System.out.println("Get flow End.");
    }

    public void getFlow(SymbolWithDate symbolWithDate, boolean debug, boolean drawChart) {

        String stockSymbol = symbolWithDate.getSymbol();
        final Date startDate = symbolWithDate.getDate();

        LineChartDataset dataset = new LineChartDataset();
        dataset.setName(symbolWithDate.getSymbol());

        int delta = 50;
        int daysToExpiry = 10;
        Strategy.ProfitExitStrategy exitStrategy = null;//Strategy.ProfitExitStrategy._50_PERCENT_PROFIT;

        System.out.println("Get Flow: " + stockSymbol + " " + sdf.format(startDate));

        List<Date> validDates = persistenceDelegate.getOptionRepository().findGreeksUpdatedAt(stockSymbol);

        Map<Date, Double> stockHistoryMap = statsDelegate.getStockHistoryMap(stockSymbol);

        FlowPosition position = new FlowPosition();

        int maxDrawDown = 0;
        StringBuilder chartData = new StringBuilder();

        Date date = new Date(startDate.getTime());
        Date endDate = null;

        while (date.before(new Date())) {

            if (!validDates.contains(date)) {
                addDays(date, 1);
                continue;
            }

            System.out.println(sdf.format(date) + " ");

            updatePosition(position, debug);

            if (exitStrategy != null) {
                updateSpreads(position, exitStrategy, debug);
            }

            if (position.itemsMap.size() < MAX_OPTIONS) {

                List<OptionV2> dateOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

                if (dateOptions.size() == 0) {
                    return;
                }

                OptionV2 option1 = buy(delta, daysToExpiry, OptionV2.OptionType.call, dateOptions, position, 1, date, debug);
                OptionV2 option2 = buy(delta, daysToExpiry, OptionV2.OptionType.put, dateOptions, position, 1, date, debug);

                if (exitStrategy != null) {
                    spreads.add(new Spread(option1.getOptionV2Id().getSymbol(), option2.getOptionV2Id().getSymbol()));
                    updateSpreads(position, exitStrategy, debug);
                }

            }

            position.calc();

            if (exitStrategy != null) {
                updateSpreads(position, exitStrategy, debug);
            }

            balanceMap.put(new Date(date.getTime()), (int) (position.positionPrice * position.contractSize + position.adjustments));

            maxDrawDown = Math.min(maxDrawDown, balanceMap.get(date));
            chartData.append(balanceMap.get(date)).append(",");

            if (drawChart) {
                dataset.add(balanceMap.get(date));
            }

            if (debug) {
                System.out.println(getBalance(date) + " Stock Price: " + stockHistoryMap.get(date) + " " + position);
            }

            endDate = new Date(date.getTime());

            addDays(date, 1);
        }
        chartData.deleteCharAt(chartData.lastIndexOf(","));

        FlowPerformance flowPerformance = createFlowPerformance(stockSymbol, startDate, endDate, stockHistoryMap, maxDrawDown, delta, daysToExpiry, chartData.toString(), exitStrategy);

        persistenceDelegate.getFlowPerformanceRepository().save(flowPerformance);

        if (drawChart) {
            List<LineChartDataset> datasets = new ArrayList<>();
            datasets.add(dataset);

            chartDelegate.drawChart(stockSymbol, datasets);
        }

    }

    private void updateSpreads(FlowPosition position, Strategy.ProfitExitStrategy exitStrategy, boolean debug) {

        List<Spread> spreadsToRemove = new ArrayList<>();

        for (Spread spread : spreads) {
            int price = 0;

            for (String optionSymbol : spread.optionSymbols) {

                if (position.itemsMap.get(optionSymbol) == null) {
                    spreadsToRemove.add(spread);
                    break;
                }
                price += position.itemsMap.get(optionSymbol).getOptionV2().getMid_price() * position.contractSize;
            }

            if (spread.getInitialPrice() == 0) {
                spread.setInitialPrice(price);
            }
            spread.setPrice(price);

            if (Strategy.ProfitExitStrategy._50_PERCENT_PROFIT.equals(exitStrategy) && spread.getPrice() > 1.5 * spread.getInitialPrice()) {

                spreadsToRemove.add(spread);

                for (String optionSymbol : spread.optionSymbols) {
                    sell(position, optionSymbol, debug);
                }
            }
        }

        for (Spread spread : spreadsToRemove) {
            spreads.remove(spread);
        }
    }

    private FlowPerformance createFlowPerformance(String stockSymbol, Date startDate, Date endDate, Map<Date, Double> stockHistoryMap, int maxDrawDown, int delta, int daysToExpiry, String chartData, Strategy.ProfitExitStrategy exitStrategy) {
        FlowPerformance flowPerformance = new FlowPerformance();
        flowPerformance.setSymbol(stockSymbol);
        flowPerformance.setStartDate(startDate);
        flowPerformance.setChangeValue(balanceMap.get(endDate));
        flowPerformance.setStockChange(Precision.round(stockHistoryMap.get(endDate) / stockHistoryMap.get(startDate), 2));
        flowPerformance.setMaxDrawDown(maxDrawDown);

        String exitStrategyStr = exitStrategy != null ? " " + exitStrategy.name() : "";

        flowPerformance.setName(stockSymbol + " " + sdf.format(startDate) + " " + delta + " " + daysToExpiry + exitStrategyStr);
        flowPerformance.setChartData(chartData);

        String[] words = flowPerformance.getChartData().split(",");

        List<Integer> upMoves = new ArrayList<>();
        List<Integer> downMoves = new ArrayList<>();

        for (int i = 0; i < words.length - 1; i++) {

            int move = Integer.parseInt(words[i + 1]) - Integer.parseInt(words[i]);

            if (move < 0) {
                downMoves.add(move);
            } else {
                upMoves.add(move);
            }
        }
        flowPerformance.setUp_chance(upMoves.size() * 100 / (upMoves.size() + downMoves.size()));
        flowPerformance.setDown_chance(100 - flowPerformance.getUp_chance());
        flowPerformance.setAvg_up_move(getAvg(upMoves));
        flowPerformance.setAvg_down_move(getAvg(downMoves));
        flowPerformance.setMedian_up_move(getMedium(upMoves));
        flowPerformance.setMedian_down_move(getMedium(downMoves));

        return flowPerformance;
    }

    private int getAvg(List<Integer> list) {
        int sum = 0;

        for (Integer item : list) {
            sum += item;
        }

        return sum / list.size();
    }

    private int getMedium(List<Integer> list) {
        Collections.sort(list);

        if (list.size() % 2 == 0) {
            return list.get(list.size() / 2 - 1);
        }

        return list.get(list.size() / 2);
    }

    protected OptionV2 buy(double delta, final int daysToExpiry, final OptionV2.OptionType optionType, final List<OptionV2> options, FlowPosition position, int coeff, Date date, boolean debug) {
        OptionV2 option = statsDelegate.getClosest(delta, daysToExpiry, optionType, null, null, options);

        if (option.getMid_price() == 0) {
            return option;
        }

        position.add(option, coeff);
        position.adjustments -= coeff * option.getMid_price() * position.contractSize;

        if (!position.optionsByOptionSymbolMap.containsKey(option.getOptionV2Id().getSymbol())) {
            position.optionsByOptionSymbolMap.put(option.getOptionV2Id().getSymbol(), persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterOrSameUpdated(option.getOptionV2Id().getSymbol(), date));
        }

        if (debug) {
            System.out.println("Buy " + coeff + " " + option);
        }

        return option;
    }

    protected void sell(FlowPosition position, String optionSymbol, boolean debug) {

        FlowPosition.Item item = position.itemsMap.get(optionSymbol);
        position.adjustments += item.getCoeff() * item.getOptionV2().getMid_price() * position.contractSize;

        position.remove(optionSymbol);
        position.optionsByOptionSymbolMap.remove(optionSymbol);

        if (debug) {
            System.out.println("Sell " + optionSymbol + " $" + (int) (item.getOptionV2().getMid_price() * position.contractSize));
        }
    }

    protected List<SymbolWithDate> getSymbolsWithMinDates(final String symbol, final String startDateString) throws ParseException {

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        if (symbol != null && startDate != null) {
            List<SymbolWithDate> symbolsWithMinDates = new ArrayList<>();

            SymbolWithDate symbolWithDate = new SymbolWithDate() {
                @Override
                public String getSymbol() {
                    return symbol;
                }

                @Override
                public Date getDate() {
                    return startDate;
                }
            };

            symbolsWithMinDates.add(symbolWithDate);
            return symbolsWithMinDates;
        }

        return persistenceDelegate.getOptionRepository().findRootSymbolsWithMinDates();
    }

    private void addDays(Date date, int days) {
        date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000);
    }

    private void updatePosition(FlowPosition position, boolean debug) {
        if (position.itemsMap.size() == 0) {
            return;
        }

        List<String> optionsSymbolsToRemove = new ArrayList<>();

        int i = 0;

        for (FlowPosition.Item item : position.itemsMap.values()) {

            String optionSymbol = item.getOptionV2().getOptionV2Id().getSymbol();

            List<OptionV2> optionsByOptionSymbol = position.optionsByOptionSymbolMap.get(optionSymbol);

            item.setIndex(item.getIndex() + 1);
            item.setOptionV2(optionsByOptionSymbol.get(item.getIndex()));

            if (item.getIndex() == optionsByOptionSymbol.size() - 1) {
                optionsSymbolsToRemove.add(optionSymbol);

            }

            i++;
        }

        for (String optionsSymbolToRemove : optionsSymbolsToRemove) {
            sell(position, optionsSymbolToRemove, debug);
        }
    }

    private String getBalance(Date date) {
        return "Balance $" + balanceMap.get(date);
    }

    @Data
    private class Spread {
        List<String> optionSymbols = new LinkedList<>();

        int initialPrice = 0;
        int price = 0;

        public Spread() {
        }

        public Spread(String optionSymbol1, String optionSymbol2) {
            getOptionSymbols().add(optionSymbol1);
            getOptionSymbols().add(optionSymbol2);
        }
    }

}
