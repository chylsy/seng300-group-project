package campusplan;

import campusplan.domain.Student;
import campusplan.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * (FR 1: validate fields; FR 3: ids never change
 *  - name: 1–100 characters after trimming
 *  - email: exactly one '@', text on both sides, no whitespace
 *  - emailKey() is the trimmed, lowercased email
 */
class StudentTest {
    /*editing tests */

    @Test
     void editsDoNotChangeId() {
        Student student = new Student("S1", "Bob", "bob@example.com");

        student.setName("Bobby bill");
        student.setEmail("bobbybill@example.com");

        assertEquals("S1", student.getId());
        assertEquals("Bobby bill", student.getName());
        assertEquals("bobbybill@example.com", student.getEmail());
    }

    @Test
    void sameIdMeansEqual() {
        //load student saved to a file
        Student original = new Student("S1", "Bob", "bob@example.com");
        Student reloaded = new Student("S1", "Bob", "bob@example.com");

        assertEquals(original, reloaded);
        assertEquals(original.hashCode(), reloaded.hashCode());
    }

    @Test
    void differentIdMeansNotEqual() {
        Student a = new Student("S1", "Bob", "bob@example.com");
        Student b = new Student("S2", "Bob", "bob@example.com");

        assertNotEquals(a, b);
    }

    /*student creation tests */

    @Test
    void validStudentIsCreated() {
        Student student = new Student("S1", "Bob Bill", "bob@example.com");

        assertEquals("S1", student.getId());
        assertEquals("Bob Bill", student.getName());
        assertEquals("bob@example.com", student.getEmail());
    }

    @Test
    void nameAndEmailAreTrimmed() {
        Student student = new Student("S1", "  Bob bill       ", "bob@example.com");

        assertEquals("Bob bill", student.getName());
        assertEquals("bob@example.com", student.getEmail());
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Student("  ", "Bob", "bob@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new Student(null, "Bob", "bob@example.com"));
    }
        /*name tests */

    @Test
    void nameLengthBoundariesAreAllowed() {
        String hundredChars = "b".repeat(100);

        Student shortName = new Student("S1", "Bob", "b@example.com");
        Student longName = new Student("S2", hundredChars, "b@example.com");

        assertEquals("Bob", shortName.getName());
        assertEquals(hundredChars, longName.getName());
    }

    @Test
    void invalidNamesAreRejected() {
        assertThrows(ValidationException.class, () -> new Student("S1", "", "bob@example.com"));
        assertThrows(ValidationException.class, () -> new Student("S1", "   ", "bob@example.com"));
        assertThrows(ValidationException.class, () -> new Student("S1", null, "bob@example.com"));
        assertThrows(ValidationException.class,
                () -> new Student("S1", "b".repeat(101), "bob@example.com"));
    }

    @Test
    void failedSetNameKeepsOldName() {
        Student student = new Student("S1", "Bob", "bob@example.com");

        assertThrows(ValidationException.class, () -> student.setName(""));
        assertEquals("Bob", student.getName());
    }
     /*email tests */
    
    @Test
    void invalidEmailsAreRejected() {
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "bobexample.com"));  // no @
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "bob@@example.com")); // two @
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "a@b@example.com"));  // two @
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "@example.com"));     // nothing before @
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "bob@"));             // nothing after @
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", "bob t@example.com")); // space inside
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", ""));                 // empty
        assertThrows(ValidationException.class, () -> new Student("S1", "Bob", null));               // null
    }

    @Test
    void failedSetEmailKeepsOldEmail() {
        Student student = new Student("S1", "Bob", "bob@example.com");

        assertThrows(ValidationException.class, () -> student.setEmail("not-valid-email"));
        assertEquals("bob@example.com", student.getEmail());
    }

    @Test
    void emailKeyIsLowercase() {
        Student student = new Student("S1", "Bob", "  Bob.Bill@Example.COM ");
        assertEquals("bob.bill@example.com", student.emailKey());
    }

  
}
