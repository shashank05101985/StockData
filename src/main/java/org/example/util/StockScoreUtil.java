package org.example.util;

import org.example.model.MinuteCandle;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class StockScoreUtil {

    public static class StockScore {

        public String symbol;

        public double score;

        public double momentumScore;
        public double breakoutScore;
        public double volumeScore;
        public double demandScore;
        public double candleScore;

        public double return5;
        public double return10;
        public double volumeRatio;
        public double buySellRatio;

        // ============================================================
        // TRADE LEVELS
        // ============================================================

        public double entry;

        public double stopLoss;

        public double target1;

        public double target2;

        public double risk;

        public double reward1;

        public double reward2;

        public double riskReward1;

        public double riskReward2;

        public double stopDistancePercent;

        public boolean tradeable;

        public String signal;


        @Override
        public String toString() {

            return String.format("%s SCORE=%.1f | " + "MOM=%.1f BREAK=%.1f VOL=%.1f " + "DEMAND=%.1f CANDLE=%.1f | " +

                    "RET5=%.2f%% RET10=%.2f%% " + "VOL=%.2fx BUY/SELL=%.2f | " +

                    "ENTRY=%.2f SL=%.2f " + "T1=%.2f T2=%.2f " + "RISK=%.2f RR1=%.2f RR2=%.2f | " +

                    "%s",

                symbol, score,

                momentumScore, breakoutScore, volumeScore, demandScore, candleScore,

                return5, return10, volumeRatio, buySellRatio,

                entry, stopLoss, target1, target2,

                risk, riskReward1, riskReward2,

                signal);
        }
    }


    public static StockScore scoreStock(String symbol, Deque<MinuteCandle> candles) {

        StockScore result = new StockScore();

        result.symbol = symbol;

        /*
         * Need at least 11 candles because we access:
         *
         * c.size() - 11
         */
        if (candles == null || candles.size() < 10) {

            result.signal = "INSUFFICIENT_DATA";

            result.tradeable = false;

            return result;
        }


        List<MinuteCandle> c = new ArrayList<>(candles);


        MinuteCandle latest = c.get(c.size() - 1);


        /*
         * ============================================================
         * 1. MOMENTUM
         * ============================================================
         */

        MinuteCandle candle5 = c.get(c.size() - 5);

        MinuteCandle candle10 = c.get(c.size() - 10);


        double return5 = ((latest.getClose() - candle5.getClose()) / candle5.getClose()) * 100.0;


        double return10 = ((latest.getClose() - candle10.getClose()) / candle10.getClose()) * 100.0;


        result.return5 = return5;

        result.return10 = return10;


        double momentumScore = 0;


        if (return5 > 0.30)
            momentumScore += 10;


        if (return5 > 0.50)
            momentumScore += 5;


        if (return10 > 0.50)
            momentumScore += 5;


        if (return10 > 1.00)
            momentumScore += 5;


        result.momentumScore = Math.min(25, momentumScore);


        /*
         * ============================================================
         * 2. BREAKOUT
         * ============================================================
         */

        double previousHigh = Double.MIN_VALUE;

        double previousLow = Double.MAX_VALUE;


        for (int i = Math.max(0, c.size() - 11); i < c.size() - 1; i++) {

            previousHigh = Math.max(previousHigh, c.get(i).getHigh());

            previousLow = Math.min(previousLow, c.get(i).getLow());
        }


        double breakoutScore = 0;


        if (latest.getClose() > previousHigh)
            breakoutScore += 10;


        if (latest.getHigh() > previousHigh)
            breakoutScore += 5;


        if (latest.getClose() > candle5.getClose())
            breakoutScore += 5;


        result.breakoutScore = Math.min(20, breakoutScore);


        /*
         * ============================================================
         * 3. VOLUME
         * ============================================================
         */

        double oldVolume = 0;

        int oldCount = 0;


        for (int i = Math.max(0, c.size() - 11); i < c.size() - 5; i++) {

            oldVolume += c.get(i).getVolume();

            oldCount++;
        }


        double recentVolume = 0;

        int recentCount = 0;


        for (int i = Math.max(0, c.size() - 5); i < c.size(); i++) {

            recentVolume += c.get(i).getVolume();

            recentCount++;
        }


        double oldAvg = oldCount == 0 ? 0 : oldVolume / oldCount;


        double recentAvg = recentCount == 0 ? 0 : recentVolume / recentCount;


        double volumeRatio = oldAvg <= 0 ? 0 : recentAvg / oldAvg;


        result.volumeRatio = volumeRatio;


        double volumeScore = 0;


        if (volumeRatio >= 1.2)
            volumeScore += 5;


        if (volumeRatio >= 1.5)
            volumeScore += 5;


        if (volumeRatio >= 2.0)
            volumeScore += 5;


        if (volumeRatio >= 3.0)
            volumeScore += 5;


        result.volumeScore = Math.min(20, volumeScore);


        /*
         * ============================================================
         * 4. DEMAND
         * ============================================================
         */

        double buy = latest.getTotalBuyQty();


        double sell = latest.getTotalSellQty();


        double buySellRatio = sell <= 0 ? 0 : buy / sell;


        result.buySellRatio = buySellRatio;


        double demandScore = 0;


        if (buySellRatio >= 1.2)
            demandScore += 5;


        if (buySellRatio >= 1.5)
            demandScore += 5;


        if (buySellRatio >= 2.0)
            demandScore += 5;


        /*
         * Demand + actual price movement.
         */
        if (buySellRatio >= 1.5 && return5 > 0.30) {

            demandScore += 5;
        }


        result.demandScore = Math.min(20, demandScore);


        /*
         * ============================================================
         * 5. CANDLE QUALITY
         * ============================================================
         */

        double candleScore = 0;

        int bullishCandles = 0;

        int strongCandles = 0;


        for (int i = Math.max(0, c.size() - 5); i < c.size(); i++) {

            MinuteCandle x = c.get(i);


            if (x.getClose() > x.getOpen())
                bullishCandles++;


            double range = x.getHigh() - x.getLow();


            if (range <= 0)
                continue;


            double body = Math.abs(x.getClose() - x.getOpen());


            double bodyRatio = body / range;


            if (x.getClose() > x.getOpen() && bodyRatio >= 0.60) {

                strongCandles++;
            }
        }


        if (bullishCandles >= 3)
            candleScore += 5;


        if (bullishCandles >= 4)
            candleScore += 5;


        if (strongCandles >= 2)
            candleScore += 5;


        result.candleScore = Math.min(15, candleScore);


        /*
         * ============================================================
         * FINAL SCORE
         * ============================================================
         */

        result.score = result.momentumScore + result.breakoutScore + result.volumeScore + result.demandScore + result.candleScore;


        /*
         * ============================================================
         * TRADE LEVELS
         * ============================================================
         */

        calculateTradeLevels(result, c);


        /*
         * ============================================================
         * SIGNAL
         * ============================================================
         */

        if (result.score >= 80) {

            result.signal = result.tradeable ? "STRONG_BULLISH" : "STRONG_BULLISH_NO_TRADE";

        } else if (result.score >= 65) {

            result.signal = result.tradeable ? "BULLISH" : "BULLISH_NO_TRADE";

        } else if (result.score >= 50) {

            result.signal = "WATCH";

        } else {

            result.signal = "IGNORE";
        }


        return result;
    }


    /*
     * ================================================================
     * ENTRY / STOP LOSS / TARGET
     * ================================================================
     */
    private static void calculateTradeLevels(StockScore result, List<MinuteCandle> candles) {

        MinuteCandle latest = candles.get(candles.size() - 1);


        /*
         * ------------------------------------------------------------
         * ENTRY
         *
         * For backtest:
         * use latest candle close.
         * ------------------------------------------------------------
         */

        double entry = latest.getClose();


        /*
         * ------------------------------------------------------------
         * SWING LOW
         *
         * Last 5 candles.
         *
         * We use the lowest low as structural SL.
         * ------------------------------------------------------------
         */

        double swingLow = Double.MAX_VALUE;


        for (int i = Math.max(0, candles.size() - 5); i < candles.size(); i++) {

            swingLow = Math.min(swingLow, candles.get(i).getLow());
        }


        /*
         * ------------------------------------------------------------
         * STOP LOSS BUFFER
         *
         * 0.1% below swing low.
         *
         * Example:
         *
         * swingLow = 100
         *
         * SL = 99.90
         * ------------------------------------------------------------
         */

        double stopLoss = swingLow * 0.999;


        /*
         * ------------------------------------------------------------
         * RISK
         * ------------------------------------------------------------
         */

        double risk = entry - stopLoss;


        if (risk <= 0) {

            result.tradeable = false;

            return;
        }


        /*
         * ------------------------------------------------------------
         * STOP DISTANCE
         * ------------------------------------------------------------
         */

        double stopDistancePercent = (risk / entry) * 100.0;


        /*
         * ------------------------------------------------------------
         * TARGET 1 = 2R
         * ------------------------------------------------------------
         */

        double target1 = entry + (risk * 2.0);


        /*
         * ------------------------------------------------------------
         * TARGET 2 = 3R
         * ------------------------------------------------------------
         */

        double target2 = entry + (risk * 3.0);


        /*
         * ------------------------------------------------------------
         * REWARD
         * ------------------------------------------------------------
         */

        double reward1 = target1 - entry;


        double reward2 = target2 - entry;


        /*
         * ------------------------------------------------------------
         * RISK / REWARD
         * ------------------------------------------------------------
         */

        double riskReward1 = reward1 / risk;


        double riskReward2 = reward2 / risk;


        /*
         * Save everything.
         */

        result.entry = entry;

        result.stopLoss = stopLoss;

        result.target1 = target1;

        result.target2 = target2;

        result.risk = risk;

        result.reward1 = reward1;

        result.reward2 = reward2;

        result.riskReward1 = riskReward1;

        result.riskReward2 = riskReward2;

        result.stopDistancePercent = stopDistancePercent;


        /*
         * ============================================================
         * TRADE FILTER
         * ============================================================
         *
         * Don't take a trade if:
         *
         * 1. Score < 65
         * 2. Stop is too far away
         * 3. R:R < 2
         *
         * The maximum stop distance can be adjusted.
         * ------------------------------------------------------------
         */

        boolean scoreOK = result.score >= 65;


        boolean riskOK = stopDistancePercent <= 1.50;


        boolean rewardOK = riskReward1 >= 2.0;


        result.tradeable = scoreOK && riskOK && rewardOK;
    }
}