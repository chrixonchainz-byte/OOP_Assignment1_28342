package com.auca.main;

import java.util.NoSuchElementException;


public class Main {
    private static final String[] LEFT = {
        "Register Individual Taxpayer", "Register Corporate Taxpayer", "Register Revenue Officer", "Register Tax Type",
        "Create Tax Obligation", "File Tax Declaration", "Assess Declaration", "Confirm Assessment",
        "View All Records", "View Declaration History", "View Assessment History"
    };
    private static final String[] RIGHT = {
        "Record Tax Payment", "Calculate / Record Penalty", "Request / Approve Refund", "Request / Issue Tax Clearance",
        "Search Record by ID / TIN", "Update Eligible Record", "View Taxpayer Statement", "View Outstanding Balances",
        "View Tax Reports", "Demonstrate Polymorphism"
    };

    private static void printMenu() {
        System.out.println();
        System.out.println("================ TAX ADMINISTRATION MANAGEMENT ================");
        for (int i = 0; i < LEFT.length; i++) {
            String right = i < RIGHT.length ? String.format("%2d %s", i + 12, RIGHT[i]) : " 0 Exit";
            System.out.printf("%2d %-32s %s%n", i + 1, LEFT[i], right);
        }
    }

    private static void dispatch(int option) {
        switch (option) {
            case 1: TaxpayerForms.registerIndividual(); break;
            case 2: TaxpayerForms.registerCorporate(); break;
            case 3: TaxpayerForms.registerOfficer(); break;
            case 4: TaxForms.registerTaxType(); break;
            case 5: TaxForms.createObligation(); break;
            case 6: TaxForms.fileDeclaration(); break;
            case 7: TaxForms.assessDeclaration(); break;
            case 8: TaxForms.confirmAssessment(); break;
            case 9: Reports.viewAll(); break;
            case 10: Reports.declarationHistory(); break;
            case 11: Reports.assessmentHistory(); break;
            case 12: MoneyForms.recordPayment(); break;
            case 13: MoneyForms.recordPenalty(); break;
            case 14: MoneyForms.refundMenu(); break;
            case 15: MoneyForms.clearanceMenu(); break;
            case 16: Reports.search(); break;
            case 17: Updates.menu(); break;
            case 18: Reports.statement(); break;
            case 19: Reports.outstanding(); break;
            case 20: Reports.taxReports(); break;
            case 21: Reports.polymorphism(); break;
            default: System.out.println("  ! Unknown option " + option + ". Please choose 0-21.");
        }
    }

    public static void main(String[] args) {
        System.out.println("TAMS - Java/OOP practical assignment | Shyaka Christian | ID 28342");
        System.out.println("(Teaching simulation: rates and rules are illustrative, not official tax law.)");
        try {
            while (true) {
                printMenu();
                String raw = Input.line("Select option: ");
                int option;
                try {
                    option = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    System.out.println("  ! '" + raw + "' is not a number. Please enter 0-21.");
                    continue;
                }
                if (option == 0) {
                    System.out.println("Goodbye.");
                    return;
                }
                try {
                    dispatch(option);
                } catch (NoSuchElementException e) {
                    throw e;
                } catch (RuntimeException e) {
                    System.out.println("  ! The operation could not be completed (" + e.getClass().getSimpleName()
                            + "). Nothing was corrupted - returning to the menu.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Input closed. Exiting.");
        }
    }
}
