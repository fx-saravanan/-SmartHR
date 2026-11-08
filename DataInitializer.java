package com.smarthr.config;

import com.smarthr.dto.EmployeeForm;
import com.smarthr.dto.LeaveForm;
import com.smarthr.entity.*;
import com.smarthr.repository.*;
import com.smarthr.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Creates the default admin on first start. With smarthr.seed.demo-data=true it also creates demo departments,
 * employees, 24 months of approved leave history (so the forecast has something to learn from), recent attendance,
 * a few pending requests and last month's payroll.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Month-of-year leave intensity (Jan..Dec) used to build a realistic seasonal pattern. */
    private static final double[] SEASON = {1.1, 0.8, 0.9, 1.3, 1.4, 0.9, 0.8, 1.2, 1.0, 1.1, 0.9, 1.7};

    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaveRepo;
    private final AttendanceRepository attendanceRepo;
    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final LeaveBalanceService balanceService;
    private final PayrollService payrollService;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemo;

    public DataInitializer(AppUserRepository users, DepartmentRepository departments, EmployeeRepository employees,
                           LeaveRequestRepository leaveRepo, AttendanceRepository attendanceRepo,
                           EmployeeService employeeService, LeaveService leaveService, LeaveBalanceService balanceService,
                           PayrollService payrollService, PasswordEncoder encoder, TransactionTemplate tx,
                           @Value("${smarthr.admin.email}") String adminEmail,
                           @Value("${smarthr.admin.password}") String adminPassword,
                           @Value("${smarthr.seed.demo-data:false}") boolean seedDemo) {
        this.users = users;
        this.departments = departments;
        this.employees = employees;
        this.leaveRepo = leaveRepo;
        this.attendanceRepo = attendanceRepo;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.balanceService = balanceService;
        this.payrollService = payrollService;
        this.encoder = encoder;
        this.tx = tx;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.seedDemo = seedDemo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsByUsernameIgnoreCase(adminEmail)) {
            users.save(new AppUser(adminEmail.toLowerCase(), encoder.encode(adminPassword), Role.ADMIN));
            log.info("Created default admin account {}", adminEmail);
        }
        if (seedDemo && employees.count() == 0) {
            tx.executeWithoutResult(status -> seedDemoData());
            log.info("Demo data loaded ({} employees)", employees.count());
        }
    }

    private void seedDemoData() {
        Map<String, Department> dept = new LinkedHashMap<>();
        for (String[] d : new String[][]{
                {"Engineering", "Product development and platform"}, {"Human Resources", "People operations and hiring"},
                {"Finance", "Accounts, payroll and audit"}, {"Sales & Marketing", "Growth and customer acquisition"},
                {"Operations", "Facilities, logistics and support"}}) {
            dept.put(d[0], departments.save(new Department(d[0], d[1])));
        }

        // name, department, designation, basic salary
        Object[][] people = {
                {"Aarav", "Sharma", "Engineering", "Senior Software Engineer", 85000},
                {"Priya", "Nair", "Engineering", "Software Engineer", 62000},
                {"Rohan", "Mehta", "Engineering", "QA Engineer", 48000},
                {"Divya", "Iyer", "Human Resources", "HR Manager", 70000},
                {"Karthik", "Raj", "Human Resources", "Recruiter", 38000},
                {"Sneha", "Pillai", "Finance", "Accountant", 52000},
                {"Arjun", "Menon", "Finance", "Finance Analyst", 58000},
                {"Meera", "Krishnan", "Sales & Marketing", "Marketing Lead", 66000},
                {"Vikram", "Singh", "Sales & Marketing", "Sales Executive", 42000},
                {"Ananya", "Das", "Sales & Marketing", "Content Strategist", 45000},
                {"Farhan", "Ali", "Operations", "Operations Manager", 72000},
                {"Lakshmi", "Narayanan", "Operations", "Support Specialist", 34000},
        };
        List<Employee> created = new ArrayList<>();
        int idx = 0;
        for (Object[] p : people) {
            EmployeeForm f = new EmployeeForm();
            f.setFirstName((String) p[0]);
            f.setLastName((String) p[1]);
            f.setEmail((p[0] + "." + p[1] + "@smarthr.com").toLowerCase());
            f.setPhone("98" + String.format("%08d", 41000000 + idx * 137));
            f.setDesignation((String) p[3]);
            f.setJoiningDate(LocalDate.now().minusYears(3).withDayOfMonth(1).minusMonths(idx));
            f.setDepartmentId(dept.get((String) p[2]).getId());
            BigDecimal basic = BigDecimal.valueOf((Integer) p[4]);
            f.setBasicSalary(basic);
            f.setHra(basic.multiply(new BigDecimal("0.40")));
            f.setAllowances(new BigDecimal("5000"));
            f.setPassword("Employee@123");
            created.add(employeeService.create(f));
            idx++;
        }

        seedLeaveHistory(created);
        seedAttendance(created);
        seedPendingRequests(created);

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        payrollService.generate(lastMonth.getYear(), lastMonth.getMonthValue(), true);
        payrollService.forPeriod(lastMonth.getYear(), lastMonth.getMonthValue()).forEach(p -> payrollService.markPaid(p.getId()));
    }

    /** 24 months of approved leave with seasonality and a mild upward trend. */
    private void seedLeaveHistory(List<Employee> staff) {
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        YearMonth last = YearMonth.now().minusMonths(1);
        for (int back = ForecastService.HISTORY_MONTHS - 1; back >= 0; back--) {
            YearMonth ym = last.minusMonths(back);
            double trend = 1 + 0.012 * (ForecastService.HISTORY_MONTHS - back);
            for (Employee e : staff) {
                double expected = 0.55 * SEASON[ym.getMonthValue() - 1] * trend;
                int events = rnd.nextDouble() < expected ? 1 : 0;
                if (rnd.nextDouble() < expected - 1) events++;
                for (int k = 0; k < events; k++) {
                    LeaveType type = pickType(rnd);
                    int length = type == LeaveType.EARNED ? 2 + rnd.nextInt(4) : 1 + rnd.nextInt(2);
                    LocalDate start = nextWeekday(ym.atDay(1 + rnd.nextInt(Math.max(1, ym.lengthOfMonth() - 6))));
                    LocalDate end = start.plusDays(length - 1L);
                    if (end.getMonth() != start.getMonth()) end = start;
                    int days = LeaveService.countWorkingDays(start, end);
                    if (days == 0 || leaveRepo.countOverlaps(e.getId(), List.of(LeaveStatus.APPROVED), start, end) > 0) continue;
                    if (start.getYear() == today.getYear() && type.isPaid()) {
                        LeaveBalance b = balanceService.getOrCreate(e, start.getYear(), type);
                        if (b.getRemaining() < days) continue;
                        b.setUsed(b.getUsed() + days);
                    }
                    LeaveRequest r = new LeaveRequest();
                    r.setEmployee(e);
                    r.setLeaveType(type);
                    r.setStartDate(start);
                    r.setEndDate(end);
                    r.setDays(days);
                    r.setReason(type == LeaveType.SICK ? "Not feeling well" : type == LeaveType.EARNED ? "Planned time off" : "Personal work");
                    r.setStatus(LeaveStatus.APPROVED);
                    r.setAppliedOn(start.minusDays(3).atTime(10, 0));
                    r.setDecidedOn(start.minusDays(2).atTime(15, 0));
                    r.setDecidedBy(adminEmail);
                    leaveRepo.save(r);
                }
            }
        }
    }

    private static LeaveType pickType(Random rnd) {
        int x = rnd.nextInt(100);
        return x < 40 ? LeaveType.CASUAL : x < 75 ? LeaveType.SICK : x < 95 ? LeaveType.EARNED : LeaveType.UNPAID;
    }

    private static LocalDate nextWeekday(LocalDate d) {
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d;
    }

    private void seedAttendance(List<Employee> staff) {
        Random rnd = new Random(7);
        LocalDate today = LocalDate.now();
        for (int back = 1; back <= 28; back++) {
            LocalDate day = today.minusDays(back);
            if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (Employee e : staff) {
                boolean onLeave = !leaveRepo.findCovering(LeaveStatus.APPROVED, day).stream().noneMatch(l -> l.getEmployee().getId().equals(e.getId()));
                if (onLeave || rnd.nextInt(100) < 4) continue;
                Attendance a = new Attendance();
                a.setEmployee(e);
                a.setWorkDate(day);
                LocalTime in = LocalTime.of(9, 0).plusMinutes(rnd.nextInt(65));
                LocalTime out = LocalTime.of(17, 30).plusMinutes(rnd.nextInt(75));
                a.setCheckIn(in);
                a.setCheckOut(out);
                a.setStatus(in.isAfter(LocalTime.of(9, 45)) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
                attendanceRepo.save(a);
            }
        }
    }

    /** Three pending requests so the admin approval screen has something to act on straight away. */
    private void seedPendingRequests(List<Employee> staff) {
        LocalDate base = nextWeekday(LocalDate.now().plusDays(10));
        Object[][] reqs = {{0, LeaveType.CASUAL, 2, "Family function"}, {3, LeaveType.EARNED, 4, "Trip with family"}, {7, LeaveType.SICK, 1, "Medical check-up"}};
        int offset = 0;
        for (Object[] r : reqs) {
            LeaveForm f = new LeaveForm();
            f.setLeaveType((LeaveType) r[1]);
            LocalDate start = nextWeekday(base.plusDays(offset));
            f.setStartDate(start);
            f.setEndDate(start.plusDays((Integer) r[2] - 1L));
            f.setReason((String) r[3]);
            try {
                leaveService.apply(staff.get((Integer) r[0]), f);
            } catch (RuntimeException ex) {
                log.warn("Skipped demo leave request: {}", ex.getMessage());
            }
            offset += 9;
        }
    }
}
