package com.smarthr.controller.admin;

import com.smarthr.entity.Payslip;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.PayrollService;
import com.smarthr.service.PayslipPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin/payroll")
public class AdminPayrollController {

    private final PayrollService payroll;
    private final PayslipPdfService pdf;

    public AdminPayrollController(PayrollService payroll, PayslipPdfService pdf) {
        this.payroll = payroll;
        this.pdf = pdf;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month, Model model) {
        YearMonth ym = (year == null || month == null || month < 1 || month > 12) ? YearMonth.now().minusMonths(1) : YearMonth.of(year, month);
        List<Payslip> slips = payroll.forPeriod(ym.getYear(), ym.getMonthValue());
        model.addAttribute("year", ym.getYear());
        model.addAttribute("month", ym.getMonthValue());
        model.addAttribute("slips", slips);
        model.addAttribute("totalNet", slips.stream().map(Payslip::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totalGross", slips.stream().map(Payslip::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("years", List.of(YearMonth.now().getYear(), YearMonth.now().getYear() - 1, YearMonth.now().getYear() - 2));
        return "admin/payroll";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam int year, @RequestParam int month, @RequestParam(defaultValue = "false") boolean overwrite,
                           RedirectAttributes ra) {
        try {
            YearMonth.of(year, month);
            var s = payroll.generate(year, month, overwrite);
            ra.addFlashAttribute("success", "Payroll run finished: " + s.created() + " created, " + s.updated() + " refreshed, "
                    + s.skipped() + " skipped (no salary set, already paid or already generated).");
        } catch (java.time.DateTimeException ex) {
            ra.addFlashAttribute("error", "Choose a valid month.");
        }
        return "redirect:/admin/payroll?year=" + year + "&month=" + month;
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Payslip p = payroll.markPaid(id);
            ra.addFlashAttribute("success", p.getEmployee().getFullName() + "'s salary for " + p.getPeriodLabel() + " is marked as paid.");
            return "redirect:/admin/payroll?year=" + p.getPayYear() + "&month=" + p.getPayMonth();
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/payroll";
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Payslip p = payroll.get(id);
        String name = "payslip-" + p.getEmployee().getEmployeeCode() + "-" + p.getPayYear() + "-" + String.format("%02d", p.getPayMonth()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.generate(p));
    }
}
