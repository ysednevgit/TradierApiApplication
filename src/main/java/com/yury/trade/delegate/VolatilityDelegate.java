package com.yury.trade.delegate;

import com.yury.trade.entity.StockHistory;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VolatilityDelegate {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    public void calcVolatility(final String stockSymbol, final String startDateString, final String endDateString) throws ParseException {

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        Map<Date, StockHistory> stockHistoryMap = getStockHistoryMap(stockSymbol, startDate);

        Map<Date, Double> volatilityMapCtC = getHistoricVolatilityMap(stockHistoryMap, ReturnType.CLOSE_TO_CLOSE);
        Map<Date, Double> volatilityMapOtC = getHistoricVolatilityMap(stockHistoryMap, ReturnType.OPEN_TO_CLOSE);
        Map<Date, Double> volatilityMapCtO = getHistoricVolatilityMap(stockHistoryMap, ReturnType.CLOSE_TO_OPEN);

        showInfo(volatilityMapCtC, ReturnType.CLOSE_TO_CLOSE);
        showInfo(volatilityMapOtC, ReturnType.OPEN_TO_CLOSE);
        showInfo(volatilityMapCtO, ReturnType.CLOSE_TO_OPEN);
    }

    private void showInfo(Map<Date, Double> volatilityMap, ReturnType returnType) {
        System.out.println(returnType);
        System.out.println(volatilityMap);
    }

    private Map<Date, Double> getHistoricVolatilityMap(Map<Date, StockHistory> stockHistoryMap, ReturnType returnType) {

        Map<Date, Double> dailyReturnsMap = getDailyReturnsMap(stockHistoryMap, returnType);

        Map<Date, Double> standardDeviationMap = getDailyStandardDeviationMap(dailyReturnsMap);

        Map<Date, Double> historicVolatilityMap = new LinkedHashMap<>();

        for (Map.Entry<Date, Double> entry : standardDeviationMap.entrySet()) {

            double value = Precision.round(100 * entry.getValue() * Math.sqrt(252 * 24 / returnType.getHours()), 2);

            historicVolatilityMap.put(entry.getKey(), value);
        }

        return historicVolatilityMap;

    }

    private Map<Date, Double> getDailyReturnsMap(Map<Date, StockHistory> stockHistoryMap, ReturnType returnType) {

        Map<Date, Double> dailyReturnsMap = new LinkedHashMap<>();

        StockHistory previous = null;

        for (Map.Entry<Date, StockHistory> entry : stockHistoryMap.entrySet()) {

            Double value = Math.log(entry.getValue().getClose() / entry.getValue().getOpen());

            if (previous != null) {
                if (returnType.equals(ReturnType.CLOSE_TO_CLOSE)) {
                    value = Math.log(entry.getValue().getClose() / previous.getClose());
                } else if (returnType.equals(ReturnType.CLOSE_TO_OPEN)) {
                    value = Math.log(entry.getValue().getOpen() / previous.getClose());
                }
            }

            dailyReturnsMap.put(entry.getKey(), value);

            previous = entry.getValue();
        }

        return dailyReturnsMap;
    }

    private Map<Date, Double> getDailyStandardDeviationMap(Map<Date, Double> returnsMap) {

        int minDaysNeeded = 20;

        Map<Date, Double> standardDeviationMap = new LinkedHashMap<>();

        LinkedList<Double> returns = new LinkedList<>();

        for (Map.Entry<Date, Double> entry : returnsMap.entrySet()) {

            returns.add(entry.getValue());

            if (returns.size() >= minDaysNeeded) {
                standardDeviationMap.put(entry.getKey(), calculateStandardDeviation(returns));
                returns.remove(0);
            }
        }
        return standardDeviationMap;
    }

    /**
     * Standard deviation is computed using the formula square root of ( ∑ ( Xi – ų ) ^ 2 ) / N, where:
     * <p>
     * ∑ is the sum of each element
     * Xi is each element of the array
     * ų is the mean of the elements of the array
     * N is the number of elements
     */
    private double calculateStandardDeviation(Collection<Double> items) {

        double sum = 0.0;
        for (double i : items) {
            sum += i;
        }

        int length = items.size();
        double avg = sum / length;

        // calculate the standard deviation
        double standardDeviation = 0.0;
        for (double num : items) {
            standardDeviation += Math.pow(num - avg, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    private Map<Date, StockHistory> getStockHistoryMap(final String stockSymbol, final Date startDate) {

        Map<Date, StockHistory> result = new TreeMap<>();

        List<StockHistory> stockHistories = persistenceDelegate.getStockHistoryRepository().findByStockHistoryIdSymbolAndDate(stockSymbol, startDate);

        for (StockHistory stockHistory : stockHistories) {
            result.put(stockHistory.getStockHistoryId().getDate(), stockHistory);
        }
        return result;
    }

    private enum ReturnType {
        CLOSE_TO_CLOSE(24),
        OPEN_TO_CLOSE(6.5),
        CLOSE_TO_OPEN(17.5);

        ReturnType(double hours) {
            this.hours = hours;
        }

        private double hours;

        public double getHours() {
            return hours;
        }
    }

}
