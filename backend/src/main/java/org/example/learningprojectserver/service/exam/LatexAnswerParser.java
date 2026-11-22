package org.example.learningprojectserver.service.exam;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads answers typed in the MathLive formula field (LaTeX) or as plain text.
 * Numbers: "3.5", "3,5", "-7/2", "\frac{7}{2}", "-\frac{1}{2}", "3{,}5".
 */
public final class LatexAnswerParser {

    private static final Pattern FRACTION =
            Pattern.compile("^(-?)\\\\[dt]?frac\\{([^{}]+)}\\{([^{}]+)}$");

    private LatexAnswerParser() {}

    /** Strips LaTeX spacing and wrappers that do not change the value. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim()
                .replace("\\left", "")
                .replace("\\right", "")
                .replace("\\dfrac", "\\frac")
                .replace("\\tfrac", "\\frac")
                .replace("\\,", "")
                .replace("\\!", "")
                .replace("\\ ", "")
                .replace("\\cdot", "*")
                .replace("\\times", "*")
                .replace("$", "")
                .replace("−", "-")
                .replace("{,}", ".")
                .replaceAll("[‎‏]", "")
                .replaceAll("\\s+", "");
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.toLowerCase(Locale.ROOT);
    }

    /** The numeric value of the answer, or null when it is not a number or a simple fraction. */
    public static Double parseNumber(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) {
            return null;
        }
        Matcher m = FRACTION.matcher(s);
        if (m.matches()) {
            Double num = plain(m.group(2));
            Double den = plain(m.group(3));
            if (num == null || den == null || den == 0) {
                return null;
            }
            double value = num / den;
            return m.group(1).isEmpty() ? value : -value;
        }
        int slash = s.indexOf('/');
        if (slash > 0 && slash == s.lastIndexOf('/')) {
            Double num = plain(s.substring(0, slash));
            Double den = plain(s.substring(slash + 1));
            if (num == null || den == null || den == 0) {
                return null;
            }
            return num / den;
        }
        return plain(s);
    }

    private static Double plain(String s) {
        String t = s.replace("{", "").replace("}", "").replace(',', '.');
        if (!t.matches("[-+]?(\\d+\\.?\\d*|\\.\\d+)([e][-+]?\\d+)?")) {
            return null;
        }
        double v = Double.parseDouble(t);
        return Double.isFinite(v) ? v : null;
    }
}
