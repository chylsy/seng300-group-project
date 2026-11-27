package campusplan;

import campusplan.domain.*;
import campusplan.error.ValidationException;
import campusplan.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceServiceTest {

    private CampusPlanData data;
    private AttendanceService attendanceService;

    private Student student1;
    private Student student2;

    private Event event;

    @BeforeEach
    void setUp() {

        data = new CampusPlanData();

        attendanceService = new AttendanceService(data);

        // create students

        student1 = new Student(
                "S1",
                "Alice",
                "alice@test.com");

        student2 = new Student(
                "S2",
                "Bob",
                "bob@test.com");

        data.getStudents().put(student1.getId(), student1);
        data.getStudents().put(student2.getId(), student2);

        // availability

        StudentAvailability availability1 =
                new StudentAvailability(student1.getId());

        availability1.add(
                new TimeInterval(
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)));

        data.getAvailability().put(
                student1.getId(),
                availability1);

        // event

        EventDraftRequest request =
                new EventDraftRequest(
                        "Meeting",
                        List.of("S1", "S2"),
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1),
                        60,
                        Set.of(),
                        2);

        event = new Event("E1", request);

        event.schedule(
                "R1",
                new TimeInterval(
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)));

        data.getEvents().put(event.getId(), event);
    }

    @Test
    void acceptAvailableStudent() {

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.ACCEPTED);

        assertEquals(
                Event.ACCEPTED,
                event.responseOf("S1"));
    }

    @Test
    void rejectUnavailableStudent() {

        assertThrows(
                ValidationException.class,
                () -> attendanceService.recordResponse(
                        "E1",
                        "S2",
                        Event.ACCEPTED));
    }

    @Test
    void canRecordDeclinedResponse() {

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.DECLINED);

        assertEquals(
                Event.DECLINED,
                event.responseOf("S1"));
    }

    @Test
    void canRecordPendingResponse() {

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.PENDING);

        assertEquals(
                Event.PENDING,
                event.responseOf("S1"));
    }

    @Test
    void acceptedCountReturnsCorrectValue() {

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.ACCEPTED);

        assertEquals(
                1,
                attendanceService.acceptedCount("E1"));
    }

    @Test
    void rejectOverlappingAcceptedEvent() {

        // First event

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.ACCEPTED);

        // Create overlapping event

        EventDraftRequest request =
                new EventDraftRequest(
                        "Second Meeting",
                        List.of("S1"),
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1),
                        60,
                        Set.of(),
                        1);

        Event secondEvent =
                new Event("E2", request);

        secondEvent.schedule(
                "R2",
                new TimeInterval(
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 30),
                        LocalTime.of(11, 30)));

        data.getEvents().put(
                secondEvent.getId(),
                secondEvent);

        assertThrows(
                ValidationException.class,
                () -> attendanceService.recordResponse(
                        "E2",
                        "S1",
                        Event.ACCEPTED));
    }

    @Test
    void pendingReleasesConflict() {

        // Accept first event

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.ACCEPTED);

        // Create overlapping second event

        EventDraftRequest request =
                new EventDraftRequest(
                        "Second Meeting",
                        List.of("S1", "S2"),
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1),
                        60,
                        Set.of(),
                        2);

        Event secondEvent =
                new Event("E2", request);

        secondEvent.schedule(
                "R2",
                new TimeInterval(
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 30),
                        LocalTime.of(11, 30)));

        data.getEvents().put(
                secondEvent.getId(),
                secondEvent);

        // Verify overlap causes rejection

        assertThrows(
                ValidationException.class,
                () -> attendanceService.recordResponse(
                        "E2",
                        "S1",
                        Event.ACCEPTED));

        // Release commitment

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.PENDING);

        // Now acceptance should succeed

        assertDoesNotThrow(
                () -> attendanceService.recordResponse(
                        "E2",
                        "S1",
                        Event.ACCEPTED));

        assertEquals(
                Event.ACCEPTED,
                secondEvent.responseOf("S1"));
    }

    @Test
    void declinedReleasesConflict() {

        // Accept first event

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.ACCEPTED);

        // Create overlapping second event

        EventDraftRequest request =
                new EventDraftRequest(
                        "Second Meeting",
                        List.of("S1", "S2"),
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1),
                        60,
                        Set.of(),
                        2);

        Event secondEvent =
                new Event("E2", request);

        secondEvent.schedule(
                "R2",
                new TimeInterval(
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 30),
                        LocalTime.of(11, 30)));

        data.getEvents().put(
                secondEvent.getId(),
                secondEvent);

        // Verify overlap causes rejection

        assertThrows(
                ValidationException.class,
                () -> attendanceService.recordResponse(
                        "E2",
                        "S1",
                        Event.ACCEPTED));

        // Release commitment by declining Event A

        attendanceService.recordResponse(
                "E1",
                "S1",
                Event.DECLINED);

        // Now Event B should be allowed

        assertDoesNotThrow(
                () -> attendanceService.recordResponse(
                        "E2",
                        "S1",
                        Event.ACCEPTED));

        assertEquals(
                Event.ACCEPTED,
                secondEvent.responseOf("S1"));
    }
}