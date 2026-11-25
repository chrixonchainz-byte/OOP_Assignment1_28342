package com.auca.individualtaxpayer;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.ValidationException;
import com.auca.main.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: a natural person registered as a taxpayer (6 own + 12 inherited = 18 fields). */
public class IndividualTaxpayer extends RegisteredTaxpayer {
    private String nationalId;
    private String occupation;
    private String businessName;
    private LocalDate businessStartDate;
    private BigDecimal annualTurnover;
    private boolean resident;

    public IndividualTaxpayer(String partyId, String firstName, String lastName, String email,
            String phoneNumber, LocalDate dateOfBirth, String tin, LocalDate registrationDate,
            String taxpayerStatus, String district, String sector, String taxOffice,
            String nationalId, String occupation, String businessName, LocalDate businessStartDate,
            BigDecimal annualTurnover, boolean resident) throws ValidationException {
        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth, tin, registrationDate,
                taxpayerStatus, district, sector, taxOffice);
        setNationalId(nationalId);
        setOccupation(occupation);
        setBusinessName(businessName);
        setBusinessStartDate(businessStartDate);
        setAnnualTurnover(annualTurnover);
        this.resident = resident;
    }

    public String getNationalId() { return nationalId; }
    public String getOccupation() { return occupation; }
    public String getBusinessName() { return businessName; }
    public LocalDate getBusinessStartDate() { return businessStartDate; }
    @Override
    public BigDecimal getAnnualTurnover() { return annualTurnover; }
    public boolean isResident() { return resident; }

    public final void setNationalId(String v) throws ValidationException { this.nationalId = Check.digits("National ID", v, 16); }
    public final void setOccupation(String v) throws ValidationException { this.occupation = Check.text("Occupation", v, 2, 60); }
    public final void setBusinessName(String v) throws ValidationException { this.businessName = Check.text("Business name", v, 2, 120); }

    public final void setBusinessStartDate(LocalDate v) throws ValidationException {
        Check.notFuture("Business start date", v);
        this.businessStartDate = v;
    }

    public final void setAnnualTurnover(BigDecimal v) throws ValidationException { this.annualTurnover = Check.amount("Annual turnover", v); }
    public void setResident(boolean resident) { this.resident = resident; }

    @Override
    public String getPartyCategory() { return "INDIVIDUAL TAXPAYER"; }

    @Override
    public String getDisplayName() { return getFirstName() + " " + getLastName() + " (" + businessName + ")"; }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Check.digits("National ID", nationalId, 16);
        Check.text("Occupation", occupation, 2, 60);
        Check.text("Business name", businessName, 2, 120);
        Check.notFuture("Business start date", businessStartDate);
        if (!businessStartDate.isBefore(getRegistrationDate())) {
            throw new ValidationException("Business start date must precede the registration date.");
        }
        Check.amount("Annual turnover", annualTurnover);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        Check.row("National ID", nationalId);
        Check.row("Occupation", occupation);
        Check.row("Business name", businessName);
        Check.row("Business start date", businessStartDate);
        Check.row("Annual turnover", annualTurnover);
        Check.row("Resident", resident ? "YES" : "NO");
    }

    @Override
    public String getSummary() {
        return super.getSummary() + " | turnover " + annualTurnover + (resident ? " | resident" : " | non-resident");
    }
}
