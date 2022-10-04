package com.yury.trade.delegate;

import com.yury.trade.entity.FlowPerformance;
import com.yury.trade.entity.OptionV2;
import com.yury.trade.util.*;
import lombok.Data;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private Map<Date, Integer> balanceMap = new LinkedHashMap<>();

    private List<Spread> spreads = new ArrayList<>();

    public void getFlow(final String symbol,
                        final String startDateString,
                        final String endDateString,
                        boolean debug,
                        boolean drawChart) throws ParseException {

        List<SymbolWithDates> symbolsWithDates = getSymbolsWithDates(symbol, startDateString, endDateString);

        for (SymbolWithDates symbolWithDates : symbolsWithDates) {

            for (Strategy strategy : new StrategyTester().getFlowStrategiesToTest()) {
                getFlow(symbolWithDates, debug, drawChart, strategy);
            }


//            getFlow(symbolWithDates, debug, drawChart, null, 50, 8, 2, Arrays.asList(1));
//            getFlow(symbolWithDates, debug, drawChart, null, 50, 8, 1, Arrays.asList(1, 2));
//            getFlow(symbolWithDates, debug, drawChart, null, 50, 7, 1, Arrays.asList(1, 3));


        }

        System.out.println("Get flow End.");
    }

    public void getFlow(SymbolWithDates symbolWithDates,
                        boolean debug,
                        boolean drawChart,
                        Strategy strategy) {

        String stockSymbol = symbolWithDates.getSymbol();
        Date startDate = symbolWithDates.getStartDate();
        Date endDate = symbolWithDates.getEndDate() != null ? symbolWithDates.getEndDate() : new Date();

        LineChartDataset dataset = new LineChartDataset();
        dataset.setName(symbolWithDates.getSymbol());

        Strategy.ProfitExitStrategy exitStrategy = strategy.getProfitExitStrategy();

        System.out.println("Get Flow: " + stockSymbol + " " + sdf.format(startDate) + " " + sdf.format(endDate) + " " + strategy);

        List<Date> validDates = persistenceDelegate.getOptionRepository().findGreeksUpdatedAt(stockSymbol);

        Map<Date, Double> stockHistoryMap = statsDelegate.getStockHistoryMap(stockSymbol);

        FlowPosition position = new FlowPosition();

        int maxDrawDown = 0;
        StringBuilder chartData = new StringBuilder();

        Date date = new Date(startDate.getTime());
        Date lastDate = null;

        while (!date.after(endDate)) {

            if (!validDates.contains(date)) {
                addDays(date, 1);

                continue;
            }

            System.out.println(sdf.format(date) + " ");

            int dayOfWeek = getDayOfWeek(convertToLocalDate(date));

            updatePosition(position, debug);

            exitPositionWhenShortStrike(strategy, position, stockHistoryMap.get(date), debug);

            updateSpreads(position, strategy, debug);

            List<Integer> buyDays = strategy.getBuyDays();

            if (buyDays.contains(dayOfWeek) || (buyDays.size() == 0 && position.getOptionsCount() == 0)) {

                List<OptionV2> dateOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

                if (dateOptions.size() == 0) {
                    return;
                }

                Spread spread = new Spread();
                spreads.add(spread);

                for (String leg : strategy.getLegs()) {

                    OptionV2 option = add(Strategy.getDelta(leg), Strategy.getDaysToExpiry(leg), Strategy.getOptionType(leg),
                            dateOptions, position, Strategy.getCoeff(leg), date, debug);

                    if (option == null) {
                        removeSpread(spread, position, debug);
                        break;
                    }

                    spread.add(option.getOptionV2Id().getSymbol());
                }

                updateSpreads(position, strategy, debug);
            }

            position.calc();

            updateSpreads(position, strategy, debug);


            balanceMap.put(new Date(date.getTime()), (int) (position.positionPrice * position.contractSize + position.adjustments));

            maxDrawDown = Math.min(maxDrawDown, balanceMap.get(date));
            chartData.append(balanceMap.get(date)).append(",");

            if (drawChart) {
                dataset.add(balanceMap.get(date));
            }

            if (debug) {
                System.out.println(getBalance(date) + " Stock Price: " + stockHistoryMap.get(date) + " " + position);
            }

            lastDate = new Date(date.getTime());

            addDays(date, 1);
        }
        chartData.deleteCharAt(chartData.lastIndexOf(","));

        FlowPerformance flowPerformance = createFlowPerformance(symbolWithDates, lastDate, stockHistoryMap, maxDrawDown, strategy, chartData.toString());

        persistenceDelegate.getFlowPerformanceRepository().save(flowPerformance);

        if (drawChart) {
            List<LineChartDataset> datasets = new ArrayList<>();
            datasets.add(dataset);

            chartDelegate.drawChart(stockSymbol, datasets);
        }

    }

    private void updateSpreads(FlowPosition position, Strategy strategy, boolean debug) {

        List<Spread> spreadsToRemove = new ArrayList<>();

        for (Spread spread : spreads) {
            int price = 0;

            for (String optionSymbol : spread.optionSymbols) {

                if (position.itemsMap.get(optionSymbol) == null) {
                    if (!strategy.getRollingStrategy().equals(Strategy.RollingStrategy.NONE)) {
                        spreadsToRemove.add(spread);
                    }
                    break;
                }
                price += position.itemsMap.get(optionSymbol).getOptionV2().getMid_price() * position.contractSize;
            }

            if (spread.getInitialPrice() == 0) {
                spread.setInitialPrice(price);
            }
            spread.setPrice(price);

            if (Strategy.ProfitExitStrategy._50_PERCENT_PROFIT.equals(strategy.getProfitExitStrategy()) && spread.getPrice() > 1.5 * spread.getInitialPrice()) {

                spreadsToRemove.add(spread);

            }
        }

        for (Spread spread : spreadsToRemove) {
            removeSpread(spread, position, debug);
        }
    }

    private void removeSpread(Spread spread, FlowPosition position, boolean debug) {

        for (String optionSymbol : spread.optionSymbols) {
            remove(position, optionSymbol, debug);
        }
        spreads.remove(spread);
    }

    private FlowPerformance createFlowPerformance(SymbolWithDates symbolWithDates,
                                                  Date lastDate,
                                                  Map<Date, Double> stockHistoryMap,
                                                  int maxDrawDown,
                                                  Strategy strategy,
                                                  String chartData) {

        String stockSymbol = symbolWithDates.getSymbol();
        Date startDate = symbolWithDates.getStartDate();
        Date endDate = symbolWithDates.getEndDate();

        FlowPerformance flowPerformance = new FlowPerformance();
        flowPerformance.setSymbol(stockSymbol);
        flowPerformance.setStartDate(startDate);
        flowPerformance.setEndDate(endDate);
        flowPerformance.setChangeValue(balanceMap.get(lastDate));
        flowPerformance.setStockChange(Precision.round(stockHistoryMap.get(lastDate) / stockHistoryMap.get(startDate), 2));
        flowPerformance.setMaxDrawDown(maxDrawDown);
        flowPerformance.setUpdated(new Date());

        String endDateStr = symbolWithDates.getEndDate() != null ? " to " + sdf.format(endDate) : "";

        flowPerformance.setName(stockSymbol + " " + sdf.format(startDate) + endDateStr + " " + strategy);
        flowPerformance.setDescription(stockSymbol + " " + strategy);

        flowPerformance.setChartData(chartData);

        String[] words = flowPerformance.getChartData().split(",");

        List<Integer> upMoves = new ArrayList<>();
        List<Integer> downMoves = new ArrayList<>();

        int maxUp = 0;
        int maxDown = 0;

        for (int i = 0; i < words.length - 1; i++) {

            int move = Integer.parseInt(words[i + 1]) - Integer.parseInt(words[i]);

            if (move < 0) {
                downMoves.add(move);
                if (move < maxDown) {
                    maxDown = move;
                }
            } else {
                upMoves.add(move);
                if (move > maxUp) {
                    maxUp = move;
                }
            }
        }
        flowPerformance.setUp_chance(upMoves.size() * 100 / (upMoves.size() + downMoves.size()));
        flowPerformance.setDown_chance(100 - flowPerformance.getUp_chance());
        flowPerformance.setAvg_up_move(getAvg(upMoves));
        flowPerformance.setAvg_down_move(getAvg(downMoves));
        flowPerformance.setMedian_up_move(getMedium(upMoves));
        flowPerformance.setMedian_down_move(getMedium(downMoves));
        flowPerformance.setMedian_down_move(getMedium(downMoves));
        flowPerformance.setMax_up_move(maxUp);
        flowPerformance.setMax_down_move(maxDown);

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

    protected OptionV2 add(double delta, final int daysToExpiry, final OptionV2.OptionType optionType, final List<OptionV2> options, FlowPosition position, int coeff, Date date, boolean debug) {
        OptionV2 option = statsDelegate.getClosest(delta, daysToExpiry, optionType, null, null, options);

        if (option.getMid_price() == 0) {
            return option;
        }

        if (!position.optionsByOptionSymbolMap.containsKey(option.getOptionV2Id().getSymbol())) {

            List<OptionV2> optionsByOptionSymbol = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterOrSameUpdated(option.getOptionV2Id().getSymbol(), date);

            if (optionsByOptionSymbol.size() < 2) {
                return null;
            }
            position.optionsByOptionSymbolMap.put(option.getOptionV2Id().getSymbol(), optionsByOptionSymbol);
        }

        position.add(option, coeff);
        position.adjustments -= coeff * option.getMid_price() * position.contractSize;

        if (debug) {
            System.out.println("Add " + coeff + " " + option);
        }

        return option;
    }

    protected void remove(FlowPosition position, String optionSymbol, boolean debug) {

        FlowPosition.Item item = position.itemsMap.get(optionSymbol);

        if (item == null) {
            return;
        }

        position.adjustments += item.getCoeff() * item.getOptionV2().getMid_price() * position.contractSize;

        position.remove(optionSymbol);
        position.optionsByOptionSymbolMap.remove(optionSymbol);

        if (debug) {
            System.out.println("Remove " + item.getCoeff() + " " + optionSymbol + " $" + (int) (item.getOptionV2().getMid_price() * position.contractSize));
        }
    }

    protected List<SymbolWithDates> getSymbolsWithDates(final String symbol, final String startDateString, final String endDateString) throws ParseException {

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;
        Date endDate = endDateString != null ? sdf.parse(endDateString) : null;

        if (symbol != null && startDate != null) {
            List<SymbolWithDates> symbolsWithDates = new ArrayList<>();

            SymbolWithDates symbolWithDates = new SymbolWithDates() {
                @Override
                public String getSymbol() {
                    return symbol;
                }

                @Override
                public Date getStartDate() {
                    return startDate;
                }

                @Override
                public Date getEndDate() {
                    return endDate;
                }
            };

            symbolsWithDates.add(symbolWithDates);
            return symbolsWithDates;
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

        for (FlowPosition.Item item : position.itemsMap.values()) {

            String optionSymbol = item.getOptionV2().getOptionV2Id().getSymbol();

            List<OptionV2> optionsByOptionSymbol = position.optionsByOptionSymbolMap.get(optionSymbol);

            item.setIndex(item.getIndex() + 1);
            item.setOptionV2(optionsByOptionSymbol.get(item.getIndex()));

            if (item.getIndex() == optionsByOptionSymbol.size() - 1) {
                optionsSymbolsToRemove.add(optionSymbol);

            }

        }

        for (String optionsSymbolToRemove : optionsSymbolsToRemove) {

            if (position.itemsMap.get(optionsSymbolToRemove).getOptionV2().getMid_price() * position.contractSize < 50) {
                for (Spread spread : spreads) {
                    if (spread.getOptionSymbols().contains(optionsSymbolToRemove)) {
                        spread.getOptionSymbols().remove(optionsSymbolToRemove);
                    }
                }
            }

            remove(position, optionsSymbolToRemove, debug);
        }

    }

    private void exitPositionWhenShortStrike(Strategy strategy, FlowPosition position, double stockPrice, boolean debug) {

        if (!Strategy.ExitStrategy.SHORT_STRIKE.equals(strategy.getExitStrategy())) {
            return;
        }

        List<Spread> spreadsToRemove = new ArrayList<>();

        for (Spread spread : spreads) {

            for (String optionSymbol : spread.getOptionSymbols()) {
                FlowPosition.Item item = position.itemsMap.get(optionSymbol);

                if (item.getCoeff() >= 0) {
                    continue;
                }

                OptionV2 option = item.getOptionV2();
                Double strike = option.getStrike();

                if (OptionV2.OptionType.call.equals(option.getOption_type()) && stockPrice > strike) {
                    spreadsToRemove.add(spread);
                    continue;
                }
                if (OptionV2.OptionType.put.equals(option.getOption_type()) && stockPrice < strike) {
                    spreadsToRemove.add(spread);
                }
            }
        }

        for (Spread spread : spreadsToRemove) {
            removeSpread(spread, position, debug);
        }

    }

    private String getBalance(Date date) {
        return "Balance $" + balanceMap.get(date);
    }

    private LocalDate convertToLocalDate(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private int getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    @Data
    private class Spread {
        List<String> optionSymbols = new LinkedList<>();

        int initialPrice = 0;
        int price = 0;

        public Spread() {
        }

        public void add(String optionSymbol) {
            optionSymbols.add(optionSymbol);
        }

    }

}
