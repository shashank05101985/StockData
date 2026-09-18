package org.example.repository;

import org.example.model.MarketMover;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class MoverRepository {

    public static List<MarketMover> getTopMovers(Connection conn, LocalDate tradingDate, LocalTime endTime, int limit, boolean gainers , boolean slideWindow , int minute  ) throws SQLException {

        String order = gainers ? "DESC " : "ASC ";

        String sql = """
            WITH first_candle AS (
                SELECT DISTINCT ON (symbol)
                       symbol,
                       open_price AS open_915
                FROM minute_candle
                WHERE time >= ?
                  AND time <= ?
                ORDER BY symbol, time ASC
            ),
            
            last_candle AS (
                SELECT DISTINCT ON (symbol)
                       symbol,
                       close_price AS current_price,
                       day_average_price,
                       volume,
                       price_velocity
                FROM minute_candle
                WHERE time >= ?
                  AND time <= ?
                ORDER BY symbol, time DESC
            )
            
            SELECT
                f.symbol,
                f.open_915,
                l.current_price,
                l.day_average_price,
                l.volume,
                l.price_velocity,
            
                (
                    (l.current_price - f.open_915)
                    / NULLIF(f.open_915, 0)
                ) * 100 AS change_pct
            
            FROM first_candle f
            
            JOIN last_candle l
                ON f.symbol = l.symbol
            
            WHERE f.open_915 > 0
              AND l.current_price > 0
            
            ORDER BY change_pct
            """ + order + """
            LIMIT ?
            """;

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime start = tradingDate.atTime(9, 15)
                        .atZone(zone);

        if(slideWindow) {
             start = tradingDate.atTime(endTime).atZone(zone).minusMinutes(minute);
        }
        ZonedDateTime end = tradingDate.atTime(endTime).atZone(zone);

        Timestamp startTimestamp = Timestamp.from(start.toInstant());

        Timestamp endTimestamp = Timestamp.from(end.toInstant());

        List<MarketMover> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, startTimestamp);
            ps.setTimestamp(2, endTimestamp);

            ps.setTimestamp(3, startTimestamp);
            ps.setTimestamp(4, endTimestamp);

            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    // SAME OBJECT for gainers and losers
                    MarketMover mover = new MarketMover();

                    mover.setSymbol(rs.getString("symbol"));

                    mover.setOpenPrice(rs.getDouble("open_915"));

                    mover.setCurrentPrice(rs.getDouble("current_price"));

                    mover.setChangePct(rs.getDouble("change_pct"));

                    mover.setVwap(rs.getDouble("day_average_price"));

                    mover.setVolume(rs.getLong("volume"));

                    mover.setPriceVelocity(rs.getDouble("price_velocity"));

                    result.add(mover);
                }
            }
        }

        return result;
    }
}
