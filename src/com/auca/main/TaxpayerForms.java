package com.auca.main;

import com.auca.corporatetaxpayer.CorporateTaxpayer;
import com.auca.exceptions.ValidationException;
import com.auca.individualtaxpayer.IndividualTaxpayer;
import com.auca.revenueofficer.RevenueOfficer;

/** Menu options 1-3: registration forms for individuals, companies and revenue officers. */
public final class TaxpayerForms {
    private TaxpayerForms() {
    }

    /** The six TaxParty fields (step 1). */
    private static Form.Field[] partyFields() {
        return new Form.Field[] {
            Form.field(1, "Party ID", "String, unique, 4-30", Form.unique("PARTY", "Party ID", Form.id("Party ID"))),
            Form.field(1, "First name", "String, 2-60", Form.text("First name", 2, 60)),
            Form.field(1, "Last name", "String, 2-60", Form.text("Last name", 2, 60)),
            Form.field(1, "Email", "valid email, <=120", Form.email()),
            Form.field(1, "Phone", "10-15 digits, optional +", Form.phone()),
            Form.field(1, "Date of birth", "LocalDate YYYY-MM-DD", Form.pastDate("Date of birth"))
        };
    }

    /** The six RegisteredTaxpayer fields (step 2). */
    private static Form.Field[] registrationFields() {
        return new Form.Field[] {
            Form.field(2, "TIN", "String, 9 digits, unique", Form.unique("TIN", "TIN", Form.digits("TIN", 9))),
            Form.field(2, "Registration date", "LocalDate YYYY-MM-DD", Form.pastDate("Registration date")),
            Form.field(2, "Taxpayer status", "ACTIVE/INACTIVE/SUSPENDED", Form.choice("Taxpayer status", "ACTIVE", "INACTIVE", "SUSPENDED")),
            Form.field(2, "District", "String, 2-60", Form.text("District", 2, 60)),
            Form.field(2, "Sector", "String, 2-60", Form.text("Sector", 2, 60)),
            Form.field(2, "Tax office", "String, 2-60", Form.text("Tax office", 2, 60))
        };
    }

    // ---------- 1. Individual taxpayer ----------
    private static IndividualTaxpayer buildIndividual(String[] v) throws ValidationException {
        return new IndividualTaxpayer(v[0], v[1], v[2], v[3], v[4], Check.date("Date of birth", v[5]),
                v[6], Check.date("Registration date", v[7]), v[8], v[9], v[10], v[11],
                v[12], v[13], v[14], Check.date("Business start date", v[15]),
                Check.parseMoney("Annual turnover", v[16]), "YES".equals(v[17]));
    }

    public static void registerIndividual() {
        Form.Field[] fields = Form.join(partyFields(), registrationFields(), new Form.Field[] {
            Form.field(3, "National ID", "String, 16 digits, unique", Form.unique("NATIONAL", "National ID", Form.digits("National ID", 16))),
            Form.field(3, "Occupation", "String, 2-60", Form.text("Occupation", 2, 60)),
            Form.field(3, "Business name", "String, 2-120", Form.text("Business name", 2, 120)),
            Form.field(3, "Business start date", "LocalDate YYYY-MM-DD", Form.pastDate("Business start date")),
            Form.field(3, "Annual turnover", "BigDecimal >=0, 2 dp", Form.money("Annual turnover")),
            Form.field(3, "Resident", "boolean YES/NO", Form.yesNo("Resident"))
        });
        String[] steps = {"PERSONAL DETAILS", "TIN, registration date, status, district, sector, office",
            "National ID, occupation, business name, start date, annual turnover, resident", "REVIEW ALL 18 FIELDS"};
        new Form("REGISTER INDIVIDUAL TAXPAYER", steps, fields,
                v -> buildIndividual(v).validate(), v -> Store.add(buildIndividual(v))).run();
    }

    // ---------- 2. Corporate taxpayer ----------
    private static CorporateTaxpayer buildCorporate(String[] v) throws ValidationException {
        return new CorporateTaxpayer(v[0], v[1], v[2], v[3], v[4], Check.date("Date of birth", v[5]),
                v[6], Check.date("Registration date", v[7]), v[8], v[9], v[10], v[11],
                v[12], v[13], Check.date("Incorporation date", v[14]), v[15], v[16],
                Check.parseMoney("Annual turnover", v[17]));
    }

    public static void registerCorporate() {
        Form.Field[] fields = Form.join(partyFields(), registrationFields(), new Form.Field[] {
            Form.field(3, "Company reg. number", "String, 5-30 A-Z 0-9 -, unique", Form.unique("COMPANY", "Company registration number", Form.companyNo())),
            Form.field(3, "Legal name", "String, 3-120", Form.text("Legal name", 3, 120)),
            Form.field(3, "Incorporation date", "LocalDate YYYY-MM-DD", Form.pastDate("Incorporation date")),
            Form.field(3, "Business sector", "String, 2-60", Form.text("Business sector", 2, 60)),
            Form.field(3, "Authorized rep.", "String, 2-60", Form.text("Authorized representative", 2, 60)),
            Form.field(3, "Annual turnover", "BigDecimal >=0, 2 dp", Form.money("Annual turnover"))
        });
        String[] steps = {"CONTACT PERSON DETAILS", "TIN, registration date, status, district, sector, office",
            "Company number, legal name, incorporation date, sector, representative, turnover", "REVIEW ALL 18 FIELDS"};
        new Form("REGISTER CORPORATE TAXPAYER", steps, fields,
                v -> buildCorporate(v).validate(), v -> Store.add(buildCorporate(v))).run();
    }

    // ---------- 3. Revenue officer ----------
    private static RevenueOfficer buildOfficer(String[] v) throws ValidationException {
        return new RevenueOfficer(v[0], v[1], v[2], v[3], v[4], Check.date("Date of birth", v[5]),
                v[6], v[7], v[8], Check.date("Hire date", v[9]), Check.parseMoney("Approval limit", v[10]), "YES".equals(v[11]));
    }

    public static void registerOfficer() {
        Form.Field[] fields = Form.join(partyFields(), new Form.Field[] {
            Form.field(2, "Staff number", "String, unique, 4-30", Form.unique("STAFF", "Staff number", Form.id("Staff number"))),
            Form.field(2, "Department", "String, 2-60", Form.text("Department", 2, 60)),
            Form.field(2, "Role", "String, 2-60", Form.text("Role", 2, 60)),
            Form.field(3, "Hire date", "LocalDate YYYY-MM-DD", Form.pastDate("Hire date")),
            Form.field(3, "Approval limit", "BigDecimal >=0, 2 dp", Form.money("Approval limit")),
            Form.field(3, "Active", "boolean YES/NO", Form.yesNo("Active"))
        });
        String[] steps = {"PERSONAL DETAILS", "STAFF NUMBER, DEPARTMENT, ROLE",
            "Hire date, approval limit, active", "REVIEW ALL 12 FIELDS"};
        new Form("REGISTER REVENUE OFFICER", steps, fields,
                v -> buildOfficer(v).validate(), v -> Store.add(buildOfficer(v))).run();
    }

    /** Prerequisite helper used by forms that need a taxpayer. */
    public static void registerAny() {
        System.out.println("    [1] Individual taxpayer   [2] Corporate taxpayer   [0] Cancel");
        String c = Input.line("    Choice : ");
        if (c.equals("1")) {
            registerIndividual();
        } else if (c.equals("2")) {
            registerCorporate();
        } else {
            System.out.println("    Prerequisite registration cancelled.");
        }
    }
}
