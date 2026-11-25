package com.auca.main;

import java.util.Scanner;

/** The single shared Scanner of the whole application (always nextLine, never nextInt). */
public final class Input {
    private static final Scanner SCANNER = new Scanner(System.in);

    private Input() {
    }

    /** Prints the prompt and returns the trimmed line. Throws NoSuchElementException at end of input. */
    public static String line(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    public static boolean yes(String prompt) {
        return line(prompt + " (YES/NO) : ").equalsIgnoreCase("YES");
    }
}
