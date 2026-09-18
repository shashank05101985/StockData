package org.example.model;

public class FundamentalData {

    private final String symbol;
    private final double marketCap;
    private final double peRatio;
    private final double promoterHoldingChange;

    public FundamentalData(
        String symbol,
        double marketCap,
        double peRatio, double promoterHoldingChange) {

        this.symbol = symbol;
        this.marketCap = marketCap;
        this.peRatio = peRatio;
        this.promoterHoldingChange =  promoterHoldingChange;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getMarketCap() {
        return marketCap;
    }

    public double getPeRatio() {
        return peRatio;
    }
}
