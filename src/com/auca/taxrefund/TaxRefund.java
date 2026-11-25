package com.auca.taxrefund;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: a refund request for an overpayment (PENDING, APPROVED or REJECTED). */
public class TaxRefund implements Identifiable, Validatable {
    private String refundId;
    private RegisteredTaxpayer taxpayer;
    private TaxAssessment assessment;
    private BigDecimal requestedAmount;
    private LocalDate requestDate;
    private String status;
    private RevenueOfficer approvedBy; // additional field: the approving officer reference

    public TaxRefund(String refundId, RegisteredTaxpayer taxpayer, TaxAssessment assessment,
            BigDecimal requestedAmount, LocalDate requestDate, String status, RevenueOfficer approvedBy)
            throws ValidationException {
        setRefundId(refundId);
        setTaxpayer(taxpayer);
        setAssessment(assessment);
        setRequestedAmount(requestedAmount);
        setRequestDate(requestDate);
        setStatus(status);
        this.approvedBy = approvedBy;
    }

    public String getRefundId() { return refundId; }
    public RegisteredTaxpayer getTaxpayer() { return taxpayer; }
    public TaxAssessment getAssessment() { return assessment; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public LocalDate getRequestDate() { return requestDate; }
    public String getStatus() { return status; }
    public RevenueOfficer getApprovedBy() { return approvedBy; }

    public final void setRefundId(String v) throws ValidationException { this.refundId = Check.id("Refund ID", v); }
    public final void setTaxpayer(RegisteredTaxpayer v) throws ValidationException { this.taxpayer = Check.notNull("Taxpayer", v); }
    public final void setAssessment(TaxAssessment v) throws ValidationException { this.assessment = Check.notNull("Assessment", v); }
    public final void setRequestedAmount(BigDecimal v) throws ValidationException { this.requestedAmount = Check.amount("Requested amount", v); }

    public final void setRequestDate(LocalDate v) throws ValidationException {
        Check.notFuture("Request date", v);
        this.requestDate = v;
    }

    public final void setStatus(String v) throws ValidationException {
        this.status = Check.choice("Refund status", v, "PENDING", "APPROVED", "REJECTED");
    }

    /** Only an ACTIVE officer whose approval limit covers the amount may approve. */
    public void approve(RevenueOfficer officer) throws ValidationException {
        if (officer == null || !officer.isActive()) {
            throw new UnauthorizedApprovalException("Only an ACTIVE revenue officer can approve a refund.");
        }
        if (!officer.canApprove(requestedAmount)) {
            throw new UnauthorizedApprovalException("Amount " + requestedAmount + " exceeds the approval limit " + officer.getApprovalLimit() + " of officer " + officer.getStaffNumber() + ".");
        }
        Check.transition("REFUND", status, "APPROVED");
        this.status = "APPROVED";
        this.approvedBy = officer;
    }

    public void reject() throws ValidationException {
        Check.transition("REFUND", status, "REJECTED");
        this.status = "REJECTED";
    }

    @Override
    public String getId() { return refundId; }

    @Override
    public String getDisplayName() { return "Refund " + refundId; }

    @Override
    public boolean isActive() { return !"REJECTED".equals(status); }

    @Override
    public void validate() throws ValidationException {
        Check.id("Refund ID", refundId);
        Check.notNull("Taxpayer", taxpayer).validate();
        Check.notNull("Assessment", assessment).validate();
        RegisteredTaxpayer owner = assessment.getDeclaration().getObligation().getTaxpayer();
        if (!owner.getTin().equals(taxpayer.getTin())) {
            throw new ValidationException("Assessment " + assessment.getId() + " does not belong to taxpayer " + taxpayer.getTin() + ".");
        }
        Check.amount("Requested amount", requestedAmount);
        if (requestedAmount.signum() <= 0) {
            throw new ValidationException("Requested refund must be greater than zero.");
        }
        Check.notFuture("Request date", requestDate);
        Check.choice("Refund status", status, "PENDING", "APPROVED", "REJECTED");
        if ("APPROVED".equals(status) && approvedBy == null) {
            throw new ValidationException("An approved refund must store the approving officer.");
        }
    }

    @Override
    public void printDetails() {
        Check.row("Refund ID", refundId);
        Check.row("Taxpayer", taxpayer.getDisplayName() + " (TIN " + taxpayer.getTin() + ")");
        Check.row("Assessment", assessment.getId());
        Check.row("Requested amount", requestedAmount);
        Check.row("Request date", requestDate);
        Check.row("Status", status);
        Check.row("Approved by", approvedBy == null ? "(none)" : approvedBy.getStaffNumber() + " " + approvedBy.getDisplayName());
    }

    @Override
    public String getSummary() {
        return refundId + " | TIN " + taxpayer.getTin() + " | " + assessment.getId() + " | " + requestedAmount + " | " + requestDate + " | " + status
                + (approvedBy == null ? "" : " | by " + approvedBy.getStaffNumber());
    }
}
