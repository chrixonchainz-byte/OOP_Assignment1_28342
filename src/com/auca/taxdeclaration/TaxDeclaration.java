package com.auca.taxdeclaration;

import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.InvalidTaxPeriodException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Calc;
import com.auca.main.Check;
import com.auca.taxobligation.TaxObligation;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: a taxpayer's declaration for one obligation (FILED, LATE or REJECTED). */
public class TaxDeclaration implements Identifiable, Validatable {
    private String declarationId;
    private TaxObligation obligation;
    private LocalDate filingDate;
    private BigDecimal taxableAmount;
    private BigDecimal declaredTax;
    private String status;

    public TaxDeclaration(String declarationId, TaxObligation obligation, LocalDate filingDate,
            BigDecimal taxableAmount, BigDecimal declaredTax, String status) throws ValidationException {
        setDeclarationId(declarationId);
        setObligation(obligation);
        setFilingDate(filingDate);
        setTaxableAmount(taxableAmount);
        setDeclaredTax(declaredTax);
        setStatus(status);
    }

    public String getDeclarationId() { return declarationId; }
    public TaxObligation getObligation() { return obligation; }
    public LocalDate getFilingDate() { return filingDate; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getDeclaredTax() { return declaredTax; }
    public String getStatus() { return status; }

    public final void setDeclarationId(String v) throws ValidationException { this.declarationId = Check.id("Declaration ID", v); }
    public final void setObligation(TaxObligation v) throws ValidationException { this.obligation = Check.notNull("Obligation", v); }

    public final void setFilingDate(LocalDate v) throws ValidationException {
        Check.notFuture("Filing date", v);
        this.filingDate = v;
    }

    public final void setTaxableAmount(BigDecimal v) throws ValidationException { this.taxableAmount = Check.amount("Taxable amount", v); }
    public final void setDeclaredTax(BigDecimal v) throws ValidationException { this.declaredTax = Check.amount("Declared tax", v); }

    public final void setStatus(String v) throws ValidationException {
        this.status = Check.choice("Declaration status", v, "FILED", "LATE", "REJECTED");
    }

    /** Controlled status change (only FILED/LATE -> REJECTED is allowed). */
    public void changeStatus(String next) throws ValidationException {
        String n = Check.choice("Declaration status", next, "FILED", "LATE", "REJECTED");
        Check.transition("DECLARATION", status, n);
        this.status = n;
    }

    public BigDecimal getExpectedTax() { return Calc.expectedTax(taxableAmount, obligation.getTaxType().getRate()); }
    public boolean isLate() { return "LATE".equals(status); }

    @Override
    public String getId() { return declarationId; }

    @Override
    public String getDisplayName() { return "Declaration " + declarationId + " for " + obligation.getId(); }

    @Override
    public boolean isActive() { return !"REJECTED".equals(status); }

    @Override
    public void validate() throws ValidationException {
        Check.id("Declaration ID", declarationId);
        Check.notNull("Obligation", obligation).validate();
        Check.notFuture("Filing date", filingDate);
        if (filingDate.isBefore(obligation.getPeriodStart())) {
            throw new InvalidTaxPeriodException("Filing date " + filingDate + " cannot be before the period start " + obligation.getPeriodStart() + ".");
        }
        Check.amount("Taxable amount", taxableAmount);
        Check.amount("Declared tax", declaredTax);
        BigDecimal expected = getExpectedTax();
        if (declaredTax.subtract(expected).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new InvalidTaxCalculationException("Declared tax " + declaredTax.toPlainString() + " does not match expected tax "
                    + expected.toPlainString() + " (taxable amount x rate / 100, rounded HALF_UP).");
        }
        boolean late = filingDate.isAfter(obligation.getDueDate());
        if (!"REJECTED".equals(status) && late != "LATE".equals(status)) {
            throw new InvalidStatusTransitionException("Status must be LATE when filed after the due date "
                    + obligation.getDueDate() + ", otherwise FILED.");
        }
    }

    @Override
    public void printDetails() {
        Check.row("Declaration ID", declarationId);
        Check.row("Obligation", obligation.getSummary());
        Check.row("Filing date", filingDate);
        Check.row("Taxable amount", taxableAmount);
        Check.row("Declared tax", declaredTax);
        Check.row("Expected tax", getExpectedTax());
        Check.row("Status", status);
    }

    @Override
    public String getSummary() {
        return declarationId + " | " + obligation.getId() + " | TIN " + obligation.getTaxpayer().getTin() + " | filed " + filingDate
                + " | taxable " + taxableAmount + " | tax " + declaredTax + " | " + status;
    }
}
