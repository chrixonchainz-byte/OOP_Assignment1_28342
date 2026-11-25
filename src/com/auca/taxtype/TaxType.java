package com.auca.taxtype;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import java.math.BigDecimal;

/** Concrete class: a (fictional) tax type with its rate and filing frequency. */
public class TaxType implements Identifiable, Validatable {
    private String taxTypeId;
    private String taxCode;
    private String taxName;
    private BigDecimal rate;
    private String filingFrequency;
    private boolean active;

    public TaxType(String taxTypeId, String taxCode, String taxName, BigDecimal rate,
            String filingFrequency, boolean active) throws ValidationException {
        setTaxTypeId(taxTypeId);
        setTaxCode(taxCode);
        setTaxName(taxName);
        setRate(rate);
        setFilingFrequency(filingFrequency);
        this.active = active;
    }

    public String getTaxTypeId() { return taxTypeId; }
    public String getTaxCode() { return taxCode; }
    public String getTaxName() { return taxName; }
    public BigDecimal getRate() { return rate; }
    public String getFilingFrequency() { return filingFrequency; }

    public final void setTaxTypeId(String v) throws ValidationException { this.taxTypeId = Check.id("Tax type ID", v); }
    public final void setTaxCode(String v) throws ValidationException { this.taxCode = Check.code(v); }
    public final void setTaxName(String v) throws ValidationException { this.taxName = Check.text("Tax name", v, 2, 60); }
    public final void setRate(BigDecimal v) throws ValidationException { this.rate = Check.rate("Rate", v); }

    public final void setFilingFrequency(String v) throws ValidationException {
        this.filingFrequency = Check.choice("Filing frequency", v, "MONTHLY", "QUARTERLY", "ANNUAL");
    }

    public void setActive(boolean active) { this.active = active; }

    @Override
    public String getId() { return taxTypeId; }

    @Override
    public String getDisplayName() { return taxCode + " - " + taxName; }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void validate() throws ValidationException {
        Check.id("Tax type ID", taxTypeId);
        Check.code(taxCode);
        Check.text("Tax name", taxName, 2, 60);
        Check.rate("Rate", rate);
        Check.choice("Filing frequency", filingFrequency, "MONTHLY", "QUARTERLY", "ANNUAL");
    }

    @Override
    public void printDetails() {
        Check.row("Tax type ID", taxTypeId);
        Check.row("Tax code", taxCode);
        Check.row("Tax name", taxName);
        Check.row("Rate (%)", rate);
        Check.row("Filing frequency", filingFrequency);
        Check.row("Active", active ? "YES" : "NO");
    }

    @Override
    public String getSummary() {
        return taxTypeId + " | " + getDisplayName() + " | " + rate + "% | " + filingFrequency + (active ? " | active" : " | inactive");
    }
}
