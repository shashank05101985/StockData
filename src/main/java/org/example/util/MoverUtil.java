package org.example.util;

import org.example.model.MarketMover;
import org.example.repository.MoverRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MoverUtil {
    public static List<MarketMover> getTopGainers(Connection conn, LocalDate tradingDate, LocalTime endTime, int limit,boolean slideWindow , int minute) throws SQLException {

        return MoverRepository.getTopMovers(conn, tradingDate, endTime, limit, true,slideWindow,minute);
    }

    public static List<MarketMover> getTopLosers(Connection conn, LocalDate tradingDate, LocalTime endTime, int limit,boolean slideWindow , int minute) throws SQLException {

        return MoverRepository.getTopMovers(conn, tradingDate, endTime, limit, false,slideWindow,minute);
    }
}
