package org.example;

import org.example.analysis.PotentiakStockAnaluzer;
import org.example.loader.DB;
import org.example.repository.StockReturnDao;

import java.sql.SQLException;

public class StockScanner {

    static void main() throws SQLException {
        StockReturnDao.updateLatestStockReturns(DB.get());
    }
}
