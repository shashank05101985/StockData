package org.example.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StockReturnDao {


    public static void updateLatestStockReturns(Connection conn) throws SQLException {

        String sql = """
            WITH latest AS (
                SELECT DISTINCT ON (symbol)
                    symbol,
                    date AS trade_date,
                    close_price
                FROM stock_data
                WHERE close_price > 0
                ORDER BY symbol, date DESC, updated_at DESC
            ),
            
            calculated AS (
                SELECT
                    c.symbol,
                    c.trade_date,
                    c.close_price,
            
                    /* =========================================
                       ROLLING CLOSE HIGHS
                       MAX(close_price)
                       ========================================= */
            
                    m1.close_high  AS close_1m,
                    m3.close_high  AS close_3m,
                    m6.close_high  AS close_6m,
                    m12.close_high AS close_12m,
            
                    /* =========================================
                       PREVIOUS DAY / WEEK
                       ========================================= */
            
                    d1.close_price  AS close_1d,
                    d1w.close_price AS close_1w,
            
                    /* =========================================
                       ROLLING INTRADAY HIGHS
                       MAX(high_price)
                       ========================================= */
            
                    m1.high_price  AS high_1m,
                    m2.high_price  AS high_2m,
                    m3.high_price  AS high_3m,
                    m6.high_price  AS high_6m,
                    m12.high_price AS high_12m,
            
                    /* =========================================
                       ALL TIME HIGH
                       MAX(high_price)
                       ========================================= */
            
                    ath.ath_price
            
                FROM latest c
            
            
                /* =========================================
                   PREVIOUS 1 DAY CLOSE
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date < c.trade_date
                      AND s.close_price > 0
                    ORDER BY s.date DESC, s.updated_at DESC
                    LIMIT 1
                ) d1 ON true
            
            
                /* =========================================
                   PREVIOUS 1 WEEK CLOSE
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date <= c.trade_date - INTERVAL '7 days'
                      AND s.close_price > 0
                    ORDER BY s.date DESC, s.updated_at DESC
                    LIMIT 1
                ) d1w ON true
            
            
                /* =========================================
                   PREVIOUS 1 MONTH
            
                   close_1m = highest close_price
                   high_1m  = highest high_price
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(close_price) AS close_high,
                        MAX(high_price)  AS high_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date >= c.trade_date - INTERVAL '1 month'
                      AND s.date < c.trade_date
                      AND s.close_price > 0
                      AND s.high_price > 0
                ) m1 ON true
            
            
                /* =========================================
                   PREVIOUS 2 MONTHS
            
                   high_2m = highest high_price
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(high_price) AS high_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date >= c.trade_date - INTERVAL '2 months'
                      AND s.date < c.trade_date
                      AND s.high_price > 0
                ) m2 ON true
            
            
                /* =========================================
                   PREVIOUS 3 MONTHS
            
                   close_3m = highest close_price
                   high_3m  = highest high_price
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(close_price) AS close_high,
                        MAX(high_price)  AS high_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date >= c.trade_date - INTERVAL '3 months'
                      AND s.date < c.trade_date
                      AND s.close_price > 0
                      AND s.high_price > 0
                ) m3 ON true
            
            
                /* =========================================
                   PREVIOUS 6 MONTHS
            
                   close_6m = highest close_price
                   high_6m  = highest high_price
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(close_price) AS close_high,
                        MAX(high_price)  AS high_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date >= c.trade_date - INTERVAL '6 months'
                      AND s.date < c.trade_date
                      AND s.close_price > 0
                      AND s.high_price > 0
                ) m6 ON true
            
            
                /* =========================================
                   PREVIOUS 12 MONTHS
            
                   close_12m = highest close_price
                   high_12m  = highest high_price
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(close_price) AS close_high,
                        MAX(high_price)  AS high_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.date >= c.trade_date - INTERVAL '1 year'
                      AND s.date < c.trade_date
                      AND s.close_price > 0
                      AND s.high_price > 0
                ) m12 ON true
            
            
                /* =========================================
                   ALL TIME HIGH
                   ========================================= */
            
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(high_price) AS ath_price
                    FROM stock_data s
                    WHERE s.symbol = c.symbol
                      AND s.high_price > 0
                ) ath ON true
            )
            
            
            /* =============================================
               INSERT / UPSERT
               ============================================= */
            
            INSERT INTO stock_returns (
                symbol,
                trade_date,
                close_price,
            
                close_1d,
                close_1w,
                close_1m,
                close_3m,
                close_6m,
                close_12m,
            
                high_1m,
                high_2m,
                high_3m,
                high_6m,
                high_12m,
            
                ath,
            
                return_1d,
                return_1w,
                return_1m,
                return_3m,
                return_6m,
                return_12m
            )
            
            SELECT
                symbol,
                trade_date,
                close_price,
            
                close_1d,
                close_1w,
            
                close_1m,
                close_3m,
                close_6m,
                close_12m,
            
                high_1m,
                high_2m,
                high_3m,
                high_6m,
                high_12m,
            
                ath_price,
            
                /* =========================================
                   RETURNS
            
                   Since close_* now represents the
                   period HIGH CLOSE, these returns mean:
            
                   Current price vs period highest close
                   ========================================= */
            
                CASE
                    WHEN close_1d > 0
                    THEN (close_price - close_1d)
                         / close_1d * 100
                END,
            
                CASE
                    WHEN close_1w > 0
                    THEN (close_price - close_1w)
                         / close_1w * 100
                END,
            
                CASE
                    WHEN close_1m > 0
                    THEN (close_price - close_1m)
                         / close_1m * 100
                END,
            
                CASE
                    WHEN close_3m > 0
                    THEN (close_price - close_3m)
                         / close_3m * 100
                END,
            
                CASE
                    WHEN close_6m > 0
                    THEN (close_price - close_6m)
                         / close_6m * 100
                END,
            
                CASE
                    WHEN close_12m > 0
                    THEN (close_price - close_12m)
                         / close_12m * 100
                END
            
            FROM calculated
            
            ON CONFLICT (symbol, trade_date)
            DO UPDATE SET
            
                close_price = EXCLUDED.close_price,
            
                close_1d = EXCLUDED.close_1d,
                close_1w = EXCLUDED.close_1w,
                close_1m = EXCLUDED.close_1m,
                close_3m = EXCLUDED.close_3m,
                close_6m = EXCLUDED.close_6m,
                close_12m = EXCLUDED.close_12m,
            
                high_1m = EXCLUDED.high_1m,
                high_2m = EXCLUDED.high_2m,
                high_3m = EXCLUDED.high_3m,
                high_6m = EXCLUDED.high_6m,
                high_12m = EXCLUDED.high_12m,
            
                ath = EXCLUDED.ath,
            
                return_1d = EXCLUDED.return_1d,
                return_1w = EXCLUDED.return_1w,
                return_1m = EXCLUDED.return_1m,
                return_3m = EXCLUDED.return_3m,
                return_6m = EXCLUDED.return_6m,
                return_12m = EXCLUDED.return_12m,
            
                updated_at = CURRENT_TIMESTAMP
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            int count = ps.executeUpdate();

            System.out.println("Latest stock returns updated: " + count);

            conn.commit();
        }
    }


}

  /*SELECT
        symbol,
        close_price,
        close_12m,
        high_12m,
        ath,

    ROUND(
        ((close_price - close_12m) / close_12m * 100)::numeric,
        2
        ) AS breakout_from_12m_close,

    ROUND(
        ((close_price - high_12m) / high_12m * 100)::numeric,
        2
        ) AS breakout_from_12m_high,

    ROUND(
        ((close_price - ath) / ath * 100)::numeric,
        2
        ) AS distance_from_ath

    FROM stock_returns
    WHERE trade_date = '2026-08-25'
    AND close_price > close_12m
    AND close_price > high_12m ORDER BY breakout_from_12m_close DESC;*/
