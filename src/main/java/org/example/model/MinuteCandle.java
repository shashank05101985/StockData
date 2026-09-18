package org.example.model;

import java.time.LocalDateTime;

public class MinuteCandle {

    public String symbol;
    public LocalDateTime time;

    public double open;
    public double high;
    public double low;
    public double close;

    public long startVolume;
    public long endVolume;

    public double averagePrice;
    public double lastQuantity;

    public double totalBuyQty;
    public double totalSellQty;

    public double oi;
    public double oiHigh;
    public double oiLow;

    public double change;

    public boolean initialized;

    public long volume;

    public int tickCount;

// ------------------------
// Derived Features
// ------------------------

    public double body;

    public double upperWick;

    public double lowerWick;

    public double range;

    public double buySellRatio;

    public double priceChange;

    public double priceVelocity;
    public double dayAveragePrice;
    public long startTickTime;
    public long lastTickTime;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getStartVolume() {
        return startVolume;
    }

    public void setStartVolume(long startVolume) {
        this.startVolume = startVolume;
    }

    public long getEndVolume() {
        return endVolume;
    }

    public void setEndVolume(long endVolume) {
        this.endVolume = endVolume;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public double getLastQuantity() {
        return lastQuantity;
    }

    public void setLastQuantity(double lastQuantity) {
        this.lastQuantity = lastQuantity;
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

    public double getOi() {
        return oi;
    }

    public void setOi(double oi) {
        this.oi = oi;
    }

    public double getOiHigh() {
        return oiHigh;
    }

    public void setOiHigh(double oiHigh) {
        this.oiHigh = oiHigh;
    }

    public double getOiLow() {
        return oiLow;
    }

    public void setOiLow(double oiLow) {
        this.oiLow = oiLow;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    public double getBody() {
        return body;
    }

    public void setBody(double body) {
        this.body = body;
    }

    public double getUpperWick() {
        return upperWick;
    }

    public void setUpperWick(double upperWick) {
        this.upperWick = upperWick;
    }

    public double getLowerWick() {
        return lowerWick;
    }

    public void setLowerWick(double lowerWick) {
        this.lowerWick = lowerWick;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getBuySellRatio() {
        return buySellRatio;
    }

    public void setBuySellRatio(double buySellRatio) {
        this.buySellRatio = buySellRatio;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public double getPriceVelocity() {
        return priceVelocity;
    }

    public void setPriceVelocity(double priceVelocity) {
        this.priceVelocity = priceVelocity;
    }

    public double getDayAveragePrice() {
        return dayAveragePrice;
    }

    public void setDayAveragePrice(double dayAveragePrice) {
        this.dayAveragePrice = dayAveragePrice;
    }

    public long getStartTickTime() {
        return startTickTime;
    }

    public void setStartTickTime(long startTickTime) {
        this.startTickTime = startTickTime;
    }

    public long getLastTickTime() {
        return lastTickTime;
    }

    public void setLastTickTime(long lastTickTime) {
        this.lastTickTime = lastTickTime;
    }
}
