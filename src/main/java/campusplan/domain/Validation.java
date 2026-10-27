package campusplan.domain;

import campusplan.error.ValidationException;
import java.time.LocalTime;
import java.util.Locale;

public final class Validation {
    private Validation() { }

    public static void require(boolean condition, String field, String message) {
        if (!condition) {
            throw new ValidationException(field, message);
        }
    }

    public static String requireText(String value, int min, int max, String field) {
        require(min >= 1 && min <= max, field, "Invalid text length bounds.");
        require(value != null, field, field + " is required.");
        String checked = value.trim();
        require(checked.codePoints().anyMatch(c -> !isWhitespace(c))
                        && checked.length() >= min && checked.length() <= max,
                field, field + " must contain " + min + "-" + max + " characters after trimming.");
        return checked;
    }

    public static String requireEmail(String value) {
        require(value != null, "email", "Email is required.");
        String checked = value.trim().toLowerCase(Locale.ROOT);
        int at = checked.indexOf('@');
        require(at > 0 && at < checked.length() - 1 && at == checked.lastIndexOf('@'),
                "email", "Email must contain exactly one @ with text on both sides.");
        require(checked.codePoints().noneMatch(Validation::isWhitespace),
                "email", "Email must not contain whitespace.");
        return checked;
    }

    public static int requireRange(int value, int min, int max, String field) {
        require(min <= max, field, "Invalid numeric bounds.");
        require(value >= min && value <= max, field,
                field + " must be between " + min + " and " + max + ".");
        return value;
    }

    public static void onHalfHour(LocalTime time) {
        require(time != null, "time", "Time is required.");
        require(time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0,
                "time", "Time must be on a 30-minute boundary with zero seconds and nanoseconds.");
    }

    static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
