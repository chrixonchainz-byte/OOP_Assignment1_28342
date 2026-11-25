package com.auca.penalty;

import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Calc;
import com.auca.main.Check;
import com.auca.taxobligation.TaxObligation;
import java.math.BigDecimal;

/** Concrete class: late-filing penalty = baseAmount x penaltyRate / 100 (illustrative only). */
public class Penalty implements Identifiable, Validatable {
    private String penaltyId;
    private TaxObligation obligation;
    private String penaltyType;
    private BigDecimal baseAmount;
    private BigDecimal penaltyRate;
    private BigDecimal penaltyAmount;

    public Penalty(String penaltyId, TaxObligation obligation, String penaltyType, BigDecimal baseAmount,
            BigDecimal penaltyRate, BigDecimal penaltyAmount) throws ValidationException {
        setPenaltyId(penaltyId);
        setObligation(obligation);
        setPenaltyType(penaltyType);
        setBaseAmount(baseAmount);
        setPenaltyRate(penaltyRate);
        setPenaltyAmount(penaltyAmount);
    }

    public String getPenaltyId() { return penaltyId; }
    public TaxObligation getObligation() { return obligation; }
    public String getPenaltyType() { return penaltyType; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getPenaltyRate() { return penaltyRate; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }

    public final void setPenaltyId(String v) throws ValidationException { this.penaltyId = Check.id("Penalty ID", v); }
    public final void setObligation(TaxObligation v) throws ValidationException { this.obligation = Check.notNull("Obligation", v); }
    public final void setPenaltyType(String v) throws ValidationException { this.penaltyType = Check.choice("Penalty type", v, "LATE_FILING"); }
    public final void setBaseAmount(BigDecimal v) throws ValidationException { this.baseAmount = Check.amount("Base amount", v); }
    public final void setPenaltyRate(BigDecimal v) throws ValidationException { this.penaltyRate = Check.rate("Penalty rate", v); }
    public final void setPenaltyAmount(BigDecimal v) throws ValidationException { this.penaltyAmount = Check.amount("Penalty amount", v); }

    @Override
    public String getId() { return penaltyId; }

    @Override
    public String getDisplayName() { return penaltyType + " penalty on " + obligation.getId(); }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Check.id("Penalty ID", penaltyId);
        Check.notNull("Obligation", obligation).validate();
        Check.choice("Penalty type", penaltyType, "LATE_FILING");
        Check.amount("Base amount", baseAmount);
        Check.rate("Penalty rate", penaltyRate);
        BigDecimal expected = Calc.penalty(baseAmount, penaltyRate);
        if (expected.compareTo(penaltyAmount) != 0) {
            throw new InvalidTaxCalculationException("Penalty amount " + penaltyAmount.toPlainString() + " must equal baseAmount x rate / 100 = " + expected.toPlainString() + ".");
        }
    }

    @Override
    public void printDetails() {
        Check.row("Penalty ID", penaltyId);
        Check.row("Obligation", obligation.getSummary());
        Check.row("Penalty type", penaltyType);
        Check.row("Base amount", baseAmount);
        Check.row("Penalty rate (%)", penaltyRate);
        Check.row("Penalty amount", penaltyAmount);
    }

    @Override
    public String getSummary() {
        return penaltyId + " | " + obligation.getId() + " | " + penaltyType + " | base " + baseAmount + " @ " + penaltyRate + "% = " + penaltyAmount;
    }
}
