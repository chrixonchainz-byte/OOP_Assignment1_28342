package com.auca.main;

import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.ValidationException;
import java.time.LocalDate;

/**
 * Reusable four-step smart form used by all 11 concrete entities:
 * (1) identification / prerequisite, (2) entity details, (3) validation / calculations, (4) review / save.
 * BACK returns to the prior step, CANCEL discards unsaved work, any field can be edited by its number.
 */
public class Form {

    /** Validates/normalises one typed value. May look at the other already-entered values. */
    public interface Rule {
        String check(String input, String[] values) throws ValidationException;
    }

    /** Calculation / cross-field validation hook (may fill calculated fields). */
    public interface Hook {
        void run(String[] values) throws ValidationException;
    }

    /** Opens a prerequisite registration form. */
    public interface Action {
        void run();
    }

    /** One field of the form: step number, label, Java type + range hint and its rule. */
    public static final class Field {
        private final int step;
        private final String label;
        private final String hint;
        private final Rule rule;
        private final Action prereq;
        private final boolean auto;

        private Field(int step, String label, String hint, Rule rule, Action prereq, boolean auto) {
            this.step = step;
            this.label = label;
            this.hint = hint;
            this.rule = rule;
            this.prereq = prereq;
            this.auto = auto;
        }
    }

    public static Field field(int step, String label, String hint, Rule rule) {
        return new Field(step, label, hint, rule, null, false);
    }

    /** Reference field: if the record is missing the user may register the prerequisite now. */
    public static Field ref(int step, String label, String hint, Rule rule, Action prereq) {
        return new Field(step, label, hint, rule, prereq, false);
    }

    /** Calculated field: filled by the calculation hook, never typed. */
    public static Field auto(int step, String label, String hint) {
        return new Field(step, label, hint, null, null, true);
    }

    public static Field[] join(Field[]... parts) {
        int n = 0;
        for (Field[] p : parts) {
            n += p.length;
        }
        Field[] r = new Field[n];
        int k = 0;
        for (Field[] p : parts) {
            for (Field f : p) {
                r[k++] = f;
            }
        }
        return r;
    }

    // ---------- ready-made rules ----------
    public static Rule id(String label) { return (s, v) -> Check.id(label, s); }
    public static Rule text(String label, int min, int max) { return (s, v) -> Check.text(label, s, min, max); }
    public static Rule digits(String label, int n) { return (s, v) -> Check.digits(label, s, n); }
    public static Rule email() { return (s, v) -> Check.email(s); }
    public static Rule phone() { return (s, v) -> Check.phone(s); }
    public static Rule companyNo() { return (s, v) -> Check.companyNo(s); }
    public static Rule code() { return (s, v) -> Check.code(s); }
    public static Rule date(String label) { return (s, v) -> Check.date(label, s).toString(); }
    public static Rule money(String label) { return (s, v) -> Check.parseMoney(label, s).toPlainString(); }
    public static Rule signed(String label) { return (s, v) -> Check.parseSigned(label, s).toPlainString(); }
    public static Rule rate(String label) { return (s, v) -> Check.parseRate(label, s).toPlainString(); }
    public static Rule choice(String label, String... options) { return (s, v) -> Check.choice(label, s, options); }
    public static Rule yesNo(String label) { return (s, v) -> Check.yesNo(label, s); }

    public static Rule pastDate(String label) {
        return (s, v) -> {
            LocalDate d = Check.date(label, s);
            Check.notFuture(label, d);
            return d.toString();
        };
    }

    public static Rule unique(String kind, String label, Rule base) {
        return (s, v) -> {
            String x = base.check(s, v);
            if (Store.exists(kind, x)) {
                throw new DuplicateRecordException(label + " '" + x + "' already exists.");
            }
            return x;
        };
    }

    // ---------- the form itself ----------
    private final String title;
    private final String[] stepTitles;
    private final Field[] fields;
    private final String[] v;
    private final Hook calc;
    private final Hook save;

    public Form(String title, String[] stepTitles, Field[] fields, Hook calc, Hook save) {
        this.title = title;
        this.stepTitles = stepTitles;
        this.fields = fields;
        this.v = new String[fields.length];
        this.calc = calc;
        this.save = save;
    }

    /** Runs the form. Returns true only when the record was saved. */
    public boolean run() {
        int step = 1;
        while (true) {
            int r = step <= 3 ? doStep(step) : review();
            if (r == 0) {
                System.out.println("  Cancelled. Nothing was saved.");
                return false;
            }
            if (r == 2) {
                return true;
            }
            step = Math.max(1, step + r);
            if (step > 4) {
                step = 4;
            }
        }
    }

    private void banner(int step) {
        System.out.println();
        System.out.println("+---------------- " + title + " ----------------+");
        System.out.println("| STEP " + step + " OF 4  - " + stepTitles[step - 1]);
        System.out.println("+ (type BACK for the previous step, CANCEL to discard)");
    }

    /** Returns 1 = next, -1 = back, 0 = cancel. */
    private int doStep(int step) {
        banner(step);
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].step != step || fields[i].auto) {
                continue;
            }
            int r = prompt(i);
            if (r == 2) {
                return 0;
            }
            if (r == 1) {
                if (step > 1) {
                    return -1;
                }
                System.out.println("  Already at the first step.");
                i--;
            }
        }
        if (step == 3) {
            int r = calcLoop();
            if (r != 1) {
                return r;
            }
        }
        while (true) {
            String s = Input.line("NEXT / BACK / CANCEL : ").toUpperCase();
            if (s.isEmpty() || s.equals("NEXT")) {
                return 1;
            } else if (s.equals("BACK")) {
                if (step > 1) {
                    return -1;
                }
                System.out.println("  Already at the first step.");
            } else if (s.equals("CANCEL")) {
                return 0;
            } else {
                System.out.println("  Type NEXT, BACK or CANCEL.");
            }
        }
    }

    /** Runs the calculation hook; on error only the field chosen by the user is re-asked. */
    private int calcLoop() {
        while (true) {
            try {
                calc.run(v);
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].auto) {
                        printField(i);
                    }
                }
                return 1;
            } catch (ValidationException e) {
                System.out.println("  ! " + e.getMessage());
                String s = Input.line("Field number to correct (or BACK / CANCEL) : ");
                if (s.equalsIgnoreCase("BACK")) {
                    return -1;
                }
                if (s.equalsIgnoreCase("CANCEL")) {
                    return 0;
                }
                if (editByText(s)) {
                    return 0;
                }
            }
        }
    }

    /** Returns 2 = saved, 0 = cancel, -1 = back to step 3. */
    private int review() {
        banner(4);
        while (true) {
            for (int i = 0; i < fields.length; i++) {
                printField(i);
            }
            System.out.println("  [1] SAVE   [2] EDIT FIELD   [0] CANCEL   (or BACK)");
            String c = Input.line("Choice : ").toUpperCase();
            if (c.equals("1")) {
                try {
                    calc.run(v);
                    save.run(v);
                    System.out.println("  Saved successfully.");
                    return 2;
                } catch (ValidationException e) {
                    System.out.println("  ! Not saved: " + e.getMessage());
                }
            } else if (c.equals("2")) {
                if (editByText(Input.line("Field number to edit : "))) {
                    return 0;
                }
                try {
                    calc.run(v);
                } catch (ValidationException e) {
                    System.out.println("  ! " + e.getMessage());
                }
            } else if (c.equals("0") || c.equals("CANCEL")) {
                return 0;
            } else if (c.equals("BACK")) {
                return -1;
            } else {
                System.out.println("  Choose 1, 2 or 0.");
            }
        }
    }

    /** Re-asks one field by number. Returns true if the user typed CANCEL. */
    private boolean editByText(String s) {
        try {
            int n = Integer.parseInt(s.trim());
            if (n < 1 || n > fields.length) {
                System.out.println("  ! Field number must be 1-" + fields.length + ".");
            } else if (fields[n - 1].auto) {
                System.out.println("  ! Field " + n + " is calculated automatically.");
            } else {
                return prompt(n - 1) == 2;
            }
        } catch (NumberFormatException e) {
            System.out.println("  ! Please enter a field number.");
        }
        return false;
    }

    /** Asks one field until valid. Returns 0 = ok, 1 = BACK, 2 = CANCEL. */
    private int prompt(int i) {
        Field f = fields[i];
        while (true) {
            String cur = v[i] == null ? "" : v[i];
            String in = Input.line(String.format("%02d %-22s [%s]%s : ", i + 1, f.label, f.hint,
                    cur.isEmpty() ? "" : " (Enter keeps " + cur + ")"));
            if (in.equalsIgnoreCase("BACK")) {
                return 1;
            }
            if (in.equalsIgnoreCase("CANCEL")) {
                return 2;
            }
            if (in.isEmpty() && !cur.isEmpty()) {
                return 0;
            }
            try {
                v[i] = f.rule.check(in, v);
                return 0;
            } catch (RecordNotFoundException e) {
                System.out.println("  ! " + e.getMessage());
                if (f.prereq != null && offerPrerequisite(f)) {
                    return 2;
                }
            } catch (ValidationException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    /** Offers to register the missing prerequisite. Returns true if the user chose to cancel the form. */
    private boolean offerPrerequisite(Field f) {
        System.out.println("    [1] Register prerequisite now   [2] Re-enter value   [0] Cancel this form");
        String c = Input.line("    Choice : ");
        if (c.equals("1")) {
            f.prereq.run();
            System.out.println("    Back to the original form - your entered values are kept.");
        }
        return c.equals("0");
    }

    private void printField(int i) {
        Field f = fields[i];
        String shown = v[i] != null ? v[i] : (f.auto ? "(calculated)" : "(not set)");
        System.out.printf("  %02d %-22s [%s] : %s%n", i + 1, f.label, f.hint, shown);
    }
}
