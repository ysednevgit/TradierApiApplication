package com.yury.trade.delegate;

import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.StockHistory;
import com.yury.trade.util.Strategy;
import com.yury.trade.util.StrategyTester;
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

    public void getStats() throws ParseException {

        String stockSymbol = "SPY";
        Date date = sdf.parse("2022-07-22");

        Iterable<OptionV2> allOptions = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

        Map<Date, Double> stockHistoryMap = getStockHistoryMap(stockSymbol);

        List<Strategy> strategies = new StrategyTester().getStrategiesToTest();

        for (Strategy strategy : strategies) {
            System.out.println("Checking strategy: " + strategy.getName() + " " + strategy.getDescription());

            Position position = new Position();

            List<List<OptionV2>> legOptionsList = new ArrayList<>();

            int maxOptionsAmount = 0;

            List<OptionV2> firstStepOptions = new ArrayList<>();

            for (int i = 0; i < strategy.getLegs().size(); i++) {

                OptionV2 option = getClosest(strategy.getLegs().get(i), firstStepOptions, allOptions);

                firstStepOptions.add(option);

                position.coeffs.add(getCoeff(strategy.getLegs().get(i)));

                List<OptionV2> legOptionsList_i = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option.getOptionV2Id().getSymbol());
                maxOptionsAmount = Math.max(maxOptionsAmount, legOptionsList_i.size());

                legOptionsList.add(legOptionsList_i);
            }

            double initialPrice = 0;
            double initialStockPrice = 0;
            double change = 0;

            Date stepDate = null;

            for (int i = 0; i < maxOptionsAmount; i++) {

                stepDate = legOptionsList.get(0).get(i).getGreeks_updated_at();

                for (int j = 0; j < legOptionsList.size(); j++) {

                    List<OptionV2> legOptionsList_j = legOptionsList.get(j);

                    int coeff = position.coeffs.get(j);

                    if (legOptionsList_j.size() == i) {

                        //Rolling to next option
                        if (!strategy.isShouldRoll()) {
                            continue;
                        }

                        boolean rollSuccessful = rollPosition(position, strategy, legOptionsList_j, i, j, allOptions);

                        if (rollSuccessful) {
                            position.adjustments *= coeff;
                        }
                    }

                    if (legOptionsList_j.size() <= i) {
                        continue;
                    }
                }

                boolean positionAddSuccessful = position.add(legOptionsList, i);

                if (!positionAddSuccessful) {
                    return;
                }

                System.out.print("Date " + sdf.format(stepDate) + " ");

                if (i == 0) {
                    initialPrice = position.positionPrice;
                    initialStockPrice = stockHistoryMap.get(stepDate);
                } else {
                    change = (position.positionPrice - position.adjustments / position.contractSize) / initialPrice;
                }

                double stockPrice = stockHistoryMap.get(stepDate);
                double changeValue = (position.positionPrice - initialPrice) * position.contractSize - position.adjustments;

                System.out.print(position);

                System.out.println("Change " + df2.format(change) + "(" + df2.format(changeValue) + ")  " +
                        stockSymbol + " " + stockPrice + " change " + df2.format(stockPrice / initialStockPrice));
            }
            System.out.println();
        }

        System.out.println("Get stats End.");
    }

    private boolean rollPosition(final Position position, final Strategy strategy, List<OptionV2> legOptionsList_j, int i, int j, Iterable<OptionV2> options) {

        OptionV2 newOption = null;
        OptionV2 originalOption = legOptionsList_j.get(i - 1);

        if (Strategy.RollingStrategy.ROLL_SAME_STRIKE.equals(strategy.getRollingStrategy())) {
            newOption = getNextWithSameStrike(originalOption, getDaysToExpiry(strategy.getLegs().get(j)), options);
        } else if (Strategy.RollingStrategy.ROLL_SAME_DELTA.equals(strategy.getRollingStrategy())) {
            newOption = getNextWithSameDelta(originalOption, getDaysToExpiry(strategy.getLegs().get(j)), options);
        }

        if (newOption.getGreeks_updated_at().before(originalOption.getGreeks_updated_at())) {
            return false;
        }

        System.out.println("Rolling to new option " + newOption);

        double priceChange = (newOption.getMid_price() - originalOption.getMid_price()) * originalOption.getContract_size();

        position.adjustments += priceChange;

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

    private int getDaysToExpiry(String leg) {
        return Integer.parseInt(leg.split(" ")[3]);
    }

    private OptionV2 getClosest(final String leg, final List<OptionV2> legOptions, final Iterable<OptionV2> options) {

        if (leg == null) {
            return null;
        }

        // 1 C 20 300  - means buy 1 call 20 delta with 300 days out
        String[] words = leg.split(" ");

        int daysToExpiry = Integer.parseInt(words[3]);
        double delta = Double.parseDouble(words[2]);
        OptionV2.OptionType optionType = "C".equals(words[1]) ? OptionV2.OptionType.call : OptionV2.OptionType.put;

        Double strikePrice = null;

        if (delta == 0 && legOptions.size() > 0) {
            strikePrice = legOptions.get(legOptions.size() - 1).getStrike();
        }

        return getClosest(delta, daysToExpiry, optionType, strikePrice, options);
    }

    private OptionV2 getClosest(double delta, final int days_to_expiry, final OptionV2.OptionType optionType, final Double strikePrice, final Iterable<OptionV2> options) {

        if (options == null || !options.iterator().hasNext()) {
            return null;
        }

        delta = delta / 100;

        OptionV2 result = null;

        for (OptionV2 optionV2 : options) {

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

    private OptionV2 getNextWithSameStrike(final OptionV2 option, final int days_to_expiry, final Iterable<OptionV2> options) {
        return getClosest(0, days_to_expiry * 2, option.getOption_type(), option.getStrike(), options);
    }

    private OptionV2 getNextWithSameDelta(final OptionV2 option, final int days_to_expiry, final Iterable<OptionV2> options) {
        return getClosest(option.getDelta(), days_to_expiry * 2, option.getOption_type(), null, options);
    }

    private class Position {

        List<OptionV2> options = new ArrayList<>();
        List<Integer> coeffs = new ArrayList<>();

        int contractSize = 100;

        double positionDelta = 0;
        double positionTheta = 0;
        double positionGamma = 0;
        double positionPrice = 0;
        double adjustments = 0;

        boolean add(final List<List<OptionV2>> legOptionsList, int index) {

            Date updated = legOptionsList.get(0).get(index).getGreeks_updated_at();

            if (legOptionsList.size() != coeffs.size()) {
                return false;
            }

            for (List<OptionV2> legOptions : legOptionsList) {

                if (legOptions.size() <= index || !updated.equals(legOptions.get(index).getGreeks_updated_at())) {
                    return false;
                }
            }

            options.clear();

            for (List<OptionV2> legOptions : legOptionsList) {
                options.add(legOptions.get(index));
            }
            calc();

            return true;
        }

        private void calc() {
            positionDelta = 0;
            positionTheta = 0;
            positionGamma = 0;
            positionPrice = 0;

            for (int i = 0; i < options.size(); i++) {
                OptionV2 option = options.get(i);
                int coeff = coeffs.get(i);

                positionPrice += option.getMid_price() * coeff;
                positionTheta += option.getTheta() * coeff;
                positionGamma += option.getGamma() * coeff;
                positionDelta += option.getDelta() * coeff;
            }
        }

        @Override
        public String toString() {
            return "Position{ " +
                    "delta=" + df2.format(contractSize * positionDelta) +
                    ", theta=" + df2.format(contractSize * positionTheta) +
                    ", gamma=" + df2.format(contractSize * positionGamma) +
                    ", price=" + df2.format(contractSize * positionPrice) +
                    ", adjustments=" + df2.format(adjustments) +
                    ", options=" + options +
                    "} ";
        }
    }

}