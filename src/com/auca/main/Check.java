package com.auca.main;

import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Shared validation helpers (strings, dates, money, rates, statuses). */
public final class Check {
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private Check() {
    }

    // ---------- text ----------
    public static String text(String label, String v, int min, int max) throws ValidationException {
        if (v == null) {
            throw new ValidationException(label + " is required.");
        }
        String t = v.trim();
        if (t.length() < min || t.length() > max) {
            throw new ValidationException(label + " must be " + min + "-" + max + " characters (got " + t.length() + ").");
        }
        return t;
    }

    public static String id(String label, String v) throws ValidationException {
        String t = text(label, v, 4, 30);
        if (!t.matches("[A-Za-z0-9_-]+")) {
            throw new ValidationException(label + " may only contain letters, digits, '-' and '_'.");
        }
        return t;
    }

    public static String code(String v) throws ValidationException {
        String t = text("Tax code", v, 2, 20).toUpperCase();
        if (!t.matches("[A-Z0-9-]+")) {
            throw new ValidationException("Tax code may only contain letters, digits and '-'.");
        }
        return t;
    }

    public static String digits(String label, String v, int len) throws ValidationException {
        if (v == null || !v.trim().matches("\\d{" + len + "}")) {
            throw new ValidationException(label + " must be exactly " + len + " digits.");
        }
        return v.trim();
    }

    public static String email(String v) throws ValidationException {
        String t = text("Email", v, 5, 120);
        if (!t.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Email is not syntactically valid (example: name@example.com).");
        }
        return t;
    }

    public static String phone(String v) throws ValidationException {
        if (v == null || !v.trim().matches("^\\+?\\d{10,15}$")) {
            throw new ValidationException("Phone must be 10-15 digits with an optional leading +.");
        }
        return v.trim();
    }

    public static String companyNo(String v) throws ValidationException {
        String t = text("Company registration number", v, 5, 30);
        if (!t.matches("[A-Z0-9-]+")) {
            throw new ValidationException("Company registration number: 5-30 UPPERCASE letters, digits or hyphens.");
        }
        return t;
    }

    public static String choice(String label, String v, String... options) throws ValidationException {
        String t = v == null ? "" : v.trim().toUpperCase();
        for (String o : options) {
            if (o.equals(t)) {
                return o;
            }
        }
        throw new ValidationException(label + " must be one of: " + String.join(", ", options) + ".");
    }

    public static String yesNo(String label, String v) throws ValidationException {
        String t = v == null ? "" : v.trim().toUpperCase();
        if (t.equals("YES") || t.equals("Y") || t.equals("TRUE")) {
            return "YES";
        }
        if (t.equals("NO") || t.equals("N") || t.equals("FALSE")) {
            return "NO";
        }
        throw new ValidationException(label + " must be YES or NO.");
    }

    public static <T> T notNull(String label, T value) throws ValidationException {
        if (value == null) {
            throw new ValidationException(label + " must not be null.");
        }
        return value;
    }

    // ---------- dates ----------
    public static LocalDate date(String label, String v) throws ValidationException {
        if (v == null || !v.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ValidationException(label + " must use the strict format YYYY-MM-DD.");
        }
        try {
            return LocalDate.parse(v.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException(label + " is not a real calendar date (" + v.trim() + ").");
        }
    }

    public static void notFuture(String label, LocalDate d) throws ValidationException {
        notNull(label, d);
        if (d.isAfter(LocalDate.now())) {
            throw new ValidationException(label + " " + d + " cannot be in the future.");
        }
    }

    // ---------- money / rates (BigDecimal only) ----------
    public static BigDecimal amount(String label, BigDecimal v) throws ValidationException {
        notNull(label, v);
        if (v.signum() < 0) {
            throw new ValidationException(label + " must not be negative.");
        }
        if (v.scale() > 2) {
            throw new ValidationException(label + " is limited to 2 decimal places.");
        }
        if (v.compareTo(MAX_MONEY) > 0) {
            throw new ValidationException(label + " must not exceed 999999999999.99.");
        }
        return v;
    }

    public static BigDecimal signed(String label, BigDecimal v) throws ValidationException {
        notNull(label, v);
        if (v.scale() > 2) {
            throw new ValidationException(label + " is limited to 2 decimal places.");
        }
        if (v.abs().compareTo(MAX_MONEY) > 0) {
            throw new ValidationException(label + " must not exceed 999999999999.99 in absolute value.");
        }
        return v;
    }

    public static BigDecimal rate(String label, BigDecimal v) throws ValidationException {
        notNull(label, v);
        if (v.signum() < 0 || v.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException(label + " must be between 0 and 100 inclusive.");
        }
        return v;
    }

    public static BigDecimal parseMoney(String label, String s) throws ValidationException {
        if (s == null || !s.trim().matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new ValidationException(label + " must be a non-negative number, max 12 integer digits and 2 decimals.");
        }
        return new BigDecimal(s.trim()).setScale(2);
    }

    public static BigDecimal parseSigned(String label, String s) throws ValidationException {
        if (s == null || !s.trim().matches("-?\\d{1,12}(\\.\\d{1,2})?")) {
            throw new ValidationException(label + " must be a number (may be negative), max 12 integer digits and 2 decimals.");
        }
        return new BigDecimal(s.trim()).setScale(2);
    }

    public static BigDecimal parseRate(String label, String s) throws ValidationException {
        if (s == null || !s.trim().matches("\\d{1,3}(\\.\\d{1,4})?")) {
            throw new ValidationException(label + " must be a number from 0 to 100 (up to 4 decimals).");
        }
        return rate(label, new BigDecimal(s.trim()));
    }

    // ---------- status transitions ----------
    public static void transition(String entity, String from, String to) throws InvalidStatusTransitionException {
        String allowed;
        switch (entity + ":" + from) {
            case "TAXPAYER:ACTIVE": allowed = "INACTIVE,SUSPENDED"; break;
            case "TAXPAYER:INACTIVE": allowed = "ACTIVE"; break;
            case "TAXPAYER:SUSPENDED": allowed = "ACTIVE,INACTIVE"; break;
            case "DECLARATION:FILED":
            case "DECLARATION:LATE": allowed = "REJECTED"; break;
            case "ASSESSMENT:DRAFT": allowed = "CONFIRMED,CANCELLED"; break;
            case "REFUND:PENDING":
            case "CLEARANCE:PENDING": allowed = "APPROVED,REJECTED"; break;
            default: allowed = "";
        }
        if (!("," + allowed + ",").contains("," + to + ",")) {
            throw new InvalidStatusTransitionException("Invalid " + entity + " status change " + from + " -> " + to
                    + (allowed.isEmpty() ? " (no further changes allowed)." : " (allowed: " + allowed + ")."));
        }
    }

    // ---------- printing ----------
    public static void row(String label, Object value) {
        System.out.printf("  %-22s: %s%n", label, value);
    }
}
