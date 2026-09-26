package org.example;


import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import org.example.live.*;
import org.example.loader.DB;
import org.example.loader.DataLoader;
import org.example.model.*;
import org.example.repository.PotentialMoverScanner;
import org.example.repository.StockDailyFeatureRepository;
import org.example.service.TelegramAlertService;
import org.example.util.*;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PotentialScanner {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);

    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final String API_KEY = "8311x4p8tm56j4vc";
    private static final String ACCESS_TOKEN = "LToUk6c2AfK9bxDjQbvEaD79Rh38YmWj";

    private static Set<String> symbols = new HashSet<>();

    private static final ConcurrentHashMap<String, Integer> qualificationMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LocalDateTime> qualificationTimeMap = new ConcurrentHashMap<>();
    static Map<String, Position> openPositions = new HashMap<>();
    static double TARGET_PROFIT = 1000.0;
    static double CAPITAL_PER_TRADE = 30000.0;
    static double totalProfit = 0;
    static double totalCapital = 150000;
    static Map<String, Double> lastPriceMap = new HashMap<>();
    static Map<String, Double> highPriceMap = new HashMap<>();

    public static void main(String[] args) throws Exception, KiteException {

        //startMinuteScanner();
        //startGapUpScanner();
        Map<String, FundamentalData> fundamentalDataMap = StockDailyFeatureRepository.loadFundamentals(DB.get());
        //startTopGainerLoserScheduler(fundamentalDataMap);
        //startScanner(fundamentalDataMap);
        Map<String, PreviousDayData> previousDayDataMap = StockDailyFeatureRepository.loadPreviousDayClose(
                LocalDate.now(), DB.get(), 1);

        startEMAandVWAPScheduler(previousDayDataMap, fundamentalDataMap);


    }

    private static void startScanner(Map<String, FundamentalData> fundamentalDataMap) throws KiteException, Exception {
        KiteConnect kite = new KiteConnect(API_KEY);
        kite.setAccessToken(ACCESS_TOKEN);
        // Map<String, StockMomentum> gapUpSymbolClosePriceMap = getMomentumMap();
        Set<String> symbols = TokenUtils.getTokens();
        //symbols.addAll(gapUpSymbolClosePriceMap.keySet());
        Map<String, PreviousDayData> previousDayDataMap = StockDailyFeatureRepository.loadPreviousDayClose(
                LocalDate.now().minusDays(1), DB.get(), 1);
        previousDayDataMap.keySet().forEach(st -> {
            PreviousDayData d = previousDayDataMap.get(st);
            if (d.getClosePrice() > 50 || d.getClosePrice() < 3000)
                symbols.add(st);
        });
        Map<String, StockDailyFeature> stockDailyFeatureMap = StockDailyFeatureRepository.loadCurrentDailyFeatures(
                DB.get(), previousDayDataMap.keySet());
        ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory = DataLoader.loadMinuteHistory(DB.get(),
                LocalDate.now().minusDays(1),LocalDate.now(), LocalTime.of(15, 10), LocalTime.of(15, 30), LocalTime.of(9, 15), LocalTime.of(15, 10));

        RealtimeMomentumEngine.setPreDayMap(previousDayDataMap, minuteHistory, stockDailyFeatureMap,
                fundamentalDataMap);
         //LiveTickerWeb.startWebSocket(kite, symbols);

        calculateEMAandVWAP(stockDailyFeatureMap, previousDayDataMap, fundamentalDataMap, minuteHistory, 1.5);
        // getCalculationStrongBuy(stockDailyFeatureMap, previousDayDataMap, fundamentalDataMap, minuteHistory, 1.5);
        //getCalculationPrevDayHighBreak(stockDailyFeatureMap, previousDayDataMap, fundamentalDataMap, minuteHistory, 2, 3, 3);
        /*System.out.println("totalProfit " + totalProfit);

        if (openPositions.isEmpty()) {
            System.out.println("No open positions.");
            System.out.println("================================");
            return;
        }

        double totalUnrealizedProfit = 0;

        for (Position position : openPositions.values()) {

            Double lastPrice =
                    lastPriceMap.get(position.symbol);

            if (lastPrice == null) {
                continue;
            }

            Double highPrice =
                    highPriceMap.get(position.symbol);
            double unrealizedProfit =
                    (lastPrice - position.entryPrice)
                            * position.quantity;

            totalUnrealizedProfit += unrealizedProfit;

            System.out.println(
                    position.symbol
                            + " | EntryTime=" + position.entryTime
                            + " | Entry=" + position.entryPrice
                            + " | Last=" + lastPrice
                            + " | High=" + highPrice
                            + " | Qty=" + position.quantity
                            + " | Capital=" + position.capital
                            + " | P&L=" + String.format("%.2f", unrealizedProfit)
            );
        }

        System.out.println(
                "TOTAL UNREALIZED P&L = "
                        + String.format("%.2f", totalUnrealizedProfit)
        );*/
        /*minuteHistory.keySet().forEach(key->{
                BreakoutProbabilityNew.BreakoutProbabilityResult result = BreakoutProbabilityNew.calculateBreakoutProbability(minuteHistory.get(key), 1);
                if(result.bias.equals("BULLISH") && result.bullishPercentage>80)
                    System.out.println(key + " : " + result);
        });*/
        /*List<StockScoreUtil.StockScore> rankings = new ArrayList<>();

        for (Map.Entry<String, Deque<MinuteCandle>> entry
            : minuteHistory.entrySet()) {

            StockScoreUtil.StockScore score =
                scoreStock(
                    entry.getKey(),
                    entry.getValue()
                );

            if (score.score >= 65 && score.entry>50) {
                rankings.add(score);
            }
        }

        rankings.sort(
            Comparator.comparingDouble(
                (StockScoreUtil.StockScore x) -> x.score
            ).reversed()
        );

        rankings.stream()
            .limit(10)
            .forEach(System.out::println);*/
    }

    public static void startTopGainerLoserScheduler(Map<String, FundamentalData> fundamentalDataMap) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {

            try {

                LocalDate date = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                if(LocalTime.now().isAfter(MARKET_CLOSE))
                    System.exit(0);

                List<MarketMover> losers = MoverUtil.getTopLosers(DB.get(), date, currentTime, 50,false,0);

                System.out.println("\n===== TOP LOSERS =====");

                StringBuilder telegramMsg = new StringBuilder();

                telegramMsg.append("🔴 <b>DAY TOP LOSERS </b>\n");
                telegramMsg.append("━━━━━━━━━━━━━━\n");

                for (MarketMover stock : losers) {
                    FundamentalData fundamentalData = fundamentalDataMap.get(stock.getSymbol());
                    if (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && fundamentalData.getPeRatio() < 50) {
                        telegramMsg.append("🔻 ").append(stock.getSymbol()).append(" | ₹").append(
                                String.format("%.2f", stock.getCurrentPrice())).append(" | ").append(
                                String.format("%.2f", stock.getChangePct())).append("%\n");
                    }
                }

                TelegramAlertService.send(telegramMsg.toString());

                List<MarketMover> gainers = MoverUtil.getTopGainers(DB.get(), date, currentTime, 50,false,0);

                System.out.println("\n===== TOP GAINERS =====");

                StringBuilder telegramMsgGainer = new StringBuilder();

                telegramMsg.append("🟢 <b>DAY TOP GAINERS</b>\n");
                telegramMsg.append("━━━━━━━━━━━━━━\n");

                for (MarketMover stock : gainers) {
                    FundamentalData fundamentalData = fundamentalDataMap.get(stock.getSymbol());
                    if (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && fundamentalData.getPeRatio() < 50) {
                        telegramMsgGainer.append("🚀 ").append(stock.getSymbol()).append(" | ₹").append(
                                String.format("%.2f", stock.getCurrentPrice())).append(" | ").append(
                                String.format("%.2f", stock.getChangePct())).append("%\n");
                    }
                }
                TelegramAlertService.send(telegramMsgGainer.toString());


                List<MarketMover> LastFiveMinlosers = MoverUtil.getTopLosers(DB.get(), date, currentTime, 50,true,5);

                System.out.println("\n===== Last 5 Min TOP LOSERS =====");

                StringBuilder telegramMsgfiveMin = new StringBuilder();

                telegramMsg.append("🔴 <b>Last 5 Min TOP LOSERS </b>\n");
                telegramMsg.append("━━━━━━━━━━━━━━\n");

                for (MarketMover stock : LastFiveMinlosers) {
                    FundamentalData fundamentalData = fundamentalDataMap.get(stock.getSymbol());
                    if (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && fundamentalData.getPeRatio() < 50) {
                        telegramMsgfiveMin.append("🔻 ").append(stock.getSymbol()).append(" | ₹").append(
                                String.format("%.2f", stock.getCurrentPrice())).append(" | ").append(
                                String.format("%.2f", stock.getChangePct())).append("%\n");
                    }
                }

                TelegramAlertService.send(telegramMsgfiveMin.toString());

                List<MarketMover> lastFiveMingainers = MoverUtil.getTopGainers(DB.get(), date, currentTime, 50,true,5);

                System.out.println("\n=====Last 5 min TOP GAINERS =====");

                StringBuilder telegramMsgFivrMinGainer = new StringBuilder();

                telegramMsg.append("🟢 <b>DAY TOP GAINERS</b>\n");
                telegramMsg.append("━━━━━━━━━━━━━━\n");

                for (MarketMover stock : lastFiveMingainers) {
                    FundamentalData fundamentalData = fundamentalDataMap.get(stock.getSymbol());
                    if (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && fundamentalData.getPeRatio() < 50) {
                        telegramMsgFivrMinGainer.append("🚀 ").append(stock.getSymbol()).append(" | ₹").append(
                                String.format("%.2f", stock.getCurrentPrice())).append(" | ").append(
                                String.format("%.2f", stock.getChangePct())).append("%\n");
                    }
                }
                TelegramAlertService.send(telegramMsgFivrMinGainer.toString());


            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 5, TimeUnit.MINUTES);
    }

    public static void startEMAandVWAPScheduler(Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {

            try {

                LocalDate date = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                if(LocalTime.now().isAfter(MARKET_CLOSE))
                    System.exit(0);

                //Map<String, FundamentalData> fundamentalDataMap = StockDailyFeatureRepository.loadFundamentals(DB.get());

                ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory = DataLoader.loadMinuteHistory(DB.get(),
                        LocalDate.now().minusDays(1),LocalDate.now(), LocalTime.of(15, 10), LocalTime.of(15, 30), LocalTime.of(9, 15), LocalTime.of(15, 10));

                calculateEMAandVWAP(null, previousDayDataMap, fundamentalDataMap, minuteHistory, 1.5);


            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 3, TimeUnit.MINUTES);
    }

    public static void getCalculationStrongBuy(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory, double maxStopLossPercent) {

        minuteHistory.forEach((symbol, minuteCandles) -> {

            // =====================================================
            // GET DATA
            // =====================================================

            PreviousDayData prev = previousDayDataMap.get(symbol);

            StockDailyFeature feature = stockDailyFeatureMap.get(symbol);

            FundamentalData fundamental = fundamentalDataMap.get(symbol);

            if (prev == null || feature == null || fundamental == null || minuteCandles == null || minuteCandles.isEmpty()) {

                return;
            }

            // =====================================================
            // BASIC FILTER
            // =====================================================

            if (prev.getClosePrice() < 50) {
                return;
            }

            double previousDayHigh = prev.getHighPrice();

            if (previousDayHigh <= 0) {
                return;
            }

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);

            // Need previous 2 candles
            if (candles.size() < 3) {
                return;
            }

            // =====================================================
            // SCAN CANDLES
            // =====================================================

            for (int i = 2; i < candles.size(); i++) {

                MinuteCandle candle = candles.get(i);

                MinuteCandle prev1 = candles.get(i - 1);

                MinuteCandle prev2 = candles.get(i - 2);

                // =================================================
                // TIME
                // =================================================

                if (candle.time.toLocalTime().isBefore(LocalTime.of(9, 20))) {

                    continue;
                }

                // =================================================
                // BASIC CANDLE DATA
                // =================================================

                double open = candle.open;

                double high = candle.high;

                double low = candle.low;

                double close = candle.close;

                if (open <= 0 || high <= 0 || low <= 0 || close <= 0) {

                    continue;
                }

                double range = high - low;

                if (range <= 0) {
                    continue;
                }

                double body = Math.abs(close - open);

                if (body <= 0) {
                    continue;
                }

                // =================================================
                // FUNDAMENTAL FILTER
                // =================================================

                double marketCap = fundamental.getMarketCap();

                double pe = fundamental.getPeRatio();

                /*
                 * Keep only reasonably large companies.
                 *
                 * Change 5000 to 10000 if you want
                 * even fewer / stronger stocks.
                 */
                if (marketCap < 5000) {
                    continue;
                }

                /*
                 * Ignore negative PE and very expensive
                 * companies.
                 */
                if (pe <= 0 || pe > 40) {
                    continue;
                }

                // =================================================
                // EMA50 TREND FILTER
                // =================================================

                double ema50 = feature.getEma50();

                if (ema50 <= 0) {
                    continue;
                }

                /*
                 * We only want stocks already trading
                 * above EMA50.
                 */
                if (close <= ema50) {
                    continue;
                }

                // =================================================
                // SUPPORT / RESISTANCE
                // =================================================

                double support = feature.getSupportPrice();

                double resistance = feature.getResistancePrice();

                if (support <= 0 || resistance <= 0) {

                    continue;
                }

                if (resistance <= close) {
                    continue;
                }

                double distanceToSupport = (close - support) * 100.0 / close;

                double distanceToResistance = (resistance - close) * 100.0 / close;

                /*
                 * Price should be reasonably close
                 * to support.
                 */
                boolean nearSupport = distanceToSupport >= 0 && distanceToSupport <= 1.5;

                if (!nearSupport) {
                    continue;
                }

                /*
                 * We need enough room to resistance.
                 */
                if (distanceToResistance < 3.0) {
                    continue;
                }

                // =================================================
                // B/S RATIO
                // =================================================

                double bs = candle.buySellRatio;

                double bs1 = prev1.buySellRatio;

                double bs2 = prev2.buySellRatio;

                if (bs <= 0 || bs1 <= 0 || bs2 <= 0) {

                    continue;
                }

                /*
                 * HARD FILTER:
                 *
                 * Buyers must clearly dominate.
                 */
                if (bs < 2.0) {
                    continue;
                }

                /*
                 * Buying pressure must be increasing.
                 *
                 * Example:
                 *
                 * 1.20 -> 1.60 -> 2.30
                 *
                 * GOOD
                 */
                boolean increasingBuying = bs > bs1 && bs1 > bs2;

                if (!increasingBuying) {
                    continue;
                }

                double ratioIncrease = (bs - bs1) * 100.0 / bs1;

                /*
                 * At least 10% increase.
                 */
                if (ratioIncrease < 10.0) {
                    continue;
                }

                // =================================================
                // BULLISH CANDLE
                // =================================================

                if (close <= open) {
                    continue;
                }

                // =================================================
                // CANDLE BODY
                // =================================================

                double bodyPercent = body * 100.0 / open;

                /*
                 * Minimum meaningful movement.
                 */
                if (bodyPercent < 0.30) {
                    continue;
                }

                // =================================================
                // WICKS
                // =================================================

                double upperWick = high - close;

                double lowerWick = open - low;

                /*
                 * Avoid strong rejection from the top.
                 */
                if (upperWick > body) {
                    continue;
                }

                // =================================================
                // CLOSE POSITION
                // =================================================

                double closePosition = (close - low) / range;

                /*
                 * Close should be in upper 30%
                 * of candle.
                 */
                if (closePosition < 0.70) {
                    continue;
                }

                // =================================================
                // PREVIOUS CANDLE BREAKOUT
                // =================================================

                /*
                 * Current candle must close above
                 * previous candle high.
                 */
                if (close <= prev1.high) {
                    continue;
                }

                // =================================================
                // PREVIOUS DAY HIGH CONFIRMATION
                // =================================================

                /*
                 * Since this is a PDH strategy,
                 * price must break previous day high.
                 */
                if (close <= previousDayHigh) {
                    continue;
                }

                double pdhBreakPercent = (close - previousDayHigh) * 100.0 / previousDayHigh;

                /*
                 * Avoid buying a stock that is already
                 * too far above PDH.
                 *
                 * First breakout is preferred.
                 */
                if (pdhBreakPercent > 2.0) {
                    continue;
                }

                // =================================================
                // VOLUME CONFIRMATION
                // =================================================

                if (prev1.volume <= 0 || prev2.volume <= 0 || candle.volume <= 0) {

                    continue;
                }

                double avgPreviousVolume = (prev1.volume + prev2.volume) / 2.0;

                if (avgPreviousVolume <= 0) {
                    continue;
                }

                double volumeRatio = candle.volume / avgPreviousVolume;

                /*
                 * Current candle volume must be
                 * at least 20% higher.
                 */
                if (volumeRatio < 1.20) {
                    continue;
                }

                // =================================================
                // STRUCTURAL STOP LOSS
                // =================================================

                /*
                 * Stop below the stronger support structure.
                 */
                double structuralLow = Math.min(candle.low, support);

                /*
                 * 0.25% safety buffer.
                 */
                double stopBuffer = close * 0.0025;

                double stopLoss = structuralLow - stopBuffer;

                if (stopLoss <= 0 || stopLoss >= close) {

                    continue;
                }

                // =================================================
                // STOP LOSS %
                // =================================================

                double stopLossPercent = (close - stopLoss) * 100.0 / close;

                /*
                 * Don't accept large risk.
                 */
                if (stopLossPercent <= 0 || stopLossPercent > maxStopLossPercent) {

                    continue;
                }

                /*
                 * Don't accept an unrealistically tiny stop.
                 */
                if (stopLossPercent < 0.20) {
                    continue;
                }

                // =================================================
                // TARGET
                // =================================================

                /*
                 * Target slightly before resistance.
                 */
                double target = resistance * 0.995;

                if (target <= close) {
                    continue;
                }

                // =================================================
                // RISK / REWARD
                // =================================================

                double risk = close - stopLoss;

                double reward = target - close;

                if (risk <= 0 || reward <= 0) {

                    continue;
                }

                double riskReward = reward / risk;

                /*
                 * Minimum 1:2.
                 */
                if (riskReward < 2.0) {
                    continue;
                }

                // =================================================
                // FINAL SCORE
                // =================================================

                int score = 0;

                // -----------------------------------------------
                // FUNDAMENTALS
                // -----------------------------------------------

                if (marketCap >= 10000) {
                    score += 10;
                } else {
                    score += 5;
                }

                if (pe <= 30) {
                    score += 5;
                }

                // -----------------------------------------------
                // TREND
                // -----------------------------------------------

                score += 10;

                double emaDistance = (close - ema50) * 100.0 / close;

                if (emaDistance >= 2.0) {
                    score += 5;
                }

                // -----------------------------------------------
                // SUPPORT
                // -----------------------------------------------

                if (distanceToSupport <= 0.75) {
                    score += 10;
                } else {
                    score += 5;
                }

                // -----------------------------------------------
                // BUYING PRESSURE
                // -----------------------------------------------

                if (bs >= 3.0) {
                    score += 15;
                } else {
                    score += 10;
                }

                if (ratioIncrease >= 25.0) {
                    score += 10;
                } else {
                    score += 5;
                }

                // -----------------------------------------------
                // VOLUME
                // -----------------------------------------------

                if (volumeRatio >= 1.50) {
                    score += 10;
                } else {
                    score += 5;
                }

                // -----------------------------------------------
                // CANDLE
                // -----------------------------------------------

                if (bodyPercent >= 1.0) {
                    score += 5;
                }

                if (closePosition >= 0.85) {
                    score += 5;
                }

                // -----------------------------------------------
                // RESISTANCE
                // -----------------------------------------------

                if (distanceToResistance >= 5.0) {
                    score += 10;
                } else {
                    score += 5;
                }

                // -----------------------------------------------
                // RR
                // -----------------------------------------------

                if (riskReward >= 4.0) {
                    score += 10;

                } else if (riskReward >= 3.0) {
                    score += 7;

                } else {
                    score += 5;
                }

                // =================================================
                // FINAL QUALITY FILTER
                // =================================================

                /*
                 * Only strong setups.
                 */
                if (score < 80) {
                    continue;
                }

                // =================================================
                // PRINT SIGNAL
                // =================================================

                System.out.println(String.format(

                        "🚨 %s | %s | SCORE %d | " + "Entry %.2f | " + "SL %.2f (-%.2f%%) | " + "Target %.2f | " + "RR 1:%.2f | " + "PDH %.2f (+%.2f%%) | " + "Support %.2f (%.2f%%) | " + "Resistance %.2f (%.2f%%) | " + "B/S %.2f (+%.1f%%) | " + "Volume %.2fx | " + "EMA50 %.2f | " + "PE %.2f | " + "MCap %.0f",

                        candle.time, symbol, score,

                        close,

                        stopLoss, stopLossPercent,

                        target, riskReward,

                        previousDayHigh, pdhBreakPercent,

                        support, distanceToSupport,

                        resistance, distanceToResistance,

                        bs, ratioIncrease,

                        volumeRatio,

                        ema50,

                        pe, marketCap));

                /*
                 * Only first valid signal for this symbol.
                 */
                break;
            }
        });
    }

    private static boolean isPriceContinuouslyIncreasing(List<CandleNMin> candles, int numberOfCandles) {

        if (candles == null || candles.size() < numberOfCandles) {
            return false;
        }

        int startIndex = candles.size() - numberOfCandles;

        for (int i = startIndex + 1; i < candles.size(); i++) {

            double previousPrice = candles.get(i - 1).getClose();
            double currentPrice = candles.get(i).getClose();

            if (currentPrice <= previousPrice) {
                return false;
            }
        }

        return true;
    }

    public static void calculateEMAandVWAP(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory, double maxStopLossPercent) {

        List<Map.Entry<String, CandleNMin>> results = new ArrayList<>();

        minuteHistory.forEach((symbol, minuteCandles) -> {

                List<MinuteCandle> candles = new ArrayList<>(minuteCandles);
                List<CandleNMin> candleNMins =  calculateTimeBasedEmaAndVwap(candles,3);
                //print5MinCandles(candles5mins);
                CandleNMin candle = candleNMins.getLast();
                double stockPrice = candle.getClose();
                FundamentalData stockFundamental = fundamentalDataMap.get(symbol);
                PreviousDayData previousDayData = previousDayDataMap.get(symbol);
                if (previousDayData == null) {
                    return;
                }

                double stockClosePrice = previousDayData.getClosePrice();
                double percentageChange = 0.0;

                if (stockClosePrice > 0) {
                    percentageChange = ((stockPrice - stockClosePrice) / stockClosePrice) * 100.0;
                }
                if(stockPrice > candle.getEma9() && stockPrice > candle.getVwap() && stockPrice > 500 && stockPrice < 2500 &&  (stockFundamental != null && stockFundamental.getMarketCap() > 1500) && candle.getCumulativeVolume() > 100000 && percentageChange > 2.0){
                    //System.out.println(symbol + " " + candle.toString());
                    //results.add(Map.entry(symbol, candle));
                    boolean continuouslyIncreasing = isPriceContinuouslyIncreasing(candleNMins,3);

                    if (continuouslyIncreasing) {
                        results.add(
                                Map.entry(symbol, candle)
                        );
                    }
                }


        });

        results.sort(
                Comparator.comparingDouble(
                        (Map.Entry<String, CandleNMin> entry) -> {
                            double currentPrice = entry.getValue().getClose();
                            double previousClose = previousDayDataMap.get(entry.getKey()).getClosePrice();
                            return ((currentPrice - previousClose) / previousClose) * 100.0;
                        }
                ).reversed()
        );

        // Print results
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Scanned at : " + LocalDateTime.now().format(formatter));
        results.forEach(entry -> {

            String symbol = entry.getKey();
            CandleNMin candle = entry.getValue();
            PreviousDayData previousDayData = previousDayDataMap.get(symbol);

            double stockPrice = candle.getClose();
            double previousClose = previousDayData.getClosePrice();

            double percentageChange =
                    ((stockPrice - previousClose) / previousClose) * 100.0;

            System.out.println(
                    symbol
                            + " ------ Price: " + candle.getClose()
                            + " ------ Percentage Change: " + String.format("%.2f", percentageChange) + "%"
                            + " ------ EMA9: " + candle.getEma9()
                            + " ------ VWAP: " + candle.getVwap()
                            + " ------ Volume: " + candle.getCumulativeVolume()
            );
        });
    }


    public static void print5MinCandles(List<CandleNMin> candles) {

        System.out.printf(
                "%-10s %-8s %-8s %-8s %-8s %-10s %-10s %-10s%n",
                "Time", "Open", "High", "Low", "Close",
                "Volume", "EMA9", "VWAP"
        );

        for (CandleNMin c : candles) {

            System.out.printf(
                    "%-10s %-8.2f %-8.2f %-8.2f %-8.2f %-10.0f %-10.2f %-10.2f%n",
                    c.getTime().toLocalTime().toString().substring(0, 5),
                    c.getOpen(),
                    c.getHigh(),
                    c.getLow(),
                    c.getClose(),
                    c.getVolume(),
                    c.getEma9(),
                    c.getVwap()
            );
        }
    }

    public static List<CandleNMin> calculateTimeBasedEmaAndVwap(
            List<MinuteCandle> minuteCandles,int minutes) {

        // 1. Group minute candles into 5-minute buckets
        Map<LocalDateTime, List<MinuteCandle>> grouped =
                minuteCandles.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getTime()
                                        .withMinute((c.getTime().getMinute() / minutes) * minutes)
                                        .withSecond(0)
                                        .withNano(0),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        List<CandleNMin> result = new ArrayList<>();

        double ema9 = 0.0;
        double cumulativePV = 0.0;
        double cumulativeVolume = 0.0;

        LocalDate currentDate = null;

        // EMA9 alpha
        double alpha = 2.0 / (9.0 + 1.0);   // 0.2

        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : grouped.entrySet()) {

            LocalDateTime bucketTime = entry.getKey();
            List<MinuteCandle> candles = entry.getValue();

            candles.sort(Comparator.comparing(MinuteCandle::getTime));

            // Reset VWAP and EMA at new trading day
            if (!bucketTime.toLocalDate().equals(currentDate)) {
                currentDate = bucketTime.toLocalDate();

                cumulativePV = 0.0;
                cumulativeVolume = 0.0;

                ema9 = 0.0;
            }

            // -----------------------------
            // Build 5-minute candle
            // -----------------------------

            double open = candles.get(0).getOpen();

            double high = candles.stream()
                    .mapToDouble(MinuteCandle::getHigh)
                    .max()
                    .orElse(open);

            double low = candles.stream()
                    .mapToDouble(MinuteCandle::getLow)
                    .min()
                    .orElse(open);

            double close = candles.get(candles.size() - 1).getClose();

            double volume = candles.stream()
                    .mapToDouble(MinuteCandle::getVolume)
                    .sum();

            // -----------------------------
            // EMA 9
            // -----------------------------

            if (ema9 == 0.0) {
                ema9 = close;
            } else {
                ema9 = (close * alpha) + (ema9 * (1.0 - alpha));
            }

            // -----------------------------
            // VWAP
            // Typical price = (H + L + C) / 3
            // -----------------------------

            double typicalPrice = (high + low + close) / 3.0;

            cumulativePV += typicalPrice * volume;
            cumulativeVolume += volume;

            double vwap = cumulativeVolume > 0
                    ? cumulativePV / cumulativeVolume
                    : close;

            // -----------------------------
            // Result
            // -----------------------------

            CandleNMin candle = new CandleNMin();

            candle.setTime(bucketTime);
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            candle.setVolume(volume);
            candle.setEma9(ema9);
            candle.setVwap(vwap);
            candle.setCumulativeVolume(cumulativeVolume);


            result.add(candle);
        }

        return result;
    }

    public static void getCalculationPrevDayHighBreak(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, Map<String, FundamentalData> fundamentalDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory, double crossPercent, double targetPercent, double maxStopLossPercent) {


        Set<String> symbolSet = new HashSet<>();

        minuteHistory.forEach((symbol, minuteCandles) -> {


            PreviousDayData prev = previousDayDataMap.get(symbol);
            StockDailyFeature feature = stockDailyFeatureMap.get(symbol);
            double firstCandleClose = 0;

            if (prev == null || feature == null || minuteCandles == null || minuteCandles.isEmpty()) {
                return;
            }

            if (prev.getClosePrice() < 50 || prev.getClosePrice() > 3000) {
                return;
            }

            double previousDayHigh = prev.getHighPrice();

            if (previousDayHigh <= 0) {
                return;
            }

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);


            // =========================================================
            // PROCESS 1-MINUTE CANDLES
            // =========================================================

            int counter =0;
            for (int i = 0; i < candles.size(); i++) {
                counter++;

                MinuteCandle candle = candles.get(i);

                if(counter==candles.size()){
                    lastPriceMap.put(symbol, candle.close);
                }
                highPriceMap.merge(symbol, candle.high, Math::max);
                if (candle == null) {
                    continue;
                }

                // Ignore pre-market
                if (candle.time.toLocalTime().isBefore(LocalTime.of(9, 15))) {
                    continue;
                }

                if (symbolSet.contains(symbol))
                    return;

                // Need at least 2 previous candles
                if (i < 2) {
                    continue;
                }



                MinuteCandle previousCandle = candles.get(i - 1);

                MinuteCandle prev2Candle = candles.get(i - 2);

                // MinuteCandle prev3Candle = candles.get(i - 3);


                // =====================================================
                // BASIC CANDLE DATA
                // =====================================================

                Position position = openPositions.get(symbol);

                if (position != null) {

                    double currentProfit =
                            position.profit(candle.high);

                    // ==========================================
                    // ACTIVATE TRAILING STOP AFTER ₹1000 PROFIT
                    // ==========================================

                  /* if (!position.trailingActive
                            && currentProfit >= TARGET_PROFIT) {

                        position.trailingActive = true;
                        position.highestProfit = currentProfit;

                        System.out.println(
                                "TRAILING ACTIVATED | "
                                        + candle.time + " | "
                                        + symbol
                                        + " | Profit=" + currentProfit
                        );
                    }*/

                    // ==========================================
                    // UPDATE HIGHEST PROFIT
                    // ==========================================

                    if (position.trailingActive
                            && currentProfit > position.highestProfit) {

                        position.highestProfit = currentProfit;
                    }

                    // ==========================================
                    // EXIT IF PROFIT FALLS ₹200 FROM HIGH
                    // ==========================================
                    double drawdown =
                            position.highestProfit - currentProfit;

                    double allowedDrawdown =
                            position.highestProfit * 0.1;

                    /*if (position.trailingActive
                            && drawdown >= allowedDrawdown) {

                        System.out.println(
                                "EXIT | "
                                        + candle.time + " | "
                                        + symbol
                                        + " | Entry=" + position.entryPrice
                                        + " | Exit=" + candle.close
                                        + " | Qty=" + position.quantity
                                        + " | Profit=" + currentProfit
                                        + " | HighestProfit=" + position.highestProfit
                                        + " | Drawdown="
                                        + (position.highestProfit - currentProfit)
                                        + " | EntryTime=" + position.entryTime
                        );
                        openPositions.remove(symbol);
                        totalProfit += currentProfit;
                        totalCapital += position.capital;
                    }*/


                }

                double range = candle.high - candle.low;

                if (range <= 0) {
                    continue;
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


                boolean notOverExtended = distanceAbovePDH <= Math.max(crossPercent, 1.0);


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

                double avgPrevVolume =
                        (prev2Candle.volume + previousCandle.volume) / 2.0;

                double volumeSpikeRatio =
                        candle.volume / avgPrevVolume;
                double prevVolumeRatio =
                        (double) previousCandle.volume / prev2Candle.volume;
                int score2=0;
                if (priceIncreasePercent >= 1.5)
                    score2 += 20;

                if (priceIncreasePercent >= 2.0)
                    score2 += 10;

                if (priceIncreasePercent >= 2.5)
                    score2 += 10;
                if (prevVolumeRatio >= 10)
                    score2 += 20;

                if (prevVolumeRatio >= 15)
                    score2 += 10;

                if (prevVolumeRatio >= 20)
                    score2 += 10;
                if (candle.close > previousCandle.close)
                    score2 += 10;

                if (candle.close > candle.open)
                    score2 += 10;
                if(veryStrongBody && candle.volume >5000 && ratioIncreasing && previousCandle.volume>0 && prev2Candle.volume>0 && priceIncreasePercent > 1 && fundamentalData != null && fundamentalData.getMarketCap()>2000 && prevVolumeRatio>10){

                    System.out.println(    candle.time + " | "
                            + pattern + " | "
                            + symbol + " | "
                            + previousCandle.close + " | " + candle.close + " | "
                            + prev2Candle.volume + " | " +
                            previousCandle.volume + " | " +
                            candle.volume + " |  prevVolumeRatio = " +  prevVolumeRatio );
                    Position existPostion = openPositions.get(symbol);
                    if (existPostion != null) {

                        double currentProfit =
                                existPostion.profit(candle.high);
                        if(currentProfit>1000){
                            System.out.println(
                                    "EXIT | "
                                            + candle.time + " | "
                                            + symbol
                                            + " | Entry=" + position.entryPrice
                                            + " | Exit=" + candle.close
                                            + " | Qty=" + position.quantity
                                            + " | Profit=" + currentProfit
                                            + " | HighestProfit=" + position.highestProfit
                                            + " | Drawdown="
                                            + (position.highestProfit - currentProfit)
                                            + " | EntryTime=" + position.entryTime
                            );
                            openPositions.remove(symbol);
                            totalProfit += currentProfit;
                            totalCapital += position.capital;
                        }
                    }


                    int QUANTITY = (int) (CAPITAL_PER_TRADE / candle.close);

                    if (QUANTITY <= 0) {
                        return;
                    }
                    Position newPosition =
                            new Position(
                                    symbol,
                                    candle.close,
                                    QUANTITY,
                                    candle.time
                            );

                    openPositions.put(symbol, newPosition);
                    totalCapital-=CAPITAL_PER_TRADE;

                }

                /*if (fundamentalData != null && fundamentalData.getMarketCap()>2000 && priceIncreasePercent > 1
                        && candle.volume > 10000
                        && volumeSpikeRatio >= 1.5) {

                    symbolSet.add(symbol);

                    System.out.println(
                            candle.time + " | "
                                    + pattern + " | "
                                    + symbol + " | "
                                    + previousCandle.close + " | "
                                    + candle.close + " | "
                                    + prev.getDeliveryPct() + " | "
                                    + String.format("%.2f", priceIncreasePercent) + "%"
                                    + " | Vol=" + candle.volume
                                    + " | AvgPrevVol=" + avgPrevVolume
                                    + " | VolSpike=" + String.format("%.2f", volumeSpikeRatio) + "x"
                    );
                }*/


                if (pattern != null && feature.getSupportPrice() > 0 && candle.getClose() > candle.getOpen() && candle.getClose() > feature.getSupportPrice() && ratioIncreasing && candle.close > prev2Candle.high && candle.close > previousCandle.high && (fundamentalData != null && fundamentalData.getMarketCap() > 2000 && score > 10)) {
                    /*symbolSet.add(symbol);
                    System.out.println(
                        candle.time + " | " + symbol + " | Pattern=" + pattern + " | Score=" + score + " | Open=" + String.format(
                            "%.2f", candle.getOpen()) + " | High=" + String.format("%.2f",
                            candle.getHigh()) + " | Close=" + String.format("%.2f",
                            candle.getClose()) + " | Low=" + String.format("%.2f",
                            candle.getLow()) + " | Support=" + String.format("%.2f",
                            feature.getSupportPrice()) + " | DistanceToSupport=" + String.format("%.2f",
                            distanceToSupport) + " | Resistance=" + String.format("%.2f",
                            feature.getResistancePrice()) + " | B/S=" + String.format("%.2f",
                            currentBS) + " | PrevDayHigh=" + String.format("%.2f", previousDayHigh));*/
                }


                // =====================================================
                // BUY SIGNAL
                // =====================================================

                /*if (validSetup) {

                    double entry = candle.close;


                    // =================================================
                    // STOP LOSS
                    // =================================================

                    double initialStopLoss = entry * (1.0 - maxStopLossPercent / 100.0);


                    if (initialStopLoss <= 0 || initialStopLoss >= entry) {

                        continue;
                    }


                    // =================================================
                    // TARGET
                    // =================================================

                    double initialTarget = entry * (1.0 + targetPercent / 100.0);


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

                    break;
                }*/
            }
        });
    }

    public static void getCalculation(Map<String, StockDailyFeature> stockDailyFeatureMap, Map<String, PreviousDayData> previousDayDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory, double crossPercent, double targetPercent, double maxStopLossPercent) {

        final double SCORE_THRESHOLD = 70.0;
        final double RISK_REWARD = 3.0;

        // Maximum acceptable stop distance from entry
        final double MAX_STOP_PERCENT = 2.0;

        minuteHistory.forEach((symbol, minuteCandles) -> {

            if (previousDayDataMap.get(symbol) == null) {
                return;
            }

            if (minuteCandles == null || minuteCandles.size() < 3) {
                return;
            }

            PreviousDayData prevDaClose = previousDayDataMap.get(symbol);

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);

            // =====================================================
            // FIND FIRST CANDLE AT / AFTER 09:15
            // =====================================================

            int startIndex = -1;

            for (int i = 0; i < candles.size(); i++) {

                if (!candles.get(i).time.toLocalTime().isBefore(LocalTime.of(9, 15))) {

                    startIndex = i;
                    break;
                }
            }

            if (startIndex == -1 || candles.size() - startIndex < 3) {

                return;
            }

            // =====================================================
            // FIRST SIGNAL
            // =====================================================

            int count = 0;

            MinuteCandle firstCandle1 = null;
            MinuteCandle firstCandle2 = null;
            MinuteCandle firstCandle3 = null;

            double firstScore = 0;
            double firstPriceChange = 0;

            double firstBodyRatio1 = 0;
            double firstBodyRatio2 = 0;
            double firstBodyRatio3 = 0;

            double firstEntry = 0;
            double firstStopLoss = 0;
            double firstTarget = 0;
            double firstRisk = 0;
            double firstRiskPercent = 0;
            double firstTargetPercent = 0;

            // =====================================================
            // SCAN EVERY 3-CANDLE WINDOW
            // =====================================================

            for (int i = startIndex; i <= candles.size() - 3; i++) {

                MinuteCandle c1 = candles.get(i);
                MinuteCandle c2 = candles.get(i + 1);
                MinuteCandle c3 = candles.get(i + 2);

                if (c1.close < prevDaClose.getClosePrice())
                    break;

                double score = 0.0;

                // =================================================
                // 1. GREEN CANDLES
                // =================================================

                int greenCount = 0;

                if (c1.close > c1.open)
                    greenCount++;

                if (c2.close > c2.open)
                    greenCount++;

                if (c3.close > c3.open)
                    greenCount++;

                score += greenCount * 5.0;

                // =================================================
                // 2. PRICE INCREASING
                // =================================================

                if (c2.close > c1.close) {
                    score += 7.5;
                }

                if (c3.close > c2.close) {
                    score += 7.5;
                }

                // =================================================
                // 3. 3-CANDLE PRICE MOVE
                // =================================================

                double priceMove = c3.close - c1.open;

                double priceChange = (priceMove / c1.open) * 100.0;

                if (priceMove >= 1.0) {
                    score += 10.0;
                } else if (priceMove > 0) {
                    score += 5.0;
                }

                if (priceChange >= 1.0) {
                    score += 5.0;
                } else if (priceChange >= 0.30) {
                    score += 2.5;
                }

                // =================================================
                // 4. BODY / RANGE
                // =================================================

                double body1 = Math.abs(c1.close - c1.open);

                double body2 = Math.abs(c2.close - c2.open);

                double body3 = Math.abs(c3.close - c3.open);

                double range1 = c1.high - c1.low;

                double range2 = c2.high - c2.low;

                double range3 = c3.high - c3.low;

                if (range1 <= 0 || range2 <= 0 || range3 <= 0) {

                    continue;
                }

                double bodyRatio1 = body1 / range1;

                double bodyRatio2 = body2 / range2;

                double bodyRatio3 = body3 / range3;

                if (bodyRatio1 >= 0.60)
                    score += 5.0;
                else if (bodyRatio1 >= 0.40)
                    score += 2.5;

                if (bodyRatio2 >= 0.60)
                    score += 2.5;
                else if (bodyRatio2 >= 0.40)
                    score += 1.25;

                if (bodyRatio3 >= 0.60)
                    score += 5.0;
                else if (bodyRatio3 >= 0.40)
                    score += 2.5;

                // =================================================
                // 5. BUY QUANTITY
                // =================================================

                if (c3.totalBuyQty > c1.totalBuyQty) {
                    score += 10.0;
                }

                if (c2.totalBuyQty > c1.totalBuyQty && c3.totalBuyQty > c2.totalBuyQty) {

                    score += 5.0;
                }

                // =================================================
                // 6. VOLUME
                // =================================================

                if (c3.volume > c1.volume) {
                    score += 7.0;
                }

                if (c2.volume > c1.volume && c3.volume > c2.volume) {

                    score += 3.0;
                }

                // =================================================
                // 7. BUY > SELL
                // =================================================

                if (c3.totalBuyQty > c3.totalSellQty) {
                    score += 10.0;
                }

                // =================================================
                // 8. BUY / SELL RATIO
                // =================================================

                if (c3.buySellRatio >= 2.0 && c3.buySellRatio <= 5.0) {

                    score += 10.0;

                } else if (c3.buySellRatio >= 1.5) {

                    score += 7.0;

                } else if (c3.buySellRatio >= 1.20) {

                    score += 5.0;
                }

                // =================================================
                // 9. WICK QUALITY
                // =================================================

                double upperWick1 = c1.high - Math.max(c1.open, c1.close);

                double lowerWick1 = Math.min(c1.open, c1.close) - c1.low;

                double upperWick2 = c2.high - Math.max(c2.open, c2.close);

                double lowerWick2 = Math.min(c2.open, c2.close) - c2.low;

                double upperWick3 = c3.high - Math.max(c3.open, c3.close);

                double lowerWick3 = Math.min(c3.open, c3.close) - c3.low;

                boolean badWick = upperWick1 > body1 * 2.5 || lowerWick1 > body1 * 2.5 || upperWick2 > body2 * 2.5 || lowerWick2 > body2 * 2.5 || upperWick3 > body3 * 2.5 || lowerWick3 > body3 * 2.5;

                if (!badWick) {
                    score += 5.0;
                }

                // =================================================
                // 10. EXTREME RANGE FILTER
                // =================================================

                double maxRange = Math.max(range1, Math.max(range2, range3));

                double minRange = Math.min(range1, Math.min(range2, range3));

                if (minRange > 0 && maxRange / minRange > 5.0) {

                    continue;
                }

                // =================================================
                // SCORE
                // =================================================

                if (score > 100.0) {
                    score = 100.0;
                }

                if (score < SCORE_THRESHOLD || c3.buySellRatio < 1.2 || c3.close < 50) {
                    continue;
                }

                // =================================================
                // ENTRY
                // =================================================

                double entry = c3.close;

                // =================================================
                // STOP LOSS
                //
                // Lowest low of the 3 candles
                // =================================================

                double stopLoss = Math.min(prevDaClose.getLowPrice(), Math.min(c1.low, Math.min(c2.low, c3.low)));

                // Make sure SL is below entry
                if (stopLoss >= entry || entry < prevDaClose.getClosePrice()) {
                    continue;
                }

                // =================================================
                // RISK
                // =================================================

                double risk = entry - stopLoss;

                double riskPercent = (risk / entry) * 100.0;

                // Reject very wide SL
                if (riskPercent > MAX_STOP_PERCENT) {
                    continue;
                }

                // =================================================
                // TARGET
                // =================================================

                double target = entry + (risk * RISK_REWARD);

                // =================================================
                // SIGNAL
                // =================================================

                count++;

                // =================================================
                // SAVE FIRST OCCURRENCE
                // =================================================

                if (count == 1) {

                    firstCandle1 = c1;
                    firstCandle2 = c2;
                    firstCandle3 = c3;

                    firstScore = score;
                    firstPriceChange = priceChange;

                    firstBodyRatio1 = bodyRatio1;
                    firstBodyRatio2 = bodyRatio2;
                    firstBodyRatio3 = bodyRatio3;

                    firstEntry = entry;
                    firstStopLoss = stopLoss;
                    firstTarget = target;
                    firstRisk = risk;
                    firstRiskPercent = riskPercent;
                    firstTargetPercent = targetPercent;
                }
            }

            // =====================================================
            // PRINT FIRST SIGNAL + COUNT
            // =====================================================

            if (count > 0 && firstCandle3 != null) {

                System.out.println(firstCandle3.time + " | " + symbol + " | Score=" + String.format("%.0f",
                        firstScore) + " | Prev Close=" + String.format("%.2f",
                        prevDaClose.getClosePrice()) + " | Entry=" + String.format("%.2f",
                        firstEntry) + " | SL=" + String.format("%.2f", firstStopLoss) + " | Target=" + String.format("%.2f",
                        firstTarget) + " | Risk=" + String.format("%.2f", firstRisk) + " (" + String.format("%.2f",
                        firstRiskPercent) + "%)" + " | TargetMove=" + String.format("%.2f",
                        firstTargetPercent) + "%" + " | 3C=" + String.format("%.2f",
                        firstPriceChange) + "%" + " | BuyQty=" + firstCandle3.totalBuyQty + " | SellQty=" + firstCandle3.totalSellQty + " | Ratio=" + String.format(
                        "%.2f", firstCandle3.buySellRatio) + " | Body=" + String.format("%.0f",
                        firstBodyRatio1 * 100) + "%/" + String.format("%.0f", firstBodyRatio2 * 100) + "%/" + String.format(
                        "%.0f", firstBodyRatio3 * 100) + "%" + " | Count=" + count);

                symbols.add(symbol);
            }
        });
    }

    public static void getCalculationGapUp(Map<String, PreviousDayData> previousDayDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory) {

        final int TOP_N = 5;

        // =========================================================
        // CANDIDATES
        // =========================================================

        List<Candidate> candidates = new ArrayList<>();

        minuteHistory.forEach((symbol, minuteCandles) -> {

            PreviousDayData prev = previousDayDataMap.get(symbol);

            if (prev == null || minuteCandles == null || minuteCandles.isEmpty()) {
                return;
            }

            List<MinuteCandle> candles = new ArrayList<>(minuteCandles);

            // =====================================================
            // GET FIRST 5 MINUTES: 09:15 - 09:19
            // =====================================================

            List<MinuteCandle> opening = new ArrayList<>(5);

            for (MinuteCandle candle : candles) {

                LocalTime time = candle.time.toLocalTime();

                if (time.isBefore(LocalTime.of(9, 15))) {
                    continue;
                }

                if (!time.isBefore(LocalTime.of(9, 20))) {
                    break;
                }

                opening.add(candle);

                if (opening.size() == 5) {
                    break;
                }
            }

            // Need enough data to make a 5-minute decision
            if (opening.size() < 5) {
                return;
            }

            MinuteCandle first = opening.get(0);
            MinuteCandle last = opening.get(opening.size() - 1);

            double previousClose = prev.getClosePrice();

            if (previousClose <= 0 || first.open <= 0) {
                return;
            }

            // =====================================================
            // 1. GAP
            // =====================================================

            double gapPercent = ((first.open - previousClose) / previousClose) * 100.0;

            boolean gapUp = gapPercent > 0;

            // =====================================================
            // 2. PRICE MOVEMENT FROM OPEN
            // =====================================================

            double movePercent = ((last.close - first.open) / first.open) * 100.0;

            // =====================================================
            // 3. OPENING HIGH / LOW
            // =====================================================

            double openingHigh = Double.MIN_VALUE;
            double openingLow = Double.MAX_VALUE;

            long totalVolume = 0;

            int greenCount = 0;
            int higherHighCount = 0;
            int higherLowCount = 0;

            MinuteCandle previous = null;

            for (MinuteCandle c : opening) {

                openingHigh = Math.max(openingHigh, c.high);
                openingLow = Math.min(openingLow, c.low);

                totalVolume += Math.max(0, c.volume);

                if (c.close > c.open) {
                    greenCount++;
                }

                if (previous != null) {

                    if (c.high > previous.high) {
                        higherHighCount++;
                    }

                    if (c.low > previous.low) {
                        higherLowCount++;
                    }
                }

                previous = c;
            }

            double openingRange = openingHigh - openingLow;

            if (openingRange <= 0) {
                return;
            }

            double openingRangePercent = (openingRange / first.open) * 100.0;

            // =====================================================
            // 4. CLOSE POSITION INSIDE 5-MINUTE RANGE
            // =====================================================

            double highRetention = (last.close - openingLow) / openingRange;

            /*
             * Example:
             *
             * 0.90 = close near high
             * 0.50 = middle
             * 0.20 = close near low
             */

            // =====================================================
            // 5. MAX PULLBACK FROM OPENING HIGH
            // =====================================================

            double maxPullback = 0.0;

            for (MinuteCandle c : opening) {

                double pullback = ((openingHigh - c.low) / openingHigh) * 100.0;

                maxPullback = Math.max(maxPullback, pullback);
            }

            // =====================================================
            // 6. VOLUME ACCELERATION
            // =====================================================

            long firstTwoVolume = 0;
            long lastTwoVolume = 0;

            for (int i = 0; i < 2; i++) {
                firstTwoVolume += Math.max(0, opening.get(i).volume);
            }

            for (int i = 3; i < 5; i++) {
                lastTwoVolume += Math.max(0, opening.get(i).volume);
            }

            double volumeAcceleration = 1.0;

            if (firstTwoVolume > 0) {
                volumeAcceleration = (double) lastTwoVolume / firstTwoVolume;
            }

            // =====================================================
            // 7. BUY / SELL PRESSURE
            // =====================================================

            double buySellRatio = last.buySellRatio;

            boolean buyingPressure = last.totalBuyQty > last.totalSellQty;

            // =====================================================
            // 8. PRICE ACCELERATION
            // =====================================================

            double firstMove = ((opening.get(1).close - first.open) / first.open) * 100.0;

            double lastMove = ((last.close - opening.get(3).close) / opening.get(3).close) * 100.0;

            double acceleration = lastMove - firstMove;

            // =====================================================
            // GAP-UP CONTINUATION FILTER
            // =====================================================

            // Gap is mandatory
            if (gapPercent < 2.0) {
                return;
            }

            // Price must still be above the opening price
            if (last.close <= first.open) {
                return;
            }

            // We don't want a strong loss of momentum
            if (acceleration < -0.50) {
                return;
            }

            // Don't accept a stock that gives back too much
            if (highRetention < 0.70) {
                return;
            }

            // =====================================================
            // 9. GAP ACCEPTANCE
            // =====================================================

            /*
             * For a gap-up:
             *
             * We want the price to remain close to / above
             * the opening price.
             */

            boolean gapAccepted = true;

            if (gapUp) {

                double openDistance = ((last.close - first.open) / first.open) * 100.0;

                // Reject gap if it gives back more than 0.5%
                if (openDistance < -0.50) {
                    gapAccepted = false;
                }
            }

            if (!gapAccepted) {
                return;
            }

            // =====================================================
            // 10. MINIMUM MOMENTUM
            // =====================================================

            /*
             * We don't want a stock that simply opened with a gap
             * but is doing nothing afterwards.
             *
             * Either:
             *
             * 1. Strong gap
             * OR
             * 2. Strong movement after open
             */

            boolean strongGap = gapPercent >= 2.0;

            boolean strongOpeningMove = movePercent >= 1.0;

            if (!strongGap && !strongOpeningMove) {
                return;
            }

            // =====================================================
            // 11. SCORE
            // =====================================================

            double score = 0.0;

            // -----------------------------------------------------
            // GAP
            // -----------------------------------------------------

            // GAP: useful, but gap alone is NOT enough
            if (gapPercent >= 3.0 && gapPercent <= 5.0) {
                score += 10.0;
            } else if (gapPercent > 5.0) {
                score += 8.0;
            } else if (gapPercent >= 2.0) {
                score += 7.0;
            } else if (gapPercent >= 1.0) {
                score += 4.0;
            } else if (gapPercent > 0) {
                score += 2.0;
            }
            // -----------------------------------------------------
            // OPENING MOVE
            // -----------------------------------------------------

            if (movePercent >= 2.0) {

                score += 25.0;

            } else if (movePercent >= 1.5) {

                score += 20.0;

            } else if (movePercent >= 1.0) {

                score += 15.0;

            } else if (movePercent >= 0.5) {

                score += 8.0;
            }

            // -----------------------------------------------------
            // HIGH RETENTION
            // -----------------------------------------------------

            if (highRetention >= 0.90) {

                score += 15.0;

            } else if (highRetention >= 0.80) {

                score += 12.0;

            } else if (highRetention >= 0.70) {

                score += 7.0;
            }

            // -----------------------------------------------------
            // HIGHER HIGHS
            // -----------------------------------------------------

            if (higherHighCount >= 3) {

                score += 10.0;

            } else if (higherHighCount >= 2) {

                score += 7.0;

            } else if (higherHighCount >= 1) {

                score += 3.0;
            }

            // -----------------------------------------------------
            // HIGHER LOWS
            // -----------------------------------------------------

            if (higherLowCount >= 3) {

                score += 8.0;

            } else if (higherLowCount >= 2) {

                score += 5.0;
            }

            // -----------------------------------------------------
            // VOLUME ACCELERATION
            // -----------------------------------------------------

            if (volumeAcceleration >= 2.0) {

                score += 10.0;

            } else if (volumeAcceleration >= 1.5) {

                score += 7.0;

            } else if (volumeAcceleration >= 1.2) {

                score += 4.0;
            }

            // -----------------------------------------------------
            // PRICE ACCELERATION
            // -----------------------------------------------------

            if (acceleration >= 0.50) {

                score += 8.0;

            } else if (acceleration >= 0.20) {

                score += 5.0;
            }

            // -----------------------------------------------------
            // BUYING PRESSURE
            // -----------------------------------------------------

            if (buyingPressure) {
                score += 3.0;
            }

            // -----------------------------------------------------
            // BUY / SELL RATIO
            // -----------------------------------------------------

            /*
             * Only confirmation.
             *
             * Do NOT make this a major condition.
             */

            if (buySellRatio >= 1.5) {

                score += 4.0;

            } else if (buySellRatio >= 1.2) {

                score += 2.0;
            }

            // =====================================================
            // 12. STOP LOSS
            // =====================================================

            double entry = last.close;

            /*
             * Use opening low as initial structural SL.
             */

            double stopLoss = openingLow;

            if (stopLoss >= entry) {
                return;
            }

            double risk = entry - stopLoss;

            double riskPercent = (risk / entry) * 100.0;

            // Don't take trades requiring a very wide stop
            if (riskPercent > 2.0) {
                return;
            }

            // =====================================================
            // 13. TARGET
            // =====================================================

            /*
             * Your objective is 3-5%.
             */

            double targetPercent;

            if (score >= 85) {
                targetPercent = 5.0;
            } else if (score >= 75) {
                targetPercent = 4.0;
            } else {
                targetPercent = 3.0;
            }

            double target = entry * (1.0 + targetPercent / 100.0);

            // =====================================================
            // 14. CANDIDATE
            // =====================================================

            Candidate candidate = new Candidate();

            candidate.symbol = symbol;

            candidate.score = Math.min(score, 100.0);

            candidate.entry = entry;
            candidate.stopLoss = stopLoss;
            candidate.target = target;

            candidate.gapPercent = gapPercent;

            candidate.movePercent = movePercent;

            candidate.openingRangePercent = openingRangePercent;

            candidate.highRetention = highRetention;

            candidate.volumeAcceleration = volumeAcceleration;

            candidate.acceleration = acceleration;

            candidate.buySellRatio = buySellRatio;

            candidate.riskPercent = riskPercent;

            candidate.signalTime = last.time;

            candidates.add(candidate);
        });

        // =========================================================
        // SORT
        // =========================================================

        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed());

        // =========================================================
        // TOP 5
        // =========================================================

        int limit = Math.min(TOP_N, candidates.size());

        System.out.println();
        System.out.println("========== TOP " + limit + " OPENING MOMENTUM STOCKS ==========");

        for (int i = 0; i < limit; i++) {

            Candidate c = candidates.get(i);

            System.out.println(String.format(
                    "%d. %-12s | Score=%5.1f | " + "Gap=%6.2f%% | Move5M=%6.2f%% | " + "Range=%5.2f%% | HighRet=%4.2f | " + "VolAcc=%5.2fx | Acc=%5.2f | " + "B/S=%4.2f | Risk=%4.2f%% | " + "Entry=%8.2f | SL=%8.2f | Target=%8.2f",

                    i + 1, c.symbol, c.score,

                    c.gapPercent, c.movePercent, c.openingRangePercent,

                    c.highRetention,

                    c.volumeAcceleration, c.acceleration,

                    c.buySellRatio,

                    c.riskPercent,

                    c.entry, c.stopLoss, c.target));

            symbols.add(c.symbol);
        }

        System.out.println("==========================================");
    }

    public static void getCalculationNew(Map<String, PreviousDayData> previousDayDataMap, ConcurrentHashMap<String, Deque<MinuteCandle>> minuteHistory) {

        minuteHistory.forEach((symbol, candles) -> {

            if (previousDayDataMap.get(symbol) == null || symbols.contains(symbol)) {
                return;
            }

            if (candles == null || candles.size() < 30) {
                return;
            }

            double prevClose = previousDayDataMap.get(symbol).getClosePrice();

            if (prevClose <= 50) {
                return;
            }

            List<MinuteCandle> list = new ArrayList<>(candles);

            for (int i = 0; i < list.size(); i++) {

                MinuteCandle current = list.get(i);

                // Don't scan before enough candles exist
                if (current.time.toLocalTime().isBefore(LocalTime.of(9, 20))) {
                    continue;
                }

                if (current.time.toLocalTime().isAfter(LocalTime.of(9, 45))) {
                    continue;
                }

                double price = current.close;

                double changePercent = ((price - prevClose) / prevClose) * 100.0;

                // Don't chase stocks already moved > 4%

                int score = 0;

                // --------------------------------------------
                // Opening range
                // --------------------------------------------

                double initialHigh = -Double.MAX_VALUE;
                double initialLow = Double.MAX_VALUE;

                for (MinuteCandle c : list) {

                    LocalTime t = c.time.toLocalTime();

                    if (!t.isBefore(LocalTime.of(9, 15)) && t.isBefore(LocalTime.of(9, 20))) {

                        initialHigh = Math.max(initialHigh, c.high);

                        initialLow = Math.min(initialLow, c.low);
                    }
                }

                double distanceToHigh = ((initialHigh - price) / initialHigh) * 100.0;

                if (distanceToHigh >= 0 && distanceToHigh <= 0.5) {

                    score += 15;
                }

                // ------------------------------------------
                // 2. BREAKOUT
                // ------------------------------------------

                boolean breakout = price > initialHigh * 1.001;

                if (breakout) {
                    score += 30;
                }

                // ------------------------------------------
                // 3. VWAP
                // ------------------------------------------

                double vwap = calculateVWAP(list, i);

                if (price > vwap) {
                    score += 15;
                }

                // ------------------------------------------
                // 4. VOLUME
                // ------------------------------------------

                double volumeRatio = calculateRecentVolumeRatio(list, i);

                if (volumeRatio >= 1.2) {
                    score += 10;
                }

                if (volumeRatio >= 1.5) {
                    score += 10;
                }

                // ------------------------------------------
                // 5. DEMAND TREND
                // ------------------------------------------

                boolean demandIncreasing = isDemandIncreasing(list, i);

                if (demandIncreasing) {
                    score += 15;
                }

                // ------------------------------------------
                // 6. HIGHER LOW
                // ------------------------------------------

                boolean higherLow = hasHigherLow(list, i);

                if (higherLow) {
                    score += 10;
                }

                // ------------------------------------------
                // 7. ABSORPTION
                // ------------------------------------------

                boolean absorption = isAbsorption(list, i);

                if (absorption) {
                    score += 15;
                }

                // ------------------------------------------
                // 8. BUY/SELL
                // ------------------------------------------

                if (current.buySellRatio >= 1.2) {
                    score += 5;
                }

                // --------------------------------------------
                // Buy / Sell
                // --------------------------------------------

                double ratio = current.buySellRatio;

                if (ratio >= 1.2) {
                    score += 5;
                }


                boolean breakoutConfirmation = breakout && price > vwap && volumeRatio >= 1.2 && ratio > 1.2;
                // --------------------------------------------
                // FIRST SIGNAL
                // --------------------------------------------

                if (score >= 75 && breakout && price > vwap && volumeRatio >= 1.2 && higherLow) {
                    qualificationMap.put(symbol, i);
                    qualificationTimeMap.put(symbol, current.time);
                }
                Integer qualifiedIndex = qualificationMap.get(symbol);

                if (qualifiedIndex != null && i > qualifiedIndex) {

                    if (breakoutConfirmation) {

                        System.out.println(current.time + " " + symbol + " SIGNAL" + " Price=" + String.format("%.2f",
                                price) + " BS=" + String.format("%.2f", ratio) + " Vol=" + String.format("%.2f",
                                volumeRatio));

                        symbols.add(symbol);
                        qualificationMap.remove(symbol);
                        qualificationTimeMap.remove(symbol);
                        break;
                    }
                }
            }
        });
    }

    private static double calculateRecentVolumeRatio(List<MinuteCandle> candles, int index) {

        if (candles == null || index < 6) {
            return 0.0;
        }

        long recentVolume = 0;
        long previousVolume = 0;

        // Last 3 completed/current candles
        for (int i = index - 2; i <= index; i++) {

            MinuteCandle c = candles.get(i);

            if (c != null) {
                recentVolume += c.volume;
            }
        }

        // 3 candles before that
        for (int i = index - 5; i <= index - 3; i++) {

            MinuteCandle c = candles.get(i);

            if (c != null) {
                previousVolume += c.volume;
            }
        }

        if (previousVolume <= 0) {
            return 0.0;
        }

        return (double) recentVolume / previousVolume;
    }

    private static boolean isDemandIncreasing(List<MinuteCandle> candles, int index) {

        if (candles == null || index < 5) {
            return false;
        }

        double r1 = candles.get(index - 5).buySellRatio;

        double r2 = candles.get(index - 3).buySellRatio;

        double r3 = candles.get(index - 1).buySellRatio;

        /*
         * Demand should be improving.
         */
        boolean increasing = r2 > r1 && r3 > r2;

        /*
         * Don't consider tiny ratios meaningful.
         */
        boolean minimumDemand = r3 >= 1.10;

        return increasing && minimumDemand;
    }

    private static double averageVolume(List<MinuteCandle> candles, int from, int to) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        from = Math.max(0, from);
        to = Math.min(candles.size() - 1, to);

        if (from > to) {
            return 0.0;
        }

        long total = 0;
        int count = 0;

        for (int i = from; i <= to; i++) {

            MinuteCandle c = candles.get(i);

            if (c == null) {
                continue;
            }

            total += c.volume;
            count++;
        }

        return count > 0 ? (double) total / count : 0.0;
    }

    private static double highestHigh(List<MinuteCandle> candles, int from, int to) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        from = Math.max(0, from);
        to = Math.min(candles.size() - 1, to);

        double highest = Double.MIN_VALUE;

        for (int i = from; i <= to; i++) {

            MinuteCandle c = candles.get(i);

            if (c != null) {
                highest = Math.max(highest, c.high);
            }
        }

        return highest == Double.MIN_VALUE ? 0.0 : highest;
    }

    private static double percentChange(List<MinuteCandle> candles, int currentIndex, int periods) {

        if (candles == null || currentIndex < 0 || currentIndex >= candles.size() || periods <= 0) {

            return 0.0;
        }

        int previousIndex = currentIndex - periods;

        if (previousIndex < 0) {
            return 0.0;
        }

        MinuteCandle current = candles.get(currentIndex);

        MinuteCandle previous = candles.get(previousIndex);

        if (current == null || previous == null || previous.close <= 0) {

            return 0.0;
        }

        return ((current.close - previous.close) / previous.close) * 100.0;
    }

    private static boolean hasHigherLow(List<MinuteCandle> candles, int n) {

        if (candles == null || n < 10) {
            return false;
        }

        /*
         * Compare two 5-candle sections.
         *
         * Older section:
         * n-10 -> n-6
         *
         * Recent section:
         * n-5 -> n-1
         */

        double olderLow = lowestLow(candles, n - 10, n - 6);

        double recentLow = lowestLow(candles, n - 5, n - 1);

        if (olderLow <= 0 || recentLow <= 0) {
            return false;
        }

        return recentLow > olderLow * 1.001;
    }

    private static boolean isAbsorption(List<MinuteCandle> candles, int n) {

        if (candles == null || n < 10) {
            return false;
        }

        int start = n - 10;

        int sellingCandles = 0;

        long totalVolume = 0;

        double lowest = Double.MAX_VALUE;
        double highest = Double.MIN_VALUE;

        for (int i = start; i < n; i++) {

            MinuteCandle c = candles.get(i);

            if (c == null) {
                continue;
            }

            totalVolume += c.volume;

            lowest = Math.min(lowest, c.low);

            highest = Math.max(highest, c.high);

            /*
             * Selling pressure.
             */
            if (c.buySellRatio < 0.90) {
                sellingCandles++;
            }
        }

        if (totalVolume <= 0 || lowest == Double.MAX_VALUE || highest == Double.MIN_VALUE) {

            return false;
        }

        double rangePercent = ((highest - lowest) / lowest) * 100.0;

        /*
         * At least 4 of last 10 candles
         * show sell-side pressure.
         */
        boolean sellingPressure = sellingCandles >= 4;

        /*
         * Price remains compressed.
         */
        boolean priceHolding = rangePercent <= 1.5;

        return sellingPressure && priceHolding;
    }

    private static double calculateVWAP(List<MinuteCandle> candles, int endIndex) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        endIndex = Math.min(endIndex, candles.size() - 1);

        double pv = 0.0;
        long volume = 0;

        for (int i = 0; i <= endIndex; i++) {

            MinuteCandle c = candles.get(i);

            if (c == null || c.volume <= 0) {
                continue;
            }

            double typicalPrice = (c.high + c.low + c.close) / 3.0;

            pv += typicalPrice * c.volume;
            volume += c.volume;
        }

        return volume > 0 ? pv / volume : 0.0;
    }

    private static boolean failedBreakdown(List<MinuteCandle> candles, int n) {

        if (candles == null || n < 15) {
            return false;
        }

        /*
         * Support from older candles.
         */
        double support = lowestLow(candles, n - 15, n - 6);

        if (support <= 0) {
            return false;
        }

        boolean brokeSupport = false;

        /*
         * Last 5 candles.
         */
        for (int i = n - 5; i < n; i++) {

            MinuteCandle c = candles.get(i);

            if (c.low < support) {
                brokeSupport = true;
                break;
            }
        }

        if (!brokeSupport) {
            return false;
        }

        /*
         * Current price recovered above support.
         */
        MinuteCandle current = candles.get(n - 1);

        return current.close > support * 1.001;
    }

    private static double lowestLow(List<MinuteCandle> candles, int from, int to) {

        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        from = Math.max(0, from);
        to = Math.min(candles.size() - 1, to);

        if (from > to) {
            return 0.0;
        }

        double lowest = Double.MAX_VALUE;

        for (int i = from; i <= to; i++) {

            MinuteCandle c = candles.get(i);

            if (c == null) {
                continue;
            }

            if (c.low < lowest) {
                lowest = c.low;
            }
        }

        return lowest == Double.MAX_VALUE ? 0.0 : lowest;
    }

    private static double buySellImbalance(MinuteCandle c) {

        if (c == null) {
            return 0.0;
        }

        double buy = c.totalBuyQty;
        double sell = c.totalSellQty;

        double total = buy + sell;

        if (total <= 0) {
            return 0.0;
        }

        return (buy - sell) / total;
    }

    public static void startGapUpScanner() throws Exception, KiteException {
        Map<String, StockMomentum> gapUpSymbolClosePriceMap = getMomentumMap();

        Map<String, StockDailyFeature> stockDailyFeatureMap = StockDailyFeatureRepository.loadCurrentDailyFeatures(
                DB.get(), gapUpSymbolClosePriceMap.keySet());

        System.out.println("========== Filter data from Daily Feature table  ==========");
        Set<String> potentialCandidates = new HashSet<>();
        List<Candidate> candidates = new ArrayList<>();

        stockDailyFeatureMap.forEach((s, feature) -> {

            StockMomentum stockMomentum = gapUpSymbolClosePriceMap.get(s);

            if (feature == null || stockMomentum == null) {
                return;
            }

            double price = stockMomentum.getCurrentClose();


            // =====================================================
            // BASIC DATA VALIDATION
            // =====================================================

            if (price <= 0 || feature.getSma50() <= 0 || feature.getEma20() <= 0) {

                return;
            }


            // =====================================================
            // 1. PRIMARY TREND
            //
            // Price > EMA20 > SMA50
            // =====================================================

            boolean bullishTrend = price > feature.getEma20() && feature.getEma20() > feature.getSma50();

            if (!bullishTrend) {
                return;
            }


            // =====================================================
            // 2. RSI
            //
            // Avoid weak stocks and already extremely overbought
            // stocks.
            // =====================================================

            double rsi = feature.getRsi14();

            if (rsi > 80) {

                return;
            }


            // =====================================================
            // 3. DISTANCE FROM 20-DAY HIGH
            //
            // We want stocks close to their recent high.
            // =====================================================

            double highDistance = feature.getDistanceFrom20dHigh();

            if (highDistance < -5) {
                return;
            }


            // =====================================================
            // 4. EMA20 DISTANCE
            //
            // Don't want a stock barely above EMA20.
            // But also don't want something extremely extended.
            // =====================================================

            double ema20Distance = (price - feature.getEma20()) * 100.0 / feature.getEma20();

            if (ema20Distance < 1.0 || ema20Distance > 10.0) {

                return;
            }


            // =====================================================
            // 5. SCORE
            // =====================================================

            double score = momentumScore(feature, price);


            // =====================================================
            // 6. ADD
            // =====================================================

            candidates.add(new Candidate(s, price, score));


            System.out.printf(
                    "%-15s | " + "Price=%8.2f | " + "SMA50=%8.2f | " + "EMA20=%8.2f | " + "RSI=%5.2f | " + "HighDist=%6.2f%% | " + "EMA20Dist=%6.2f%% | " + "Score=%6.2f%n",

                    s, price, feature.getSma50(), feature.getEma20(), rsi, highDistance, ema20Distance, score);
        });


        // =========================================================
        // RANK
        // =========================================================

        candidates.sort(Comparator.comparingDouble(Candidate::getScore).reversed());


        // =========================================================
        // TOP 20 → LIVETICKER
        // =========================================================

        candidates.stream().forEach(c -> {

            potentialCandidates.add(c.symbol);

            System.out.printf("%-15s | Price=%8.2f | Score=%6.2f%n", c.symbol, c.price, c.score);
        });

        System.out.println("LIVE CANDIDATES = " + potentialCandidates);
        KiteConnect kite = new KiteConnect(API_KEY);
        kite.setAccessToken(ACCESS_TOKEN);

        List<Instrument> instruments = kite.getInstruments("NSE");

        List<String> symbolsList = new ArrayList<>();

        for (Instrument i : instruments) {

            if ("EQ".equals(i.instrument_type) && potentialCandidates.contains(i.tradingsymbol)) {

                symbolsList.add("NSE:" + i.tradingsymbol);
            }
        }

        String[] symbols = symbolsList.toArray(new String[0]);
        //MinuteQuoteScanner scanner = new MinuteQuoteScanner(kite,symbols);
        // LiveTickerNew.startWebSocket(kite,potentialCandidates,gapUpSymbolClosePriceMap);

        LiveTickerWeb.startWebSocket(kite, potentialCandidates);

        //scanner.start();
    }

    @NotNull
    private static Map<String, StockMomentum> getMomentumMap() throws SQLException {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 21, 9, 8);

        LocalDateTime endTime = LocalDateTime.of(2026, 8, 21, 9, 17);

        Map<String, StockMomentum> gapUpSymbolClosePriceMap = new ConcurrentHashMap<>();
        Map<String, StockMomentum> latestData = new HashMap<>();

        LocalDateTime time = startTime;

        while (!time.isAfter(endTime)) {

            System.out.println("\n========== " + time + " ==========");

            List<StockMomentum> preOpenMomentumData = PotentialMoverScanner.findPreOpenGapStocks(DB.get(), time, 2,
                    // minimum gap %
                    0       // minimum buy/sell ratio
            );
            preOpenMomentumData.stream().filter(s -> s.getPrevClose() > 30).forEach(s -> {
                gapUpSymbolClosePriceMap.put(s.getSymbol(), s);
                   /* System.out.printf("%-15s | GAP %.2f%% | MOVE %.2f%% | BS %.2f | BREAKOUT%n", s.getSymbol(),
                    s.getGapPct(), s.getMoveFrom0915Pct(), s.getCurrentBuySellRatio());*/
            });

            // -----------------------------------------
            // 1. Normal momentum candidates
            // -----------------------------------------

            List<StockMomentum> momentumData = PotentialMoverScanner.findStocks(DB.get(), time, 2,      // minimum gap %
                    1,      // minimum move from 09:15 %
                    0       // minimum buy/sell ratio
            );

            momentumData.stream().filter(s -> s.getPrevClose() > 30).forEach(s -> {
                gapUpSymbolClosePriceMap.put(s.getSymbol(), s);
                   /* System.out.printf("%-15s | GAP %.2f%% | MOVE %.2f%% | BS %.2f | BREAKOUT%n", s.getSymbol(),
                    s.getGapPct(), s.getMoveFrom0915Pct(), s.getCurrentBuySellRatio());*/
            });
            // -----------------------------------------
            // 2. Previous-day-high breakout candidates
            // -----------------------------------------

            List<StockMomentum> breakoutData = PotentialMoverScanner.findHighBreakouts(DB.get(), time);


            // Convert breakout symbols to Set
            Set<String> breakoutSymbols = breakoutData.stream().map(StockMomentum::getSymbol).collect(
                    Collectors.toSet());

            List<StockMomentum> surgeStocks = PotentialMoverScanner.findPriceBuyQuantitySurge(DB.get(), time,
                    breakoutSymbols, 2);
            surgeStocks.stream().filter(
                    s -> s.getPrevClose() > 30 && s.getBuyQtyChange() > 15_000 && s.getBuyQtyChangePct() > 25).forEach(
                    s -> {
                        gapUpSymbolClosePriceMap.put(s.getSymbol(), s);
                   /* System.out.printf("%-15s | Price %.2f -> %.2f | " + "BuyQty Δ %d | BuyQty Surge %6.2f%%%n",
                        s.getSymbol(), s.getPrevClose(), s.getCurrentClose(), s.getBuyQtyChange(),
                        s.getBuyQtyChangePct());*/
                    });
            time = time.plusMinutes(1);
        }
        System.out.println("GapUp Stocks " + gapUpSymbolClosePriceMap.keySet());
        return gapUpSymbolClosePriceMap;
    }

    private static double momentumScore(StockDailyFeature f, double price) {

        double score = 0;

        // =========================================
        // 1. TREND - 30 points
        // =========================================

        double ema50Spread = (f.getEma20() - f.getSma50()) * 100.0 / f.getSma50();

        double priceEmaSpread = (price - f.getEma20()) * 100.0 / f.getEma20();

        // EMA20 > SMA50
        if (ema50Spread > 10)
            score += 15;
        else if (ema50Spread > 5)
            score += 12;
        else if (ema50Spread > 2)
            score += 8;
        else
            score += 4;

        // Price above EMA20
        if (priceEmaSpread > 8)
            score += 15;
        else if (priceEmaSpread > 5)
            score += 12;
        else if (priceEmaSpread > 2)
            score += 8;
        else
            score += 4;

        // =========================================
        // 2. RSI - 20 points
        // =========================================

        double rsi = f.getRsi14();

        if (rsi >= 60 && rsi <= 68)
            score += 20;
        else if (rsi >= 55 && rsi < 60)
            score += 15;
        else if (rsi > 68 && rsi <= 70)
            score += 12;

        // =========================================
        // 3. DISTANCE FROM 20D HIGH - 30 points
        // =========================================

        double dist = f.getDistanceFrom20dHigh();

        if (dist >= -1)
            score += 30;
        else if (dist >= -2)
            score += 26;
        else if (dist >= -3)
            score += 22;
        else if (dist >= -4)
            score += 17;
        else if (dist >= -5)
            score += 10;

        return score;
    }

    static class Candidate {

        public Candidate(String symbol, double price, double score) {
            this.symbol = symbol;
            this.price = price;
            this.score = score;
        }

        String symbol;
        double price;

        double score;

        double entry;
        double stopLoss;
        double target;

        double gapPercent;

        double return5;
        double return10;
        double return15;

        double acceleration;
        double volumeAcceleration;

        double highRetention;

        double prevHighBreakPercent;

        double buySellRatio;

        double movePercent;

        double openingRangePercent;
        double riskPercent;

        LocalDateTime signalTime;

        public Candidate() {

        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getEntry() {
            return entry;
        }

        public void setEntry(double entry) {
            this.entry = entry;
        }

        public double getStopLoss() {
            return stopLoss;
        }

        public void setStopLoss(double stopLoss) {
            this.stopLoss = stopLoss;
        }

        public double getTarget() {
            return target;
        }

        public void setTarget(double target) {
            this.target = target;
        }

        public double getGapPercent() {
            return gapPercent;
        }

        public void setGapPercent(double gapPercent) {
            this.gapPercent = gapPercent;
        }

        public double getReturn5() {
            return return5;
        }

        public void setReturn5(double return5) {
            this.return5 = return5;
        }

        public double getReturn10() {
            return return10;
        }

        public void setReturn10(double return10) {
            this.return10 = return10;
        }

        public double getReturn15() {
            return return15;
        }

        public void setReturn15(double return15) {
            this.return15 = return15;
        }

        public double getAcceleration() {
            return acceleration;
        }

        public void setAcceleration(double acceleration) {
            this.acceleration = acceleration;
        }

        public double getVolumeAcceleration() {
            return volumeAcceleration;
        }

        public void setVolumeAcceleration(double volumeAcceleration) {
            this.volumeAcceleration = volumeAcceleration;
        }

        public double getHighRetention() {
            return highRetention;
        }

        public void setHighRetention(double highRetention) {
            this.highRetention = highRetention;
        }

        public double getPrevHighBreakPercent() {
            return prevHighBreakPercent;
        }

        public void setPrevHighBreakPercent(double prevHighBreakPercent) {
            this.prevHighBreakPercent = prevHighBreakPercent;
        }

        public double getBuySellRatio() {
            return buySellRatio;
        }

        public void setBuySellRatio(double buySellRatio) {
            this.buySellRatio = buySellRatio;
        }

        public LocalDateTime getSignalTime() {
            return signalTime;
        }

        public void setSignalTime(LocalDateTime signalTime) {
            this.signalTime = signalTime;
        }
    }

    public static BreakoutProbability calculateBreakoutProbability(Deque<MinuteCandle> candles, double percentageStep) {

        BreakoutProbability result = new BreakoutProbability();

        if (candles == null || candles.size() < 2) {
            result.bias = "NEUTRAL";
            return result;
        }

        long[] greenCount = new long[5];
        long[] redCount = new long[5];

        long[] greenUp = new long[5];
        long[] greenDown = new long[5];

        long[] redUp = new long[5];
        long[] redDown = new long[5];

        // IMPORTANT:
        // candles must be oldest -> newest
        MinuteCandle previous = null;

        for (MinuteCandle current : candles) {

            if (previous == null) {
                previous = current;
                continue;
            }

            double prevOpen = previous.getOpen();
            double prevClose = previous.getClose();
            double prevHigh = previous.getHigh();
            double prevLow = previous.getLow();

            double currentHigh = current.getHigh();
            double currentLow = current.getLow();

            boolean green = prevClose > prevOpen;
            boolean red = prevClose < prevOpen;

            if (!green && !red) {
                previous = current;
                continue;
            }

            // Same as Pine:
            // step = c * (perc / 100)
            double step = current.getClose() * (percentageStep / 100.0);

            for (int level = 0; level < 5; level++) {

                double upLevel = prevHigh + (step * level);

                double downLevel = prevLow - (step * level);

                boolean hitUp = currentHigh >= upLevel;

                boolean hitDown = currentLow <= downLevel;

                if (green) {

                    greenCount[level]++;

                    if (hitUp)
                        greenUp[level]++;

                    if (hitDown)
                        greenDown[level]++;

                } else {

                    redCount[level]++;

                    if (hitUp)
                        redUp[level]++;

                    if (hitDown)
                        redDown[level]++;
                }
            }

            previous = current;
        }

        /*
         * Determine direction of the latest completed candle.
         */
        MinuteCandle last = candles.peekLast();

        boolean lastGreen = last.getClose() > last.getOpen();

        boolean lastRed = last.getClose() < last.getOpen();

        for (int level = 0; level < 5; level++) {

            if (lastGreen && greenCount[level] > 0) {

                result.upProbability[level] = 100.0 * greenUp[level] / greenCount[level];

                result.downProbability[level] = 100.0 * greenDown[level] / greenCount[level];

            } else if (lastRed && redCount[level] > 0) {

                result.upProbability[level] = 100.0 * redUp[level] / redCount[level];

                result.downProbability[level] = 100.0 * redDown[level] / redCount[level];
            }
        }

        double up = result.upProbability[1];
        double down = result.downProbability[1];

        result.bullishScore = up;
        result.bearishScore = down;

        if (up > down) {
            result.bias = "BULLISH";
        } else if (down > up) {
            result.bias = "BEARISH";
        } else {
            result.bias = "NEUTRAL";
        }

        return result;
    }

    public static class Position {

        String symbol;
        double entryPrice;
        int quantity;
        LocalDateTime entryTime;
        double capital;
        double highestProfit = 0.0;
        boolean trailingActive = false;
        double closePrice = 0.0;
        double highPrice = 0.0;


        public Position(
                String symbol,
                double entryPrice,
                int quantity,
                LocalDateTime entryTime) {

            this.symbol = symbol;
            this.entryPrice = entryPrice;
            this.quantity = quantity;
            this.capital = entryPrice * quantity;
            this.entryTime = entryTime;
        }

        public double profit(double currentPrice) {
            return (currentPrice - entryPrice) * quantity;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getEntryPrice() {
            return entryPrice;
        }

        public void setEntryPrice(double entryPrice) {
            this.entryPrice = entryPrice;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public LocalDateTime getEntryTime() {
            return entryTime;
        }

        public void setEntryTime(LocalDateTime entryTime) {
            this.entryTime = entryTime;
        }

        public double getCapital() {
            return capital;
        }

        public void setCapital(double capital) {
            this.capital = capital;
        }

        public double getHighestProfit() {
            return highestProfit;
        }

        public void setHighestProfit(double highestProfit) {
            this.highestProfit = highestProfit;
        }

        public boolean isTrailingActive() {
            return trailingActive;
        }

        public void setTrailingActive(boolean trailingActive) {
            this.trailingActive = trailingActive;
        }

        public double getClosePrice() {
            return closePrice;
        }

        public void setClosePrice(double closePrice) {
            this.closePrice = closePrice;
        }

        public double getHighPrice() {
            return highPrice;
        }

        public void setHighPrice(double highPrice) {
            this.highPrice = highPrice;
        }
    }

}