package com.auca.taxpayment;

import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import com.auca.taxassessment.TaxAssessment;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete class: a (possibly partial) payment against a CONFIRMED assessment. */
public class TaxPayment implements Identifiable, Validatable {
    private String paymentId;
    private TaxAssessment assessment;
    private String paymentReference;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;

    public TaxPayment(String paymentId, TaxAssessment assessment, String paymentReference,
            BigDecimal amount, LocalDate paymentDate, String paymentMethod) throws ValidationException {
        setPaymentId(paymentId);
        setAssessment(assessment);
        setPaymentReference(paymentReference);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
    }

    public String getPaymentId() { return paymentId; }
    public TaxAssessment getAssessment() { return assessment; }
    public String getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }

    public final void setPaymentId(String v) throws ValidationException { this.paymentId = Check.id("Payment ID", v); }
    public final void setAssessment(TaxAssessment v) throws ValidationException { this.assessment = Check.notNull("Assessment", v); }
    public final void setPaymentReference(String v) throws ValidationException { this.paymentReference = Check.id("Payment reference", v); }
    public final void setAmount(BigDecimal v) throws ValidationException { this.amount = Check.amount("Payment amount", v); }

    public final void setPaymentDate(LocalDate v) throws ValidationException {
        Check.notFuture("Payment date", v);
        this.paymentDate = v;
    }

    public final void setPaymentMethod(String v) throws ValidationException {
        this.paymentMethod = Check.choice("Payment method", v, "CASH", "BANK_TRANSFER", "MOBILE_MONEY", "CARD");
    }

    @Override
    public String getId() { return paymentId; }

    @Override
    public String getDisplayName() { return "Payment " + paymentReference; }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Check.id("Payment ID", paymentId);
        Check.notNull("Assessment", assessment).validate();
        if (!"CONFIRMED".equals(assessment.getStatus())) {
            throw new PrerequisiteMissingException("Payments require a CONFIRMED assessment (current: " + assessment.getStatus() + ").");
        }
        Check.id("Payment reference", paymentReference);
        Check.amount("Payment amount", amount);
        if (amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        Check.notFuture("Payment date", paymentDate);
        if (paymentDate.isBefore(assessment.getAssessmentDate())) {
            throw new ValidationException("Payment date cannot be before the assessment date " + assessment.getAssessmentDate() + ".");
        }
        Check.choice("Payment method", paymentMethod, "CASH", "BANK_TRANSFER", "MOBILE_MONEY", "CARD");
    }

    @Override
    public void printDetails() {
        Check.row("Payment ID", paymentId);
        Check.row("Assessment", assessment.getId());
        Check.row("Payment reference", paymentReference);
        Check.row("Amount", amount);
        Check.row("Payment date", paymentDate);
        Check.row("Payment method", paymentMethod);
    }

    @Override
    public String getSummary() {
        return paymentId + " | " + assessment.getId() + " | ref " + paymentReference + " | " + amount + " | " + paymentDate + " | " + paymentMethod;
    }
}
