package campusplan;

import campusplan.domain.*;
import campusplan.error.*;
import campusplan.service.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampusPlanDataTest {
    private CampusPlanData seeded() {
        return new SeedDataLoader().loadFromJson(Path.of("sample-data/seed.json"));
    }

    @Test void allFourMapsParticipateInEmptyCheck() {
        CampusPlanData data = new CampusPlanData();
        assertTrue(data.isEmpty());
        data.getAvailability().put("orphan", new StudentAvailability("orphan"));
        assertFalse(data.isEmpty());
        assertThrows(ValidationException.class, () -> data.loadSeedIfEmpty(seeded()));
        assertTrue(data.getStudents().isEmpty());
    }

    @Test void emailLookupTracksEditsInsteadOfKeepingAStaleIndex() {
        CampusPlanData data = seeded();
        Student student = data.findByEmail(" STUDENT01@EXAMPLE.TEST ").orElseThrow();
        student.setEmail("updated@example.test");
        assertTrue(data.findByEmail("student01@example.test").isEmpty());
        assertSame(student, data.findByEmail("UPDATED@EXAMPLE.TEST").orElseThrow());
    }

    @Test void knownStudentWithoutAvailabilityGetsAnEmptyRecord() {
        CampusPlanData data = new CampusPlanData();
        data.getStudents().put("S1", new Student("S1", "Student", "student@example.test"));
        StudentAvailability a = data.getStudentAvailability(" S1 ");
        assertTrue(a.getIntervals().isEmpty());
        assertSame(a, data.getStudentAvailability("S1"));
        assertFalse(a.covers(new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(10, 0))));
    }

    @Test void unknownStudentDoesNotCreateAvailability() {
        CampusPlanData data = new CampusPlanData();
        assertThrows(NotFoundException.class, () -> data.getStudentAvailability("missing"));
        assertTrue(data.isEmpty());
    }

    @Test void snapshotIsIndependentAndPreservesIdsAndResponses() {
        CampusPlanData original = seeded();
        CampusPlanData copy = original.deepCopy();
        assertEquals(original.getStudents().keySet(), copy.getStudents().keySet());
        assertEquals(original.getRooms().keySet(), copy.getRooms().keySet());
        assertEquals(original.getEvents().keySet(), copy.getEvents().keySet());
        assertNotSame(original.getEvents().get("EV-001"), copy.getEvents().get("EV-001"));
        copy.getStudents().get("S001").setName("Changed");
        copy.getStudentAvailability("S001").remove(new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(16, 0)));
        copy.getEvents().get("EV-001").getResponses().put("S001", "DECLINED");
        copy.getRooms().clear();
        assertEquals("Avery Chen", original.getStudents().get("S001").getName());
        assertTrue(original.getStudentAvailability("S001").covers(new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(12, 0))));
        assertEquals("PENDING", original.getEvents().get("EV-001").getResponses().get("S001"));
        assertEquals(6, original.getRooms().size());
    }

    @Test void loadingAnEmptyTargetCopiesInsteadOfAliasing() {
        CampusPlanData seed = seeded();
        CampusPlanData target = new CampusPlanData();
        target.loadSeedIfEmpty(seed);
        seed.getStudents().get("S001").setName("Changed source");
        assertEquals("Avery Chen", target.getStudents().get("S001").getName());
        assertThrows(ValidationException.class, () -> target.loadSeedIfEmpty(seed));
        assertEquals(12, target.getStudents().size());
    }

    @Test void copyFailureDoesNotPartiallyInstallSeed() {
        CampusPlanData bad = seeded();
        bad.getEvents().put("bad", null);
        CampusPlanData target = new CampusPlanData();
        assertThrows(RuntimeException.class, () -> target.loadSeedIfEmpty(bad));
        assertTrue(target.isEmpty());
        assertThrows(ValidationException.class, () -> target.loadSeedIfEmpty(null));
        assertTrue(target.isEmpty());
    }

    @Test void studentServiceUsesTheAgreedInfrastructure() {
        CampusPlanData data = new CampusPlanData();
        StudentService service = new StudentService(data, new UuidIdGenerator());
        Student student = service.create("  New Student ", " NEW@EXAMPLE.TEST ");
        assertTrue(student.getId().startsWith("S-"));
        assertEquals("New Student", student.getName());
        assertThrows(DuplicateEmailException.class, () -> service.create("Other", "new@example.test"));
        assertEquals(1, data.getStudents().size());
        assertThrows(ValidationException.class, () -> service.edit(student.getId(), "Changed", "invalid"));
        assertEquals("New Student", student.getName());
        assertThrows(NotFoundException.class, () -> service.get("missing"));
    }

    @Test void availabilityServiceWorksWithSeededAndNewStudents() {
        CampusPlanData data = seeded();
        AvailabilityService service = new AvailabilityService(data);
        TimeInterval slot = new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertTrue(service.isAvailable("S001", slot));
        Student student = new StudentService(data, new UuidIdGenerator()).create("New", "new@example.test");
        assertFalse(service.isAvailable(student.getId(), slot));
        service.addAvailability(student.getId(), slot);
        assertTrue(service.isAvailable(student.getId(), slot));
    }
}
