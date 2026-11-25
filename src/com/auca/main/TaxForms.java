package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.InvalidTaxPeriodException;
import com.auca.exceptions.ValidationException;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxobligation.TaxObligation;
import com.auca.taxtype.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Menu options 4-8: tax types, obligations, declarations, assessments. */
public final class TaxForms {
    private static final String[] STEPS_6 = {"IDENTIFICATION / PREREQUISITES", "DETAILS", "VALIDATION / CALCULATIONS", "REVIEW ALL 6 FIELDS"};

    private TaxForms() {
    }

    // ---------- shared reference rules ----------
    static Form.Rule taxpayerRule() {
        return (s, v) -> {
            RegisteredTaxpayer t = Store.taxpayer(s);
            if (!t.isActive()) {
                throw new ValidationException("Taxpayer " + t.getTin() + " is not ACTIVE (status " + t.getTaxpayerStatus() + ").");
            }
            return t.getTin();
        };
    }

    // ---------- 4. Tax type ----------
    private static TaxType buildTaxType(String[] v) throws ValidationException {
        return new TaxType(v[0], v[1], v[2], Check.parseRate("Rate", v[4]), v[3], "YES".equals(v[5]));
    }

    public static void registerTaxType() {
        Form.Field[] fields = {
            Form.field(1, "Tax type ID", "String, unique, 4-30", Form.unique("TAXTYPE", "Tax type ID", Form.id("Tax type ID"))),
            Form.field(1, "Tax code", "String, unique, 2-20 A-Z 0-9 -", Form.unique("TAXCODE", "Tax code", Form.code())),
            Form.field(2, "Tax name", "String, 2-60", Form.text("Tax name", 2, 60)),
            Form.field(2, "Filing frequency", "MONTHLY/QUARTERLY/ANNUAL", Form.choice("Filing frequency", "MONTHLY", "QUARTERLY", "ANNUAL")),
            Form.field(3, "Rate (%)", "BigDecimal 0-100 inclusive", Form.rate("Rate")),
            Form.field(3, "Active", "boolean YES/NO", Form.yesNo("Active"))
        };
        new Form("REGISTER TAX TYPE", STEPS_6, fields, v -> buildTaxType(v).validate(), v -> Store.add(buildTaxType(v))).run();
    }

    // ---------- 5. Tax obligation ----------
    private static TaxObligation buildObligation(String[] v) throws ValidationException {
        return new TaxObligation(v[0], Store.taxpayer(v[1]), Store.taxType(v[2]), Check.date("Period start", v[3]),
                Check.date("Period end", v[4]), Check.date("Due date", v[5]));
    }

    public static void createObligation() {
        Form.Field[] fields = {
            Form.field(1, "Obligation ID", "String, unique, 4-30", Form.unique("OBLIGATION", "Obligation ID", Form.id("Obligation ID"))),
            Form.ref(1, "Taxpayer (TIN/ID)", "RegisteredTaxpayer, ACTIVE", taxpayerRule(), TaxpayerForms::registerAny),
            Form.ref(1, "Tax type (ID/code)", "TaxType, active", (s, v) -> {
                TaxType t = Store.taxType(s);
                if (!t.isActive()) {
                    throw new ValidationException("Tax type " + t.getTaxCode() + " is not active.");
                }
                return t.getTaxCode();
            }, TaxForms::registerTaxType),
            Form.field(2, "Period start", "LocalDate YYYY-MM-DD", Form.date("Period start")),
            Form.field(2, "Period end", "LocalDate, >= start", (s, v) -> {
                LocalDate end = Check.date("Period end", s);
                if (end.isBefore(Check.date("Period start", v[3]))) {
                    throw new InvalidTaxPeriodException("Period end " + end + " cannot be before period start " + v[3] + ".");
                }
                return end.toString();
            }),
            Form.field(3, "Due date", "LocalDate, >= period end", (s, v) -> {
                LocalDate due = Check.date("Due date", s);
                if (due.isBefore(Check.date("Period end", v[4]))) {
                    throw new InvalidTaxPeriodException("Due date " + due + " must not precede period end " + v[4] + ".");
                }
                return due.toString();
            })
        };
        new Form("CREATE TAX OBLIGATION", STEPS_6, fields,
                v -> Store.checkObligation(buildObligation(v)), v -> Store.add(buildObligation(v))).run();
    }

    // ---------- 6. Tax declaration ----------
    private static TaxDeclaration buildDeclaration(String[] v) throws ValidationException {
        return new TaxDeclaration(v[0], Store.obligation(v[1]), Check.date("Filing date", v[2]),
                Check.parseMoney("Taxable amount", v[3]), Check.parseMoney("Declared tax", v[4]), v[5]);
    }

    public static void fileDeclaration() {
        Form.Field[] fields = {
            Form.field(1, "Declaration ID", "String, unique, 4-30", Form.unique("DECLARATION", "Declaration ID", Form.id("Declaration ID"))),
            Form.ref(1, "Obligation ID", "TaxObligation, no active declaration", (s, v) -> {
                TaxObligation o = Store.obligation(s);
                if (!o.isActive()) {
                    throw new ValidationException("Obligation " + o.getId() + " is not active (taxpayer or tax type inactive).");
                }
                if (Store.activeDeclarationFor(o) != null) {
                    throw new DuplicateRecordException("Obligation " + o.getId() + " already has an active declaration.");
                }
                return o.getId();
            }, TaxForms::createObligation),
            Form.field(2, "Filing date", "LocalDate YYYY-MM-DD, not future", Form.pastDate("Filing date")),
            Form.field(2, "Taxable amount", "BigDecimal >=0, 2 dp", Form.money("Taxable amount")),
            Form.field(3, "Declared tax", "BigDecimal >=0, 2 dp", Form.money("Declared tax")),
            Form.auto(3, "Status", "String FILED/LATE (auto)"),
            Form.auto(3, "Expected tax", "taxable x rate / 100 (auto)")
        };
        String[] steps = {"IDENTIFICATION / PREREQUISITE (obligation)", "FILING DATE AND TAXABLE AMOUNT",
            "TAX CALCULATION AND LATE CHECK", "REVIEW ALL FIELDS"};
        new Form("FILE TAX DECLARATION", steps, fields, v -> {
            TaxObligation o = Store.obligation(v[1]);
            BigDecimal taxable = Check.parseMoney("Taxable amount", v[3]);
            v[6] = Calc.expectedTax(taxable, o.getTaxType().getRate()).toPlainString();
            v[5] = Check.date("Filing date", v[2]).isAfter(o.getDueDate()) ? "LATE" : "FILED";
            buildDeclaration(v).validate();
        }, v -> Store.add(buildDeclaration(v))).run();
    }

    // ---------- 7. Tax assessment ----------
    private static TaxAssessment buildAssessment(String[] v) throws ValidationException {
        return new TaxAssessment(v[0], Store.declaration(v[1]), Check.parseMoney("Assessed tax", v[2]),
                Check.parseSigned("Adjustment amount", v[3]), Check.date("Assessment date", v[4]), v[5]);
    }

    public static void assessDeclaration() {
        Form.Field[] fields = {
            Form.field(1, "Assessment ID", "String, unique, 4-30", Form.unique("ASSESSMENT", "Assessment ID", Form.id("Assessment ID"))),
            Form.ref(1, "Declaration ID", "TaxDeclaration, not rejected", (s, v) -> {
                TaxDeclaration d = Store.declaration(s);
                if (!d.isActive()) {
                    throw new ValidationException("A rejected declaration cannot be assessed.");
                }
                if (Store.liveAssessmentFor(d) != null) {
                    throw new DuplicateRecordException("Declaration " + d.getId() + " already has a live assessment.");
                }
                return d.getId();
            }, TaxForms::fileDeclaration),
            Form.auto(3, "Assessed tax", "BigDecimal = declared tax (auto)"),
            Form.field(2, "Adjustment amount", "BigDecimal, may be negative, 2 dp", (s, v) -> {
                BigDecimal adj = Check.parseSigned("Adjustment amount", s);
                if (Store.declaration(v[1]).getDeclaredTax().add(adj).signum() < 0) {
                    throw new InvalidTaxCalculationException("Assessment total cannot be negative (declared tax "
                            + Store.declaration(v[1]).getDeclaredTax() + " + adjustment " + adj + ").");
                }
                return adj.toPlainString();
            }),
            Form.field(2, "Assessment date", "LocalDate YYYY-MM-DD, not future", Form.pastDate("Assessment date")),
            Form.auto(3, "Status", "String DRAFT (auto)")
        };
        new Form("ASSESS DECLARATION", STEPS_6, fields, v -> {
            v[2] = Store.declaration(v[1]).getDeclaredTax().toPlainString();
            v[5] = "DRAFT";
            buildAssessment(v).validate();
        }, v -> Store.add(buildAssessment(v))).run();
    }

    // ---------- 8. Confirm / cancel assessment ----------
    public static void confirmAssessment() {
        try {
            TaxAssessment a = Store.assessment(Input.line("Assessment ID : "));
            a.printDetails();
            System.out.println("  Outstanding balance now: " + Calc.outstanding(a).toPlainString());
            System.out.println("  [1] Confirm assessment   [2] Cancel assessment   [0] Back");
            String c = Input.line("Choice : ");
            if (c.equals("1")) {
                a.validate();
                if (Input.yes("  Confirm assessment " + a.getId() + "?")) {
                    a.changeStatus("CONFIRMED");
                    System.out.println("  Assessment " + a.getId() + " is now CONFIRMED.");
                } else {
                    System.out.println("  Not confirmed. Nothing changed.");
                }
            } else if (c.equals("2")) {
                if (Input.yes("  Cancel assessment " + a.getId() + "?")) {
                    a.changeStatus("CANCELLED");
                    System.out.println("  Assessment " + a.getId() + " is now CANCELLED.");
                } else {
                    System.out.println("  Nothing changed.");
                }
            } else {
                System.out.println("  Back to menu.");
            }
        } catch (ValidationException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }
}
