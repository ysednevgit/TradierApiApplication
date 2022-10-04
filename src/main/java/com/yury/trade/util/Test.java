package com.yury.trade.util;

import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        getData("0,600,660,1069,1453,1154,1644,1605,-469,-490,1323,2906,1571,490,1436,2602,1784,2515,4522,4816,8036");
        getData("0,600,472,483,574,294,473,434,-1640,-1661,152,1736,1825,1131,1743,2374,1812,2149,3427,3721,6274");

    }

    static void getData(String str) {

        String[] words = str.split(",");

        List<Integer> upMoves = new ArrayList<>();
        List<Integer> downMoves = new ArrayList<>();

        for (int i = 0; i < words.length - 1; i++) {

            int move = Integer.parseInt(words[i + 1]) - Integer.parseInt(words[i]);

            if (move < 0) {
                downMoves.add(move);
            } else {
                upMoves.add(move);
            }
        }

        int upChance = (upMoves.size() * 100 / (upMoves.size() + downMoves.size()));

        System.out.println("Up Chance" + upChance);

    }

}
