package org.example.repository;


import org.example.loader.DB;
import org.example.model.MomentumCandidate;
import org.example.model.StockCandidate;
import org.example.model.StockMomentum;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

public class PotentialMoverScanner {

    /**
     * Find stocks with potential upward momentum
     * for the next 5-10 minutes.
     *
     * @param currentTime completed minute candle time
     * @param minPrice    minimum stock price
     * @param maxPrice    maximum stock price
     * @param limit       maximum candidates
     */
    public static List<MomentumCandidate> findPotentialStocks(
        LocalDateTime currentTime,
        double minPrice,
        double maxPrice,
        int limit) {

        if (currentTime == null) {
            return new ArrayList<>();
        }

        if (minPrice < 0 ||
            maxPrice <= 0 ||
            minPrice > maxPrice ||
            limit <= 0) {

            return new ArrayList<>();
        }

        /*
         * We need:
         *
         * rn 1       = latest completed candle
         * rn 2-6     = 1-5 minutes ago
         *
         * rn 7-21    = previous 15 candles
         *              for volume baseline
         *
         * Therefore we need 21 candles.
         */

        String sql = """

            WITH ranked AS (

                SELECT
                    time,
                    symbol,

                    open_price,
                    high_price,
                    low_price,
                    close_price,

                    volume,

                    total_buy_qty,
                    total_sell_qty,

                    buy_sell_ratio,

                    ROW_NUMBER() OVER (
                        PARTITION BY symbol
                        ORDER BY time DESC
                    ) AS rn

                FROM minute_candle

                WHERE time <= ?::timestamptz

                  AND time >=
                      (?::timestamptz - INTERVAL '20 minutes')

                  AND close_price >= ?
                  AND close_price <= ?
            ),

            data AS (

                SELECT
                    symbol,

                    /*
                     * ============================
                     * LATEST CANDLE
                     * ============================
                     */

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN open_price
                        END
                    ) AS last_open,

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN high_price
                        END
                    ) AS last_high,

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN low_price
                        END
                    ) AS last_low,

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN close_price
                        END
                    ) AS last_close,

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN volume
                        END
                    ) AS last_volume,

                    MAX(
                        CASE
                            WHEN rn = 1
                            THEN buy_sell_ratio
                        END
                    ) AS last_ratio,


                    /*
                     * ============================
                     * TRUE 5-MINUTE AGO CLOSE
                     * ============================
                     *
                     * rn 1 = current
                     * rn 6 = 5 minutes ago
                     */

                    MAX(
                        CASE
                            WHEN rn = 6
                            THEN close_price
                        END
                    ) AS close_5m_ago,


                    /*
                     * ============================
                     * PREVIOUS HIGH
                     * ============================
                     *
                     * Exclude current candle.
                     */

                    MAX(
                        CASE
                            WHEN rn BETWEEN 2 AND 5
                            THEN high_price
                        END
                    ) AS previous_high,


                    /*
                     * ============================
                     * 5-MINUTE AVERAGE B/S
                     * ============================
                     */

                    AVG(
                        CASE
                            WHEN rn BETWEEN 1 AND 5
                            THEN buy_sell_ratio
                        END
                    ) AS avg_buy_sell_ratio,


                    /*
                     * ============================
                     * VOLUME BASELINE
                     * ============================
                     *
                     * Previous 15 candles.
                     * Excludes current 5 candles.
                     */

                    AVG(
                        CASE
                            WHEN rn BETWEEN 7 AND 21
                            THEN volume
                        END
                    ) AS avg_previous_volume,


                    /*
                     * ============================
                     * POSITIVE CANDLES
                     * ============================
                     */

                    SUM(
                        CASE
                            WHEN rn BETWEEN 1 AND 5
                             AND close_price > open_price
                            THEN 1
                            ELSE 0
                        END
                    ) AS positive_candles,


                    /*
                     * ============================
                     * BUYING PRESSURE CANDLES
                     * ============================
                     */

                    SUM(
                        CASE
                            WHEN rn BETWEEN 1 AND 5
                             AND buy_sell_ratio >= 1.05
                            THEN 1
                            ELSE 0
                        END
                    ) AS buying_candles,


                    /*
                     * ============================
                     * 5-MINUTE TOTAL VOLUME
                     * ============================
                     */

                    SUM(
                        CASE
                            WHEN rn BETWEEN 1 AND 5
                            THEN volume
                            ELSE 0
                        END
                    ) AS volume_5m


                FROM ranked

                WHERE rn <= 21

                GROUP BY symbol
            )

            SELECT

                symbol,

                last_open,
                last_high,
                last_low,
                last_close,

                last_volume,
                last_ratio,

                close_5m_ago,

                previous_high,

                avg_buy_sell_ratio,
                avg_previous_volume,

                positive_candles,
                buying_candles,

                volume_5m

            FROM data

            WHERE close_5m_ago IS NOT NULL

              AND last_close > 0

              /*
               * ============================
               * PRICE MOMENTUM
               * ============================
               *
               * At least +0.30% over 5 minutes.
               */

              AND (
                    (last_close - close_5m_ago)
                    / NULLIF(close_5m_ago, 0)
                  ) * 100.0 >= 0.30

              /*
               * ============================
               * BUYING PRESSURE
               * ============================
               */

              AND last_ratio >= 1.10

              /*
               * ============================
               * PRICE CONSISTENCY
               * ============================
               *
               * At least 3 positive candles
               * out of the latest 5.
               */

              AND positive_candles >= 3

              /*
               * At least 3 of 5 candles
               * have B/S >= 1.05.
               */

              AND buying_candles >= 3

              /*
               * Current candle must have
               * meaningful volume.
               */

              AND last_volume > 0

              /*
               * Final price protection.
               */

              AND last_close >= ?
              AND last_close <= ?

            LIMIT ?

            """;

        List<MomentumCandidate> result =
            new ArrayList<>();

        try (
            Connection con = DB.get();
            PreparedStatement ps =
                con.prepareStatement(sql)
        ) {

            int i = 1;

            /*
             * Query end time
             */
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(currentTime)
            );

            /*
             * Query start time
             */
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(currentTime)
            );

            /*
             * Minimum price
             */
            ps.setDouble(
                i++,
                minPrice
            );

            /*
             * Maximum price
             */
            ps.setDouble(
                i++,
                maxPrice
            );

            /*
             * Final minimum price
             */
            ps.setDouble(
                i++,
                minPrice
            );

            /*
             * Final maximum price
             */
            ps.setDouble(
                i++,
                maxPrice
            );

            /*
             * LIMIT
             */
            ps.setInt(
                i++,
                limit
            );

            try (ResultSet rs =
                     ps.executeQuery()) {

                while (rs.next()) {

                    MomentumCandidate c =
                        new MomentumCandidate();

                    c.symbol =
                        rs.getString("symbol");


                    double lastOpen =
                        rs.getDouble("last_open");

                    double lastHigh =
                        rs.getDouble("last_high");

                    double lastLow =
                        rs.getDouble("last_low");

                    double lastClose =
                        rs.getDouble("last_close");

                    double close5m =
                        rs.getDouble(
                            "close_5m_ago"
                        );

                    double previousHigh =
                        rs.getDouble(
                            "previous_high"
                        );

                    c.currentVolume =
                        rs.getLong(
                            "last_volume"
                        );

                    c.lastBuySellRatio =
                        rs.getDouble(
                            "last_ratio"
                        );

                    c.avgBuySellRatio =
                        rs.getDouble(
                            "avg_buy_sell_ratio"
                        );

                    double avgVolume =
                        rs.getDouble(
                            "avg_previous_volume"
                        );

                    int positiveCandles =
                        rs.getInt(
                            "positive_candles"
                        );

                    int buyingCandles =
                        rs.getInt(
                            "buying_candles"
                        );

                    /*
                     * ==================================
                     * 1. 5-MINUTE PRICE MOMENTUM
                     * ==================================
                     */

                    c.priceChange5m =
                        close5m > 0
                            ? (
                            (lastClose - close5m)
                                / close5m
                        ) * 100.0
                            : 0.0;


                    /*
                     * ==================================
                     * 2. VOLUME RATIO
                     * ==================================
                     *
                     * Current 1-minute volume /
                     * average volume of previous
                     * 15 minutes.
                     */

                    c.volumeRatio =
                        avgVolume > 0
                            ? c.currentVolume
                            / avgVolume
                            : 0.0;


                    /*
                     * ==================================
                     * 3. CLOSE POSITION
                     * ==================================
                     */

                    double range =
                        lastHigh - lastLow;

                    c.closePosition =
                        range > 0
                            ? (
                            (lastClose - lastLow)
                                / range
                        ) * 100.0
                            : 0.0;


                    /*
                     * ==================================
                     * 4. BREAKOUT
                     * ==================================
                     */

                    c.highBreakoutPct =
                        previousHigh > 0
                            ? (
                            (lastClose - previousHigh)
                                / previousHigh
                        ) * 100.0
                            : 0.0;


                    /*
                     * ==================================
                     * 5. PRICE SCORE
                     * ==================================
                     *
                     * 0.30% = 6 points
                     * 1.00% = 20 points
                     * 2.00%+ = 40 points
                     */

                    double priceScore =
                        Math.min(
                            Math.max(
                                c.priceChange5m,
                                0.0
                            ),
                            2.0
                        )
                            / 2.0
                            * 40.0;


                    /*
                     * ==================================
                     * 6. VOLUME SCORE
                     * ==================================
                     *
                     * Cap at 3x.
                     *
                     * Prevents:
                     *
                     * 500x
                     * 5000x
                     * 45000x
                     *
                     * from dominating the score.
                     */

                    double volumeScore =
                        Math.min(
                            Math.max(
                                c.volumeRatio,
                                0.0
                            ),
                            3.0
                        )
                            / 3.0
                            * 15.0;


                    /*
                     * ==================================
                     * 7. BUY/SELL SCORE
                     * ==================================
                     *
                     * B/S 1.0 = 0
                     * B/S 1.5 = 7.5
                     * B/S 2.0+ = 15
                     */

                    double buySellScore =
                        Math.min(
                            Math.max(
                                c.lastBuySellRatio
                                    - 1.0,
                                0.0
                            ),
                            1.0
                        )
                            * 15.0;


                    /*
                     * ==================================
                     * 8. CONSISTENCY SCORE
                     * ==================================
                     *
                     * Positive candles = 5
                     * Buying candles  = 5
                     */

                    double consistencyScore =
                        (positiveCandles / 5.0)
                            * 5.0
                            +
                            (buyingCandles / 5.0)
                                * 5.0;


                    /*
                     * ==================================
                     * 9. CLOSE POSITION SCORE
                     * ==================================
                     */

                    double closeScore =
                        Math.min(
                            Math.max(
                                c.closePosition,
                                0.0
                            ),
                            100.0
                        )
                            / 100.0
                            * 10.0;


                    /*
                     * ==================================
                     * 10. BREAKOUT SCORE
                     * ==================================
                     *
                     * 0.00% = 0
                     * 0.25% = 2.5
                     * 0.50% = 5
                     * 1.00%+ = 10
                     */

                    double breakoutScore =
                        Math.min(
                            Math.max(
                                c.highBreakoutPct,
                                0.0
                            ),
                            1.0
                        )
                            * 10.0;


                    /*
                     * ==================================
                     * FINAL SCORE
                     * ==================================
                     *
                     * Price        40
                     * Volume       15
                     * B/S          15
                     * Consistency  10
                     * Close        10
                     * Breakout     10
                     *
                     * TOTAL       100
                     */

                    c.score =
                        priceScore
                            + volumeScore
                            + buySellScore
                            + consistencyScore
                            + closeScore
                            + breakoutScore;


                    /*
                     * ==================================
                     * STRONG CANDIDATE
                     * ==================================
                     */

                    c.strongCandidate =
                        c.score >= 70.0;
                    c.ltp = lastClose;


                    result.add(c);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        }


        /*
         * ==========================================
         * SORT BY SCORE
         * ==========================================
         */

        result.sort(
            Comparator
                .comparingDouble(
                    (MomentumCandidate c)
                        -> c.score
                )
                .reversed()
        );


        /*
         * ==========================================
         * RETURN TOP N
         * ==========================================
         */

        if (result.size() > limit) {

            return new ArrayList<>(
                result.subList(0, limit)
            );
        }

        return result;
    }

    public static List<StockCandidate> findPotentialStocks(
        Connection connection,
        LocalDateTime currentTime,
        int nMinutes) throws SQLException {

        String sql = """
        WITH candles AS (
            SELECT
                symbol,
                time,
                open_price,
                high_price,
                low_price,
                close_price,
                volume,
                tick_count,
                total_buy_qty,
                total_sell_qty,
                buy_sell_ratio,
                change_percent,
                body,
                upper_wick,
                lower_wick,
                range,
                price_change,
                price_velocity
            FROM minute_candle
            WHERE time >= ?
              AND time <= ?
              AND close_price > 0
        ),

        latest AS (
            SELECT DISTINCT ON (symbol)
                symbol,
                time,
                open_price,
                high_price,
                low_price,
                close_price,
                volume,
                tick_count,
                total_buy_qty,
                total_sell_qty,
                buy_sell_ratio,
                change_percent,
                body,
                upper_wick,
                lower_wick,
                range,
                price_change,
                price_velocity
            FROM candles
            ORDER BY symbol, time DESC
        ),

        previous AS (
            SELECT c.*
            FROM candles c
            JOIN latest l
                ON l.symbol = c.symbol
               AND c.time < l.time
        ),

        stats AS (
            SELECT
                symbol,

                MIN(low_price) AS period_low,
                MAX(high_price) AS period_high,

                AVG(close_price) AS avg_price,

                AVG(NULLIF(volume, 0)) AS avg_volume,

                SUM(volume) AS total_volume,

                AVG(range) AS avg_range,

                AVG(price_velocity) AS avg_velocity,

                AVG(buy_sell_ratio) AS avg_buy_sell_ratio,

                SUM(total_buy_qty) AS total_buy_qty,

                SUM(total_sell_qty) AS total_sell_qty,

                COUNT(*) AS candle_count

            FROM previous
            GROUP BY symbol
        ),

        first_candle AS (
            SELECT DISTINCT ON (symbol)
                symbol,
                close_price AS first_price
            FROM previous
            ORDER BY symbol, time ASC
        )

        SELECT

            l.symbol,
            l.time,

            l.close_price,
            f.first_price,

            s.period_high,
            s.period_low,

            s.avg_volume,
            s.total_volume,

            s.avg_range,
            s.avg_velocity,

            s.avg_buy_sell_ratio,

            s.total_buy_qty,
            s.total_sell_qty,

            l.volume,
            l.tick_count,

            l.buy_sell_ratio,
            l.change_percent,

            l.range,
            l.price_change,
            l.price_velocity,

            s.candle_count,

            -- ============================================
            -- RETURN
            -- ============================================

            CASE
                WHEN f.first_price > 0
                THEN (
                    (l.close_price - f.first_price)
                    / f.first_price
                ) * 100
                ELSE 0
            END AS period_return,

            -- ============================================
            -- VOLUME RATIO
            -- ============================================

            CASE
                WHEN s.avg_volume > 0
                     AND l.volume > 0
                THEN l.volume / s.avg_volume
                ELSE 0
            END AS volume_ratio,

            -- ============================================
            -- RANGE RATIO
            -- ============================================

            CASE
                WHEN s.avg_range > 0
                THEN l.range / s.avg_range
                ELSE 0
            END AS range_ratio,

            -- ============================================
            -- VELOCITY RATIO
            -- ============================================

            CASE
                WHEN s.avg_velocity > 0
                THEN l.price_velocity / s.avg_velocity
                ELSE 0
            END AS velocity_ratio,

            -- ============================================
            -- BREAKOUT
            -- ============================================

            CASE
                WHEN l.close_price > s.period_high
                THEN true
                ELSE false
            END AS breakout_high,

            -- ============================================
            -- BREAKDOWN
            -- ============================================

            CASE
                WHEN l.close_price < s.period_low
                THEN true
                ELSE false
            END AS breakout_low

        FROM latest l

        JOIN stats s
            ON s.symbol = l.symbol

        JOIN first_candle f
            ON f.symbol = l.symbol

        WHERE s.candle_count >= 1

        ORDER BY period_return DESC
        """;

        List<StockCandidate> candidates = new ArrayList<>();

        LocalDateTime from =
            currentTime.minusMinutes(nMinutes);

        ZoneId zone = ZoneId.of("Asia/Kolkata");

        OffsetDateTime fromTime =
            from.atZone(zone).toOffsetDateTime();

        OffsetDateTime toTime =
            currentTime.atZone(zone).toOffsetDateTime();

        System.out.println(
            "Scanning minute_candle: "
                + fromTime
                + " -> "
                + toTime
        );

        try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

            /*
             * IMPORTANT:
             *
             * minute_candle.time is TIMESTAMP WITH TIME ZONE.
             *
             * Therefore use OffsetDateTime rather than
             * Timestamp.valueOf(LocalDateTime).
             */

            ps.setObject(1, fromTime);
            ps.setObject(2, toTime);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockCandidate c =
                        new StockCandidate();

                    c.symbol =
                        rs.getString("symbol");

                    c.time =
                        rs.getTimestamp("time")
                            .toLocalDateTime();

                    c.price =
                        rs.getDouble("close_price");

                    c.firstPrice =
                        rs.getDouble("first_price");

                    c.periodHigh =
                        rs.getDouble("period_high");

                    c.periodLow =
                        rs.getDouble("period_low");

                    c.periodReturn =
                        rs.getDouble("period_return");

                    c.volume =
                        rs.getLong("volume");

                    c.avgVolume =
                        rs.getDouble("avg_volume");

                    c.volumeRatio =
                        rs.getDouble("volume_ratio");

                    c.range =
                        rs.getDouble("range");

                    c.rangeRatio =
                        rs.getDouble("range_ratio");

                    c.priceVelocity =
                        rs.getDouble("price_velocity");

                    c.velocityRatio =
                        rs.getDouble("velocity_ratio");

                    c.buySellRatio =
                        rs.getDouble("buy_sell_ratio");

                    c.changePercent =
                        rs.getDouble("change_percent");

                    c.breakoutHigh =
                        rs.getBoolean("breakout_high");

                    c.breakoutLow =
                        rs.getBoolean("breakout_low");

                    c.candleCount =
                        rs.getInt("candle_count");

                    c.score =
                        calculateScore(c);

                    candidates.add(c);
                }
            }
        }

        connection.commit();
        candidates.sort(
            Comparator.comparingInt(
                StockCandidate::getScore
            ).reversed()
        );

        System.out.println(
            "Potential stocks found: "
                + candidates.size()
        );

        return candidates;
    }

    private static int calculateScore(StockCandidate c) {

        int score = 0;

        if (c.periodReturn >= 3.0) {
            score += 20;
        } else if (c.periodReturn >= 2.0) {
            score += 15;
        } else if (c.periodReturn >= 1.0) {
            score += 10;
        } else if (c.periodReturn >= 0.5) {
            score += 5;
        }

        if (c.volumeRatio >= 3.0) {
            score += 25;
        } else if (c.volumeRatio >= 2.0) {
            score += 18;
        } else if (c.volumeRatio >= 1.5) {
            score += 10;
        }

        if (c.rangeRatio >= 2.0) {
            score += 15;
        } else if (c.rangeRatio >= 1.5) {
            score += 10;
        }

        if (c.velocityRatio >= 3.0) {
            score += 15;
        } else if (c.velocityRatio >= 2.0) {
            score += 10;
        }

        if (c.buySellRatio >= 1.50) {
            score += 15;
        } else if (c.buySellRatio >= 1.25) {
            score += 10;
        }

        if (c.breakoutHigh) {
            score += 10;
        }

        return Math.min(score, 100);
    }


    public static List<StockMomentum> findPreOpenGapStocks(
        Connection connection,
        LocalDateTime runTime,
        double minGapPct,
        double minBuySellRatio) throws SQLException {

        String sql = """
        WITH prev_trading_day AS (

            SELECT MAX(date) AS prev_date
            FROM stock_data
            WHERE date < ?

        ),

        prev_data AS (

            SELECT
                sd.symbol,
                sd.close_price AS prev_close
            FROM stock_data sd
            JOIN prev_trading_day p
                ON sd.date = p.prev_date

        ),

        preopen AS (

            SELECT DISTINCT ON (mc.symbol)

                mc.symbol,

                mc.time AS preopen_time,

                mc.close_price AS preopen_price,

                mc.total_buy_qty AS preopen_buy_qty,

                mc.total_sell_qty AS preopen_sell_qty,

                mc.buy_sell_ratio AS preopen_buy_sell_ratio

            FROM minute_candle mc

            WHERE mc.time >= ?
              AND mc.time <= ?

            ORDER BY
                mc.symbol,
                mc.time DESC
        )

        SELECT

            po.symbol,

            p.prev_close,

            po.preopen_time,

            po.preopen_price,

            /* PREOPEN GAP */

            ROUND(
                (
                    (po.preopen_price - p.prev_close)
                    / NULLIF(p.prev_close, 0)
                    * 100
                )::numeric,
                2
            ) AS preopen_gap_pct,

            po.preopen_buy_qty,

            po.preopen_sell_qty,

            po.preopen_buy_sell_ratio,

            /* BUY QUANTITY PRESSURE */

            (
                po.preopen_buy_qty -
                po.preopen_sell_qty
            ) AS buy_sell_qty_difference

        FROM preopen po

        JOIN prev_data p
            ON p.symbol = po.symbol

        WHERE

            (
                (po.preopen_price - p.prev_close)
                / NULLIF(p.prev_close, 0)
                * 100
            ) >= ?

            AND COALESCE(
                po.preopen_buy_sell_ratio,
                0
            ) >= ?

        ORDER BY
            preopen_gap_pct DESC
        """;

        LocalDate date =
            runTime.toLocalDate();

        /*
         * We want the latest pre-open
         * information available between
         * 09:01 and 09:07.
         */

        LocalDateTime preopenStart =
            date.atTime(9, 1);

        LocalDateTime preopenEnd =
            date.atTime(9, 7);

        List<StockMomentum> result =
            new ArrayList<>();

        try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

            int i = 1;

            // =========================================
            // PREVIOUS TRADING DAY
            // =========================================

            ps.setDate(
                i++,
                java.sql.Date.valueOf(date)
            );

            // =========================================
            // PREOPEN START
            // =========================================

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(preopenStart)
            );

            // =========================================
            // PREOPEN END
            // =========================================

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(preopenEnd)
            );

            // =========================================
            // MIN GAP
            // =========================================

            ps.setDouble(
                i++,
                minGapPct
            );

            // =========================================
            // MIN BUY/SELL RATIO
            // =========================================

            ps.setDouble(
                i++,
                minBuySellRatio
            );

            try (ResultSet rs =
                     ps.executeQuery()) {

                while (rs.next()) {

                    StockMomentum s =
                        new StockMomentum();

                    s.setSymbol(
                        rs.getString("symbol")
                    );

                    s.setPrevClose(
                        rs.getDouble("prev_close")
                    );

                    /*
                     * Use pre-open price as
                     * current price for this scan.
                     */

                    s.setCurrentClose(
                        rs.getDouble("preopen_price")
                    );

                    s.setCurrentTime(
                        rs.getTimestamp("preopen_time")
                            .toLocalDateTime()
                    );

                    s.setPreopenPrice(
                        rs.getDouble("preopen_price")
                    );

                    s.setPreopenGapPct(
                        rs.getDouble("preopen_gap_pct")
                    );

                    s.setPreopenBuyQty(
                        rs.getLong("preopen_buy_qty")
                    );

                    s.setPreopenSellQty(
                        rs.getLong("preopen_sell_qty")
                    );

                    s.setPreopenBuySellRatio(
                        rs.getDouble(
                            "preopen_buy_sell_ratio"
                        )
                    );

                    result.add(s);
                }
            }
            connection.commit();
        }

        return result;
    }

    public static List<StockMomentum> findStocks(
        Connection connection,
        LocalDateTime runTime,
        double minGapPct,
        double minMoveFrom0915Pct,
        double minBuySellRatio) throws SQLException {

        String sql = """
        WITH prev_trading_day AS (
            SELECT MAX(date) AS prev_date
            FROM stock_data
            WHERE date < ?
        ),

        prev_data AS (
            SELECT
                sd.symbol,
                sd.close_price AS prev_close
            FROM stock_data sd
            JOIN prev_trading_day p
                ON sd.date = p.prev_date
        ),

        open_915 AS (
            SELECT DISTINCT ON (mc.symbol)
                mc.symbol,
                mc.time AS time_0915,
                mc.close_price AS close_0915,
                mc.total_buy_qty AS buy_qty_0915,
                mc.total_sell_qty AS sell_qty_0915,
                mc.buy_sell_ratio AS buy_sell_ratio_0915
            FROM minute_candle mc
            WHERE mc.time >= ?
              AND mc.time < ?
            ORDER BY mc.symbol, mc.time
        ),

        run_price AS (
            SELECT DISTINCT ON (mc.symbol)
                mc.symbol,
                mc.time AS current_time,
                mc.close_price AS current_close,
                mc.total_buy_qty AS current_buy_qty,
                mc.total_sell_qty AS current_sell_qty,
                mc.buy_sell_ratio AS current_buy_sell_ratio
            FROM minute_candle mc
            WHERE mc.time >= ?
              AND mc.time <= ?
            ORDER BY mc.symbol, mc.time DESC
        )

        SELECT
            r.symbol,

            p.prev_close,
            o.close_0915,

            r.current_close,
            r.current_time,

            /* 09:15 GAP */

            ROUND(
                (
                    (o.close_0915 - p.prev_close)
                    / NULLIF(p.prev_close, 0) * 100
                )::numeric,
                2
            ) AS gap_0915_pct,

            /* CURRENT GAP */

            ROUND(
                (
                    (r.current_close - p.prev_close)
                    / NULLIF(p.prev_close, 0) * 100
                )::numeric,
                2
            ) AS gap_pct,

            /* MOVE FROM 09:15 */

            ROUND(
                (
                    (r.current_close - o.close_0915)
                    / NULLIF(o.close_0915, 0) * 100
                )::numeric,
                2
            ) AS move_from_0915_pct,

            /* TOTAL MOVE */

            ROUND(
                (
                    (r.current_close - p.prev_close)
                    / NULLIF(p.prev_close, 0) * 100
                )::numeric,
                2
            ) AS total_move_pct,

            /* BUY / SELL */

            o.buy_qty_0915,
            o.sell_qty_0915,
            o.buy_sell_ratio_0915,

            r.current_buy_qty,
            r.current_sell_qty,
            r.current_buy_sell_ratio,

            /* QUANTITY CHANGE */

            r.current_buy_qty - o.buy_qty_0915
                AS buy_qty_change,

            r.current_sell_qty - o.sell_qty_0915
                AS sell_qty_change,

            /* RATIO CHANGE */

            ROUND(
                (
                    r.current_buy_sell_ratio
                    - o.buy_sell_ratio_0915
                )::numeric,
                2
            ) AS buy_sell_ratio_change

        FROM run_price r

        JOIN open_915 o
            ON o.symbol = r.symbol

        JOIN prev_data p
            ON p.symbol = r.symbol

        WHERE

            /*
             * CASE 1:
             * Already gap-up at 09:15
             *
             * OR
             *
             * CASE 2:
             * Became gap-up by current time
             */

            (
                (
                    (o.close_0915 - p.prev_close)
                    / NULLIF(p.prev_close, 0) * 100
                ) >= ?

                OR

                (
                    (r.current_close - p.prev_close)
                    / NULLIF(p.prev_close, 0) * 100
                ) >= ?
            )

            /*
             * BUY / SELL FILTER
             *
             * COALESCE prevents NULL ratio
             * from eliminating the stock.
             */

            AND COALESCE(
                r.current_buy_sell_ratio,
                0
            ) >= ?

        ORDER BY
            gap_pct DESC
        """;

        LocalDate date = runTime.toLocalDate();

        LocalDateTime time0915 =
            date.atTime(9, 15);

        LocalDateTime time0916 =
            date.atTime(9, 16);

        List<StockMomentum> result =
            new ArrayList<>();

        try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

            int i = 1;

            // Previous trading day
            ps.setDate(
                i++,
                java.sql.Date.valueOf(date)
            );

            // 09:15 candle
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0915)
            );

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0916)
            );

            // Current data
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0915)
            );

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(runTime)
            );

            // 09:15 gap threshold
            ps.setDouble(
                i++,
                minGapPct
            );

            // Current gap threshold
            ps.setDouble(
                i++,
                minGapPct
            );

            // Buy/sell
            ps.setDouble(
                i++,
                minBuySellRatio
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockMomentum s =
                        new StockMomentum();

                    s.setSymbol(
                        rs.getString("symbol")
                    );

                    s.setPrevClose(
                        rs.getDouble("prev_close")
                    );

                    s.setClose0915(
                        rs.getDouble("close_0915")
                    );

                    s.setCurrentClose(
                        rs.getDouble("current_close")
                    );

                    s.setCurrentTime(
                        rs.getTimestamp("current_time")
                            .toLocalDateTime()
                    );

                    s.setGapPct(
                        rs.getDouble("gap_pct")
                    );

                    s.setMoveFrom0915Pct(
                        rs.getDouble("move_from_0915_pct")
                    );

                    s.setTotalMovePct(
                        rs.getDouble("total_move_pct")
                    );

                    s.setBuyQty0915(
                        rs.getLong("buy_qty_0915")
                    );

                    s.setSellQty0915(
                        rs.getLong("sell_qty_0915")
                    );

                    s.setBuySellRatio0915(
                        rs.getDouble("buy_sell_ratio_0915")
                    );

                    s.setCurrentBuyQty(
                        rs.getLong("current_buy_qty")
                    );

                    s.setCurrentSellQty(
                        rs.getLong("current_sell_qty")
                    );

                    s.setCurrentBuySellRatio(
                        rs.getDouble(
                            "current_buy_sell_ratio"
                        )
                    );

                    s.setBuyQtyChange(
                        rs.getLong("buy_qty_change")
                    );

                    s.setSellQtyChange(
                        rs.getLong("sell_qty_change")
                    );

                    s.setBuySellRatioChange(
                        rs.getDouble(
                            "buy_sell_ratio_change"
                        )
                    );

                    result.add(s);
                }
            }
            connection.commit();
        }

        return result;
    }

    public static List<StockMomentum> findHighBreakouts(
        Connection connection,
        LocalDateTime runTime) throws SQLException {

        String sql = """
        WITH opening_candle AS (
            SELECT DISTINCT ON (mc.symbol)
                mc.symbol,
                mc.time AS opening_time,
                mc.open_price AS open_0915,
                mc.high_price AS high_0915,
                mc.low_price AS low_0915,
                mc.close_price AS close_0915
            FROM minute_candle mc
            WHERE mc.time >= ?
              AND mc.time < ?
            ORDER BY mc.symbol, mc.time
        ),

        current_candle AS (
            SELECT DISTINCT ON (mc.symbol)
                mc.symbol,
                mc.time AS current_time,
                mc.open_price AS current_open,
                mc.high_price AS current_high,
                mc.low_price AS current_low,
                mc.close_price AS current_close,
                mc.volume AS current_volume,
                mc.total_buy_qty AS current_buy_qty,
                mc.total_sell_qty AS current_sell_qty,
                mc.buy_sell_ratio AS current_buy_sell_ratio
            FROM minute_candle mc
            WHERE mc.time >= ?
              AND mc.time <= ?
            ORDER BY mc.symbol, mc.time DESC
        )

        SELECT
            c.symbol,

            o.open_0915,
            o.high_0915,
            o.low_0915,
            o.close_0915,

            c.current_time,
            c.current_open,
            c.current_high,
            c.current_low,
            c.current_close,

            c.current_volume,
            c.current_buy_qty,
            c.current_sell_qty,
            c.current_buy_sell_ratio,

            ROUND(
                (
                    (c.current_close - o.high_0915)
                    / NULLIF(o.high_0915, 0) * 100
                )::numeric,
                2
            ) AS breakout_pct,

            ROUND(
                (
                    (c.current_close - o.close_0915)
                    / NULLIF(o.close_0915, 0) * 100
                )::numeric,
                2
            ) AS move_from_0915_pct

        FROM current_candle c

        JOIN opening_candle o
            ON o.symbol = c.symbol

        WHERE
            c.current_close > o.high_0915

        ORDER BY
            breakout_pct DESC
        """;

        LocalDate date = runTime.toLocalDate();

        LocalDateTime time0915 =
            date.atTime(9, 15);

        LocalDateTime time0916 =
            date.atTime(9, 16);

        List<StockMomentum> result =
            new ArrayList<>();

        try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

            int i = 1;

            // 09:15 opening candle
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0915)
            );

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0916)
            );

            // Current/latest candle
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(time0915)
            );

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(runTime)
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockMomentum s =
                        new StockMomentum();

                    s.setSymbol(
                        rs.getString("symbol")
                    );

                    // -------------------------
                    // 09:15 candle
                    // -------------------------

                    s.setOpen0915(
                        rs.getDouble("open_0915")
                    );

                    s.setHigh0915(
                        rs.getDouble("high_0915")
                    );

                    s.setLow0915(
                        rs.getDouble("low_0915")
                    );

                    s.setClose0915(
                        rs.getDouble("close_0915")
                    );

                    // -------------------------
                    // Current candle
                    // -------------------------

                    s.setCurrentTime(
                        rs.getTimestamp("current_time")
                            .toLocalDateTime()
                    );

                    s.setCurrentOpen(
                        rs.getDouble("current_open")
                    );

                    s.setCurrentHigh(
                        rs.getDouble("current_high")
                    );

                    s.setCurrentLow(
                        rs.getDouble("current_low")
                    );

                    s.setCurrentClose(
                        rs.getDouble("current_close")
                    );

                    // -------------------------
                    // Volume
                    // -------------------------

                    s.setCurrentVolume(
                        rs.getLong("current_volume")
                    );

                    // -------------------------
                    // Buy / Sell
                    // -------------------------

                    s.setCurrentBuyQty(
                        rs.getLong("current_buy_qty")
                    );

                    s.setCurrentSellQty(
                        rs.getLong("current_sell_qty")
                    );

                    s.setCurrentBuySellRatio(
                        rs.getDouble("current_buy_sell_ratio")
                    );

                    // -------------------------
                    // Breakout
                    // -------------------------

                    s.setBreakoutPct(
                        rs.getDouble("breakout_pct")
                    );

                    s.setMoveFrom0915Pct(
                        rs.getDouble("move_from_0915_pct")
                    );

                    result.add(s);
                }
            }
            connection.commit();
        }

        return result;
    }

    public static List<StockMomentum> findPriceBuyQuantitySurge(
        Connection connection,
        LocalDateTime currentTime,
        Set<String> symbols,
        double minBuyQtyIncreasePct) throws SQLException {

        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = String.join(
            ",",
            Collections.nCopies(symbols.size(), "?")
        );

        String sql = """
        WITH candles AS (
            SELECT
                mc.symbol,
                mc.time,
                mc.close_price,
                mc.total_buy_qty,

                LAG(mc.close_price) OVER (
                    PARTITION BY mc.symbol
                    ORDER BY mc.time
                ) AS prev_close,

                LAG(mc.total_buy_qty) OVER (
                    PARTITION BY mc.symbol
                    ORDER BY mc.time
                ) AS prev_buy_qty

            FROM minute_candle mc

            WHERE mc.symbol IN (%s)
              AND mc.time >= ?
              AND mc.time <= ?
        )

        SELECT
            symbol,
            time,

            prev_close,
            close_price,

            prev_buy_qty,
            total_buy_qty,

            close_price - prev_close
                AS price_change,

            total_buy_qty - prev_buy_qty
                AS buy_qty_change,

            ROUND(
                (
                    (total_buy_qty - prev_buy_qty)
                    / NULLIF(prev_buy_qty, 0) * 100
                )::numeric,
                2
            ) AS buy_qty_change_pct

        FROM candles

        WHERE time = ?

          /* Price must increase */

          AND close_price > prev_close

          /* Buy quantity must increase */

          AND total_buy_qty > prev_buy_qty

          /* Sudden buy quantity increase */

          AND (
                (total_buy_qty - prev_buy_qty)
                / NULLIF(prev_buy_qty, 0) * 100
              ) >= ?

        ORDER BY
            buy_qty_change_pct DESC
        """.formatted(placeholders);

        LocalDate date =
            currentTime.toLocalDate();

        LocalDateTime startTime =
            date.atTime(9, 15);

        List<StockMomentum> result =
            new ArrayList<>();

        try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

            int i = 1;

            // Symbols
            for (String symbol : symbols) {
                ps.setString(i++, symbol);
            }

            // Candle history
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(startTime)
            );

            ps.setTimestamp(
                i++,
                Timestamp.valueOf(currentTime)
            );

            // Candle being evaluated
            ps.setTimestamp(
                i++,
                Timestamp.valueOf(currentTime)
            );

            // Minimum buy quantity increase %
            ps.setDouble(
                i++,
                minBuyQtyIncreasePct
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockMomentum s =
                        new StockMomentum();

                    s.setSymbol(
                        rs.getString("symbol")
                    );

                    s.setPrevClose(
                        rs.getDouble("prev_close")
                    );

                    s.setCurrentClose(
                        rs.getDouble("close_price")
                    );

                    s.setCurrentBuyQty(
                        rs.getLong("total_buy_qty")
                    );

                    s.setBuyQtyChange(
                        rs.getLong("buy_qty_change")
                    );

                    s.setBuyQtyChangePct(
                        rs.getDouble(
                            "buy_qty_change_pct"
                        )
                    );

                    s.setCurrentTime(
                        rs.getTimestamp("time")
                            .toLocalDateTime()
                    );

                    result.add(s);
                }
            }
            connection.commit();
        }

        return result;
    }


}