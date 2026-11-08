package com.smarthr.controller.employee;

import com.smarthr.entity.Employee;
import com.smarthr.entity.Payslip;
import com.smarthr.exception.ResourceNotFoundException;
import com.smarthr.service.EmployeeService;
import com.smarthr.service.PayrollService;
import com.smarthr.service.PayslipPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/employee/salary")
public class EmployeeSalaryController {

    private final EmployeeService employees;
    private final PayrollService payroll;
    private final PayslipPdfService pdf;

    public EmployeeSalaryController(EmployeeService employees, PayrollService payroll, PayslipPdfService pdf) {
        this.employees = employees;
        this.payroll = payroll;
        this.pdf = pdf;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Employee e = employees.getByUsername(principal.getName());
        model.addAttribute("employee", e);
        model.addAttribute("slips", payroll.forEmployee(e));
        return "employee/salary";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Principal principal) {
        Employee e = employees.getByUsername(principal.getName());
        Payslip p = payroll.get(id);
        if (!p.getEmployee().getId().equals(e.getId())) throw new ResourceNotFoundException("Salary slip " + id + " was not found.");
        String name = "payslip-" + e.getEmployeeCode() + "-" + p.getPayYear() + "-" + String.format("%02d", p.getPayMonth()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.generate(p));
    }
}
