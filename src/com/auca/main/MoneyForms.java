package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.OverpaymentException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.penalty.Penalty;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxclearance.TaxClearance;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxobligation.TaxObligation;
import com.auca.taxpayment.TaxPayment;
import com.auca.taxrefund.TaxRefund;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Menu options 12-15: payments, penalties, refunds and tax clearances. */
public final class MoneyForms {
    private static final String[] STEPS_6 = {"IDENTIFICATION / PREREQUISITES", "DETAILS", "VALIDATION / CALCULATIONS", "REVIEW ALL 6 FIELDS"};

    private MoneyForms() {
    }

    // ---------- 12. Payment ----------
    private static TaxPayment buildPayment(String[] v) throws ValidationException {
        return new TaxPayment(v[0], Store.assessment(v[1]), v[2], Check.parseMoney("Amount", v[3]),
                Check.date("Payment date", v[4]), v[5]);
    }

    public static void recordPayment() {
        Form.Field[] fields = {
            Form.field(1, "Payment ID", "String, unique, 4-30", Form.unique("PAYMENT", "Payment ID", Form.id("Payment ID"))),
            Form.ref(1, "Assessment ID", "TaxAssessment, CONFIRMED", (s, v) -> {
                TaxAssessment a = Store.assessment(s);
                if (!"CONFIRMED".equals(a.getStatus())) {
                    throw new PrerequisiteMissingException("Assessment " + a.getId() + " is " + a.getStatus() + "; payments need a CONFIRMED assessment (use menu 8).");
                }
                if (Calc.outstanding(a).signum() == 0) {
                    throw new OverpaymentException("Assessment " + a.getId() + " has no outstanding balance.");
                }
                return a.getId();
            }, TaxForms::assessDeclaration),
            Form.field(2, "Payment reference", "String, unique globally, 4-30", Form.unique("REF", "Payment reference", Form.id("Payment reference"))),
            Form.field(2, "Amount", "BigDecimal >0 and <= balance", (s, v) -> {
                BigDecimal amt = Check.parseMoney("Amount", s);
                BigDecimal out = Calc.outstanding(Store.assessment(v[1]));
                if (amt.signum() <= 0) {
                    throw new ValidationException("Payment amount must be greater than zero.");
                }
                if (amt.compareTo(out) > 0) {
                    throw new OverpaymentException("Amount " + amt + " exceeds the outstanding balance " + out.toPlainString() + ".");
                }
                return amt.toPlainString();
            }),
            Form.field(3, "Payment date", "LocalDate YYYY-MM-DD, not future", Form.pastDate("Payment date")),
            Form.field(3, "Payment method", "CASH/BANK_TRANSFER/MOBILE_MONEY/CARD",
                    Form.choice("Payment method", "CASH", "BANK_TRANSFER", "MOBILE_MONEY", "CARD"))
        };
        new Form("RECORD TAX PAYMENT", STEPS_6, fields, v -> buildPayment(v).validate(), v -> Store.add(buildPayment(v))).run();
    }

    // ---------- 13. Penalty ----------
    private static Penalty buildPenalty(String[] v) throws ValidationException {
        return new Penalty(v[0], Store.obligation(v[1]), v[2], Check.parseMoney("Base amount", v[3]),
                Check.parseRate("Penalty rate", v[4]), Check.parseMoney("Penalty amount", v[5]));
    }

    public static void recordPenalty() {
        Form.Field[] fields = {
            Form.field(1, "Penalty ID", "String, unique, 4-30", Form.unique("PENALTY", "Penalty ID", Form.id("Penalty ID"))),
            Form.ref(1, "Obligation ID", "TaxObligation with LATE declaration", (s, v) -> {
                TaxObligation o = Store.obligation(s);
                TaxDeclaration d = Store.activeDeclarationFor(o);
                if (d == null || !d.isLate()) {
                    throw new PrerequisiteMissingException("Obligation " + o.getId() + " has no LATE declaration, so no late-filing penalty applies.");
                }
                return o.getId();
            }, TaxForms::createObligation),
            Form.field(2, "Penalty type", "String LATE_FILING, unique per obligation", (s, v) -> {
                String type = Check.choice("Penalty type", s, "LATE_FILING");
                if (Store.penaltyExists(Store.obligation(v[1]), type)) {
                    throw new DuplicateRecordException("A " + type + " penalty already exists for obligation " + v[1] + ".");
                }
                return type;
            }),
            Form.field(2, "Base amount", "BigDecimal >=0, 2 dp", Form.money("Base amount")),
            Form.field(3, "Penalty rate (%)", "BigDecimal 0-100 (illustrative 5)", Form.rate("Penalty rate")),
            Form.auto(3, "Penalty amount", "base x rate / 100 HALF_UP (auto)")
        };
        new Form("CALCULATE / RECORD PENALTY", STEPS_6, fields, v -> {
            v[5] = Calc.penalty(Check.parseMoney("Base amount", v[3]), Check.parseRate("Penalty rate", v[4])).toPlainString();
            buildPenalty(v).validate();
        }, v -> Store.add(buildPenalty(v))).run();
    }

    // ---------- 14. Refund ----------
    private static TaxRefund buildRefund(String[] v) throws ValidationException {
        return new TaxRefund(v[0], Store.taxpayer(v[1]), Store.assessment(v[2]), Check.parseMoney("Requested amount", v[3]),
                Check.date("Request date", v[4]), v[5], null);
    }

    public static void refundMenu() {
        System.out.println("  [1] Request refund   [2] Approve / reject pending refund   [0] Back");
        String c = Input.line("  Choice : ");
        if (c.equals("1")) {
            requestRefund();
        } else if (c.equals("2")) {
            decideRefund();
        } else if (!c.equals("0")) {
            System.out.println("  ! Unknown option '" + c + "'.");
        }
    }

    private static void requestRefund() {
        Form.Field[] fields = {
            Form.field(1, "Refund ID", "String, unique, 4-30", Form.unique("REFUND", "Refund ID", Form.id("Refund ID"))),
            Form.ref(1, "Taxpayer (TIN/ID)", "RegisteredTaxpayer, ACTIVE", TaxForms.taxpayerRule(), TaxpayerForms::registerAny),
            Form.ref(1, "Assessment ID", "TaxAssessment of that taxpayer", (s, v) -> {
                TaxAssessment a = Store.assessment(s);
                if (!Calc.owner(a).getTin().equals(v[1])) {
                    throw new ValidationException("Assessment " + a.getId() + " does not belong to taxpayer " + v[1] + ".");
                }
                if (!"CONFIRMED".equals(a.getStatus())) {
                    throw new PrerequisiteMissingException("Assessment " + a.getId() + " is not CONFIRMED.");
                }
                if (Store.pendingRefundFor(a)) {
                    throw new DuplicateRecordException("Assessment " + a.getId() + " already has a pending refund.");
                }
                return a.getId();
            }, TaxForms::assessDeclaration),
            Form.field(2, "Requested amount", "BigDecimal >0 and <= overpayment", (s, v) -> {
                BigDecimal amt = Check.parseMoney("Requested amount", s);
                BigDecimal over = Calc.overpayment(Store.assessment(v[2]));
                if (over.signum() == 0) {
                    throw new OverpaymentException("No verified overpayment exists for assessment " + v[2] + "; a refund cannot be requested.");
                }
                if (amt.signum() <= 0 || amt.compareTo(over) > 0) {
                    throw new OverpaymentException("Refund must be > 0 and cannot exceed the verified overpayment " + over.toPlainString() + ".");
                }
                return amt.toPlainString();
            }),
            Form.field(2, "Request date", "LocalDate YYYY-MM-DD, not future", Form.pastDate("Request date")),
            Form.auto(3, "Status", "String PENDING (auto)")
        };
        new Form("REQUEST TAX REFUND", STEPS_6, fields, v -> {
            v[5] = "PENDING";
            buildRefund(v).validate();
        }, v -> Store.add(buildRefund(v))).run();
    }

    private static void decideRefund() {
        try {
            TaxRefund r = Store.refund(Input.line("Refund ID : "));
            r.printDetails();
            System.out.println("  Verified overpayment available: " + Calc.overpayment(r.getAssessment()).toPlainString());
            RevenueOfficer officer = Store.officer(Input.line("Approving officer (staff number / ID) : "));
            System.out.println("  [1] Approve   [2] Reject   [0] Back");
            String c = Input.line("Choice : ");
            if (c.equals("1")) {
                r.validate();
                if (!officer.isActive()) {
                    throw new UnauthorizedApprovalException("Officer " + officer.getStaffNumber() + " is not active and cannot approve.");
                }
                if (!officer.canApprove(r.getRequestedAmount())) {
                    throw new UnauthorizedApprovalException("Refund " + r.getRequestedAmount() + " is above the approval limit " + officer.getApprovalLimit() + " of officer " + officer.getStaffNumber() + ".");
                }
                if (r.getRequestedAmount().compareTo(Calc.overpayment(r.getAssessment())) > 0) {
                    throw new OverpaymentException("The supporting overpayment is no longer sufficient for this refund.");
                }
                if (Input.yes("  Approve refund " + r.getId() + "?")) {
                    r.approve(officer);
                    System.out.println("  Refund " + r.getId() + " APPROVED by " + officer.getStaffNumber() + ".");
                } else {
                    System.out.println("  Nothing changed.");
                }
            } else if (c.equals("2")) {
                if (!officer.isActive()) {
                    throw new UnauthorizedApprovalException("Officer " + officer.getStaffNumber() + " is not active and cannot decide refunds.");
                }
                if (Input.yes("  Reject refund " + r.getId() + "?")) {
                    r.reject();
                    System.out.println("  Refund " + r.getId() + " REJECTED.");
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

    // ---------- 15. Clearance ----------
    private static TaxClearance buildClearance(String[] v) throws ValidationException {
        return new TaxClearance(v[0], Store.taxpayer(v[1]), Check.date("Request date", v[2]),
                Check.date("Expiry date", v[3]), v[4], null);
    }

    public static void clearanceMenu() {
        System.out.println("  [1] Request clearance   [2] Issue / reject clearance   [0] Back");
        String c = Input.line("  Choice : ");
        if (c.equals("1")) {
            requestClearance();
        } else if (c.equals("2")) {
            issueClearance();
        } else if (!c.equals("0")) {
            System.out.println("  ! Unknown option '" + c + "'.");
        }
    }

    private static void requestClearance() {
        Form.Field[] fields = {
            Form.field(1, "Clearance ID", "String, unique, 4-30", Form.unique("CLEARANCE", "Clearance ID", Form.id("Clearance ID"))),
            Form.ref(1, "Taxpayer (TIN/ID)", "RegisteredTaxpayer, ACTIVE", TaxForms.taxpayerRule(), TaxpayerForms::registerAny),
            Form.field(2, "Request date", "LocalDate YYYY-MM-DD, not future", Form.pastDate("Request date")),
            Form.field(2, "Expiry date", "LocalDate, after request date", (s, v) -> {
                LocalDate exp = Check.date("Expiry date", s);
                if (!exp.isAfter(Check.date("Request date", v[2]))) {
                    throw new ValidationException("Expiry date must be after the request date " + v[2] + ".");
                }
                return exp.toString();
            }),
            Form.auto(3, "Status", "String PENDING (auto)"),
            Form.auto(3, "Approved by", "RevenueOfficer (set on approval)")
        };
        new Form("REQUEST TAX CLEARANCE", STEPS_6, fields, v -> {
            v[4] = "PENDING";
            v[5] = "(none yet)";
            buildClearance(v).validate();
        }, v -> Store.add(buildClearance(v))).run();
    }

    private static void issueClearance() {
        try {
            TaxClearance c = Store.clearance(Input.line("Clearance ID : "));
            c.printDetails();
            RevenueOfficer officer = Store.officer(Input.line("Approving officer (staff number / ID) : "));
            System.out.println("  [1] Issue (approve)   [2] Reject   [0] Back");
            String ch = Input.line("Choice : ");
            if (ch.equals("1")) {
                c.validate();
                RegisteredTaxpayer t = c.getTaxpayer();
                String problem = Calc.clearanceProblem(t);
                if (problem != null) {
                    throw new PrerequisiteMissingException("Clearance cannot be approved: " + problem);
                }
                if (Input.yes("  Issue clearance " + c.getId() + "?")) {
                    c.approve(officer);
                    System.out.println("  Clearance " + c.getId() + " APPROVED by " + officer.getStaffNumber() + ", valid until " + c.getExpiryDate() + ".");
                } else {
                    System.out.println("  Nothing changed.");
                }
            } else if (ch.equals("2")) {
                if (!officer.isActive()) {
                    throw new UnauthorizedApprovalException("Officer " + officer.getStaffNumber() + " is not active and cannot decide clearances.");
                }
                if (Input.yes("  Reject clearance " + c.getId() + "?")) {
                    c.reject();
                    System.out.println("  Clearance " + c.getId() + " REJECTED.");
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
