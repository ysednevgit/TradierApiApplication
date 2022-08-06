package com.yury.trade.delegate;

import com.yury.trade.entity.*;
import com.yury.trade.util.Position;
import com.yury.trade.util.Strategy;
import com.yury.trade.util.StrategyTester;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private Map<StrategyPerformanceId, StrategyPerformance> strategyPerformanceMap = new LinkedHashMap<>();
    private Map<StrategyPerformanceId, StrategyPerformanceData> strategyPerformanceDataMap = new LinkedHashMap<>();

    public void getStats(String symbol) throws ParseException {

        strategyPerformanceMap.clear();
        strategyPerformanceDataMap.clear();

        Date startDate = sdf.parse("2022-07-25");

        List<String> stockSymbols = new ArrayList<>();

        if (symbol != null) {
            stockSymbols.add(symbol);
        } else {
            stockSymbols = persistenceDelegate.getOptionRepository().findRootSymbols(startDate);
        }

        for (String stockSymbol : stockSymbols) {

            getStats(stockSymbol, startDate);
        }

        persistenceDelegate.getStrategyPerformanceRepository().saveAll(strategyPerformanceMap.values());
        persistenceDelegate.getStrategyPerformanceDataRepository().saveAll(strategyPerformanceDataMap.values());

        System.out.println("Get stats End.");
    }

    private void getStats(String stockSymbol, Date startDate) {

        List<OptionV2> allOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, startDate);

        Map<Date, Double> stockHistoryMap = getStockHistoryMap(stockSymbol);

        List<Strategy> strategies = new StrategyTester().getStrategiesToTest();

        for (Strategy strategy : strategies) {
            System.out.println("Strategy: " + strategy);

            Position position = new Position();

            List<List<OptionV2>> legOptionsList = new ArrayList<>();

            int maxOptionsAmount = 0;
            double maxDrawDown = 0;
            int maxDrawDownValue = 0;

            List<OptionV2> firstStepOptions = new ArrayList<>();

            for (int i = 0; i < strategy.getLegs().size(); i++) {

                OptionV2 option = getClosest(strategy.getLegs().get(i), firstStepOptions, allOptions);

                firstStepOptions.add(option);

                position.addCoeff(getCoeff(strategy.getLegs().get(i)));

                List<OptionV2> legOptionsList_i = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbolWithGreaterOrSameUpdated(option.getOptionV2Id().getSymbol(), startDate);
                maxOptionsAmount = Math.max(maxOptionsAmount, legOptionsList_i.size());

                legOptionsList.add(legOptionsList_i);
            }

            double initialPrice = 0;
            double initialStockPrice = 0;
            double change = 0;

            Date stepDate;

            for (int i = 0; i < maxOptionsAmount; i++) {

                stepDate = legOptionsList.get(0).get(i).getGreeks_updated_at();
                position.daysRun++;

                for (int j = 0; j < legOptionsList.size(); j++) {

                    List<OptionV2> legOptionsList_j = legOptionsList.get(j);

                    if (legOptionsList_j.size() == i) {

                        //Rolling to next option
                        if (Strategy.RollingStrategy.NONE.equals(strategy.getRollingStrategy())) {
                            break;
                        }

                        rollPosition(position, strategy, legOptionsList_j, i, j);
                    }

                    if (legOptionsList_j.size() <= i) {
                        continue;
                    }

                    //adding check for bad data
                    if (legOptionsList_j.get(i).getMid_price() == 0) {
                        continue;
                    }
                }

                boolean positionAddSuccessful = position.add(legOptionsList, i);

                if (!positionAddSuccessful) {
                    continue;
                }

                System.out.print(sdf.format(stepDate) + " ");

                if (i == 0) {
                    initialPrice = position.positionPrice;
                    initialStockPrice = stockHistoryMap.get(stepDate);
                } else {
                    change = (position.positionPrice - position.adjustments / position.contractSize) / initialPrice;
                }

                double stockPrice = stockHistoryMap.get(stepDate);
                double changeValue = (position.positionPrice - initialPrice) * position.contractSize - position.adjustments;

                if (changeValue < maxDrawDownValue) {
                    maxDrawDownValue = (int) changeValue;
                    maxDrawDown = maxDrawDownValue / (initialPrice * position.contractSize);
                }

                System.out.print(position);

                String changeStr = "Change " + df2.format(change) + "(" + df2.format(changeValue) + ")  " +
                        stockSymbol + " " + stockPrice + " change " + df2.format(stockPrice / initialStockPrice);

                System.out.println(changeStr);

                StrategyPerformance strategyPerformance = new StrategyPerformance();
                StrategyPerformanceId strategyPerformanceId = new StrategyPerformanceId();
                strategyPerformance.setStrategyPerformanceId(strategyPerformanceId);

                strategyPerformanceId.setSymbol(stockSymbol);
                strategyPerformanceId.setStartDate(startDate);
                strategyPerformanceId.setStrategyDescription(strategy.toString());
                strategyPerformance.setChange(Precision.round(change, 2));
                strategyPerformance.setChangeValue((int) changeValue);
                strategyPerformance.setMaxDrawDown(Precision.round(maxDrawDown, 2));
                strategyPerformance.setMaxDrawDownValue(maxDrawDownValue);
                strategyPerformance.setDaysRun(position.daysRun);
                strategyPerformance.setStockLatest(stockPrice);
                strategyPerformance.setStockChange(Precision.round(stockPrice / initialStockPrice, 2));
                strategyPerformance.setStrategyType(strategy.getStrategyType().name());
                strategyPerformance.setIndex(new Date().getTime());

                strategyPerformanceMap.put(strategyPerformanceId, strategyPerformance);

                if (strategyPerformanceDataMap.containsKey(strategyPerformanceId)) {
                    StrategyPerformanceData strategyPerformanceData = strategyPerformanceDataMap.get(strategyPerformanceId);
                    strategyPerformanceData.addData(sdf.format(stepDate) + " " + position + " " + changeStr);
                } else {
                    StrategyPerformanceData strategyPerformanceData = new StrategyPerformanceData();
                    strategyPerformanceData.setId(strategyPerformance.getIndex());
                    strategyPerformanceData.addData(strategy.toString());
                    strategyPerformanceDataMap.put(strategyPerformanceId, strategyPerformanceData);
                }
            }
            System.out.println();

        }
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

        position.roll(j);

        System.out.println("Rolling to new option " + newOption);

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

    private OptionV2 getClosest(double delta,
                                final int days_to_expiry,
                                final OptionV2.OptionType optionType,
                                final Double strikePrice,
                                final Date minUpdated,
                                final List<OptionV2> options) {

        if (options == null || !options.iterator().hasNext()) {
            return null;
        }

        delta = delta / 100;

        OptionV2 result = null;

        for (OptionV2 optionV2 : options) {

            if (minUpdated != null && minUpdated.after(optionV2.getGreeks_updated_at())) {
                continue;
            }

            //     options.stream().filter(o -> o.getStrike().equals(strikePrice)).filter(o -> o.getOption_type().equals(optionType)).collect(Collectors.toList());
            //    options.stream().filter(o -> o.getStrike().equals(strikePrice)).filter(o -> o.getGreeks_updated_at().after(minUpdated)).collect(Collectors.toList());

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

                if ((daysDistance == resultDaysDistance) && (deltaDistance < resultDeltaDistance)) {
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