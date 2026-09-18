package org.example.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Candle {
    public LocalDateTime time;
    public double open, high, low, close;
    public long volume;

    public double vwap;
    public double ema20;
    public double avgVolume;
    public String symbol;
    public double ema50;
    public double rsi14;
    public Long oi;
    public Integer trades;

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

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public double getEma20() {
        return ema20;
    }

    public void setEma20(double ema20) {
        this.ema20 = ema20;
    }

    public double getAvgVolume() {
        return avgVolume;
    }

    public void setAvgVolume(double avgVolume) {
        this.avgVolume = avgVolume;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getEma50() {
        return ema50;
    }

    public void setEma50(double ema50) {
        this.ema50 = ema50;
    }

    public double getRsi14() {
        return rsi14;
    }

    public void setRsi14(double rsi14) {
        this.rsi14 = rsi14;
    }

    public Long getOi() {
        return oi;
    }

    public void setOi(Long oi) {
        this.oi = oi;
    }

    public Integer getTrades() {
        return trades;
    }

    public void setTrades(Integer trades) {
        this.trades = trades;
    }

    public Candle(LocalDateTime time, String symbol, double open, double high, double low, double close, long volume, long oi, long trades) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.vwap = vwap;
        this.ema20 = ema20;
        this.avgVolume = avgVolume;
        this.symbol = symbol;
        this.ema50 = ema50;
        this.rsi14 = rsi14;
        this.oi = oi;
    }
}
