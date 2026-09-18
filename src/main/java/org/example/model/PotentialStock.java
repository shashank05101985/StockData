package org.example.model;

public record PotentialStock(
    String symbol,
    double score,
    double price,
    double volumeRatio,
    double change1m,
    double change5m,
    double vwap,
    double ema20,
    double breakoutLevel,
    String signal,
    String reason
) {}
