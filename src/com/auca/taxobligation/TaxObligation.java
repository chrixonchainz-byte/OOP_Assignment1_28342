package com.auca.taxobligation;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.InvalidTaxPeriodException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import com.auca.taxtype.TaxType;
import java.time.LocalDate;

/** Concrete class: the duty of one taxpayer to file/pay one tax type for a period. */
public class TaxObligation implements Identifiable, Validatable {
    private String obligationId;
    private RegisteredTaxpayer taxpayer;
    private TaxType taxType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;

    public TaxObligation(String obligationId, RegisteredTaxpayer taxpayer, TaxType taxType,
            LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate) throws ValidationException {
        setObligationId(obligationId);
        setTaxpayer(taxpayer);
        setTaxType(taxType);
        setPeriodStart(periodStart);
        setPeriodEnd(periodEnd);
        setDueDate(dueDate);
    }

    public String getObligationId() { return obligationId; }
    public RegisteredTaxpayer getTaxpayer() { return taxpayer; }
    public TaxType getTaxType() { return taxType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public LocalDate getDueDate() { return dueDate; }

    public final void setObligationId(String v) throws ValidationException { this.obligationId = Check.id("Obligation ID", v); }
    public final void setTaxpayer(RegisteredTaxpayer v) throws ValidationException { this.taxpayer = Check.notNull("Taxpayer", v); }
    public final void setTaxType(TaxType v) throws ValidationException { this.taxType = Check.notNull("Tax type", v); }
    public final void setPeriodStart(LocalDate v) throws ValidationException { this.periodStart = Check.notNull("Period start", v); }
    public final void setPeriodEnd(LocalDate v) throws ValidationException { this.periodEnd = Check.notNull("Period end", v); }
    public final void setDueDate(LocalDate v) throws ValidationException { this.dueDate = Check.notNull("Due date", v); }

    @Override
    public String getId() { return obligationId; }

    @Override
    public String getDisplayName() { return taxType.getTaxCode() + " " + periodStart + ".." + periodEnd + " (" + taxpayer.getTin() + ")"; }

    @Override
    public boolean isActive() { return taxpayer.isActive() && taxType.isActive(); }

    @Override
    public void validate() throws ValidationException {
        Check.id("Obligation ID", obligationId);
        Check.notNull("Taxpayer", taxpayer).validate();
        Check.notNull("Tax type", taxType).validate();
        if (!taxpayer.isActive()) {
            throw new ValidationException("Taxpayer " + taxpayer.getTin() + " must be ACTIVE (current: " + taxpayer.getTaxpayerStatus() + ").");
        }
        if (!taxType.isActive()) {
            throw new ValidationException("Tax type " + taxType.getTaxCode() + " must be active.");
        }
        Check.notNull("Period start", periodStart);
        Check.notNull("Period end", periodEnd);
        Check.notNull("Due date", dueDate);
        if (periodStart.isAfter(periodEnd)) {
            throw new InvalidTaxPeriodException("Period start " + periodStart + " must be on or before period end " + periodEnd + ".");
        }
        if (dueDate.isBefore(periodEnd)) {
            throw new InvalidTaxPeriodException("Due date " + dueDate + " must not precede period end " + periodEnd + ".");
        }
    }

    @Override
    public void printDetails() {
        Check.row("Obligation ID", obligationId);
        Check.row("Taxpayer", taxpayer.getDisplayName() + " (TIN " + taxpayer.getTin() + ")");
        Check.row("Tax type", taxType.getDisplayName() + " @ " + taxType.getRate() + "%");
        Check.row("Period start", periodStart);
        Check.row("Period end", periodEnd);
        Check.row("Due date", dueDate);
    }

    @Override
    public String getSummary() {
        return obligationId + " | TIN " + taxpayer.getTin() + " | " + taxType.getTaxCode() + " | " + periodStart + ".." + periodEnd + " | due " + dueDate;
    }
}
