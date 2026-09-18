package org.example.util;

import org.example.model.MinuteCandle;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class BreakoutProbabilityNew {

    public static class Result {

        // Same as Pine `vals`:
        //
        // [level][0] = green + high probability
        // [level][1] = green + low probability
        // [level][2] = red   + high probability
        // [level][3] = red   + low probability
        //
        // We expose the final probabilities selected for the latest
        // previous-candle direction.
        public final double[] upProbability = new double[5];
        public final double[] downProbability = new double[5];

        public double bullishPercentage;
        public double bearishPercentage;

        public String bias;

        // Equivalent to the useful Pine counters
        public final long[] greenHigh = new long[5];
        public final long[] greenLow = new long[5];
        public final long[] redHigh = new long[5];
        public final long[] redLow = new long[5];

        public long greenTotal;
        public long redTotal;

        @Override
        public String toString() {

            return "Bias=" + bias + " Bullish=" + bullishPercentage + "%" + " Bearish=" + bearishPercentage + "%" + " UP=" + Arrays.toString(
                upProbability) + " DOWN=" + Arrays.toString(downProbability);
        }
    }


    /*
     * Exact equivalent of Pine:
     *
     * Score(x, i)
     */
    private static void score(MinuteCandle previous, MinuteCandle current, double x, int level, Result result) {

        double previousOpen = previous.getOpen();
        double previousClose = previous.getClose();

        double previousHigh = previous.getHigh();
        double previousLow = previous.getLow();

        double currentHigh = current.getHigh();
        double currentLow = current.getLow();

        boolean green = previousClose > previousOpen;
        boolean red = previousClose < previousOpen;

        /*
         * Pine:
         *
         * hh = h >= h[1] + x
         * ll = l <= l[1] - x
         */
        boolean hh = currentHigh >= previousHigh + x;

        boolean ll = currentLow <= previousLow - x;


        /*
         * Pine:
         *
         * if green and hh
         *     ghh + 1
         */
        if (green && hh) {
            result.greenHigh[level]++;
        }


        /*
         * Pine:
         *
         * if green and ll
         *     gll + 1
         */
        if (green && ll) {
            result.greenLow[level]++;
        }


        /*
         * Pine:
         *
         * if red and hh
         *     rhh + 1
         */
        if (red && hh) {
            result.redHigh[level]++;
        }


        /*
         * Pine:
         *
         * if red and ll
         *     rll + 1
         */
        if (red && ll) {
            result.redLow[level]++;
        }
    }


    /*
     * Strict Java equivalent of:
     *
     * Score(0,0)
     * Score(step,1)
     * Score(step*2,2)
     * Score(step*3,3)
     * Score(step*4,4)
     *
     * `candles` MUST be oldest -> newest.
     */
    public static Result calculate(Deque<MinuteCandle> candles, double percentageStep) {

        Result result = new Result();

        if (candles == null || candles.size() < 2) {
            result.bias = "NEUTRAL";
            return result;
        }


        /*
         * ============================================================
         * FIRST PASS
         *
         * Reproduce Pine's historical matrix counters.
         * ============================================================
         */

        MinuteCandle previous = null;

        for (MinuteCandle current : candles) {

            if (previous == null) {
                previous = current;
                continue;
            }


            /*
             * Pine:
             *
             * green = c[1] > o[1]
             * red   = c[1] < o[1]
             */
            boolean green = previous.getClose() > previous.getOpen();

            boolean red = previous.getClose() < previous.getOpen();


            /*
             * Pine:
             *
             * if green
             *     total green + 1
             *
             * if red
             *     total red + 1
             */
            if (green) {
                result.greenTotal++;
            }

            if (red) {
                result.redTotal++;
            }


            /*
             * IMPORTANT:
             *
             * Pine:
             *
             * step = c * (perc/100)
             *
             * `c` is CURRENT candle close.
             */
            double step = current.getClose() * (percentageStep / 100.0);


            /*
             * Pine:
             *
             * Score(0,0)
             * Score(step,1)
             * Score(step*2,2)
             * Score(step*3,3)
             * Score(step*4,4)
             */

            score(previous, current, 0.0, 0, result);

            score(previous, current, step, 1, result);

            score(previous, current, step * 2, 2, result);

            score(previous, current, step * 3, 3, result);

            score(previous, current, step * 4, 4, result);


            previous = current;
        }


        /*
         * ============================================================
         * SECOND PASS
         *
         * Reproduce Pine's `vals` percentages.
         * ============================================================
         */

        MinuteCandle last = candles.peekLast();


        /*
         * Pine's current `green` / `red` refers to:
         *
         * c[1] > o[1]
         * c[1] < o[1]
         *
         * Therefore when the script is evaluated on the latest bar,
         * it uses the candle immediately BEFORE the latest candle.
         *
         * We need the candle before `last`.
         */
        MinuteCandle previousToLast = null;

        if (candles.size() >= 2) {

            var iterator = candles.descendingIterator();

            iterator.next(); // skip latest

            previousToLast = iterator.next();
        }


        if (previousToLast == null) {
            result.bias = "NEUTRAL";
            return result;
        }


        boolean green = previousToLast.getClose() > previousToLast.getOpen();

        boolean red = previousToLast.getClose() < previousToLast.getOpen();


        /*
         * ============================================================
         * EXACT PINE PROBABILITY SELECTION
         * ============================================================
         *
         * If green:
         *
         * a1 = vals[0][0]
         * b1 = vals[0][1]
         *
         * If red:
         *
         * a2 = vals[0][2]
         * b2 = vals[0][3]
         */

        for (int i = 0; i < 5; i++) {

            if (green) {

                /*
                 * Pine:
                 *
                 * vals[i][0] =
                 *     round((ghh / gtotal) * 100, 2)
                 *
                 * vals[i][1] =
                 *     round((gll / gtotal) * 100, 2)
                 */

                if (result.greenTotal > 0) {

                    result.upProbability[i] = round2(result.greenHigh[i] * 100.0 / result.greenTotal);

                    result.downProbability[i] = round2(result.greenLow[i] * 100.0 / result.greenTotal);
                }

            } else if (red) {

                /*
                 * Pine:
                 *
                 * vals[i][2] =
                 *     round((rhh / rtotal) * 100, 2)
                 *
                 * vals[i][3] =
                 *     round((rll / rtotal) * 100, 2)
                 */

                if (result.redTotal > 0) {

                    result.upProbability[i] = round2(result.redHigh[i] * 100.0 / result.redTotal);

                    result.downProbability[i] = round2(result.redLow[i] * 100.0 / result.redTotal);
                }
            }
        }


        /*
         * ============================================================
         * EXACT PINE BIAS LOGIC
         * ============================================================
         *
         * Pine:
         *
         * s3 = green ?
         *      (max(a1,b1)==a1 ? "BULLISH" : "BEARISH") :
         *      (max(a2,b2)==a2 ? "BULLISH" : "BEARISH")
         */

        double a1 = result.upProbability[0];
        double b1 = result.downProbability[0];

        double a2 = result.upProbability[0];
        double b2 = result.downProbability[0];


        if (green) {

            result.bias = Math.max(a1, b1) == a1 ? "BULLISH" : "BEARISH";

        } else if (red) {

            result.bias = Math.max(a2, b2) == a2 ? "BULLISH" : "BEARISH";

        } else {

            result.bias = "NEUTRAL";
        }


        /*
         * ============================================================
         * EXACT PINE s4 / s5
         * ============================================================
         *
         * s4:
         *
         * green ?
         *     (max(a1,b1)==a1 ? a1 : b1) :
         *     (min(a2,b2)==a2 ? a2 : b2)
         *
         * s5:
         *
         * red ?
         *     (max(a2,b2)==a2 ? a2 : b2) :
         *     (min(a1,b1)==a1 ? a1 : b1)
         */

        double s4;
        double s5;

        if (green) {

            s4 = Math.max(a1, b1) == a1 ? a1 : b1;

            s5 = Math.min(a1, b1) == a1 ? a1 : b1;

        } else {

            s4 = Math.min(a2, b2) == a2 ? a2 : b2;

            s5 = Math.max(a2, b2) == a2 ? a2 : b2;
        }


        /*
         * These correspond to the percentages displayed
         * by the Pine alert.
         */
        result.bullishPercentage = round2(s4);

        result.bearishPercentage = round2(s5);


        return result;
    }


    private static double round2(double value) {

        return Math.round(value * 100.0) / 100.0;
    }

    public static BreakoutProbabilityResult calculateBreakoutProbability(
        Deque<MinuteCandle> candles,
        double perc) {

        BreakoutProbabilityResult result =
            new BreakoutProbabilityResult();

        if (candles == null || candles.size() < 2) {
            result.bias = "NEUTRAL";
            return result;
        }

        /*
         * ============================================================
         * Pine:
         *
         * var total = matrix.new<int>(7,4,0)
         * var vals  = matrix.new<float>(5,4,0.0)
         *
         * ============================================================
         */


        /*
         * ============================================================
         * SAVE NUMBER OF GREEN & RED CANDLES
         *
         * Pine:
         *
         * green = c[1] > o[1]
         * red   = c[1] < o[1]
         *
         * if green
         *     total[5][0] += 1
         *
         * if red
         *     total[5][1] += 1
         *
         * ============================================================
         */

        MinuteCandle previous = null;

        for (MinuteCandle current : candles) {

            if (previous == null) {
                previous = current;
                continue;
            }

            boolean green =
                previous.getClose() > previous.getOpen();

            boolean red =
                previous.getClose() < previous.getOpen();

            if (green) {
                result.greenTotal++;
            }

            if (red) {
                result.redTotal++;
            }

            /*
             * Pine:
             *
             * step = c*(perc/100)
             *
             * c = CURRENT candle close
             */
            double step =
                current.getClose() * (perc / 100.0);


            /*
             * ========================================================
             * Score(0,0)
             * ========================================================
             */
            score(
                previous,
                current,
                0.0,
                0,
                result
            );


            /*
             * ========================================================
             * Score(step,1)
             * ========================================================
             */
            score(
                previous,
                current,
                step,
                1,
                result
            );


            /*
             * ========================================================
             * Score(step*2,2)
             * ========================================================
             */
            score(
                previous,
                current,
                step * 2.0,
                2,
                result
            );


            /*
             * ========================================================
             * Score(step*3,3)
             * ========================================================
             */
            score(
                previous,
                current,
                step * 3.0,
                3,
                result
            );


            /*
             * ========================================================
             * Score(step*4,4)
             * ========================================================
             */
            score(
                previous,
                current,
                step * 4.0,
                4,
                result
            );


            previous = current;
        }


        /*
         * ============================================================
         * FETCH SCORE VALUES
         *
         * Pine:
         *
         * a1 = matrix.get(vals,0,0)
         * b1 = matrix.get(vals,0,1)
         * a2 = matrix.get(vals,0,2)
         * b2 = matrix.get(vals,0,3)
         *
         * ============================================================
         */

        if (result.greenTotal > 0) {

            for (int i = 0; i < 5; i++) {

                result.greenHighProbability[i] =
                    round2(
                        result.greenHigh[i]
                            * 100.0
                            / result.greenTotal
                    );

                result.greenLowProbability[i] =
                    round2(
                        result.greenLow[i]
                            * 100.0
                            / result.greenTotal
                    );
            }
        }


        if (result.redTotal > 0) {

            for (int i = 0; i < 5; i++) {

                result.redHighProbability[i] =
                    round2(
                        result.redHigh[i]
                            * 100.0
                            / result.redTotal
                    );

                result.redLowProbability[i] =
                    round2(
                        result.redLow[i]
                            * 100.0
                            / result.redTotal
                    );
            }
        }


        /*
         * ============================================================
         * IMPORTANT:
         *
         * Pine's `green` and `red` at the LAST BAR refer to c[1]/o[1].
         *
         * Therefore we need the candle immediately before the latest
         * candle.
         * ============================================================
         */

        MinuteCandle latest = candles.peekLast();

        MinuteCandle previousToLatest = null;

        var iterator = candles.descendingIterator();

        // latest
        iterator.next();

        // candle immediately before latest
        if (iterator.hasNext()) {
            previousToLatest = iterator.next();
        }


        if (previousToLatest == null) {
            result.bias = "NEUTRAL";
            return result;
        }


        boolean green =
            previousToLatest.getClose()
                > previousToLatest.getOpen();

        boolean red =
            previousToLatest.getClose()
                < previousToLatest.getOpen();

        /*
         * Store which probability set is selected.
         */
        result.selectedGreen = green;


        /*
         * ============================================================
         * Pine:
         *
         * s3 = green ?
         *      (math.max(a1,b1)==a1 ? "BULLISH" : "BEARISH") :
         *      (math.max(a2,b2)==a2 ? "BULLISH" : "BEARISH")
         *
         * ============================================================
         */

        double a1 =
            result.greenHighProbability[0];

        double b1 =
            result.greenLowProbability[0];

        double a2 =
            result.redHighProbability[0];

        double b2 =
            result.redLowProbability[0];


        if (green) {

            result.bias =
                Math.max(a1, b1) == a1
                    ? "BULLISH"
                    : "BEARISH";

        } else if (red) {

            result.bias =
                Math.max(a2, b2) == a2
                    ? "BULLISH"
                    : "BEARISH";

        } else {

            result.bias = "NEUTRAL";
        }


        /*
         * ============================================================
         * Pine:
         *
         * s4 = green ?
         *      (max(a1,b1)==a1 ? a1 : b1) :
         *      (min(a2,b2)==a2 ? a2 : b2)
         *
         * ============================================================
         */

        double s4;

        if (green) {

            s4 =
                Math.max(a1, b1) == a1
                    ? a1
                    : b1;

        } else {

            s4 =
                Math.min(a2, b2) == a2
                    ? a2
                    : b2;
        }


        /*
         * ============================================================
         * Pine:
         *
         * s5 = red ?
         *      (max(a2,b2)==a2 ? a2 : b2) :
         *      (min(a1,b1)==a1 ? a1 : b1)
         *
         * ============================================================
         */

        double s5;

        if (red) {

            s5 =
                Math.max(a2, b2) == a2
                    ? a2
                    : b2;

        } else {

            s5 =
                Math.min(a1, b1) == a1
                    ? a1
                    : b1;
        }


        result.bullishPercentage = round2(s4);
        result.bearishPercentage = round2(s5);

        return result;
    }
    private static void score(
        MinuteCandle previous,
        MinuteCandle current,
        double x,
        int level,
        BreakoutProbabilityResult result) {

        /*
         * Pine:
         *
         * ghh = matrix.get(total,i,0)
         * gll = matrix.get(total,i,1)
         * rhh = matrix.get(total,i,2)
         * rll = matrix.get(total,i,3)
         */

        boolean green =
            previous.getClose() > previous.getOpen();

        boolean red =
            previous.getClose() < previous.getOpen();


        /*
         * Pine:
         *
         * hh = h >= h[1] + x
         * ll = l <= l[1] - x
         */

        boolean hh =
            current.getHigh()
                >= previous.getHigh() + x;

        boolean ll =
            current.getLow()
                <= previous.getLow() - x;


        /*
         * Pine:
         *
         * if green and hh
         */
        if (green && hh) {
            result.greenHigh[level]++;
        }


        /*
         * Pine:
         *
         * if green and ll
         */
        if (green && ll) {
            result.greenLow[level]++;
        }


        /*
         * Pine:
         *
         * if red and hh
         */
        if (red && hh) {
            result.redHigh[level]++;
        }


        /*
         * Pine:
         *
         * if red and ll
         */
        if (red && ll) {
            result.redLow[level]++;
        }
    }
    public static class BreakoutProbabilityResult {

        // Pine vals matrix equivalent:
        //
        // vals[level][0] = Green + High
        // vals[level][1] = Green + Low
        // vals[level][2] = Red   + High
        // vals[level][3] = Red   + Low

        public final double[] greenHighProbability = new double[5];
        public final double[] greenLowProbability  = new double[5];
        public final double[] redHighProbability   = new double[5];
        public final double[] redLowProbability    = new double[5];

        // Same as useful Pine alert values
        public double bullishPercentage;
        public double bearishPercentage;

        public String bias;

        // Pine total matrix equivalent
        public final long[] greenHigh = new long[5];
        public final long[] greenLow  = new long[5];
        public final long[] redHigh   = new long[5];
        public final long[] redLow    = new long[5];

        // total[5][0] and total[5][1]
        public long greenTotal;
        public long redTotal;

        @Override
        public String toString() {
            return String.format(
                "Bias=%s Bullish=%.2f%% Bearish=%.2f%% " +
                    "UP=[%.2f, %.2f, %.2f, %.2f, %.2f] " +
                    "DOWN=[%.2f, %.2f, %.2f, %.2f, %.2f]",

                bias,
                bullishPercentage,
                bearishPercentage,

                getSelectedUp(0),
                getSelectedUp(1),
                getSelectedUp(2),
                getSelectedUp(3),
                getSelectedUp(4),

                getSelectedDown(0),
                getSelectedDown(1),
                getSelectedDown(2),
                getSelectedDown(3),
                getSelectedDown(4)
            );
        }

        private double getSelectedUp(int level) {
            return selectedGreen
                ? greenHighProbability[level]
                : redHighProbability[level];
        }

        private double getSelectedDown(int level) {
            return selectedGreen
                ? greenLowProbability[level]
                : redLowProbability[level];
        }

        private boolean selectedGreen;
    }
}
