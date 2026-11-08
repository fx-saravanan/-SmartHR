# SmartHR – AI-Powered Employee Management & Leave Prediction System

Spring Boot 4 · Java 26 · Spring Security · Spring Data JPA · MySQL / H2 · Thymeleaf · Bootstrap 5 · Maven

## Requirements
* **JDK 26** (Spring Boot 4.x supports Java 17 to 26; lower `java.version` in `pom.xml` if you use an older JDK)
* **Maven 3.9+**
* MySQL 8 (only for the production profile)
* Internet access in the browser for Bootstrap, Bootstrap Icons, Chart.js and Google Fonts (loaded from CDNs)

## Quick start (H2, no database needed)
```bash
mvn spring-boot:run
```
Open http://localhost:8080. The default **h2** profile uses an in-memory database and loads demo data
(12 employees, 5 departments, 24 months of leave history, 4 weeks of attendance, 3 pending requests, last month's payroll).
Data is lost when the app stops.

| Role     | Login                         | Password       |
|----------|-------------------------------|----------------|
| Admin    | `admin@smarthr.com`           | `Admin@123`    |
| Employee | `aarav.sharma@smarthr.com` (or any `first.last@smarthr.com` in the employee list) | `Employee@123` |

New employees can also self-register at `/register`. An admin then sets their salary on the employee page.

## Production (MySQL)
1. Create the database and user (or run `database/schema-mysql.sql`, which also creates the tables):
   ```sql
   CREATE DATABASE smarthr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Configure and run:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/smarthr?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   export DB_USERNAME=root
   export DB_PASSWORD=your_password
   export ADMIN_PASSWORD='choose-a-strong-one'   # password of the admin created on first start
   export SEED_DEMO_DATA=false                   # true loads the demo data once into an empty database
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
   or build a jar: `mvn clean package` then
   `java -Dnet.bytebuddy.experimental=true -jar target/smarthr-1.0.0.jar --spring.profiles.active=mysql`.
   Tables are created and updated by Hibernate (`ddl-auto=update`). Defaults are in `application-mysql.properties`.

## Tests
```bash
mvn test        # uses the "test" profile (in-memory H2 with demo data)
```
Unit tests cover the forecasting maths and working-day counting. Integration tests cover the leave workflow
(apply, approve, reject, cancel, balance and notification effects), payroll, analytics, and security rules
(anonymous redirect, role separation, BCrypt hashes).

## What is where
```
src/main/java/com/smarthr
  config/        SecurityConfig, GlobalModelAdvice, DataInitializer (admin + demo data)
  controller/    AuthController, NotificationController, admin/*, employee/*
  service/       business logic: Employee, Department, Leave, LeaveBalance, Attendance, Payroll,
                 PayslipPdf, Forecast, Analytics, Report, Notification
  repository/    Spring Data JPA repositories
  entity/        AppUser, Employee, Department, LeaveRequest, LeaveBalance, Attendance, Payslip, Notification + enums
  dto/           form objects (validated) and view models
  exception/     BusinessException, ResourceNotFoundException, GlobalExceptionHandler
src/main/resources
  templates/     Thymeleaf pages (fragments/layout.html is the shared shell), admin/, employee/
  static/        css/app.css, js/app.js
  application*.properties   common, h2, mysql, test
database/schema-mysql.sql   reference DDL
```

## Feature notes
* **Security**: form login, BCrypt (strength 10), CSRF on all forms, `/admin/**` for ADMIN and `/employee/**` for EMPLOYEE only,
  deactivated employees cannot sign in. Each employee can only download their own salary slips.
* **Leave rules**: casual 12, sick 10, earned 15 days a year (`LeaveType`); unpaid leave has no balance and is deducted in payroll.
  Balances are pro-rated for mid-year joiners, created automatically, and rolled over every 1 January (earned leave carries up to 5 days).
  Requests count Monday to Friday only, cannot overlap, cannot exceed the available balance (pending days are held), and must stay in one calendar year.
  Only sick leave can be back-dated (30 days). Public holidays are not modelled.
* **Notifications**: employees are notified of approvals, rejections and payslips; admins of new and cancelled requests. A bell with an unread count is in the top bar.
* **Attendance**: check-in after 09:45 (`smarthr.attendance.late-after`) is Late; under 4 hours worked is Half day. Admins see a daily board with on-leave and absent employees.
* **Payroll**: gross = basic + HRA + allowances. Deductions: provident fund 12% of basic, an **illustrative** income-tax slab
  (`PayrollService.incomeTax`, replace it with your local rules), and loss of pay for approved unpaid leave days. Slips are PDFs (OpenPDF).
* **Forecast ("AI")**: `ForecastService` fits a seasonal-adjusted linear regression to 24 months of approved leave days
  (company-wide and per department) and predicts the next 3 months with a ~95% band, R², and typical error.
  It is a classical statistical model written in plain Java, not an external ML service. It needs a year or more of real history to be reliable;
  the demo data is synthetic. `/admin/analytics` also flags employees whose leave in the last six months is 1.5× the average or more.
* **Reports**: CSV downloads of employees, leave, attendance and payroll from `/admin/reports`.

## Troubleshooting
* `Unknown Java version` / Byte Buddy errors: keep the `-Dnet.bytebuddy.experimental=true` flag (already set for `spring-boot:run` and tests).
* Charts or styles missing: the browser cannot reach the CDNs.
* Port 8080 busy: `-Dspring-boot.run.arguments=--server.port=9090`.

## Put it online so anyone can open it in a browser
The app is a normal Spring Boot server, so it needs a host that runs Java or Docker. A `Dockerfile` is included.

**Option A: Railway, Render, Fly.io or any Docker host (permanent link)**
1. Push the project to a GitHub repository.
2. Create a new "Web Service" from that repo; the host detects the `Dockerfile` and builds it.
3. Set these environment variables:
   * `SPRING_PROFILES_ACTIVE=mysql`
   * `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from a managed MySQL database (Railway MySQL, Aiven, TiDB Cloud, PlanetScale-compatible, etc.)
   * `ADMIN_PASSWORD` = a strong password of your own
   * `SEED_DEMO_DATA=true` only if you want the sample employees (their password `Employee@123` is public in this README, so leave it `false` for real use)
4. The host gives you an `https://...` address. Share it.

For a quick demo without a database, leave `SPRING_PROFILES_ACTIVE` unset (H2 is the default in the Docker image). Data is lost on every restart.

**Option B: temporary link from your own PC (for a demo)**
Run `mvn spring-boot:run`, then in another terminal run a tunnel, for example `cloudflared tunnel --url http://localhost:8080`
or `ngrok http 8080`. It prints a public `https://` URL that works while your PC and the app are running.

Before sharing publicly: change the admin password, turn off demo data, and use HTTPS (hosts above provide it).
