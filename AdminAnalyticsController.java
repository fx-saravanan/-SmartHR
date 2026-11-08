package com.smarthr.controller.admin;

import com.smarthr.service.AnalyticsService;
import com.smarthr.service.ForecastService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminAnalyticsController {

    private final AnalyticsService analytics;
    private final ForecastService forecasts;

    public AdminAnalyticsController(AnalyticsService analytics, ForecastService forecasts) {
        this.analytics = analytics;
        this.forecasts = forecasts;
    }

    @GetMapping("/admin/analytics")
    public String analytics(@RequestParam(required = false) Integer year, Model model) {
        int thisYear = LocalDate.now().getYear();
        int y = year == null ? thisYear : year;
        var byDept = analytics.leaveDaysByDepartment(y);
        var byType = analytics.leaveDaysByType(y);
        List<Integer> yearOptions = new ArrayList<>(List.of(thisYear, thisYear - 1, thisYear - 2));

        model.addAttribute("year", y);
        model.addAttribute("yearOptions", yearOptions);
        model.addAttribute("monthLabels", AnalyticsService.monthLabels());
        model.addAttribute("monthlyLeave", analytics.monthlyLeaveDays(y));
        model.addAttribute("deptLabels", new ArrayList<>(byDept.keySet()));
        model.addAttribute("deptValues", new ArrayList<>(byDept.values()));
        model.addAttribute("typeLabels", new ArrayList<>(byType.keySet()));
        model.addAttribute("typeValues", new ArrayList<>(byType.values()));
        model.addAttribute("forecast", forecasts.forecastCompany());
        model.addAttribute("deptForecasts", forecasts.forecastByDepartment());
        model.addAttribute("risks", analytics.highLeaveEmployees());
        model.addAttribute("historyMonths", ForecastService.HISTORY_MONTHS);
        return "admin/analytics";
    }
}
