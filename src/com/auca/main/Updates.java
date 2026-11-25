package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxtype.TaxType;
import java.math.BigDecimal;

/** Menu option 17: update eligible records. Nothing is changed unless the user confirms. */
public final class Updates {
    private Updates() {
    }

    /** Asks a value (Enter keeps the current one). Returns null when the user types CANCEL. */
    private static String ask(String label, String current, Form.Rule rule) {
        while (true) {
            String s = Input.line("  New " + label + " [" + current + "] (Enter keeps, CANCEL aborts) : ");
            if (s.isEmpty()) {
                return current;
            }
            if (s.equalsIgnoreCase("CANCEL")) {
                return null;
            }
            try {
                return rule.check(s, null);
            } catch (ValidationException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private static boolean confirm() {
        if (Input.yes("  Apply this change?")) {
            return true;
        }
        System.out.println("  Cancelled. Record left unchanged.");
        return false;
    }

    public static void menu() {
        System.out.println("  [1] Taxpayer contact / status    [2] Officer limit / active");
        System.out.println("  [3] Tax type rate / active       [4] Reject a declaration");
        System.out.println("  [5] Adjust an assessment         [0] Back");
        String c = Input.line("  Choice : ");
        try {
            switch (c) {
                case "1": taxpayer(); break;
                case "2": officer(); break;
                case "3": taxType(); break;
                case "4": rejectDeclaration(); break;
                case "5": adjustAssessment(); break;
                case "0": break;
                default: System.out.println("  ! Unknown option '" + c + "'.");
            }
        } catch (ValidationException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    private static void taxpayer() throws ValidationException {
        RegisteredTaxpayer t = Store.taxpayer(Input.line("Taxpayer TIN/ID : "));
        System.out.println("  Current: " + t.getSummary());
        String email = ask("email", t.getEmail(), Form.email());
        if (email == null) { return; }
        String phone = ask("phone", t.getPhoneNumber(), Form.phone());
        if (phone == null) { return; }
        final String cur = t.getTaxpayerStatus();
        String status = ask("status", cur, (s, v) -> {
            String n = Check.choice("Taxpayer status", s, "ACTIVE", "INACTIVE", "SUSPENDED");
            if (!n.equals(cur)) {
                Check.transition("TAXPAYER", cur, n);
            }
            return n;
        });
        if (status == null || !confirm()) { return; }
        t.setEmail(email);
        t.setPhoneNumber(phone);
        if (!status.equals(cur)) {
            t.changeStatus(status);
        }
        System.out.println("  Updated: " + t.getSummary());
    }

    private static void officer() throws ValidationException {
        RevenueOfficer o = Store.officer(Input.line("Officer staff number / ID : "));
        System.out.println("  Current: " + o.getSummary());
        String limit = ask("approval limit", o.getApprovalLimit().toPlainString(), Form.money("Approval limit"));
        if (limit == null) { return; }
        String active = ask("active (YES/NO)", o.isActive() ? "YES" : "NO", Form.yesNo("Active"));
        if (active == null || !confirm()) { return; }
        o.setApprovalLimit(Check.parseMoney("Approval limit", limit));
        o.setActive("YES".equals(active));
        System.out.println("  Updated: " + o.getSummary());
    }

    private static void taxType() throws ValidationException {
        TaxType t = Store.taxType(Input.line("Tax type ID / code : "));
        System.out.println("  Current: " + t.getSummary());
        String rate = ask("rate (%)", t.getRate().toPlainString(), Form.rate("Rate"));
        if (rate == null) { return; }
        String active = ask("active (YES/NO)", t.isActive() ? "YES" : "NO", Form.yesNo("Active"));
        if (active == null || !confirm()) { return; }
        t.setRate(Check.parseRate("Rate", rate));
        t.setActive("YES".equals(active));
        System.out.println("  Updated: " + t.getSummary());
    }

    private static void rejectDeclaration() throws ValidationException {
        TaxDeclaration d = Store.declaration(Input.line("Declaration ID : "));
        System.out.println("  Current: " + d.getSummary());
        if (Store.liveAssessmentFor(d) != null) {
            throw new InvalidStatusTransitionException("Declaration " + d.getId() + " already has an assessment and can no longer be rejected.");
        }
        Check.transition("DECLARATION", d.getStatus(), "REJECTED");
        if (confirm()) {
            d.changeStatus("REJECTED");
            System.out.println("  Declaration " + d.getId() + " is now REJECTED (it can no longer be assessed).");
        }
    }

    private static void adjustAssessment() throws ValidationException {
        TaxAssessment a = Store.assessment(Input.line("Assessment ID : "));
        System.out.println("  Current: " + a.getSummary());
        if (!a.isActive()) {
            throw new InvalidStatusTransitionException("A CANCELLED assessment cannot be adjusted.");
        }
        RevenueOfficer officer = Store.officer(Input.line("Authorising officer (staff number / ID) : "));
        if (!officer.isActive()) {
            throw new UnauthorizedApprovalException("Officer " + officer.getStaffNumber() + " is not active and cannot authorise adjustments.");
        }
        final BigDecimal assessedTax = a.getAssessedTax();
        String adj = ask("adjustment amount", a.getAdjustmentAmount().toPlainString(), (s, v) -> {
            BigDecimal x = Check.parseSigned("Adjustment amount", s);
            if (assessedTax.add(x).signum() < 0) {
                throw new com.auca.exceptions.InvalidTaxCalculationException("Assessment total cannot be negative.");
            }
            return x.toPlainString();
        });
        if (adj == null || !confirm()) { return; }
        a.adjust(Check.parseSigned("Adjustment amount", adj));
        System.out.println("  Updated: " + a.getSummary());
        System.out.println("  Verified overpayment now available for refund: " + Calc.overpayment(a).toPlainString());
    }
}
