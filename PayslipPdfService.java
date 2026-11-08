package com.smarthr.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smarthr.entity.Employee;
import com.smarthr.entity.Payslip;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PayslipPdfService {

    private static final Color INK = new Color(0x0E, 0x3B, 0x43);
    private static final Color TINT = new Color(0xEA, 0xF1, 0xF2);
    private static final Color RULE = new Color(0xC9, 0xD6, 0xD9);

    public byte[] generate(Payslip p) {
        Employee e = p.getEmployee();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font brand = new Font(Font.HELVETICA, 22, Font.BOLD, INK);
            Font h2 = new Font(Font.HELVETICA, 12, Font.BOLD, INK);
            Font body = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            Font bold = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
            Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            doc.add(new Paragraph("SmartHR", brand));
            Paragraph title = new Paragraph("Salary slip for " + p.getPeriodLabel(), h2);
            title.setSpacingAfter(12);
            doc.add(title);

            PdfPTable info = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 2f});
            info.setWidthPercentage(100);
            info.setSpacingAfter(14);
            row(info, "Employee", e.getFullName(), "Employee ID", e.getEmployeeCode(), body, bold);
            row(info, "Department", e.getDepartment() == null ? "-" : e.getDepartment().getName(), "Designation",
                    e.getDesignation() == null ? "-" : e.getDesignation(), body, bold);
            row(info, "Working days", String.valueOf(p.getWorkingDays()), "Unpaid leave days", String.valueOf(p.getUnpaidLeaveDays()), body, bold);
            row(info, "Email", e.getEmail(), "Status", p.getStatus().name(), body, bold);
            doc.add(info);

            PdfPTable money = new PdfPTable(new float[]{2.2f, 1f, 2.2f, 1f});
            money.setWidthPercentage(100);
            header(money, "Earnings", h2);
            header(money, "Amount (Rs.)", h2);
            header(money, "Deductions", h2);
            header(money, "Amount (Rs.)", h2);
            line(money, "Basic salary", p.getBasic(), "Provident fund (12% of basic)", p.getPfDeduction(), body);
            line(money, "House rent allowance", p.getHra(), "Income tax (illustrative slab)", p.getTaxDeduction(), body);
            line(money, "Other allowances", p.getAllowances(), "Loss of pay (unpaid leave)", p.getUnpaidDeduction(), body);
            line(money, "Gross earnings", p.getGrossPay(), "Total deductions", p.getTotalDeductions(), bold);
            doc.add(money);

            PdfPTable net = new PdfPTable(new float[]{3f, 1.2f});
            net.setWidthPercentage(100);
            net.setSpacingBefore(14);
            PdfPCell a = new PdfPCell(new Phrase("Net pay", new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE)));
            PdfPCell b = new PdfPCell(new Phrase("Rs. " + money(p.getNetPay()), new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE)));
            for (PdfPCell c : new PdfPCell[]{a, b}) {
                c.setBackgroundColor(INK);
                c.setBorder(Rectangle.NO_BORDER);
                c.setPadding(9);
            }
            b.setHorizontalAlignment(Element.ALIGN_RIGHT);
            net.addCell(a);
            net.addCell(b);
            doc.add(net);

            Paragraph foot = new Paragraph("Generated on "
                    + DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).format(p.getGeneratedOn() == null ? LocalDateTime.now() : p.getGeneratedOn())
                    + ". This is a system-generated document and does not need a signature.", small);
            foot.setSpacingBefore(18);
            doc.add(foot);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the salary slip PDF", ex);
        }
    }

    private static String money(BigDecimal v) {
        return new DecimalFormat("#,##0.00").format(v == null ? BigDecimal.ZERO : v);
    }

    private static void row(PdfPTable t, String l1, String v1, String l2, String v2, Font label, Font value) {
        t.addCell(plain(l1, label));
        t.addCell(plain(v1, value));
        t.addCell(plain(l2, label));
        t.addCell(plain(v2, value));
    }

    private static PdfPCell plain(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPadding(5);
        return c;
    }

    private static void header(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(TINT);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        if (text.startsWith("Amount")) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private static void line(PdfPTable t, String l1, BigDecimal v1, String l2, BigDecimal v2, Font f) {
        t.addCell(plain(l1, f));
        PdfPCell a = plain(money(v1), f);
        a.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a);
        t.addCell(plain(l2, f));
        PdfPCell b = plain(money(v2), f);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(b);
    }
}
