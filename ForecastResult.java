package com.smarthr.dto;

import java.util.List;

/** Output of the leave forecasting model for one scope (whole company or one department). */
public class ForecastResult {
    private final String scope;
    private final List<String> historyLabels;
    private final List<Double> historyValues;
    private final List<String> forecastLabels;
    private final List<Double> forecastValues;
    private final List<Double> lowerBand;
    private final List<Double> upperBand;
    private final String trend;
    private final double trendPercent;
    private final double rSquared;
    private final double mae;
    private final double movingAverage;
    private final double averageMonthly;
    private final String insight;

    public ForecastResult(String scope, List<String> historyLabels, List<Double> historyValues,
                          List<String> forecastLabels, List<Double> forecastValues,
                          List<Double> lowerBand, List<Double> upperBand,
                          String trend, double trendPercent, double rSquared, double mae,
                          double movingAverage, double averageMonthly, String insight) {
        this.scope = scope;
        this.historyLabels = historyLabels;
        this.historyValues = historyValues;
        this.forecastLabels = forecastLabels;
        this.forecastValues = forecastValues;
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
        this.trend = trend;
        this.trendPercent = trendPercent;
        this.rSquared = rSquared;
        this.mae = mae;
        this.movingAverage = movingAverage;
        this.averageMonthly = averageMonthly;
        this.insight = insight;
    }

    public String getScope() { return scope; }
    public List<String> getHistoryLabels() { return historyLabels; }
    public List<Double> getHistoryValues() { return historyValues; }
    public List<String> getForecastLabels() { return forecastLabels; }
    public List<Double> getForecastValues() { return forecastValues; }
    public List<Double> getLowerBand() { return lowerBand; }
    public List<Double> getUpperBand() { return upperBand; }
    public String getTrend() { return trend; }
    public double getTrendPercent() { return trendPercent; }
    public double getRSquared() { return rSquared; }
    public double getMae() { return mae; }
    public double getMovingAverage() { return movingAverage; }
    public double getAverageMonthly() { return averageMonthly; }
    public String getInsight() { return insight; }
    public double getNextMonth() { return forecastValues.isEmpty() ? 0 : forecastValues.get(0); }
    public String getNextMonthLabel() { return forecastLabels.isEmpty() ? "" : forecastLabels.get(0); }
}
