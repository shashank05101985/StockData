package org.example.model;

public class PreviousDayData {
    double closePrice;
    long tradedQty;
    double highPrice;
    double deliveryPct;
    double lowPrice;


    public PreviousDayData(double closePrice, long tradedQty,double highPrice,double deliveryPct,double lowPrice) {
        this.closePrice = closePrice;
        this.tradedQty = tradedQty;
        this.highPrice = highPrice;
        this.deliveryPct = deliveryPct;
        this.lowPrice = lowPrice;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public long getTradedQty() {
        return tradedQty;
    }

    public void setTradedQty(long tradedQty) {
        this.tradedQty = tradedQty;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    public double getDeliveryPct() {
        return deliveryPct;
    }

    public void setDeliveryPct(double deliveryPct) {
        this.deliveryPct = deliveryPct;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }
}
