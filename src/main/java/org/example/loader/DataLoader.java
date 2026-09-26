package org.example.loader;

import org.example.model.MinuteCandle;
import org.example.model.StockData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataLoader {

    private static final ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory = new ConcurrentHashMap<>();
    public static Map<String, List<StockData>> loadLastNDays(
        Connection connection,
        LocalDate backtestDate,
        int numberOfDays) throws SQLException {

        String sql = """
        SELECT
            symbol,
            date,
            open_price,
            high_price,
            low_price,
            close_price,
            traded_qty,
            delivery_qty,
            delivery_percent,
            number_of_trades,
            turnover,
            series
        FROM (
            SELECT
                s.*,
                ROW_NUMBER() OVER (
                    PARTITION BY symbol
                    ORDER BY date DESC
                ) AS rn
            FROM stock_data s
            WHERE series = 'EQ'
              AND date <= ?
        ) x
        WHERE rn <= ?
        ORDER BY symbol, date ASC
        """;

        Map<String, List<StockData>> stockDataMap = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(backtestDate));
            ps.setInt(2, numberOfDays);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    StockData data = new StockData();

                    data.setSymbol(symbol);

                    java.sql.Date sqlDate = rs.getDate("date");
                    if (sqlDate != null) {
                        data.setDate(sqlDate.toLocalDate());
                    }

                    data.setOpen(rs.getDouble("open_price"));
                    data.setHigh(rs.getDouble("high_price"));
                    data.setLow(rs.getDouble("low_price"));
                    data.setClose(rs.getDouble("close_price"));

                    data.setTradedQty(rs.getLong("traded_qty"));
                    data.setDeliveredQty(rs.getLong("delivery_qty"));

                    data.setDeliveryPercent(
                        rs.getDouble("delivery_percent")
                    );

                    data.setTurnover(
                        rs.getDouble("turnover")
                    );

                    stockDataMap
                        .computeIfAbsent(symbol, k -> new ArrayList<>())
                        .add(data);
                }
            }
        }

        return stockDataMap;
    }

    public static ConcurrentHashMap<String, Deque<MinuteCandle>> loadMinuteHistory(
        Connection conn,
        LocalDate tradeDate,
        LocalTime startTime,
        LocalTime endTime) throws SQLException {

        String sql = """
        SELECT
            symbol,
            time,
            open_price,
            high_price,
            low_price,
            close_price,
            volume,
            tick_count,
            last_quantity,
            day_average_price,
            total_buy_qty,
            total_sell_qty,
            buy_sell_ratio,
            oi,
            oi_high,
            oi_low,
            change_percent,
            body,
            upper_wick,
            lower_wick,
            range,
            price_change,
            price_velocity,
            start_tick_time,
            last_tick_time
        FROM minute_candle
        WHERE (time AT TIME ZONE 'Asia/Kolkata')::date = ?
          AND (time AT TIME ZONE 'Asia/Kolkata')::time >= ?
          AND (time AT TIME ZONE 'Asia/Kolkata')::time <= ?
        ORDER BY symbol, time
        """;

        ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory =
            new ConcurrentHashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, tradeDate);
            ps.setObject(2, startTime);
            ps.setObject(3, endTime);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    MinuteCandle candle = new MinuteCandle();

                    candle.setSymbol(symbol);

                    candle.setTime(
                        rs.getTimestamp("time")
                            .toInstant()
                            .atZone(ZoneId.of("Asia/Kolkata"))
                            .toLocalDateTime()
                    );

                    candle.setOpen(rs.getDouble("open_price"));
                    candle.setHigh(rs.getDouble("high_price"));
                    candle.setLow(rs.getDouble("low_price"));
                    candle.setClose(rs.getDouble("close_price"));

                    candle.setVolume(rs.getLong("volume"));
                    candle.setTickCount(rs.getInt("tick_count"));
                    candle.setLastQuantity(rs.getLong("last_quantity"));

                    candle.setDayAveragePrice(
                        rs.getDouble("day_average_price")
                    );

                    candle.setTotalBuyQty(
                        rs.getLong("total_buy_qty")
                    );

                    candle.setTotalSellQty(
                        rs.getLong("total_sell_qty")
                    );

                    candle.setBuySellRatio(
                        rs.getDouble("buy_sell_ratio")
                    );

                    candle.setOi(rs.getLong("oi"));
                    candle.setOiHigh(rs.getLong("oi_high"));
                    candle.setOiLow(rs.getLong("oi_low"));

                    candle.setChange(
                        rs.getDouble("change_percent")
                    );

                    candle.setBody(rs.getDouble("body"));
                    candle.setUpperWick(rs.getDouble("upper_wick"));
                    candle.setLowerWick(rs.getDouble("lower_wick"));
                    candle.setRange(rs.getDouble("range"));
                    candle.setPriceChange(rs.getDouble("price_change"));
                    candle.setPriceVelocity(rs.getDouble("price_velocity"));

                    Deque<MinuteCandle> history =
                        minuteHistory.computeIfAbsent(
                            symbol,
                            k -> new ArrayDeque<>(400)
                        );

                    history.addLast(candle);
                }
            }
        }

        System.out.println(
            "Loaded minute history for "
                + tradeDate
                + " : "
                + minuteHistory.size()
                + " symbols"
                + " | "
                + startTime
                + " -> "
                + endTime
        );

        return minuteHistory;
    }

    public static ConcurrentHashMap<String, Deque<MinuteCandle>> loadMinuteHistory(
            Connection conn,
            LocalDate previousTradingDate,
            LocalDate tradeDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime,
            LocalTime currentStartTime,
            LocalTime currentEndTime) throws SQLException {

        String sql = """
        SELECT
            symbol,
            time,
            open_price,
            high_price,
            low_price,
            close_price,
            volume,
            tick_count,
            last_quantity,
            day_average_price,
            total_buy_qty,
            total_sell_qty,
            buy_sell_ratio,
            oi,
            oi_high,
            oi_low,
            change_percent,
            body,
            upper_wick,
            lower_wick,
            range,
            price_change,
            price_velocity,
            start_tick_time,
            last_tick_time
        FROM minute_candle
        WHERE
            (
                (time AT TIME ZONE 'Asia/Kolkata')::date = ?
                AND (time AT TIME ZONE 'Asia/Kolkata')::time >= ?
                AND (time AT TIME ZONE 'Asia/Kolkata')::time <= ?
            )
            OR
            (
                (time AT TIME ZONE 'Asia/Kolkata')::date = ?
                AND (time AT TIME ZONE 'Asia/Kolkata')::time >= ?
                AND (time AT TIME ZONE 'Asia/Kolkata')::time <= ?
            )
        ORDER BY symbol, time
        """;

        ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory =
                new ConcurrentHashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            /*
             * Previous trading day
             */
            ps.setObject(1, previousTradingDate);
            ps.setObject(2, previousStartTime);
            ps.setObject(3, previousEndTime);

            /*
             * Current trading day
             */
            ps.setObject(4, tradeDate);
            ps.setObject(5, currentStartTime);
            ps.setObject(6, currentEndTime);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    MinuteCandle candle = new MinuteCandle();

                    candle.setSymbol(symbol);

                    /*
                     * PostgreSQL `time` is assumed to be TIMESTAMPTZ.
                     *
                     * Convert the timestamp into Asia/Kolkata explicitly.
                     */
                    candle.setTime(
                            rs.getTimestamp("time")
                                    .toInstant()
                                    .atZone(ZoneId.of("Asia/Kolkata"))
                                    .toLocalDateTime()
                    );

                    candle.setOpen(
                            rs.getDouble("open_price")
                    );

                    candle.setHigh(
                            rs.getDouble("high_price")
                    );

                    candle.setLow(
                            rs.getDouble("low_price")
                    );

                    candle.setClose(
                            rs.getDouble("close_price")
                    );

                    candle.setVolume(
                            rs.getLong("volume")
                    );

                    candle.setTickCount(
                            rs.getInt("tick_count")
                    );

                    candle.setLastQuantity(
                            rs.getLong("last_quantity")
                    );

                    candle.setDayAveragePrice(
                            rs.getDouble("day_average_price")
                    );

                    candle.setTotalBuyQty(
                            rs.getLong("total_buy_qty")
                    );

                    candle.setTotalSellQty(
                            rs.getLong("total_sell_qty")
                    );

                    candle.setBuySellRatio(
                            rs.getDouble("buy_sell_ratio")
                    );

                    candle.setOi(
                            rs.getLong("oi")
                    );

                    candle.setOiHigh(
                            rs.getLong("oi_high")
                    );

                    candle.setOiLow(
                            rs.getLong("oi_low")
                    );

                    candle.setChange(
                            rs.getDouble("change_percent")
                    );

                    candle.setBody(
                            rs.getDouble("body")
                    );

                    candle.setUpperWick(
                            rs.getDouble("upper_wick")
                    );

                    candle.setLowerWick(
                            rs.getDouble("lower_wick")
                    );

                    candle.setRange(
                            rs.getDouble("range")
                    );

                    candle.setPriceChange(
                            rs.getDouble("price_change")
                    );

                    candle.setPriceVelocity(
                            rs.getDouble("price_velocity")
                    );

                    /*
                     * Add candle to symbol history.
                     */
                    Deque<MinuteCandle> history =
                            minuteHistory.computeIfAbsent(
                                    symbol,
                                    k -> new ArrayDeque<>(800)
                            );

                    history.addLast(candle);
                }
            }
        }

        System.out.println(
                "Loaded minute history"
                        + " | Previous Day: "
                        + previousTradingDate
                        + " "
                        + previousStartTime
                        + " -> "
                        + previousEndTime
                        + " | Current Day: "
                        + tradeDate
                        + " "
                        + currentStartTime
                        + " -> "
                        + currentEndTime
                        + " | Symbols: "
                        + minuteHistory.size()
        );

        return minuteHistory;
    }

    public static Map<LocalDateTime, List<MinuteCandle>> loadMinuteHistoryBasedOnTime(
            Connection conn,
            LocalDate previousTradingDate,
            LocalDate tradeDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime,
            LocalTime currentStartTime,
            LocalTime currentEndTime) throws SQLException {

        String sql = """
    SELECT
        symbol,
        time,
        open_price,
        high_price,
        low_price,
        close_price,
        volume,
        tick_count,
        last_quantity,
        day_average_price,
        total_buy_qty,
        total_sell_qty,
        buy_sell_ratio,
        oi,
        oi_high,
        oi_low,
        change_percent,
        body,
        upper_wick,
        lower_wick,
        range,
        price_change,
        price_velocity,
        start_tick_time,
        last_tick_time
    FROM minute_candle
    WHERE
        (
            (time AT TIME ZONE 'Asia/Kolkata')::date = ?
            AND (time AT TIME ZONE 'Asia/Kolkata')::time >= ?
            AND (time AT TIME ZONE 'Asia/Kolkata')::time <= ?
        )
        OR
        (
            (time AT TIME ZONE 'Asia/Kolkata')::date = ?
            AND (time AT TIME ZONE 'Asia/Kolkata')::time >= ?
            AND (time AT TIME ZONE 'Asia/Kolkata')::time <= ?
        )
    ORDER BY time ASC, symbol ASC
    """;

        Map<LocalDateTime, List<MinuteCandle>> minuteHistory =
                new TreeMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // Previous trading day
            ps.setObject(1, previousTradingDate);
            ps.setObject(2, previousStartTime);
            ps.setObject(3, previousEndTime);

            // Current trading day
            ps.setObject(4, tradeDate);
            ps.setObject(5, currentStartTime);
            ps.setObject(6, currentEndTime);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    MinuteCandle candle = new MinuteCandle();

                    candle.setSymbol(symbol);

                    LocalDateTime candleTime =
                            rs.getTimestamp("time")
                                    .toInstant()
                                    .atZone(ZoneId.of("Asia/Kolkata"))
                                    .toLocalDateTime();

                    candle.setTime(candleTime);

                    candle.setOpen(rs.getDouble("open_price"));
                    candle.setHigh(rs.getDouble("high_price"));
                    candle.setLow(rs.getDouble("low_price"));
                    candle.setClose(rs.getDouble("close_price"));

                    candle.setVolume(rs.getLong("volume"));
                    candle.setTickCount(rs.getInt("tick_count"));
                    candle.setLastQuantity(rs.getLong("last_quantity"));

                    candle.setDayAveragePrice(
                            rs.getDouble("day_average_price")
                    );

                    candle.setTotalBuyQty(
                            rs.getLong("total_buy_qty")
                    );

                    candle.setTotalSellQty(
                            rs.getLong("total_sell_qty")
                    );

                    candle.setBuySellRatio(
                            rs.getDouble("buy_sell_ratio")
                    );

                    candle.setOi(rs.getLong("oi"));
                    candle.setOiHigh(rs.getLong("oi_high"));
                    candle.setOiLow(rs.getLong("oi_low"));

                    candle.setChange(
                            rs.getDouble("change_percent")
                    );

                    candle.setBody(
                            rs.getDouble("body")
                    );

                    candle.setUpperWick(
                            rs.getDouble("upper_wick")
                    );

                    candle.setLowerWick(
                            rs.getDouble("lower_wick")
                    );

                    candle.setRange(
                            rs.getDouble("range")
                    );

                    candle.setPriceChange(
                            rs.getDouble("price_change")
                    );

                    candle.setPriceVelocity(
                            rs.getDouble("price_velocity")
                    );

                    /*
                     * Group candles by time.
                     *
                     * Example:
                     *
                     * 09:15 -> [RELIANCE candle, TCS candle, INFY candle...]
                     * 09:16 -> [RELIANCE candle, TCS candle, INFY candle...]
                     */
                    minuteHistory
                            .computeIfAbsent(
                                    candleTime,
                                    k -> new ArrayList<>()
                            )
                            .add(candle);
                }
            }
        }

        System.out.println(
                "Loaded minute history"
                        + " | Previous Day: "
                        + previousTradingDate
                        + " "
                        + previousStartTime
                        + " -> "
                        + previousEndTime
                        + " | Current Day: "
                        + tradeDate
                        + " "
                        + currentStartTime
                        + " -> "
                        + currentEndTime
                        + " | Times: "
                        + minuteHistory.size()
        );

        return minuteHistory;
    }

}
