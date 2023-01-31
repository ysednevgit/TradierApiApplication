package com.yury.trade.util;

import org.apache.commons.math3.util.Precision;

import java.util.concurrent.*;

public class Test {

    public static void main(String... args1) throws Exception {

        testConcurrency();
    }


    static void testConcurrency() throws ExecutionException, InterruptedException {

        Callable callable1 = () -> {

            int i = 1;

            while (i <= 10) {
                System.out.println(i);
                TimeUnit.MILLISECONDS.sleep(100);
                i++;
            }

            return true;
        };


        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future future = executorService.submit(callable1);

        while (!future.isDone()) {
            System.out.println("Done " + future.get());
        }

        executorService.shutdown();

    }


    public static void testBalance() throws InterruptedException {

        double balance = 1000;

        double betPct = 5;

        int step = 0;

        while (step <= 1000) {

            if (balance < 10) {
                break;
            }

            int rand = (int) (Math.random() * 100);

            double bet = (int) (balance * betPct / 100);

            double startBet = bet;

            balance = balance - bet;
/**
 if (rand <= 5) {
 bet = 0;
 } else if (rand <= 25) {
 bet = bet / 2;
 } else if (rand <= 40) {
 bet = bet;
 } else if (rand <= 90) {
 bet = bet * 1.3;
 } else if (rand <= 100) {
 bet = bet * 1.5;
 }
 **/

            if (rand <= 30) {
                bet = 0;
            } else if (rand <= 55) {
                bet = bet / 2;
            } else if (rand <= 75) {
                bet = bet * 1.5;
            } else if (rand <= 85) {
                bet = bet * 2;
            } else if (rand <= 95) {
                bet = bet * 3;
            } else if (rand <= 100) {
                bet = bet * 4;
            }

            balance += bet;

            Thread.sleep(20);

            System.out.println("Step=" + step + " Balance=" + Precision.round(balance, 2) + " startBet=" + Precision.round(startBet, 2) + " endBet=" + Precision.round(bet, 2));

            step++;

        }


        //System.out.println(isCorrectBrackets("([{}]}"));
/**
 Consumer cc = new Consumer() {
@Override public void accept(Object o) {
System.out.println(o);
}
};

 Predicate pp = (Predicate<Integer>) o -> o % 5 == 0;

 //        Stream.iterate(1, i -> i <= 50, i -> i + 1).filter(pp).forEach(cc);

 Stream.iterate(1, i -> i + 1).limit(10).map(i -> i * i).forEach(System.out::println);

 /**
 *
 int[] ints = IntStream.range(1,51).filter(i -> i % 5 == 0).toArray();

 for (int i : ints)
 {
 System.out.println(i);
 }
 **/

    }

    public static boolean isCorrectBrackets(String s) {

        if (s.length() % 2 == 1) {
            return false;
        }

        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {

            char char1 = chars[i];
            char char2 = chars[chars.length - 1 - i];

            if (char1 == '(' && char2 != ')') {
                return false;
            }
            if (char1 == '{' && char2 != '}') {
                return false;
            }
            if (char1 == '[' && char2 != ']') {
                return false;
            }
        }

        return true;
    }

}
