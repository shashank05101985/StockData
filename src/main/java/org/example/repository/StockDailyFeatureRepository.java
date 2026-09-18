package org.example.repository;

import org.example.loader.DB;
import org.example.model.FundamentalData;
import org.example.model.PreviousDayData;
import org.example.model.StockDailyFeature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StockDailyFeatureRepository {

    private static Map<String, PreviousDayData> previousDayMap = new ConcurrentHashMap<>();

    public static Map<String, StockDailyFeature> loadCurrentDailyFeatures(Connection connection, Set<String> symbols) throws SQLException {

        Map<String, StockDailyFeature> result = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        String placeholders = String.join(",", Collections.nCopies(symbols.size(), "?"));

        String sql = """
            SELECT DISTINCT ON (symbol)
                   symbol,
                   trade_date,
                   close_price,
                   sma20,
                   sma50,
                   sma100,
                   sma200,
                   ema9,
                   ema20,
                   ema50,
                   ema100,
                   ema200,
                   close_above_ema20,
                   ema20_above_ema50,
                   ema50_above_ema200,
                   rsi14,
                   atr14,
                   atr_percent,
                   avg_volume5,
                   avg_volume20,
                   volume_ratio5,
                   volume_ratio20,
                   high_5d,
                   high_10d,
                   high_20d,
                   distance_from_5d_high,
                   distance_from_20d_high,
                   breakout_5d,
                   breakout_10d,
                   breakout_20d,
                   consolidation_width,
                   consolidation_score,
                   breakout_strength,
                   trend_score,
                   momentum_score,
                   volume_score,
                   bullish_score,
                   market_bias,
                    resistance_price,
                    support_price,
                    resistance_strength,
                    support_strength,
                    distance_to_resistance,
                    distance_to_support
            FROM stock_daily_features
            WHERE symbol IN (""" + placeholders + """
            )
                AND trade_date = (
                          SELECT MAX(trade_date)
                          FROM stock_daily_features
                          WHERE trade_date < CURRENT_DATE
                      )
            ORDER BY symbol
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            int index = 1;

            for (String symbol : symbols) {
                ps.setString(index++, symbol);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockDailyFeature data = new StockDailyFeature();

                    data.setSymbol(rs.getString("symbol"));
                    data.setTradeDate(rs.getDate("trade_date").toLocalDate());

                    data.setClose(rs.getDouble("close_price"));

                    data.setSma20(rs.getDouble("sma20"));
                    data.setSma50(rs.getDouble("sma50"));
                    data.setSma100(rs.getDouble("sma100"));
                    data.setSma200(rs.getDouble("sma200"));

                    data.setEma9(rs.getDouble("ema9"));
                    data.setEma20(rs.getDouble("ema20"));
                    data.setEma50(rs.getDouble("ema50"));
                    data.setEma100(rs.getDouble("ema100"));
                    data.setEma200(rs.getDouble("ema200"));

                    data.setCloseAboveEma20(rs.getBoolean("close_above_ema20"));

                    data.setEma20AboveEma50(rs.getBoolean("ema20_above_ema50"));

                    data.setEma50AboveEma200(rs.getBoolean("ema50_above_ema200"));

                    data.setRsi14(rs.getDouble("rsi14"));

                    data.setAtr14(rs.getDouble("atr14"));
                    data.setAtrPercent(rs.getDouble("atr_percent"));

                    data.setAvgVolume5(rs.getDouble("avg_volume5"));
                    data.setAvgVolume20(rs.getDouble("avg_volume20"));

                    data.setVolumeRatio5(rs.getDouble("volume_ratio5"));

                    data.setVolumeRatio20(rs.getDouble("volume_ratio20"));

                    data.setHigh5d(rs.getDouble("high_5d"));
                    data.setHigh10d(rs.getDouble("high_10d"));
                    data.setHigh20d(rs.getDouble("high_20d"));

                    data.setDistanceFrom5dHigh(rs.getDouble("distance_from_5d_high"));

                    data.setDistanceFrom20dHigh(rs.getDouble("distance_from_20d_high"));

                    data.setBreakout5d(rs.getBoolean("breakout_5d"));

                    data.setBreakout10d(rs.getBoolean("breakout_10d"));

                    data.setBreakout20d(rs.getBoolean("breakout_20d"));

                    data.setConsolidationWidth(rs.getDouble("consolidation_width"));

                    data.setConsolidationScore(rs.getDouble("consolidation_score"));

                    data.setBreakoutStrength(rs.getDouble("breakout_strength"));

                    data.setTrendScore(rs.getDouble("trend_score"));

                    data.setMomentumScore(rs.getDouble("momentum_score"));

                    data.setVolumeScore(rs.getDouble("volume_score"));

                    data.setBullishScore(rs.getDouble("bullish_score"));

                    data.setMarketBias(rs.getString("market_bias"));
                    data.setResistancePrice(rs.getDouble("resistance_price"));
                    data.setSupportPrice(rs.getDouble("support_price"));
                    data.setResistanceStrength(rs.getDouble("resistance_strength"));
                    data.setSupportStrength(rs.getDouble("support_strength"));
                    data.setDistanceToSupport(rs.getDouble("distance_to_support"));
                    data.setDistanceToResistance(rs.getDouble("distance_to_resistance"));


                    result.put(data.getSymbol(), data);
                }
            }
            connection.commit();
        }

        return result;
    }

    public static Map<String, FundamentalData> loadFundamentals(Connection conn) throws SQLException {

        Map<String, FundamentalData> result = new HashMap<>();

        String sql = """
            SELECT
                symbol,
                market_cap,
                pe_ratio,
                promoter_holding_change_qoq
            FROM stock_fundamentals
            WHERE market_cap > 0
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String symbol = rs.getString("symbol");

                double marketCap = rs.getDouble("market_cap");

                double peRatio = rs.getDouble("pe_ratio");

                double promoterHoldingChange = rs.getDouble("promoter_holding_change_qoq");

                if (rs.wasNull()) {
                    peRatio = 0.0;
                }

                FundamentalData data = new FundamentalData(symbol, marketCap, peRatio, promoterHoldingChange);

                result.put(symbol, data);
            }
        }

        System.out.println("Loaded fundamental data: " + result.size());

        return result;
    }

    public static Map<String, PreviousDayData> loadPreviousDayClose(LocalDate date, Connection connection) throws SQLException {
        if (connection == null) {
            connection = DB.get();
        }

        LocalDate targetDate = date.minusDays(1);

        String sql = """
            SELECT DISTINCT ON (symbol)
                   symbol,
                   close_price,
                   high_price,
                   traded_qty,
                   delivery_percent,
                   low_price
            FROM stock_data
            WHERE date = ?
            ORDER BY symbol, date DESC
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(targetDate));

            try (ResultSet rs = ps.executeQuery()) {

                previousDayMap.clear();

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    double closePrice = rs.getDouble("close_price");

                    double highPrice = rs.getDouble("high_price");

                    long tradedQty = rs.getLong("traded_qty");

                    double deliveryPct = rs.getDouble("delivery_percent");

                    double lowPrice = rs.getDouble("low_price");

                    previousDayMap.put(symbol,
                        new PreviousDayData(closePrice, tradedQty, highPrice, deliveryPct, lowPrice

                        ));
                }

                System.out.println(
                    "Loaded previous day data for: " + targetDate + " | stocks=" + previousDayMap.size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return previousDayMap;
    }

    public static Map<String, PreviousDayData> loadPreviousDayClose(LocalDate date, Connection connection, int days) throws SQLException {

        if (connection == null) {
            connection = DB.get();
        }

        if (days <= 0) {
            throw new IllegalArgumentException("days must be > 0");
        }

        /*
         * We use a larger calendar window because we want
         * approximately N trading days, not N calendar days.
         *
         * 3 * days is generally enough for weekends/holidays.
         */
        LocalDate fromDate = date.minusDays(days * 3L);

        String sql = """
            WITH ranked AS (
                SELECT
                    symbol,
                    date,
                    high_price,
                    low_price,
                    close_price,
                    traded_qty,
                    delivery_percent,
            
                    ROW_NUMBER() OVER (
                        PARTITION BY symbol
                        ORDER BY date DESC
                    ) AS rn
            
                FROM stock_data
            
                WHERE date < ?
                  AND date >= ?
                  AND series = 'EQ'
            )
            
            SELECT
                symbol,
            
                MAX(high_price) AS max_high,
                MIN(low_price) AS min_low,
            
                AVG(traded_qty) AS avg_traded_qty,
            
                AVG(delivery_percent) AS avg_delivery_percent,
            
                MAX(close_price) AS max_close,
                MIN(close_price) AS min_close,
            
                COUNT(*) AS trading_days
            
            FROM ranked
            
            WHERE rn <= ?
            
            GROUP BY symbol
            
            HAVING COUNT(*) = ?
            
            ORDER BY symbol
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));

            ps.setDate(2, java.sql.Date.valueOf(fromDate));

            /*
             * Number of trading days we actually want.
             */
            ps.setInt(3, days);
            ps.setInt(4, days);

            try (ResultSet rs = ps.executeQuery()) {

                previousDayMap.clear();

                while (rs.next()) {

                    String symbol = rs.getString("symbol");

                    double maxHigh = rs.getDouble("max_high");

                    double minLow = rs.getDouble("min_low");

                    long avgTradedQty = rs.getLong("avg_traded_qty");

                    double avgDeliveryPct = rs.getDouble("avg_delivery_percent");

                    double maxClose = rs.getDouble("max_close");

                    double minClose = rs.getDouble("min_close");

                    int tradingDays = rs.getInt("trading_days");

                    previousDayMap.put(symbol,
                        new PreviousDayData(maxClose, avgTradedQty, maxHigh, avgDeliveryPct, minLow));

                    if (symbol.equals("ITDC") || symbol.equals("TCS")) {

                        System.out.println(
                            "Historical " + days + "D | " + symbol + " | High=" + maxHigh + " | Low=" + minLow + " | AvgVolume=" + avgTradedQty + " | AvgDelivery=" + avgDeliveryPct + " | MaxClose=" + maxClose + " | MinClose=" + minClose + " | Days=" + tradingDays);
                    }
                }

                System.out.println(
                    "Loaded " + days + "-day historical data before " + date + " | stocks=" + previousDayMap.size());
            }

        } catch (SQLException e) {

            System.err.println("Failed to load historical stock data before " + date);

            throw e;
        }

        return previousDayMap;
    }

    public static PreviousDayData getPreviosData(String symbol) {
        return previousDayMap.get(symbol);
    }
}
