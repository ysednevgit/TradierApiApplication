package com.yury.trade.delegate;

import com.yury.trade.entity.StockHistory;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class CustomStatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private FlowDelegate flowDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    Map<Double, String> coeffs = new TreeMap<>();

    public void getStats(String symbol, String startDateString) throws ParseException {

        coeffs.clear();

        Date startDate = startDateString != null ? sdf.parse(startDateString) : null;

        List<String> symbols = new ArrayList<>();

        if (symbol == null) {
            symbols = persistenceDelegate.getStockSymbolRepository().findEnabledSymbols();
        } else {
            symbols.add(symbol);
        }

        for (String stockSymbol : symbols) {
            getStats(stockSymbol, startDate);
            //testBigMove(symbol, startDate);
        }

    }

    private void getStats(String symbol, Date startDate) {

        System.out.println(symbol);
        List<StockHistory> stockHistories = persistenceDelegate.getStockHistoryRepository().findByStockHistoryIdSymbolAndDate(symbol, startDate);

        Map<Integer, List<Double>> averageMoveMap = new TreeMap<>();
        Map<Integer, List<Double>> intradayAverageMoveMap = new TreeMap<>();

        double previousClose = 0;
        for (StockHistory stockHistory : stockHistories) {

            if (previousClose == 0) {
                previousClose = stockHistory.getClose();
            }

            int dayOfWeek = flowDelegate.getDayOfWeek(flowDelegate.convertToLocalDate(stockHistory.getStockHistoryId().getDate()));

            double movePct = Precision.round(previousClose / stockHistory.getClose() * 100 - 100, 2);
            double intradayMovePct = Precision.round(stockHistory.getClose() / stockHistory.getOpen() * 100 - 100, 2);

            if (!averageMoveMap.containsKey(dayOfWeek)) {
                averageMoveMap.put(dayOfWeek, new LinkedList<>());
            }
            averageMoveMap.get(dayOfWeek).add(movePct);

            if (!intradayAverageMoveMap.containsKey(dayOfWeek)) {
                intradayAverageMoveMap.put(dayOfWeek, new LinkedList<>());
            }
            intradayAverageMoveMap.get(dayOfWeek).add(intradayMovePct);

            previousClose = stockHistory.getClose();
        }

        double average = 0;

        List<Double> closeToCloseList = new ArrayList<>();

        for (Map.Entry<Integer, List<Double>> entry : averageMoveMap.entrySet()) {
            System.out.println("Day " + entry.getKey());

            double sum = 0;
            for (Double movePct : entry.getValue()) {
                sum += Math.abs(movePct);
            }

            double closeToClose = Precision.round(sum / entry.getValue().size(), 2);

            average += closeToClose;
/**
 double intraSum = 0;
 for (Double movePct : intradayAverageMoveMap.get(entry.getKey())) {
 intraSum += Math.abs(movePct);
 }
 System.out.println("Intraday: " + Precision.round(intraSum / intradayAverageMoveMap.get(entry.getKey()).size(), 2));
 **/

            System.out.println("Close to close: " + closeToClose);
            closeToCloseList.add(closeToClose);
        }

        average = Precision.round(average / averageMoveMap.size(), 2);

        System.out.println("average: " + average);

        for (int i = 0; i < closeToCloseList.size(); i++) {

            Double closeToClose = closeToCloseList.get(i);

            double coeff = closeToClose / average;

            coeffs.put(coeff, symbol + (i + 1));
        }

        System.out.println();
        System.out.println(coeffs);

    }

    private void testBigMove(String symbol, Date startDate) {

        System.out.println("testBigMove " + symbol);

        List<StockHistory> stockHistories = persistenceDelegate.getStockHistoryRepository().findByStockHistoryIdSymbolAndDate(symbol, startDate);

        double previousClose = 0;

        List<Double> nextDayMoves = new ArrayList<>();

        boolean addToNextDays = false;

        for (StockHistory stockHistory : stockHistories) {

            if (previousClose == 0) {
                previousClose = stockHistory.getClose();
            }

            double movePct = Precision.round(stockHistory.getClose() / previousClose * 100 - 100, 2);

            if (addToNextDays) {
                nextDayMoves.add(movePct);
                addToNextDays = false;
                System.out.println(" Next day move:" + movePct);
            }

            if (Math.abs(movePct) > 2.2) {
                System.out.print("Move:" + movePct);
                addToNextDays = true;
            }

            previousClose = stockHistory.getClose();
        }

        System.out.println(nextDayMoves);

        Double sum = 0d;

        for (Double nextDay : nextDayMoves) {
            sum += Math.abs(nextDay);
        }

        Double average = Precision.round(sum / nextDayMoves.size(), 2);

        System.out.println("Average " + average);
    }

}