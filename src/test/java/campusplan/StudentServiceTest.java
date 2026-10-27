package campusplan;

import campusplan.service.StudentService;
import campusplan.domain.CampusPlanData;
import campusplan.domain.IdGenerator;
import campusplan.domain.Student;
import campusplan.error.DuplicateEmailException;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 FR 1 - create, edit, view and search students; validate fields; reject duplicate emails.
 FR 3 - students loaded from storage keep their ids and still work with the service.
 QR 2 - a failed operation leaves existing data unchanged.
 QR 4 - everything here runs without the console UI.
 */
class StudentServiceTest {

    private CampusPlanData data;
    private StudentService service;

    /*
      fake id generator to create simple id's for testing
     */
    private static class CountingIdGenerator implements IdGenerator {
        private int count = 0;

        @Override
        public String next(String prefix) {
            count = count + 1;
            return prefix + count;
        }
    }

    /* each test starts with an empty set of students */
    @BeforeEach
    void setUp() {
        data = new CampusPlanData();
        service = new StudentService(data, new CountingIdGenerator());
    }

    
    @Test
    void createStoresStudent() {
        Student bob = service.create("Bob Bill", "bob@example.com");

        assertEquals("S1", bob.getId());
        assertEquals("Bob Bill", bob.getName());
        assertEquals("bob@example.com", bob.getEmail());
        assertEquals(1, service.listAll().size());
    }
    
    @Test
    void createGivesUniqueIds() {
        Student bob = service.create("Bob", "bob@example.com");
        Student ben = service.create("Ben", "ben@example.com");

        assertNotEquals(bob.getId(), ben.getId());
    }

    @Test
    void createRejectsInvalidName() {
        assertThrows(ValidationException.class, () -> service.create("   ", "bob@example.com"));
        assertThrows(ValidationException.class, () -> service.create("b".repeat(101), "bob@example.com"));

        assertEquals(0, service.listAll().size());
    }

    @Test
    void createRejectsInvalidEmail() {
        assertThrows(ValidationException.class, () -> service.create("Bob", "bob.example.com")); /*no@ */

        assertEquals(0, service.listAll().size());
    }

    @Test
    void createRejectsDuplicateEmail() {
        service.create("bob", "bob@example.com");

        assertThrows(DuplicateEmailException.class,
                () -> service.create("Second Bob", "  bob@Example.com ")); 

        assertEquals(1, service.listAll().size());
    }

    /*edit tests */
    /*id cannot be changed */
    @Test
    void editUpdatesStudentIDstaysSame() {
        Student bob = service.create("Bob", "bob@example.com");

        service.edit(bob.getId(), "Bobby", "bobby@example.com");

        Student bobby = service.get(bob.getId());
        assertEquals("Bobby", bobby.getName());
        assertEquals("bobby@example.com", bobby.getEmail());
        assertEquals(bob.getId(), bobby.getId());
    }

    @Test
    void editAllowsKeepOwnEmail() {
        Student bob = service.create("Bob", "bob@example.com");

        service.edit(bob.getId(), "Bobby", "BOB@example.com");

        assertEquals("Bobby", service.get(bob.getId()).getName());
    }

    @Test
    void editRejectsOtherStudentEmailChangeNothing() {
        Student bob = service.create("Bob", "bob@example.com");
        service.create("Ben", "ben@example.com");
        assertThrows(DuplicateEmailException.class,
                () -> service.edit(bob.getId(), "Bob", "ben@example.com"));

        // neither the name nor the email changed as per QR 2
        Student found = service.get(bob.getId());
        assertEquals("Bob", found.getName());
        assertEquals("bob@example.com", found.getEmail());
    }

    @Test
    void editWithInvalidEmailChangesNothing() {
        Student bob = service.create("Bob", "bob@example.com");

        assertThrows(ValidationException.class,
                () -> service.edit(bob.getId(), "Bobby", "invalid email"));

        // valid name must not save on its own (QR 2)
        Student found = service.get(bob.getId());
        assertEquals("Bob", found.getName());
        assertEquals("bob@example.com", found.getEmail());
    }

    @Test
    void editUnknownIdThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.edit("S999", "Bob", "bob@example.com"));
    }

    /*get and listAll test */

    @Test
    void getReturnsStudent() {
        Student bob = service.create("Bob", "bob@example.com");
        assertSame(bob, service.get(bob.getId()));
    }

    @Test
    void getTrimsId() {
        Student bob = service.create("Bob", "bob@example.com");
        assertSame(bob, service.get("  " + bob.getId() + "  ")); //should ignore spaces
    }

    @Test
    void getUnknownIdThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.get("S9989"));
        assertThrows(NotFoundException.class, () -> service.get(null));
    }

    @Test
    void NoStudentsListAllEmpty() {
        assertTrue(service.listAll().isEmpty());
    }

    @Test
    void listAllIsSortedByNameThenID() {
        Student bob1 = service.create("bob", "bob1@example.com");  // id:S1
        Student alice = service.create("Alice", "alice@example.com"); // id:S2
        Student bob2 = service.create("Bob", "bob2@example.com");  // id:S3
        List<Student> all = service.listAll();

        assertEquals(List.of(alice, bob1, bob2), all);
    }

    @Test
    void listAllReturnsCopy() {//changing list does not change stored students
        service.create("Bob", "bob@example.com");
        List<Student> all = service.listAll();
        all.clear();

        assertEquals(1, service.listAll().size());
    }

    /*search tests */
    @Test
    void searchByName() {//should be able to search with a part of name, ignoring case
        Student bob = service.create("Bob Bill", "bob@example.com");
        service.create("Ben Smith", "ben@example.com");

        assertEquals(List.of(bob), service.search("BIL"));
    }

    @Test
    void searchByEmail() {//should be able to search with a part of email
        service.create("Alice Zoo", "alice@example.com");
        Student ben = service.create("Ben Smith", "ben@school.ca");

        assertEquals(List.of(ben), service.search("school.ca"));
    }

    @Test
    void searchNoMatchesReturnEmpty() {
        service.create("Bob Bill", "bob@example.com");
        assertTrue(service.search("meow").isEmpty());
    }

    @Test
    void searchBlankReturnsAll() {
        service.create("Alice", "alice@example.com");
        service.create("Ben", "ben@example.com");

        assertEquals(2, service.search("   ").size());
        assertEquals(2, service.search(null).size());
    }

    /*load from storage tests */
    @Test
    void loadedStudentKeepsId() {//can be found and edited
        // let this student was loaded from the save file
        Student loaded = new Student("S-saved-42", "Alice", "alice@example.com");
        data.getStudents().put(loaded.getId(), loaded);

        Student found = service.get("S-saved-42");
        service.edit("S-saved-42", "Alice Zoo", "alice@example.com");

        assertEquals("S-saved-42", found.getId());
        assertEquals("Alice Zoo", service.get("S-saved-42").getName());
    }

    @Test
    void duplicateCheckIncludesLoadedStudents() {
        Student loaded = new Student("S-saved-42", "Ben", "ben@example.com");
        data.getStudents().put(loaded.getId(), loaded);

        assertThrows(DuplicateEmailException.class,
                () -> service.create("ken", "ben@example.com"));
    }
}