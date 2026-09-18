package org.example.model;

import java.time.LocalDateTime;

public  class MinuteState {

    LocalDateTime minute;

    double openPrice;
    double highPrice;
    double lowPrice;
    double closePrice;

    double startBuyQty;
    double endBuyQty;

    long startVolume;
    long endVolume;

    long tickCount;

    public LocalDateTime getMinute() {
        return minute;
    }

    public void setMinute(LocalDateTime minute) {
        this.minute = minute;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public double getStartBuyQty() {
        return startBuyQty;
    }

    public void setStartBuyQty(double startBuyQty) {
        this.startBuyQty = startBuyQty;
    }

    public double getEndBuyQty() {
        return endBuyQty;
    }

    public void setEndBuyQty(double endBuyQty) {
        this.endBuyQty = endBuyQty;
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

    public long getTickCount() {
        return tickCount;
    }

    public void setTickCount(long tickCount) {
        this.tickCount = tickCount;
    }
}
