package com.yury.trade.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TradeInfo {

    public static void showInfo(List<Trade> trades) {

        System.out.println();
        System.out.println("Trades:");

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

            System.out.println(trade);
            System.out.println();

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

        System.out.println("Total trades: " + trades.size());

        double profitable = upMoves.size() * 100 / (upMoves.size() + downMoves.size());

        System.out.println("Profitable %: " + profitable);
        System.out.println("Loss %: " + (100 - profitable));
        System.out.println();

        System.out.println("Avg profit: " + getAvg(upMoves));
        System.out.println("Avg loss: " + getAvg(downMoves));
        System.out.println();

        System.out.println("Max profit: " + maxUp);
        System.out.println("Max loss: " + maxDown);
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
