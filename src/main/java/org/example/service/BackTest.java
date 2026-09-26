package org.example.service;

import org.example.CandleNMin;
import org.example.PotentialScanner;
import org.example.loader.DB;
import org.example.loader.DataLoader;
import org.example.model.FundamentalData;
import org.example.model.MinuteCandle;
import org.example.model.PreviousDayData;
import org.example.model.StockDailyFeature;
import org.example.repository.StockDailyFeatureRepository;

import java.lang.annotation.Target;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class BackTest {

    static Map<String, PotentialScanner.Position> openPositions = new HashMap<>();
    static Set<String> todayTradedSymbol = new HashSet<>();
    static double totalProfit = 0;
    static double totalCapital = 150000;
    static double CAPITAL_PER_TRADE = 30000.0;
    static int canleCount = 5;
    static int prevDay = 3;
    static int currentDay = 2;

    static void main() throws SQLException {
        backTest();
    }

    public static void backTest() throws SQLException {

        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);
        LocalDate today = LocalDate.now();
        Map<String, FundamentalData> fundamentalDataMap = StockDailyFeatureRepository.loadFundamentals(DB.get());
        Map<String, PreviousDayData> previousDayDataMap =
                StockDailyFeatureRepository.loadPreviousDayClose(
                        today.minusDays(prevDay),
                        DB.get(),
                        1
                );
        ConcurrentHashMap<String, Deque<MinuteCandle>> fullHistory =
                DataLoader.loadMinuteHistory(
                        DB.get(),
                        today.minusDays(currentDay),
                        // Previous day
                        // Today
                        LocalTime.of(9, 15),
                        LocalTime.of(15, 15)
                );
        Map<LocalDateTime, List<MinuteCandle>> minuteHistoryBasedOnTime =
                DataLoader.loadMinuteHistoryBasedOnTime(
                        DB.get(),
                        today.minusDays(prevDay),
                        today.minusDays(currentDay),
                        // Previous day
                        LocalTime.of(14, 30),
                        LocalTime.of(15, 30),

                        // Today
                        LocalTime.of(9, 15),
                        LocalTime.of(15, 15)
                );
        LocalDateTime startTime = today.minusDays(prevDay).atTime(14, 30);
        LocalDateTime endTime = today.minusDays(currentDay).atTime(9, 18);

        while (true) {
            try {

                if (endTime.isAfter(today.minusDays(currentDay).atTime(14, 30))) {
                    System.out.println(
                            "TOTAL REALIZED P&L = "
                                    + String.format("%.2f", totalProfit)
                    );
                    if (openPositions.isEmpty()) {
                        System.out.println("No open positions.");
                        System.out.println("================================");
                        return;
                    }

                    double totalUnrealizedProfit = 0;

                    for (PotentialScanner.Position position : openPositions.values()) {


                        double unrealizedProfit =
                                (position.getClosePrice() - position.getEntryPrice())
                                        * position.getQuantity();

                        totalUnrealizedProfit += unrealizedProfit;

                        System.out.println(
                                position.getSymbol()
                                        + " | EntryTime=" + position.getEntryTime()
                                        + " | Entry=" + position.getEntryPrice()
                                        + " | Last=" + position.getClosePrice()
                                        + " | High=" + position.getHighPrice()
                                        + " | Qty=" + position.getQuantity()
                                        + " | Capital=" + position.getCapital()
                                        + " | P&L=" + String.format("%.2f", unrealizedProfit)
                        );

                    }

                    System.out.println(
                            "TOTAL UNREALIZED P&L = "
                                    + String.format("%.2f", totalUnrealizedProfit)
                    );
                    System.exit(0);
                }
                System.out.println(
                        "EMA/VWAP calculation: for = " + endTime
                );


                Map<String, List<MinuteCandle>> minuteHistory = getMinuteHistoryBySymbol(
                        minuteHistoryBasedOnTime,
                        startTime,
                        endTime
                );
                calculateEMAandVWAP(
                        null,
                        previousDayDataMap,
                        fundamentalDataMap,
                        minuteHistory,
                        1.5,
                        endTime
                );

                // Next execution: +3 minutes
                endTime = endTime.plusMinutes(canleCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, List<MinuteCandle>> getMinuteHistoryBySymbol(
            Map<LocalDateTime, List<MinuteCandle>> minuteHistoryBasedOnTime,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        Map<String, List<MinuteCandle>> result = new HashMap<>();

        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry
                : minuteHistoryBasedOnTime.entrySet()) {

            LocalDateTime time = entry.getKey();

            // Since TreeMap is sorted by time, we can skip/stop efficiently
            if (time.isBefore(startTime)) {
                continue;
            }

            if (time.isAfter(endTime)) {
                break;
            }

            for (MinuteCandle candle : entry.getValue()) {

                result.computeIfAbsent(
                        candle.getSymbol(),
                        k -> new ArrayList<>()
                ).add(candle);
            }
        }

        return result;
    }

    public static void calculateEMAandVWAP(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap, Map<String, List<MinuteCandle>> minuteHistory, double maxStopLossPercent, LocalDateTime endTime) {

        List<Map.Entry<String, CandleNMin>> results = new ArrayList<>();

        minuteHistory.forEach((symbol, minuteCandles) -> {

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);
            List<CandleNMin> candleNMins = calculateTimeBasedEmaVwapAndAtr(candles, canleCount, 10);
            //print5MinCandles(candles5mins);
            CandleNMin candle = candleNMins.getLast();
            double stockPrice = candle.getClose();
            FundamentalData stockFundamental = fundamentalDataMap.get(symbol);
            PreviousDayData previousDayData = previousDayDataMap.get(symbol);
            if (previousDayData == null) {
                return;
            }
            if (!openPositions.isEmpty()) {

                if (openPositions.get(symbol) != null) {

                    PotentialScanner.Position openPosition = openPositions.get(symbol);


                    if (candle.getTime().isAfter(openPosition.getEntryTime())) {

                        double currentProfit =
                                openPosition.profit(candle.getHigh());

                        if (candle.getClose() < candle.getEma9() || currentProfit > 1000) {
                            System.out.println(
                                    "EXIT | "
                                            + symbol
                                            + " | Entry=" + openPosition.getEntryPrice()
                                            + " | Exit=" + candle.getClose()
                                            + " | High = " + candle.getHigh()
                                            + " | Qty=" + openPosition.getQuantity()
                                            + " | Profit=" + currentProfit
                                            + " | HighestProfit=" + openPosition.getHighestProfit()
                                            + " | Drawdown="
                                            + (openPosition.getHighestProfit() - currentProfit)
                                            + " | EntryTime=" + openPosition.getEntryTime()
                            );
                            totalCapital += openPosition.getCapital();
                            totalProfit += currentProfit;
                            openPositions.remove(symbol);
                        }
                        openPosition.setClosePrice(candle.getClose());
                        openPosition.setHighPrice(Math.max(openPosition.getHighPrice(), candle.getHigh()));
                    }
                }
            }

            double stockClosePrice = previousDayData.getClosePrice();
            double stockLastDayHigh = previousDayData.getHighPrice();
            double percentageChange = 0.0;

            if (stockClosePrice > 0) {
                percentageChange = ((stockPrice - stockClosePrice) / stockClosePrice) * 100.0;
            }

            candle.setPercentageChange(percentageChange);
            candle.setLastDayHigh(stockLastDayHigh);
            if (candle.getEma9() > candle.getVwap() && stockPrice > candle.getEma9() && stockPrice > 500 && stockPrice < 2500 && (stockFundamental != null && stockFundamental.getMarketCap() > 1500)) {
                //System.out.println(symbol + " " + candle.toString());
                //results.add(Map.entry(symbol, candle));
                boolean continuouslyIncreasing = isPriceContinuouslyIncreasing(candleNMins, canleCount);
                boolean voluemeIncreasing = isVolumeContinuouslyIncreasing(candleNMins, canleCount);
                if (continuouslyIncreasing && voluemeIncreasing) {
                    results.add(
                            Map.entry(symbol, candle)
                    );
                }
            }


        });


       /* results.sort(
                Comparator.comparingDouble(
                        (Map.Entry<String, CandleNMin> entry) -> {
                            double currentPrice = entry.getValue().getClose();
                            double previousClose = previousDayDataMap.get(entry.getKey()).getClosePrice();
                            return ((currentPrice - previousClose) / previousClose) * 100.0;
                        }
                ).reversed()
        );*/
        results.sort(
                Comparator.comparingDouble(
                        (Map.Entry<String, CandleNMin> entry) ->
                                entry.getValue().getVolume()
                ).reversed()
        );

        // Print results
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        AtomicInteger counter = new AtomicInteger(1);
        results.forEach(entry -> {

            String symbol = entry.getKey();
            CandleNMin candle = entry.getValue();
            if (!openPositions.containsKey(symbol) && totalCapital > CAPITAL_PER_TRADE && !todayTradedSymbol.contains(symbol) && counter.get() == 1) {
                int QUANTITY = (int) (CAPITAL_PER_TRADE / candle.getClose());

                if (QUANTITY <= 0) {
                    return;
                }
                PotentialScanner.Position newPosition =
                        new PotentialScanner.Position(
                                symbol,
                                candle.getClose(),
                                QUANTITY,
                                candle.getTime()
                        );

                openPositions.put(symbol, newPosition);
                totalCapital -= CAPITAL_PER_TRADE;
                todayTradedSymbol.add(symbol);
                System.out.println( "ENTER IN TRADE FOR " +
                        symbol
                                + " | Price: " + candle.getClose()
                                + " | EMA9: " + candle.getEma9()
                                + " | VWAP: " + candle.getVwap()
                                + " | Volume: " + candle.getCumulativeVolume()
                                + " | REMANING CAPITAL : " + totalCapital
                );
            }
            counter.getAndIncrement();
        });
    }

    public static List<CandleNMin> calculateTimeBasedEmaVwapAndAtr(
            List<MinuteCandle> minuteCandles, int minutes, int atrPeriod) {

        // 1. Group minute candles into N-minute buckets
        Map<LocalDateTime, List<MinuteCandle>> grouped =
                minuteCandles.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getTime()
                                        .withMinute((c.getTime().getMinute() / minutes) * minutes)
                                        .withSecond(0)
                                        .withNano(0),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        List<CandleNMin> result = new ArrayList<>();

        double ema9 = 0.0;

        double cumulativePV = 0.0;
        double cumulativeVolume = 0.0;

        LocalDate currentDate = null;

        // EMA9 alpha
        double alpha = 2.0 / (9.0 + 1.0);   // 0.2

        // ATR state
        double atr = 0.0;
        boolean atrInitialized = false;

        List<Double> trueRanges = new ArrayList<>();

        CandleNMin previousCandle = null;

        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : grouped.entrySet()) {

            LocalDateTime bucketTime = entry.getKey();
            List<MinuteCandle> candles = entry.getValue();

            candles.sort(Comparator.comparing(MinuteCandle::getTime));

            // --------------------------------------------------
            // Reset VWAP and EMA at new trading day
            // --------------------------------------------------

            if (!bucketTime.toLocalDate().equals(currentDate)) {

                currentDate = bucketTime.toLocalDate();

                cumulativePV = 0.0;
                cumulativeVolume = 0.0;

                ema9 = 0.0;

                // Reset ATR for each trading day
                atr = 0.0;
                atrInitialized = false;
                trueRanges.clear();

                previousCandle = null;
            }

            // --------------------------------------------------
            // Build N-minute candle
            // --------------------------------------------------

            double open = candles.get(0).getOpen();

            double high = candles.stream()
                    .mapToDouble(MinuteCandle::getHigh)
                    .max()
                    .orElse(open);

            double low = candles.stream()
                    .mapToDouble(MinuteCandle::getLow)
                    .min()
                    .orElse(open);

            double close = candles.get(candles.size() - 1).getClose();

            double volume = candles.stream()
                    .mapToDouble(MinuteCandle::getVolume)
                    .sum();

            // --------------------------------------------------
            // EMA 9
            // --------------------------------------------------

            if (ema9 == 0.0) {
                ema9 = close;
            } else {
                ema9 = (close * alpha)
                        + (ema9 * (1.0 - alpha));
            }

            // --------------------------------------------------
            // VWAP
            // Typical Price = (H + L + C) / 3
            // --------------------------------------------------

            double typicalPrice = (high + low + close) / 3.0;

            cumulativePV += typicalPrice * volume;
            cumulativeVolume += volume;

            double vwap = cumulativeVolume > 0
                    ? cumulativePV / cumulativeVolume
                    : close;

            // --------------------------------------------------
            // TRUE RANGE
            // --------------------------------------------------

            double tr;

            if (previousCandle == null) {

                // First candle of the day
                tr = high - low;

            } else {

                double previousClose = previousCandle.getClose();

                tr = Math.max(
                        high - low,
                        Math.max(
                                Math.abs(high - previousClose),
                                Math.abs(low - previousClose)
                        )
                );
            }

            // --------------------------------------------------
            // ATR using Wilder's method
            // --------------------------------------------------

            double currentAtr = 0.0;

            if (!atrInitialized) {

                trueRanges.add(tr);

                if (trueRanges.size() == atrPeriod) {

                    // First ATR = SMA of first N TR values
                    atr = trueRanges.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    atrInitialized = true;

                    currentAtr = atr;
                }

            } else {

                // Wilder smoothing:
                //
                // ATR = ((Previous ATR * (period - 1)) + TR) / period

                atr = ((atr * (atrPeriod - 1)) + tr) / atrPeriod;

                currentAtr = atr;
            }

            // --------------------------------------------------
            // Result
            // --------------------------------------------------

            CandleNMin candle = new CandleNMin();

            candle.setTime(bucketTime);
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            candle.setVolume(volume);

            candle.setEma9(ema9);
            candle.setVwap(vwap);
            candle.setCumulativeVolume(cumulativeVolume);

            // Add ATR
            candle.setAtr(currentAtr);

            result.add(candle);

            // Current candle becomes previous candle
            previousCandle = candle;
        }

        return result;
    }


    private static boolean isPriceContinuouslyIncreasing(List<CandleNMin> candles, int numberOfCandles) {

        if (candles == null || candles.size() < numberOfCandles) {
            return false;
        }

        int startIndex = candles.size() - numberOfCandles;

        for (int i = startIndex + 1; i < candles.size(); i++) {

            double previousPrice = candles.get(i - 1).getClose();
            double currentPrice = candles.get(i).getClose();

            if (currentPrice <= previousPrice) {
                return false;
            }
        }

        return true;
    }

    private static boolean isVolumeContinuouslyIncreasing(List<CandleNMin> candles, int numberOfCandles) {

        if (candles == null || candles.size() < numberOfCandles) {
            return false;
        }

        int startIndex = candles.size() - numberOfCandles;

        for (int i = startIndex + 1; i < candles.size(); i++) {

            double prevVolume = candles.get(i - 1).getVolume();
            double currentVolume = candles.get(i).getVolume();

            if (currentVolume <= prevVolume) {
                return false;
            }
        }

        return true;
    }
}
