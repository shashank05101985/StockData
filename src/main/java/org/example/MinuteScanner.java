package org.example;

import org.example.loader.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinuteScanner {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static void scanIncreasingBuySellRatio(Connection connection, OffsetDateTime targetTime) throws SQLException {

        System.out.println("\n==========================================");
        System.out.println("Scanning minute: " + targetTime);
        System.out.println("==========================================");

        String sql = """
            WITH candles AS (
                SELECT
                    symbol,
                    time,
                    buy_sell_ratio,
                    close_price,
            
                    LAG(buy_sell_ratio, 1) OVER (
                        PARTITION BY symbol
                        ORDER BY time
                    ) AS ratio_1m,
            
                    LAG(buy_sell_ratio, 2) OVER (
                        PARTITION BY symbol
                        ORDER BY time
                    ) AS ratio_2m,
            
                    LAG(close_price, 1) OVER (
                        PARTITION BY symbol
                        ORDER BY time
                    ) AS price_1m,
            
                    LAG(close_price, 2) OVER (
                        PARTITION BY symbol
                        ORDER BY time
                    ) AS price_2m
            
                FROM minute_candle
            
                WHERE time >= ?
                  AND time <= ?
            )
            
            SELECT
                symbol,
            
                ratio_2m,
                ratio_1m,
                buy_sell_ratio AS current_ratio,
            
                price_2m,
                price_1m,
                close_price AS current_price,
            
                ROUND(
                    (
                        (buy_sell_ratio - ratio_1m)
                        / NULLIF(ratio_1m, 0) * 100
                    )::numeric,
                    2
                ) AS ratio_increase_pct,
            
                ROUND(
                    (
                        (close_price - price_1m)
                        / NULLIF(price_1m, 0) * 100
                    )::numeric,
                    2
                ) AS price_change_pct
            
            FROM candles
            
            WHERE time = ?
            
              -- ==========================================
              -- 3 MINUTES CONTINUOUS RATIO INCREASE
              -- ==========================================
            
              AND ratio_2m < ratio_1m
              AND ratio_1m < buy_sell_ratio
            
              -- ==========================================
              -- 3 MINUTES CONTINUOUS PRICE INCREASE
              -- ==========================================
            
              AND price_2m < price_1m
              AND price_1m < close_price
            
              -- ==========================================
              -- STRONG CURRENT BUYING PRESSURE
              -- ==========================================
            
              AND buy_sell_ratio >= 1.5
            
            ORDER BY buy_sell_ratio DESC
            
            LIMIT 5
            """;

        // We need 2 previous minutes
        OffsetDateTime startTime = targetTime.minusMinutes(2);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setObject(1, startTime);
            ps.setObject(2, targetTime);
            ps.setObject(3, targetTime);

            try (ResultSet rs = ps.executeQuery()) {

                int rank = 1;

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    double ratio2 = rs.getDouble("ratio_2m");

                    double ratio1 = rs.getDouble("ratio_1m");

                    double currentRatio = rs.getDouble("current_ratio");

                    double price2 = rs.getDouble("price_2m");

                    double price1 = rs.getDouble("price_1m");

                    double currentPrice = rs.getDouble("current_price");

                    double ratioIncrease = rs.getDouble("ratio_increase_pct");

                    double priceChange = rs.getDouble("price_change_pct");

                    System.out.printf(
                        "%d. %-15s " + "Ratio: %.2f -> %.2f -> %.2f " + "(+%.2f%%) | " + "Price: %.2f -> %.2f -> %.2f " + "(+%.2f%%)%n",

                        rank++, symbol,

                        ratio2, ratio1, currentRatio, ratioIncrease,

                        price2, price1, currentPrice, priceChange);
                }
            }
        }
    }

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");

    private static final LocalTime START_TIME = LocalTime.of(9, 16);

    private static final LocalTime END_TIME = LocalTime.of(15, 30);


    public static void main(String[] args) {

        LocalDate today = LocalDate.now(MARKET_ZONE);

        LocalTime startTime = LocalTime.of(9, 16);

        LocalDateTime nextScan = LocalDateTime.of(today, startTime);

        while (!nextScan.toLocalTime().isAfter(END_TIME)) {

            // =====================================================
            // WAIT UNTIL TARGET MINUTE
            // =====================================================

            LocalDateTime now = LocalDateTime.now(MARKET_ZONE);

            long waitMillis = java.time.Duration.between(now, nextScan).toMillis();

            if (waitMillis > 0) {

                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                    break;
                }
            }


            // =====================================================
            // SCAN
            // =====================================================

            System.out.println("\n==========================================");

            System.out.println("Scanning time: " + nextScan.toLocalTime());


            OffsetDateTime targetTime = nextScan.atZone(MARKET_ZONE).toOffsetDateTime();

            System.out.println("Target minute: " + targetTime);


            try (Connection connection = DB.get()) {

                scanIncreasingBuySellRatio(connection, targetTime);

            } catch (Exception e) {

                e.printStackTrace();
            }


            // =====================================================
            // NEXT MINUTE
            // =====================================================

            nextScan = nextScan.plusMinutes(1);
        }

        System.out.println("\nMarket scan finished for today.");
    }

}
