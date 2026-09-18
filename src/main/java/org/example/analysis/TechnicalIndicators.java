package org.example.analysis;

import org.example.model.StockData;
import java.util.*;

/**
 * Technical Indicators Calculator
 * Calculates various technical indicators for stock analysis
 */
public class TechnicalIndicators {

    /**
     * Calculate Simple Moving Average (SMA)
     */
    public static double calculateSMA(List<StockData> data, int period) {
        if (data.size() < period) return 0;

        double sum = 0;
        for (int i = data.size() - period; i < data.size(); i++) {
            sum += data.get(i).getClose();
        }
        return sum / period;
    }

    /**
     * Calculate Exponential Moving Average (EMA)
     */
    public static double calculateEMA(List<StockData> data, int period) {
        if (data.size() < period) return 0;

        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(data.subList(0, period), period);

        for (int i = period; i < data.size(); i++) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
        }
        return ema;
    }

    /**
     * Calculate RSI (Relative Strength Index)
     */
    public static double calculateRSI(List<StockData> data, int period) {
        if (data.size() < period + 1) return 50;

        double avgGain = 0, avgLoss = 0;

        // Calculate initial average gain/loss
        for (int i = 1; i <= period; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        // Calculate RSI using smoothed averages
        for (int i = period + 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) {
                avgGain = (avgGain * (period - 1) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgGain = (avgGain * (period - 1)) / period;
                avgLoss = (avgLoss * (period - 1) + Math.abs(change)) / period;
            }
        }

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * Calculate Average True Range (ATR)
     */
    public static double calculateATR(List<StockData> data, int period) {
        if (data.size() < period + 1) return 0;

        List<Double> trueRanges = new ArrayList<>();

        for (int i = 1; i < data.size(); i++) {
            double high = data.get(i).getHigh();
            double low = data.get(i).getLow();
            double prevClose = data.get(i - 1).getClose();

            double tr = Math.max(high - low,
                    Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            trueRanges.add(tr);
        }

        // Calculate ATR as SMA of True Ranges
        double sum = 0;
        int start = Math.max(0, trueRanges.size() - period);
        for (int i = start; i < trueRanges.size(); i++) {
            sum += trueRanges.get(i);
        }
        return sum / period;
    }

    /**
     * Calculate Average Volume
     */
    public static double calculateAverageVolume(List<StockData> data, int period) {
        if (data.isEmpty()) return 0;

        int count = Math.min(period, data.size());
        long sum = 0;
        for (int i = data.size() - count; i < data.size(); i++) {
            sum += data.get(i).getTradedQty();
        }
        return (double) sum / count;
    }

    /**
     * Calculate Average Delivery Percentage
     */
    public static double calculateAverageDeliveryPercent(List<StockData> data, int period) {
        if (data.isEmpty()) return 0;

        int count = Math.min(period, data.size());
        double sum = 0;
        for (int i = data.size() - count; i < data.size(); i++) {
            sum += data.get(i).getDeliveryPercent();
        }
        return sum / count;
    }

    /**
     * Calculate VWAP (Volume Weighted Average Price)
     */
    public static double calculateVWAP(List<StockData> data) {
        if (data.isEmpty()) return 0;

        double cumulativeTPV = 0;  // Typical Price * Volume
        long cumulativeVolume = 0;

        for (StockData stock : data) {
            double typicalPrice = (stock.getHigh() + stock.getLow() + stock.getClose()) / 3;
            cumulativeTPV += typicalPrice * stock.getTradedQty();
            cumulativeVolume += stock.getTradedQty();
        }

        return cumulativeVolume > 0 ? cumulativeTPV / cumulativeVolume : 0;
    }

    /**
     * Calculate Bollinger Bands
     * Returns: [Lower Band, Middle Band (SMA), Upper Band]
     */
    public static double[] calculateBollingerBands(List<StockData> data, int period, double stdDevMultiplier) {
        if (data.size() < period) return new double[]{0, 0, 0};

        double sma = calculateSMA(data, period);

        // Calculate Standard Deviation
        double sumSquaredDiff = 0;
        for (int i = data.size() - period; i < data.size(); i++) {
            double diff = data.get(i).getClose() - sma;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / period);

        return new double[]{
                sma - (stdDevMultiplier * stdDev),  // Lower Band
                sma,                                  // Middle Band
                sma + (stdDevMultiplier * stdDev)    // Upper Band
        };
    }

    /**
     * Calculate Support and Resistance levels
     * Uses recent highs and lows
     */
    public static double[] calculateSupportResistance(List<StockData> data, int period) {
        if (data.isEmpty()) return new double[]{0, 0, 0, 0};

        int count = Math.min(period, data.size());
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        double sumHigh = 0, sumLow = 0;

        for (int i = data.size() - count; i < data.size(); i++) {
            highestHigh = Math.max(highestHigh, data.get(i).getHigh());
            lowestLow = Math.min(lowestLow, data.get(i).getLow());
            sumHigh += data.get(i).getHigh();
            sumLow += data.get(i).getLow();
        }

        // Support 1: Recent low, Support 2: Average low
        // Resistance 1: Average high, Resistance 2: Recent high
        return new double[]{
                lowestLow,                    // Strong Support
                sumLow / count,               // Average Support
                sumHigh / count,              // Average Resistance
                highestHigh                   // Strong Resistance
        };
    }

    /**
     * Check if there's a volume breakout
     */
    public static boolean isVolumeBreakout(List<StockData> data, int period, double threshold) {
        if (data.size() < period + 1) return false;

        double avgVolume = calculateAverageVolume(data.subList(0, data.size() - 1), period);
        long currentVolume = data.get(data.size() - 1).getTradedQty();

        return currentVolume > avgVolume * threshold;
    }

    /**
     * Check if there's a delivery percentage breakout
     */
    public static boolean isDeliveryBreakout(List<StockData> data, int period, double threshold) {
        if (data.size() < period + 1) return false;

        double avgDelivery = calculateAverageDeliveryPercent(data.subList(0, data.size() - 1), period);
        double currentDelivery = data.get(data.size() - 1).getDeliveryPercent();

        return currentDelivery > avgDelivery * threshold;
    }

    /**
     * Check if price broke above resistance
     */
    public static boolean isPriceBreakout(List<StockData> data, int period) {
        if (data.size() < period + 1) return false;

        double[] sr = calculateSupportResistance(data.subList(0, data.size() - 1), period);
        double currentClose = data.get(data.size() - 1).getClose();

        return currentClose > sr[2]; // Above average resistance
    }
    public static boolean isPriceBreakdown(
        List<StockData> data,
        int period) {

        if (data == null || data.size() < period + 1) {
            return false;
        }

        // Exclude today's candle from support calculation
        List<StockData> previousData =
            data.subList(0, data.size() - 1);

        double[] sr =
            calculateSupportResistance(previousData, period);

        // sr[0] = lowest low of previous period
        double support = sr[0];

        double currentClose =
            data.get(data.size() - 1).getClose();

        return currentClose < support;
    }

    /**
     * Detect candlestick patterns
     */
    public static String detectCandlePattern(List<StockData> data) {
        if (data == null || data.isEmpty()) {
            return "None";
        }

        StockData latest = data.get(data.size() - 1);

        double body = latest.getBody();
        double range = latest.getRange();

        // Prevent division by zero / invalid candle
        if (range <= 0) {
            return "None";
        }

        double upperWick = latest.getHigh()
            - Math.max(latest.getOpen(), latest.getClose());

        double lowerWick = Math.min(latest.getOpen(), latest.getClose())
            - latest.getLow();

        double bodyPercent = body / range;
        double upperWickPercent = upperWick / range;
        double lowerWickPercent = lowerWick / range;

        /*
         * ---------------------------------------------------------
         * Previous candle
         * ---------------------------------------------------------
         */
        StockData prev = data.size() >= 2
            ? data.get(data.size() - 2)
            : null;

        /*
         * ---------------------------------------------------------
         * 1. Bullish / Bearish Engulfing
         * Check before generic candle patterns.
         * ---------------------------------------------------------
         */
        if (prev != null) {

            double prevBody = prev.getBody();

            // Bullish Engulfing
            if (prev.isBearish()
                && latest.isBullish()
                && latest.getOpen() <= prev.getClose()
                && latest.getClose() >= prev.getOpen()
                && body > prevBody * 0.8) {

                return "Bullish Engulfing - Strong Buy Signal";
            }

            // Bearish Engulfing
            if (prev.isBullish()
                && latest.isBearish()
                && latest.getOpen() >= prev.getClose()
                && latest.getClose() <= prev.getOpen()
                && body > prevBody * 0.8) {

                return "Bearish Engulfing - Strong Sell Signal";
            }
        }

        /*
         * ---------------------------------------------------------
         * 2. Doji
         * ---------------------------------------------------------
         */
        if (bodyPercent <= 0.10) {

            // Long-legged Doji
            if (upperWickPercent > 0.30 && lowerWickPercent > 0.30) {
                return "Long-Legged Doji - Strong Indecision";
            }

            return "Doji - Indecision";
        }

        /*
         * ---------------------------------------------------------
         * 3. Bullish Hammer
         *
         * Long lower wick
         * Small upper wick
         * Reasonable body
         * ---------------------------------------------------------
         */
        if (lowerWick >= body * 2.0
            && upperWick <= body * 0.5
            && bodyPercent >= 0.10
            && latest.isBullish()) {

            return "Hammer - Bullish Reversal";
        }

        /*
         * ---------------------------------------------------------
         * 4. Bearish Hanging Man
         *
         * Same structure as hammer but bearish candle.
         * Context determines whether it is truly a hanging man.
         * ---------------------------------------------------------
         */
        if (lowerWick >= body * 2.0
            && upperWick <= body * 0.5
            && bodyPercent >= 0.10
            && latest.isBearish()) {

            return "Hanging Man - Bearish Warning";
        }

        /*
         * ---------------------------------------------------------
         * 5. Bullish Inverted Hammer
         * ---------------------------------------------------------
         */
        if (upperWick >= body * 2.0
            && lowerWick <= body * 0.5
            && bodyPercent >= 0.10
            && latest.isBullish()) {

            return "Inverted Hammer - Potential Bullish Reversal";
        }

        /*
         * ---------------------------------------------------------
         * 6. Shooting Star
         *
         * Long upper wick + small lower wick
         * ---------------------------------------------------------
         */
        if (upperWick >= body * 2.0
            && lowerWick <= body * 0.5
            && bodyPercent >= 0.10
            && latest.isBearish()) {

            return "Shooting Star - Bearish Reversal";
        }

        /*
         * ---------------------------------------------------------
         * 7. Bullish Marubozu
         * ---------------------------------------------------------
         */
        if (bodyPercent >= 0.90
            && upperWickPercent <= 0.05
            && lowerWickPercent <= 0.05
            && latest.isBullish()) {

            return "Bullish Marubozu - Strong Bullish Momentum";
        }

        /*
         * ---------------------------------------------------------
         * 8. Bearish Marubozu
         * ---------------------------------------------------------
         */
        if (bodyPercent >= 0.90
            && upperWickPercent <= 0.05
            && lowerWickPercent <= 0.05
            && latest.isBearish()) {

            return "Bearish Marubozu - Strong Bearish Momentum";
        }

        /*
         * ---------------------------------------------------------
         * 9. Strong Bullish Candle
         * ---------------------------------------------------------
         */
        if (latest.isBullish()
            && bodyPercent >= 0.60
            && upperWickPercent <= 0.20) {

            return "Strong Bullish Candle";
        }

        /*
         * ---------------------------------------------------------
         * 10. Strong Bearish Candle
         * ---------------------------------------------------------
         */
        if (latest.isBearish()
            && bodyPercent >= 0.60
            && lowerWickPercent <= 0.20) {

            return "Strong Bearish Candle";
        }

        /*
         * ---------------------------------------------------------
         * 11. Normal Bullish / Bearish Candle
         * ---------------------------------------------------------
         */
        if (latest.isBullish()) {
            return "Bullish Candle";
        }

        if (latest.isBearish()) {
            return "Bearish Candle";
        }

        return "None";
    }

    /**
     * Calculate momentum (Rate of Change)
     */
    public static double calculateMomentum(List<StockData> data, int period) {
        if (data.size() < period + 1) return 0;

        double currentPrice = data.get(data.size() - 1).getClose();
        double pastPrice = data.get(data.size() - 1 - period).getClose();

        return ((currentPrice - pastPrice) / pastPrice) * 100;
    }

    /**
     * Check for price consolidation
     */
    public static boolean isConsolidating(List<StockData> data, int period, double rangePercent) {
        if (data.size() < period) return false;

        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;

        for (int i = data.size() - period; i < data.size(); i++) {
            highestHigh = Math.max(highestHigh, data.get(i).getHigh());
            lowestLow = Math.min(lowestLow, data.get(i).getLow());
        }

        double range = ((highestHigh - lowestLow) / lowestLow) * 100;
        return range < rangePercent;
    }
    /**
     * Calculate ADX (Average Directional Index)
     *
     * ADX measures trend strength.
     *
     * ADX < 20  = Weak / sideways
     * ADX 20-25 = Developing trend
     * ADX > 25  = Strong trend
     */
    public static double calculateADX(List<StockData> data, int period) {

        if (data == null || data.size() < period * 2 + 1) {
            return 0;
        }

        double[] tr = new double[data.size()];
        double[] plusDM = new double[data.size()];
        double[] minusDM = new double[data.size()];

        for (int i = 1; i < data.size(); i++) {

            StockData current = data.get(i);
            StockData previous = data.get(i - 1);

            double high = current.getHigh();
            double low = current.getLow();

            double prevHigh = previous.getHigh();
            double prevLow = previous.getLow();
            double prevClose = previous.getClose();

            // True Range
            tr[i] = Math.max(
                high - low,
                Math.max(
                    Math.abs(high - prevClose),
                    Math.abs(low - prevClose)
                )
            );

            // Directional Movement
            double upMove = high - prevHigh;
            double downMove = prevLow - low;

            plusDM[i] =
                (upMove > downMove && upMove > 0)
                    ? upMove
                    : 0;

            minusDM[i] =
                (downMove > upMove && downMove > 0)
                    ? downMove
                    : 0;
        }

        // Initial smoothed values
        double smoothedTR = 0;
        double smoothedPlusDM = 0;
        double smoothedMinusDM = 0;

        for (int i = 1; i <= period; i++) {
            smoothedTR += tr[i];
            smoothedPlusDM += plusDM[i];
            smoothedMinusDM += minusDM[i];
        }

        List<Double> dxValues = new ArrayList<>();

        for (int i = period + 1; i < data.size(); i++) {

            smoothedTR =
                smoothedTR - (smoothedTR / period) + tr[i];

            smoothedPlusDM =
                smoothedPlusDM
                    - (smoothedPlusDM / period)
                    + plusDM[i];

            smoothedMinusDM =
                smoothedMinusDM
                    - (smoothedMinusDM / period)
                    + minusDM[i];

            if (smoothedTR == 0) {
                continue;
            }

            double plusDI =
                100.0 * smoothedPlusDM / smoothedTR;

            double minusDI =
                100.0 * smoothedMinusDM / smoothedTR;

            double denominator = plusDI + minusDI;

            if (denominator == 0) {
                dxValues.add(0.0);
            } else {
                double dx =
                    100.0
                        * Math.abs(plusDI - minusDI)
                        / denominator;

                dxValues.add(dx);
            }
        }

        if (dxValues.size() < period) {
            return 0;
        }

        // Initial ADX
        double adx = 0;

        for (int i = 0; i < period; i++) {
            adx += dxValues.get(i);
        }

        adx /= period;

        // Wilder smoothing
        for (int i = period; i < dxValues.size(); i++) {
            adx =
                ((adx * (period - 1))
                    + dxValues.get(i))
                    / period;
        }

        return adx;
    }


    /**
     * Calculate +DI
     *
     * Positive directional strength.
     */
    public static double calculatePlusDI(
        List<StockData> data,
        int period) {

        if (data == null || data.size() < period + 1) {
            return 0;
        }

        double trSum = 0;
        double plusDMSum = 0;

        int start = data.size() - period;

        for (int i = Math.max(1, start); i < data.size(); i++) {

            StockData current = data.get(i);
            StockData previous = data.get(i - 1);

            double tr = Math.max(
                current.getHigh() - current.getLow(),
                Math.max(
                    Math.abs(current.getHigh() - previous.getClose()),
                    Math.abs(current.getLow() - previous.getClose())
                )
            );

            double upMove =
                current.getHigh() - previous.getHigh();

            double downMove =
                previous.getLow() - current.getLow();

            double plusDM =
                (upMove > downMove && upMove > 0)
                    ? upMove
                    : 0;

            trSum += tr;
            plusDMSum += plusDM;
        }

        if (trSum == 0) {
            return 0;
        }

        return 100.0 * plusDMSum / trSum;
    }


    /**
     * Calculate -DI
     *
     * Negative directional strength.
     */
    public static double calculateMinusDI(
        List<StockData> data,
        int period) {

        if (data == null || data.size() < period + 1) {
            return 0;
        }

        double trSum = 0;
        double minusDMSum = 0;

        int start = data.size() - period;

        for (int i = Math.max(1, start); i < data.size(); i++) {

            StockData current = data.get(i);
            StockData previous = data.get(i - 1);

            double tr = Math.max(
                current.getHigh() - current.getLow(),
                Math.max(
                    Math.abs(current.getHigh() - previous.getClose()),
                    Math.abs(current.getLow() - previous.getClose())
                )
            );

            double upMove =
                current.getHigh() - previous.getHigh();

            double downMove =
                previous.getLow() - current.getLow();

            double minusDM =
                (downMove > upMove && downMove > 0)
                    ? downMove
                    : 0;

            trSum += tr;
            minusDMSum += minusDM;
        }

        if (trSum == 0) {
            return 0;
        }

        return 100.0 * minusDMSum / trSum;
    }


    /**
     * Calculate Relative Strength of stock against benchmark.
     *
     * Example:
     *
     * Stock return  = +8%
     * NIFTY return  = +3%
     *
     * Relative Strength = +5%
     *
     * Positive = stock outperforming benchmark
     * Negative = stock underperforming benchmark
     */
    public static double calculateRelativeStrength(
        List<StockData> stockData,
        List<StockData> benchmarkData,
        int period) {

        if (stockData == null ||
            benchmarkData == null ||
            stockData.size() <= period ||
            benchmarkData.size() <= period) {

            return 0;
        }

        double currentStock =
            stockData.get(stockData.size() - 1).getClose();

        double previousStock =
            stockData.get(stockData.size() - 1 - period).getClose();

        double currentBenchmark =
            benchmarkData.get(benchmarkData.size() - 1).getClose();

        double previousBenchmark =
            benchmarkData.get(benchmarkData.size() - 1 - period).getClose();

        if (previousStock <= 0 || previousBenchmark <= 0) {
            return 0;
        }

        double stockReturn =
            ((currentStock - previousStock)
                / previousStock) * 100.0;

        double benchmarkReturn =
            ((currentBenchmark - previousBenchmark)
                / previousBenchmark) * 100.0;

        return stockReturn - benchmarkReturn;
    }


    /**
     * Check whether stock is in bullish trend structure.
     *
     * Uses:
     * Higher High + Higher Low
     */
    public static boolean isHigherHighHigherLow(
        List<StockData> data,
        int lookback) {

        if (data == null || data.size() < lookback * 2) {
            return false;
        }

        int size = data.size();

        double recentHigh = Double.NEGATIVE_INFINITY;
        double previousHigh = Double.NEGATIVE_INFINITY;

        double recentLow = Double.POSITIVE_INFINITY;
        double previousLow = Double.POSITIVE_INFINITY;

        int recentStart = size - lookback;
        int previousStart = size - (lookback * 2);

        for (int i = recentStart; i < size; i++) {

            recentHigh =
                Math.max(recentHigh, data.get(i).getHigh());

            recentLow =
                Math.min(recentLow, data.get(i).getLow());
        }

        for (int i = previousStart; i < recentStart; i++) {

            previousHigh =
                Math.max(previousHigh, data.get(i).getHigh());

            previousLow =
                Math.min(previousLow, data.get(i).getLow());
        }

        return recentHigh > previousHigh
            && recentLow > previousLow;
    }


    /**
     * Check whether stock is in bearish trend structure.
     *
     * Lower High + Lower Low
     */
    public static boolean isLowerHighLowerLow(
        List<StockData> data,
        int lookback) {

        if (data == null || data.size() < lookback * 2) {
            return false;
        }

        int size = data.size();

        double recentHigh = Double.NEGATIVE_INFINITY;
        double previousHigh = Double.NEGATIVE_INFINITY;

        double recentLow = Double.POSITIVE_INFINITY;
        double previousLow = Double.POSITIVE_INFINITY;

        int recentStart = size - lookback;
        int previousStart = size - (lookback * 2);

        for (int i = recentStart; i < size; i++) {

            recentHigh =
                Math.max(recentHigh, data.get(i).getHigh());

            recentLow =
                Math.min(recentLow, data.get(i).getLow());
        }

        for (int i = previousStart; i < recentStart; i++) {

            previousHigh =
                Math.max(previousHigh, data.get(i).getHigh());

            previousLow =
                Math.min(previousLow, data.get(i).getLow());
        }

        return recentHigh < previousHigh
            && recentLow < previousLow;
    }


    /**
     * Confirm bullish breakout.
     *
     * Conditions:
     * 1. Current close above previous resistance
     * 2. Volume confirmation
     * 3. Price above VWAP
     * 4. EMA20 above EMA50
     * 5. ADX confirms trend strength
     * 6. Not consolidating
     */
    public static boolean isBreakoutConfirmed(
        List<StockData> data,
        int resistancePeriod,
        int volumePeriod,
        double volumeMultiplier,
        int adxPeriod,
        double minimumADX) {

        if (data == null ||
            data.size() < Math.max(
                resistancePeriod + 1,
                Math.max(volumePeriod + 1, adxPeriod * 2 + 1))) {

            return false;
        }

        StockData latest =
            data.get(data.size() - 1);

        List<StockData> previousData =
            data.subList(0, data.size() - 1);

        double[] sr =
            calculateSupportResistance(
                previousData,
                resistancePeriod
            );

        double resistance =
            sr[3]; // actual previous highest high

        double currentClose =
            latest.getClose();

        double averageVolume =
            calculateAverageVolume(
                previousData,
                volumePeriod
            );

        double currentVolume =
            latest.getTradedQty();

        double vwap =
            calculateVWAP(data);

        double ema20 =
            calculateEMA(data, 20);

        double ema50 =
            calculateEMA(data, 50);

        double adx =
            calculateADX(data, adxPeriod);

        boolean priceBreakout =
            currentClose > resistance;

        boolean volumeConfirmed =
            currentVolume >
                averageVolume * volumeMultiplier;

        boolean aboveVWAP =
            currentClose > vwap;

        boolean bullishEMA =
            ema20 > ema50;

        boolean trendStrong =
            adx >= minimumADX;

        boolean notConsolidating =
            !isConsolidating(
                previousData,
                Math.min(10, previousData.size()),
                3.0
            );

        return priceBreakout
            && volumeConfirmed
            && aboveVWAP
            && bullishEMA
            && trendStrong
            && notConsolidating;
    }


    /**
     * Confirm bearish breakdown.
     */
    public static boolean isBreakdownConfirmed(
        List<StockData> data,
        int supportPeriod,
        int volumePeriod,
        double volumeMultiplier,
        int adxPeriod,
        double minimumADX) {

        if (data == null ||
            data.size() < Math.max(
                supportPeriod + 1,
                Math.max(volumePeriod + 1, adxPeriod * 2 + 1))) {

            return false;
        }

        StockData latest =
            data.get(data.size() - 1);

        List<StockData> previousData =
            data.subList(0, data.size() - 1);

        double[] sr =
            calculateSupportResistance(
                previousData,
                supportPeriod
            );

        double support =
            sr[0]; // actual previous lowest low

        double currentClose =
            latest.getClose();

        double averageVolume =
            calculateAverageVolume(
                previousData,
                volumePeriod
            );

        double currentVolume =
            latest.getTradedQty();

        double vwap =
            calculateVWAP(data);

        double ema20 =
            calculateEMA(data, 20);

        double ema50 =
            calculateEMA(data, 50);

        double adx =
            calculateADX(data, adxPeriod);

        boolean priceBreakdown =
            currentClose < support;

        boolean volumeConfirmed =
            currentVolume >
                averageVolume * volumeMultiplier;

        boolean belowVWAP =
            currentClose < vwap;

        boolean bearishEMA =
            ema20 < ema50;

        boolean trendStrong =
            adx >= minimumADX;

        boolean notConsolidating =
            !isConsolidating(
                previousData,
                Math.min(10, previousData.size()),
                3.0
            );

        return priceBreakdown
            && volumeConfirmed
            && belowVWAP
            && bearishEMA
            && trendStrong
            && notConsolidating;
    }


    /**
     * Overall bullish trend confirmation.
     */
    public static boolean isBullishTrend(
        List<StockData> data) {

        if (data == null || data.size() < 50) {
            return false;
        }

        double price =
            data.get(data.size() - 1).getClose();

        double ema20 =
            calculateEMA(data, 20);

        double ema50 =
            calculateEMA(data, 50);

        double rsi =
            calculateRSI(data, 14);

        double adx =
            calculateADX(data, 14);

        return price > ema20
            && ema20 > ema50
            && rsi > 50
            && adx >= 20
            && isHigherHighHigherLow(data, 10);
    }


    /**
     * Overall bearish trend confirmation.
     */
    public static boolean isBearishTrend(
        List<StockData> data) {

        if (data == null || data.size() < 50) {
            return false;
        }

        double price =
            data.get(data.size() - 1).getClose();

        double ema20 =
            calculateEMA(data, 20);

        double ema50 =
            calculateEMA(data, 50);

        double rsi =
            calculateRSI(data, 14);

        double adx =
            calculateADX(data, 14);

        return price < ema20
            && ema20 < ema50
            && rsi < 50
            && adx >= 20
            && isLowerHighLowerLow(data, 10);
    }
}
