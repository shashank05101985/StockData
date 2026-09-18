package org.example.analysis;

import org.example.model.Candle;
import org.example.model.PotentialStock;
import org.example.util.StockUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class PotentiakStockAnaluzer {
    public List<PotentialStock> scanPotentialStocks() {

        Collection<String> symbols = StockUtil.getStockList();
        LocalDate date = LocalDate.now().minusDays(2);

        List<PotentialStock> results = new ArrayList<>();

        for (String symbol : symbols) {

            try {
                PotentialStock result = analyze(symbol,date);

                if (result != null && result.score() >= 60) {
                    results.add(result);
                }

            } catch (Exception e) {
                System.err.println(
                    "Failed scanning " + symbol + ": " + e.getMessage()
                );
            }
        }

        results.sort(
            Comparator.comparingDouble(PotentialStock::score)
                .reversed()
        );

        return results;
    }
    private PotentialStock analyze(String symbol,LocalDate date) {

        List<Candle> candles = loadRecentCandles(symbol, 100,date);

        if (candles.size() < 30) {
            return null;
        }

        Candle current = candles.get(candles.size() - 1);
        Candle previous = candles.get(candles.size() - 2);

        double price = current.getClose();

        // --------------------------------------------------
        // 1-minute momentum
        // --------------------------------------------------

        double change1m =
            ((current.getClose() - previous.getClose())
                / previous.getClose()) * 100.0;

        double close5mAgo =
            candles.get(candles.size() - 6).getClose();

        double change5m =
            ((price - close5mAgo)
                / close5mAgo) * 100.0;

        // --------------------------------------------------
        // Volume
        // --------------------------------------------------

        double avgVolume = averageVolume(
            candles.subList(
                Math.max(0, candles.size() - 21),
                candles.size() - 1
            )
        );

        double volumeRatio =
            avgVolume > 0
                ? current.getVolume() / avgVolume
                : 0;

        // --------------------------------------------------
        // EMA
        // --------------------------------------------------

        double ema20 = calculateEMA(candles, 20);

        // --------------------------------------------------
        // VWAP
        // --------------------------------------------------

        double vwap = calculateVWAP(candles);

        // --------------------------------------------------
        // Recent resistance
        // --------------------------------------------------

        double resistance = highestHigh(
            candles.subList(
                Math.max(0, candles.size() - 21),
                candles.size() - 1
            )
        );

        boolean priceBreakout = price > resistance;

        // --------------------------------------------------
        // Score
        // --------------------------------------------------

        double score = 0;

        List<String> reasons = new ArrayList<>();

        // 1m momentum
        if (change1m >= 0.30) {
            score += 10;
            reasons.add("1m momentum");
        } else if (change1m >= 0.15) {
            score += 5;
        }

        // 5m momentum
        if (change5m >= 1.0) {
            score += 15;
            reasons.add("strong 5m momentum");
        } else if (change5m >= 0.50) {
            score += 8;
            reasons.add("positive 5m momentum");
        }

        // Volume
        if (volumeRatio >= 5.0) {
            score += 20;
            reasons.add("exceptional volume");
        } else if (volumeRatio >= 3.0) {
            score += 15;
            reasons.add("volume breakout");
        } else if (volumeRatio >= 2.0) {
            score += 8;
            reasons.add("high volume");
        }

        // EMA
        if (price > ema20) {
            score += 10;
            reasons.add("above EMA20");
        }

        // VWAP
        if (price > vwap) {
            score += 10;
            reasons.add("above VWAP");
        }

        // Breakout
        if (priceBreakout) {
            score += 20;
            reasons.add("resistance breakout");
        }

        // Candle
        double candleRange =
            current.getHigh() - current.getLow();

        if (candleRange > 0) {

            double body =
                Math.abs(current.getClose() - current.getOpen());

            double bodyRatio =
                body / candleRange;

            if (current.getClose() > current.getOpen()
                && bodyRatio >= 0.60) {

                score += 10;
                reasons.add("strong bullish candle");
            }
        }

        // --------------------------------------------------
        // Extension penalty
        // --------------------------------------------------

        if (change5m > 4.0) {
            score -= 10;
            reasons.add("5m overextended");
        }

        if (change1m > 2.0) {
            score -= 10;
            reasons.add("1m spike risk");
        }

        // --------------------------------------------------
        // Signal
        // --------------------------------------------------

        String signal;

        if (score >= 80) {
            signal = "STRONG BUY";
        } else if (score >= 70) {
            signal = "BUY";
        } else if (score >= 60) {
            signal = "WATCH";
        } else {
            signal = "NO TRADE";
        }

        if (score < 60) {
            return null;
        }

        return new PotentialStock(
            symbol,
            Math.min(100, Math.max(0, score)),
            price,
            volumeRatio,
            change1m,
            change5m,
            vwap,
            ema20,
            resistance,
            signal,
            String.join(", ", reasons)
        );
    }

    private List<Candle> loadCandlesUntil(
        String symbol,
        LocalDate tradingDate,
        LocalTime fromTime,
        LocalTime toTime) {

        String sql = """
        SELECT
            time,
            symbol,
            open,
            high,
            low,
            close,
            volume,
            oi,
            trades
        FROM candle_data
        WHERE symbol = ?
          AND time >= ?
          AND time < ?
        ORDER BY time ASC
        """;

        List<Candle> candles = new ArrayList<>();

        String url = "jdbc:postgresql://localhost:5432/stockdb";
        String user = "shashankmishra";
        String pass = "password";

        ZoneId zone = ZoneId.of("Asia/Kolkata");

        ZonedDateTime from =
            tradingDate
                .atTime(fromTime)
                .atZone(zone);

        ZonedDateTime to =
            tradingDate
                .atTime(toTime)
                .atZone(zone);

        try (Connection conn =
                 DriverManager.getConnection(url, user, pass);
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, symbol);

            ps.setTimestamp(
                2,
                Timestamp.from(from.toInstant())
            );

            ps.setTimestamp(
                3,
                Timestamp.from(to.toInstant())
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    candles.add(
                        new Candle(
                            rs.getTimestamp("time")
                                .toLocalDateTime(),

                            rs.getString("symbol"),

                            rs.getDouble("open"),
                            rs.getDouble("high"),
                            rs.getDouble("low"),
                            rs.getDouble("close"),

                            rs.getLong("volume"),
                            rs.getLong("oi"),
                            rs.getLong("trades")
                        )
                    );
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                "Failed loading candles for "
                    + symbol
                    + " from "
                    + from
                    + " to "
                    + to,
                e
            );
        }

        return candles;
    }

    private List<Candle> loadRecentCandles(
        String symbol,
        int limit,
        LocalDate tradingDate) {

        String sql = """
        SELECT
            time,
            symbol,
            open,
            high,
            low,
            close,
            volume,
            oi,
            trades
        FROM candle_data
        WHERE symbol = ?
          AND time >= ?::date + TIME '09:15:00'
          AND time <  ?::date + TIME '10:30:00'
        ORDER BY time DESC
        LIMIT ?
        """;

        List<Candle> candles = new ArrayList<>();

        String url = "jdbc:postgresql://localhost:5432/stockdb";
        String user = "shashankmishra";
        String pass = "password";

        try (Connection conn =
                 DriverManager.getConnection(url, user, pass);
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setObject(2, tradingDate);
            ps.setObject(3, tradingDate);
            ps.setInt(4, limit);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    candles.add(
                        new Candle(
                            rs.getTimestamp("time")
                                .toLocalDateTime(),

                            rs.getString("symbol"),

                            rs.getDouble("open"),

                            rs.getDouble("high"),

                            rs.getDouble("low"),

                            rs.getDouble("close"),

                            rs.getLong("volume"),

                            rs.getLong("oi"),

                            rs.getLong("trades")
                        )
                    );
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                "Failed loading candles for "
                    + symbol
                    + " on "
                    + tradingDate,
                e
            );
        }

        // Database returns newest -> oldest.
        // Indicators need oldest -> newest.
        Collections.reverse(candles);

        return candles;
    }
    private double averageVolume(List<Candle> candles) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        return candles.stream()
            .mapToLong(Candle::getVolume)
            .average()
            .orElse(0.0);
    }
    private double calculateEMA(
        List<Candle> candles,
        int period
    ) {

        if (candles == null || candles.size() < period) {
            return 0.0;
        }

        double multiplier =
            2.0 / (period + 1.0);

        // Initial SMA
        double ema = 0.0;

        for (int i = 0; i < period; i++) {
            ema += candles.get(i).getClose();
        }

        ema /= period;

        // EMA
        for (int i = period; i < candles.size(); i++) {

            double close =
                candles.get(i).getClose();

            ema =
                ((close - ema) * multiplier)
                    + ema;
        }

        return ema;
    }

    private double calculateVWAP(List<Candle> candles) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        double totalPV = 0.0;
        long totalVolume = 0;

        for (Candle candle : candles) {

            double typicalPrice =
                (candle.getHigh()
                    + candle.getLow()
                    + candle.getClose()) / 3.0;

            long volume = candle.getVolume();

            if (volume <= 0) {
                continue;
            }

            totalPV += typicalPrice * volume;
            totalVolume += volume;
        }

        if (totalVolume == 0) {
            return candles.get(candles.size() - 1).getClose();
        }

        return totalPV / totalVolume;
    }

    private double highestHigh(List<Candle> candles) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        double highest = Double.MIN_VALUE;

        for (Candle candle : candles) {
            highest = Math.max(highest, candle.getHigh());
        }

        return highest;
    }

    static void main() {

    }
}
