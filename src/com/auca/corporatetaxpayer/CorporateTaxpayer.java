package com.auca.corporatetaxpayer;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.ValidationException;
import com.auca.main.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: a company registered as a taxpayer (6 own + 12 inherited = 18 fields). */
public class CorporateTaxpayer extends RegisteredTaxpayer {
    private String companyRegistrationNo;
    private String legalName;
    private LocalDate incorporationDate;
    private String businessSector;
    private String authorizedRepresentative;
    private BigDecimal annualTurnover;

    public CorporateTaxpayer(String partyId, String firstName, String lastName, String email,
            String phoneNumber, LocalDate dateOfBirth, String tin, LocalDate registrationDate,
            String taxpayerStatus, String district, String sector, String taxOffice,
            String companyRegistrationNo, String legalName, LocalDate incorporationDate,
            String businessSector, String authorizedRepresentative, BigDecimal annualTurnover)
            throws ValidationException {
        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth, tin, registrationDate,
                taxpayerStatus, district, sector, taxOffice);
        setCompanyRegistrationNo(companyRegistrationNo);
        setLegalName(legalName);
        setIncorporationDate(incorporationDate);
        setBusinessSector(businessSector);
        setAuthorizedRepresentative(authorizedRepresentative);
        setAnnualTurnover(annualTurnover);
    }

    public String getCompanyRegistrationNo() { return companyRegistrationNo; }
    public String getLegalName() { return legalName; }
    public LocalDate getIncorporationDate() { return incorporationDate; }
    public String getBusinessSector() { return businessSector; }
    public String getAuthorizedRepresentative() { return authorizedRepresentative; }
    @Override
    public BigDecimal getAnnualTurnover() { return annualTurnover; }

    public final void setCompanyRegistrationNo(String v) throws ValidationException { this.companyRegistrationNo = Check.companyNo(v); }
    public final void setLegalName(String v) throws ValidationException { this.legalName = Check.text("Legal name", v, 3, 120); }

    public final void setIncorporationDate(LocalDate v) throws ValidationException {
        Check.notFuture("Incorporation date", v);
        this.incorporationDate = v;
    }

    public final void setBusinessSector(String v) throws ValidationException { this.businessSector = Check.text("Business sector", v, 2, 60); }
    public final void setAuthorizedRepresentative(String v) throws ValidationException { this.authorizedRepresentative = Check.text("Authorized representative", v, 2, 60); }
    public final void setAnnualTurnover(BigDecimal v) throws ValidationException { this.annualTurnover = Check.amount("Annual turnover", v); }

    @Override
    public String getPartyCategory() { return "CORPORATE TAXPAYER"; }

    @Override
    public String getDisplayName() { return legalName; }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Check.companyNo(companyRegistrationNo);
        Check.text("Legal name", legalName, 3, 120);
        Check.notFuture("Incorporation date", incorporationDate);
        if (!incorporationDate.isBefore(getRegistrationDate())) {
            throw new ValidationException("Incorporation date must precede the registration date.");
        }
        Check.text("Business sector", businessSector, 2, 60);
        Check.text("Authorized representative", authorizedRepresentative, 2, 60);
        Check.amount("Annual turnover", annualTurnover);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        Check.row("Company reg. no", companyRegistrationNo);
        Check.row("Legal name", legalName);
        Check.row("Incorporation date", incorporationDate);
        Check.row("Business sector", businessSector);
        Check.row("Authorized rep.", authorizedRepresentative);
        Check.row("Annual turnover", annualTurnover);
    }

    @Override
    public String getSummary() { return super.getSummary() + " | company " + companyRegistrationNo + " | turnover " + annualTurnover; }
}
