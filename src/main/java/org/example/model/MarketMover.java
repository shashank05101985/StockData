package org.example.model;

public class MarketMover {

    private String symbol;
    private double openPrice;
    private double currentPrice;
    private double changePct;
    private double vwap;
    private long volume;
    private double priceVelocity;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getChangePct() {
        return changePct;
    }

    public void setChangePct(double changePct) {
        this.changePct = changePct;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getPriceVelocity() {
        return priceVelocity;
    }

    public void setPriceVelocity(double priceVelocity) {
        this.priceVelocity = priceVelocity;
    }

    @Override
    public String toString() {
        return String.format("%-15s Open=%8.2f Current=%8.2f Change=%7.2f%% " + "VWAP=%8.2f Volume=%d Velocity=%8.4f",
            symbol, openPrice, currentPrice, changePct, vwap, volume, priceVelocity);
    }
}
