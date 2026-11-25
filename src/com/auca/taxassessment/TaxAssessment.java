package com.auca.taxassessment;

import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import com.auca.taxdeclaration.TaxDeclaration;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: the officer's assessment of a declaration (DRAFT, CONFIRMED or CANCELLED). */
public class TaxAssessment implements Identifiable, Validatable {
    private String assessmentId;
    private TaxDeclaration declaration;
    private BigDecimal assessedTax;
    private BigDecimal adjustmentAmount;
    private LocalDate assessmentDate;
    private String status;

    public TaxAssessment(String assessmentId, TaxDeclaration declaration, BigDecimal assessedTax,
            BigDecimal adjustmentAmount, LocalDate assessmentDate, String status) throws ValidationException {
        setAssessmentId(assessmentId);
        setDeclaration(declaration);
        setAssessedTax(assessedTax);
        setAdjustmentAmount(adjustmentAmount);
        setAssessmentDate(assessmentDate);
        setStatus(status);
    }

    public String getAssessmentId() { return assessmentId; }
    public TaxDeclaration getDeclaration() { return declaration; }
    public BigDecimal getAssessedTax() { return assessedTax; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public String getStatus() { return status; }

    public final void setAssessmentId(String v) throws ValidationException { this.assessmentId = Check.id("Assessment ID", v); }
    public final void setDeclaration(TaxDeclaration v) throws ValidationException { this.declaration = Check.notNull("Declaration", v); }
    public final void setAssessedTax(BigDecimal v) throws ValidationException { this.assessedTax = Check.amount("Assessed tax", v); }
    public final void setAdjustmentAmount(BigDecimal v) throws ValidationException { this.adjustmentAmount = Check.signed("Adjustment amount", v); }

    public final void setAssessmentDate(LocalDate v) throws ValidationException {
        Check.notFuture("Assessment date", v);
        this.assessmentDate = v;
    }

    public final void setStatus(String v) throws ValidationException {
        this.status = Check.choice("Assessment status", v, "DRAFT", "CONFIRMED", "CANCELLED");
    }

    /** Assessment total = assessedTax + adjustmentAmount. */
    public BigDecimal total() { return assessedTax.add(adjustmentAmount); }

    public void changeStatus(String next) throws ValidationException {
        String n = Check.choice("Assessment status", next, "DRAFT", "CONFIRMED", "CANCELLED");
        Check.transition("ASSESSMENT", status, n);
        this.status = n;
    }

    /** Authorised adjustment; the new total may never be negative. */
    public void adjust(BigDecimal newAdjustment) throws ValidationException {
        Check.signed("Adjustment amount", newAdjustment);
        if (assessedTax.add(newAdjustment).signum() < 0) {
            throw new InvalidTaxCalculationException("Assessment total cannot be negative (assessed tax " + assessedTax + " + adjustment " + newAdjustment + ").");
        }
        this.adjustmentAmount = newAdjustment;
    }

    @Override
    public String getId() { return assessmentId; }

    @Override
    public String getDisplayName() { return "Assessment " + assessmentId; }

    @Override
    public boolean isActive() { return !"CANCELLED".equals(status); }

    @Override
    public void validate() throws ValidationException {
        Check.id("Assessment ID", assessmentId);
        Check.notNull("Declaration", declaration).validate();
        if ("REJECTED".equals(declaration.getStatus())) {
            throw new ValidationException("A rejected declaration cannot be assessed.");
        }
        Check.amount("Assessed tax", assessedTax);
        Check.signed("Adjustment amount", adjustmentAmount);
        if (total().signum() < 0) {
            throw new InvalidTaxCalculationException("Assessment total cannot be negative (" + total().toPlainString() + ").");
        }
        Check.notFuture("Assessment date", assessmentDate);
        if (assessmentDate.isBefore(declaration.getFilingDate())) {
            throw new ValidationException("Assessment date cannot be before the filing date " + declaration.getFilingDate() + ".");
        }
        Check.choice("Assessment status", status, "DRAFT", "CONFIRMED", "CANCELLED");
    }

    @Override
    public void printDetails() {
        Check.row("Assessment ID", assessmentId);
        Check.row("Declaration", declaration.getId() + " (" + declaration.getObligation().getTaxpayer().getTin() + ")");
        Check.row("Assessed tax", assessedTax);
        Check.row("Adjustment amount", adjustmentAmount);
        Check.row("Total", total());
        Check.row("Assessment date", assessmentDate);
        Check.row("Status", status);
    }

    @Override
    public String getSummary() {
        return assessmentId + " | " + declaration.getId() + " | TIN " + declaration.getObligation().getTaxpayer().getTin()
                + " | assessed " + assessedTax + " | adj " + adjustmentAmount + " | total " + total() + " | " + status;
    }
}
