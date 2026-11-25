package com.auca.abstracts;

import com.auca.exceptions.ValidationException;
import com.auca.main.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Level 2 abstract class (extends TaxParty): tax registration data of a taxpayer. */
public abstract class RegisteredTaxpayer extends TaxParty {
    private String tin;
    private LocalDate registrationDate;
    private String taxpayerStatus;
    private String district;
    private String sector;
    private String taxOffice;

    protected RegisteredTaxpayer(String partyId, String firstName, String lastName, String email,
            String phoneNumber, LocalDate dateOfBirth, String tin, LocalDate registrationDate,
            String taxpayerStatus, String district, String sector, String taxOffice) throws ValidationException {
        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth);
        setTin(tin);
        setRegistrationDate(registrationDate);
        setTaxpayerStatus(taxpayerStatus);
        setDistrict(district);
        setSector(sector);
        setTaxOffice(taxOffice);
    }

    public String getTin() { return tin; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public String getTaxpayerStatus() { return taxpayerStatus; }
    public String getDistrict() { return district; }
    public String getSector() { return sector; }
    public String getTaxOffice() { return taxOffice; }

    public final void setTin(String v) throws ValidationException { this.tin = Check.digits("TIN", v, 9); }

    public final void setRegistrationDate(LocalDate v) throws ValidationException {
        Check.notFuture("Registration date", v);
        this.registrationDate = v;
    }

    public final void setTaxpayerStatus(String v) throws ValidationException {
        this.taxpayerStatus = Check.choice("Taxpayer status", v, "ACTIVE", "INACTIVE", "SUSPENDED");
    }

    public final void setDistrict(String v) throws ValidationException { this.district = Check.text("District", v, 2, 60); }
    public final void setSector(String v) throws ValidationException { this.sector = Check.text("Sector", v, 2, 60); }
    public final void setTaxOffice(String v) throws ValidationException { this.taxOffice = Check.text("Tax office", v, 2, 60); }

    /** Status change with transition rules (ACTIVE, INACTIVE, SUSPENDED). */
    public final void changeStatus(String next) throws ValidationException {
        String n = Check.choice("Taxpayer status", next, "ACTIVE", "INACTIVE", "SUSPENDED");
        Check.transition("TAXPAYER", taxpayerStatus, n);
        this.taxpayerStatus = n;
    }

    public abstract BigDecimal getAnnualTurnover();

    @Override
    public boolean isActive() { return "ACTIVE".equals(taxpayerStatus); }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Check.digits("TIN", tin, 9);
        Check.notFuture("Registration date", registrationDate);
        if (!getDateOfBirth().isBefore(registrationDate)) {
            throw new ValidationException("Date of birth must precede the registration date.");
        }
        Check.choice("Taxpayer status", taxpayerStatus, "ACTIVE", "INACTIVE", "SUSPENDED");
        Check.text("District", district, 2, 60);
        Check.text("Sector", sector, 2, 60);
        Check.text("Tax office", taxOffice, 2, 60);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        Check.row("TIN", tin);
        Check.row("Registration date", registrationDate);
        Check.row("Taxpayer status", taxpayerStatus);
        Check.row("District", district);
        Check.row("Sector", sector);
        Check.row("Tax office", taxOffice);
    }

    @Override
    public String getSummary() { return super.getSummary() + " | TIN " + tin + " | " + taxpayerStatus; }
}
