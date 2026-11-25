package com.auca.revenueofficer;

import com.auca.abstracts.TaxParty;
import com.auca.exceptions.ValidationException;
import com.auca.main.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class (extends TaxParty): tax officer who approves refunds and clearances (6 + 6 = 12 fields). */
public class RevenueOfficer extends TaxParty {
    private String staffNumber;
    private String department;
    private String role;
    private LocalDate hireDate;
    private BigDecimal approvalLimit;
    private boolean active;

    public RevenueOfficer(String partyId, String firstName, String lastName, String email,
            String phoneNumber, LocalDate dateOfBirth, String staffNumber, String department,
            String role, LocalDate hireDate, BigDecimal approvalLimit, boolean active)
            throws ValidationException {
        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth);
        setStaffNumber(staffNumber);
        setDepartment(department);
        setRole(role);
        setHireDate(hireDate);
        setApprovalLimit(approvalLimit);
        this.active = active;
    }

    public String getStaffNumber() { return staffNumber; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public LocalDate getHireDate() { return hireDate; }
    public BigDecimal getApprovalLimit() { return approvalLimit; }

    public final void setStaffNumber(String v) throws ValidationException { this.staffNumber = Check.id("Staff number", v); }
    public final void setDepartment(String v) throws ValidationException { this.department = Check.text("Department", v, 2, 60); }
    public final void setRole(String v) throws ValidationException { this.role = Check.text("Role", v, 2, 60); }

    public final void setHireDate(LocalDate v) throws ValidationException {
        Check.notFuture("Hire date", v);
        this.hireDate = v;
    }

    public final void setApprovalLimit(BigDecimal v) throws ValidationException { this.approvalLimit = Check.amount("Approval limit", v); }
    public void setActive(boolean active) { this.active = active; }

    /** Role-specific rule: only an active officer with a big enough limit may approve. */
    public boolean canApprove(BigDecimal amount) {
        return active && amount != null && amount.compareTo(approvalLimit) <= 0;
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public String getPartyCategory() { return "REVENUE OFFICER"; }

    @Override
    public String getDisplayName() { return "Officer " + getFirstName() + " " + getLastName(); }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Check.id("Staff number", staffNumber);
        Check.text("Department", department, 2, 60);
        Check.text("Role", role, 2, 60);
        Check.notFuture("Hire date", hireDate);
        if (!hireDate.isAfter(getDateOfBirth())) {
            throw new ValidationException("Hire date must be after the date of birth.");
        }
        Check.amount("Approval limit", approvalLimit);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        Check.row("Staff number", staffNumber);
        Check.row("Department", department);
        Check.row("Role", role);
        Check.row("Hire date", hireDate);
        Check.row("Approval limit", approvalLimit);
        Check.row("Active", active ? "YES" : "NO");
    }

    @Override
    public String getSummary() {
        return super.getSummary() + " | staff " + staffNumber + " | limit " + approvalLimit + (active ? " | active" : " | inactive");
    }
}
