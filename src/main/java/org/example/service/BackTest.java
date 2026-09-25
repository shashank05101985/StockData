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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BackTest {

    static void main() throws SQLException {
        backTest();
    }

    public static void backTest() throws SQLException {

        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);
        LocalDate today = LocalDate.now();
        AtomicReference<LocalTime> endTime =
                new AtomicReference<>(LocalTime.of(9, 18));
        Map<String, FundamentalData> fundamentalDataMap = StockDailyFeatureRepository.loadFundamentals(DB.get());
        Map<String, PreviousDayData> previousDayDataMap =
                StockDailyFeatureRepository.loadPreviousDayClose(
                        today,
                        DB.get(),
                        1
                );

        while (true) {
            try {

                LocalTime currentEndTime = endTime.get();
                if (currentEndTime.isAfter(LocalTime.of(15, 10)))
                    System.exit(0);
                System.out.println(
                        "EMA/VWAP calculation: endTime = " + currentEndTime
                );

                ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory =
                        DataLoader.loadMinuteHistory(
                                DB.get(),
                                today.minusDays(1),
                                today,
                                // Previous day
                                LocalTime.of(15, 10),
                                LocalTime.of(15, 30),

                                // Today
                                LocalTime.of(9, 15),
                                currentEndTime
                        );

                calculateEMAandVWAP(
                        null,
                        previousDayDataMap,
                        fundamentalDataMap,
                        minuteHistory,
                        1.5
                );

                // Next execution: +3 minutes
                endTime.updateAndGet(t -> t.plusMinutes(3));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void calculateEMAandVWAP(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory, double maxStopLossPercent) {

        List<Map.Entry<String, CandleNMin>> results = new ArrayList<>();

        minuteHistory.forEach((symbol, minuteCandles) -> {

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);
            List<CandleNMin> candleNMins = calculateTimeBasedEmaAndVwap(candles, 3);
            //print5MinCandles(candles5mins);
            CandleNMin candle = candleNMins.getLast();
            double stockPrice = candle.getClose();
            FundamentalData stockFundamental = fundamentalDataMap.get(symbol);
            PreviousDayData previousDayData = previousDayDataMap.get(symbol);
            if (previousDayData == null) {
                return;
            }

            double stockClosePrice = previousDayData.getClosePrice();
            double percentageChange = 0.0;

            if (stockClosePrice > 0) {
                percentageChange = ((stockPrice - stockClosePrice) / stockClosePrice) * 100.0;
            }
            if (stockPrice > candle.getEma9() && stockPrice > candle.getVwap() && stockPrice > 500 && stockPrice < 2500 && (stockFundamental != null && stockFundamental.getMarketCap() > 1500) && candle.getCumulativeVolume() > 100000 && percentageChange > 2.0) {
                //System.out.println(symbol + " " + candle.toString());
                //results.add(Map.entry(symbol, candle));
                boolean continuouslyIncreasing = isPriceContinuouslyIncreasing(candleNMins, 3);

                if (continuouslyIncreasing) {
                    results.add(
                            Map.entry(symbol, candle)
                    );
                }
            }


        });

        results.sort(
                Comparator.comparingDouble(
                        (Map.Entry<String, CandleNMin> entry) -> {
                            double currentPrice = entry.getValue().getClose();
                            double previousClose = previousDayDataMap.get(entry.getKey()).getClosePrice();
                            return ((currentPrice - previousClose) / previousClose) * 100.0;
                        }
                ).reversed()
        );

        // Print results
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Scanned at : " + LocalDateTime.now().format(formatter));
        results.forEach(entry -> {

            String symbol = entry.getKey();
            CandleNMin candle = entry.getValue();
            PreviousDayData previousDayData = previousDayDataMap.get(symbol);

            double stockPrice = candle.getClose();
            double previousClose = previousDayData.getClosePrice();

            double percentageChange =
                    ((stockPrice - previousClose) / previousClose) * 100.0;

            System.out.println(
                    symbol
                            + " ------ Price: " + candle.getClose()
                            + " ------ Percentage Change: " + String.format("%.2f", percentageChange) + "%"
                            + " ------ EMA9: " + candle.getEma9()
                            + " ------ VWAP: " + candle.getVwap()
                            + " ------ Volume: " + candle.getCumulativeVolume()
            );
        });
    }

    public static List<CandleNMin> calculateTimeBasedEmaAndVwap(
            List<MinuteCandle> minuteCandles, int minutes) {

        // 1. Group minute candles into 5-minute buckets
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

        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : grouped.entrySet()) {

            LocalDateTime bucketTime = entry.getKey();
            List<MinuteCandle> candles = entry.getValue();

            candles.sort(Comparator.comparing(MinuteCandle::getTime));

            // Reset VWAP and EMA at new trading day
            if (!bucketTime.toLocalDate().equals(currentDate)) {
                currentDate = bucketTime.toLocalDate();

                cumulativePV = 0.0;
                cumulativeVolume = 0.0;

                ema9 = 0.0;
            }

            // -----------------------------
            // Build 5-minute candle
            // -----------------------------

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

            // -----------------------------
            // EMA 9
            // -----------------------------

            if (ema9 == 0.0) {
                ema9 = close;
            } else {
                ema9 = (close * alpha) + (ema9 * (1.0 - alpha));
            }

            // -----------------------------
            // VWAP
            // Typical price = (H + L + C) / 3
            // -----------------------------

            double typicalPrice = (high + low + close) / 3.0;

            cumulativePV += typicalPrice * volume;
            cumulativeVolume += volume;

            double vwap = cumulativeVolume > 0
                    ? cumulativePV / cumulativeVolume
                    : close;

            // -----------------------------
            // Result
            // -----------------------------

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


            result.add(candle);
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
}
