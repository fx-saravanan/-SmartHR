package com.smarthr.controller.admin;

import com.smarthr.dto.DepartmentForm;
import com.smarthr.entity.Department;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/departments")
public class AdminDepartmentController {

    private final DepartmentService departments;

    public AdminDepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long edit, Model model) {
        DepartmentForm form = new DepartmentForm();
        if (edit != null) {
            Department d = departments.get(edit);
            form.setId(d.getId());
            form.setName(d.getName());
            form.setDescription(d.getDescription());
        }
        return show(model, form);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") DepartmentForm form, BindingResult br, Model model, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                Department d = departments.save(form);
                ra.addFlashAttribute("success", "Department \"" + d.getName() + "\" saved.");
                return "redirect:/admin/departments";
            } catch (BusinessException ex) {
                br.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        return show(model, form);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            departments.delete(id);
            ra.addFlashAttribute("success", "Department deleted.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/departments";
    }

    private String show(Model model, DepartmentForm form) {
        model.addAttribute("form", form);
        model.addAttribute("departments", departments.findAll());
        model.addAttribute("headcounts", departments.headcounts());
        return "admin/departments";
    }
}
