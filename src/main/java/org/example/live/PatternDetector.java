package org.example.live;

import org.example.model.Candle;
import org.example.model.MinuteCandle;

import java.util.List;

public class PatternDetector {

    public enum Pattern {
        BREAKOUT,
        BULL_FLAG,
        PULLBACK_RECOVERY,
        CONSOLIDATION_BREAKOUT,
        HIGHER_HIGH_HIGHER_LOW,
        NONE
    }

    public static class PatternResult {

        public Pattern pattern;
        public double score;
        public String description;

        public PatternResult(Pattern pattern, double score, String description) {
            this.pattern = pattern;
            this.score = score;
            this.description = description;
        }

        @Override
        public String toString() {
            return pattern +
                " | score=" + String.format("%.1f", score) +
                " | " + description;
        }
    }

    // ============================================================
    // MAIN DETECTOR
    // ============================================================

    public static PatternResult detect(List<MinuteCandle> candles) {

        if (candles == null || candles.size() < 30) {
            return new PatternResult(
                Pattern.NONE,
                0,
                "Not enough candles"
            );
        }

        PatternResult breakout = detectBreakout(candles);

        if (breakout.score >= 75)
            return breakout;

        PatternResult bullFlag = detectBullFlag(candles);

        if (bullFlag.score >= 75)
            return bullFlag;

        PatternResult recovery = detectPullbackRecovery(candles);

        if (recovery.score >= 75)
            return recovery;

        PatternResult consolidation =
            detectConsolidationBreakout(candles);

        if (consolidation.score >= 75)
            return consolidation;

        PatternResult structure =
            detectHigherHighHigherLow(candles);

        if (structure.score >= 70)
            return structure;

        return new PatternResult(
            Pattern.NONE,
            0,
            "No strong pattern"
        );
    }

    // ============================================================
    // 1. BREAKOUT
    // ============================================================

    private static PatternResult detectBreakout(List<MinuteCandle> c) {

        int n = c.size();

        MinuteCandle current = c.get(n - 1);

        double previousHigh = Double.MIN_VALUE;

        double avgVolume = 0;

        int lookback = 20;

        for (int i = n - lookback - 1; i < n - 1; i++) {

            previousHigh =
                Math.max(previousHigh, c.get(i).high);

            avgVolume += c.get(i).volume;
        }

        avgVolume /= lookback;

        double volumeRatio =
            current.volume / avgVolume;

        double score = 0;

        // Price breakout
        if (current.close > previousHigh)
            score += 40;

        // Volume confirmation
        if (volumeRatio >= 1.5)
            score += 25;

        else if (volumeRatio >= 1.2)
            score += 15;

        // Strong candle
        double range =
            current.high - current.low;

        if (range > 0) {

            double closePosition =
                (current.close - current.low) / range;

            if (closePosition >= 0.75)
                score += 20;
        }

        // Positive momentum
        if (current.close > c.get(n - 2).close)
            score += 15;

        return new PatternResult(
            Pattern.BREAKOUT,
            score,
            "Price breakout with volume ratio "
                + String.format("%.2f", volumeRatio)
        );
    }

    // ============================================================
    // 2. BULL FLAG
    // ============================================================

    private static PatternResult detectBullFlag(List<MinuteCandle> c) {

        int n = c.size();

        if (n < 15)
            return new PatternResult(
                Pattern.BULL_FLAG,
                0,
                "Not enough candles"
            );

        /*
         * Structure:
         *
         * Strong upward impulse
         *       ↓
         * Controlled pullback
         *       ↓
         * Lower volume
         *       ↓
         * Recovery
         */

        double impulseStart =
            c.get(n - 12).close;

        double impulseHigh =
            c.get(n - 7).high;

        double impulseReturn =
            (impulseHigh - impulseStart)
                / impulseStart * 100;

        // Need strong initial move
        if (impulseReturn < 1.0) {

            return new PatternResult(
                Pattern.BULL_FLAG,
                0,
                "No strong impulse"
            );
        }

        double pullbackLow =
            Double.MAX_VALUE;

        double pullbackVolume = 0;

        for (int i = n - 6; i < n - 1; i++) {

            pullbackLow =
                Math.min(pullbackLow, c.get(i).low);

            pullbackVolume += c.get(i).volume;
        }

        pullbackVolume /= 5;

        double pullback =
            (impulseHigh - pullbackLow)
                / impulseHigh * 100;

        // Pullback should be controlled
        if (pullback > 2.5) {

            return new PatternResult(
                Pattern.BULL_FLAG,
                0,
                "Pullback too deep"
            );
        }

        double avgVolume = 0;

        for (int i = n - 15; i < n - 10; i++) {
            avgVolume += c.get(i).volume;
        }

        avgVolume /= 5;

        double volumeRatio =
            pullbackVolume / avgVolume;

        double score = 0;

        // Strong impulse
        if (impulseReturn >= 2.0)
            score += 30;
        else
            score += 20;

        // Controlled pullback
        if (pullback <= 1.5)
            score += 25;
        else
            score += 15;

        // Volume contraction
        if (volumeRatio < 0.8)
            score += 20;

        else if (volumeRatio < 1.0)
            score += 10;

        // Recovery
        MinuteCandle current = c.get(n - 1);
        MinuteCandle previous = c.get(n - 2);

        if (current.close > previous.close)
            score += 15;

        // Higher low
        if (current.low > pullbackLow)
            score += 10;

        return new PatternResult(
            Pattern.BULL_FLAG,
            score,
            "Impulse "
                + String.format("%.2f", impulseReturn)
                + "%, pullback "
                + String.format("%.2f", pullback)
                + "%"
        );
    }

    // ============================================================
    // 3. PULLBACK RECOVERY
    // ============================================================

    private static PatternResult detectPullbackRecovery(
        List<MinuteCandle> c) {

        int n = c.size();

        double high =
            Double.MIN_VALUE;

        double low =
            Double.MAX_VALUE;

        for (int i = n - 10; i < n - 3; i++) {

            high = Math.max(high, c.get(i).high);
            low = Math.min(low, c.get(i).low);
        }

        MinuteCandle current = c.get(n - 1);
        MinuteCandle previous = c.get(n - 2);

        double score = 0;

        // Previous strong move
        double move =
            (high - low) / low * 100;

        if (move >= 1.5)
            score += 25;

        // Recovery candle
        if (current.close > previous.close)
            score += 20;

        // Higher low
        if (current.low > low)
            score += 20;

        // Close near high
        double range =
            current.high - current.low;

        if (range > 0) {

            double position =
                (current.close - current.low)
                    / range;

            if (position > 0.7)
                score += 20;
        }

        // Break previous candle high
        if (current.close > previous.high)
            score += 15;

        return new PatternResult(
            Pattern.PULLBACK_RECOVERY,
            score,
            "Pullback followed by recovery"
        );
    }

    // ============================================================
    // 4. CONSOLIDATION BREAKOUT
    // ============================================================

    private static PatternResult detectConsolidationBreakout(
        List<MinuteCandle> c) {

        int n = c.size();

        double high =
            Double.MIN_VALUE;

        double low =
            Double.MAX_VALUE;

        double volume = 0;

        for (int i = n - 10; i < n - 2; i++) {

            high = Math.max(high, c.get(i).high);
            low = Math.min(low, c.get(i).low);

            volume += c.get(i).volume;
        }

        volume /= 8;

        MinuteCandle current = c.get(n - 1);

        double rangePercent =
            (high - low) / low * 100;

        double volumeRatio =
            current.volume / volume;

        double score = 0;

        // Tight range
        if (rangePercent <= 2.0)
            score += 35;

        else if (rangePercent <= 3.0)
            score += 20;

        // Breakout
        if (current.close > high)
            score += 40;

        // Volume
        if (volumeRatio >= 1.5)
            score += 25;

        else if (volumeRatio >= 1.2)
            score += 15;

        return new PatternResult(
            Pattern.CONSOLIDATION_BREAKOUT,
            score,
            "Range "
                + String.format("%.2f", rangePercent)
                + "%, volume "
                + String.format("%.2f", volumeRatio)
                + "x"
        );
    }

    // ============================================================
    // 5. HIGHER HIGH / HIGHER LOW
    // ============================================================

    private static PatternResult detectHigherHighHigherLow(
        List<MinuteCandle> c) {

        int n = c.size();

        MinuteCandle c1 = c.get(n - 5);
        MinuteCandle c2 = c.get(n - 3);
        MinuteCandle c3 = c.get(n - 1);

        double score = 0;

        boolean higherHigh =
            c3.high > c1.high &&
                c2.high > c1.high;

        boolean higherLow =
            c3.low > c1.low;

        if (higherHigh)
            score += 50;

        if (higherLow)
            score += 30;

        if (c3.close > c2.close)
            score += 20;

        return new PatternResult(
            Pattern.HIGHER_HIGH_HIGHER_LOW,
            score,
            higherHigh && higherLow
                ? "Bullish HH/HL structure"
                : "Partial HH/HL structure"
        );
    }
}
