package com.yury.trade.delegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class VolatilityDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private StatsDelegate statsDelegate;

    public void calcVolatility(String stockSymbol) {
        Map<Date, Double> stockHistoryMap = statsDelegate.getStockHistoryMap(stockSymbol);

    }

    private Map<Date, Double> getDailyReturnsMap(Map<Date, Double> stockHistoryMap) {

        Map<Date, Double> dailyReturnsMap = new HashMap<>();

        Double previousValue = null;

        for (Map.Entry<Date, Double> entry : stockHistoryMap.entrySet()) {

            if (previousValue != null) {
                Double value = Math.log(entry.getValue() / previousValue);

                dailyReturnsMap.put(entry.getKey(), value);
            }
            previousValue = entry.getValue();
        }

        return dailyReturnsMap;
    }

    private Map<Date, Double> getDailyStandardDeviationMap(Map<Date, Double> returnsMap) {
        return null;
    }

    private Map<Date, Double> getHistoricDailyVolatilityMap(Map<Date, Double> dailyStandardDeviationMap) {
        return null;
    }

    /**
     * Standard deviation is computed using the formula square root of ( ∑ ( Xi – ų ) ^ 2 ) / N, where:
     * <p>
     * ∑ is the sum of each element
     * Xi is each element of the array
     * ų is the mean of the elements of the array
     * N is the number of elements
     */
    private double calculateStandardDeviation(double[] array) {

        double sum = 0.0;
        for (double i : array) {
            sum += i;
        }

        int length = array.length;
        double avg = sum / length;

        // calculate the standard deviation
        double standardDeviation = 0.0;
        for (double num : array) {
            standardDeviation += Math.pow(num - avg, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }


}
