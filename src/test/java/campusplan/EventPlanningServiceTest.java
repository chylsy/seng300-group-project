package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import campusplan.domain.IdGenerator;
import campusplan.domain.UuidIdGenerator;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.EventStatus;
import campusplan.domain.RoomFeature;
import campusplan.domain.Student;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;
import campusplan.service.EventPlanningService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventPlanningServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    //student ids s1..sN
    private static List<String> ids(int n) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add("s" + i);
        }
        return list;
    }

    //data that already contains students s1..sN
    private static CampusPlanData dataWithStudents(int n) {
        CampusPlanData data = new CampusPlanData();
        for (String id : ids(n)) {
            data.getStudents().put(id, new Student(id, id, id + "@ucalgary.ca"));
        }
        return data;
    }

    private static EventDraftRequest req(String title, List<String> invitees, LocalDate start,
                                         LocalDate end, int minutes, int minAttendance) {
        return new EventDraftRequest(title, invitees, start, end, minutes,
                Set.of(RoomFeature.PROJECTOR), minAttendance);
    }

    //a fake generator that hands out EV-001, EV-002, ... so tests can predict ids
    private static IdGenerator sequential() {
        int[] counter = {0};
        return prefix -> String.format("EV-%03d", ++counter[0]);
    }

    private static EventPlanningService serviceFor(CampusPlanData data) {
        return new EventPlanningService(data, sequential());
    }

    //a request that passes every rule
    private static EventDraftRequest valid() {
        return req("Study night", ids(3), START, START.plusDays(2), 60, 2);
    }

    //createDraft success
    @Test
    void createDraftReturnsDraftWithIdAndNoRoomOrSlot() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        Event event = service.createDraft(valid());
        assertEquals("EV-001", event.getId());
        assertEquals("Study night", event.getTitle());
        assertEquals(EventStatus.DRAFT, event.getStatus());
        assertEquals(ids(3), event.getInviteeIds());
        assertEquals(3, event.getResponses().size());
        for (String studentId : ids(3)) {
            assertEquals(Event.PENDING, event.getResponses().get(studentId));
        }
        assertNull(event.getRoomId());
        assertNull(event.getSlot());
    }

    @Test
    void eachDraftGetsItsOwnId() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        Event first = service.createDraft(valid());
        Event second = service.createDraft(valid());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, service.listAll().size());
    }

    //E1 title
    @Test
    void emptyOrBlankTitleRejected() {
        CampusPlanData data = dataWithStudents(3);
        EventPlanningService service = serviceFor(data);
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.createDraft(req("", ids(3), START, START.plusDays(2), 60, 2)));
        assertEquals("title", error.getField());
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("   ", ids(3), START, START.plusDays(2), 60, 2)));
        assertTrue(data.getEvents().isEmpty());
    }

    @Test
    void titleLongerThan100Rejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("x".repeat(101), ids(3), START, START.plusDays(2), 60, 2)));
        assertEquals("EV-001",
                service.createDraft(req("x".repeat(100), ids(3), START, START.plusDays(2), 60, 2)).getId());
    }

    //E2 invitee count
    @Test
    void fewerThanTwoOrMoreThanTwentyInviteesRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(21));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Too few", ids(1), START, START.plusDays(2), 60, 1)));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Too many", ids(21), START, START.plusDays(2), 60, 2)));
        assertEquals(0, service.listAll().size());
    }

    @Test
    void twoAndTwentyInviteesAccepted() {
        EventPlanningService service = serviceFor(dataWithStudents(20));
        service.createDraft(req("Two", ids(2), START, START.plusDays(2), 60, 2));
        service.createDraft(req("Twenty", ids(20), START, START.plusDays(2), 60, 2));
        assertEquals(2, service.listAll().size());
    }

    //E3 duplicates
    @Test
    void duplicateInviteeRejected() {
        CampusPlanData data = dataWithStudents(3);
        EventPlanningService service = serviceFor(data);
        List<String> withRepeat = List.of("s1", "s2", "s1");
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Repeat", withRepeat, START, START.plusDays(2), 60, 2)));
        assertTrue(data.getEvents().isEmpty());
    }

    //E4 window
    @Test
    void windowLongerThanSevenDaysOrBackwardsRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Long", ids(3), START, START.plusDays(7), 60, 2)));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Backwards", ids(3), START, START.minusDays(1), 60, 2)));
    }

    @Test
    void oneDayAndSevenDayWindowsAccepted() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        service.createDraft(req("One day", ids(3), START, START, 60, 2));
        service.createDraft(req("Seven days", ids(3), START, START.plusDays(6), 60, 2));
        assertEquals(2, service.listAll().size());
    }

    //E5 duration
    @Test
    void durationOutsideRangeOrNotInThirtyMinuteStepsRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        for (int minutes : new int[] {0, 29, 45, 181, 210}) {
            assertThrows(ValidationException.class,
                    () -> service.createDraft(req("Bad length", ids(3), START, START.plusDays(2), minutes, 2)));
        }
        assertEquals(0, service.listAll().size());
    }

    @Test
    void thirtyAndOneEightyMinutesAccepted() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        service.createDraft(req("Short", ids(3), START, START.plusDays(2), 30, 2));
        service.createDraft(req("Long", ids(3), START, START.plusDays(2), 180, 2));
        assertEquals(2, service.listAll().size());
    }

    //E6 minimum attendance
    @Test
    void minimumAttendanceBelowTwoOrAboveInviteesRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("Low", ids(3), START, START.plusDays(2), 60, 1)));
        assertThrows(ValidationException.class,
                () -> service.createDraft(req("High", ids(3), START, START.plusDays(2), 60, 4)));
    }

    @Test
    void minimumAttendanceEqualToInviteesAccepted() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        service.createDraft(req("Everyone", ids(3), START, START.plusDays(2), 60, 3));
        assertEquals(1, service.listAll().size());
    }

    //E7 unknown student
    @Test
    void unknownInviteeRejectedAndNothingSaved() {
        CampusPlanData data = dataWithStudents(2);
        EventPlanningService service = serviceFor(data);
        List<String> withStranger = List.of("s1", "s2", "ghost");
        assertThrows(NotFoundException.class,
                () -> service.createDraft(req("Stranger", withStranger, START, START.plusDays(2), 60, 2)));
        assertTrue(data.getEvents().isEmpty());
    }

    //editDraft
    @Test
    void editDraftReplacesDetailsButKeepsIdAndStatus() {
        EventPlanningService service = serviceFor(dataWithStudents(4));
        Event created = service.createDraft(valid());
        Event edited = service.editDraft(created.getId(),
                req("Exam prep", ids(4), START, START.plusDays(3), 90, 3));
        assertEquals(created.getId(), edited.getId());
        assertEquals("Exam prep", edited.getTitle());
        assertEquals(4, edited.getInviteeIds().size());
        assertEquals(90, edited.getDurationMinutes());
        assertEquals(EventStatus.DRAFT, edited.getStatus());
        assertEquals(1, service.listAll().size());
    }

    @Test
    void rejectedEditLeavesTheDraftExactlyAsItWas() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        Event created = service.createDraft(valid());
        assertThrows(ValidationException.class,
                () -> service.editDraft(created.getId(), req("", ids(3), START, START.plusDays(2), 60, 2)));
        assertThrows(NotFoundException.class,
                () -> service.editDraft(created.getId(), req("New title", List.of("s1", "ghost"), START, START.plusDays(2), 60, 2)));
        Event after = service.get(created.getId());
        assertEquals("Study night", after.getTitle());
        assertEquals(ids(3), after.getInviteeIds());
        assertEquals(60, after.getDurationMinutes());
    }

    @Test
    void editingUnknownEventRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        assertThrows(NotFoundException.class, () -> service.editDraft("EV-999", valid()));
    }

    //get and listAll
    @Test
    void getReturnsTheEventAndUnknownIdIsRejected() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        Event created = service.createDraft(valid());
        assertEquals("Study night", service.get(created.getId()).getTitle());
        assertThrows(NotFoundException.class, () -> service.get("EV-999"));
    }

    @Test
    void listAllOnNoEventsReturnsEmptyList() {
        assertTrue(serviceFor(dataWithStudents(3)).listAll().isEmpty());
    }

    @Test
    void listAllIsSortedByIdAndChangingItDoesNotChangeTheData() {
        EventPlanningService service = serviceFor(dataWithStudents(3));
        service.createDraft(valid());
        service.createDraft(valid());
        List<Event> all = service.listAll();
        assertEquals("EV-001", all.get(0).getId());
        assertEquals("EV-002", all.get(1).getId());
        all.clear();
        assertEquals(2, service.listAll().size());
    }

    //ids come from the generator
    @Test
    void idsComeFromTheGeneratorUsingTheEventPrefix() {
        EventPlanningService service = new EventPlanningService(dataWithStudents(3), prefix -> prefix + "-fixed");
        assertEquals("event-fixed", service.createDraft(valid()).getId());
    }

    @Test
    void defaultGeneratorGivesEachDraftADifferentEventId() {
        EventPlanningService service = new EventPlanningService(dataWithStudents(3));
        Event first = service.createDraft(valid());
        Event second = service.createDraft(valid());
        assertTrue(first.getId().startsWith("event-"));
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, service.listAll().size());
    }

    @Test
    void editingInviteesDropsRemovedResponsesAndStartsNewOnesPending() {
        EventPlanningService service = serviceFor(dataWithStudents(4));
        Event created = service.createDraft(req("Study night", List.of("s1", "s2", "s3"), START, START.plusDays(2), 60, 2));
        created.getResponses().put("s2", "ACCEPTED");
        Event edited = service.editDraft(created.getId(),
                req("Study night", List.of("s2", "s3", "s4"), START, START.plusDays(2), 60, 2));
        assertEquals(Set.of("s2", "s3", "s4"), edited.getResponses().keySet());
        assertEquals("ACCEPTED", edited.getResponses().get("s2"));
        assertEquals(Event.PENDING, edited.getResponses().get("s4"));
    }

    @Test
    void copyOfAnEventIsIndependentOfTheOriginal() {
        Event original = new Event("EV-001", valid());
        Event copy = new Event(original);
        copy.getResponses().put("s1", "ACCEPTED");
        assertEquals(Event.PENDING, original.getResponses().get("s1"));
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getInviteeIds(), copy.getInviteeIds());
    }
}