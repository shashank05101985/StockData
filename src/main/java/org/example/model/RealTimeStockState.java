package org.example.model;

import java.time.LocalDateTime;

public class RealTimeStockState {

    private String symbol;

    private LocalDateTime lastTickTime;
    private double lastPrice;

    private double totalBuyQty;
    private double totalSellQty;

    private double buySellRatio;

    private long volume;

    private LocalDateTime currentMinute;

    // Pre-open / opening information
    private double preopenPrice;
    private long preopenBuyQty;
    private long preopenSellQty;
    private double preopenBuySellRatio;

    // 09:15 opening values
    private double open0915Price;
    private long open0915BuyQty;
    private long open0915SellQty;
    private double open0915BuySellRatio;
    private boolean momentumWatch;


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getLastTickTime() {
        return lastTickTime;
    }

    public void setLastTickTime(LocalDateTime lastTickTime) {
        this.lastTickTime = lastTickTime;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public double getTotalBuyQty() {
        return totalBuyQty;
    }

    public void setTotalBuyQty(double totalBuyQty) {
        this.totalBuyQty = totalBuyQty;
    }

    public double getTotalSellQty() {
        return totalSellQty;
    }

    public void setTotalSellQty(double totalSellQty) {
        this.totalSellQty = totalSellQty;
    }

    public double getBuySellRatio() {
        return buySellRatio;
    }

    public void setBuySellRatio(double buySellRatio) {
        this.buySellRatio = buySellRatio;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public LocalDateTime getCurrentMinute() {
        return currentMinute;
    }

    public void setCurrentMinute(LocalDateTime currentMinute) {
        this.currentMinute = currentMinute;
    }

    public double getPreopenPrice() {
        return preopenPrice;
    }

    public void setPreopenPrice(double preopenPrice) {
        this.preopenPrice = preopenPrice;
    }

    public long getPreopenBuyQty() {
        return preopenBuyQty;
    }

    public void setPreopenBuyQty(long preopenBuyQty) {
        this.preopenBuyQty = preopenBuyQty;
    }

    public long getPreopenSellQty() {
        return preopenSellQty;
    }

    public void setPreopenSellQty(long preopenSellQty) {
        this.preopenSellQty = preopenSellQty;
    }

    public double getPreopenBuySellRatio() {
        return preopenBuySellRatio;
    }

    public void setPreopenBuySellRatio(double preopenBuySellRatio) {
        this.preopenBuySellRatio = preopenBuySellRatio;
    }

    public double getOpen0915Price() {
        return open0915Price;
    }

    public void setOpen0915Price(double open0915Price) {
        this.open0915Price = open0915Price;
    }

    public long getOpen0915BuyQty() {
        return open0915BuyQty;
    }

    public void setOpen0915BuyQty(long open0915BuyQty) {
        this.open0915BuyQty = open0915BuyQty;
    }

    public long getOpen0915SellQty() {
        return open0915SellQty;
    }

    public void setOpen0915SellQty(long open0915SellQty) {
        this.open0915SellQty = open0915SellQty;
    }

    public double getOpen0915BuySellRatio() {
        return open0915BuySellRatio;
    }

    public void setOpen0915BuySellRatio(double open0915BuySellRatio) {
        this.open0915BuySellRatio = open0915BuySellRatio;
    }

    public boolean isMomentumWatch() {
        return momentumWatch;
    }

    public void setMomentumWatch(boolean momentumWatch) {
        this.momentumWatch = momentumWatch;
    }
    // getters/setters
}
