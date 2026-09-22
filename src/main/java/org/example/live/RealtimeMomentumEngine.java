package org.example.live;

import com.zerodhatech.models.Depth;
import com.zerodhatech.models.Tick;
import org.example.model.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import com.zerodhatech.models.Tick;
import org.example.service.TelegramAlertService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class RealtimeMomentumEngine {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");

    // =========================================================
    // MARKET TIMES
    // =========================================================

    private static final LocalTime PREOPEN_START = LocalTime.of(9, 0);

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);

    private static final LocalDateTime PREOPEN_END = LocalDate.now(MARKET_ZONE).atTime(9, 8);

    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);


    // =========================================================
    // HISTORY
    // =========================================================

    private static final int HISTORY_SIZE = 500;

    private static final int MIN_HISTORY = 1;


    // =========================================================
    // MOMENTUM FILTERS
    // =========================================================

    private static final double MIN_BUY_SELL_RATIO = 1.5;

    private static final double MIN_RATIO_INCREASE_PCT = 10.0;

    private static final double MIN_RVOL = 1.5;

    private static final double MIN_MOMENTUM_SCORE = 70.0;


    // =========================================================
    // LIVE CANDLE
    // =========================================================

    private static final ConcurrentHashMap<String, MinuteCandle> candles = new ConcurrentHashMap<>();


    // =========================================================
    // COMPLETED CANDLE HISTORY
    // =========================================================

    private static ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory = new ConcurrentHashMap<>();


    // =========================================================
    // REAL-TIME STATE
    // =========================================================

    private static final ConcurrentHashMap<String, RealTimeStockState> realtimeState = new ConcurrentHashMap<>();


    // =========================================================
    // PRE-OPEN STATE
    //
    // Populated directly from WebSocket ticks
    // between 09:00 and 09:15.
    // =========================================================

    private static final ConcurrentHashMap<String, PreOpenState> preOpenStates = new ConcurrentHashMap<>();


    // =========================================================
    // FINAL PRE-OPEN CANDIDATES
    //
    // symbol -> PreOpenData
    // =========================================================

    private static final ConcurrentHashMap<String, PreOpenData> preOpenCandidates = new ConcurrentHashMap<>();


    // =========================================================
    // PRE-OPEN FINALIZED FLAG
    // =========================================================

    private static volatile boolean preOpenFinalized = false;

    private static volatile LocalDate preOpenDate = null;


    // =========================================================
    // SIGNAL CONTROL
    // =========================================================

    private static final ConcurrentHashMap<String, LocalDateTime> lastSignalTime = new ConcurrentHashMap<>();


    // =========================================================
    // ASYNC DB SAVE QUEUE
    // =========================================================

    private static final BlockingQueue<MinuteCandle> saveQueue = new LinkedBlockingQueue<>(100_000);

    private static final Map<String, Long> lastTickTime = new ConcurrentHashMap<>();


    // =========================================================
    // ON TICK
    // =========================================================
    private static int preOpencounter = 0;

    private static Map<String, PreviousDayData> previousDayDataMap = null;

    private static Set<String> symbols = new HashSet<>();

    private static final Map<String, IntraminuteState> intraminuteStates = new ConcurrentHashMap<>();

    private static Map<String, StockDailyFeature> stockDailyFeatureMap = null;
    private static Map<String, FundamentalData> fundamentalDataMap = null;

    private static final Map<String, VWAPState> vwapStates =
            new ConcurrentHashMap<>();

    private static double updateVWAP(
            String symbol,
            MinuteCandle candle) {

        VWAPState state =
                vwapStates.computeIfAbsent(
                        symbol,
                        k -> new VWAPState()
                );

        LocalDate candleDate =
                candle.time.toLocalDate();

        // New trading day
        if (state.date == null ||
                !state.date.equals(candleDate)) {

            state.date = candleDate;

            state.cumulativePV = 0.0;
            state.cumulativeVolume = 0.0;
            state.vwap = 0.0;
        }

        if (candle.volume <= 0) {
            return state.vwap;
        }

        double typicalPrice =
                (candle.high
                        + candle.low
                        + candle.close) / 3.0;

        state.cumulativePV +=
                typicalPrice * candle.volume;

        state.cumulativeVolume += candle.volume;

        state.vwap =
                state.cumulativePV /
                        state.cumulativeVolume;

        return state.vwap;
    }

    public static void setPreDayMap(Map<String, PreviousDayData> data, ConcurrentHashMap<String, Deque<MinuteCandle>> misHis, Map<String, StockDailyFeature> featueMap, Map<String, FundamentalData> fundamentalMap) {
        previousDayDataMap = data;
        minuteHistory = misHis;
        stockDailyFeatureMap = featueMap;
        fundamentalDataMap = fundamentalMap;
    }

    public static void onTick(String symbol, Tick tick) {

        if (symbol == null || tick == null || tick.getLastTradedTime() == null) {

            return;
        }
        long tickTime = tick.getLastTradedTime().getTime();

        Long previousTime = lastTickTime.get(symbol);

        // Ignore old / duplicate tick
        if (previousTime != null && tickTime <= previousTime) {
            return;
        }

        // Store latest tick time
        lastTickTime.put(symbol, tickTime);

        // =====================================================
        // MARKET TIME
        // =====================================================

        LocalDateTime dateTime = Instant.ofEpochMilli(tickTime).atZone(MARKET_ZONE).toLocalDateTime();

        LocalTime time = dateTime.toLocalTime();


        // =====================================================
        // PRE-OPEN
        // 09:00 - 09:15
        // =====================================================

        if (dateTime.isBefore(PREOPEN_END)) {
            preOpencounter++;
            processPreOpenTick(symbol, tick, dateTime);
            if (preOpencounter % 100 == 0) {
                System.out.println("preOpencounter " + preOpencounter);
            }

            return;
        }


        // =====================================================
        // OUTSIDE MARKET
        // =====================================================


        // =====================================================
        // FIRST MARKET TICK
        //
        // Finalize pre-open exactly once.
        // =====================================================

        if (!preOpenFinalized && !dateTime.isBefore(PREOPEN_END)) {

            finalizePreOpen(dateTime.toLocalDate());
        }
        if (time.isBefore(MARKET_OPEN) || time.isAfter(MARKET_CLOSE)) {

            System.exit(0);
            return;
        }


        // =====================================================
        // MARKET MINUTE
        // =====================================================

        LocalDateTime minute = dateTime.withSecond(0).withNano(0);


        // =====================================================
        // REALTIME STOCK STATE
        // =====================================================


        evaluateIntraminuteMomentum(symbol, tick, minute);

        realtimeState.compute(symbol, (key, state) -> {

            if (state == null) {

                state = new RealTimeStockState();

                state.setSymbol(symbol);
            }

            state.setLastTickTime(dateTime);

            state.setLastPrice(tick.getLastTradedPrice());

            double buy = tick.getTotalBuyQuantity();

            double sell = tick.getTotalSellQuantity();

            state.setTotalBuyQty(buy);

            state.setTotalSellQty(sell);

            double ratio = sell > 0 ? buy / sell : 0.0;

            state.setBuySellRatio(ratio);

            state.setCurrentMinute(minute);

            return state;
        });


        // =====================================================
        // LIVE CANDLE
        // =====================================================

        candles.compute(symbol, (key, candle) -> {

            // =========================================
            // FIRST TICK FOR THIS SYMBOL
            // =========================================

            if (candle == null) {

                return createNew(symbol, minute, tick);
            }


            // =========================================
            // NEW MINUTE
            // =========================================

            if (!candle.time.equals(minute)) {

                // -------------------------------------
                // FINALIZE VOLUME
                // -------------------------------------

                candle.volume = Math.max(0, candle.endVolume - candle.startVolume);

                double vwap = updateVWAP(symbol, candle);


                // -------------------------------------
                // PROCESS COMPLETED CANDLE
                // -------------------------------------

                processCompletedMinute(symbol, candle,vwap);


                // -------------------------------------
                // ASYNC DB SAVE
                // -------------------------------------


                // -------------------------------------
                // NEW CANDLE
                // -------------------------------------

                return createNew(symbol, minute, tick);
            }


            // =========================================
            // SAME MINUTE
            // =========================================
            update(candle, tick);

            return candle;
        });
    }

    private static void evaluateIntraminuteMomentum(String symbol, Tick tick, LocalDateTime minute) {

        double price = tick.getLastTradedPrice();

        if (price <= 0) {
            return;
        }


        // =====================================================
        // TICK DATA
        // =====================================================


        double qtyDouble = tick.getLastTradedQuantity();

        long lastQty = Double.isFinite(qtyDouble) ? (long) Math.max(0.0, qtyDouble) : 0L;

        long volumeToday = tick.getVolumeTradedToday();


        // =====================================================
        // MARKET DEPTH
        // =====================================================

        Map<String, ArrayList<Depth>> depth = tick.getMarketDepth();

            ArrayList<Depth> bids = depth != null ? depth.get("BUY") : null;

        ArrayList<Depth> asks = depth != null ? depth.get("SELL") : null;


        // =====================================================
        // DEPTH VALUES
        // =====================================================

        double bestBid = 0.0;
        double bestAsk = 0.0;

        double bidQty = 0.0;
        double askQty = 0.0;

        double weightedBidQty = 0.0;
        double weightedAskQty = 0.0;


        // =====================================================
        // TOP 5 DEPTH
        // =====================================================

        int depthLevels = 5;


        if (bids != null && !bids.isEmpty()) {

            int n = Math.min(depthLevels, bids.size());

            for (int i = 0; i < n; i++) {

                Depth d = bids.get(i);

                if (d == null) {
                    continue;
                }

                double qty = Math.max(0.0, d.getQuantity());
                double p = d.getPrice();

                if (i == 0) {
                    bestBid = p;
                }

                bidQty += qty;

                /*
                 * Give more importance to levels
                 * closer to market.
                 */

                double weight = switch (i) {
                    case 0 -> 1.00;
                    case 1 -> 0.75;
                    case 2 -> 0.50;
                    case 3 -> 0.35;
                    default -> 0.20;
                };

                weightedBidQty += qty * weight;
            }
        }


        if (asks != null && !asks.isEmpty()) {

            int n = Math.min(depthLevels, asks.size());

            for (int i = 0; i < n; i++) {

                Depth d = asks.get(i);

                if (d == null) {
                    continue;
                }

                double qty = Math.max(0.0, d.getQuantity());
                double p = d.getPrice();

                if (i == 0) {
                    bestAsk = p;
                }

                askQty += qty;

                double weight = switch (i) {
                    case 0 -> 1.00;
                    case 1 -> 0.75;
                    case 2 -> 0.50;
                    case 3 -> 0.35;
                    default -> 0.20;
                };

                weightedAskQty += qty * weight;
            }
        }


        // =====================================================
        // DEPTH IMBALANCE
        // =====================================================

        double depthTotal = weightedBidQty + weightedAskQty;

        double depthImbalance = depthTotal > 0 ? (weightedBidQty - weightedAskQty) / depthTotal : 0.0;


        double bidRatio = depthTotal > 0 ? weightedBidQty / depthTotal : 0.50;


        double rawBSRatio = weightedAskQty > 0 ? weightedBidQty / weightedAskQty : 0.0;


        // =====================================================
        // SPREAD
        // =====================================================

        double spread = 0.0;
        double spreadPct = 0.0;

        if (bestBid > 0 && bestAsk > 0) {

            spread = bestAsk - bestBid;

            double mid = (bestBid + bestAsk) / 2.0;

            if (mid > 0) {
                spreadPct = (spread / mid) * 100.0;
            }
        }


        // =====================================================
        // GET STATE
        // =====================================================

        IntraminuteState state = intraminuteStates.get(symbol);


        // =====================================================
        // NEW MINUTE
        // =====================================================

        if (state == null || !minute.equals(state.minute)) {

            state = new IntraminuteState();

            state.symbol = symbol;
            state.minute = minute;

            state.openPrice = price;
            state.lastPrice = price;

            state.startVolume = volumeToday;
            state.lastVolume = volumeToday;

            state.buyVolume = 0L;
            state.sellVolume = 0L;
            state.volumeDelta = 0L;

            state.lastTradeDirection = 0;

            state.upTicks = 0;
            state.downTicks = 0;

            state.signalLevel = 0;

            state.tickCount = 1;

            /*
             * Store initial depth.
             */

            state.bestBid = bestBid;
            state.bestAsk = bestAsk;

            state.depthImbalance = depthImbalance;

            intraminuteStates.put(symbol, state);

            return;
        }


        // =====================================================
        // PREVIOUS VALUES
        // =====================================================

        double previousPrice = state.lastPrice;


        // =====================================================
        // PRICE CHANGE
        // =====================================================

        double tickPriceChange = 0.0;

        if (previousPrice > 0) {

            tickPriceChange = ((price - previousPrice) / previousPrice) * 100.0;
        }


        double priceReturn = 0.0;

        if (state.openPrice > 0) {

            priceReturn = ((price - state.openPrice) / state.openPrice) * 100.0;
        }


        // =====================================================
        // CLASSIFY TRADE
        // =====================================================

        int direction = state.lastTradeDirection;

        if (lastQty > 0) {

            if (price > previousPrice) {

                direction = 1;

            } else if (price < previousPrice) {

                direction = -1;
            }

            if (direction > 0) {

                state.buyVolume += lastQty;

            } else if (direction < 0) {

                state.sellVolume += lastQty;
            }

            state.lastTradeDirection = direction;
        }


        // =====================================================
        // DELTA
        // =====================================================

        state.volumeDelta = state.buyVolume - state.sellVolume;


        long classifiedVolume = state.buyVolume + state.sellVolume;


        double deltaRatio = classifiedVolume > 0 ? (double) state.volumeDelta / classifiedVolume : 0.0;


        // =====================================================
        // MINUTE VOLUME
        // =====================================================

        long candleVolume = Math.max(0L, volumeToday - state.startVolume);


        // =====================================================
        // UP / DOWN TICKS
        // =====================================================

        if (tickPriceChange > 0) {

            state.upTicks++;

        } else if (tickPriceChange < 0) {

            state.downTicks++;
        }


        int directionalTicks = state.upTicks + state.downTicks;


        double upTickRatio = directionalTicks > 0 ? (double) state.upTicks / directionalTicks : 0.50;


        // =====================================================
        // UPDATE STATE
        // =====================================================

        state.lastPrice = price;
        state.lastVolume = volumeToday;

        state.depthImbalance = depthImbalance;

        state.bestBid = bestBid;
        state.bestAsk = bestAsk;

        state.tickCount++;


        // =====================================================
        // SCORE
        // =====================================================

        int score = 0;


        // =====================================================
        // 1. PRICE MOMENTUM — 20 POINTS
        // =====================================================

        if (priceReturn >= 0.10)
            score += 5;

        if (priceReturn >= 0.20)
            score += 5;

        if (priceReturn >= 0.30)
            score += 5;

        if (priceReturn >= 0.50)
            score += 5;


        // =====================================================
        // 2. TICK DIRECTION — 15 POINTS
        // =====================================================

        if (upTickRatio >= 0.55)
            score += 5;

        if (upTickRatio >= 0.65)
            score += 5;

        if (upTickRatio >= 0.75)
            score += 5;


        // =====================================================
        // 3. EXECUTED BUYING / DELTA — 20 POINTS
        // =====================================================

        if (deltaRatio >= 0.10)
            score += 5;

        if (deltaRatio >= 0.20)
            score += 5;

        if (deltaRatio >= 0.30)
            score += 5;

        if (deltaRatio >= 0.40)
            score += 5;


        // =====================================================
        // 4. MARKET DEPTH — 25 POINTS
        // =====================================================

        /*
         * Positive depth imbalance.
         */

        if (depthImbalance >= 0.10)
            score += 5;

        if (depthImbalance >= 0.20)
            score += 5;

        if (depthImbalance >= 0.35)
            score += 5;

        if (depthImbalance >= 0.50)
            score += 5;


        /*
         * Bid quantity greater than ask quantity.
         */

        if (weightedBidQty > weightedAskQty)
            score += 5;


        // =====================================================
        // 5. VOLUME — 10 POINTS
        // =====================================================

        if (candleVolume >= 25_000)
            score += 3;

        if (candleVolume >= 75_000)
            score += 3;

        if (candleVolume >= 150_000)
            score += 4;


        // =====================================================
        // 6. PRICE RESPONDING TO BUYING — 10 POINTS
        // =====================================================

        if (deltaRatio >= 0.20 && priceReturn >= 0.10) {

            score += 5;
        }

        if (deltaRatio >= 0.30 && priceReturn >= 0.20) {

            score += 5;
        }


        // =====================================================
        // NEGATIVE CONDITIONS
        // =====================================================

        if (priceReturn < 0)
            score -= 15;

        if (upTickRatio < 0.45)
            score -= 10;

        if (deltaRatio < -0.20)
            score -= 15;

        if (depthImbalance < -0.20)
            score -= 10;


        // =====================================================
        // SPREAD PENALTY
        // =====================================================

        /*
         * Avoid stocks with excessively wide spread.
         *
         * Adjust this threshold based on the stock universe.
         */

        if (spreadPct > 0.50)
            score -= 5;

        if (spreadPct > 1.00)
            score -= 10;


        // =====================================================
        // LIMIT
        // =====================================================

        score = Math.max(0, Math.min(100, score));


        // =====================================================
        // HIGH PROBABILITY CONDITIONS
        // =====================================================

        boolean highProbability = lastQty > 0 && candleVolume >= 20_000 && priceReturn >= 0.15 && upTickRatio >= 0.58 && deltaRatio >= 0.20 && depthImbalance >= 0.15 && score >= 65;


        // =====================================================
        // VERY STRONG
        // =====================================================

        boolean veryStrong = lastQty > 0 && candleVolume >= 75_000 && priceReturn >= 0.30 && upTickRatio >= 0.68 && deltaRatio >= 0.30 && depthImbalance >= 0.30 && score >= 80;


        // =====================================================
        // SIGNAL
        // =====================================================

        if (veryStrong) {

            state.signalLevel = veryStrong ? 2 : 1;


            System.out.println();

            System.out.println("================================================");

            System.out.println(veryStrong ? "🔥 VERY STRONG LONG" : "🚨 HIGH PROBABILITY LONG");

            System.out.println("================================================");

            System.out.println("Symbol          : " + symbol);

            System.out.println("Time            : " + tick.getLastTradedTime());

            System.out.println("Price           : " + String.format("%.2f", price));

            System.out.println("Price Return    : " + String.format("%.2f", priceReturn) + "%");

            System.out.println("Minute Volume   : " + candleVolume);

            System.out.println("Last Qty        : " + lastQty);

            System.out.println("Buy Volume      : " + state.buyVolume);

            System.out.println("Sell Volume     : " + state.sellVolume);

            System.out.println("Volume Delta    : " + state.volumeDelta);

            System.out.println("Delta %         : " + String.format("%.1f", deltaRatio * 100) + "%");

            System.out.println("Up Tick %       : " + String.format("%.1f", upTickRatio * 100) + "%");

            System.out.println("Weighted Bid    : " + String.format("%.0f", weightedBidQty));

            System.out.println("Weighted Ask    : " + String.format("%.0f", weightedAskQty));

            System.out.println("Depth Imbalance : " + String.format("%.1f", depthImbalance * 100) + "%");

            System.out.println("Depth B/S       : " + String.format("%.2f", rawBSRatio));

            System.out.println("Best Bid        : " + bestBid);

            System.out.println("Best Ask        : " + bestAsk);

            System.out.println("Spread %        : " + String.format("%.3f", spreadPct) + "%");

            System.out.println("Score           : " + score);

            System.out.println("Signal          : " + (veryStrong ? "VERY STRONG" : "HIGH PROBABILITY"));

            System.out.println("================================================");
        }
    }

    // =========================================================
    // PRE-OPEN TICK
    // =========================================================

    private static void processPreOpenTick(String symbol, Tick tick, LocalDateTime dateTime) {

        preOpenStates.compute(symbol, (key, state) -> {

            double price = tick.getLastTradedPrice();

            if (price <= 0) {
                return state;
            }


            // =========================================
            // FIRST PRE-OPEN TICK
            // =========================================

            if (state == null) {

                state = new PreOpenState();

                state.symbol = symbol;

                /*
                 * Kite Tick provides previous close.
                 *
                 * If your Tick version exposes this
                 * differently, replace this getter
                 * with the previous-close source you
                 * already use.
                 */
                state.previousClose = tick.getClosePrice();

                state.firstPrice = price;

                state.lastPrice = price;

                state.high = price;

                state.low = price;

                state.startVolume = tick.getVolumeTradedToday();

                state.lastVolume = tick.getVolumeTradedToday();

                state.tickCount = 1;

                state.lastTickTime = dateTime;

                return state;
            }


            // =========================================
            // UPDATE PRE-OPEN STATE
            // =========================================

            state.lastPrice = price;

            state.high = Math.max(state.high, price);

            state.low = Math.min(state.low, price);

            state.lastVolume = tick.getVolumeTradedToday();

            state.tickCount++;

            state.lastTickTime = dateTime;

            return state;
        });
    }


    // =========================================================
    // FINALIZE PRE-OPEN
    // =========================================================

    private static synchronized void finalizePreOpen(java.time.LocalDate tradingDate) {

        if (preOpenFinalized && tradingDate.equals(preOpenDate)) {

            return;
        }


        preOpenCandidates.clear();


        System.out.println();
        System.out.println("==========================================");

        System.out.println("FINALIZING PRE-OPEN");

        System.out.println("Stocks observed: " + preOpenStates.size());

        System.out.println("==========================================");


        for (PreOpenState state : preOpenStates.values()) {

            if (state == null) {
                continue;
            }


            if (state.previousClose <= 0 || state.lastPrice <= 0) {

                continue;
            }


            // =============================================
            // GAP
            // =============================================

            double gapPct = percent(state.previousClose, state.lastPrice);


            /*
             * We are looking for LONG momentum.
             *
             * Ignore negative/flat gaps for the
             * initial pre-open candidate list.
             */

            if (gapPct < 1.0) {
                continue;
            }


            // =============================================
            // PRE-OPEN RANGE
            // =============================================

            double rangePct = state.previousClose > 0 ? ((state.high - state.low) / state.previousClose) * 100.0 : 0;


            // =============================================
            // PRE-OPEN VOLUME
            // =============================================

            double preOpenVolume = Math.max(0, state.lastVolume - state.startVolume);


            // =============================================
            // SCORE
            // =============================================

            double score = calculatePreOpenScore(gapPct, rangePct, preOpenVolume, state.tickCount);


            if (score < 50) {
                continue;
            }


            PreOpenData data = new PreOpenData();

            data.symbol = state.symbol;

            data.previousClose = state.previousClose;

            data.preOpenPrice = state.lastPrice;

            data.gapPct = gapPct;

            data.preOpenHigh = state.high;

            data.preOpenLow = state.low;

            data.preOpenRangePct = rangePct;

            data.preOpenVolume = preOpenVolume;

            data.tickCount = state.tickCount;

            data.score = score;


            preOpenCandidates.put(state.symbol, data);
        }


        preOpenFinalized = true;

        preOpenDate = tradingDate;


        // =============================================
        // PRINT TOP CANDIDATES
        // =============================================

        preOpenCandidates.values().stream().sorted((a, b) -> Double.compare(b.score, a.score)).limit(20).forEach(
                data -> System.out.printf(
                        "PREOPEN %-15s " + "Score:%6.1f " + "Gap:%+6.2f%% " + "Price:%8.2f " + "Range:%5.2f%%%n",

                        data.symbol, data.score, data.gapPct, data.preOpenPrice, data.preOpenRangePct));


        System.out.println("Pre-open candidates: " + preOpenCandidates.size());

        System.out.println("==========================================");
    }


    // =========================================================
    // PRE-OPEN SCORE
    // =========================================================

    private static double calculatePreOpenScore(double gapPct, double rangePct, double preOpenVolume, int tickCount) {

        double score = 0;


        // =============================================
        // GAP - 50 POINTS
        // =============================================

        if (gapPct >= 5.0) {

            score += 50;

        } else if (gapPct >= 4.0) {

            score += 45;

        } else if (gapPct >= 3.0) {

            score += 35;

        } else if (gapPct >= 2.0) {

            score += 25;

        } else if (gapPct >= 1.0) {

            score += 15;
        }


        // =============================================
        // PRE-OPEN PRICE RANGE - 25 POINTS
        // =============================================

        if (rangePct >= 2.0) {

            score += 25;

        } else if (rangePct >= 1.0) {

            score += 15;

        } else if (rangePct >= 0.5) {

            score += 8;
        }


        // =============================================
        // PRE-OPEN ACTIVITY
        //
        // We don't use absolute volume thresholds
        // here because stocks have very different
        // liquidity.
        //
        // Tick count is only a weak activity signal.
        // =============================================

        if (tickCount >= 100) {

            score += 15;

        } else if (tickCount >= 50) {

            score += 10;

        } else if (tickCount >= 20) {

            score += 5;
        }


        // =============================================
        // ACTIVITY EXISTS
        // =============================================

        if (preOpenVolume > 0) {
            score += 10;
        }


        return Math.min(100, score);
    }


    // =========================================================
    // COMPLETED MINUTE
    // =========================================================

    public static void processCompleteMinuteNew(String symbol, MinuteCandle candle) {
        // =====================================================
        // INTRADAY BREAKOUT ENGINE
        // =====================================================
        Deque<MinuteCandle> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>(HISTORY_SIZE));
        MinuteCandle previousCandle = history.peekLast();
        if (previousCandle == null) {
            history.addLast(candle);
            return;
        }

        MinuteCandle prev2Candle = null;

        if (history.size() >= 2) {
            Iterator<MinuteCandle> iterator =
                    history.descendingIterator();

            iterator.next(); // previous candle
            prev2Candle = iterator.next();
        }

        if (previousCandle == null || prev2Candle == null) {
            history.addLast(candle);
            return;
        }

        double prevPriceIncreasePercent =
                ((previousCandle.close - prev2Candle.close)
                        / prev2Candle.close) * 100.0;

        // =====================================================
        // 5. VOLUME MOMENTUM
        // =====================================================

        double prevVolumeRatio =
                (double) previousCandle.volume
                        / prev2Candle.volume;

        double currentVolumeRatio =
                (double) candle.volume
                        / previousCandle.volume;
        FundamentalData fundamentalData = fundamentalDataMap.get(symbol);
        double currentBS = candle.buySellRatio;

        double previousBS = previousCandle.buySellRatio;

        boolean ratioIncreasing = currentBS > previousBS;
        double body = Math.abs(candle.close - candle.open);

        double upperWick = candle.high - Math.max(candle.open, candle.close);

        double lowerWick = Math.min(candle.open, candle.close) - candle.low;


        boolean bullish = candle.close > candle.open;

        boolean previousBullish = previousCandle.close > previousCandle.open;


        // =====================================================
        // PERCENTAGE BASED CANDLE MEASUREMENT
        // =====================================================
        double range = candle.high - candle.low;

        if (range <= 0) {
            return;
        }

        double bodyPercentOfPrice = body * 100.0 / candle.open;

        double rangePercentOfPrice = range * 100.0 / candle.open;

        double bodyPercentOfRange = body * 100.0 / range;

        double upperWickPercent = upperWick * 100.0 / range;

        double lowerWickPercent = lowerWick * 100.0 / range;


        // =====================================================
        // WHERE DID CANDLE CLOSE?
        //
        // 1.0 = high
        // 0.0 = low
        // =====================================================

        double closePosition = (candle.close - candle.low) / range;

        double closeNearHighPercent = closePosition * 100.0;


        // =====================================================
        // 1. PRICE STRUCTURE
        // =====================================================

        boolean higherClose = candle.close > previousCandle.close;

        boolean previousHigherClose = previousCandle.close > prev2Candle.close;

        boolean threeCandleHigherClose = higherClose && previousHigherClose;


        double priceIncreasePercent = previousCandle.close > 0 ? ((candle.close - previousCandle.close) * 100.0 / previousCandle.close) : 0;


        // =====================================================
        // 2. BUY/SELL PRESSURE
        // =====================================================

        double prev2BS = prev2Candle.buySellRatio;

        boolean ratioPreviouslyIncreasing = previousBS >= prev2BS;


        double ratioIncreasePercent = 0.0;

        if (previousBS > 0) {

            ratioIncreasePercent = ((currentBS - previousBS) * 100.0) / previousBS;
        }


        boolean meaningfulBuyingIncrease = ratioIncreasePercent >= 10.0;


        boolean strongBuying = currentBS >= 1.5;


        boolean veryStrongBuying = currentBS >= 2.0;


        // =====================================================
        // 3. BUY QUANTITY
        // =====================================================

        double buyQtyChange = candle.totalBuyQty - previousCandle.totalBuyQty;

        boolean buyQtyIncreasing = buyQtyChange > 0;


        // =====================================================
        // 4. STRONG CLOSE
        // =====================================================

        boolean strongClose = closeNearHighPercent >= 70;


        boolean veryStrongClose = closeNearHighPercent >= 80;


        // =====================================================
        // 5. WICK CONTROL
        // =====================================================

        boolean controlledUpperWick = upperWickPercent <= 25;


        boolean veryControlledUpperWick = upperWickPercent <= 15;


        // =====================================================
        // 6. STRONG BODY
        // =====================================================

        boolean strongBody = bodyPercentOfPrice >= 0.15;


        boolean veryStrongBody = bodyPercentOfRange >= 60;


        // =====================================================
        // 7. CANDLE PATTERNS
        // =====================================================

        boolean strongBullishBreakout = bullish && bodyPercentOfRange >= 55 && closeNearHighPercent >= 70 && upperWickPercent <= 25;


        boolean bullishEngulfing = previousCandle.close < previousCandle.open && bullish && candle.open <= previousCandle.close && candle.close >= previousCandle.open;


        boolean hammer = bullish && lowerWickPercent >= 40 && upperWickPercent <= 20 && bodyPercentOfRange >= 20 && closeNearHighPercent >= 65;


        boolean bullishMarubozu = bullish && bodyPercentOfRange >= 75 && upperWickPercent <= 10 && lowerWickPercent <= 10;

        String pattern = null;

        if (strongBullishBreakout) {

            pattern = "STRONG_BREAKOUT";

        } else if (bullishMarubozu) {

            pattern = "MARUBOZU";

        } else if (bullishEngulfing) {

            pattern = "BULLISH_ENGULFING";

        } else if (hammer) {

            pattern = "HAMMER";
        }

        if(candle.volume >5000 && ratioIncreasing && previousCandle.volume>0 && prev2Candle.volume>0 && priceIncreasePercent > 1 && fundamentalData != null && fundamentalData.getMarketCap()>2000 && prevVolumeRatio>10){
            System.out.println(    candle.time + " | "
                    + pattern + " | "
                    + symbol + " | "
                    + previousCandle.close + " | " + candle.close + " | "
                    + prev2Candle.volume + " | " +
                    previousCandle.volume + " | " +
                    candle.volume + " |  prevVolumeRatio = " +  prevVolumeRatio);
        }


    }

    private static void processCompletedMinute(String symbol, MinuteCandle candle, double vwap) {

        Deque<MinuteCandle> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>(HISTORY_SIZE));

        MinuteCandle previousCandle = history.peekLast();

        // Need previous candle for comparison
        if (previousCandle == null) {
            history.addLast(candle);
            return;
        }

        StockDailyFeature feature = stockDailyFeatureMap.get(symbol);
        PreviousDayData previousDayData = previousDayDataMap.get(symbol);
        FundamentalData fundamentalData = fundamentalDataMap.get(symbol);
        boolean fundamentalOK = fundamentalData != null && fundamentalData.getMarketCap() > 1500 && fundamentalData.getPeRatio() < 50;

        if (feature == null) {
            history.addLast(candle);
            return;
        }

        // =====================================================
        // BASIC CANDLE DATA
        // =====================================================

        double range = candle.high - candle.low;

        if (range <= 0) {
            history.addLast(candle);
            return;
        }
        double body = Math.abs(candle.close - candle.open);

        double upperWick = candle.high - Math.max(candle.open, candle.close);

        double lowerWick = Math.min(candle.open, candle.close) - candle.low;

        boolean bullish = candle.close > candle.open;

        boolean bearish = candle.close < candle.open;

        double bodyPercent = body * 100.0 / range;

        double upperWickPercent = upperWick * 100.0 / range;

        double lowerWickPercent = lowerWick * 100.0 / range;

        // Where did candle close inside its range?
        // 1.0 = exactly at high
        // 0.0 = exactly at low
        double closePosition = (candle.close - candle.low) / range;

        double closeNearHighPercent = closePosition * 100.0;

        // =====================================================
        // DAILY SUPPORT / RESISTANCE
        // =====================================================

        double support = feature.getSupportPrice();

        double resistance = feature.getResistancePrice();


        if (support <= 0 || resistance <= 0) {
            history.addLast(candle);
            return;
        }

        if (support >= resistance) {
            history.addLast(candle);
            return;
        }

        double distanceToSupport = (candle.close - support) * 100.0 / candle.close;

        double distanceToResistance = (resistance - candle.close) * 100.0 / candle.close;

        boolean nearSupport = distanceToSupport >= 0 && distanceToSupport <= 1.0;

        boolean nearResistance = distanceToResistance >= 0 && distanceToResistance <= 1.0;

        // =====================================================
        // FUNDAMENTAL DATA
        // =====================================================


        // =====================================================
        // BUY/SELL PRESSURE
        // =====================================================

        double ratioChange = candle.buySellRatio - previousCandle.buySellRatio;

        double buyQtyChange = candle.totalBuyQty - previousCandle.totalBuyQty;

        double priceChange = candle.close - previousCandle.close;

        boolean priceIncreasing = priceChange > 0;

        boolean buyQtyIncreasing = buyQtyChange > 0;

        boolean ratioIncreasing = ratioChange > 0;

        boolean buyingPressure = candle.buySellRatio >= 1.2 && ratioIncreasing;

        boolean strongBuying = candle.buySellRatio >= 1.5 && ratioIncreasing && buyQtyIncreasing;


        // =====================================================
        // CANDLE QUALITY
        // =====================================================

        boolean closeNearHigh = closeNearHighPercent >= 70;

        // =====================================================
        // 1. STRONG BULLISH BREAKOUT CANDLE
        // =====================================================

        boolean strongBullishBreakout = bullish && bodyPercent >= 55 && closeNearHigh && upperWickPercent <= 25;

        // =====================================================
        // 2. BULLISH ENGULFING
        // =====================================================

        boolean bullishEngulfing = previousCandle.close < previousCandle.open && bullish && candle.open <= previousCandle.close && candle.close >= previousCandle.open;

        // =====================================================
        // 3. HAMMER
        // =====================================================

        boolean hammer = bullish && lowerWickPercent >= 40 && upperWickPercent <= 20 && bodyPercent >= 20 && closeNearHighPercent >= 65;

        // =====================================================
        // 4. BULLISH MARUBOZU
        // =====================================================

        boolean bullishMarubozu = bullish && bodyPercent >= 75 && upperWickPercent <= 10 && lowerWickPercent <= 10;

        // =====================================================
        // 5. INSIDE BAR BREAKOUT
        // =====================================================

        boolean insideBarBreakout = false;

        if (history.size() >= 2) {

            MinuteCandle previousPrevious = getPreviousCandle(history);

            if (previousPrevious != null) {

                boolean insideBar = previousCandle.high <= previousPrevious.high && previousCandle.low >= previousPrevious.low;

                boolean breaksInsideBarHigh = candle.close > previousCandle.high;

                insideBarBreakout = insideBar && breaksInsideBarHigh && bullish;
            }
        }

        // =====================================================
        // 6. BREAKOUT ABOVE DAILY RESISTANCE
        // =====================================================

        double breakoutPercent = (candle.close - resistance) * 100.0 / resistance;

        boolean resistanceBreakout = candle.close > resistance && breakoutPercent >= 0.10;

        // =====================================================
        // 7. RETEST OF RESISTANCE
        // =====================================================

        boolean retestSupport = previousCandle.close > resistance && candle.low <= resistance && candle.close > resistance && bullish;

        // =====================================================
        // 8. CANDLE PATTERN CONFIRMATION
        // =====================================================

        boolean bullishPattern = strongBullishBreakout || bullishEngulfing || hammer || bullishMarubozu || insideBarBreakout;

        // =====================================================
        // SUPPORT ENTRY
        // =====================================================

        boolean supportCandidate = nearSupport && distanceToResistance > 5.0 && buyingPressure && fundamentalOK;

        boolean supportEntry = supportCandidate && priceIncreasing && buyQtyIncreasing && strongBuying && bullish && bullishPattern;

        // =====================================================
        // BREAKOUT ENTRY
        // =====================================================

        boolean breakoutEntry = resistanceBreakout && strongBuying && bullishPattern;

        // =====================================================
        // RETEST ENTRY
        // =====================================================

        boolean retestEntry = retestSupport && buyingPressure && bullishPattern;

        // =====================================================
        // FINAL ENTRY
        // =====================================================

        boolean entrySignal = supportEntry || breakoutEntry || retestEntry;

        // =====================================================
        // PRINT SIGNAL
        // =====================================================


        if (entrySignal) {

            double entry = candle.close;

            double initialStopLoss = entry * 0.98;

            double initialTarget = entry * 1.03;

            double risk = entry - initialStopLoss;

            double reward = initialTarget - entry;

            if (risk <= 0 || reward <= 0) {
                history.addLast(candle);
                return;
            }

            double riskReward = reward / risk;

            String pattern;

            if (strongBullishBreakout) {
                pattern = "STRONG_BREAKOUT";

            } else if (bullishMarubozu) {
                pattern = "MARUBOZU";

            } else if (bullishEngulfing) {
                pattern = "BULLISH_ENGULFING";

            } else if (insideBarBreakout) {
                pattern = "INSIDE_BAR_BREAKOUT";

            } else if (hammer) {
                pattern = "HAMMER";

            } else if (retestEntry) {
                pattern = "RETEST";

            } else {
                pattern = "BULLISH";
            }
            String telegramMessage = String.format(
                    "🚨 BUY SIGNAL | %s | %s | " + "Pattern=%s | " + "Price=%.2f | " + "Entry=%.2f | " + "SL=%.2f | " + "Target=%.2f | " + "RR=1:%.2f | " + "Support=%.2f (%.2f%%) | " + "Resistance=%.2f (%.2f%%) | " + "B/S=%.2f | " + "RatioChange=%.2f | " + "Body=%.1f%% | " + "CloseHigh=%.1f%%",

                    candle.time, symbol, pattern,

                    candle.close, entry, initialStopLoss, initialTarget, riskReward,

                    support, distanceToSupport,

                    resistance, distanceToResistance,

                    candle.buySellRatio, ratioChange,

                    bodyPercent, closeNearHighPercent);
            System.out.println(telegramMessage);
            TelegramAlertService.send(telegramMessage);
        }

        // =====================================================
        // STORE CANDLE
        // =====================================================

        history.addLast(candle);

        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }

        if (history.isEmpty()) {

            return;
        }


        evaluateMomentum(symbol, history);
    }

    public static void getCalculationPrevDayHighBreak(String symbol, MinuteCandle candle) {
        Deque<MinuteCandle> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>(HISTORY_SIZE));


        PreviousDayData prev = previousDayDataMap.get(symbol);
        StockDailyFeature feature = stockDailyFeatureMap.get(symbol);

        if (prev == null || feature == null || history == null || history.isEmpty()) {
            return;
        }

        if (prev.getClosePrice() < 500 || prev.getClosePrice() > 3000) {
            return;
        }

        double previousDayHigh = prev.getHighPrice();

        if (previousDayHigh <= 0) {
            return;
        }


        MinuteCandle previousCandle = history.peekLast();

        MinuteCandle prev2Candle = history.peekLast();


        // =====================================================
        // BASIC CANDLE DATA
        // =====================================================

        double range = candle.high - candle.low;

        if (range <= 0) {
            return;
        }

        double body = Math.abs(candle.close - candle.open);

        double upperWick = candle.high - Math.max(candle.open, candle.close);

        double lowerWick = Math.min(candle.open, candle.close) - candle.low;


        boolean bullish = candle.close > candle.open;

        boolean previousBullish = previousCandle.close > previousCandle.open;


        // =====================================================
        // PERCENTAGE BASED CANDLE MEASUREMENT
        // =====================================================

        double bodyPercentOfPrice = body * 100.0 / candle.open;

        double rangePercentOfPrice = range * 100.0 / candle.open;

        double bodyPercentOfRange = body * 100.0 / range;

        double upperWickPercent = upperWick * 100.0 / range;

        double lowerWickPercent = lowerWick * 100.0 / range;


        // =====================================================
        // WHERE DID CANDLE CLOSE?
        //
        // 1.0 = high
        // 0.0 = low
        // =====================================================

        double closePosition = (candle.close - candle.low) / range;

        double closeNearHighPercent = closePosition * 100.0;


        // =====================================================
        // 1. PRICE STRUCTURE
        // =====================================================

        boolean higherClose = candle.close > previousCandle.close;

        boolean previousHigherClose = previousCandle.close > prev2Candle.close;

        boolean threeCandleHigherClose = higherClose && previousHigherClose;


        double priceIncreasePercent = previousCandle.close > 0 ? ((candle.close - previousCandle.close) * 100.0 / previousCandle.close) : 0;


        // =====================================================
        // 2. BUY/SELL PRESSURE
        // =====================================================

        double currentBS = candle.buySellRatio;

        double previousBS = previousCandle.buySellRatio;

        double prev2BS = prev2Candle.buySellRatio;


        boolean ratioIncreasing = currentBS > previousBS;

        boolean ratioPreviouslyIncreasing = previousBS >= prev2BS;


        double ratioIncreasePercent = 0.0;

        if (previousBS > 0) {

            ratioIncreasePercent = ((currentBS - previousBS) * 100.0) / previousBS;
        }


        boolean meaningfulBuyingIncrease = ratioIncreasePercent >= 10.0;


        boolean strongBuying = currentBS >= 1.5;


        boolean veryStrongBuying = currentBS >= 2.0;


        // =====================================================
        // 3. BUY QUANTITY
        // =====================================================

        double buyQtyChange = candle.totalBuyQty - previousCandle.totalBuyQty;

        boolean buyQtyIncreasing = buyQtyChange > 0;


        // =====================================================
        // 4. STRONG CLOSE
        // =====================================================

        boolean strongClose = closeNearHighPercent >= 70;


        boolean veryStrongClose = closeNearHighPercent >= 80;


        // =====================================================
        // 5. WICK CONTROL
        // =====================================================

        boolean controlledUpperWick = upperWickPercent <= 25;


        boolean veryControlledUpperWick = upperWickPercent <= 15;


        // =====================================================
        // 6. STRONG BODY
        // =====================================================

        boolean strongBody = bodyPercentOfPrice >= 0.15;


        boolean veryStrongBody = bodyPercentOfRange >= 60;


        // =====================================================
        // 7. CANDLE PATTERNS
        // =====================================================

        boolean strongBullishBreakout = bullish && bodyPercentOfRange >= 55 && closeNearHighPercent >= 70 && upperWickPercent <= 25;


        boolean bullishEngulfing = previousCandle.close < previousCandle.open && bullish && candle.open <= previousCandle.close && candle.close >= previousCandle.open;


        boolean hammer = bullish && lowerWickPercent >= 40 && upperWickPercent <= 20 && bodyPercentOfRange >= 20 && closeNearHighPercent >= 65;


        boolean bullishMarubozu = bullish && bodyPercentOfRange >= 75 && upperWickPercent <= 10 && lowerWickPercent <= 10;


        // =====================================================
        // 8. PREVIOUS DAY HIGH BREAKOUT
        // =====================================================

        boolean breakout = candle.close > previousDayHigh;


        boolean previousAlreadyAbovePDH = previousCandle.close > previousDayHigh;


        // First candle breaking PDH
        boolean freshBreakout = breakout && !previousAlreadyAbovePDH;


        // =====================================================
        // 9. HOW FAR ABOVE PDH?
        //
        // Avoid entering after a huge extension.
        // =====================================================

        double distanceAbovePDH = (candle.close - previousDayHigh) * 100.0 / previousDayHigh;


        boolean notOverExtended = distanceAbovePDH <= Math.max(2, 1.0);


        // =====================================================
        // 10. DAILY SUPPORT / RESISTANCE
        // =====================================================

        double support = feature.getSupportPrice();

        double resistance = feature.getResistancePrice();


        double distanceToSupport = support > 0 ? (candle.close - support) * 100.0 / candle.close : 999;


        double distanceToResistance = resistance > 0 ? (resistance - candle.close) * 100.0 / candle.close : 999;


        boolean nearResistance = distanceToResistance >= 0 && distanceToResistance <= 1.0;


        // =====================================================
        // 11. FUNDAMENTAL FILTER
        // =====================================================

        FundamentalData fundamentalData = fundamentalDataMap.get(symbol);


        boolean fundamentalOK = fundamentalData != null && fundamentalData.getMarketCap() > 1500 && fundamentalData.getPeRatio() < 50;


        // =====================================================
        // 12. SCORE
        // =====================================================

        int score = 0;


        // ---- PRICE STRUCTURE ----

        if (higherClose) {
            score += 1;
        }

        if (threeCandleHigherClose) {
            score += 2;
        }


        // ---- BREAKOUT ----

        if (freshBreakout) {
            score += 2;
        } else if (breakout) {
            score += 1;
        }


        // ---- BUYING PRESSURE ----

        if (ratioIncreasing) {
            score += 1;
        }

        if (meaningfulBuyingIncrease) {
            score += 2;
        }

        if (strongBuying) {
            score += 1;
        }

        if (veryStrongBuying) {
            score += 1;
        }


        // ---- BUY QUANTITY ----

        if (buyQtyIncreasing) {
            score += 1;
        }


        // ---- CANDLE QUALITY ----

        if (strongBody) {
            score += 1;
        }

        if (veryStrongBody) {
            score += 1;
        }

        if (strongClose) {
            score += 1;
        }

        if (veryStrongClose) {
            score += 1;
        }

        if (controlledUpperWick) {
            score += 1;
        }


        // ---- PATTERN ----

        if (strongBullishBreakout) {
            score += 2;
        } else if (bullishMarubozu) {
            score += 2;
        } else if (bullishEngulfing) {
            score += 1;
        } else if (hammer) {
            score += 1;
        }


        // ---- FUNDAMENTALS ----

        if (fundamentalOK) {
            score += 1;
        }


        // =====================================================
        // 13. HARD FILTERS
        // =====================================================

        boolean priceConfirmation = higherClose && threeCandleHigherClose;


        boolean buyingConfirmation = ratioIncreasing && meaningfulBuyingIncrease && strongBuying;


        boolean candleConfirmation = bullish && strongClose && controlledUpperWick;


        boolean validSetup = breakout && priceConfirmation && buyingConfirmation && candleConfirmation && notOverExtended && score >= 10;


        // =====================================================
        // PATTERN LOG
        // =====================================================

        String pattern = null;

        if (strongBullishBreakout) {

            pattern = "STRONG_BREAKOUT";

        } else if (bullishMarubozu) {

            pattern = "MARUBOZU";

        } else if (bullishEngulfing) {

            pattern = "BULLISH_ENGULFING";

        } else if (hammer) {

            pattern = "HAMMER";
        }


        if (pattern != null && currentBS > 2.5 && feature.getSupportPrice() > 0 && candle.getClose() > candle.getOpen() && candle.getClose() > feature.getSupportPrice() && ratioIncreasing && candle.close > prev2Candle.high && candle.close > previousCandle.high && (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && score > 10)) {
            System.out.println(
                    candle.time + " | " + symbol + " | Pattern=" + pattern + " | Score=" + score + " | Open=" + String.format(
                            "%.2f", candle.getOpen()) + " | High=" + String.format("%.2f",
                            candle.getHigh()) + " | Close=" + String.format("%.2f",
                            candle.getClose()) + " | Low=" + String.format("%.2f",
                            candle.getLow()) + " | Support=" + String.format("%.2f",
                            feature.getSupportPrice()) + " | DistanceToSupport=" + String.format("%.2f",
                            distanceToSupport) + " | Resistance=" + String.format("%.2f",
                            feature.getResistancePrice()) + " | B/S=" + String.format("%.2f",
                            currentBS) + " | PrevDayHigh=" + String.format("%.2f", previousDayHigh));
        }


        // =====================================================
        // BUY SIGNAL
        // =====================================================

        if (validSetup) {

            double entry = candle.close;


            // =================================================
            // STOP LOSS
            // =================================================

            double initialStopLoss = entry * (1.0 - 3 / 100.0);


            if (initialStopLoss <= 0 || initialStopLoss >= entry) {

                return;
            }


            // =================================================
            // TARGET
            // =================================================

            double initialTarget = entry * (1.0 + 3 / 100.0);


            double risk = entry - initialStopLoss;


            double reward = initialTarget - entry;


            double riskReward = risk > 0 ? reward / risk : 0;


            // =================================================
            // FINAL SIGNAL
            // =================================================

            System.out.println(String.format(
                    "🚨🚨 STRONG BUY SIGNAL 🚨🚨 " + "| %s | %s | %s " + "Score=%d | " + "PDH=%.2f | " + "Entry=%.2f | " + "PDH Break=+%.2f%% | " + "PrevClose=%.2f -> %.2f | " + "B/S %.2f -> %.2f (+%.2f%%) | " + "Body=%.2f%% | " + "CloseHigh=%.2f%% | " + "SL=%.2f | " + "Target=%.2f | " + "RR=1:%.2f | " + "Support=%.2f (%.2f%%) | " + "Resistance=%.2f (%.2f%%)",

                    candle.time, symbol, pattern,

                    score,

                    previousDayHigh, entry,

                    distanceAbovePDH,

                    previousCandle.close, candle.close,

                    previousBS, currentBS, ratioIncreasePercent,

                    bodyPercentOfPrice, closeNearHighPercent,

                    initialStopLoss, initialTarget,

                    riskReward,

                    support, distanceToSupport,

                    resistance, distanceToResistance));

        }
    }

    private static MinuteCandle getPreviousCandle(Deque<MinuteCandle> history) {

        if (history.size() < 2) {
            return null;
        }

        Iterator<MinuteCandle> iterator = history.descendingIterator();

        iterator.next(); // current previous candle

        return iterator.next(); // candle before it
    }


    // =========================================================
    // MOMENTUM ENGINE
    // =========================================================

    private static void evaluateMomentum(String symbol, Deque<MinuteCandle> history) {
        identifyPreBreakout(symbol, history);

        MinuteCandle c0 = get(history, 0);
        MinuteCandle c1 = get(history, 1);
        MinuteCandle c2 = get(history, 2);
        MinuteCandle c3 = get(history, 3);
        MinuteCandle c5 = get(history, 5);

        if (previousDayDataMap.get(symbol) != null && !symbols.contains(symbol) && c0!=null) {

            double prevDayClose = previousDayDataMap.get(symbol).getClosePrice();
            double currentClose = c0.close;

            if (prevDayClose > 0) {

                double changePercent =
                    ((currentClose - prevDayClose) / prevDayClose) * 100.0;

                if (Math.abs(changePercent) > 3.0 && c0.endVolume>10000) {

                    String changeText;

                    if (changePercent > 0) {
                        changeText = "🟢 +" + String.format("%.2f", changePercent) + "%";
                    } else if (changePercent < 0) {
                        changeText = "🔴 " + String.format("%.2f", changePercent) + "%";
                    } else {
                        changeText = "⚪ 0.00%";
                    }

                   /* TelegramAlertService.send(
                        c0.time + " " + symbol
                            + " prev close=" + prevDayClose
                            + " curr close=" + currentClose
                            + " change=" + changeText
                    );*/
                    System.out.println(c0.time + " " + symbol
                        + " prev close=" + prevDayClose
                        + " curr close=" + currentClose
                        + " change=" + changeText);

                    List<MinuteCandle> candles = getLastNMinute(history, 50);

                    PatternDetector.PatternResult result =
                        PatternDetector.detect(candles);

                    if (result.score > 0) {
                        System.out.println(
                            symbol +
                                " Change=" + String.format("%.2f", changePercent) + "%" +
                                " Pattern -> " + result
                        );
                    }
                    symbols.add(symbol);
                }
            }
        }

        if (c0 == null || c1 == null || c2 == null || c3 == null || c5 == null) {
            return;
        }


        // =====================================================
        // PRICE MOMENTUM
        // =====================================================

        double return1m = percent(c1.close, c0.close);
        double return3m = percent(c3.close, c0.close);
        double return5m = percent(c5.close, c0.close);


        // =====================================================
        // PRICE PERSISTENCE
        // =====================================================

        boolean priceIncreasing = c5.close < c3.close && c3.close < c2.close && c2.close < c1.close && c1.close < c0.close;

        boolean buyQtyIncreasing = c5.totalBuyQty < c3.totalBuyQty && c3.totalBuyQty < c2.totalBuyQty && c2.totalBuyQty < c1.totalBuyQty && c1.totalBuyQty < c0.totalBuyQty;

        boolean sellQtyDecreasing = c5.totalSellQty > c3.totalSellQty && c3.totalSellQty > c2.totalSellQty && c2.totalSellQty > c1.totalSellQty && c1.totalSellQty > c0.totalSellQty;

        if (buyQtyIncreasing && sellQtyDecreasing && c0.close > 50) {
            System.out.println("Symbol " + symbol);
        }

        // =====================================================
        // BUY / SELL RATIO
        // =====================================================

        double ratio0 = c0.buySellRatio;
        double ratio1 = c1.buySellRatio;
        double ratio2 = c2.buySellRatio;

        boolean ratioIncreasing = ratio2 < ratio1 && ratio1 < ratio0;

        double ratioIncrease = ratio1 > 0 ? percent(ratio1, ratio0) : 0;

        double ratioChange1 = ratio1 - ratio2;
        double ratioChange2 = ratio0 - ratio1;

        boolean ratioAccelerating = ratioChange2 > ratioChange1;


        // =====================================================
        // CANDLE STRENGTH
        // =====================================================

        double range = c0.high - c0.low;

        double candleStrength = range > 0 ? (c0.close - c0.low) / range : 0;


        // =====================================================
        // RELATIVE VOLUME
        // =====================================================

        double averageVolume = averagePreviousVolume(history);

        double rvol = averageVolume > 0 ? c0.volume / averageVolume : 0;


        // =====================================================
        // RECENT HIGH / BREAKOUT
        // =====================================================

        double recentHigh = previousHigh(history, 5);

        boolean breakout = recentHigh > 0 && c0.close > recentHigh;

        double breakoutPct = recentHigh > 0 ? percent(recentHigh, c0.close) : 0;


        // =====================================================
        // PRE-OPEN SCORE
        // =====================================================

        PreOpenData preOpen = preOpenCandidates.get(symbol);

        double preOpenScore = preOpen != null ? preOpen.score : 0;


        // =====================================================
        // TECHNICAL SCORE = 100
        // =====================================================

        double score = 0;


        // -----------------------------------------------------
        // 1. 1M PRICE MOMENTUM - 10
        // -----------------------------------------------------

        if (return1m >= 0.50) {
            score += 10;
        } else if (return1m >= 0.25) {
            score += 7;
        } else if (return1m > 0) {
            score += 3;
        }


        // -----------------------------------------------------
        // 2. 3M PRICE MOMENTUM - 15
        // -----------------------------------------------------

        if (return3m >= 1.50) {
            score += 15;
        } else if (return3m >= 0.80) {
            score += 10;
        } else if (return3m >= 0.30) {
            score += 6;
        } else if (return3m > 0) {
            score += 3;
        }


        // -----------------------------------------------------
        // 3. 5M PRICE MOMENTUM - 15
        // -----------------------------------------------------

        if (return5m >= 2.00) {
            score += 15;
        } else if (return5m >= 1.00) {
            score += 10;
        } else if (return5m >= 0.50) {
            score += 6;
        } else if (return5m > 0) {
            score += 3;
        }


        // -----------------------------------------------------
        // 4. PRICE PERSISTENCE - 10
        // -----------------------------------------------------

        if (priceIncreasing) {
            score += 10;
        }


        // -----------------------------------------------------
        // 5. BUY / SELL RATIO - 15
        // -----------------------------------------------------

        if (ratio0 >= 5.0) {
            score += 15;
        } else if (ratio0 >= 3.0) {
            score += 11;
        } else if (ratio0 >= 2.0) {
            score += 7;
        } else if (ratio0 >= MIN_BUY_SELL_RATIO) {
            score += 4;
        }


        // -----------------------------------------------------
        // 6. RATIO TREND - 5
        // -----------------------------------------------------

        if (ratioIncreasing) {
            score += 5;
        }


        // -----------------------------------------------------
        // 7. RATIO ACCELERATION - 5
        // -----------------------------------------------------

        if (ratioAccelerating) {
            score += 5;
        }


        // -----------------------------------------------------
        // 8. RVOL - 15
        // -----------------------------------------------------

        if (rvol >= 5.0) {
            score += 15;
        } else if (rvol >= 3.0) {
            score += 12;
        } else if (rvol >= 2.0) {
            score += 8;
        } else if (rvol >= MIN_RVOL) {
            score += 5;
        }


        // -----------------------------------------------------
        // 9. CANDLE STRENGTH - 5
        // -----------------------------------------------------

        if (candleStrength >= 0.85) {
            score += 5;
        } else if (candleStrength >= 0.75) {
            score += 4;
        } else if (candleStrength >= 0.65) {
            score += 2;
        }


        // -----------------------------------------------------
        // 10. BREAKOUT - 5
        // -----------------------------------------------------

        if (breakout) {
            score += 5;
        }


        // =====================================================
        // SHORT-TERM PROBABILITY SCORE = 100
        //
        // This is specifically about continuation potential.
        // =====================================================

        double probabilityScore = 0;


        // -----------------------------------------------------
        // PRICE MOMENTUM - 20
        // -----------------------------------------------------

        if (return3m >= 1.50) {
            probabilityScore += 10;
        } else if (return3m >= 0.80) {
            probabilityScore += 7;
        } else if (return3m >= 0.50) {
            probabilityScore += 4;
        }

        if (return5m >= 2.00) {
            probabilityScore += 10;
        } else if (return5m >= 1.00) {
            probabilityScore += 7;
        } else if (return5m >= 0.50) {
            probabilityScore += 4;
        }


        // -----------------------------------------------------
        // BUYING PRESSURE - 20
        // -----------------------------------------------------

        if (ratio0 >= 5.0) {
            probabilityScore += 15;
        } else if (ratio0 >= 3.0) {
            probabilityScore += 11;
        } else if (ratio0 >= 2.0) {
            probabilityScore += 7;
        }

        if (ratioIncreasing) {
            probabilityScore += 3;
        }

        if (ratioAccelerating) {
            probabilityScore += 2;
        }


        // -----------------------------------------------------
        // VOLUME CONFIRMATION - 15
        // -----------------------------------------------------

        if (rvol >= 5.0) {
            probabilityScore += 15;
        } else if (rvol >= 3.0) {
            probabilityScore += 12;
        } else if (rvol >= 2.0) {
            probabilityScore += 8;
        } else if (rvol >= 1.5) {
            probabilityScore += 4;
        }


        // -----------------------------------------------------
        // CANDLE QUALITY - 10
        // -----------------------------------------------------

        if (candleStrength >= 0.85) {
            probabilityScore += 10;
        } else if (candleStrength >= 0.75) {
            probabilityScore += 7;
        } else if (candleStrength >= 0.65) {
            probabilityScore += 4;
        }


        // -----------------------------------------------------
        // BREAKOUT QUALITY - 15
        // -----------------------------------------------------

        boolean validBreakout = breakout && breakoutPct >= 0.20 && breakoutPct <= 1.50;

        if (validBreakout) {
            probabilityScore += 15;
        } else if (breakout) {
            probabilityScore += 5;
        }


        // -----------------------------------------------------
        // PRE-OPEN - 5
        // -----------------------------------------------------

        if (preOpenScore >= 80) {
            probabilityScore += 5;
        } else if (preOpenScore >= 60) {
            probabilityScore += 3;
        }


        // -----------------------------------------------------
        // PRICE PERSISTENCE - 5
        // -----------------------------------------------------

        if (priceIncreasing) {
            probabilityScore += 5;
        }


        // =====================================================
        // AVOID CHASING
        // =====================================================

        boolean notChasing = return1m < 3.0 && return3m < 5.0 && return5m < 8.0;


        // =====================================================
        // FINAL TRADABLE CONDITIONS
        // =====================================================

        boolean tradable = score >= 80 && probabilityScore >= 75 &&

                return3m >= 0.30 && return5m >= 0.50 &&

                ratio0 >= 2.5 && ratioIncreasing &&

                rvol >= 2.0 &&

                candleStrength >= 0.70 &&

                validBreakout &&

                notChasing;


        // =====================================================
        // HIGH QUALITY BUY
        // =====================================================

        boolean highQuality = score >= 85 && probabilityScore >= 85 &&

                return3m >= 0.70 && return5m >= 1.00 &&

                ratio0 >= 4.0 && ratioIncreasing && ratioAccelerating &&

                rvol >= 3.0 &&

                candleStrength >= 0.80 &&

                breakout && breakoutPct >= 0.30 && breakoutPct <= 1.25 &&

                return1m < 2.50;


        // =====================================================
        // ONLY BUY STOCKS ARE ALERTED
        // =====================================================

        TradeSignal signal;
        String reason;

        if (highQuality) {

            signal = TradeSignal.BUY;
            reason = "HIGH PROBABILITY MOMENTUM";

        } else if (tradable) {

            signal = TradeSignal.BUY;
            reason = "TRADABLE MOMENTUM + BREAKOUT";

        } else {

            // WATCH / weak signals are completely ignored.
            return;
        }


        // =====================================================
        // DUPLICATE / COOLDOWN
        // =====================================================

        LocalDateTime lastSignal = lastSignalTime.get(symbol);

        if (lastSignal != null) {

            long minutes = Duration.between(lastSignal, c0.time).toMinutes();

            if (minutes < 10) {
                return;
            }
        }

        lastSignalTime.put(symbol, c0.time);


        // =====================================================
        // TELEGRAM MESSAGE
        // =====================================================

        String telegramMessage = String.format("""
                        
                        🟢 *TRADABLE BUY*
                        
                        📌 *%s*
                        
                        💰 Price: `%.2f`
                        
                        ⭐ Technical Score: `%.0f/100`
                        🎯 Continuation Score: `%.0f/100`
                        
                        📈 1m: `%+.2f%%`
                        📈 3m: `%+.2f%%`
                        📈 5m: `%+.2f%%`
                        
                        🟢 B/S Ratio: `%.2f`
                        📊 Ratio Increase: `%+.2f%%`
                        ⚡ Ratio Acceleration: `%s`
                        
                        🔊 RVOL: `%.2fx`
                        🕯 Candle Strength: `%.2f`
                        
                        🚀 Breakout: `YES`
                        📐 Breakout: `%+.2f%%`
                        
                        🌅 PreOpen: `%.0f`
                        
                        🎯 *%s*
                        
                        """,

                symbol, c0.close,

                score, probabilityScore,

                return1m, return3m, return5m,

                ratio0, ratioIncrease, ratioAccelerating ? "YES" : "NO",

                rvol, candleStrength,

                breakoutPct, preOpenScore,

                reason);


        // =====================================================
        // TELEGRAM
        // =====================================================

        TelegramAlertService.send(telegramMessage);


        // =====================================================
        // CONSOLE
        // =====================================================

        System.out.printf("""
                        
                        ==========================================
                        🟢 TRADABLE BUY
                        ==========================================
                        Time              : %s
                        Symbol            : %s
                        Price             : %.2f
                        
                        Technical Score   : %.1f / 100
                        Probability Score : %.1f / 100
                        
                        1m Return         : %+.2f%%
                        3m Return         : %+.2f%%
                        5m Return         : %+.2f%%
                        
                        B/S Ratio         : %.2f
                        Ratio Increase    : %+.2f%%
                        Ratio Accelerating: %s
                        
                        RVOL              : %.2fx
                        Candle Strength   : %.2f
                        
                        Breakout          : %s
                        Breakout %%        : %+.2f%%
                        
                        PreOpen Score     : %.1f
                        
                        Reason            : %s
                        ==========================================
                        
                        """,

                c0.time, symbol, c0.close,

                score, probabilityScore,

                return1m, return3m, return5m,

                ratio0, ratioIncrease, ratioAccelerating ? "YES" : "NO",

                rvol, candleStrength,

                breakout ? "YES" : "NO", breakoutPct,

                preOpenScore,

                reason);
    }

    private static String buildTelegramMessage(TradeSignal signal, LocalDateTime time, String symbol, double score, double price, double return1m, double return3m, double return5m, double ratio0, double ratioIncrease, boolean ratioAccelerating, double rvol, double candleStrength, boolean breakout, double breakoutPct, double preOpenScore, String reason) {

        String emoji;

        if (signal == TradeSignal.BUY) {
            emoji = "🟢";
        } else if (signal == TradeSignal.WATCH) {
            emoji = "🟡";
        } else {
            emoji = "⚪";
        }

        return String.format("""
                        
                        %s *%s*
                        
                        📊 *Momentum Signal*
                        
                        🕐 Time: `%s`
                        💰 Price: `%.2f`
                        ⭐ Score: `%.1f/100`
                        
                        📈 *Momentum*
                        1m: `%+.2f%%`
                        3m: `%+.2f%%`
                        5m: `%+.2f%%`
                        
                        🟢 B/S Ratio: `%.2f`
                        📈 Ratio Increase: `%+.2f%%`
                        ⚡ Ratio Accel: `%s`
                        
                        🔊 RVOL: `%.2fx`
                        🕯 Candle Strength: `%.2f`
                        
                        🚀 Breakout: `%s`
                        Breakout: `%+.2f%%`
                        
                        🌅 PreOpen Score: `%.1f`
                        
                        🎯 *Action: %s*
                        📝 Reason: `%s`
                        
                        """, emoji, symbol, time, price, score, return1m, return3m, return5m, ratio0, ratioIncrease,
                ratioAccelerating ? "YES" : "NO", rvol, candleStrength, breakout ? "YES" : "NO", breakoutPct, preOpenScore,
                signal, reason);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static MinuteCandle get(Deque<MinuteCandle> history, int fromLast) {

        if (history.size() <= fromLast) {

            return null;
        }


        Iterator<MinuteCandle> iterator = history.descendingIterator();


        for (int i = 0; i < fromLast; i++) {

            iterator.next();
        }


        return iterator.next();
    }

    private static List<MinuteCandle> getLastNMinute(Deque<MinuteCandle> history, int fromLast) {

        if (history.size() <= fromLast) {

            return null;
        }
        List<MinuteCandle> list = new ArrayList<>();

        Iterator<MinuteCandle> iterator = history.descendingIterator();


        for (int i = 0; i < fromLast; i++) {

            list.add(iterator.next());
        }

        list.add(iterator.next());
        return list;
    }


    private static double percent(double previous, double current) {

        if (previous == 0) {
            return 0;
        }

        return ((current - previous) / previous) * 100.0;
    }


    private static double averagePreviousVolume(Deque<MinuteCandle> history) {

        if (history.size() <= 1) {
            return 0;
        }


        double total = 0;

        int count = 0;


        Iterator<MinuteCandle> iterator = history.descendingIterator();


        // Skip current candle
        iterator.next();


        while (iterator.hasNext()) {

            MinuteCandle candle = iterator.next();

            total += candle.volume;

            count++;


            if (count >= 5) {
                break;
            }
        }


        return count > 0 ? total / count : 0;
    }


    private static double previousHigh(Deque<MinuteCandle> history, int candlesBack) {

        if (history.size() <= candlesBack) {

            return 0;
        }


        double high = 0;


        Iterator<MinuteCandle> iterator = history.descendingIterator();


        // Skip current
        iterator.next();


        for (int i = 1; i <= candlesBack; i++) {

            if (!iterator.hasNext()) {
                break;
            }


            MinuteCandle candle = iterator.next();


            high = Math.max(high, candle.high);
        }


        return high;
    }


    // =========================================================
    // PRE-OPEN DATA
    // =========================================================

    static class PreOpenState {

        String symbol;

        double previousClose;

        double firstPrice;

        double lastPrice;

        double high;

        double low;

        long startVolume;

        long lastVolume;

        int tickCount;

        LocalDateTime lastTickTime;
    }


    static class PreOpenData {

        String symbol;

        double previousClose;

        double preOpenPrice;

        double gapPct;

        double preOpenHigh;

        double preOpenLow;

        double preOpenRangePct;

        double preOpenVolume;

        int tickCount;

        double score;
    }


    // =========================================================
    // OPTIONAL ACCESSORS
    // =========================================================

    public static Map<String, PreOpenData> getPreOpenCandidates() {

        return Map.copyOf(preOpenCandidates);
    }


    public static void clearForNewDay() {

        candles.clear();

        minuteHistory.clear();

        realtimeState.clear();

        preOpenStates.clear();

        preOpenCandidates.clear();

        lastSignalTime.clear();

        preOpenFinalized = false;

        preOpenDate = null;
    }


    public static BlockingQueue<MinuteCandle> getSaveQueue() {

        return saveQueue;
    }


    // =========================================================
    // IMPORTANT:
    // Keep your existing createNew/update implementation
    // if MinuteCandle has additional fields.
    // =========================================================

    private static MinuteCandle createNew(String symbol, LocalDateTime minute, Tick tick) {

        MinuteCandle candle = new MinuteCandle();

        candle.symbol = symbol;

        candle.time = minute;

        double price = tick.getLastTradedPrice();

        candle.open = price;

        candle.high = price;

        candle.low = price;

        candle.close = price;

        candle.startVolume = tick.getVolumeTradedToday();

        candle.endVolume = tick.getVolumeTradedToday();

        double buy = tick.getTotalBuyQuantity();

        double sell = tick.getTotalSellQuantity();

        candle.buySellRatio = sell > 0 ? buy / sell : 0;

        return candle;
    }


    private static void update(MinuteCandle candle, Tick tick) {

        double price = tick.getLastTradedPrice();

        candle.close = price;

        candle.high = Math.max(candle.high, price);

        candle.low = Math.min(candle.low, price);

        candle.endVolume = tick.getVolumeTradedToday();


        double buy = tick.getTotalBuyQuantity();

        double sell = tick.getTotalSellQuantity();


        candle.buySellRatio = sell > 0 ? buy / sell : 0;
    }

    static class IntraminuteState {

        public double bestAsk;
        public double bestBid;
        public double depthImbalance;
        String symbol;
        LocalDateTime minute;

        double openPrice;
        double lastPrice;

        long startVolume;
        long lastVolume;

        double buySellRatio;
        double previousBuySellRatio;

        int upTicks;
        int downTicks;

        int signalLevel;

        long tickCount;

        long signalTime;

        long buyVolume;
        long sellVolume;
        long volumeDelta;

        int lastTradeDirection;
    }

    private static void identifyPreBreakout(String symbol, Deque<MinuteCandle> history) {

        if (history == null || history.size() < 6) {
            return;
        }

        MinuteCandle c0 = get(history, 0); // latest
        MinuteCandle c1 = get(history, 1);
        MinuteCandle c2 = get(history, 2);
        MinuteCandle c3 = get(history, 3);
        MinuteCandle c4 = get(history, 4);
        MinuteCandle c5 = get(history, 5);

        if (c0 == null || c1 == null || c2 == null || c3 == null || c4 == null || c5 == null) {
            return;
        }

        if (c0.close <= 0) {
            return;
        }

        // ---------------------------------------------------------
        // 1. BUY / SELL PRESSURE
        // ---------------------------------------------------------

        double buySell0 = (double) c0.totalBuyQty / Math.max(1.0, c0.totalSellQty);

        double buySell1 = (double) c1.totalBuyQty / Math.max(1.0, c1.totalSellQty);

        double buySell2 = (double) c2.totalBuyQty / Math.max(1.0, c2.totalSellQty);

        // Demand improving
        boolean buyRatioImproving = buySell0 > buySell1 && buySell1 >= buySell2;

        // Current demand > supply
        boolean buyersDominating = buySell0 >= 1.10;

        // ---------------------------------------------------------
        // 2. BUY QUANTITY TREND
        // ---------------------------------------------------------

        boolean buyQtyIncreasing = c5.totalBuyQty < c4.totalBuyQty && c4.totalBuyQty < c3.totalBuyQty && c3.totalBuyQty < c2.totalBuyQty && c2.totalBuyQty < c1.totalBuyQty && c1.totalBuyQty < c0.totalBuyQty;

        // More flexible version
        double buyQtyGrowth = ((double) c0.totalBuyQty - c3.totalBuyQty) / Math.max(1.0, c3.totalBuyQty);

        // ---------------------------------------------------------
        // 3. PRICE MUST HOLD
        // ---------------------------------------------------------

        boolean priceHolding = c0.close >= c3.close * 0.998;

        // Don't allow price to already be falling strongly
        double priceChange3 = ((c0.close - c3.close) / c3.close) * 100.0;

        boolean notFalling = priceChange3 > -0.20;

        // ---------------------------------------------------------
        // 4. FIND RECENT HIGH / LOW
        // ---------------------------------------------------------

        double recentHigh = Math.max(c0.high,
                Math.max(c1.high, Math.max(c2.high, Math.max(c3.high, Math.max(c4.high, c5.high)))));

        double recentLow = Math.min(c0.low,
                Math.min(c1.low, Math.min(c2.low, Math.min(c3.low, Math.min(c4.low, c5.low)))));

        double rangePct = ((recentHigh - recentLow) / recentLow) * 100.0;

        // Tight consolidation
        boolean compressed = rangePct <= 1.20;

        // ---------------------------------------------------------
        // 5. DISTANCE FROM BREAKOUT
        // ---------------------------------------------------------

        double distanceToHighPct = ((recentHigh - c0.close) / c0.close) * 100.0;

        boolean nearBreakout = distanceToHighPct <= 0.40;

        // ---------------------------------------------------------
        // 6. VOLUME
        // ---------------------------------------------------------

        double avgVolume = (c1.volume + c2.volume + c3.volume + c4.volume + c5.volume) / 5.0;

        double volumeRatio = c0.volume / Math.max(1.0, avgVolume);

        boolean volumeIncreasing = volumeRatio >= 1.20;

        // ---------------------------------------------------------
        // 7. SCORE
        // ---------------------------------------------------------

        double score = 0;

        if (buyersDominating)
            score += 20;

        if (buyRatioImproving)
            score += 15;

        if (buyQtyIncreasing)
            score += 15;

        else if (buyQtyGrowth > 0.15)
            score += 10;

        if (priceHolding)
            score += 15;

        if (notFalling)
            score += 5;

        if (compressed)
            score += 15;

        if (nearBreakout)
            score += 10;

        if (volumeIncreasing)
            score += 5;

        // ---------------------------------------------------------
        // 8. FINAL FILTER
        // ---------------------------------------------------------

        if (score >= 70 && buyersDominating && priceHolding && compressed && nearBreakout) {

            System.out.printf(
                    "%n🔥 PRE-BREAKOUT | %-12s | score=%4.0f%n" + "   Price       : %.2f%n" + "   Buy/Sell    : %.2f%n" + "   BuyQty      : %d%n" + "   BuyQtyGrowth: %.1f%%%n" + "   VolumeRatio : %.2fx%n" + "   Range       : %.2f%%%n" + "   Dist. High  : %.2f%%%n" + "   Price 3m    : %+.2f%%%n",
                    symbol, score, c0.close, buySell0, c0.totalBuyQty, buyQtyGrowth * 100.0, volumeRatio, rangePct,
                    distanceToHighPct, priceChange3);
        }
    }

    public static class VWAPState {

        double cumulativePV = 0.0;
        double cumulativeVolume = 0.0;
        double vwap = 0.0;

        LocalDate date;
    }
}
