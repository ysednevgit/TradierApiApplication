package com.yury.trade.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TradeInfo {

    public static String getInfo(List<Trade> trades) {

        StringBuilder info = new StringBuilder();

        info.append("\n");
        info.append("\n");

        info.append("Trades:");

        List<Integer> upMoves = new ArrayList<>();
        List<Integer> downMoves = new ArrayList<>();

        int maxUp = 0;
        int maxDown = 0;

        Iterator<Trade> iterator = trades.iterator();

        while (iterator.hasNext()) {
            Trade trade = iterator.next();

            if (trade.getEndDate() == null) {
                iterator.remove();
                continue;
            }

            info.append("\n");
            info.append(trade);
            info.append("\n");

            int move = (int) trade.getProfit();

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

        info.append("\n").append("Total trades: " + trades.size());

        double diviser = upMoves.size() + downMoves.size();
        if (diviser == 0) {
            diviser = 1;
        }

        double profitable = upMoves.size() * 100 / diviser;

        info.append("\n").append("Profitable %: " + profitable);
        info.append("\n").append("Loss %: " + (100 - profitable));
        info.append("\n");

        info.append("\n").append("Avg profit: " + getAvg(upMoves));
        info.append("\n").append("Avg loss: " + getAvg(downMoves));
        info.append("\n");

        info.append("\n").append("Max profit: " + maxUp);
        info.append("\n").append("Max loss: " + maxDown);

        return info.toString();
    }

    private static int getAvg(List<Integer> list) {

        if (list.size() == 0) {
            return 0;
        }

        int sum = 0;

        for (Integer item : list) {
            sum += item;
        }

        return sum / list.size();
    }
}
