package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxobligation.TaxObligation;
import com.auca.taxpayment.TaxPayment;
import com.auca.taxrefund.TaxRefund;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** All fictional tax calculations and cross-record balances (illustrative, not official tax law). */
public final class Calc {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Calc() {
    }

    /** expected tax = taxableAmount x rate / 100, HALF_UP to 2 decimals. */
    public static BigDecimal expectedTax(BigDecimal taxable, BigDecimal rate) {
        return taxable.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** penalty = baseAmount x penaltyRate / 100, HALF_UP to 2 decimals. */
    public static BigDecimal penalty(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static RegisteredTaxpayer owner(TaxAssessment a) {
        return a.getDeclaration().getObligation().getTaxpayer();
    }

    public static boolean sameTaxpayer(RegisteredTaxpayer a, RegisteredTaxpayer b) {
        return a.getId().equals(b.getId());
    }

    public static BigDecimal penaltiesOf(TaxObligation o) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < Store.nPen; i++) {
            if (Store.penalties[i].getObligation() == o) {
                sum = sum.add(Store.penalties[i].getPenaltyAmount());
            }
        }
        return sum;
    }

    public static BigDecimal paidOn(TaxAssessment a) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < Store.nPay; i++) {
            TaxPayment p = Store.payments[i];
            if (p.getAssessment() == a) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    public static BigDecimal refundedOn(TaxAssessment a) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < Store.nRef; i++) {
            TaxRefund r = Store.refunds[i];
            if (r.getAssessment() == a && "APPROVED".equals(r.getStatus())) {
                sum = sum.add(r.getRequestedAmount());
            }
        }
        return sum;
    }

    /** balance = assessment total + applicable penalties - confirmed payments (may be negative). */
    public static BigDecimal balance(TaxAssessment a) {
        if ("CANCELLED".equals(a.getStatus())) {
            return BigDecimal.ZERO;
        }
        return a.total().add(penaltiesOf(a.getDeclaration().getObligation())).subtract(paidOn(a));
    }

    public static BigDecimal outstanding(TaxAssessment a) {
        BigDecimal b = balance(a);
        return b.signum() > 0 ? b : BigDecimal.ZERO;
    }

    /** Verified overpayment still available for refund (payments - total - penalties - approved refunds). */
    public static BigDecimal overpayment(TaxAssessment a) {
        if ("CANCELLED".equals(a.getStatus())) {
            return BigDecimal.ZERO;
        }
        BigDecimal over = paidOn(a).subtract(a.total())
                .subtract(penaltiesOf(a.getDeclaration().getObligation())).subtract(refundedOn(a));
        return over.signum() > 0 ? over : BigDecimal.ZERO;
    }

    /** An obligation is settled when its live declaration has a CONFIRMED assessment with nothing outstanding. */
    public static boolean settled(TaxObligation o) {
        TaxDeclaration d = Store.activeDeclarationFor(o);
        if (d == null) {
            return false;
        }
        TaxAssessment a = Store.liveAssessmentFor(d);
        return a != null && "CONFIRMED".equals(a.getStatus()) && outstanding(a).signum() == 0;
    }

    /** Returns the reason why a clearance cannot be approved, or null when the taxpayer is clean. */
    public static String clearanceProblem(RegisteredTaxpayer t) {
        if (!t.isActive()) {
            return "Taxpayer " + t.getTin() + " is not ACTIVE.";
        }
        for (int i = 0; i < Store.nObl; i++) {
            TaxObligation o = Store.obligations[i];
            if (sameTaxpayer(o.getTaxpayer(), t) && o.getDueDate().isBefore(LocalDate.now()) && !settled(o)) {
                return "Overdue unpaid obligation " + o.getId() + " (due " + o.getDueDate() + ").";
            }
        }
        for (int i = 0; i < Store.nAss; i++) {
            TaxAssessment a = Store.assessments[i];
            if (sameTaxpayer(owner(a), t) && "CONFIRMED".equals(a.getStatus()) && outstanding(a).signum() > 0) {
                return "Outstanding balance " + outstanding(a).toPlainString() + " on assessment " + a.getId() + ".";
            }
        }
        for (int i = 0; i < Store.nDecl; i++) {
            TaxDeclaration d = Store.declarations[i];
            if (sameTaxpayer(d.getObligation().getTaxpayer(), t) && d.isActive()) {
                TaxAssessment a = Store.liveAssessmentFor(d);
                if (a == null || !"CONFIRMED".equals(a.getStatus())) {
                    return "Unresolved declaration " + d.getId() + " (not yet assessed and confirmed).";
                }
            }
        }
        return null;
    }
}
