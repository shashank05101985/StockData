package org.example.news;

import java.util.*;
import java.util.regex.Pattern;

public class StockSymbolDetector {

    private final Map<String, List<String>> aliases = new HashMap<>();

    public StockSymbolDetector() {

        add("HDFCBANK",
            "HDFC Bank",
            "HDFC Bank Ltd",
            "HDFC Bank Limited");

        add("RELIANCE",
            "Reliance Industries",
            "Reliance Industries Ltd",
            "Reliance");

        add("ICICIBANK",
            "ICICI Bank",
            "ICICI Bank Ltd",
            "ICICI Bank Limited");

        add("SBIN",
            "State Bank of India",
            "SBI",
            "State Bank");

        add("INFY",
            "Infosys",
            "Infosys Ltd",
            "Infosys Limited");

        add("TCS",
            "Tata Consultancy Services",
            "TCS Ltd",
            "TCS");

        add("BHARTIARTL",
            "Bharti Airtel",
            "Airtel",
            "Bharti Airtel Ltd");

        add("BHEL",
            "Bharat Heavy Electricals",
            "BHEL Ltd",
            "BHEL");

        add("LT",
            "Larsen & Toubro",
            "Larsen and Toubro",
            "L&T");

        add("ITC",
            "ITC Ltd",
            "ITC Limited",
            "ITC");

        add("AXISBANK",
            "Axis Bank",
            "Axis Bank Ltd",
            "Axis Bank Limited");

        add("KOTAKBANK",
            "Kotak Mahindra Bank",
            "Kotak Bank",
            "Kotak Mahindra Bank Ltd");
    }

    private void add(
        String symbol,
        String... companyNames) {

        aliases.put(
            symbol,
            Arrays.asList(companyNames)
        );
    }

    public List<String> detect(String text) {

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String normalized =
            text.toLowerCase(Locale.ROOT);

        Set<String> found =
            new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry
            : aliases.entrySet()) {

            String symbol = entry.getKey();

            for (String alias : entry.getValue()) {

                String normalizedAlias =
                    alias.toLowerCase(Locale.ROOT);

                /*
                 * Word-boundary matching.
                 *
                 * Prevents:
                 *
                 * SBI
                 *
                 * from matching inside:
                 *
                 * "possible"
                 */

                String regex =
                    "(?<![a-z0-9])"
                        + Pattern.quote(normalizedAlias)
                        + "(?![a-z0-9])";

                if (Pattern
                    .compile(regex)
                    .matcher(normalized)
                    .find()) {

                    found.add(symbol);
                    break;
                }
            }
        }

        return new ArrayList<>(found);
    }
}
