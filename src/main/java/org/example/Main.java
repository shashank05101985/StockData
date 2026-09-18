package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.io.FeedException;
import org.example.analysis.StockAnalyzer;
import org.example.loader.DB;
import org.example.loader.DataLoader;
import org.example.model.StockData;
import org.example.model.StockSignal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    static void main() throws Exception {
        String url = "jdbc:postgresql://localhost:5432/stockdb";
        String user = "shashankmishra";
        String pass = "password";
        StockAnalyzer analyzer = new StockAnalyzer();
        LocalDate now = LocalDate.now().minusDays(4);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Run for date " + now);
            Map<String, List<StockData>> stockDataMap = DataLoader.loadLastNDays(conn, now, 450);
            System.out.println("Analyzing " + stockDataMap.size() + " stocks...\n");
            List<StockSignal> signals = analyzer.analyzeAllStocks(stockDataMap);
            // Print summary
            analyzer.printSummary(signals);
            // Interactive mode
            interactiveMode(signals, stockDataMap, analyzer);

        }
    }

    private static void interactiveMode(List<StockSignal> signals, Map<String, List<StockData>> stockDataMap, StockAnalyzer analyzer) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("INTERACTIVE MODE - Enter stock symbol to see detailed analysis");
        System.out.println("Commands: 'list' = show all symbols, 'top N' = show top N stocks, 'exit' = quit");
        System.out.println("=".repeat(70));

        while (true) {
            System.out.print("\nEnter symbol or command: ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("EXIT") || input.equals("QUIT") || input.equals("Q")) {
                System.out.println("Thank you for using Stock Analysis Tool!");
                break;
            }

            if (input.equals("LIST")) {
                System.out.println("\nAvailable symbols:");
                stockDataMap.keySet().stream().sorted().forEach(s -> System.out.print(s + " "));
                System.out.println();
                continue;
            }

            if (input.startsWith("TOP")) {
                try {
                    int n = Integer.parseInt(input.replace("TOP", "").trim());
                    System.out.println("\n--- Top " + n + " Stocks ---");
                    signals.stream().limit(n).forEach(System.out::println);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid format. Use: top 5");
                }
                continue;
            }

            // Look up specific stock
            StockSignal found = signals.stream().filter(s -> s.getSymbol().equals(input)).findFirst().orElse(null);

            if (found != null) {
                System.out.println(found);

                // Show additional data
                List<StockData> data = stockDataMap.get(input);
                if (data != null && !data.isEmpty()) {
                    System.out.println("\nRecent Price History:");
                    System.out.printf("%-12s %10s %10s %10s %10s %12s %10s\n", "Date", "Open", "High", "Low", "Close",
                        "Volume", "Del%");
                    System.out.println("-".repeat(76));

                    int start = Math.max(0, data.size() - 5);
                    for (int i = start; i < data.size(); i++) {
                        StockData d = data.get(i);
                        System.out.printf("%-12s %10.2f %10.2f %10.2f %10.2f %,12d %9.1f%%\n", d.getDate(), d.getOpen(),
                            d.getHigh(), d.getLow(), d.getClose(), d.getTradedQty(), d.getDeliveryPercent());
                    }
                }
            } else if (stockDataMap.containsKey(input)) {
                // Stock exists but might not have enough data or was filtered out
                List<StockData> data = stockDataMap.get(input);
                StockSignal signal = analyzer.analyzeStock(input, data);
                if (signal != null) {
                    System.out.println(signal);
                } else {
                    System.out.println("Insufficient data for analysis (need at least 20 days)");
                }
            } else {
                System.out.println("Symbol not found: " + input);
            }
        }

        scanner.close();
    }
}
