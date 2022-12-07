package com.yury.trade.delegate;

import com.yury.trade.entity.StockHistory;
import com.yury.trade.entity.VolatilityInfo;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VolatilityDelegate {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private final int MIN_DAYS = 20;

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    public void calcVolatility(final String stockSymbol, final String startDateString, final String endDateString) throws ParseException {

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        List<String> stockSymbols = new ArrayList<>();

        if (stockSymbol != null) {
            stockSymbols.add(stockSymbol);
        } else {
            stockSymbols = persistenceDelegate.getStockQuoteRepository().findGoodSymbols();
        }

        List<VolatilityInfo> volatilityInfos = new ArrayList<>();

        for (String tempSymbol : stockSymbols) {

            Map<Date, StockHistory> stockHistoryMap = getStockHistoryMap(tempSymbol, startDate);

            if (stockHistoryMap.size() == 0) {
                continue;
            }

            volatilityInfos.addAll(createVolatilityInfos(stockHistoryMap));
        }

        Collections.sort(volatilityInfos);

        persistenceDelegate.getVolatilityInfoRepository().saveAll(volatilityInfos);

        System.out.println("Saved into VolatilityInfo  " + volatilityInfos.size());
    }

    private List<VolatilityInfo> createVolatilityInfos(Map<Date, StockHistory> stockHistoryMap) {

        List<VolatilityInfo> volatilityInfos = new ArrayList<>();

        Map<Date, Double> volatilityMapCtC = getHistoricVolatilityMap(stockHistoryMap, ReturnType.CLOSE_TO_CLOSE);
        Map<Date, Double> volatilityMapOtC = getHistoricVolatilityMap(stockHistoryMap, ReturnType.OPEN_TO_CLOSE);
        Map<Date, Double> volatilityMapCtO = getHistoricVolatilityMap(stockHistoryMap, ReturnType.CLOSE_TO_OPEN);

        Map<Date, Double> dailyReturnsMapCC = getDailyReturnsMap(stockHistoryMap, ReturnType.CLOSE_TO_CLOSE);
        Map<Date, Double> standardDeviationMapCC = getDailyStandardDeviationMap(dailyReturnsMapCC);

        Date[] stockHistoryMapKeys = stockHistoryMap.keySet().toArray(new Date[stockHistoryMap.size()]);

        for (int i = 0; i < stockHistoryMap.size(); i++) {

            Date date = stockHistoryMapKeys[i];

            if (!volatilityMapCtC.containsKey(date)) {
                continue;
            }

            VolatilityInfo volatilityInfo = new VolatilityInfo();

            StockHistory last = stockHistoryMap.get(stockHistoryMapKeys[i]);
            StockHistory previous = stockHistoryMap.get(stockHistoryMapKeys[i - 1]);

            volatilityInfo.setId(last.getStockHistoryId());

            volatilityInfo.setCC_vol(volatilityMapCtC.get(date));
            volatilityInfo.setOC_vol(volatilityMapOtC.get(date));
            volatilityInfo.setCO_vol(volatilityMapCtO.get(date));

            Double stDeviation = last.getClose() * standardDeviationMapCC.get(date);

            volatilityInfo.setStDeviation(Precision.round(stDeviation, 2));

            volatilityInfo.setCC_price_change(Precision.round(last.getClose() - previous.getClose(), 2));
            volatilityInfo.setOC_price_change(Precision.round(last.getClose() - last.getOpen(), 2));
            volatilityInfo.setCO_price_change(Precision.round(last.getOpen() - previous.getClose(), 2));

            volatilityInfo.setOCbyCC_index(Precision.round(volatilityInfo.getOC_vol() / volatilityInfo.getCC_vol(), 2));

            volatilityInfo.setCC_spike(Precision.round(volatilityInfo.getCC_price_change() / stDeviation, 2));
            volatilityInfo.setOC_spike(Precision.round(volatilityInfo.getOC_price_change() / stDeviation, 2));
            volatilityInfo.setCO_spike(Precision.round(volatilityInfo.getCO_price_change() / stDeviation, 2));

            volatilityInfos.add(volatilityInfo);
        }

        return volatilityInfos;
    }

    private void showInfo(Map<Date, Double> volatilityMap, ReturnType returnType) {
        System.out.println(returnType);
        System.out.println(volatilityMap);
    }

    private Double getLast(Map<Date, Double> map) {

        Date[] keys = map.keySet().toArray(new Date[map.size()]);
        return map.get(keys[keys.length - 1]);
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

        Map<Date, Double> standardDeviationMap = new LinkedHashMap<>();

        LinkedList<Double> returns = new LinkedList<>();

        for (Map.Entry<Date, Double> entry : returnsMap.entrySet()) {

            returns.add(entry.getValue());

            if (returns.size() >= MIN_DAYS) {
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
