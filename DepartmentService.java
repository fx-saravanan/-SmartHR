package com.smarthr.service;

import com.smarthr.dto.DepartmentForm;
import com.smarthr.entity.Department;
import com.smarthr.exception.BusinessException;
import com.smarthr.exception.ResourceNotFoundException;
import com.smarthr.repository.DepartmentRepository;
import com.smarthr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public List<Department> findAll() { return departments.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public Department get(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department " + id + " was not found."));
    }

    /** Headcount per department id, for the department list. */
    @Transactional(readOnly = true)
    public Map<Long, Long> headcounts() {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Department d : departments.findAllByOrderByNameAsc()) map.put(d.getId(), employees.countByDepartmentId(d.getId()));
        return map;
    }

    public Department save(DepartmentForm form) {
        String name = form.getName().trim();
        Department d;
        if (form.getId() == null) {
            if (departments.existsByNameIgnoreCase(name)) throw new BusinessException("A department named \"" + name + "\" already exists.");
            d = new Department();
        } else {
            d = get(form.getId());
            departments.findByNameIgnoreCase(name).filter(o -> !o.getId().equals(d.getId())).ifPresent(o -> {
                throw new BusinessException("A department named \"" + name + "\" already exists.");
            });
        }
        d.setName(name);
        d.setDescription(form.getDescription() == null ? null : form.getDescription().trim());
        return departments.save(d);
    }

    public void delete(Long id) {
        Department d = get(id);
        if (employees.countByDepartmentId(id) > 0) {
            throw new BusinessException("\"" + d.getName() + "\" still has employees. Move them to another department first.");
        }
        departments.delete(d);
    }
}
