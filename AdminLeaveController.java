package com.smarthr.controller.admin;

import com.smarthr.entity.LeaveStatus;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.LeaveService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/leaves")
public class AdminLeaveController {

    private final LeaveService leaves;

    public AdminLeaveController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @GetMapping
    public String list(@RequestParam(required = false) LeaveStatus status, Model model) {
        model.addAttribute("requests", leaves.list(status));
        model.addAttribute("status", status);
        model.addAttribute("statuses", LeaveStatus.values());
        return "admin/leaves";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(defaultValue = "") String remark, Principal principal,
                          RedirectAttributes ra) {
        return decide(id, true, remark, principal, ra);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(defaultValue = "") String remark, Principal principal,
                         RedirectAttributes ra) {
        return decide(id, false, remark, principal, ra);
    }

    private String decide(Long id, boolean approve, String remark, Principal principal, RedirectAttributes ra) {
        try {
            var r = approve ? leaves.approve(id, principal.getName(), remark) : leaves.reject(id, principal.getName(), remark);
            ra.addFlashAttribute("success", "Leave for " + r.getEmployee().getFullName() + " was " + r.getStatus().name().toLowerCase()
                    + ". The employee has been notified.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/leaves?status=PENDING";
    }
}
