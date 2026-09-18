package org.example.live;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Quote;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;


import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class MinuteQuoteScanner {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");

    private static final LocalTime START_TIME = LocalTime.of(9, 16);

    private static final LocalTime END_TIME = LocalTime.of(15, 25);

    private static final int HISTORY_SIZE = 30;

    private static final int TOP_N = 20;

    private static final double MIN_PRICE = 50;

    private static final long MIN_DAILY_VOLUME = 100_000;


    private final KiteConnect kite;

    private final String[] symbols;


    /*
     * Previous quote for calculating
     * 1-minute price and volume changes.
     */
    private final Map<String, QuoteSnapshot> previousQuotes = new ConcurrentHashMap<>();


    /*
     * Minute history for each stock.
     */
    private final Map<String, Deque<MinuteData>> minuteHistory = new ConcurrentHashMap<>();


    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public MinuteQuoteScanner(KiteConnect kite, String[] symbols) {

        this.kite = kite;

        this.symbols = symbols;
    }


    // =====================================================
    // START
    // =====================================================

    public void start() {

        scheduler.scheduleAtFixedRate(() -> {

                try {

                    scan();

                } catch (Exception e) {

                    e.printStackTrace();
                } catch (KiteException e) {
                    throw new RuntimeException(e);
                }

            },

            secondsUntilNextMinute(),

            60,

            TimeUnit.SECONDS);
    }


    // =====================================================
    // STOP
    // =====================================================

    public void stop() {

        scheduler.shutdownNow();
    }


    // =====================================================
    // SCAN
    // =====================================================

    private void scan() throws KiteException, IOException {

        LocalDateTime now = LocalDateTime.now(MARKET_ZONE);

        LocalTime time = now.toLocalTime();


        if (time.isBefore(START_TIME) || time.isAfter(END_TIME)) {

            return;
        }


        System.out.println();
        System.out.println("==============================================================");

        System.out.println("MINUTE SCAN : " + now);

        System.out.println("==============================================================");


        // =================================================
        // KITE INSTRUMENTS
        // =================================================


        Map<String, Quote> quotes = kite.getQuote(symbols);


        if (quotes == null || quotes.isEmpty()) {

            System.out.println("No quote data");

            return;
        }


        List<Candidate> candidates = new ArrayList<>();


        // =================================================
        // PROCESS QUOTES
        // =================================================

        for (Map.Entry<String, Quote> entry : quotes.entrySet()) {

            try {
                Candidate candidate = processQuote(entry.getKey(), entry.getValue(), now);
                if (candidate != null) {

                    candidates.add(candidate);
                }

            } catch (Exception e) {

                System.err.println("Error processing " + entry.getKey() + " : " + e.getMessage());
            }
        }


        // =================================================
        // SORT
        // =================================================

        candidates.sort(Comparator.comparingDouble(Candidate::getScore).reversed());


        // =================================================
        // PRINT
        // =================================================

        printTopCandidates(candidates);
    }


    // =====================================================
    // PROCESS QUOTE
    // =====================================================

    private Candidate processQuote(String instrument, Quote quote, LocalDateTime now) {


        if (quote == null) {
            return null;
        }


        String symbol = instrument.startsWith("NSE:") ? instrument.substring(4) : instrument;


        // =================================================
        // PRICE
        // =================================================

        double price = quote.lastPrice;


        if (price <= 0 || price < MIN_PRICE) {

            return null;
        }


        // =================================================
        // CUMULATIVE VOLUME
        // =================================================

        long cumulativeVolume = (long) quote.volumeTradedToday;


        if (cumulativeVolume < 0) {
            return null;
        }


        // =================================================
        // DAY VWAP
        // =================================================

        double vwap = quote.averagePrice;


        // =================================================
        // TODAY HIGH / LOW
        // =================================================

        double dayHigh = quote.ohlc != null ? quote.ohlc.high : 0;

        double dayLow = quote.ohlc != null ? quote.ohlc.low : 0;


        // =================================================
        // PREVIOUS QUOTE
        // =================================================

        QuoteSnapshot previous = previousQuotes.get(symbol);


        /*
         * First observation.
         *
         * We need one previous quote before
         * calculating 1-minute changes.
         */
        if (previous == null) {

            previousQuotes.put(symbol, new QuoteSnapshot(now, price, cumulativeVolume));

            return null;
        }


        // =================================================
        // 1 MINUTE PRICE CHANGE
        // =================================================

        double priceChange1m = percentageChange(previous.price, price);


        // =================================================
        // 1 MINUTE VOLUME
        // =================================================

        long volume1m = cumulativeVolume - previous.cumulativeVolume;


        /*
         * Protect against volume reset /
         * bad data.
         */
        if (volume1m < 0) {
            volume1m = 0;
        }


        // =================================================
        // HISTORY
        // =================================================

        Deque<MinuteData> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>());


        // =================================================
        // 3 MINUTE PRICE
        // =================================================

        MinuteData threeMinutesAgo = getAgo(history, 3);


        double priceChange3m = threeMinutesAgo != null ? percentageChange(threeMinutesAgo.price, price) : priceChange1m;


        // =================================================
        // 5 MINUTE PRICE
        // =================================================

        MinuteData fiveMinutesAgo = getAgo(history, 5);


        double priceChange5m = fiveMinutesAgo != null ? percentageChange(fiveMinutesAgo.price, price) : priceChange3m;


        // =================================================
        // 3 MINUTE VOLUME
        // =================================================

        long volume3m = sumVolume(history, 3) + volume1m;


        // =================================================
        // PREVIOUS 3 MINUTE VOLUME
        // =================================================

        long previousVolume3m = sumVolume(history, 3);


        // =================================================
        // VOLUME ACCELERATION
        // =================================================

        double volumeAcceleration = previousVolume3m > 0 ? volume1m / (double) previousVolume3m : 0;


        // =================================================
        // AVERAGE MINUTE VOLUME
        // =================================================

        double averageMinuteVolume = averageVolume(history, 5);


        // =================================================
        // RELATIVE VOLUME
        // =================================================

        double relativeVolume = averageMinuteVolume > 0 ? volume1m / averageMinuteVolume : 0;


        // =================================================
        // PREVIOUS 5 MINUTE HIGH
        // =================================================

        double previous5mHigh = highestHigh(history, 5);


        // =================================================
        // PREVIOUS 5 MINUTE LOW
        // =================================================

        double previous5mLow = lowestLow(history, 5);


        // =================================================
        // BREAKOUT
        // =================================================

        boolean highBreakout = previous5mHigh > 0 && price > previous5mHigh;


        // =================================================
        // BREAKDOWN
        // =================================================

        boolean lowBreakdown = previous5mLow > 0 && price < previous5mLow;


        // =================================================
        // VWAP
        // =================================================

        boolean aboveVWAP = vwap > 0 && price > vwap;


        double distanceFromVWAP = vwap > 0 ? percentageChange(vwap, price) : 0;


        // =================================================
        // PRICE ACCELERATION
        // =================================================

        double previousPriceChange1m = history.isEmpty() ? 0 : history.peekLast().priceChange1m;


        double priceAcceleration = priceChange1m - previousPriceChange1m;


        // =================================================
        // SCORE
        // =================================================

        double score = calculateScore(priceChange1m, priceChange3m, priceChange5m, volume1m, volume3m,
            volumeAcceleration, relativeVolume, highBreakout, aboveVWAP, distanceFromVWAP, priceAcceleration);


        // =================================================
        // STORE MINUTE
        // =================================================

        MinuteData current = new MinuteData();

        current.time = now;

        current.price = price;

        current.high = price;

        current.low = price;

        current.volume = volume1m;

        current.priceChange1m = priceChange1m;

        current.priceChange3m = priceChange3m;

        current.priceChange5m = priceChange5m;

        current.volume3m = volume3m;

        current.volumeAcceleration = volumeAcceleration;

        current.relativeVolume = relativeVolume;

        current.vwap = vwap;

        current.distanceFromVWAP = distanceFromVWAP;

        current.highBreakout = highBreakout;

        current.lowBreakdown = lowBreakdown;

        current.priceAcceleration = priceAcceleration;


        history.addLast(current);


        while (history.size() > HISTORY_SIZE) {

            history.removeFirst();
        }


        // =================================================
        // UPDATE PREVIOUS QUOTE
        // =================================================

        previousQuotes.put(symbol, new QuoteSnapshot(now, price, cumulativeVolume));


        // =================================================
        // RETURN CANDIDATE
        // =================================================



        Candidate candidate = new  Candidate(symbol, price,

            priceChange1m, priceChange3m, priceChange5m,

            volume1m, volume3m,

            volumeAcceleration, relativeVolume,

            highBreakout, lowBreakdown,

            vwap, aboveVWAP, distanceFromVWAP,

            priceAcceleration,

            score);

        return  candidate;
    }


    // =====================================================
    // SCORE
    // =====================================================

    private double calculateScore(double price1m, double price3m, double price5m, long volume1m, long volume3m, double volumeAcceleration, double relativeVolume, boolean breakout, boolean aboveVWAP, double distanceFromVWAP, double priceAcceleration) {


        double score = 0;


        // =================================================
        // PRICE 1M : 15
        // =================================================

        if (price1m >= 1.0) {

            score += 15;

        } else if (price1m >= 0.5) {

            score += 12;

        } else if (price1m >= 0.25) {

            score += 8;

        } else if (price1m > 0) {

            score += 3;
        }


        // =================================================
        // PRICE 3M : 15
        // =================================================

        if (price3m >= 2.0) {

            score += 15;

        } else if (price3m >= 1.0) {

            score += 12;

        } else if (price3m >= 0.5) {

            score += 7;

        } else if (price3m > 0) {

            score += 3;
        }


        // =================================================
        // PRICE 5M : 15
        // =================================================

        if (price5m >= 3.0) {

            score += 15;

        } else if (price5m >= 2.0) {

            score += 12;

        } else if (price5m >= 1.0) {

            score += 8;

        } else if (price5m > 0) {

            score += 3;
        }


        // =================================================
        // RELATIVE VOLUME : 20
        // =================================================

        if (relativeVolume >= 5.0) {

            score += 20;

        } else if (relativeVolume >= 3.0) {

            score += 17;

        } else if (relativeVolume >= 2.0) {

            score += 13;

        } else if (relativeVolume >= 1.5) {

            score += 8;

        } else if (relativeVolume >= 1.0) {

            score += 3;
        }


        // =================================================
        // VOLUME ACCELERATION : 10
        // =================================================

        if (volumeAcceleration >= 2.0) {

            score += 10;

        } else if (volumeAcceleration >= 1.5) {

            score += 8;

        } else if (volumeAcceleration >= 1.0) {

            score += 5;

        } else if (volumeAcceleration > 0) {

            score += 2;
        }


        // =================================================
        // BREAKOUT : 10
        // =================================================

        if (breakout) {

            score += 10;
        }


        // =================================================
        // VWAP : 5
        // =================================================

        if (aboveVWAP) {

            score += 5;
        }


        // =================================================
        // PRICE ACCELERATION : 10
        // =================================================

        if (priceAcceleration >= 0.50) {

            score += 10;

        } else if (priceAcceleration >= 0.25) {

            score += 7;

        } else if (priceAcceleration > 0) {

            score += 3;
        }


        return Math.min(score, 100);
    }


    // =====================================================
    // PRINT
    // =====================================================

    private void printTopCandidates(List<Candidate> candidates) {


        System.out.printf("%-12s %9s %8s %8s %8s %11s %11s %8s %8s %8s %10s%n",

            "SYMBOL", "PRICE", "1M%", "3M%", "5M%", "VOL1M", "VOL3M", "REL_VOL", "VWAP", "BREAK", "SCORE");


        System.out.println(
            "------------------------------------------------------------------------------------------------");


        candidates.stream().limit(TOP_N).forEach(c -> {

            System.out.printf(

                "%-12s %9.2f %7.2f%% %7.2f%% %7.2f%% %11d %11d %8.2f %8s %8s %10.1f%n",

                c.symbol,

                c.price,

                c.priceChange1m,

                c.priceChange3m,

                c.priceChange5m,

                c.volume1m,

                c.volume3m,

                c.relativeVolume,

                c.aboveVWAP ? "YES" : "NO",

                c.highBreakout ? "YES" : "NO",

                c.score);
        });
    }


    // =====================================================
    // HELPERS
    // =====================================================

    private static double percentageChange(double oldPrice, double newPrice) {

        if (oldPrice <= 0) {
            return 0;
        }

        return ((newPrice - oldPrice) * 100.0 / oldPrice);
    }


    private static long sumVolume(Deque<MinuteData> history, int minutes) {

        long total = 0;

        Iterator<MinuteData> iterator = history.descendingIterator();


        int count = 0;

        while (iterator.hasNext() && count < minutes) {

            total += iterator.next().volume;

            count++;
        }

        return total;
    }


    private static double averageVolume(Deque<MinuteData> history, int minutes) {

        int count = Math.min(history.size(), minutes);


        if (count == 0) {
            return 0;
        }


        return sumVolume(history, minutes) / (double) count;
    }


    private static MinuteData getAgo(Deque<MinuteData> history, int minutesAgo) {

        if (history.size() < minutesAgo) {
            return null;
        }


        Iterator<MinuteData> iterator = history.descendingIterator();


        MinuteData result = null;


        for (int i = 0; i < minutesAgo && iterator.hasNext(); i++) {

            result = iterator.next();
        }


        return result;
    }


    private static double highestHigh(Deque<MinuteData> history, int minutes) {

        if (history.isEmpty()) {
            return 0;
        }


        double high = Double.MIN_VALUE;


        Iterator<MinuteData> iterator = history.descendingIterator();


        int count = 0;


        while (iterator.hasNext() && count < minutes) {

            MinuteData data = iterator.next();


            high = Math.max(high, data.high);

            count++;
        }


        return high == Double.MIN_VALUE ? 0 : high;
    }


    private static double lowestLow(Deque<MinuteData> history, int minutes) {

        if (history.isEmpty()) {
            return 0;
        }


        double low = Double.MAX_VALUE;


        Iterator<MinuteData> iterator = history.descendingIterator();


        int count = 0;


        while (iterator.hasNext() && count < minutes) {

            MinuteData data = iterator.next();


            low = Math.min(low, data.low);

            count++;
        }


        return low == Double.MAX_VALUE ? 0 : low;
    }


    private static long secondsUntilNextMinute() {

        ZonedDateTime now = ZonedDateTime.now(MARKET_ZONE);


        ZonedDateTime next = now.plusMinutes(1).withSecond(2).withNano(0);


        return Math.max(1, Duration.between(now, next).getSeconds());
    }


    // =====================================================
    // DATA
    // =====================================================

    private static class QuoteSnapshot {

        LocalDateTime time;

        double price;

        long cumulativeVolume;


        QuoteSnapshot(LocalDateTime time, double price, long cumulativeVolume) {

            this.time = time;

            this.price = price;

            this.cumulativeVolume = cumulativeVolume;
        }
    }


    private static class MinuteData {

        LocalDateTime time;

        double price;

        double high;

        double low;

        long volume;

        double priceChange1m;

        double priceChange3m;

        double priceChange5m;

        long volume3m;

        double volumeAcceleration;

        double relativeVolume;

        double vwap;

        double distanceFromVWAP;

        boolean highBreakout;

        boolean lowBreakdown;

        double priceAcceleration;
    }


    public static class Candidate {

        String symbol;

        double price;

        double priceChange1m;

        double priceChange3m;

        double priceChange5m;

        long volume1m;

        long volume3m;

        double volumeAcceleration;

        double relativeVolume;

        boolean highBreakout;

        boolean lowBreakdown;

        double vwap;

        boolean aboveVWAP;

        double distanceFromVWAP;

        double priceAcceleration;

        double score;


        Candidate(String symbol, double price, double priceChange1m, double priceChange3m, double priceChange5m, long volume1m, long volume3m, double volumeAcceleration, double relativeVolume, boolean highBreakout, boolean lowBreakdown, double vwap, boolean aboveVWAP, double distanceFromVWAP, double priceAcceleration, double score) {

            this.symbol = symbol;
            this.price = price;

            this.priceChange1m = priceChange1m;

            this.priceChange3m = priceChange3m;

            this.priceChange5m = priceChange5m;

            this.volume1m = volume1m;

            this.volume3m = volume3m;

            this.volumeAcceleration = volumeAcceleration;

            this.relativeVolume = relativeVolume;

            this.highBreakout = highBreakout;

            this.lowBreakdown = lowBreakdown;

            this.vwap = vwap;

            this.aboveVWAP = aboveVWAP;

            this.distanceFromVWAP = distanceFromVWAP;

            this.priceAcceleration = priceAcceleration;

            this.score = score;
        }


        public double getScore() {
            return score;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getPrice() {
            return price;
        }

        public double getPriceChange1m() {
            return priceChange1m;
        }

        public double getPriceChange3m() {
            return priceChange3m;
        }

        public double getPriceChange5m() {
            return priceChange5m;
        }

        public long getVolume1m() {
            return volume1m;
        }

        public long getVolume3m() {
            return volume3m;
        }

        public double getVolumeAcceleration() {
            return volumeAcceleration;
        }

        public double getRelativeVolume() {
            return relativeVolume;
        }

        public boolean isHighBreakout() {
            return highBreakout;
        }

        public boolean isLowBreakdown() {
            return lowBreakdown;
        }

        public double getVwap() {
            return vwap;
        }

        public boolean isAboveVWAP() {
            return aboveVWAP;
        }

        public double getDistanceFromVWAP() {
            return distanceFromVWAP;
        }

        public double getPriceAcceleration() {
            return priceAcceleration;
        }
    }

}
