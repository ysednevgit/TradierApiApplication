package com.yury.trade.delegate;

import com.yury.trade.entity.*;
import com.yury.trade.util.*;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private static final int MIN_DAYS_TO_CONTINUE = 2;

    public void getStats(final String symbol, final String startDateString, boolean debug, boolean test) throws Exception {

        Map<StrategyPerformanceId, StrategyPerformance> strategyPerformanceMap = new LinkedHashMap<>();

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        List<SymbolWithDate> symbolsWithMinDates = persistenceDelegate.getOptionRepository().findRootSymbolsWithMinDates();

        Map<StrategyPerformanceId, StrategyPerformanceTotal> strategyPerformanceTotalMap = new LinkedHashMap<>();

        for (SymbolWithDate symbolWithDate : symbolsWithMinDates) {

            if (symbol != null && !symbol.equals(symbolWithDate.getSymbol())) {
                continue;
            }

            Date runStartDate = symbolWithDate.getDate();

            if (startDate != null && runStartDate.before(startDate)) {
                runStartDate = startDate;
            }

            List<Strategy> strategies = test ? new StrategyTester().getTestStrategiesToTest() : new StrategyTester().getStrategiesToTest();

            for (Strategy strategy : strategies) {

                Date newRunStartDate = runStartDate;

                if (strategy.getChangeStartDateByDays() != 0) {
                    newRunStartDate = new Date(runStartDate.getTime() + strategy.getChangeStartDateByDays() * 24 * 60 * 60 * 1000);
                }

                StrategyRunData strategyRunData = getStats(null, symbolWithDate.getSymbol(), newRunStartDate, strategy, debug);

                if (strategyRunData.getStrategyPerformanceMap().size() > 0) {
                    StrategyPerformanceTotal strategyPerformanceTotal = createStrategyPerformanceTotal(strategyRunData);
                    strategyPerformanceTotalMap.put(strategyPerformanceTotal.getStrategyPerformanceId(), strategyPerformanceTotal);

                    strategyPerformanceMap.putAll(strategyRunData.getStrategyPerformanceMap());
                }

            }
        }

        persistenceDelegate.getStrategyPerformanceRepository().saveAll(strategyPerformanceMap.values());
        System.out.println("Saved to StrategyPerformance " + strategyPerformanceMap.values().size());

        persistenceDelegate.getStrategyPerformanceTotalRepository().saveAll(strategyPerformanceTotalMap.values());
        System.out.println("Saved to StrategyPerformanceTotal " + strategyPerformanceTotalMap.values().size());

        System.out.println("Get stats End.");
    }

    private StrategyRunData getStats(StrategyRunData strategyRunData, String stockSymbol, Date startDate, Strategy strategy, boolean debug) throws InterruptedException {

        System.out.println();

        System.out.println("Get stats: " + stockSymbol + " " + startDate + " " + "Strategy: " + strategy + " " + strategy.getStrategyType());

        List<OptionV2> allOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, startDate);

        if (strategyRunData == null) {
            strategyRunData = new StrategyRunData();
            strategyRunData.setStrategy(strategy);
            strategyRunData.setStartDate(startDate);

        }

        if (allOptions.size() == 0) {
            return strategyRunData;
        }

        Map<Date, Double> stockHistoryMap = getStockHistoryMap(stockSymbol);

        Position position = new Position();

        List<List<OptionV2>> legOptionsList = new ArrayList<>();

        int maxOptionsAmount = 0;
        double maxDrawDown = 0;
        int maxDrawDownValue = 0;

        double thetaTotal = 0;

        List<OptionV2> firstStepOptions = new ArrayList<>();

        for (int i = 0; i < strategy.getLegs().size(); i++) {

            OptionV2 option = getClosest(strategy.getLegs().get(i), firstStepOptions, allOptions);

            firstStepOptions.add(option);

            position.addCoeff(getCoeff(strategy.getLegs().get(i)));

            List<OptionV2> legOptionsList_i = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterOrSameUpdated(option.getOptionV2Id().getSymbol(), startDate);
            maxOptionsAmount = Math.max(maxOptionsAmount, legOptionsList_i.size());

            legOptionsList.add(legOptionsList_i);
        }

        if (legOptionsList.get(0).size() == 1) {
            return strategyRunData;
        }

        double initialPrice = 0;
        double initialStockPrice = 0;
        double change = 0;

        Date stepDate = null;

        for (int i = 0; i < maxOptionsAmount; i++) {

            if (legOptionsList.get(0).size() < maxOptionsAmount) {
                break;
            }

            stepDate = legOptionsList.get(0).get(i).getGreeks_updated_at();
            position.daysRun++;

            boolean shouldBreak = false;

            for (int j = 0; j < legOptionsList.size(); j++) {

                List<OptionV2> legOptionsList_j = legOptionsList.get(j);

                if (legOptionsList_j.size() == i) {

                    //Rolling to next option
                    if (Strategy.RollingStrategy.NONE.equals(strategy.getRollingStrategy())) {
                        shouldBreak = true;
                        break;
                    }

                    if (!rollPosition(position, strategy, legOptionsList_j, i, j)) {
                        break;
                    }
                }

                if (legOptionsList_j.size() <= i) {
                    continue;
                }

                //adding check for bad data
                if (legOptionsList_j.get(i).getMid_price() == null || legOptionsList_j.get(i).getMid_price() == 0) {
                    continue;
                }
            }

            if (shouldBreak) {
                break;
            }

            boolean positionAddSuccessful = position.add(legOptionsList, i);

            if (!positionAddSuccessful) {

                continue;
            }

            if (initialStockPrice == 0) {
                initialPrice = position.positionPrice;
                initialStockPrice = stockHistoryMap.get(stepDate);
            } else {
                if (initialPrice == 0) {
                    //usually means bought and sold same options
                    break;
                }
                change = (position.positionPrice - position.adjustments / position.contractSize) / initialPrice;
            }

            if (strategyRunData.getInitialStockPrice() == null) {
                strategyRunData.setInitialStockPrice(initialStockPrice);
            }

            double stockPrice = stockHistoryMap.get(stepDate);
            double changeValue = (position.positionPrice - initialPrice) * position.contractSize - position.adjustments;

            thetaTotal += position.positionTheta * position.contractSize;

            if (changeValue < maxDrawDownValue) {
                maxDrawDownValue = (int) changeValue;
                maxDrawDown = maxDrawDownValue / (initialPrice * position.contractSize);
            }

            String changeStr = "Change " + df2.format(change) + "(" + df2.format(changeValue) + ")  " + stockSymbol + " " + stockPrice + " change " + df2.format(stockPrice / initialStockPrice);

            if (debug) {
                System.out.println(sdf.format(stepDate) + " " + position + " " + changeStr);
            }

            StrategyPerformance strategyPerformance = createStrategyPerformance(strategy, stockSymbol, startDate, change, changeValue, maxDrawDown, maxDrawDownValue, position, stockPrice, initialStockPrice, initialPrice * position.contractSize, thetaTotal);

            addToStrategyPerformance(strategyRunData, strategyPerformance, stepDate, position, changeStr, strategy, changeValue);

            if (shouldExit(strategy, change)) {
                break;
            }
        }

        if (stepDate != null) {
            Date checkDate = new Date(stepDate.getTime() + MIN_DAYS_TO_CONTINUE * 24 * 60 * 60 * 1000);

            if (checkDate.before(new Date())) {
                return getStats(strategyRunData, stockSymbol, stepDate, strategy, debug);
            }
        }

        return strategyRunData;
    }

    private boolean shouldExit(Strategy strategy, double change) {
        if (Strategy.ExitStrategy._10_PERCENT_PROFIT.equals(strategy.getExitStrategy()) && change >= 1.1) {
            return true;
        }
        if (Strategy.ExitStrategy._20_PERCENT_PROFIT.equals(strategy.getExitStrategy()) && change >= 1.2) {
            return true;
        }
        if (Strategy.ExitStrategy._50_PERCENT_PROFIT.equals(strategy.getExitStrategy()) && change >= 1.5) {
            return true;
        }
        return false;
    }

    private StrategyPerformanceTotal createStrategyPerformanceTotal(StrategyRunData strategyRunData) {
        StrategyPerformanceTotal strategyPerformanceTotal = new StrategyPerformanceTotal();

        Double thetaTotal = 0d;
        Integer changeValue = 0;
        Integer maxDrawDownValue = 0;
        Double stockLatestPrice = 0d;
        double averageChange = 0;
        double initialPriceTotal = 0;
        double changeValueTotal = 0;

        String indexes = "";
        int wins = 0;

        String chartData = "";

        for (StrategyPerformance strategyPerformance : strategyRunData.getStrategyPerformanceMap().values()) {
            if (strategyPerformanceTotal.getStrategyPerformanceId() == null) {
                strategyPerformanceTotal.setStrategyPerformanceId(strategyPerformance.getStrategyPerformanceId());
                strategyPerformanceTotal.setStrategyType(strategyPerformance.getStrategyType());
            }

            stockLatestPrice = strategyPerformance.getStockLatest();

            maxDrawDownValue = Math.min(maxDrawDownValue, strategyPerformance.getMaxDrawDownValue());

            thetaTotal += strategyPerformance.getThetaTotal();
            changeValue += strategyPerformance.getChangeValue();
            averageChange += strategyPerformance.getChange();
            initialPriceTotal += strategyPerformance.getInitialPrice();

            chartData += getAdjustedChartValue(strategyPerformance.getChartData(), changeValueTotal);

            changeValueTotal += strategyPerformance.getChangeValue();

            if (strategyPerformance.getChangeValue() > 0) {
                wins++;
            }

            indexes += "" + strategyPerformance.getIndex() + "\n";
        }

        double finalPriceTotal = initialPriceTotal + changeValueTotal;

        strategyPerformanceTotal.setStockInitial(strategyRunData.getInitialStockPrice());
        strategyPerformanceTotal.setThetaTotal(Precision.round(thetaTotal, 2));
        strategyPerformanceTotal.setChangeValue(changeValue);
        strategyPerformanceTotal.setRuns(strategyRunData.getStrategyPerformanceMap().size());
        strategyPerformanceTotal.setWins(wins);
        strategyPerformanceTotal.setAvgChange(Precision.round(averageChange / strategyPerformanceTotal.getRuns(), 2));
        strategyPerformanceTotal.setChange(Precision.round(finalPriceTotal / initialPriceTotal, 2));
        strategyPerformanceTotal.setMaxDrawDownValue(maxDrawDownValue);
        strategyPerformanceTotal.setStockChange(Precision.round(stockLatestPrice / strategyRunData.getInitialStockPrice(), 2));
        strategyPerformanceTotal.setStockLatest(stockLatestPrice);
        strategyPerformanceTotal.setDataIds(indexes);
        strategyPerformanceTotal.setChartData(chartData);

        return strategyPerformanceTotal;
    }

    private String getAdjustedChartValue(String chartValue, double adjustment) {
        if (adjustment == 0) {
            return chartValue;
        }

        StringBuilder result = new StringBuilder();
        for (String part : chartValue.split("\n")) {
            String[] words = part.split(",");
            result.append(words[0]).append(",").append((int) (Double.parseDouble(words[1]) + adjustment)).append("\n");
        }
        return result.toString();
    }

    private StrategyPerformance createStrategyPerformance(Strategy strategy, String stockSymbol, Date startDate, Double change, double changeValue, Double maxDrawDown, Integer maxDrawDownValue, Position position, double stockPrice, double initialStockPrice, double initialPrice, double thetaTotal) throws InterruptedException {

        StrategyPerformance strategyPerformance = new StrategyPerformance();
        StrategyPerformanceId strategyPerformanceId = new StrategyPerformanceId();
        strategyPerformance.setStrategyPerformanceId(strategyPerformanceId);

        strategyPerformanceId.setSymbol(stockSymbol);
        strategyPerformanceId.setStartDate(startDate);
        strategyPerformanceId.setStrategyDescription(strategy.toString());
        strategyPerformance.setChange(Precision.round(change, 2));
        strategyPerformance.setChangeValue((int) changeValue);

        strategyPerformance.setThetaTotal(Precision.round(thetaTotal, 2));

        strategyPerformance.setMaxDrawDown(Precision.round(maxDrawDown, 2));
        strategyPerformance.setMaxDrawDownValue(maxDrawDownValue);
        strategyPerformance.setDaysRun(position.daysRun);
        strategyPerformance.setStockLatest(stockPrice);
        strategyPerformance.setStockChange(Precision.round(stockPrice / initialStockPrice, 2));
        strategyPerformance.setInitialPrice(Precision.round(initialPrice, 2));
        strategyPerformance.setStrategyType(strategy.getStrategyType().name());
        strategyPerformance.setIndex(new Date().getTime());
        Thread.sleep(1);

        return strategyPerformance;
    }

    private void addToStrategyPerformance(StrategyRunData strategyRunData, StrategyPerformance strategyPerformance, Date stepDate, Position position, String changeStr, Strategy strategy, double changeValue) {
        StrategyPerformanceId strategyPerformanceId = strategyPerformance.getStrategyPerformanceId();

        String data = sdf.format(stepDate) + " " + position + " " + changeStr;
        String chartData = sdf.format(stepDate) + "," + (int) (changeValue);

        if (strategyRunData.getStrategyPerformanceMap().containsKey(strategyPerformanceId)) {
            StrategyPerformance oldStrategyPerformance = strategyRunData.getStrategyPerformanceMap().get(strategyPerformanceId);
            strategyPerformance.addData(oldStrategyPerformance.getData() + data);
            strategyPerformance.addChartData(oldStrategyPerformance.getChartData() + chartData);
        } else {
            strategyPerformance.addData(strategy.toString());
            strategyPerformance.addData(data);
            strategyPerformance.addChartData(chartData);
        }
        strategyRunData.getStrategyPerformanceMap().put(strategyPerformanceId, strategyPerformance);
    }

    private boolean rollPosition(final Position position, final Strategy strategy, List<OptionV2> legOptionsList_j, int i, int j) {

        OptionV2 newOption = null;
        OptionV2 originalOption = legOptionsList_j.get(i - 1);

        //int rollCoeff = position.rolls.get(j);

        int rollCoeff = 0;
        Date updated = originalOption.getGreeks_updated_at();

        List<OptionV2> options;

        String leg = strategy.getLegs().get(j);

        int daysToExpiry = getDaysToExpiry(leg);

        Calendar cal = Calendar.getInstance();
        cal.setTime(originalOption.getExpiration_date());

        cal.add(Calendar.DAY_OF_MONTH, daysToExpiry);

        Date nextDate = cal.getTime();

        if (Strategy.RollingStrategy.ROLL_SAME_STRIKE.equals(strategy.getRollingStrategy())) {

            options = persistenceDelegate.getOptionRepository().findByNextByStrike(originalOption.getUnderlying(), originalOption.getStrike(), updated, nextDate);
            newOption = getNextWithSameStrike(originalOption, daysToExpiry, null, options, rollCoeff);

        } else if (Strategy.RollingStrategy.ROLL_SAME_DELTA.equals(strategy.getRollingStrategy())) {

            options = persistenceDelegate.getOptionRepository().findNext(originalOption.getUnderlying(), updated, nextDate);
            newOption = getNextWithSameDelta(getDelta(leg), originalOption.getOption_type(), getDaysToExpiry(leg), null, options, rollCoeff);
        }

        if (newOption == null || newOption.getMid_price() == 0) {
            return false;
        }

        position.roll(j);

        double priceChange = (newOption.getMid_price() - originalOption.getMid_price()) * originalOption.getContract_size();

        position.adjustments += position.coeffs.get(j) * priceChange;

        List<OptionV2> newOptions = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterUpdated(newOption.getOptionV2Id().getSymbol(), originalOption.getGreeks_updated_at());

        legOptionsList_j.addAll(newOptions);

        return true;
    }

    private Map<Date, Double> getStockHistoryMap(final String stockSymbol) {
        Map<Date, Double> result = new HashMap<>();
        List<StockHistory> stockHistories = persistenceDelegate.getStockHistoryRepository().findByStockHistoryIdSymbol(stockSymbol);

        for (StockHistory stockHistory : stockHistories) {
            result.put(stockHistory.getStockHistoryId().getDate(), stockHistory.getClose());
        }
        return result;
    }

    private int getCoeff(String leg) {
        return Integer.parseInt(leg.split(" ")[0]);
    }

    private double getDelta(String leg) {
        return Double.parseDouble(leg.split(" ")[2]);
    }

    private int getDaysToExpiry(String leg) {
        return Integer.parseInt(leg.split(" ")[3]);
    }

    private OptionV2.OptionType getOptionType(String leg) {
        return "C".equals(leg.split(" ")[1]) ? OptionV2.OptionType.call : OptionV2.OptionType.put;
    }

    private OptionV2 getClosest(final String leg, final List<OptionV2> legOptions, final List<OptionV2> options) {

        if (leg == null) {
            return null;
        }

        int daysToExpiry = getDaysToExpiry(leg);
        double delta = getDelta(leg);
        OptionV2.OptionType optionType = getOptionType(leg);

        Double strikePrice = null;

        if (delta == 0 && legOptions.size() > 0) {
            strikePrice = legOptions.get(legOptions.size() - 1).getStrike();
        }

        return getClosest(delta, daysToExpiry, optionType, strikePrice, null, options);
    }

    private OptionV2 getClosest(double delta, final int days_to_expiry, final OptionV2.OptionType optionType, final Double strikePrice, final Date minUpdated, final List<OptionV2> options) {

        if (options == null || !options.iterator().hasNext()) {
            return null;
        }

        delta = delta / 100;

        OptionV2 result = null;

        for (OptionV2 optionV2 : options) {

            if (minUpdated != null && minUpdated.after(optionV2.getGreeks_updated_at())) {
                continue;
            }

            if (strikePrice != null && !strikePrice.equals(optionV2.getStrike())) {
                continue;
            }

            boolean isCorrectOptionType = optionType.equals(optionV2.getOption_type());

            if (!isCorrectOptionType) {
                continue;
            }

            if (result == null) {

                result = optionV2;
                continue;
            }

            double resultDaysDistance = Math.abs(result.getDays_left() - days_to_expiry);

            double daysDistance = Math.abs(optionV2.getDays_left() - days_to_expiry);

            if (daysDistance < resultDaysDistance) {
                result = optionV2;
            }

            if (strikePrice == null) {
                double resultDeltaDistance = Math.abs(Math.abs(result.getDelta()) - Math.abs(delta));
                double deltaDistance = Math.abs(Math.abs(optionV2.getDelta()) - Math.abs(delta));

                if ((daysDistance == resultDaysDistance) && (deltaDistance < resultDeltaDistance) && (optionV2.getDays_left().equals(result.getDays_left()))) {
                    result = optionV2;
                }
            }

        }

        return result;
    }

    private OptionV2 getNextWithSameStrike(final OptionV2 option, final int days_to_expiry, final Date minUpdated, final List<OptionV2> options, int rollCoeff) {
        return getClosest(0, days_to_expiry * (rollCoeff + 2), option.getOption_type(), option.getStrike(), minUpdated, options);
    }

    private OptionV2 getNextWithSameDelta(final Double delta, final OptionV2.OptionType optionType, final int days_to_expiry, final Date minUpdated, final List<OptionV2> options, int rollCoeff) {
        return getClosest(delta, days_to_expiry * (rollCoeff + 2), optionType, null, minUpdated, options);
    }

}