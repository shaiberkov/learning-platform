package org.example.learningprojectserver.service.diagnosis;

import org.example.learningprojectserver.enums.MistakeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based first guess at the type of a mistake, from the expected and the given answer alone.
 * It tags the wrong answer as a distractor of a known shape: a sign error, a small slip, swapped
 * digits or a shifted decimal point is a calculation error; a number copied from the question or the
 * right values in the wrong order or format is a misread question; any other wrong number is a
 * conceptual mistake. Non-numeric answers stay UNCLASSIFIED for the AI analysis.
 */
public final class MistakeClassifier {

    public record Diagnosis(MistakeType type, String reason) {}

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:/\\d+(?:\\.\\d+)?)?");
    private static final double EPSILON = 1e-9;

    private MistakeClassifier() {}

    public static Diagnosis classify(String questionText, String expected, String given) {
        if (given == null || given.isBlank()) {
            return new Diagnosis(MistakeType.UNCLASSIFIED, "לא נכתבה תשובה");
        }
        String e = expected == null ? "" : expected.trim();
        String g = given.trim();
        if (e.equalsIgnoreCase(g)) {
            // Answered correctly, but only after opening the full solution.
            return new Diagnosis(MistakeType.CONCEPTUAL, "השאלה נפתרה רק בעזרת הפתרון המלא");
        }
        if (compact(e).equalsIgnoreCase(compact(g))) {
            return new Diagnosis(MistakeType.MISREAD, "התשובה נכונה אבל לא נכתבה בצורה שהשאלה ביקשה");
        }

        List<Double> expectedNumbers = numbers(e);
        List<Double> givenNumbers = numbers(g);
        if (expectedNumbers.isEmpty() || givenNumbers.isEmpty()) {
            return new Diagnosis(MistakeType.UNCLASSIFIED, "תשובה לא מספרית – אפשר לבקש ניתוח AI");
        }
        if (expectedNumbers.size() != givenNumbers.size()) {
            return new Diagnosis(MistakeType.MISREAD, "מספר הערכים בתשובה שונה ממה שהשאלה ביקשה");
        }
        if (sameValues(expectedNumbers, givenNumbers)) {
            return new Diagnosis(MistakeType.MISREAD,
                    expectedNumbers.equals(givenNumbers) || sameInOrder(expectedNumbers, givenNumbers)
                            ? "הערכים נכונים אבל לא נכתבו בצורה שהשאלה ביקשה"
                            : "הערכים נכונים אבל בסדר הפוך");
        }

        // Compare value by value; the answer is as wrong as its worst value.
        List<Double> questionNumbers = numbers(questionText == null ? "" : questionText);
        Diagnosis worst = null;
        for (int i = 0; i < expectedNumbers.size(); i++) {
            double exp = expectedNumbers.get(i);
            double giv = givenNumbers.get(i);
            if (equal(exp, giv)) {
                continue;
            }
            Diagnosis d = classifyNumber(exp, giv, questionNumbers);
            if (worst == null || rank(d.type()) > rank(worst.type())) {
                worst = d;
            }
        }
        return worst == null
                ? new Diagnosis(MistakeType.MISREAD, "הערכים נכונים אבל לא נכתבו בצורה שהשאלה ביקשה")
                : worst;
    }

    static Diagnosis classifyNumber(double expected, double given, List<Double> questionNumbers) {
        if (!equal(expected, 0) && equal(given, -expected)) {
            return new Diagnosis(MistakeType.CALCULATION, "טעות בסימן (פלוס/מינוס)");
        }
        if (isDigitSwap(expected, given)) {
            return new Diagnosis(MistakeType.CALCULATION, "הספרות נכונות אבל הוחלפו ביניהן");
        }
        double diff = Math.abs(expected - given);
        boolean integers = expected == Math.rint(expected) && given == Math.rint(given);
        double tolerance = integers ? (Math.abs(expected) >= 10 ? 2 : 1) : Math.abs(expected) * 0.05;
        if (diff <= Math.max(tolerance, Math.abs(expected) * 0.05) + EPSILON) {
            return new Diagnosis(MistakeType.CALCULATION, "התוצאה קרובה מאוד לנכונה – כנראה טעות חישוב קטנה");
        }
        if (!equal(expected, 0) && isPowerOfTenShift(expected, given)) {
            return new Diagnosis(MistakeType.CALCULATION, "הנקודה העשרונית או מספר האפסים זזו");
        }
        for (double q : questionNumbers) {
            if (equal(q, given) && !equal(q, expected)) {
                return new Diagnosis(MistakeType.MISREAD, "התשובה היא מספר שמופיע בשאלה עצמה");
            }
        }
        return new Diagnosis(MistakeType.CONCEPTUAL, "התוצאה רחוקה מהנכונה – כנראה שיטת הפתרון לא נכונה");
    }

    private static int rank(MistakeType type) {
        return switch (type) {
            case UNCLASSIFIED -> 0;
            case CALCULATION -> 1;
            case MISREAD -> 2;
            case CONCEPTUAL -> 3;
        };
    }

    private static boolean isDigitSwap(double expected, double given) {
        if (expected != Math.rint(expected) || given != Math.rint(given) || Math.signum(expected) != Math.signum(given)) {
            return false;
        }
        String a = Long.toString(Math.abs((long) expected));
        String b = Long.toString(Math.abs((long) given));
        if (a.length() != b.length() || a.length() < 2 || a.equals(b)) {
            return false;
        }
        char[] ca = a.toCharArray();
        char[] cb = b.toCharArray();
        Arrays.sort(ca);
        Arrays.sort(cb);
        return Arrays.equals(ca, cb);
    }

    private static boolean isPowerOfTenShift(double expected, double given) {
        double ratio = given / expected;
        for (double p : new double[]{10, 100, 1000, 0.1, 0.01, 0.001}) {
            if (Math.abs(ratio - p) < 1e-6 * p) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameValues(List<Double> a, List<Double> b) {
        List<Double> rest = new ArrayList<>(b);
        for (double x : a) {
            int idx = -1;
            for (int i = 0; i < rest.size(); i++) {
                if (equal(rest.get(i), x)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return false;
            }
            rest.remove(idx);
        }
        return true;
    }

    private static boolean sameInOrder(List<Double> a, List<Double> b) {
        for (int i = 0; i < a.size(); i++) {
            if (!equal(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Every number in the text, with {@code a/b} read as one fraction. */
    static List<Double> numbers(String text) {
        List<Double> result = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String token = m.group();
            int slash = token.indexOf('/');
            try {
                if (slash > 0) {
                    double denominator = Double.parseDouble(token.substring(slash + 1));
                    if (denominator == 0) {
                        continue;
                    }
                    result.add(Double.parseDouble(token.substring(0, slash)) / denominator);
                } else {
                    result.add(Double.parseDouble(token));
                }
            } catch (NumberFormatException ignored) {
                // The pattern only matches numbers.
            }
        }
        return result;
    }

    private static String compact(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static boolean equal(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.max(Math.abs(a), Math.abs(b)));
    }
}
