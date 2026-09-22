package org.example;

import java.time.LocalDateTime;

public class Candle5Min {

    private LocalDateTime time;

    private double open;
    private double high;
    private double low;
    private double close;

    private double volume;

    private double ema9;
    private double vwap;

    private double buyQty;
    private double sellQty;
    private double buySellRatio;

    public Candle5Min() {
    }

    public Candle5Min(
            LocalDateTime time,
            double open,
            double high,
            double low,
            double close,
            double volume,
            double ema9,
            double vwap,
            double buyQty,
            double sellQty,
            double buySellRatio) {

        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.ema9 = ema9;
        this.vwap = vwap;
        this.buyQty = buyQty;
        this.sellQty = sellQty;
        this.buySellRatio = buySellRatio;
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

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getEma9() {
        return ema9;
    }

    public void setEma9(double ema9) {
        this.ema9 = ema9;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public double getBuyQty() {
        return buyQty;
    }

    public void setBuyQty(double buyQty) {
        this.buyQty = buyQty;
    }

    public double getSellQty() {
        return sellQty;
    }

    public void setSellQty(double sellQty) {
        this.sellQty = sellQty;
    }

    public double getBuySellRatio() {
        return buySellRatio;
    }

    public void setBuySellRatio(double buySellRatio) {
        this.buySellRatio = buySellRatio;
    }

    @Override
    public String toString() {
        return "Candle5Min{" +
                "time=" + time +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", ema9=" + ema9 +
                ", vwap=" + vwap +
                ", buyQty=" + buyQty +
                ", sellQty=" + sellQty +
                ", buySellRatio=" + buySellRatio +
                '}';
    }
}
