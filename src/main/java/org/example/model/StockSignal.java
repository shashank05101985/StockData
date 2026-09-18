package org.example.model;

/**
 * Model class representing a trading signal with entry, stop-loss and targets
 */
public class StockSignal {

    public enum SignalType {
        STRONG_BUY("Strong Buy - High Probability"),
        BUY("Buy - Good Potential"),
        WEAK_BUY("Weak Buy - Monitor Closely"),
        HOLD("Hold "),
        AVOID("Avoid - High Risk"),
        STRONG_SELL("Strong Sell "),
        WEAK_SELL("Weak Sell "),
        SELL("Sell "),
        EXTENDED("EXTENDED — DO NOT CHASE");

        private final String description;

        SignalType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private String symbol;
    private SignalType signalType;
    private double currentPrice;
    private double entryPrice;
    private double stopLoss;
    private double target1;
    private double target2;
    private double target3;
    private double riskRewardRatio;
    private int score;                  // Overall score out of 100
    private double deliveryPercent;
    private double volumeChange;        // Volume change vs average
    private String reason;
    private boolean volumeBreakout;
    private boolean priceBreakout;
    private boolean priceBreakdown;
    private boolean deliveryBreakout;
    private double rsi;
    private double bbPosition;
    private double momentum;



    public StockSignal() {
    }

    public double getRsi() {
        return rsi;
    }

    public void setRsi(double rsi) {
        this.rsi = rsi;
    }

    public double getBbPosition() {
        return bbPosition;
    }

    public void setBbPosition(double bbPosition) {
        this.bbPosition = bbPosition;
    }

    public double getMomentum() {
        return momentum;
    }

    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getTarget1() {
        return target1;
    }

    public void setTarget1(double target1) {
        this.target1 = target1;
    }

    public double getTarget2() {
        return target2;
    }

    public void setTarget2(double target2) {
        this.target2 = target2;
    }

    public double getTarget3() {
        return target3;
    }

    public void setTarget3(double target3) {
        this.target3 = target3;
    }

    public double getRiskRewardRatio() {
        return riskRewardRatio;
    }

    public void setRiskRewardRatio(double riskRewardRatio) {
        this.riskRewardRatio = riskRewardRatio;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getDeliveryPercent() {
        return deliveryPercent;
    }

    public void setDeliveryPercent(double deliveryPercent) {
        this.deliveryPercent = deliveryPercent;
    }

    public double getVolumeChange() {
        return volumeChange;
    }

    public void setVolumeChange(double volumeChange) {
        this.volumeChange = volumeChange;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isVolumeBreakout() {
        return volumeBreakout;
    }

    public void setVolumeBreakout(boolean volumeBreakout) {
        this.volumeBreakout = volumeBreakout;
    }

    public boolean isPriceBreakout() {
        return priceBreakout;
    }

    public void setPriceBreakout(boolean priceBreakout) {
        this.priceBreakout = priceBreakout;
    }

    public boolean isPriceBreakdown() {
        return priceBreakdown;
    }

    public void setPriceBreakdown(boolean priceBreakdown) {
        this.priceBreakdown = priceBreakdown;
    }

    public boolean isDeliveryBreakout() {
        return deliveryBreakout;
    }

    public void setDeliveryBreakout(boolean deliveryBreakout) {
        this.deliveryBreakout = deliveryBreakout;
    }

    public double getRiskPercent() {
        if (currentPrice > 0) {
            return ((currentPrice - stopLoss) / currentPrice) * 100;
        }
        return 0;
    }

    public double getTarget1Percent() {
        if (currentPrice > 0) {
            return ((target1 - currentPrice) / currentPrice) * 100;
        }
        return 0;
    }

    public double getTarget2Percent() {
        if (currentPrice > 0) {
            return ((target2 - currentPrice) / currentPrice) * 100;
        }
        return 0;
    }

    public double getTarget3Percent() {
        if (currentPrice > 0) {
            return ((target3 - currentPrice) / currentPrice) * 100;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("%-12s  SCORE %d/100  |  %s%n",
            symbol, score, signalType.getDescription()));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        sb.append(String.format(
            "Price ₹%.2f → Entry ₹%.2f | SL ₹%.2f (%.1f%%)%n",
            currentPrice, entryPrice, stopLoss, getRiskPercent()));

        sb.append(String.format(
            "T1 ₹%.2f | T2 ₹%.2f | T3 ₹%.2f | R:R 1:%.2f%n",
            target1, target2, target3, riskRewardRatio));

        sb.append(String.format(
            "Delivery %.1f%% | Volume %+.1f%% | Breakout V:%s P:%s D:%s%n",
            deliveryPercent,
            volumeChange,
            volumeBreakout ? "✓" : "✗",
            priceBreakout ? "✓" : "✗",
            deliveryBreakout ? "✓" : "✗"));

        sb.append(String.format(
            "Reason: %s%n",
            reason));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }

    public String formatOutPut()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║ %-18s SCORE: %3d/100                           ║%n",
            symbol, score));
        sb.append(String.format("║ %-58s ║%n",
            signalType.getDescription()));
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        sb.append("║  PRICE & RISK                                                ║\n");
        sb.append(String.format("║  Current      : ₹%-10.2f  Entry       : ₹%-10.2f      ║%n",
            currentPrice, entryPrice));
        sb.append(String.format("║  Stop Loss    : ₹%-10.2f  Risk        : %-6.2f%%         ║%n",
            stopLoss, getRiskPercent()));

        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        sb.append("║  TARGETS                                                     ║\n");
        sb.append(String.format("║  Target 1     : ₹%-10.2f  Return      : +%-6.2f%%         ║%n",
            target1, getTarget1Percent()));
        sb.append(String.format("║  Target 2     : ₹%-10.2f  Return      : +%-6.2f%%         ║%n",
            target2, getTarget2Percent()));
        sb.append(String.format("║  Target 3     : ₹%-10.2f  Return      : +%-6.2f%%         ║%n",
            target3, getTarget3Percent()));
        sb.append(String.format("║  Risk / Reward: 1:%-6.2f                                      ║%n",
            riskRewardRatio));

        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        sb.append("║  CONFIRMATION                                                ║\n");
        sb.append(String.format("║  Delivery     : %-6.2f%%   Volume       : %-6.2f%% vs Avg ║%n",
            deliveryPercent, volumeChange));
        sb.append(String.format("║  Volume       : %s       Price        : %s              ║%n",
            volumeBreakout ? "✓ Confirmed" : "✗ No Breakout",
            priceBreakout ? "✓ Confirmed" : "✗ No Breakout"));
        sb.append(String.format("║  Delivery     : %-13s                             ║%n",
            deliveryBreakout ? "✓ Confirmed" : "✗ No Breakout"));

        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        sb.append("║  ANALYSIS                                                    ║\n");

        if (reason != null && !reason.isBlank()) {
            String[] reasons = reason.split("(?<=\\. )");

            for (String r : reasons) {
                if (r.isBlank()) {
                    continue;
                }

                String text = r.trim();

                // Wrap long reason text into console-friendly lines
                while (text.length() > 56) {
                    int breakAt = text.lastIndexOf(' ', 56);

                    if (breakAt <= 0) {
                        breakAt = 56;
                    }

                    sb.append(String.format("║  %-58s ║%n",
                        text.substring(0, breakAt).trim()));

                    text = text.substring(breakAt).trim();
                }

                sb.append(String.format("║  • %-56s ║%n", text));
            }
        } else {
            sb.append("║  • No additional analysis available.                       ║\n");
        }

        sb.append("╚══════════════════════════════════════════════════════════════╝");

        return sb.toString();
    }
}
