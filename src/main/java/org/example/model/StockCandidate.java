package org.example.model;

import java.time.LocalDateTime;

public class StockCandidate {

    public String symbol;
    public LocalDateTime time;

    public double price;
    public double firstPrice;

    public double periodHigh;
    public double periodLow;

    public double periodReturn;

    public long volume;
    public double avgVolume;
    public double volumeRatio;

    public double range;
    public double rangeRatio;

    public double priceVelocity;
    public double velocityRatio;

    public double buySellRatio;
    public double changePercent;

    public boolean breakoutHigh;
    public boolean breakoutLow;

    public int candleCount;
    public int score;

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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getFirstPrice() {
        return firstPrice;
    }

    public void setFirstPrice(double firstPrice) {
        this.firstPrice = firstPrice;
    }

    public double getPeriodHigh() {
        return periodHigh;
    }

    public void setPeriodHigh(double periodHigh) {
        this.periodHigh = periodHigh;
    }

    public double getPeriodLow() {
        return periodLow;
    }

    public void setPeriodLow(double periodLow) {
        this.periodLow = periodLow;
    }

    public double getPeriodReturn() {
        return periodReturn;
    }

    public void setPeriodReturn(double periodReturn) {
        this.periodReturn = periodReturn;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getAvgVolume() {
        return avgVolume;
    }

    public void setAvgVolume(double avgVolume) {
        this.avgVolume = avgVolume;
    }

    public double getVolumeRatio() {
        return volumeRatio;
    }

    public void setVolumeRatio(double volumeRatio) {
        this.volumeRatio = volumeRatio;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getRangeRatio() {
        return rangeRatio;
    }

    public void setRangeRatio(double rangeRatio) {
        this.rangeRatio = rangeRatio;
    }

    public double getPriceVelocity() {
        return priceVelocity;
    }

    public void setPriceVelocity(double priceVelocity) {
        this.priceVelocity = priceVelocity;
    }

    public double getVelocityRatio() {
        return velocityRatio;
    }

    public void setVelocityRatio(double velocityRatio) {
        this.velocityRatio = velocityRatio;
    }

    public double getBuySellRatio() {
        return buySellRatio;
    }

    public void setBuySellRatio(double buySellRatio) {
        this.buySellRatio = buySellRatio;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public boolean isBreakoutHigh() {
        return breakoutHigh;
    }

    public void setBreakoutHigh(boolean breakoutHigh) {
        this.breakoutHigh = breakoutHigh;
    }

    public boolean isBreakoutLow() {
        return breakoutLow;
    }

    public void setBreakoutLow(boolean breakoutLow) {
        this.breakoutLow = breakoutLow;
    }

    public int getCandleCount() {
        return candleCount;
    }

    public void setCandleCount(int candleCount) {
        this.candleCount = candleCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    // getters/setters
}
