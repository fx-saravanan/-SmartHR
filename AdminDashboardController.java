package com.smarthr.controller.admin;

import com.smarthr.entity.LeaveRequest;
import com.smarthr.entity.LeaveStatus;
import com.smarthr.service.AnalyticsService;
import com.smarthr.service.ForecastService;
import com.smarthr.service.LeaveService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminDashboardController {

    private final AnalyticsService analytics;
    private final LeaveService leaves;
    private final ForecastService forecasts;

    public AdminDashboardController(AnalyticsService analytics, LeaveService leaves, ForecastService forecasts) {
        this.analytics = analytics;
        this.leaves = leaves;
        this.forecasts = forecasts;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        int year = LocalDate.now().getYear();
        model.addAllAttributes(analytics.dashboardStats());
        List<LeaveRequest> pending = leaves.list(LeaveStatus.PENDING);
        model.addAttribute("pending", pending.stream().limit(6).toList());
        model.addAttribute("year", year);
        model.addAttribute("monthLabels", AnalyticsService.monthLabels());
        model.addAttribute("monthlyLeave", analytics.monthlyLeaveDays(year));
        model.addAttribute("forecast", forecasts.forecastCompany());
        return "admin/dashboard";
    }
}
