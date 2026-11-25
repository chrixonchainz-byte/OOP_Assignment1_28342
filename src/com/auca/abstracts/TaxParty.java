package com.auca.abstracts;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.main.Check;
import java.time.LocalDate;

/** Level 1 abstract class: identity and contact data shared by every person in TAMS. */
public abstract class TaxParty implements Identifiable, Validatable {
    private String partyId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    protected TaxParty(String partyId, String firstName, String lastName, String email,
            String phoneNumber, LocalDate dateOfBirth) throws ValidationException {
        setPartyId(partyId);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        setDateOfBirth(dateOfBirth);
    }

    public String getPartyId() { return partyId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public final void setPartyId(String v) throws ValidationException { this.partyId = Check.id("Party ID", v); }
    public final void setFirstName(String v) throws ValidationException { this.firstName = Check.text("First name", v, 2, 60); }
    public final void setLastName(String v) throws ValidationException { this.lastName = Check.text("Last name", v, 2, 60); }
    public final void setEmail(String v) throws ValidationException { this.email = Check.email(v); }
    public final void setPhoneNumber(String v) throws ValidationException { this.phoneNumber = Check.phone(v); }

    public final void setDateOfBirth(LocalDate v) throws ValidationException {
        Check.notFuture("Date of birth", v);
        this.dateOfBirth = v;
    }

    /** Role-specific label implemented by the concrete subclasses. */
    public abstract String getPartyCategory();

    @Override
    public String getId() { return partyId; }

    @Override
    public String getDisplayName() { return firstName + " " + lastName; }

    @Override
    public void validate() throws ValidationException {
        Check.id("Party ID", partyId);
        Check.text("First name", firstName, 2, 60);
        Check.text("Last name", lastName, 2, 60);
        Check.email(email);
        Check.phone(phoneNumber);
        Check.notFuture("Date of birth", dateOfBirth);
    }

    @Override
    public void printDetails() {
        Check.row("Category", getPartyCategory());
        Check.row("Party ID", partyId);
        Check.row("First name", firstName);
        Check.row("Last name", lastName);
        Check.row("Email", email);
        Check.row("Phone", phoneNumber);
        Check.row("Date of birth", dateOfBirth);
    }

    @Override
    public String getSummary() { return partyId + " | " + getDisplayName() + " | " + getPartyCategory(); }
}
