package campusplan;

import campusplan.domain.*;
import campusplan.error.ValidationException;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task7ValidationTest {
    @Test void textUsesTrimmedLengthAndAllowsBoundaries() {
        assertEquals("A", Validation.requireText(" A ", 1, 100, "name"));
        assertEquals("x".repeat(100), Validation.requireText("x".repeat(100), 1, 100, "name"));
        assertEquals(60, Validation.requireText("x".repeat(60), 1, 60, "room").length());
    }

    @Test void missingBlankAndLongTextHaveFieldErrors() {
        for (String input : new String[]{null, "", " ", "\t", "\u00a0", "x".repeat(101)}) {
            ValidationException e = assertThrows(ValidationException.class,
                    () -> Validation.requireText(input, 1, 100, "name"));
            assertEquals("name", e.getField());
        }
    }

    @Test void invalidTextBoundsAreRejected() {
        assertThrows(ValidationException.class, () -> Validation.requireText("a", 0, 10, "name"));
        assertThrows(ValidationException.class, () -> Validation.requireText("a", 2, 1, "name"));
    }

    @Test void emailNormalizesWithoutAddingExtraDomainRules() {
        assertEquals("a@b", Validation.requireEmail(" A@B "));
        assertEquals("a+b@example.test", Validation.requireEmail("A+B@EXAMPLE.TEST"));
    }

    @Test void malformedAndWhitespaceEmailsAreRejected() {
        for (String input : new String[]{null, "", "ab", "@b", "a@", "a@@b", "a@b@c", "a b@c", "a@b\nc", "a\u00a0@b"}) {
            assertThrows(ValidationException.class, () -> Validation.requireEmail(input));
        }
    }

    @Test void emailNormalizationIsLocaleIndependent() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("i@example.test", Validation.requireEmail("I@EXAMPLE.TEST"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test void rangesIncludeEndpointsAndRejectOutsideAndReversedBounds() {
        assertEquals(1, Validation.requireRange(1, 1, 100, "capacity"));
        assertEquals(100, Validation.requireRange(100, 1, 100, "capacity"));
        assertEquals(2, Validation.requireRange(2, 2, 2, "minimum"));
        for (int n : new int[]{0, 101}) {
            assertThrows(ValidationException.class, () -> Validation.requireRange(n, 1, 100, "capacity"));
        }
        assertThrows(ValidationException.class, () -> Validation.requireRange(2, 3, 1, "capacity"));
    }

    @Test void timeAlignmentRejectsSecondsAndNanoseconds() {
        assertDoesNotThrow(() -> Validation.onHalfHour(LocalTime.of(8, 0)));
        assertDoesNotThrow(() -> Validation.onHalfHour(LocalTime.of(21, 30)));
        for (LocalTime t : new LocalTime[]{null, LocalTime.of(8, 15), LocalTime.of(8, 30, 1), LocalTime.of(8, 0, 0, 1)}) {
            assertThrows(ValidationException.class, () -> Validation.onHalfHour(t));
        }
    }

    @Test void requirementKeepsItsFieldAndMessage() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> Validation.require(false, "invitees", "Select two students."));
        assertEquals("invitees", e.getField());
        assertEquals("Select two students.", e.getMessage());
        assertDoesNotThrow(() -> Validation.require(true, "unused", "unused"));
    }

    @Test void uuidIdsHavePrefixAndDoNotRestartAcrossInstances() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = new UuidIdGenerator().next(" S ");
            assertTrue(id.startsWith("S-"));
            assertEquals(4, UUID.fromString(id.substring(2)).version());
            assertTrue(generated.add(id));
        }
        assertTrue(new UuidIdGenerator().nextId("EV").startsWith("EV-"));
    }

    @Test void invalidIdPrefixesAreRejected() {
        for (String prefix : new String[]{null, "", " ", "student id", "x".repeat(41)}) {
            assertThrows(ValidationException.class, () -> new UuidIdGenerator().next(prefix));
        }
    }
}
