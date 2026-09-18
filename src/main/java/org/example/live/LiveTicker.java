package org.example.live;


import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnTicks;
import org.example.util.TokenUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LiveTicker {

    private static KiteTicker liveTicker;
    private static final Map<String, Double> lastPrice = new ConcurrentHashMap<>();
    private static final Map<String, Double> lastBuyQty = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastVolume = new ConcurrentHashMap<>();

    private static final Set<String> buySurgeSymbols = ConcurrentHashMap.newKeySet();

    private static final Map<String, Tick> PREVIOUS = new ConcurrentHashMap<>();

    private static final Map<String, Tick> CURRENT = new ConcurrentHashMap<>();
    private static final Map<String, MinuteState> minuteStates = new ConcurrentHashMap<>();

    private static final Map<String, MinuteState> previousMinuteStates = new ConcurrentHashMap<>();
    private static final Map<String, Integer> continuationCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LocalDateTime> specialFirstOccurrence = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> specialCount = new ConcurrentHashMap<>();

    private static final Set<String> tradedSymbols = ConcurrentHashMap.newKeySet();
    private static final int RANGE_LOOKBACK = 10;
    private static final int RETURN_LOOKBACK = 5;

    private static final double MIN_5M_RETURN_PCT = 0.30;
    private static final double MIN_VOLUME_RATIO = 1.50;
    private static final double BREAKOUT_BUFFER_PCT = 0.10;
    private static final int LOOKBACK_MINUTES = 10;

    private static final double MIN_1M_MOVE_PCT = 0.30;
    private static final double MIN_BREAKOUT_PCT = 0.25;
    private static final double STRONG_1M_MOVE_PCT = 0.50;
    private static final double STRONG_VOLUME_RATIO = 3.0;

    private static final Map<String, Deque<MinuteState>> minuteHistory =
        new ConcurrentHashMap<>();
    private static final Map<String, Double> activeBreakout =
        new ConcurrentHashMap<>();
    private static final Map<String, RealtimeCandidate>
        realtimeCandidates = new ConcurrentHashMap<>();

    public static void startWebSocket(KiteConnect kite, Set<String> tokens) throws KiteException, Exception {

        Map<Long, String> tokenMap = TokenUtils.getTokens(kite, tokens);

        liveTicker = new KiteTicker(kite.getAccessToken(), kite.getApiKey());

        liveTicker.setTryReconnection(true);
        liveTicker.setMaximumRetries(10);
        liveTicker.setMaximumRetryInterval(30);

        liveTicker.setOnConnectedListener(new OnConnect() {

            @Override
            public void onConnected() {

                System.out.println("WebSocket Connected");

                ArrayList<Long> instrumentTokens = new ArrayList<>(tokenMap.keySet());

                liveTicker.subscribe(instrumentTokens);

                liveTicker.setMode(instrumentTokens, KiteTicker.modeFull);
            }
        });

        liveTicker.setOnTickerArrivalListener(new OnTicks() {

            @Override
            public void onTicks(ArrayList<Tick> ticks) {

                for (Tick tick : ticks) {

                    if (tick == null || tick.getLastTradedTime() == null) {
                        continue;
                    }

                    // =====================================
                    // TIME
                    // =====================================

                    LocalDateTime dateTime = tick.getLastTradedTime().toInstant().atZone(
                        ZoneId.of("Asia/Kolkata")).toLocalDateTime();

                    LocalTime tickTime = dateTime.toLocalTime();

                    if (tickTime.isBefore(LocalTime.of(9, 15))) {
                        continue;
                    }

                    if (tickTime.isAfter(LocalTime.of(15, 30))) {
                        continue;
                    }

                    LocalDateTime minute = dateTime.withSecond(0).withNano(0);

                    // =====================================
                    // SYMBOL
                    // =====================================

                    String symbol = tokenMap.get(tick.getInstrumentToken());

                    if (symbol == null) {
                        continue;
                    }

                    // =====================================
                    // CURRENT VALUES
                    // =====================================

                    double price = tick.getLastTradedPrice();

                    double buyQty = tick.getTotalBuyQuantity();

                    long volume = tick.getVolumeTradedToday();

                    if (price <= 0) {
                        continue;
                    }

                    // =====================================
                    // MINUTE STATE
                    // =====================================

                    minuteStates.compute(symbol, (key, state) -> {

                        // =================================
                        // FIRST TICK
                        // =================================

                        if (state == null) {

                            state = new MinuteState();

                            state.minute = minute;

                            state.openPrice = price;

                            state.highPrice = price;

                            state.lowPrice = price;

                            state.closePrice = price;

                            state.startBuyQty = buyQty;

                            state.endBuyQty = buyQty;

                            state.startVolume = volume;

                            state.endVolume = volume;

                            state.tickCount = 1;

                            return state;
                        }

                        // =================================
                        // NEW MINUTE
                        // =================================

                        if (!state.minute.equals(minute)) {

                            // Previous minute completed
                            processCompletedMinute(symbol, state);

                            // Create new minute
                            MinuteState newState = new MinuteState();

                            newState.minute = minute;

                            newState.openPrice = price;

                            newState.highPrice = price;

                            newState.lowPrice = price;

                            newState.closePrice = price;

                            newState.startBuyQty = buyQty;

                            newState.endBuyQty = buyQty;

                            newState.startVolume = volume;

                            newState.endVolume = volume;

                            newState.tickCount = 1;

                            return newState;
                        }

                        // =================================
                        // SAME MINUTE
                        // =================================

                        state.highPrice = Math.max(state.highPrice, price);

                        state.lowPrice = Math.min(state.lowPrice, price);

                        state.closePrice = price;

                        state.endBuyQty = buyQty;

                        state.endVolume = volume;

                        state.tickCount++;

                        return state;
                    });
                }
            }
        });

        liveTicker.connect();
    }

    private static void processCompletedMinuteNew(
        String symbol,
        MinuteState current) {

        // =====================================================
        // HISTORY
        // =====================================================

        Deque<MinuteState> history =
            minuteHistory.computeIfAbsent(
                symbol,
                k -> new ArrayDeque<>()
            );


        // =====================================================
        // FIRST CANDLE
        // =====================================================

        MinuteState previous =
            history.peekLast();

        if (previous == null) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // PRICE
        // =====================================================

        double currentPrice =
            current.closePrice;

        double previousPrice =
            previous.closePrice;

        if (currentPrice <= 0 ||
            previousPrice <= 0) {

            history.addLast(current);

            trimHistory(history);

            return;
        }


        // =====================================================
        // 1-MINUTE PRICE CHANGE
        // =====================================================

        double priceChangePct =
            (currentPrice - previousPrice)
                * 100.0
                / previousPrice;


        // =====================================================
        // CURRENT MINUTE VOLUME
        // =====================================================

        long currentVolume =
            current.endVolume -
                current.startVolume;


        // =====================================================
        // PREVIOUS RANGE
        //
        // IMPORTANT:
        // Only previous candles are used.
        // Current candle is NOT included.
        // =====================================================

        double rangeHigh =
            Double.MIN_VALUE;

        double rangeLow =
            Double.MAX_VALUE;

        long totalPreviousVolume = 0;

        int volumeSamples = 0;


        for (MinuteState state : history) {

            rangeHigh =
                Math.max(
                    rangeHigh,
                    state.highPrice
                );

            rangeLow =
                Math.min(
                    rangeLow,
                    state.lowPrice
                );


            long minuteVolume =
                state.endVolume -
                    state.startVolume;

            if (minuteVolume > 0) {

                totalPreviousVolume +=
                    minuteVolume;

                volumeSamples++;
            }
        }


        // =====================================================
        // AVERAGE PREVIOUS VOLUME
        // =====================================================

        double averageVolume =
            volumeSamples > 0
                ? totalPreviousVolume * 1.0
                / volumeSamples
                : 0;


        double volumeRatio =
            averageVolume > 0
                ? currentVolume * 1.0
                / averageVolume
                : 0;


        // =====================================================
        // BREAKOUT %
        //
        // Example:
        //
        // RangeHigh = 100
        // Current   = 100.40
        //
        // Breakout = 0.40%
        // =====================================================

        double breakoutPct =
            rangeHigh > 0
                ? (currentPrice - rangeHigh)
                * 100.0
                / rangeHigh
                : 0;


        // =====================================================
        // 5-MINUTE RETURN
        //
        // ONLY FOR INFORMATION / STRENGTH.
        // NOT REQUIRED FOR FIRST SIGNAL.
        // =====================================================

        double return5mPct = 0;

        MinuteState fiveMinuteAgo = null;

        if (history.size() >= 5) {

            Iterator<MinuteState> iterator =
                history.descendingIterator();

            // Current history contains completed candles
            // immediately BEFORE current.
            //
            // We want the candle 5 positions back.

            for (int i = 0; i < 5 && iterator.hasNext(); i++) {
                fiveMinuteAgo = iterator.next();
            }
        }

        if (fiveMinuteAgo != null &&
            fiveMinuteAgo.closePrice > 0) {

            return5mPct =
                (currentPrice -
                    fiveMinuteAgo.closePrice)
                    * 100.0
                    / fiveMinuteAgo.closePrice;
        }


        // =====================================================
        // CONDITIONS
        // =====================================================

        boolean priceMomentum =
            priceChangePct >=
                MIN_1M_MOVE_PCT;


        boolean breakout =
            breakoutPct >=
                MIN_BREAKOUT_PCT;


        boolean volumeExpansion =
            volumeRatio >=
                MIN_VOLUME_RATIO;


        // =====================================================
        // FINAL CANDIDATE
        // =====================================================

        double realtimeScore =
            calculateRealtimeScore(
                priceChangePct,
                return5mPct,
                volumeRatio,
                breakoutPct
            );
        if (realtimeScore >= 60) {

            realtimeCandidates.put(
                symbol,
                new RealtimeCandidate(
                    symbol,
                    priceChangePct,
                    return5mPct,
                    volumeRatio,
                    breakoutPct,
                    realtimeScore
                )
            );
        }
        List<RealtimeCandidate> topRealtime =
            realtimeCandidates.values()
                .stream()
                .sorted(
                    Comparator.comparingDouble(
                        (RealtimeCandidate x) -> x.score
                    ).reversed()
                )
                .limit(3)
                .toList();

        // =====================================================
        // OUTPUT
        // =====================================================

        System.out.println();
        System.out.println("========== REALTIME TOP MOMENTUM ==========");

        for (RealtimeCandidate c : topRealtime) {

            System.out.printf(
                "%-15s | Score %6.1f | " +
                    "1m %+5.2f%% | " +
                    "5m %+5.2f%% | " +
                    "Vol %.2fx | " +
                    "Breakout %+5.2f%%%n",

                c.symbol,
                c.score,
                c.priceChange1m,
                c.return5m,
                c.volumeRatio,
                c.breakoutPct
            );
        }

        // =====================================================
        // SAVE CURRENT CANDLE
        // =====================================================

        history.addLast(current);

        trimHistory(history);
    }
    private static void trimHistory(
        Deque<MinuteState> history) {

        while (history.size() > RANGE_LOOKBACK) {
            history.removeFirst();
        }
    }

    private static void processCompletedMinute(String symbol, MinuteState current) {

        MinuteState previous = previousMinuteStates.put(symbol, current);

        // =========================================
        // FIRST COMPLETED MINUTE
        // =========================================

        if (previous == null) {
            return;
        }

        // =========================================
        // PRICE CHANGE
        // =========================================

        double priceChangePct = previous.closePrice != 0 ? ((current.closePrice - previous.closePrice) / previous.closePrice) * 100.0 : 0;

        // =========================================
        // BUY QTY CHANGE
        // =========================================

        double buyQtyChange = current.endBuyQty - previous.endBuyQty;

        double buyQtyChangePct = previous.endBuyQty > 0 ? buyQtyChange * 100.0 / previous.endBuyQty : 0;

        // =========================================
        // VOLUME CHANGE
        // =========================================

        long currentVolume = current.endVolume - current.startVolume;

        long previousVolume = previous.endVolume - previous.startVolume;

        long volumeChange = currentVolume - previousVolume;

        double volumeChangePct = previousVolume > 0 ? volumeChange * 100.0 / previousVolume : 0;

        // =========================================
        // MOMENTUM CONDITIONS
        // =========================================

        boolean priceUp = current.closePrice > previous.closePrice;

        boolean buyQtyUp = current.endBuyQty > previous.endBuyQty;

        boolean volumeUp = currentVolume > previousVolume;

        boolean continuation = priceUp && buyQtyUp && volumeUp;

        // =========================================
        // SURGE
        // =========================================

        boolean buyQtySurge = previous.endBuyQty > 0 && current.endBuyQty >= previous.endBuyQty * 2;

        boolean volumeSurge = previousVolume > 0 && currentVolume >= previousVolume * 2;

        // =========================================
        // OUTPUT
        // =========================================

        if (continuation) {

            if (buyQtySurge || volumeSurge) {

                LocalDateTime firstOccurrence = specialFirstOccurrence.putIfAbsent(symbol, current.minute);

                int count = specialCount.merge(symbol, 1, Integer::sum);

                // =====================================
                // FIRST SPECIAL
                // =====================================

                if (firstOccurrence == null) {

                    System.out.printf(
                        "🔥 FIRST SPECIAL | %-15s | " + "First=%s | Current=%s | " + "Price %.2f -> %.2f | " + "PriceΔ %+6.2f%% | " + "BuyQtyΔ %+7.2f%% | " + "VolumeΔ %+7.2f%%%n",

                        symbol,

                        current.minute, current.minute,

                        previous.closePrice, current.closePrice,

                        priceChangePct, buyQtyChangePct, volumeChangePct);

                } else {

                    // =================================
                    // REPEATED SPECIAL
                    // =================================

                    System.out.printf(
                        "🔥 REPEATED SPECIAL | %-15s | " + "First=%s | Current=%s | " + "Count=%d | " + "Price %.2f -> %.2f | " + "PriceΔ %+6.2f%% | " + "BuyQtyΔ %+7.2f%% | " + "VolumeΔ %+7.2f%%%n",

                        symbol,

                        firstOccurrence, current.minute,

                        count,

                        previous.closePrice, current.closePrice,

                        priceChangePct, buyQtyChangePct, volumeChangePct);
                }
            } else {

                System.out.printf(
                    "CANDIDATE | %s | " + "Minute %s | " + "Price %.2f -> %.2f | " + "PriceΔ %.2f%% | " + "BuyQtyΔ %.2f%% | " + "VolumeΔ %.2f%%%n",

                    symbol,

                    current.minute,

                    previous.closePrice, current.closePrice,

                    priceChangePct,

                    buyQtyChangePct,

                    volumeChangePct);
            }
        }
    }
    private static double calculateRealtimeScore(
        double priceChangePct,
        double return5mPct,
        double volumeRatio,
        double breakoutPct) {

        double score = 0;

        // =====================================================
        // 1-MINUTE PRICE MOMENTUM - 25
        // =====================================================

        if (priceChangePct >= 1.0)
            score += 25;
        else if (priceChangePct >= 0.60)
            score += 20;
        else if (priceChangePct >= 0.30)
            score += 15;
        else if (priceChangePct >= 0.15)
            score += 8;


        // =====================================================
        // 5-MINUTE MOMENTUM - 15
        // =====================================================

        if (return5mPct >= 2.0)
            score += 15;
        else if (return5mPct >= 1.0)
            score += 12;
        else if (return5mPct >= 0.50)
            score += 8;
        else if (return5mPct >= 0.20)
            score += 4;


        // =====================================================
        // VOLUME EXPANSION - 25
        // =====================================================

        if (volumeRatio >= 3.0)
            score += 25;
        else if (volumeRatio >= 2.0)
            score += 20;
        else if (volumeRatio >= 1.5)
            score += 15;
        else if (volumeRatio >= 1.2)
            score += 8;


        // =====================================================
        // BREAKOUT STRENGTH - 20
        // =====================================================

        if (breakoutPct >= 1.0)
            score += 20;
        else if (breakoutPct >= 0.50)
            score += 15;
        else if (breakoutPct >= 0.20)
            score += 10;
        else if (breakoutPct >= 0.05)
            score += 5;


        // =====================================================
        // CONSISTENCY / CONFIRMATION - 15
        // =====================================================

        if (priceChangePct > 0 && return5mPct > 0)
            score += 8;

        if (volumeRatio >= 1.5)
            score += 7;


        return score;
    }
    static class RealtimeCandidate {

        String symbol;

        double priceChange1m;
        double return5m;
        double volumeRatio;
        double breakoutPct;

        double score;

        RealtimeCandidate(
            String symbol,
            double priceChange1m,
            double return5m,
            double volumeRatio,
            double breakoutPct,
            double score) {

            this.symbol = symbol;
            this.priceChange1m = priceChange1m;
            this.return5m = return5m;
            this.volumeRatio = volumeRatio;
            this.breakoutPct = breakoutPct;
            this.score = score;
        }
    }

    public static class MinuteState {

        LocalDateTime minute;

        double openPrice;
        double highPrice;
        double lowPrice;
        double closePrice;

        double startBuyQty;
        double endBuyQty;

        long startVolume;
        long endVolume;

        long tickCount;

        // Order book snapshot
        long startBidQty;
        long endBidQty;

        long startAskQty;
        long endAskQty;

        // Estimated aggressive trade volume
        long buyAggressiveVolume;
        long sellAggressiveVolume;
    }
}

