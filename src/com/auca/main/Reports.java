package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.abstracts.TaxParty;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxtype.TaxType;
import java.math.BigDecimal;

/** Menu options 9-11 and 16-21: viewing, searching, statements, reports and the polymorphism demo. */
public final class Reports {
    private Reports() {
    }

    private static void section(String title, Validatable[] arr, int n) {
        System.out.println();
        System.out.println("--- " + title + " (" + n + ") ---");
        if (n == 0) {
            System.out.println("  (none)");
        }
        for (int i = 0; i < n; i++) {
            System.out.println("  " + (i + 1) + ". " + arr[i].getSummary());
        }
    }

    // ---------- 9. View all ----------
    public static void viewAll() {
        section("Individual taxpayers", Store.individuals, Store.nInd);
        section("Corporate taxpayers", Store.corporates, Store.nCorp);
        section("Revenue officers", Store.officers, Store.nOff);
        section("Tax types", Store.taxTypes, Store.nType);
        section("Tax obligations", Store.obligations, Store.nObl);
        section("Tax declarations", Store.declarations, Store.nDecl);
        section("Tax assessments", Store.assessments, Store.nAss);
        section("Tax payments", Store.payments, Store.nPay);
        section("Penalties", Store.penalties, Store.nPen);
        section("Tax refunds", Store.refunds, Store.nRef);
        section("Tax clearances", Store.clearances, Store.nClr);
    }

    // ---------- 10 / 11. Histories ----------
    public static void declarationHistory() {
        try {
            String key = Input.line("Taxpayer TIN/ID (Enter = all taxpayers) : ");
            RegisteredTaxpayer t = key.isEmpty() ? null : Store.taxpayer(key);
            System.out.println("\n--- Declaration history ---");
            int shown = 0;
            for (int i = 0; i < Store.nDecl; i++) {
                TaxDeclaration d = Store.declarations[i];
                if (t == null || Calc.sameTaxpayer(d.getObligation().getTaxpayer(), t)) {
                    System.out.println("  " + d.getSummary());
                    shown++;
                }
            }
            System.out.println(shown == 0 ? "  (no declarations)" : "  Total: " + shown);
        } catch (ValidationException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    public static void assessmentHistory() {
        try {
            String key = Input.line("Taxpayer TIN/ID (Enter = all taxpayers) : ");
            RegisteredTaxpayer t = key.isEmpty() ? null : Store.taxpayer(key);
            System.out.println("\n--- Assessment history ---");
            int shown = 0;
            for (int i = 0; i < Store.nAss; i++) {
                TaxAssessment a = Store.assessments[i];
                if (t == null || Calc.sameTaxpayer(Calc.owner(a), t)) {
                    System.out.println("  " + a.getSummary() + " | outstanding " + Calc.outstanding(a).toPlainString());
                    shown++;
                }
            }
            System.out.println(shown == 0 ? "  (no assessments)" : "  Total: " + shown);
        } catch (ValidationException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    // ---------- 16. Search ----------
    private static int found;

    private static void hit(Identifiable[] arr, int n, String key) {
        for (int i = 0; i < n; i++) {
            if (arr[i].getId().equalsIgnoreCase(key)) {
                System.out.println("\n[" + arr[i].getClass().getSimpleName() + "]");
                ((Validatable) arr[i]).printDetails();
                found++;
            }
        }
    }

    public static void search() {
        String key = Input.line("Enter ID or TIN to search : ");
        if (key.isEmpty()) {
            System.out.println("  ! Please type something to search for.");
            return;
        }
        found = 0;
        hit(Store.individuals, Store.nInd, key);
        hit(Store.corporates, Store.nCorp, key);
        hit(Store.officers, Store.nOff, key);
        hit(Store.taxTypes, Store.nType, key);
        hit(Store.obligations, Store.nObl, key);
        hit(Store.declarations, Store.nDecl, key);
        hit(Store.assessments, Store.nAss, key);
        hit(Store.payments, Store.nPay, key);
        hit(Store.penalties, Store.nPen, key);
        hit(Store.refunds, Store.nRef, key);
        hit(Store.clearances, Store.nClr, key);
        try {
            RegisteredTaxpayer t = Store.taxpayer(key);
            boolean byIdAlready = t.getId().equalsIgnoreCase(key);
            if (!byIdAlready) {
                System.out.println("\n[" + t.getClass().getSimpleName() + "] (matched by TIN)");
                t.printDetails();
                found++;
            }
        } catch (ValidationException e) {
            System.out.println("  (no taxpayer with that TIN)");
        }
        if (found == 0) {
            System.out.println("  No record found for '" + key + "'.");
        }
    }

    // ---------- 18. Taxpayer statement ----------
    public static void statement() {
        try {
            RegisteredTaxpayer t = Store.taxpayer(Input.line("Taxpayer TIN/ID : "));
            System.out.println("\n===== TAXPAYER STATEMENT =====");
            t.printDetails();
            BigDecimal assessed = BigDecimal.ZERO;
            BigDecimal pens = BigDecimal.ZERO;
            BigDecimal paid = BigDecimal.ZERO;
            BigDecimal due = BigDecimal.ZERO;
            System.out.println("\n--- Obligations ---");
            for (int i = 0; i < Store.nObl; i++) {
                if (Calc.sameTaxpayer(Store.obligations[i].getTaxpayer(), t)) {
                    System.out.println("  " + Store.obligations[i].getSummary() + (Calc.settled(Store.obligations[i]) ? " | SETTLED" : ""));
                }
            }
            System.out.println("\n--- Declarations ---");
            for (int i = 0; i < Store.nDecl; i++) {
                if (Calc.sameTaxpayer(Store.declarations[i].getObligation().getTaxpayer(), t)) {
                    System.out.println("  " + Store.declarations[i].getSummary());
                }
            }
            System.out.println("\n--- Assessments ---");
            for (int i = 0; i < Store.nAss; i++) {
                TaxAssessment a = Store.assessments[i];
                if (Calc.sameTaxpayer(Calc.owner(a), t)) {
                    System.out.println("  " + a.getSummary());
                    if (a.isActive()) {
                        assessed = assessed.add(a.total());
                        pens = pens.add(Calc.penaltiesOf(a.getDeclaration().getObligation()));
                        paid = paid.add(Calc.paidOn(a));
                        due = due.add(Calc.outstanding(a));
                    }
                }
            }
            System.out.println("\n--- Payments ---");
            for (int i = 0; i < Store.nPay; i++) {
                if (Calc.sameTaxpayer(Calc.owner(Store.payments[i].getAssessment()), t)) {
                    System.out.println("  " + Store.payments[i].getSummary());
                }
            }
            System.out.println("\n--- Penalties ---");
            for (int i = 0; i < Store.nPen; i++) {
                if (Calc.sameTaxpayer(Store.penalties[i].getObligation().getTaxpayer(), t)) {
                    System.out.println("  " + Store.penalties[i].getSummary());
                }
            }
            System.out.println("\n--- Refunds ---");
            for (int i = 0; i < Store.nRef; i++) {
                if (Calc.sameTaxpayer(Store.refunds[i].getTaxpayer(), t)) {
                    System.out.println("  " + Store.refunds[i].getSummary());
                }
            }
            System.out.println("\n--- Clearances ---");
            for (int i = 0; i < Store.nClr; i++) {
                if (Calc.sameTaxpayer(Store.clearances[i].getTaxpayer(), t)) {
                    System.out.println("  " + Store.clearances[i].getSummary());
                }
            }
            System.out.println("\n--- Totals (live assessments) ---");
            Check.row("Assessment totals", assessed);
            Check.row("Penalties", pens);
            Check.row("Payments", paid);
            Check.row("Outstanding balance", due);
        } catch (ValidationException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    // ---------- 19. Outstanding balances ----------
    public static void outstanding() {
        System.out.println("\n--- Outstanding balances (CONFIRMED assessments) ---");
        BigDecimal total = BigDecimal.ZERO;
        int shown = 0;
        for (int i = 0; i < Store.nAss; i++) {
            TaxAssessment a = Store.assessments[i];
            if (!"CONFIRMED".equals(a.getStatus())) {
                continue;
            }
            BigDecimal out = Calc.outstanding(a);
            System.out.println("  " + a.getId() + " | TIN " + Calc.owner(a).getTin() + " | total " + a.total() + " | penalties "
                    + Calc.penaltiesOf(a.getDeclaration().getObligation()) + " | paid " + Calc.paidOn(a) + " | OUTSTANDING " + out.toPlainString());
            total = total.add(out);
            shown++;
        }
        System.out.println(shown == 0 ? "  (no confirmed assessments)" : "  Total outstanding: " + total.toPlainString());
    }

    // ---------- 20. Reports ----------
    public static void taxReports() {
        System.out.println("\n===== TAX REPORTS (illustrative data) =====");
        Check.row("Individual taxpayers", Store.nInd);
        Check.row("Corporate taxpayers", Store.nCorp);
        Check.row("Revenue officers", Store.nOff);
        Check.row("Obligations", Store.nObl);
        Check.row("Declarations", Store.nDecl);
        BigDecimal declared = BigDecimal.ZERO;
        int late = 0;
        int rejected = 0;
        for (int i = 0; i < Store.nDecl; i++) {
            TaxDeclaration d = Store.declarations[i];
            if (d.isActive()) {
                declared = declared.add(d.getDeclaredTax());
            } else {
                rejected++;
            }
            if (d.isLate()) {
                late++;
            }
        }
        Check.row("Late declarations", late);
        Check.row("Rejected declarations", rejected);
        Check.row("Declared tax (active)", declared);
        BigDecimal assessed = BigDecimal.ZERO;
        BigDecimal due = BigDecimal.ZERO;
        for (int i = 0; i < Store.nAss; i++) {
            if ("CONFIRMED".equals(Store.assessments[i].getStatus())) {
                assessed = assessed.add(Store.assessments[i].total());
                due = due.add(Calc.outstanding(Store.assessments[i]));
            }
        }
        Check.row("Confirmed assessments", assessed);
        BigDecimal paid = BigDecimal.ZERO;
        for (int i = 0; i < Store.nPay; i++) {
            paid = paid.add(Store.payments[i].getAmount());
        }
        BigDecimal pens = BigDecimal.ZERO;
        for (int i = 0; i < Store.nPen; i++) {
            pens = pens.add(Store.penalties[i].getPenaltyAmount());
        }
        BigDecimal refunded = BigDecimal.ZERO;
        int pending = 0;
        for (int i = 0; i < Store.nRef; i++) {
            if ("APPROVED".equals(Store.refunds[i].getStatus())) {
                refunded = refunded.add(Store.refunds[i].getRequestedAmount());
            } else if ("PENDING".equals(Store.refunds[i].getStatus())) {
                pending++;
            }
        }
        int cleared = 0;
        for (int i = 0; i < Store.nClr; i++) {
            if ("APPROVED".equals(Store.clearances[i].getStatus())) {
                cleared++;
            }
        }
        Check.row("Payments received", paid);
        Check.row("Penalties recorded", pens);
        Check.row("Outstanding balances", due);
        Check.row("Refunds approved", refunded);
        Check.row("Refunds pending", pending);
        Check.row("Clearances approved", cleared);
        System.out.println("\n--- Declared tax by tax type ---");
        for (int k = 0; k < Store.nType; k++) {
            TaxType ty = Store.taxTypes[k];
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < Store.nDecl; i++) {
                if (Store.declarations[i].isActive() && Store.declarations[i].getObligation().getTaxType() == ty) {
                    sum = sum.add(Store.declarations[i].getDeclaredTax());
                }
            }
            System.out.println("  " + ty.getDisplayName() + " (" + ty.getRate() + "%) : " + sum.toPlainString());
        }
        if (Store.nType == 0) {
            System.out.println("  (no tax types)");
        }
    }

    // ---------- 21. Polymorphism ----------
    public static void polymorphism() {
        TaxParty[] parties = Store.allParties();
        Identifiable[] all = Store.allIdentifiables();
        if (all.length == 0) {
            System.out.println("  Nothing to show yet - register some records first (menu 1-7).");
            return;
        }
        System.out.println("\n=== TaxParty[] : one array, three concrete runtime types ===");
        for (TaxParty p : parties) {
            System.out.println("  " + p.getClass().getSimpleName() + " -> " + p.getDisplayName() + " | " + p.getPartyCategory());
            System.out.println("      summary : " + p.getSummary());
            if (p instanceof RegisteredTaxpayer) {
                System.out.println("      role method : annual turnover = " + ((RegisteredTaxpayer) p).getAnnualTurnover());
            } else if (p instanceof RevenueOfficer) {
                RevenueOfficer o = (RevenueOfficer) p;
                System.out.println("      role method : canApprove(1000.00) = " + o.canApprove(new BigDecimal("1000.00")) + " (limit " + o.getApprovalLimit() + ")");
            }
        }
        System.out.println("\n=== Identifiable[] : parties and business records mixed ===");
        for (Identifiable x : all) {
            System.out.println("  " + x.getClass().getSimpleName() + " | id " + x.getId() + " | " + x.getDisplayName() + " | active=" + x.isActive());
        }
        System.out.println("\n(Every call above is resolved at runtime: the same method name runs different overridden code.)");
    }
}
