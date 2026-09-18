package org.example.live;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Depth;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnTicks;
import org.example.model.StockMomentum;
import org.example.util.TokenUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class LiveTickerNew {

    // =========================================================
    // KITE
    // =========================================================

    private static KiteTicker liveTicker;

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");


    // =========================================================
    // MARKET TIME
    // =========================================================

    private static final LocalTime START_TIME = LocalTime.of(9, 15);

    private static final LocalTime END_TIME = LocalTime.of(15, 30);


    // =========================================================
    // SCANNER PARAMETERS
    // =========================================================

    /*
     * Previous completed candles used for range.
     */
    private static final int RANGE_LOOKBACK = 10;

    /*
     * Need at least 5 previous candles
     * before calculating 5-minute return.
     */
    private static final int RETURN_LOOKBACK = 5;

    /*
     * Minimum 1-minute price acceleration.
     */
    private static final double MIN_1M_MOVE_PCT = 0.20;

    /*
     * Minimum 5-minute return.
     */
    private static final double MIN_5M_RETURN_PCT = 0.40;

    /*
     * Current volume must be at least 1.5x
     * recent average volume.
     */
    private static final double MIN_VOLUME_RATIO = 1.50;

    /*
     * Minimum breakout above previous range.
     */
    private static final double MIN_BREAKOUT_PCT = 0.10;

    /*
     * Minimum estimated aggressive buying.
     */
    private static final double MIN_BUY_PRESSURE = 55.0;

    /*
     * Minimum bid/ask imbalance.
     */
    private static final double MIN_DEPTH_IMBALANCE = 5.0;

    /*
     * Only show strong candidates.
     */
    private static final double MIN_SCORE = 60.0;


    // =========================================================
    // CURRENT MINUTE
    // =========================================================

    private static final Map<String, MinuteState> minuteStates = new ConcurrentHashMap<>();


    // =========================================================
    // PREVIOUS COMPLETED MINUTES
    // =========================================================

    private static final Map<String, Deque<MinuteState>> minuteHistory = new ConcurrentHashMap<>();


    // =========================================================
    // PREVIOUS TICK
    //
    // Used for aggressive buy/sell estimation.
    // =========================================================

    private static final Map<String, TickFlowState> previousTicks = new ConcurrentHashMap<>();


    // =========================================================
    // CURRENT CANDIDATES
    // =========================================================

    private static final Map<String, RealtimeCandidate> realtimeCandidates = new ConcurrentHashMap<>();

    private static Map<String, StockMomentum> momentumMap = new HashMap<>();

    private static final Map<LocalDateTime, Set<String>> completedMinutes = new ConcurrentHashMap<>();
    private static Set<String> liveUniverse = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<LocalDateTime> lastProcessedMinute = new AtomicReference<>();


    // =========================================================
    // START WEBSOCKET
    // =========================================================

    public static void startWebSocket(KiteConnect kite, java.util.Set<String> tokens, Map<String, StockMomentum> gapUpSymbolClosePriceMap) throws KiteException, Exception {

        Map<Long, String> tokenMap = TokenUtils.getTokens(kite, tokens);
        liveUniverse.addAll(tokens);
        momentumMap = gapUpSymbolClosePriceMap;

        liveTicker = new KiteTicker(kite.getAccessToken(), kite.getApiKey());

        liveTicker.setTryReconnection(true);
        liveTicker.setMaximumRetries(10);
        liveTicker.setMaximumRetryInterval(30);


        // =====================================================
        // CONNECT
        // =====================================================

        liveTicker.setOnConnectedListener(new OnConnect() {

            @Override
            public void onConnected() {

                System.out.println("WebSocket Connected");

                ArrayList<Long> instrumentTokens = new ArrayList<>(tokenMap.keySet());

                liveTicker.subscribe(instrumentTokens);

                /*
                 * FULL mode is required because
                 * we need market depth.
                 */
                liveTicker.setMode(instrumentTokens, KiteTicker.modeFull);
            }
        });


        // =====================================================
        // TICKS
        // =====================================================

        liveTicker.setOnTickerArrivalListener(new OnTicks() {

            @Override
            public void onTicks(ArrayList<Tick> ticks) {

                for (Tick tick : ticks) {

                    try {

                        processTick(tokenMap, tick);

                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
            }
        });


        liveTicker.connect();
    }


    // =========================================================
    // PROCESS EVERY TICK
    // =========================================================

    private static void processTick(Map<Long, String> tokenMap, Tick tick) {

        if (tick == null || tick.getLastTradedTime() == null) {

            return;
        }


        // =====================================================
        // SYMBOL
        // =====================================================

        String symbol = tokenMap.get(tick.getInstrumentToken());

        if (symbol == null) {
            return;
        }


        // =====================================================
        // TIME
        // =====================================================

        LocalDateTime dateTime = tick.getLastTradedTime().toInstant().atZone(MARKET_ZONE).toLocalDateTime();

        LocalTime tickTime = dateTime.toLocalTime();

        if (tickTime.isBefore(START_TIME) || tickTime.isAfter(END_TIME)) {

            return;
        }


        LocalDateTime minute = dateTime.withSecond(0).withNano(0);


        // =====================================================
        // PRICE
        // =====================================================

        double price = tick.getLastTradedPrice();

        if (price <= 0) {
            return;
        }


        // =====================================================
        // CUMULATIVE VOLUME
        // =====================================================

        long volume = tick.getVolumeTradedToday();

        if (volume < 0) {
            return;
        }


        // =====================================================
        // MARKET DEPTH
        // =====================================================

        DepthSnapshot depth = readDepth(tick);


        // =====================================================
        // CURRENT MINUTE
        // =====================================================

        minuteStates.compute(symbol, (key, state) -> {

            // =============================================
            // FIRST MINUTE
            // =============================================

            if (state == null) {
                lastProcessedMinute.compareAndSet(null, minute);
                return createMinuteState(minute, price, volume, depth);
            }


            // =============================================
            // NEW MINUTE
            // =============================================

            if (!state.minute.equals(minute)) {

                LocalDateTime completedMinute = state.minute;

                /*
                 * First symbol entering new minute
                 */
                if (!lastProcessedMinute.compareAndSet(completedMinute, minute)) {

                    /*
                     * Snapshot ALL symbols whose state
                     * belongs to the completed minute.
                     */
                    Map<String, MinuteState> snapshot = new HashMap<>();

                    minuteStates.forEach((symbolName, minuteState) -> {

                        if (minuteState.minute.equals(completedMinute)) {

                            snapshot.put(symbolName, minuteState);
                        }
                    });


                    /*
                     * Process ALL symbols together.
                     */
                    processCompletedMinute(snapshot);
                }

                /*
                 * Start new minute.
                 */
                return createMinuteState(minute, price, volume, depth);
            }


            // =============================================
            // SAME MINUTE
            // =============================================

            state.highPrice = Math.max(state.highPrice, price);

            state.lowPrice = Math.min(state.lowPrice, price);

            state.closePrice = price;

            state.endVolume = volume;

            state.endBidQty = depth.bidQty;

            state.endAskQty = depth.askQty;

            state.lastBid = depth.bidPrice;

            state.lastAsk = depth.askPrice;

            state.tickCount++;


            /*
             * VWAP accumulation.
             */
            TickFlowState previous = previousTicks.get(symbol);

            if (previous != null) {

                long volumeDelta = volume - previous.volume;

                if (volumeDelta > 0) {

                    state.vwapValue += price * volumeDelta;

                    state.vwapVolume += volumeDelta;
                }
            }

            return state;
        });
        // =====================================================
        // ESTIMATE AGGRESSIVE BUY / SELL
        // =====================================================

        updateTradeFlow(symbol, tick, depth);
    }


    // =========================================================
    // CREATE MINUTE STATE
    // =========================================================

    private static MinuteState createMinuteState(LocalDateTime minute, double price, long volume, DepthSnapshot depth) {

        MinuteState state = new MinuteState();

        state.minute = minute;

        state.openPrice = price;

        state.highPrice = price;

        state.lowPrice = price;

        state.closePrice = price;

        state.startVolume = volume;

        state.endVolume = volume;

        state.startBidQty = depth.bidQty;

        state.endBidQty = depth.bidQty;

        state.startAskQty = depth.askQty;

        state.endAskQty = depth.askQty;

        state.lastBid = depth.bidPrice;

        state.lastAsk = depth.askPrice;

        state.tickCount = 1;

        return state;
    }


    // =========================================================
    // READ MARKET DEPTH
    // =========================================================

    private static DepthSnapshot readDepth(Tick tick) {

        DepthSnapshot result = new DepthSnapshot();

        Map<String, ArrayList<Depth>> marketDepth = tick.getMarketDepth();

        if (marketDepth == null) {
            return result;
        }


        ArrayList<Depth> buy = marketDepth.get("buy");

        ArrayList<Depth> sell = marketDepth.get("sell");


        // =====================================================
        // BEST BID
        // =====================================================

        if (buy != null && !buy.isEmpty()) {

            Depth bestBid = buy.getFirst();

            result.bidPrice = bestBid.getPrice();

            result.bidQty = bestBid.getQuantity();
        }


        // =====================================================
        // BEST ASK
        // =====================================================

        if (sell != null && !sell.isEmpty()) {

            Depth bestAsk = sell.getFirst();

            result.askPrice = bestAsk.getPrice();

            result.askQty = bestAsk.getQuantity();
        }


        // =====================================================
        // 5-LEVEL DEPTH
        // =====================================================

        if (buy != null) {

            for (Depth d : buy) {

                if (d != null) {

                    result.totalBidDepth += d.getQuantity();
                }
            }
        }

        if (sell != null) {

            for (Depth d : sell) {

                if (d != null) {

                    result.totalAskDepth += d.getQuantity();
                }
            }
        }

        return result;
    }


    // =========================================================
    // TRADE FLOW
    //
    // IMPORTANT:
    //
    // This is an ESTIMATE.
    //
    // Volume itself is NOT buy/sell volume.
    //
    // We classify incremental traded quantity based on
    // LTP relative to best bid / best ask.
    // =========================================================

    private static void updateTradeFlow(String symbol, Tick tick, DepthSnapshot depth) {

        double price = tick.getLastTradedPrice();

        long volume = tick.getVolumeTradedToday();


        TickFlowState previous = previousTicks.get(symbol);


        if (previous != null) {

            long volumeDelta = volume - previous.volume;


            if (volumeDelta > 0) {

                TradeSide side = classifyTrade(price, depth.bidPrice, depth.askPrice, previous.price, previous.side);


                if (side == TradeSide.BUY) {

                    MinuteState state = minuteStates.get(symbol);

                    if (state != null) {

                        state.buyAggressiveVolume += volumeDelta;
                    }

                } else if (side == TradeSide.SELL) {

                    MinuteState state = minuteStates.get(symbol);

                    if (state != null) {

                        state.sellAggressiveVolume += volumeDelta;
                    }
                }
            }
        }


        // =====================================================
        // SAVE CURRENT TICK
        // =====================================================

        TickFlowState current = new TickFlowState();

        current.price = price;

        current.volume = volume;

        current.bid = depth.bidPrice;

        current.ask = depth.askPrice;

        current.side = previous != null ? classifyTrade(price, depth.bidPrice, depth.askPrice, previous.price,
            previous.side) : TradeSide.UNKNOWN;

        previousTicks.put(symbol, current);
    }


    // =========================================================
    // CLASSIFY TRADE
    // =========================================================

    private static TradeSide classifyTrade(double price, double bid, double ask, double previousPrice, TradeSide previousSide) {

        if (ask > 0 && price >= ask) {

            return TradeSide.BUY;
        }

        if (bid > 0 && price <= bid) {

            return TradeSide.SELL;
        }


        /*
         * If price is between bid and ask,
         * use tick rule.
         */

        if (price > previousPrice) {
            return TradeSide.BUY;
        }

        if (price < previousPrice) {
            return TradeSide.SELL;
        }


        return previousSide;
    }


    // =========================================================
    // COMPLETED MINUTE
    // =========================================================

    private static void processCompletedMinute(String symbol, MinuteState current) {

        Deque<MinuteState> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>());


        // =====================================================
        // NEED PREVIOUS CANDLE
        // =====================================================

        MinuteState previous = history.peekLast();


        if (previous == null) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // CURRENT PRICE
        // =====================================================

        double currentPrice = current.closePrice;

        double previousPrice = previous.closePrice;


        if (currentPrice <= 0 || previousPrice <= 0) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // 1 MINUTE RETURN
        // =====================================================

        double return1m = percentageChange(previousPrice, currentPrice);


        // =====================================================
        // CURRENT VOLUME
        // =====================================================

        long currentVolume = minuteVolume(current);


        /*
         * Protect against bad cumulative-volume packets.
         */
        if (currentVolume < 0) {
            currentVolume = 0;
        }


        // =====================================================
        // PREVIOUS RANGE
        //
        // CURRENT CANDLE IS NOT INCLUDED.
        // =====================================================

        double rangeHigh = Double.NEGATIVE_INFINITY;

        double rangeLow = Double.POSITIVE_INFINITY;


        long totalPreviousVolume = 0;

        int volumeSamples = 0;


        for (MinuteState state : history) {

            rangeHigh = Math.max(rangeHigh, state.highPrice);

            rangeLow = Math.min(rangeLow, state.lowPrice);


            long volume = minuteVolume(state);


            if (volume > 0) {

                totalPreviousVolume += volume;

                volumeSamples++;
            }
        }


        if (!Double.isFinite(rangeHigh) || !Double.isFinite(rangeLow)) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // AVERAGE PREVIOUS VOLUME
        // =====================================================

        double averageVolume = volumeSamples > 0 ? totalPreviousVolume * 1.0 / volumeSamples : 0;


        double volumeRatio = averageVolume > 0 ? currentVolume * 1.0 / averageVolume : 0;


        // =====================================================
        // 5 MINUTE RETURN
        // =====================================================

        double return5m = 0;

        boolean has5m = history.size() >= RETURN_LOOKBACK;


        if (has5m) {

            MinuteState fiveMinutesAgo = getFiveMinuteAgo(history);

            if (fiveMinutesAgo != null && fiveMinutesAgo.closePrice > 0) {

                return5m = percentageChange(fiveMinutesAgo.closePrice, currentPrice);
            }
        }


        // =====================================================
        // BREAKOUT
        //
        // IMPORTANT:
        //
        // We use CLOSE above the previous range.
        // This avoids treating a temporary wick as
        // a confirmed breakout.
        // =====================================================

        double breakoutPct = rangeHigh > 0 ? (currentPrice - rangeHigh) * 100.0 / rangeHigh : 0;


        boolean breakout = currentPrice > rangeHigh && breakoutPct >= MIN_BREAKOUT_PCT;


        // =====================================================
        // BUY / SELL PRESSURE
        // =====================================================

        long buyVolume = current.buyAggressiveVolume;

        long sellVolume = current.sellAggressiveVolume;


        long directionalVolume = buyVolume + sellVolume;


        double buyPressure = directionalVolume > 0 ? buyVolume * 100.0 / directionalVolume : 50.0;


        double sellPressure = directionalVolume > 0 ? sellVolume * 100.0 / directionalVolume : 50.0;


        // =====================================================
        // DEPTH IMBALANCE
        // =====================================================

        long bidDepth = current.endBidQty;

        long askDepth = current.endAskQty;


        double depthImbalance = bidDepth + askDepth > 0 ? (bidDepth - askDepth) * 100.0 / (bidDepth + askDepth) : 0;


        double depthRatio = askDepth > 0 ? bidDepth * 1.0 / askDepth : 0;


        // =====================================================
        // VWAP
        // =====================================================

        double minuteVwap = current.vwapVolume > 0 ? current.vwapValue / current.vwapVolume : current.closePrice;


        boolean aboveVwap = current.closePrice >= minuteVwap;


        // =====================================================
        // BASIC CONDITIONS
        // =====================================================

        boolean priceMomentum = return1m >= MIN_1M_MOVE_PCT;


        boolean volumeExpansion = volumeRatio >= MIN_VOLUME_RATIO;


        boolean shortMomentum = has5m && return5m >= MIN_5M_RETURN_PCT;


        boolean buyingPressure = buyPressure >= MIN_BUY_PRESSURE;


        boolean positiveDepth = depthImbalance >= MIN_DEPTH_IMBALANCE;


        // =====================================================
        // HARD CANDIDATE GATE
        //
        // This is the most important improvement.
        //
        // We don't want a stock simply scoring high
        // because of one strong number.
        // =====================================================

        boolean candidate = priceMomentum && shortMomentum && volumeExpansion && breakout && buyingPressure && aboveVwap;


        // =====================================================
        // SCORE
        // =====================================================
        StockMomentum momentum = momentumMap.get(symbol);

        double close0915 = momentum != null ? momentum.getClose0915() : 0;

        double changeFrom0915Pct = close0915 > 0 ? (currentPrice - close0915) * 100.0 / close0915 : 0;

        double score = calculateScore(changeFrom0915Pct, return1m, return5m, volumeRatio, breakoutPct, buyPressure,
            depthImbalance, aboveVwap);

        // =====================================================
        // TRADE ENTRY
        // =====================================================

        boolean strongCandidate = priceMomentum && shortMomentum && volumeExpansion && breakout && buyingPressure && aboveVwap && score >= 80;

        if (strongCandidate) {

            System.out.printf(
                "🟢 TRADE CANDIDATE | %s | " + "Score %.1f | " + "Price %.2f | " + "Price change form open %.2f | " + "1m %+5.2f%% | " + "5m %+5.2f%% | " + "Vol %.2fx | " + "Breakout %+5.2f%% | " + "Buy %.1f%% | " + "VWAP %.2f%n",

                symbol, score, currentPrice, changeFrom0915Pct, return1m, return5m, volumeRatio, breakoutPct,
                buyPressure, minuteVwap);
        }

        // =====================================================
        // STORE ONLY REAL CANDIDATES
        // =====================================================

        if (candidate && score >= MIN_SCORE) {

            realtimeCandidates.put(symbol,
                new RealtimeCandidate(symbol, current.minute, currentPrice, changeFrom0915Pct, return1m, return5m,
                    volumeRatio, breakoutPct, buyPressure, sellPressure, depthRatio, depthImbalance, minuteVwap,
                    score));
        } else {
            realtimeCandidates.remove(symbol);
        }


        // =====================================================
        // SAVE HISTORY
        // =====================================================
        RealtimeCandidate best = getBestCandidate();

        if (best != null) {

            System.out.printf("🔥 BEST TRADE: %s | Score %.1f%n", best.symbol, best.score);
        }
        Set<String> completed = completedMinutes.computeIfAbsent(current.minute, k -> ConcurrentHashMap.newKeySet());

        completed.add(symbol);

        history.addLast(current);

        trimHistory(history);

    }

    private static void processCompletedMinute(Map<String, MinuteState> completedStates) {

        /*
         * =====================================================
         * PROCESS EVERY SYMBOL
         * =====================================================
         */

        for (Map.Entry<String, MinuteState> entry : completedStates.entrySet()) {

            String symbol = entry.getKey();
            MinuteState current = entry.getValue();
            processSymbolMinute(symbol, current);
        }


        /*
         * =====================================================
         * NOW ALL SYMBOLS ARE PROCESSED
         *
         * Only NOW find the best candidate.
         * =====================================================
         */

        RealtimeCandidate best = getBestCandidate();

        if (best != null) {

            System.out.printf(
                "🔥 BEST TRADE | %s | " + "Score %.1f | " + "Price %.2f | " + "Open %+5.2f%% | " + "1m %+5.2f%% | " + "5m %+5.2f%% | " + "Vol %.2fx | " + "Breakout %+5.2f%% | " + "Buy %.1f%% | " + "VWAP %.2f%n",

                best.symbol, best.score, best.price, best.changeFrom0915Pct, best.return1m, best.return5m,
                best.volumeRatio, best.breakoutPct, best.buyPressure, best.vwap);
        }
    }

    private static void processSymbolMinute(String symbol, MinuteState current) {

        // =====================================================
        // HISTORY
        // =====================================================

        Deque<MinuteState> history = minuteHistory.computeIfAbsent(symbol, k -> new ArrayDeque<>());


        // =====================================================
        // PREVIOUS CANDLE
        // =====================================================

        MinuteState previous = history.peekLast();


        if (previous == null) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // CURRENT / PREVIOUS PRICE
        // =====================================================

        double currentPrice = current.closePrice;
        double previousPrice = previous.closePrice;


        if (currentPrice <= 0 || previousPrice <= 0) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // 1 MINUTE RETURN
        // =====================================================

        double return1m = percentageChange(previousPrice, currentPrice);


        // =====================================================
        // CURRENT VOLUME
        // =====================================================

        long currentVolume = minuteVolume(current);

        if (currentVolume < 0) {
            currentVolume = 0;
        }


        // =====================================================
        // PREVIOUS RANGE + AVERAGE VOLUME
        //
        // CURRENT CANDLE IS NOT INCLUDED
        // =====================================================

        double rangeHigh = Double.NEGATIVE_INFINITY;

        double rangeLow = Double.POSITIVE_INFINITY;

        long totalPreviousVolume = 0;

        int volumeSamples = 0;


        for (MinuteState state : history) {

            rangeHigh = Math.max(rangeHigh, state.highPrice);

            rangeLow = Math.min(rangeLow, state.lowPrice);


            long volume = minuteVolume(state);


            if (volume > 0) {

                totalPreviousVolume += volume;

                volumeSamples++;
            }
        }


        if (!Double.isFinite(rangeHigh) || !Double.isFinite(rangeLow)) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // AVERAGE VOLUME
        // =====================================================

        double averageVolume = volumeSamples > 0 ? totalPreviousVolume * 1.0 / volumeSamples : 0;


        double volumeRatio = averageVolume > 0 ? currentVolume * 1.0 / averageVolume : 0;


        // =====================================================
        // 5 MINUTE RETURN
        // =====================================================

        double return5m = 0;

        boolean has5m = history.size() >= RETURN_LOOKBACK;


        if (has5m) {

            MinuteState fiveMinutesAgo = getFiveMinuteAgo(history);


            if (fiveMinutesAgo != null && fiveMinutesAgo.closePrice > 0) {

                return5m = percentageChange(fiveMinutesAgo.closePrice, currentPrice);
            }
        }


        // =====================================================
        // BREAKOUT
        // =====================================================

        double breakoutPct = rangeHigh > 0 ? (currentPrice - rangeHigh) * 100.0 / rangeHigh : 0;


        boolean breakout = currentPrice > rangeHigh && breakoutPct >= MIN_BREAKOUT_PCT;


        // =====================================================
        // BUY / SELL PRESSURE
        // =====================================================

        long buyVolume = current.buyAggressiveVolume;

        long sellVolume = current.sellAggressiveVolume;


        long directionalVolume = buyVolume + sellVolume;


        double buyPressure = directionalVolume > 0 ? buyVolume * 100.0 / directionalVolume : 50.0;


        double sellPressure = directionalVolume > 0 ? sellVolume * 100.0 / directionalVolume : 50.0;


        // =====================================================
        // DEPTH
        // =====================================================

        long bidDepth = current.endBidQty;

        long askDepth = current.endAskQty;


        double depthImbalance = bidDepth + askDepth > 0 ? (bidDepth - askDepth) * 100.0 / (bidDepth + askDepth) : 0;


        double depthRatio = askDepth > 0 ? bidDepth * 1.0 / askDepth : 0;


        // =====================================================
        // VWAP
        // =====================================================

        double minuteVwap = current.vwapVolume > 0 ? current.vwapValue / current.vwapVolume : current.closePrice;


        boolean aboveVwap = current.closePrice >= minuteVwap;


        // =====================================================
        // CONDITIONS
        // =====================================================

        boolean priceMomentum = return1m >= MIN_1M_MOVE_PCT;


        boolean volumeExpansion = volumeRatio >= MIN_VOLUME_RATIO;


        boolean shortMomentum = has5m && return5m >= MIN_5M_RETURN_PCT;


        boolean buyingPressure = buyPressure >= MIN_BUY_PRESSURE;


        boolean positiveDepth = depthImbalance >= MIN_DEPTH_IMBALANCE;


        // =====================================================
        // CANDIDATE
        // =====================================================

        boolean candidate = priceMomentum && shortMomentum && volumeExpansion && breakout && buyingPressure && aboveVwap;


        // =====================================================
        // PRICE FROM 09:15
        // =====================================================

        StockMomentum momentum = momentumMap.get(symbol);


        double close0915 = momentum != null ? momentum.getClose0915() : 0;


        double changeFrom0915Pct = close0915 > 0 ? (currentPrice - close0915) * 100.0 / close0915 : 0;


        // =====================================================
        // SCORE
        // =====================================================

        double score = calculateScore(changeFrom0915Pct, return1m, return5m, volumeRatio, breakoutPct, buyPressure,
            depthImbalance, aboveVwap);


        // =====================================================
        // STRONG CANDIDATE
        // =====================================================

        boolean strongCandidate = candidate && score >= 50;


        if (strongCandidate) {

            System.out.printf(
                "🟢 CANDIDATE | %s | " + "Score %.1f | " + "OClose %.2f |  " +  "Price %.2f |  " + "Open %+5.2f%% | " + "1m %+5.2f%% | " + "5m %+5.2f%% | " + "Vol %.2fx | " + "Breakout %+5.2f%% | " + "Buy %.1f%% | " + "VWAP %.2f%n",

                symbol, score, close0915, currentPrice, changeFrom0915Pct, return1m, return5m, volumeRatio, breakoutPct,
                buyPressure, minuteVwap);
        }


        // =====================================================
        // STORE CANDIDATE
        // =====================================================

        if (candidate && score >= MIN_SCORE) {

            realtimeCandidates.put(symbol,
                new RealtimeCandidate(symbol, current.minute, currentPrice, changeFrom0915Pct, return1m, return5m,
                    volumeRatio, breakoutPct, buyPressure, sellPressure, depthRatio, depthImbalance, minuteVwap,
                    score));

        } else {

            realtimeCandidates.remove(symbol);
        }

        // =====================================================
        // SAVE HISTORY
        // =====================================================

        history.addLast(current);

        trimHistory(history);
    }


    private static RealtimeCandidate getBestCandidate() {

        return realtimeCandidates.values().stream().filter(c -> c.score >= 80).max(
            Comparator.comparingDouble(c -> c.score)).orElse(null);
    }


    // =========================================================
    // GET 5-MINUTE-AGO CANDLE
    // =========================================================

    private static MinuteState getFiveMinuteAgo(Deque<MinuteState> history) {

        java.util.Iterator<MinuteState> iterator = history.descendingIterator();


        MinuteState result = null;


        for (int i = 0; i < RETURN_LOOKBACK && iterator.hasNext(); i++) {

            result = iterator.next();
        }


        return result;
    }


    // =========================================================
    // SCORE
    //
    // TOTAL = 100
    //
    // 1m momentum      20
    // 5m momentum      15
    // volume           20
    // breakout         20
    // buy pressure     15
    // depth             5
    // VWAP               5
    // =========================================================

    private static double calculateScore(double changeFrom0915Pct, double return1m, double return5m, double volumeRatio, double breakoutPct, double buyPressure, double depthImbalance, boolean aboveVwap) {

        double score = 0;


        // =====================================================
        // 09:15 → CURRENT CONTINUATION — 10
        // =====================================================

        if (changeFrom0915Pct >= 3.0)
            score += 10;

        else if (changeFrom0915Pct >= 2.0)
            score += 8;

        else if (changeFrom0915Pct >= 1.0)
            score += 6;

        else if (changeFrom0915Pct >= 0.5)
            score += 3;


        // =====================================================
        // 1-MINUTE MOMENTUM — 15
        // =====================================================

        if (return1m >= 0.60)
            score += 15;

        else if (return1m >= 0.40)
            score += 12;

        else if (return1m >= 0.30)
            score += 9;

        else if (return1m >= 0.20)
            score += 5;


        // =====================================================
        // 5-MINUTE MOMENTUM — 15
        // =====================================================

        if (return5m >= 2.0)
            score += 15;

        else if (return5m >= 1.5)
            score += 12;

        else if (return5m >= 1.0)
            score += 9;

        else if (return5m >= 0.70)
            score += 6;

        else if (return5m >= 0.40)
            score += 3;


        // =====================================================
        // VOLUME EXPANSION — 20
        // =====================================================

        if (volumeRatio >= 4.0)
            score += 20;

        else if (volumeRatio >= 3.0)
            score += 17;

        else if (volumeRatio >= 2.5)
            score += 14;

        else if (volumeRatio >= 2.0)
            score += 10;

        else if (volumeRatio >= 1.5)
            score += 6;


        // =====================================================
        // BREAKOUT — 15
        // =====================================================

        if (breakoutPct >= 0.50)
            score += 15;

        else if (breakoutPct >= 0.30)
            score += 12;

        else if (breakoutPct >= 0.20)
            score += 8;

        else if (breakoutPct >= 0.10)
            score += 4;


        // =====================================================
        // BUY PRESSURE — 15
        // =====================================================

        if (buyPressure >= 80)
            score += 15;

        else if (buyPressure >= 75)
            score += 13;

        else if (buyPressure >= 70)
            score += 10;

        else if (buyPressure >= 65)
            score += 7;

        else if (buyPressure >= 60)
            score += 4;

        else if (buyPressure >= 55)
            score += 2;


        // =====================================================
        // BID / ASK DEPTH — 5
        // =====================================================

        if (depthImbalance >= 40)
            score += 5;

        else if (depthImbalance >= 30)
            score += 4;

        else if (depthImbalance >= 20)
            score += 3;

        else if (depthImbalance >= 10)
            score += 2;


        // =====================================================
        // VWAP — 5
        // =====================================================

        if (aboveVwap)
            score += 5;


        return score;
    }


    // =========================================================
    // PRINT TOP STOCKS
    // =========================================================

    private static void printTopCandidates() {

        List<RealtimeCandidate> top = realtimeCandidates.values().stream().sorted(
            Comparator.comparingDouble((RealtimeCandidate x) -> x.score).reversed()).limit(5).toList();


        System.out.println();

        System.out.println("==============================================================");

        System.out.println("             REALTIME CASH EQUITY MOMENTUM");

        System.out.println("==============================================================");


        for (RealtimeCandidate c : top) {

            System.out.printf(
                "%-14s | " + "Score %5.1f | " + "Price %8.2f | " + "1m %+5.2f%% | " + "5m %+5.2f%% | " + "Vol %4.1fx | " + "BO %+5.2f%% | " + "Buy %5.1f%% | " + "Dpth %+5.1f%% | " + "VWAP %8.2f%n",

                c.symbol, c.score, c.price, c.return1m, c.return5m, c.volumeRatio, c.breakoutPct, c.buyPressure,
                c.depthImbalance, c.vwap);
        }
    }


    // =========================================================
    // TRIM HISTORY
    // =========================================================

    private static void trimHistory(Deque<MinuteState> history) {

        while (history.size() > RANGE_LOOKBACK) {

            history.removeFirst();
        }
    }


    // =========================================================
    // MINUTE VOLUME
    // =========================================================

    private static long minuteVolume(MinuteState state) {

        long volume = state.endVolume - state.startVolume;

        return Math.max(volume, 0);
    }


    // =========================================================
    // PERCENTAGE CHANGE
    // =========================================================

    private static double percentageChange(double oldPrice, double newPrice) {

        if (oldPrice <= 0 || newPrice <= 0) {

            return 0;
        }

        return (newPrice - oldPrice) * 100.0 / oldPrice;
    }


    // =========================================================
    // TRADE SIDE
    // =========================================================

    enum TradeSide {

        BUY, SELL, UNKNOWN
    }


    // =========================================================
    // TICK FLOW STATE
    // =========================================================

    static class TickFlowState {

        double price;

        long volume;

        double bid;

        double ask;

        TradeSide side;
    }


    // =========================================================
    // DEPTH SNAPSHOT
    // =========================================================

    static class DepthSnapshot {

        double bidPrice;

        double askPrice;

        long bidQty;

        long askQty;

        long totalBidDepth;

        long totalAskDepth;
    }


    // =========================================================
    // REALTIME CANDIDATE
    // =========================================================

    static class RealtimeCandidate {

        double changeFrom0915Pct;
        String symbol;

        LocalDateTime minute;

        double price;

        double return1m;

        double return5m;

        double volumeRatio;

        double breakoutPct;

        double buyPressure;

        double sellPressure;

        double depthRatio;

        double depthImbalance;

        double vwap;

        double score;


        RealtimeCandidate(String symbol, LocalDateTime minute, double price, double changeFrom0915Pct, double return1m, double return5m, double volumeRatio, double breakoutPct, double buyPressure, double sellPressure, double depthRatio, double depthImbalance, double vwap, double score) {

            this.symbol = symbol;

            this.minute = minute;

            this.price = price;

            this.return1m = return1m;

            this.return5m = return5m;

            this.volumeRatio = volumeRatio;

            this.breakoutPct = breakoutPct;

            this.buyPressure = buyPressure;

            this.sellPressure = sellPressure;

            this.depthRatio = depthRatio;

            this.depthImbalance = depthImbalance;

            this.vwap = vwap;

            this.score = score;
            this.changeFrom0915Pct = changeFrom0915Pct;
        }
    }


    // =========================================================
    // MINUTE STATE
    // =========================================================

    public static class MinuteState {

        LocalDateTime minute;


        // =====================================================
        // OHLC
        // =====================================================

        double openPrice;

        double highPrice;

        double lowPrice;

        double closePrice;


        // =====================================================
        // TOTAL TRADED VOLUME
        // =====================================================

        long startVolume;

        long endVolume;


        // =====================================================
        // ORDER BOOK
        // =====================================================

        long startBidQty;

        long endBidQty;

        long startAskQty;

        long endAskQty;


        double lastBid;

        double lastAsk;


        // =====================================================
        // ESTIMATED AGGRESSIVE TRADE VOLUME
        // =====================================================

        long buyAggressiveVolume;

        long sellAggressiveVolume;


        // =====================================================
        // VWAP
        // =====================================================

        double vwapValue;

        long vwapVolume;


        // =====================================================
        // TICKS
        // =====================================================

        long tickCount;
    }
}
