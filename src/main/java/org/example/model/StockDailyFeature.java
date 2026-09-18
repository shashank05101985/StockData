package org.example.model;

import java.time.LocalDate;

public class StockDailyFeature {

    public String symbol;
    public LocalDate tradeDate;

    public double open;
    public double high;
    public double low;
    public double close;
    public long volume;

    // Returns
    public Double return1d;
    public Double return3d;
    public Double return5d;
    public Double return10d;
    public Double return20d;

    // SMA
    public Double sma20;
    public Double sma50;
    public Double sma100;
    public Double sma200;

    // EMA
    public Double ema9;
    public Double ema20;
    public Double ema50;
    public Double ema100;
    public Double ema200;

    // Trend flags
    public Boolean closeAboveEma20;
    public Boolean ema20AboveEma50;
    public Boolean ema50AboveEma200;

    // RSI
    public Double rsi14;

    // MACD
    public Double macd;
    public Double macdSignal;
    public Double macdHistogram;

    // ATR / ADX
    public Double tr14;
    public Double atr14;
    public Double atrPercent;

    public Double plusDm14;
    public Double minusDm14;

    public Double plusDi14;
    public Double minusDi14;
    public Double adx14;

    // Stochastic
    public Double stochasticK;
    public Double stochasticD;

    // CCI
    public Double cci20;

    // ROC
    public Double roc5;
    public Double roc10;
    public Double roc20;

    // Bollinger
    public Double bbMiddle;
    public Double bbUpper;
    public Double bbLower;
    public Double bbWidth;
    public Double bbPercentB;

    // Volume
    public Double avgVolume5;
    public Double avgVolume10;
    public Double avgVolume20;
    public Double avgVolume50;

    public Double volumeRatio5;
    public Double volumeRatio20;
    public Double volumeRatio50;

    public Double obv;

    // High / low
    public Double high5d;
    public Double high10d;
    public Double high20d;
    public Double high50d;

    public Double low5d;
    public Double low10d;
    public Double low20d;
    public Double low50d;

    public Double distanceFrom5dHigh;
    public Double distanceFrom10dHigh;
    public Double distanceFrom20dHigh;
    public Double distanceFrom52wHigh;

    // Volatility
    public Double dailyRangePercent;
    public Double avgRange5Percent;
    public Double avgRange20Percent;

    public Double volatility5;
    public Double volatility20;

    // Structure
    public Boolean higherHigh;
    public Boolean higherLow;
    public Boolean lowerHigh;
    public Boolean lowerLow;

    // Breakout
    public Boolean breakout5d;
    public Boolean breakout10d;
    public Boolean breakout20d;

    public Boolean breakdown5d;
    public Boolean breakdown10d;
    public Boolean breakdown20d;

    public Double consolidationWidth;
    public Double consolidationScore;
    public Double breakoutStrength;

    // NIFTY
    public Double niftyReturn5d;
    public Double niftyReturn10d;
    public Double niftyReturn20d;

    public Double relativeStrength5d;
    public Double relativeStrength10d;
    public Double relativeStrength20d;

    // Scores - leave NULL initially
    public Double trendScore;
    public Double momentumScore;
    public Double volumeScore;
    public Double volatilityScore;
    public Double relativeStrengthScore;
    public Double priceStructureScore;
    public Double breakoutScore;

    public Double bullishScore;

    public String marketBias;

    public Double resistancePrice;
    public Double supportPrice;
    public Double resistanceStrength;
    public Double supportStrength;
    public Double distanceToResistance;
    public Double distanceToSupport;

    public Double getResistancePrice() {
        return resistancePrice;
    }

    public void setResistancePrice(Double resistancePrice) {
        this.resistancePrice = resistancePrice;
    }

    public Double getSupportPrice() {
        return supportPrice;
    }

    public void setSupportPrice(Double supportPrice) {
        this.supportPrice = supportPrice;
    }

    public Double getResistanceStrength() {
        return resistanceStrength;
    }

    public void setResistanceStrength(Double resistanceStrength) {
        this.resistanceStrength = resistanceStrength;
    }

    public Double getSupportStrength() {
        return supportStrength;
    }

    public void setSupportStrength(Double supportStrength) {
        this.supportStrength = supportStrength;
    }

    public Double getDistanceToResistance() {
        return distanceToResistance;
    }

    public void setDistanceToResistance(Double distanceToResistance) {
        this.distanceToResistance = distanceToResistance;
    }

    public Double getDistanceToSupport() {
        return distanceToSupport;
    }

    public void setDistanceToSupport(Double distanceToSupport) {
        this.distanceToSupport = distanceToSupport;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public Double getReturn1d() {
        return return1d;
    }

    public void setReturn1d(Double return1d) {
        this.return1d = return1d;
    }

    public Double getReturn3d() {
        return return3d;
    }

    public void setReturn3d(Double return3d) {
        this.return3d = return3d;
    }

    public Double getReturn5d() {
        return return5d;
    }

    public void setReturn5d(Double return5d) {
        this.return5d = return5d;
    }

    public Double getReturn10d() {
        return return10d;
    }

    public void setReturn10d(Double return10d) {
        this.return10d = return10d;
    }

    public Double getReturn20d() {
        return return20d;
    }

    public void setReturn20d(Double return20d) {
        this.return20d = return20d;
    }

    public Double getSma20() {
        return sma20;
    }

    public void setSma20(Double sma20) {
        this.sma20 = sma20;
    }

    public Double getSma50() {
        return sma50;
    }

    public void setSma50(Double sma50) {
        this.sma50 = sma50;
    }

    public Double getSma100() {
        return sma100;
    }

    public void setSma100(Double sma100) {
        this.sma100 = sma100;
    }

    public Double getSma200() {
        return sma200;
    }

    public void setSma200(Double sma200) {
        this.sma200 = sma200;
    }

    public Double getEma9() {
        return ema9;
    }

    public void setEma9(Double ema9) {
        this.ema9 = ema9;
    }

    public Double getEma20() {
        return ema20;
    }

    public void setEma20(Double ema20) {
        this.ema20 = ema20;
    }

    public Double getEma50() {
        return ema50;
    }

    public void setEma50(Double ema50) {
        this.ema50 = ema50;
    }

    public Double getEma100() {
        return ema100;
    }

    public void setEma100(Double ema100) {
        this.ema100 = ema100;
    }

    public Double getEma200() {
        return ema200;
    }

    public void setEma200(Double ema200) {
        this.ema200 = ema200;
    }

    public Boolean getCloseAboveEma20() {
        return closeAboveEma20;
    }

    public void setCloseAboveEma20(Boolean closeAboveEma20) {
        this.closeAboveEma20 = closeAboveEma20;
    }

    public Boolean getEma20AboveEma50() {
        return ema20AboveEma50;
    }

    public void setEma20AboveEma50(Boolean ema20AboveEma50) {
        this.ema20AboveEma50 = ema20AboveEma50;
    }

    public Boolean getEma50AboveEma200() {
        return ema50AboveEma200;
    }

    public void setEma50AboveEma200(Boolean ema50AboveEma200) {
        this.ema50AboveEma200 = ema50AboveEma200;
    }

    public Double getRsi14() {
        return rsi14;
    }

    public void setRsi14(Double rsi14) {
        this.rsi14 = rsi14;
    }

    public Double getMacd() {
        return macd;
    }

    public void setMacd(Double macd) {
        this.macd = macd;
    }

    public Double getMacdSignal() {
        return macdSignal;
    }

    public void setMacdSignal(Double macdSignal) {
        this.macdSignal = macdSignal;
    }

    public Double getMacdHistogram() {
        return macdHistogram;
    }

    public void setMacdHistogram(Double macdHistogram) {
        this.macdHistogram = macdHistogram;
    }

    public Double getTr14() {
        return tr14;
    }

    public void setTr14(Double tr14) {
        this.tr14 = tr14;
    }

    public Double getAtr14() {
        return atr14;
    }

    public void setAtr14(Double atr14) {
        this.atr14 = atr14;
    }

    public Double getAtrPercent() {
        return atrPercent;
    }

    public void setAtrPercent(Double atrPercent) {
        this.atrPercent = atrPercent;
    }

    public Double getPlusDm14() {
        return plusDm14;
    }

    public void setPlusDm14(Double plusDm14) {
        this.plusDm14 = plusDm14;
    }

    public Double getMinusDm14() {
        return minusDm14;
    }

    public void setMinusDm14(Double minusDm14) {
        this.minusDm14 = minusDm14;
    }

    public Double getPlusDi14() {
        return plusDi14;
    }

    public void setPlusDi14(Double plusDi14) {
        this.plusDi14 = plusDi14;
    }

    public Double getMinusDi14() {
        return minusDi14;
    }

    public void setMinusDi14(Double minusDi14) {
        this.minusDi14 = minusDi14;
    }

    public Double getAdx14() {
        return adx14;
    }

    public void setAdx14(Double adx14) {
        this.adx14 = adx14;
    }

    public Double getStochasticK() {
        return stochasticK;
    }

    public void setStochasticK(Double stochasticK) {
        this.stochasticK = stochasticK;
    }

    public Double getStochasticD() {
        return stochasticD;
    }

    public void setStochasticD(Double stochasticD) {
        this.stochasticD = stochasticD;
    }

    public Double getCci20() {
        return cci20;
    }

    public void setCci20(Double cci20) {
        this.cci20 = cci20;
    }

    public Double getRoc5() {
        return roc5;
    }

    public void setRoc5(Double roc5) {
        this.roc5 = roc5;
    }

    public Double getRoc10() {
        return roc10;
    }

    public void setRoc10(Double roc10) {
        this.roc10 = roc10;
    }

    public Double getRoc20() {
        return roc20;
    }

    public void setRoc20(Double roc20) {
        this.roc20 = roc20;
    }

    public Double getBbMiddle() {
        return bbMiddle;
    }

    public void setBbMiddle(Double bbMiddle) {
        this.bbMiddle = bbMiddle;
    }

    public Double getBbUpper() {
        return bbUpper;
    }

    public void setBbUpper(Double bbUpper) {
        this.bbUpper = bbUpper;
    }

    public Double getBbLower() {
        return bbLower;
    }

    public void setBbLower(Double bbLower) {
        this.bbLower = bbLower;
    }

    public Double getBbWidth() {
        return bbWidth;
    }

    public void setBbWidth(Double bbWidth) {
        this.bbWidth = bbWidth;
    }

    public Double getBbPercentB() {
        return bbPercentB;
    }

    public void setBbPercentB(Double bbPercentB) {
        this.bbPercentB = bbPercentB;
    }

    public Double getAvgVolume5() {
        return avgVolume5;
    }

    public void setAvgVolume5(Double avgVolume5) {
        this.avgVolume5 = avgVolume5;
    }

    public Double getAvgVolume10() {
        return avgVolume10;
    }

    public void setAvgVolume10(Double avgVolume10) {
        this.avgVolume10 = avgVolume10;
    }

    public Double getAvgVolume20() {
        return avgVolume20;
    }

    public void setAvgVolume20(Double avgVolume20) {
        this.avgVolume20 = avgVolume20;
    }

    public Double getAvgVolume50() {
        return avgVolume50;
    }

    public void setAvgVolume50(Double avgVolume50) {
        this.avgVolume50 = avgVolume50;
    }

    public Double getVolumeRatio5() {
        return volumeRatio5;
    }

    public void setVolumeRatio5(Double volumeRatio5) {
        this.volumeRatio5 = volumeRatio5;
    }

    public Double getVolumeRatio20() {
        return volumeRatio20;
    }

    public void setVolumeRatio20(Double volumeRatio20) {
        this.volumeRatio20 = volumeRatio20;
    }

    public Double getVolumeRatio50() {
        return volumeRatio50;
    }

    public void setVolumeRatio50(Double volumeRatio50) {
        this.volumeRatio50 = volumeRatio50;
    }

    public Double getObv() {
        return obv;
    }

    public void setObv(Double obv) {
        this.obv = obv;
    }

    public Double getHigh5d() {
        return high5d;
    }

    public void setHigh5d(Double high5d) {
        this.high5d = high5d;
    }

    public Double getHigh10d() {
        return high10d;
    }

    public void setHigh10d(Double high10d) {
        this.high10d = high10d;
    }

    public Double getHigh20d() {
        return high20d;
    }

    public void setHigh20d(Double high20d) {
        this.high20d = high20d;
    }

    public Double getHigh50d() {
        return high50d;
    }

    public void setHigh50d(Double high50d) {
        this.high50d = high50d;
    }

    public Double getLow5d() {
        return low5d;
    }

    public void setLow5d(Double low5d) {
        this.low5d = low5d;
    }

    public Double getLow10d() {
        return low10d;
    }

    public void setLow10d(Double low10d) {
        this.low10d = low10d;
    }

    public Double getLow20d() {
        return low20d;
    }

    public void setLow20d(Double low20d) {
        this.low20d = low20d;
    }

    public Double getLow50d() {
        return low50d;
    }

    public void setLow50d(Double low50d) {
        this.low50d = low50d;
    }

    public Double getDistanceFrom5dHigh() {
        return distanceFrom5dHigh;
    }

    public void setDistanceFrom5dHigh(Double distanceFrom5dHigh) {
        this.distanceFrom5dHigh = distanceFrom5dHigh;
    }

    public Double getDistanceFrom10dHigh() {
        return distanceFrom10dHigh;
    }

    public void setDistanceFrom10dHigh(Double distanceFrom10dHigh) {
        this.distanceFrom10dHigh = distanceFrom10dHigh;
    }

    public Double getDistanceFrom20dHigh() {
        return distanceFrom20dHigh;
    }

    public void setDistanceFrom20dHigh(Double distanceFrom20dHigh) {
        this.distanceFrom20dHigh = distanceFrom20dHigh;
    }

    public Double getDistanceFrom52wHigh() {
        return distanceFrom52wHigh;
    }

    public void setDistanceFrom52wHigh(Double distanceFrom52wHigh) {
        this.distanceFrom52wHigh = distanceFrom52wHigh;
    }

    public Double getDailyRangePercent() {
        return dailyRangePercent;
    }

    public void setDailyRangePercent(Double dailyRangePercent) {
        this.dailyRangePercent = dailyRangePercent;
    }

    public Double getAvgRange5Percent() {
        return avgRange5Percent;
    }

    public void setAvgRange5Percent(Double avgRange5Percent) {
        this.avgRange5Percent = avgRange5Percent;
    }

    public Double getAvgRange20Percent() {
        return avgRange20Percent;
    }

    public void setAvgRange20Percent(Double avgRange20Percent) {
        this.avgRange20Percent = avgRange20Percent;
    }

    public Double getVolatility5() {
        return volatility5;
    }

    public void setVolatility5(Double volatility5) {
        this.volatility5 = volatility5;
    }

    public Double getVolatility20() {
        return volatility20;
    }

    public void setVolatility20(Double volatility20) {
        this.volatility20 = volatility20;
    }

    public Boolean getHigherHigh() {
        return higherHigh;
    }

    public void setHigherHigh(Boolean higherHigh) {
        this.higherHigh = higherHigh;
    }

    public Boolean getHigherLow() {
        return higherLow;
    }

    public void setHigherLow(Boolean higherLow) {
        this.higherLow = higherLow;
    }

    public Boolean getLowerHigh() {
        return lowerHigh;
    }

    public void setLowerHigh(Boolean lowerHigh) {
        this.lowerHigh = lowerHigh;
    }

    public Boolean getLowerLow() {
        return lowerLow;
    }

    public void setLowerLow(Boolean lowerLow) {
        this.lowerLow = lowerLow;
    }

    public Boolean getBreakout5d() {
        return breakout5d;
    }

    public void setBreakout5d(Boolean breakout5d) {
        this.breakout5d = breakout5d;
    }

    public Boolean getBreakout10d() {
        return breakout10d;
    }

    public void setBreakout10d(Boolean breakout10d) {
        this.breakout10d = breakout10d;
    }

    public Boolean getBreakout20d() {
        return breakout20d;
    }

    public void setBreakout20d(Boolean breakout20d) {
        this.breakout20d = breakout20d;
    }

    public Boolean getBreakdown5d() {
        return breakdown5d;
    }

    public void setBreakdown5d(Boolean breakdown5d) {
        this.breakdown5d = breakdown5d;
    }

    public Boolean getBreakdown10d() {
        return breakdown10d;
    }

    public void setBreakdown10d(Boolean breakdown10d) {
        this.breakdown10d = breakdown10d;
    }

    public Boolean getBreakdown20d() {
        return breakdown20d;
    }

    public void setBreakdown20d(Boolean breakdown20d) {
        this.breakdown20d = breakdown20d;
    }

    public Double getConsolidationWidth() {
        return consolidationWidth;
    }

    public void setConsolidationWidth(Double consolidationWidth) {
        this.consolidationWidth = consolidationWidth;
    }

    public Double getConsolidationScore() {
        return consolidationScore;
    }

    public void setConsolidationScore(Double consolidationScore) {
        this.consolidationScore = consolidationScore;
    }

    public Double getBreakoutStrength() {
        return breakoutStrength;
    }

    public void setBreakoutStrength(Double breakoutStrength) {
        this.breakoutStrength = breakoutStrength;
    }

    public Double getNiftyReturn5d() {
        return niftyReturn5d;
    }

    public void setNiftyReturn5d(Double niftyReturn5d) {
        this.niftyReturn5d = niftyReturn5d;
    }

    public Double getNiftyReturn10d() {
        return niftyReturn10d;
    }

    public void setNiftyReturn10d(Double niftyReturn10d) {
        this.niftyReturn10d = niftyReturn10d;
    }

    public Double getNiftyReturn20d() {
        return niftyReturn20d;
    }

    public void setNiftyReturn20d(Double niftyReturn20d) {
        this.niftyReturn20d = niftyReturn20d;
    }

    public Double getRelativeStrength5d() {
        return relativeStrength5d;
    }

    public void setRelativeStrength5d(Double relativeStrength5d) {
        this.relativeStrength5d = relativeStrength5d;
    }

    public Double getRelativeStrength10d() {
        return relativeStrength10d;
    }

    public void setRelativeStrength10d(Double relativeStrength10d) {
        this.relativeStrength10d = relativeStrength10d;
    }

    public Double getRelativeStrength20d() {
        return relativeStrength20d;
    }

    public void setRelativeStrength20d(Double relativeStrength20d) {
        this.relativeStrength20d = relativeStrength20d;
    }

    public Double getTrendScore() {
        return trendScore;
    }

    public void setTrendScore(Double trendScore) {
        this.trendScore = trendScore;
    }

    public Double getMomentumScore() {
        return momentumScore;
    }

    public void setMomentumScore(Double momentumScore) {
        this.momentumScore = momentumScore;
    }

    public Double getVolumeScore() {
        return volumeScore;
    }

    public void setVolumeScore(Double volumeScore) {
        this.volumeScore = volumeScore;
    }

    public Double getVolatilityScore() {
        return volatilityScore;
    }

    public void setVolatilityScore(Double volatilityScore) {
        this.volatilityScore = volatilityScore;
    }

    public Double getRelativeStrengthScore() {
        return relativeStrengthScore;
    }

    public void setRelativeStrengthScore(Double relativeStrengthScore) {
        this.relativeStrengthScore = relativeStrengthScore;
    }

    public Double getPriceStructureScore() {
        return priceStructureScore;
    }

    public void setPriceStructureScore(Double priceStructureScore) {
        this.priceStructureScore = priceStructureScore;
    }

    public Double getBreakoutScore() {
        return breakoutScore;
    }

    public void setBreakoutScore(Double breakoutScore) {
        this.breakoutScore = breakoutScore;
    }

    public Double getBullishScore() {
        return bullishScore;
    }

    public void setBullishScore(Double bullishScore) {
        this.bullishScore = bullishScore;
    }

    public String getMarketBias() {
        return marketBias;
    }

    public void setMarketBias(String marketBias) {
        this.marketBias = marketBias;
    }
}
