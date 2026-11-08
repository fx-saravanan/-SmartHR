-- SmartHR MySQL 8 schema (reference).
-- The application creates/updates these tables itself with spring.jpa.hibernate.ddl-auto=update in the "mysql" profile.
-- Run this script manually if you prefer to manage the schema yourself (then set ddl-auto=validate or none).

CREATE DATABASE IF NOT EXISTS smarthr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smarthr;

CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    enabled    BIT          NOT NULL,
    created_at DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS employees (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20)  NOT NULL UNIQUE,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    address       VARCHAR(255),
    designation   VARCHAR(100),
    joining_date  DATE,
    basic_salary  DECIMAL(12,2) NOT NULL DEFAULT 0,
    hra           DECIMAL(12,2) NOT NULL DEFAULT 0,
    allowances    DECIMAL(12,2) NOT NULL DEFAULT 0,
    active        BIT NOT NULL,
    department_id BIGINT,
    user_id       BIGINT UNIQUE,
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_emp_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leave_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id  BIGINT       NOT NULL,
    leave_type   VARCHAR(20)  NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    days         INT          NOT NULL,
    reason       VARCHAR(500),
    status       VARCHAR(20)  NOT NULL,
    applied_on   DATETIME(6)  NOT NULL,
    decided_on   DATETIME(6),
    decided_by   VARCHAR(150),
    admin_remark VARCHAR(500),
    CONSTRAINT fk_leave_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_leave_status_start (status, start_date),
    INDEX idx_leave_emp (employee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leave_balances (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id  BIGINT      NOT NULL,
    balance_year INT         NOT NULL,
    leave_type   VARCHAR(20) NOT NULL,
    allocated    INT         NOT NULL,
    used         INT         NOT NULL,
    CONSTRAINT uk_balance UNIQUE (employee_id, balance_year, leave_type),
    CONSTRAINT fk_bal_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attendance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT      NOT NULL,
    work_date   DATE        NOT NULL,
    check_in    TIME,
    check_out   TIME,
    status      VARCHAR(20) NOT NULL,
    CONSTRAINT uk_attendance UNIQUE (employee_id, work_date),
    CONSTRAINT fk_att_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payslips (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id       BIGINT NOT NULL,
    pay_year          INT    NOT NULL,
    pay_month         INT    NOT NULL,
    working_days      INT    NOT NULL,
    unpaid_leave_days INT    NOT NULL,
    basic             DECIMAL(12,2) NOT NULL,
    hra               DECIMAL(12,2) NOT NULL,
    allowances        DECIMAL(12,2) NOT NULL,
    gross_pay         DECIMAL(12,2) NOT NULL,
    pf_deduction      DECIMAL(12,2) NOT NULL,
    tax_deduction     DECIMAL(12,2) NOT NULL,
    unpaid_deduction  DECIMAL(12,2) NOT NULL,
    net_pay           DECIMAL(12,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    generated_on      DATETIME(6)   NOT NULL,
    CONSTRAINT uk_payslip UNIQUE (employee_id, pay_year, pay_month),
    CONSTRAINT fk_pay_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id      BIGINT       NOT NULL,
    notification_type VARCHAR(30)  NOT NULL,
    message           VARCHAR(500) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    is_read           BIT          NOT NULL,
    CONSTRAINT fk_note_user FOREIGN KEY (recipient_id) REFERENCES users(id),
    INDEX idx_note_recipient (recipient_id, is_read)
) ENGINE=InnoDB;
