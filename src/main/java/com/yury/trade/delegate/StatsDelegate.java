package com.yury.trade.delegate;

import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.StockHistory;
import com.yury.trade.entity.Strategy;
import com.yury.trade.util.StrategyTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    public void getStats() throws ParseException {

        String stockSymbol = "SPY";
        Date date = sdf.parse("2022-07-22");

        Iterable<OptionV2> options = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

        Map<Date, Double> stockHistoryMap = getStockHistoryMap(stockSymbol);

        //       List<Strategy> strategies = persistenceDelegate.getStrategyRepository().findAll());
        List<Strategy> strategies = new StrategyTester().getStrategiesToTest();


        for (Strategy strategy : strategies) {
            System.out.println("Checking strategy: " + strategy.getName() + " " + strategy.getDescription());

            OptionV2 option1, option2 = null, option3 = null, option4 = null;
            int coeff1, coeff2 = 0, coeff3 = 0, coeff4 = 0;

            option1 = getClosest(strategy.getLeg1(), options);
            coeff1 = getCoeff(strategy.getLeg1());

            System.out.println(getInfo(coeff1, option1));

            if (strategy.getLeg2() != null) {
                option2 = getClosest(strategy.getLeg2(), option1.getStrike(), options);
                coeff2 = getCoeff(strategy.getLeg2());
                System.out.println(getInfo(coeff2, option2));
            }

            if (strategy.getLeg3() != null) {
                option3 = getClosest(strategy.getLeg3(), options);
                coeff3 = getCoeff(strategy.getLeg3());
                System.out.println(getInfo(coeff3, option3));
            }

            if (strategy.getLeg4() != null) {
                option4 = getClosest(strategy.getLeg4(), option3.getStrike(), options);
                coeff4 = getCoeff(strategy.getLeg4());
                System.out.println(getInfo(coeff4, option4));
            }

            List<OptionV2> options1, options2 = null, options3 = null, options4 = null;

            options1 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option1.getOptionV2Id().getSymbol());

            if (option2 != null) {
                options2 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option2.getOptionV2Id().getSymbol());
            }
            if (option3 != null) {
                options3 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option3.getOptionV2Id().getSymbol());
            }
            if (option4 != null) {
                options4 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option4.getOptionV2Id().getSymbol());
            }

            double initialPrice = 0;
            double initialStockPrice = 0;
            double change = 0;

            double positionDelta = 0;
            double positionTheta = 0;
            double positionGamma = 0;

            Date stepDate = null;

            for (int i = 0; i < options1.size(); i++) {

                double price = options1.get(i).getMid_price() * coeff1;

                positionDelta = options1.get(i).getDelta() * coeff1;
                positionTheta = options1.get(i).getTheta() * coeff1;
                positionGamma = options1.get(i).getGamma() * coeff1;
                stepDate = options1.get(i).getGreeks_updated_at();

                if (options2 != null) {
                    price += options2.get(i).getMid_price() * coeff2;

                    positionDelta += options2.get(i).getDelta() * coeff2;
                    positionTheta += options2.get(i).getTheta() * coeff2;
                    positionGamma += options2.get(i).getGamma() * coeff2;
                }

                if (options3 != null) {
                    price += options3.get(i).getMid_price() * coeff3;

                    positionDelta += options3.get(i).getDelta() * coeff3;
                    positionTheta += options3.get(i).getTheta() * coeff3;
                    positionGamma += options3.get(i).getGamma() * coeff3;
                }

                if (options4 != null) {
                    price += options4.get(i).getMid_price() * coeff4;

                    positionDelta += options4.get(i).getDelta() * coeff4;
                    positionTheta += options4.get(i).getTheta() * coeff4;
                    positionGamma += options4.get(i).getGamma() * coeff4;
                }

                price *= option1.getContract_size();
                positionDelta *= option1.getContract_size();
                positionTheta *= option1.getContract_size();
                positionGamma *= option1.getContract_size();

                System.out.print("Date " + sdf.format(stepDate) + " ");

                if (i == 0) {
                    initialPrice = price;
                    initialStockPrice = stockHistoryMap.get(stepDate);
                } else {
                    change = price / initialPrice;
                }

                double stockPrice = stockHistoryMap.get(stepDate);

                System.out.print("Position delta: " + df2.format(positionDelta) + " theta: " + df2.format(positionTheta) + " gamma: " + df2.format(positionGamma));

                System.out.println(" Price:" + df2.format(price) + " change " + df2.format(change) + "(" + df2.format(price - initialPrice) + ")  " +
                        stockSymbol + " " + stockPrice + " change " + df2.format(stockPrice / initialStockPrice));
            }
            System.out.println();
        }

        System.out.println("Get stats End.");
    }

    private String getInfo(int coef, OptionV2 optionV2) {
        return (coef + " " + optionV2.getOptionV2Id().getSymbol() + "  " + optionV2.getDays_left() + " days left");
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

    private OptionV2 getClosest(String leg, Double strikePrice, final Iterable<OptionV2> options) {

        if (leg == null) {
            return null;
        }

        // 1 C 20 300  - means buy 1 call 20 delta with 300 days out
        String[] words = leg.split(" ");

        int daysToExpiry = Integer.parseInt(words[3]);
        double delta = Double.parseDouble(words[2]);
        OptionV2.OptionType optionType = "C".equals(words[1]) ? OptionV2.OptionType.call : OptionV2.OptionType.put;

        if (delta != 0) {
            strikePrice = null;
        }

        return getClosest(delta, daysToExpiry, optionType, strikePrice, options);
    }

    private OptionV2 getClosest(String leg, final Iterable<OptionV2> options) {
        return getClosest(leg, null, options);
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

}