package com.auca.taxclearance;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import com.auca.revenueofficer.RevenueOfficer;
import java.time.LocalDate;

/** Concrete class: a tax-clearance decision for a taxpayer (PENDING, APPROVED or REJECTED). */
public class TaxClearance implements Identifiable, Validatable {
    private String clearanceId;
    private RegisteredTaxpayer taxpayer;
    private LocalDate requestDate;
    private LocalDate expiryDate;
    private String status;
    private RevenueOfficer approvedBy;

    public TaxClearance(String clearanceId, RegisteredTaxpayer taxpayer, LocalDate requestDate,
            LocalDate expiryDate, String status, RevenueOfficer approvedBy) throws ValidationException {
        setClearanceId(clearanceId);
        setTaxpayer(taxpayer);
        setRequestDate(requestDate);
        setExpiryDate(expiryDate);
        setStatus(status);
        this.approvedBy = approvedBy;
    }

    public String getClearanceId() { return clearanceId; }
    public RegisteredTaxpayer getTaxpayer() { return taxpayer; }
    public LocalDate getRequestDate() { return requestDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getStatus() { return status; }
    public RevenueOfficer getApprovedBy() { return approvedBy; }

    public final void setClearanceId(String v) throws ValidationException { this.clearanceId = Check.id("Clearance ID", v); }
    public final void setTaxpayer(RegisteredTaxpayer v) throws ValidationException { this.taxpayer = Check.notNull("Taxpayer", v); }

    public final void setRequestDate(LocalDate v) throws ValidationException {
        Check.notFuture("Request date", v);
        this.requestDate = v;
    }

    public final void setExpiryDate(LocalDate v) throws ValidationException { this.expiryDate = Check.notNull("Expiry date", v); }

    public final void setStatus(String v) throws ValidationException {
        this.status = Check.choice("Clearance status", v, "PENDING", "APPROVED", "REJECTED");
    }

    public void approve(RevenueOfficer officer) throws ValidationException {
        if (officer == null || !officer.isActive()) {
            throw new UnauthorizedApprovalException("Only an ACTIVE revenue officer can issue a clearance.");
        }
        Check.transition("CLEARANCE", status, "APPROVED");
        this.status = "APPROVED";
        this.approvedBy = officer;
    }

    public void reject() throws ValidationException {
        Check.transition("CLEARANCE", status, "REJECTED");
        this.status = "REJECTED";
    }

    @Override
    public String getId() { return clearanceId; }

    @Override
    public String getDisplayName() { return "Clearance " + clearanceId; }

    @Override
    public boolean isActive() { return "APPROVED".equals(status) && !expiryDate.isBefore(LocalDate.now()); }

    @Override
    public void validate() throws ValidationException {
        Check.id("Clearance ID", clearanceId);
        Check.notNull("Taxpayer", taxpayer).validate();
        Check.notFuture("Request date", requestDate);
        Check.notNull("Expiry date", expiryDate);
        if (!expiryDate.isAfter(requestDate)) {
            throw new ValidationException("Expiry date " + expiryDate + " must be after the request date " + requestDate + ".");
        }
        Check.choice("Clearance status", status, "PENDING", "APPROVED", "REJECTED");
        if ("APPROVED".equals(status) && approvedBy == null) {
            throw new ValidationException("An approved clearance must store the approving officer.");
        }
    }

    @Override
    public void printDetails() {
        Check.row("Clearance ID", clearanceId);
        Check.row("Taxpayer", taxpayer.getDisplayName() + " (TIN " + taxpayer.getTin() + ")");
        Check.row("Request date", requestDate);
        Check.row("Expiry date", expiryDate);
        Check.row("Status", status);
        Check.row("Approved by", approvedBy == null ? "(none)" : approvedBy.getStaffNumber() + " " + approvedBy.getDisplayName());
    }

    @Override
    public String getSummary() {
        return clearanceId + " | TIN " + taxpayer.getTin() + " | " + requestDate + " -> " + expiryDate + " | " + status
                + (approvedBy == null ? "" : " | by " + approvedBy.getStaffNumber());
    }
}
