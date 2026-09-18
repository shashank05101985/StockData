package org.example.model;

import java.util.Arrays;

public  class BreakoutProbability {

    public double[] upProbability = new double[5];
    public double[] downProbability = new double[5];

    public double bullishScore;
    public double bearishScore;

    public String bias;

    @Override
    public String toString() {
        return String.format(
            "Bias=%s Bullish=%.2f%% Bearish=%.2f%% UP=%s DOWN=%s",
            bias,
            bullishScore,
            bearishScore,
            Arrays.toString(upProbability),
            Arrays.toString(downProbability)
        );
    }
}
