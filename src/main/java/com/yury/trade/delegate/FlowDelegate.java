package com.yury.trade.delegate;

import com.yury.trade.entity.FlowPerformance;
import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.StockHistory;
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
import java.util.stream.Collectors;

@Component
public class FlowDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private StatsDelegate statsDelegate;

    @Autowired
    private ChartDelegate chartDelegate;

//    private List<Integer> STRADDLE_BUY_DAYS = Arrays.asList(1,3);

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private Map<Date, Integer> balanceMap = new LinkedHashMap<>();

    private Map<Date, StockHistory> stockHistoryMap;

    private StrategyTester strategyTester = new StrategyTester();

    private Calendar calendar = Calendar.getInstance();

    private boolean showTrades = true;

    private final int DELTA_ROLL_SAME_STRIKE = 200;


    public void getFlow(final String symbol, final String startDateString, final String endDateString, boolean debug, boolean drawChart) throws ParseException {

        List<SymbolWithDates> symbolsWithDates = getSymbolsWithDates(symbol, startDateString, endDateString);

        for (SymbolWithDates symbolWithDates : symbolsWithDates) {

            for (Strategy strategy : strategyTester.getFlowStrategiesToTest()) {
                getFlow(symbolWithDates, debug, drawChart, strategy);
            }

        }

        System.out.println("Get flow End.");
    }

    public void getFlow(SymbolWithDates symbolWithDates, boolean debug, boolean drawChart, Strategy strategy) {

        balanceMap.clear();

        String stockSymbol = symbolWithDates.getSymbol();
        Date startDate = symbolWithDates.getStartDate();
        Date endDate = symbolWithDates.getEndDate() != null ? symbolWithDates.getEndDate() : new Date();

        List<Trade> trades = new ArrayList<>();

        LineChartDataset dataset = new LineChartDataset();
        dataset.setName(symbolWithDates.getSymbol());

        System.out.println("Get Flow: " + stockSymbol + " " + sdf.format(startDate) + " " + sdf.format(endDate) + " " + strategy);

        List<Date> validDates = persistenceDelegate.getOptionRepository().findGreeksUpdatedAt(stockSymbol);

        stockHistoryMap = getStockHistoryMap(stockSymbol);

        FlowPosition position = new FlowPosition();

        int maxDrawDown = 0;
        StringBuilder chartData = new StringBuilder();

        int initialValue = 0;

        Date date = new Date(startDate.getTime());
        Date lastDate = null;

        String thetaDescr = "";

        String description = stockSymbol + " " + strategy + thetaDescr;

        while (!date.after(endDate)) {

            if (!validDates.contains(date)) {
                addDays(date, 1);

                continue;
            }

            StockHistory stockHistory = stockHistoryMap.get(date);

            Double stockPrice = stockHistory.getClose();

            if (debug) {
                System.out.println(sdf.format(date) + " (" + Precision.round(stockHistory.getHigh(), 1) + "/" + Precision.round(stockHistory.getLow(), 1) + ")" + " Stock Price: " + stockPrice);
            }

            int shortStrike = 0;

            if (position.getItemsMap().size() == 2) {
                shortStrike = position.getItemsMap().values().stream().collect(Collectors.toList()).get(1).getOptionV2().getStrike().intValue();
            }

            updatePosition(position, strategy, debug);

            int dayOfWeek = getDayOfWeek(convertToLocalDate(date));

            double theta = position.getPositionTheta() * position.getContractSize();

            if (position.getItemsMap().size() < strategy.getLegs().size()
                    || (Strategy.StrategyType.RATIO_DIAGONAL.equals(strategy.getStrategyType()) && shortStrike + 2 > stockPrice)
                    || (Strategy.StrategyType.RATIO_DIAGONAL_6by1.equals(strategy.getStrategyType()) && shortStrike + 1 > stockPrice)
            ) {
                roll(strategy, position, debug);
            }

            exitPositionWhenStrike(strategy, position, stockHistoryMap.get(date).getClose(), debug);

            if (position.getOptionsCount() == 0) {
                updateTrade(position, date, trades);
            }

            if (shouldAdd(strategy, position, date)) {

                List<OptionV2> dateOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

                if (dateOptions.size() == 0) {
                    return;
                }

                add(strategy, position, dateOptions, date, debug);

                Trade trade = new Trade();
                trade.setStockSymbol(stockSymbol);
                trade.setStartDate(new Date(date.getTime()));
                trade.setStartStockPrice(stockHistory.getClose());
                trades.add(trade);
            }

            if (strategy.getSellDays().contains(dayOfWeek)) {
                remove(position, debug);
            }

            position.calc();
            balanceMap.put(new Date(date.getTime()), (int) (position.getPositionPrice() * position.getContractSize() + position.getAdjustments()));

            if (initialValue == 0) {
                initialValue = (int) position.getPositionPrice() * position.getContractSize();
            }

            maxDrawDown = Math.min(maxDrawDown, balanceMap.get(date));
            chartData.append(balanceMap.get(date)).append(",");

            if (drawChart) {
                dataset.add(balanceMap.get(date));
            }

            if (debug) {
                System.out.println(getBalance(date) + " " + position);
            }

            lastDate = new Date(date.getTime());

            addDays(date, 1);

            if (debug) {
                System.out.println();
            }
        }

        chartData.deleteCharAt(chartData.lastIndexOf(","));

        String tradeInfo = TradeInfo.getInfo(trades);

        FlowPerformance flowPerformance = createFlowPerformance(symbolWithDates,
                lastDate, stockHistoryMap, maxDrawDown, chartData.toString(), description, initialValue, tradeInfo);

        persistenceDelegate.getFlowPerformanceRepository().save(flowPerformance);

        if (drawChart) {
            List<LineChartDataset> datasets = new ArrayList<>();
            datasets.add(dataset);

            chartDelegate.drawChart(stockSymbol, datasets);
        }

        if (debug && showTrades) {
            System.out.println(tradeInfo);
        }
    }

    private void updateTrade(FlowPosition position, Date date, List<Trade> trades) {

        if (trades.size() == 0) {
            return;
        }

        position.calc();
        balanceMap.put(new Date(date.getTime()), (int) (position.getPositionPrice() * position.getContractSize() + position.getAdjustments()));

        Trade trade = trades.get(trades.size() - 1);

        if (trade.getEndDate() == null) {
            trade.setEndDate(new Date(date.getTime()));
            trade.setEndStockPrice(stockHistoryMap.get(date).getClose());

            Date[] keys = balanceMap.keySet().toArray(new Date[balanceMap.size()]);
            trade.setProfit(balanceMap.get(keys[keys.length - 1]) - balanceMap.get(trade.getStartDate()));
        }

    }

    private void add(Strategy strategy, FlowPosition position, List<OptionV2> dateOptions, Date date, boolean debug) {

        for (String leg : strategy.getLegs()) {

            OptionV2 option = add(Strategy.getDelta(leg), Strategy.getDaysToExpiry(leg), Strategy.getOptionType(leg), dateOptions, position, Strategy.getCoeff(leg), date, debug);

            if (option == null) {
                remove(position, debug);
                return;
            }
        }
    }

    private boolean shouldAdd(Strategy strategy, FlowPosition position, Date date) {

        int dayOfWeek = getDayOfWeek(convertToLocalDate(date));

        if (strategy.getBuyDays().size() > 0) {
            if (strategy.getBuyDays().contains(dayOfWeek)) {
                return true;
            }
            return false;
        }

        if (position.getOptionsCount() == 0) {
            return true;
        }

        return false;
    }

    private FlowPerformance createFlowPerformance(
            SymbolWithDates symbolWithDates,
            Date lastDate,
            Map<Date, StockHistory> stockHistoryMap,
            int maxDrawDown,
            String chartData,
            String descr,
            int initialPrice,
            String tradeInfo) {

        String stockSymbol = symbolWithDates.getSymbol();
        Date startDate = symbolWithDates.getStartDate();
        Date endDate = symbolWithDates.getEndDate();

        FlowPerformance flowPerformance = new FlowPerformance();
        flowPerformance.setSymbol(stockSymbol);
        flowPerformance.setStartDate(startDate);
        flowPerformance.setEndDate(endDate);
        flowPerformance.setChangeValue(balanceMap.get(lastDate));
        flowPerformance.setStockChange(Precision.round(stockHistoryMap.get(lastDate).getClose() / stockHistoryMap.get(startDate).getClose(), 2));
        flowPerformance.setMaxDrawDown(maxDrawDown);
        flowPerformance.setInitialValue(initialPrice);
        flowPerformance.setChange(Precision.round((double) flowPerformance.getChangeValue() / initialPrice, 2));
        flowPerformance.setUpdated(new Date());

        String endDateStr = symbolWithDates.getEndDate() != null ? " to " + sdf.format(endDate) : "";

        flowPerformance.setName(descr + " " + sdf.format(startDate) + endDateStr);
        flowPerformance.setDescription(descr);

        flowPerformance.setChartData(chartData);

        flowPerformance.setTradesData(tradeInfo);

        addInfoToFlowPerformance(flowPerformance);
        return flowPerformance;
    }

    protected void addInfoToFlowPerformance(FlowPerformance flowPerformance) {
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
    }

    private int getAvg(List<Integer> list) {

        if (list.size() == 0) {
            return 0;
        }

        int sum = 0;

        for (Integer item : list) {
            sum += item;
        }

        return sum / list.size();
    }

    private int getMedium(List<Integer> list) {

        if (list.size() == 0) {
            return 0;
        }
        Collections.sort(list);

        if (list.size() % 2 == 0) {
            return list.get(list.size() / 2 - 1);
        }

        return list.get(list.size() / 2);
    }

    protected OptionV2 add(double delta, final int daysToExpiry, final OptionV2.OptionType optionType, final List<OptionV2> options, FlowPosition position, int coeff, Date date, boolean debug) {

        Double strikePrice = null;

        if (delta == DELTA_ROLL_SAME_STRIKE) {
            strikePrice = position.getShortStrike();

        } else if (delta == 0 && position.getLastOption() != null) {
            strikePrice = position.getLastOption().getStrike();
        }

        OptionV2 option = statsDelegate.getClosest(delta, daysToExpiry, optionType, strikePrice, null, options);

        if (option.getMid_price() == 0) {
            return repeatAdd(delta, daysToExpiry, optionType, options, position, coeff, date, debug);
        }

        if (!position.getOptionsByOptionSymbolMap().containsKey(option.getOptionV2Id().getSymbol())) {

            List<OptionV2> optionsByOptionSymbol = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterOrSameUpdated(option.getOptionV2Id().getSymbol(), date);

            if (optionsByOptionSymbol.size() < 2) {
                return repeatAdd(delta, daysToExpiry, optionType, options, position, coeff, date, debug);
            }
            position.getOptionsByOptionSymbolMap().put(option.getOptionV2Id().getSymbol(), optionsByOptionSymbol);
        } else {
            return null;
        }

        position.add(option, coeff);
        position.setAdjustments(position.getAdjustments() - coeff * option.getMid_price() * position.getContractSize());

        if (debug) {
            System.out.println("Add " + coeff + " " + option);
        }

        position.getCoeffsHistory().add(coeff);

        return option;
    }

    private OptionV2 repeatAdd(double delta, final int daysToExpiry, final OptionV2.OptionType optionType, final List<OptionV2> options, FlowPosition position, int coeff, Date date, boolean debug) {
        Date copyDate = new Date(date.getTime());

        addDays(copyDate, daysToExpiry);

        if (copyDate.before(new Date())) {
            return add(delta, daysToExpiry + 1, optionType, options, position, coeff, date, debug);
        }

        return null;
    }

    protected void remove(FlowPosition position, boolean debug) {

        List<String> optionSymbols = new ArrayList<>();
        optionSymbols.addAll(position.getItemsMap().keySet());

        for (String optionSymbol : optionSymbols) {
            remove(position, optionSymbol, debug);
        }
    }

    protected void remove(FlowPosition position, String optionSymbol, int count, boolean debug) {

        FlowPosition.Item item = position.getItemsMap().get(optionSymbol);

        if (item == null) {
            return;
        }

        if (item.getCoeff() <= count) {
            remove(position, optionSymbol, debug);
            return;
        }
        item.setCoeff(item.getCoeff() - count);

        position.setAdjustments(position.getAdjustments() + count * item.getOptionV2().getMid_price() * position.getContractSize());

        if (debug) {
            System.out.println("Remove " + count + " " + optionSymbol + " $" + (int) (item.getOptionV2().getMid_price() * position.getContractSize()));
        }
    }

    protected void remove(FlowPosition position, String optionSymbol, boolean debug) {

        FlowPosition.Item item = position.getItemsMap().get(optionSymbol);

        if (item == null) {
            return;
        }

        position.setAdjustments(position.getAdjustments() + item.getCoeff() * item.getOptionV2().getMid_price() * position.getContractSize());

        position.getItemsMap().remove(optionSymbol);
        position.getOptionsByOptionSymbolMap().remove(optionSymbol);

        if (debug) {
            System.out.println("Remove " + item.getCoeff() + " " + optionSymbol + " $" + (int) (item.getOptionV2().getMid_price() * position.getContractSize()));
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

    protected void addDays(Date date, int days) {

        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        date.setTime(calendar.getTimeInMillis());
    }

    private void updatePosition(FlowPosition position, Strategy strategy, boolean debug) {
        if (position.getItemsMap().size() == 0) {
            return;
        }

        List<String> optionsSymbolsToRemove = new ArrayList<>();

        for (FlowPosition.Item item : position.getItemsMap().values()) {

            String optionSymbol = item.getOptionV2().getOptionV2Id().getSymbol();

            List<OptionV2> optionsByOptionSymbol = position.getOptionsByOptionSymbolMap().get(optionSymbol);

            item.setIndex(item.getIndex() + 1);
            item.setOptionV2(optionsByOptionSymbol.get(item.getIndex()));

            if (item.getIndex() == optionsByOptionSymbol.size() - 1) {
                optionsSymbolsToRemove.add(optionSymbol);
            }

            if (item.getCoeff() < 0) {
                if (item.getOptionV2().getDays_left() < strategy.getRollingStrategy().getMinDays()) {
                    optionsSymbolsToRemove.add(optionSymbol);
                }
            }
        }

        if (optionsSymbolsToRemove.size() > 0 && Strategy.RollingStrategy.NONE.equals(strategy.getRollingStrategy())) {
            remove(position, debug);
            return;
        }

        for (String optionsSymbolToRemove : optionsSymbolsToRemove) {
            remove(position, optionsSymbolToRemove, debug);
        }
    }

    private void exitPositionWhenStrike(Strategy strategy, FlowPosition position, double stockPrice, boolean debug) {

        if (Strategy.ExitStrategy.NONE.equals(strategy.getExitStrategy())) {
            return;
        }

        boolean shouldExit = false;

        boolean inTheMoney = false;

        for (String leg : strategy.getLegs()) {
            double delta = Strategy.getDelta(leg);
            int coeff = Strategy.getCoeff(leg);

            if (coeff < 0) {
                inTheMoney = delta >= 50;
                break;
            }
        }

        OptionV2 hitOption = null;
        int hitCoeff = 0;

        for (String optionSymbol : position.getItemsMap().keySet()) {
            FlowPosition.Item item = position.getItemsMap().get(optionSymbol);

            if (item == null) {
                continue;
            }

            if (item.getCoeff() > 0) {
                continue;
            }

            OptionV2 option = item.getOptionV2();
            Double strike = option.getStrike();

            hitOption = option;
            hitCoeff = item.getCoeff();

            if (!inTheMoney) {
                if (OptionV2.OptionType.call.equals(option.getOption_type()) && stockPrice > strike) {
                    shouldExit = true;
                    break;
                }
                if (OptionV2.OptionType.put.equals(option.getOption_type()) && stockPrice < strike) {
                    shouldExit = true;
                    break;
                }
            }

            if (inTheMoney) {
                if (OptionV2.OptionType.call.equals(option.getOption_type()) && stockPrice < strike) {
                    shouldExit = true;
                    break;
                }
                if (OptionV2.OptionType.put.equals(option.getOption_type()) && stockPrice > strike) {
                    shouldExit = true;
                    break;
                }
            }
        }

        if (shouldExit) {

            if (debug) {
                System.out.println("Short strike hit!");
            }

            if (Strategy.StrategyType.RATIO_DIAGONAL_2by1.equals(strategy.getStrategyType()) && hitCoeff != -1) {
                remove(position, hitOption.getOptionV2Id().getSymbol(), debug);

                if (hitCoeff == -2) {
                    hitCoeff = -1;
                }

                List<OptionV2> dateOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(hitOption.getUnderlying(), hitOption.getGreeks_updated_at());
                add(Strategy.getDelta(strategy.getLegs().get(1)), Strategy.getDaysToExpiry(strategy.getLegs().get(1)), hitOption.getOption_type(), dateOptions, position, hitCoeff, hitOption.getGreeks_updated_at(), debug);
            } else {
                remove(position, debug);
            }
        }
    }

    private void roll(Strategy strategy, FlowPosition position, boolean debug) {

        if (Strategy.RollingStrategy.NONE.equals(strategy.getRollingStrategy())) {
            return;
        }

        if (position.getItemsMap().size() == 0) {
            return;
        }

        OptionV2 option = position.getItemsMap().values().stream().findFirst().get().getOptionV2();

        int shortDelta = (int) Strategy.getDelta(strategy.getLegs().get(1));
        int daysToExpiry = Strategy.getDaysToExpiry(strategy.getLegs().get(1));
        OptionV2.OptionType optionType = Strategy.getOptionType(strategy.getLegs().get(1));

        String stockSymbol = option.getUnderlying();
        Date date = option.getGreeks_updated_at();

        int longDelta = 0;
        int newCoeff = 0;

        Set<String> optionsSymbolsToRemove = new HashSet<>();
        String firstOptionSymbol = null;

        for (FlowPosition.Item item : position.getItemsMap().values()) {

            if (firstOptionSymbol == null) {
                firstOptionSymbol = item.getOptionV2().getOptionV2Id().getSymbol();
            }

            if (item.getCoeff() > 0) {
                longDelta = (int) (item.getCoeff() * item.getOptionV2().getDelta() * position.getContractSize());
            }
            if (item.getCoeff() < 0) {
                optionsSymbolsToRemove.add(item.getOptionV2().getOptionV2Id().getSymbol());
            }
            if (item.getCoeff() < 0 || (position.getItemsMap().size() < strategy.getLegs().size() && !strategy.getStrategyType().equals(Strategy.StrategyType.SIMPLE))) {

                newCoeff = Strategy.getCoeff(strategy.getLegs().get(1));

                double totalDelta = longDelta + newCoeff * shortDelta;

                if (Strategy.StrategyType.RATIO_DIAGONAL.equals(strategy.getStrategyType())) {
                    newCoeff = -1 * ((longDelta) / shortDelta);

                    if (totalDelta <= 50 && newCoeff != -1) {
                        newCoeff++;
                    }

                }

                if (Strategy.StrategyType.RATIO_DIAGONAL_2by1.equals(strategy.getStrategyType())) {

                    if (totalDelta >= 140) {
                        newCoeff = -2;
                    }
                }

                if (Strategy.StrategyType.RATIO_DIAGONAL_6by1.equals(strategy.getStrategyType())) {

                    if (totalDelta >= 60) {
                        remove(position, firstOptionSymbol, 1, debug);
                    }
                }

/**
 newCoeff = -1 * ((longDelta) / shortDelta);

 if (newCoeff == 0) {
 addToRemoved(position, optionsSymbolsToRemove);
 }

 if (totalDelta <= 50 && newCoeff != -1) {
 newCoeff++;
 }
 }
 **/

            }

            if (strategy.getRollingStrategy().equals(Strategy.RollingStrategy.ROLL_SAME_STRIKE)) {
                shortDelta = DELTA_ROLL_SAME_STRIKE;
            }

            if (newCoeff < 0) {

                List<OptionV2> dateOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

                if (dateOptions.size() == 0) {
                    return;
                }
                if (debug) {
                    System.out.println("Rolling");
                }

                add(shortDelta, daysToExpiry, optionType, dateOptions, position, newCoeff, date, debug);

                break;
            }
        }

        for (String optionsSymbolToRemove : optionsSymbolsToRemove) {
            remove(position, optionsSymbolToRemove, debug);
        }
    }

    private Map<Date, StockHistory> getStockHistoryMap(final String stockSymbol) {

        Map<Date, StockHistory> result = new TreeMap<>();

        List<StockHistory> stockHistories = persistenceDelegate.getStockHistoryRepository().findByStockHistoryIdSymbol(stockSymbol);

        for (StockHistory stockHistory : stockHistories) {
            result.put(stockHistory.getStockHistoryId().getDate(), stockHistory);
        }
        return result;
    }


    private String getBalance(Date date) {
        return "Balance $" + balanceMap.get(date);
    }

    protected LocalDate convertToLocalDate(Date dateToConvert) {
        return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    protected int getDayOfWeek(LocalDate date) {
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
