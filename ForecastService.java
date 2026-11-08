package com.smarthr.service;

import com.smarthr.dto.ForecastResult;
import com.smarthr.entity.Department;
import com.smarthr.entity.LeaveRequest;
import com.smarthr.entity.LeaveStatus;
import com.smarthr.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Leave forecasting from historical approved leave (working days per month).
 *
 * Model: seasonal-adjusted linear regression, implemented in plain Java so there is no external ML service.
 *  1. Seasonal index per calendar month = (average of that month / overall average), damped towards 1 to limit over-fitting.
 *  2. Each month is de-seasonalised (value / index) and an ordinary least-squares trend line is fitted.
 *  3. Forecast = trend value * seasonal index for the target month, never below zero.
 *  4. The band is the forecast +/- 1.96 x the residual standard deviation (roughly a 95% interval).
 * R-squared (on the de-seasonalised series) and the mean absolute error of the fit are reported so the result can be judged.
 */
@Service
public class ForecastService {

    public static final int HISTORY_MONTHS = 24;
    public static final int HORIZON = 3;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final LeaveRequestRepository leaves;

    public ForecastService(LeaveRequestRepository leaves) {
        this.leaves = leaves;
    }

    @Transactional(readOnly = true)
    public ForecastResult forecastCompany() {
        YearMonth last = YearMonth.now().minusMonths(1);
        Map<YearMonth, Double> series = new HashMap<>();
        for (LeaveRequest l : history(last)) series.merge(YearMonth.from(l.getStartDate()), (double) l.getDays(), Double::sum);
        return forecast("Whole company", series, last, HORIZON);
    }

    @Transactional(readOnly = true)
    public List<ForecastResult> forecastByDepartment() {
        YearMonth last = YearMonth.now().minusMonths(1);
        Map<String, Map<YearMonth, Double>> byDept = new TreeMap<>();
        for (LeaveRequest l : history(last)) {
            Department d = l.getEmployee().getDepartment();
            byDept.computeIfAbsent(d == null ? "Unassigned" : d.getName(), k -> new HashMap<>())
                    .merge(YearMonth.from(l.getStartDate()), (double) l.getDays(), Double::sum);
        }
        List<ForecastResult> results = new ArrayList<>();
        byDept.forEach((name, series) -> results.add(forecast(name, series, last, HORIZON)));
        return results;
    }

    private List<LeaveRequest> history(YearMonth last) {
        return leaves.findSince(LeaveStatus.APPROVED, last.minusMonths(HISTORY_MONTHS - 1L).atDay(1));
    }

    /** Pure function so it can be unit-tested without a database. */
    public ForecastResult forecast(String scope, Map<YearMonth, Double> series, YearMonth last, int horizon) {
        final int n = HISTORY_MONTHS;
        YearMonth first = last.minusMonths(n - 1L);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) y[i] = series.getOrDefault(first.plusMonths(i), 0.0);
        double mean = Arrays.stream(y).average().orElse(0);

        // 1. damped seasonal indices
        double[] si = new double[12];
        Arrays.fill(si, 1.0);
        if (mean > 0) {
            double[] sum = new double[12];
            int[] cnt = new int[12];
            for (int i = 0; i < n; i++) {
                int m = first.plusMonths(i).getMonthValue() - 1;
                sum[m] += y[i];
                cnt[m]++;
            }
            double total = 0;
            for (int m = 0; m < 12; m++) {
                double raw = cnt[m] > 0 ? (sum[m] / cnt[m]) / mean : 1.0;
                si[m] = Math.max(0.3, 1 + 0.7 * (raw - 1));
                total += si[m];
            }
            for (int m = 0; m < 12; m++) si[m] = si[m] * 12 / total;
        }

        // 2. OLS trend on the de-seasonalised series
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            d[i] = y[i] / si[first.plusMonths(i).getMonthValue() - 1];
            sx += i; sy += d[i]; sxx += (double) i * i; sxy += i * d[i];
        }
        double denom = n * sxx - sx * sx;
        double slope = denom == 0 ? 0 : (n * sxy - sx * sy) / denom;
        double intercept = (sy - slope * sx) / n;

        double dMean = sy / n, ssTot = 0, ssRes = 0, se = 0, mae = 0;
        for (int i = 0; i < n; i++) {
            double fit = intercept + slope * i;
            ssTot += (d[i] - dMean) * (d[i] - dMean);
            ssRes += (d[i] - fit) * (d[i] - fit);
            double fitted = Math.max(0, fit * si[first.plusMonths(i).getMonthValue() - 1]);
            double res = y[i] - fitted;
            se += res * res;
            mae += Math.abs(res);
        }
        double r2 = ssTot == 0 ? 0 : Math.max(0, 1 - ssRes / ssTot);
        double sigma = n > 2 ? Math.sqrt(se / (n - 2)) : 0;
        mae /= n;

        // 3 & 4. forecast with band
        List<String> hl = new ArrayList<>(), fl = new ArrayList<>();
        List<Double> hv = new ArrayList<>(), fv = new ArrayList<>(), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int i = 0; i < n; i++) { hl.add(LABEL.format(first.plusMonths(i))); hv.add(round1(y[i])); }
        double peak = -1;
        String peakLabel = "";
        for (int k = 1; k <= horizon; k++) {
            YearMonth ym = last.plusMonths(k);
            double f = Math.max(0, (intercept + slope * (n - 1 + k)) * si[ym.getMonthValue() - 1]);
            fl.add(LABEL.format(ym));
            fv.add(round1(f));
            lo.add(round1(Math.max(0, f - 1.96 * sigma)));
            hi.add(round1(f + 1.96 * sigma));
            if (f > peak) { peak = f; peakLabel = LABEL.format(ym); }
        }

        double relSlope = mean > 0 ? slope / mean : 0;
        String trend = relSlope > 0.005 ? "Rising" : relSlope < -0.005 ? "Falling" : "Stable";
        double ma = n >= 3 ? (y[n - 1] + y[n - 2] + y[n - 3]) / 3 : mean;

        String insight;
        if (mean == 0) insight = "No approved leave in the last " + n + " months, so no pattern can be learned yet.";
        else if (peak > mean * 1.25) insight = "Expect elevated absence around " + peakLabel + " (about " + round1(peak) + " days against a " + round1(mean) + "-day monthly average). Plan cover early.";
        else if (peak < mean * 0.75) insight = "Absence should stay light through the next " + horizon + " months.";
        else insight = "Absence should stay close to the usual " + round1(mean) + " days a month.";

        return new ForecastResult(scope, hl, hv, fl, fv, lo, hi, trend, round1(relSlope * 100), round2(r2), round1(mae),
                round1(ma), round1(mean), insight);
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
