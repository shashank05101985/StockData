package org.example.model;

import java.time.LocalDateTime;

public class StockMomentum {

    private String symbol;

    private double prevClose;
    private double close0915;
    private double currentClose;

    private double gapPct;
    private double moveFrom0915Pct;
    private double totalMovePct;

    private LocalDateTime currentTime;
    private long volume0915;
    private long currentVolume;
    private long volumeDifference;

    private long buyQty0915;
    private long sellQty0915;

    private long currentBuyQty;
    private long currentSellQty;

    private double buySellRatio0915;
    private double currentBuySellRatio;
    private double buySellRatioChange;

    private long buyQtyChange;
    private long sellQtyChange;
    private double prevHigh;
    private double todayOpen;

    private double currentHigh;

    private double breakoutPct;
    private double moveFromOpenPct;

    private long volume;
    private double open0915;
    private double high0915;
    private double low0915;

    private double currentOpen;
    private double currentLow;
    private double buyQtyChangePct;

    private double preopenPrice;
    private double preopenGapPct;

    private long preopenBuyQty;
    private long preopenSellQty;

    private double preopenBuySellRatio;




    // getters and setters


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(double prevClose) {
        this.prevClose = prevClose;
    }

    public double getClose0915() {
        return close0915;
    }

    public void setClose0915(double close0915) {
        this.close0915 = close0915;
    }

    public double getCurrentClose() {
        return currentClose;
    }

    public void setCurrentClose(double currentClose) {
        this.currentClose = currentClose;
    }

    public double getGapPct() {
        return gapPct;
    }

    public void setGapPct(double gapPct) {
        this.gapPct = gapPct;
    }

    public double getMoveFrom0915Pct() {
        return moveFrom0915Pct;
    }

    public void setMoveFrom0915Pct(double moveFrom0915Pct) {
        this.moveFrom0915Pct = moveFrom0915Pct;
    }

    public double getTotalMovePct() {
        return totalMovePct;
    }

    public void setTotalMovePct(double totalMovePct) {
        this.totalMovePct = totalMovePct;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(LocalDateTime currentTime) {
        this.currentTime = currentTime;
    }

    public long getVolume0915() {
        return volume0915;
    }

    public void setVolume0915(long volume0915) {
        this.volume0915 = volume0915;
    }

    public long getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(long currentVolume) {
        this.currentVolume = currentVolume;
    }

    public long getVolumeDifference() {
        return volumeDifference;
    }

    public void setVolumeDifference(long volumeDifference) {
        this.volumeDifference = volumeDifference;
    }

    public long getBuyQty0915() {
        return buyQty0915;
    }

    public void setBuyQty0915(long buyQty0915) {
        this.buyQty0915 = buyQty0915;
    }

    public long getSellQty0915() {
        return sellQty0915;
    }

    public void setSellQty0915(long sellQty0915) {
        this.sellQty0915 = sellQty0915;
    }

    public long getCurrentBuyQty() {
        return currentBuyQty;
    }

    public void setCurrentBuyQty(long currentBuyQty) {
        this.currentBuyQty = currentBuyQty;
    }

    public long getCurrentSellQty() {
        return currentSellQty;
    }

    public void setCurrentSellQty(long currentSellQty) {
        this.currentSellQty = currentSellQty;
    }

    public double getBuySellRatio0915() {
        return buySellRatio0915;
    }

    public void setBuySellRatio0915(double buySellRatio0915) {
        this.buySellRatio0915 = buySellRatio0915;
    }

    public double getCurrentBuySellRatio() {
        return currentBuySellRatio;
    }

    public void setCurrentBuySellRatio(double currentBuySellRatio) {
        this.currentBuySellRatio = currentBuySellRatio;
    }

    public double getBuySellRatioChange() {
        return buySellRatioChange;
    }

    public void setBuySellRatioChange(double buySellRatioChange) {
        this.buySellRatioChange = buySellRatioChange;
    }

    public long getBuyQtyChange() {
        return buyQtyChange;
    }

    public void setBuyQtyChange(long buyQtyChange) {
        this.buyQtyChange = buyQtyChange;
    }

    public long getSellQtyChange() {
        return sellQtyChange;
    }

    public void setSellQtyChange(long sellQtyChange) {
        this.sellQtyChange = sellQtyChange;
    }

    public double getPrevHigh() {
        return prevHigh;
    }

    public void setPrevHigh(double prevHigh) {
        this.prevHigh = prevHigh;
    }

    public double getTodayOpen() {
        return todayOpen;
    }

    public void setTodayOpen(double todayOpen) {
        this.todayOpen = todayOpen;
    }

    public double getCurrentHigh() {
        return currentHigh;
    }

    public void setCurrentHigh(double currentHigh) {
        this.currentHigh = currentHigh;
    }

    public double getBreakoutPct() {
        return breakoutPct;
    }

    public void setBreakoutPct(double breakoutPct) {
        this.breakoutPct = breakoutPct;
    }

    public double getMoveFromOpenPct() {
        return moveFromOpenPct;
    }

    public void setMoveFromOpenPct(double moveFromOpenPct) {
        this.moveFromOpenPct = moveFromOpenPct;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getOpen0915() {
        return open0915;
    }

    public void setOpen0915(double open0915) {
        this.open0915 = open0915;
    }

    public double getHigh0915() {
        return high0915;
    }

    public void setHigh0915(double high0915) {
        this.high0915 = high0915;
    }

    public double getLow0915() {
        return low0915;
    }

    public void setLow0915(double low0915) {
        this.low0915 = low0915;
    }

    public double getCurrentOpen() {
        return currentOpen;
    }

    public void setCurrentOpen(double currentOpen) {
        this.currentOpen = currentOpen;
    }

    public double getCurrentLow() {
        return currentLow;
    }

    public void setCurrentLow(double currentLow) {
        this.currentLow = currentLow;
    }

    public void setBuyQtyChangePct(double buyQtyChangePct) {
        this.buyQtyChangePct = buyQtyChangePct;
    }

    public double getBuyQtyChangePct() {
        return buyQtyChangePct;
    }

    public double getPreopenPrice() {
        return preopenPrice;
    }

    public void setPreopenPrice(double preopenPrice) {
        this.preopenPrice = preopenPrice;
    }

    public double getPreopenGapPct() {
        return preopenGapPct;
    }

    public void setPreopenGapPct(double preopenGapPct) {
        this.preopenGapPct = preopenGapPct;
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
}
