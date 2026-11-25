package com.auca.main;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.abstracts.TaxParty;
import com.auca.corporatetaxpayer.CorporateTaxpayer;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.OverpaymentException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.ValidationException;
import com.auca.individualtaxpayer.IndividualTaxpayer;
import com.auca.interfaces.Identifiable;
import com.auca.penalty.Penalty;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxclearance.TaxClearance;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxobligation.TaxObligation;
import com.auca.taxpayment.TaxPayment;
import com.auca.taxrefund.TaxRefund;
import com.auca.taxtype.TaxType;
import java.time.LocalDate;

/** Fixed-size typed arrays + integer counters. No List, Map, Set or Stream anywhere. */
public final class Store {
    static final IndividualTaxpayer[] individuals = new IndividualTaxpayer[100];
    static final CorporateTaxpayer[] corporates = new CorporateTaxpayer[100];
    static final RevenueOfficer[] officers = new RevenueOfficer[50];
    static final TaxType[] taxTypes = new TaxType[50];
    static final TaxObligation[] obligations = new TaxObligation[200];
    static final TaxDeclaration[] declarations = new TaxDeclaration[200];
    static final TaxAssessment[] assessments = new TaxAssessment[200];
    static final TaxPayment[] payments = new TaxPayment[300];
    static final Penalty[] penalties = new Penalty[200];
    static final TaxRefund[] refunds = new TaxRefund[100];
    static final TaxClearance[] clearances = new TaxClearance[100];

    static int nInd;
    static int nCorp;
    static int nOff;
    static int nType;
    static int nObl;
    static int nDecl;
    static int nAss;
    static int nPay;
    static int nPen;
    static int nRef;
    static int nClr;

    private Store() {
    }

    // ---------- helpers ----------
    private static void capacity(int count, int max, String what) throws ValidationException {
        if (count >= max) {
            throw new ValidationException("Storage is full: no more " + what + " can be saved (capacity " + max + "). Existing data is untouched.");
        }
    }

    private static void unique(String kind, String value, String label) throws DuplicateRecordException {
        if (exists(kind, value)) {
            throw new DuplicateRecordException(label + " '" + value + "' already exists.");
        }
    }

    private static boolean has(Identifiable[] arr, int n, String id) {
        for (int i = 0; i < n; i++) {
            if (arr[i].getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Identifiable> T byId(T[] arr, int n, String id, String what) throws RecordNotFoundException {
        String key = id == null ? "" : id.trim();
        for (int i = 0; i < n; i++) {
            if (arr[i].getId().equalsIgnoreCase(key)) {
                return arr[i];
            }
        }
        throw new RecordNotFoundException(what + " '" + key + "' was not found.");
    }

    /** Uniqueness test used by forms and by the add() methods. */
    public static boolean exists(String kind, String x) {
        switch (kind) {
            case "PARTY":
                return has(individuals, nInd, x) || has(corporates, nCorp, x) || has(officers, nOff, x);
            case "TIN":
                for (int i = 0; i < nInd; i++) {
                    if (individuals[i].getTin().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                for (int i = 0; i < nCorp; i++) {
                    if (corporates[i].getTin().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "NATIONAL":
                for (int i = 0; i < nInd; i++) {
                    if (individuals[i].getNationalId().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "COMPANY":
                for (int i = 0; i < nCorp; i++) {
                    if (corporates[i].getCompanyRegistrationNo().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "STAFF":
                for (int i = 0; i < nOff; i++) {
                    if (officers[i].getStaffNumber().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "TAXTYPE":
                return has(taxTypes, nType, x);
            case "TAXCODE":
                for (int i = 0; i < nType; i++) {
                    if (taxTypes[i].getTaxCode().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "OBLIGATION":
                return has(obligations, nObl, x);
            case "DECLARATION":
                return has(declarations, nDecl, x);
            case "ASSESSMENT":
                return has(assessments, nAss, x);
            case "PAYMENT":
                return has(payments, nPay, x);
            case "REF":
                for (int i = 0; i < nPay; i++) {
                    if (payments[i].getPaymentReference().equalsIgnoreCase(x)) {
                        return true;
                    }
                }
                return false;
            case "PENALTY":
                return has(penalties, nPen, x);
            case "REFUND":
                return has(refunds, nRef, x);
            case "CLEARANCE":
                return has(clearances, nClr, x);
            default:
                return false;
        }
    }

    // ---------- finders ----------
    public static RegisteredTaxpayer taxpayer(String key) throws RecordNotFoundException {
        String k = key == null ? "" : key.trim();
        for (int i = 0; i < nInd; i++) {
            if (individuals[i].getTin().equalsIgnoreCase(k) || individuals[i].getId().equalsIgnoreCase(k)) {
                return individuals[i];
            }
        }
        for (int i = 0; i < nCorp; i++) {
            if (corporates[i].getTin().equalsIgnoreCase(k) || corporates[i].getId().equalsIgnoreCase(k)) {
                return corporates[i];
            }
        }
        throw new RecordNotFoundException("No registered taxpayer with TIN / Party ID '" + k + "' was found.");
    }

    public static RevenueOfficer officer(String key) throws RecordNotFoundException {
        String k = key == null ? "" : key.trim();
        for (int i = 0; i < nOff; i++) {
            if (officers[i].getStaffNumber().equalsIgnoreCase(k) || officers[i].getId().equalsIgnoreCase(k)) {
                return officers[i];
            }
        }
        throw new RecordNotFoundException("No revenue officer with staff number / Party ID '" + k + "' was found.");
    }

    public static TaxType taxType(String key) throws RecordNotFoundException {
        String k = key == null ? "" : key.trim();
        for (int i = 0; i < nType; i++) {
            if (taxTypes[i].getId().equalsIgnoreCase(k) || taxTypes[i].getTaxCode().equalsIgnoreCase(k)) {
                return taxTypes[i];
            }
        }
        throw new RecordNotFoundException("No tax type with ID / code '" + k + "' was found.");
    }

    public static TaxObligation obligation(String id) throws RecordNotFoundException { return byId(obligations, nObl, id, "Tax obligation"); }
    public static TaxDeclaration declaration(String id) throws RecordNotFoundException { return byId(declarations, nDecl, id, "Tax declaration"); }
    public static TaxAssessment assessment(String id) throws RecordNotFoundException { return byId(assessments, nAss, id, "Tax assessment"); }
    public static TaxRefund refund(String id) throws RecordNotFoundException { return byId(refunds, nRef, id, "Tax refund"); }
    public static TaxClearance clearance(String id) throws RecordNotFoundException { return byId(clearances, nClr, id, "Tax clearance"); }

    // ---------- relationship helpers ----------
    /** The one non-rejected declaration of an obligation, or null. */
    public static TaxDeclaration activeDeclarationFor(TaxObligation o) {
        for (int i = 0; i < nDecl; i++) {
            if (declarations[i].getObligation() == o && declarations[i].isActive()) {
                return declarations[i];
            }
        }
        return null;
    }

    /** The one non-cancelled assessment of a declaration, or null. */
    public static TaxAssessment liveAssessmentFor(TaxDeclaration d) {
        for (int i = 0; i < nAss; i++) {
            if (assessments[i].getDeclaration() == d && assessments[i].isActive()) {
                return assessments[i];
            }
        }
        return null;
    }

    public static boolean penaltyExists(TaxObligation o, String type) {
        for (int i = 0; i < nPen; i++) {
            if (penalties[i].getObligation() == o && penalties[i].getPenaltyType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean pendingRefundFor(TaxAssessment a) {
        for (int i = 0; i < nRef; i++) {
            if (refunds[i].getAssessment() == a && "PENDING".equals(refunds[i].getStatus())) {
                return true;
            }
        }
        return false;
    }

    private static void checkOverlap(TaxObligation n) throws ValidationException {
        for (int i = 0; i < nObl; i++) {
            TaxObligation o = obligations[i];
            if (Calc.sameTaxpayer(o.getTaxpayer(), n.getTaxpayer()) && o.getTaxType() == n.getTaxType()
                    && !n.getPeriodStart().isAfter(o.getPeriodEnd()) && !o.getPeriodStart().isAfter(n.getPeriodEnd())) {
                throw new DuplicateRecordException("Period overlaps existing obligation " + o.getId() + " (" + o.getPeriodStart() + ".." + o.getPeriodEnd()
                        + ") for the same taxpayer and tax type.");
            }
        }
    }

    public static void checkObligation(TaxObligation o) throws ValidationException {
        o.validate();
        checkOverlap(o);
    }

    // ---------- add (revalidate everything related, then store) ----------
    public static void add(IndividualTaxpayer t) throws ValidationException {
        t.validate();
        capacity(nInd, individuals.length, "individual taxpayers");
        unique("PARTY", t.getId(), "Party ID");
        unique("TIN", t.getTin(), "TIN");
        unique("NATIONAL", t.getNationalId(), "National ID");
        individuals[nInd++] = t;
    }

    public static void add(CorporateTaxpayer t) throws ValidationException {
        t.validate();
        capacity(nCorp, corporates.length, "corporate taxpayers");
        unique("PARTY", t.getId(), "Party ID");
        unique("TIN", t.getTin(), "TIN");
        unique("COMPANY", t.getCompanyRegistrationNo(), "Company registration number");
        corporates[nCorp++] = t;
    }

    public static void add(RevenueOfficer o) throws ValidationException {
        o.validate();
        capacity(nOff, officers.length, "revenue officers");
        unique("PARTY", o.getId(), "Party ID");
        unique("STAFF", o.getStaffNumber(), "Staff number");
        officers[nOff++] = o;
    }

    public static void add(TaxType t) throws ValidationException {
        t.validate();
        capacity(nType, taxTypes.length, "tax types");
        unique("TAXTYPE", t.getId(), "Tax type ID");
        unique("TAXCODE", t.getTaxCode(), "Tax code");
        taxTypes[nType++] = t;
    }

    public static void add(TaxObligation o) throws ValidationException {
        checkObligation(o);
        capacity(nObl, obligations.length, "tax obligations");
        unique("OBLIGATION", o.getId(), "Obligation ID");
        obligations[nObl++] = o;
    }

    public static void add(TaxDeclaration d) throws ValidationException {
        d.validate();
        capacity(nDecl, declarations.length, "tax declarations");
        unique("DECLARATION", d.getId(), "Declaration ID");
        if (activeDeclarationFor(d.getObligation()) != null) {
            throw new DuplicateRecordException("Obligation " + d.getObligation().getId() + " already has an active declaration.");
        }
        declarations[nDecl++] = d;
    }

    public static void add(TaxAssessment a) throws ValidationException {
        a.validate();
        capacity(nAss, assessments.length, "tax assessments");
        unique("ASSESSMENT", a.getId(), "Assessment ID");
        if (liveAssessmentFor(a.getDeclaration()) != null) {
            throw new DuplicateRecordException("Declaration " + a.getDeclaration().getId() + " already has a live assessment.");
        }
        assessments[nAss++] = a;
    }

    public static void add(TaxPayment p) throws ValidationException {
        p.validate();
        capacity(nPay, payments.length, "tax payments");
        unique("PAYMENT", p.getId(), "Payment ID");
        unique("REF", p.getPaymentReference(), "Payment reference");
        if (p.getAmount().compareTo(Calc.outstanding(p.getAssessment())) > 0) {
            throw new OverpaymentException("Payment " + p.getAmount().toPlainString() + " exceeds the outstanding balance "
                    + Calc.outstanding(p.getAssessment()).toPlainString() + ".");
        }
        payments[nPay++] = p;
    }

    public static void add(Penalty p) throws ValidationException {
        p.validate();
        capacity(nPen, penalties.length, "penalties");
        unique("PENALTY", p.getId(), "Penalty ID");
        TaxDeclaration d = activeDeclarationFor(p.getObligation());
        if (d == null || !d.isLate()) {
            throw new PrerequisiteMissingException("A late-filing penalty needs a LATE declaration for obligation " + p.getObligation().getId() + ".");
        }
        if (penaltyExists(p.getObligation(), p.getPenaltyType())) {
            throw new DuplicateRecordException("Penalty " + p.getPenaltyType() + " already exists for obligation " + p.getObligation().getId() + ".");
        }
        penalties[nPen++] = p;
    }

    public static void add(TaxRefund r) throws ValidationException {
        r.validate();
        capacity(nRef, refunds.length, "tax refunds");
        unique("REFUND", r.getId(), "Refund ID");
        if (pendingRefundFor(r.getAssessment())) {
            throw new DuplicateRecordException("Assessment " + r.getAssessment().getId() + " already has a pending refund.");
        }
        if (r.getRequestedAmount().compareTo(Calc.overpayment(r.getAssessment())) > 0) {
            throw new OverpaymentException("Requested refund exceeds the verified overpayment " + Calc.overpayment(r.getAssessment()).toPlainString() + ".");
        }
        refunds[nRef++] = r;
    }

    public static void add(TaxClearance c) throws ValidationException {
        c.validate();
        capacity(nClr, clearances.length, "tax clearances");
        unique("CLEARANCE", c.getId(), "Clearance ID");
        clearances[nClr++] = c;
    }

    // ---------- arrays for the polymorphism demo ----------
    public static TaxParty[] allParties() {
        TaxParty[] r = new TaxParty[nInd + nCorp + nOff];
        int k = 0;
        for (int i = 0; i < nInd; i++) {
            r[k++] = individuals[i];
        }
        for (int i = 0; i < nCorp; i++) {
            r[k++] = corporates[i];
        }
        for (int i = 0; i < nOff; i++) {
            r[k++] = officers[i];
        }
        return r;
    }

    public static Identifiable[] allIdentifiables() {
        Identifiable[] r = new Identifiable[nInd + nCorp + nOff + nType + nObl + nDecl + nAss + nPay + nPen + nRef + nClr];
        int k = 0;
        for (int i = 0; i < nInd; i++) { r[k++] = individuals[i]; }
        for (int i = 0; i < nCorp; i++) { r[k++] = corporates[i]; }
        for (int i = 0; i < nOff; i++) { r[k++] = officers[i]; }
        for (int i = 0; i < nType; i++) { r[k++] = taxTypes[i]; }
        for (int i = 0; i < nObl; i++) { r[k++] = obligations[i]; }
        for (int i = 0; i < nDecl; i++) { r[k++] = declarations[i]; }
        for (int i = 0; i < nAss; i++) { r[k++] = assessments[i]; }
        for (int i = 0; i < nPay; i++) { r[k++] = payments[i]; }
        for (int i = 0; i < nPen; i++) { r[k++] = penalties[i]; }
        for (int i = 0; i < nRef; i++) { r[k++] = refunds[i]; }
        for (int i = 0; i < nClr; i++) { r[k++] = clearances[i]; }
        return r;
    }
}
