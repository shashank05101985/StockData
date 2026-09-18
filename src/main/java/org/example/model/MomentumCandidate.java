package org.example.model;

public class MomentumCandidate {

    public String symbol;

    public double priceChange5m;

    public double volumeRatio;

    public double lastBuySellRatio;

    public double avgBuySellRatio;

    public double closePosition;

    public double highBreakoutPct;

    public long currentVolume;

    public double score;

    public boolean strongCandidate;

    public double ltp;

    @Override
    public String toString() {

        return String.format(
            "%-12s SCORE=%6.2f | " +
                "5M=%6.2f%% | " +
                "VOL=%7.2fx | " +
                "B/S=%5.2f | " +
                "AVG B/S=%5.2f | " +
                "CLOSE=%6.2f%% | " +
                "BREAK=%6.2f%% | " +
                "LTP=%6.2f" ,

            symbol,
            score,
            priceChange5m,
            volumeRatio,
            lastBuySellRatio,
            avgBuySellRatio,
            closePosition,
            highBreakoutPct,
            ltp
        );
    }
}
