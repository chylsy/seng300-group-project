package campusplan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.Room;
import campusplan.domain.Student;
import campusplan.domain.StudentAvailability;
import campusplan.domain.Suggestion;
import campusplan.domain.SuggestionResult;
import campusplan.domain.TimeInterval;
import campusplan.service.AvailabilityEligibilityRule;
import campusplan.service.EventPlanningService;
import campusplan.service.SuggestionGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;




/*TEST FR4: who can attend and why
 - a student is eligible if they availability covers the whole slot &they
 have no ACCEPTED at another SCHEDULED event that overlaps it */
 class AvailabilityEligibilityRuleTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);
    private static final String NOT_AVAILABLE = "not available at that time";
    private static final String ALREADY_ACCEPTED = "already accepted another event at that time";

    private CampusPlanData data;
    private EventPlanningService service;
    private AvailabilityEligibilityRule rule;
    private Event meeting;   //the draft being planned, invitees s1 and s2

    //use a fresh setup before each test 
    @BeforeEach
    void setUp() {
        data = new CampusPlanData();
        for (String id : List.of("s1", "s2", "s3")) {
            data.getStudents().put(id, new Student(id, id, id + "@ucalgary.ca"));
        }
        data.getRooms().put("R1", new Room("R1", "Room 1", 10, Set.of()));
        service = new EventPlanningService(data);
        rule = new AvailabilityEligibilityRule(data);
        meeting = draft("Project meeting", List.of("s1", "s2"));
    }
    //helpers
    private static TimeInterval slot(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeInterval(DAY, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    //gives student availability
    private void free(String studentId, TimeInterval interval) {
        data.getAvailability().computeIfAbsent(studentId, StudentAvailability::new).add(interval);
    }

    private void freeAllDay(String... studentIds) {
        for (String id : studentIds) {
            free(id, slot(8, 0, 22, 0));
        }
    }

    private Event draft(String title, List<String> invitees) {
        return service.createDraft(new EventDraftRequest(title, invitees, DAY, DAY, 60, Set.of(), 2));
    }

    //create a second event, confirm that it is in R1, set a student response
    private Event scheduled(List<String> invitees, TimeInterval at, String studentId, String response) {
        Event other = draft("Other", invitees);
        service.confirm(other.getId(), "R1", at);
        other.getResponses().put(studentId, response);
        return other;
    }
    
    private static void assertSameSlot(TimeInterval expected, TimeInterval actual) {
    assertEquals(expected.getDate(), actual.getDate());
    assertEquals(expected.getStart(), actual.getStart());
    assertEquals(expected.getEnd(), actual.getEnd());
}

    @Test
    void availabilityCoveringTheSlotIsEligible() {
        free("s1", slot(9, 0, 12, 0));
        assertNull(rule.ineligibleReason(meeting, "s1", slot(10, 0, 11, 0)));
    }

    @Test
    void availabilityMatchingTheSlotIsEligible() {
        free("s1", slot(10, 0, 11, 0));
        assertNull(rule.ineligibleReason(meeting, "s1", slot(10, 0, 11, 0)));
    }

    @Test
    void noAvailabilityRecordIsIneligible() {
        //missing availability means unavailable
        assertEquals(NOT_AVAILABLE, rule.ineligibleReason(meeting, "s1", slot(10, 0, 11, 0)));
    }

    @Test
    void availabilityCoveringOnlyPartOfTheSlotIsIneligible() {
        free("s1", slot(9, 0, 10, 30));
        assertEquals(NOT_AVAILABLE, rule.ineligibleReason(meeting, "s1", slot(10, 0, 11, 0)));
    }

    @Test
    void availabilityOnAnotherDayIsIneligible() {
        free("s1", new TimeInterval(DAY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertEquals(NOT_AVAILABLE, rule.ineligibleReason(meeting, "s1", slot(10, 0, 11, 0)));
    }

    @Test
    void adjacentIntervalsMergeToCoverASlotAcrossThem() {
        free("s1", slot(9, 0, 10, 0));
        free("s1", slot(10, 0, 11, 0));
        assertNull(rule.ineligibleReason(meeting, "s1", slot(9, 30, 10, 30)));
    }
    /*slot 13:00-14:00 accepted elsewhere by student */
    @Test
    void acceptedOverlappingEventMakesTheStudentIneligible() {
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        assertEquals(ALREADY_ACCEPTED, rule.ineligibleReason(meeting, "s1", slot(13, 30, 14, 30)));
    }

    @Test
    void anotherInviteesAcceptanceDoesNotAffectThisStudent() {
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        assertNull(rule.ineligibleReason(meeting, "s2", slot(13, 30, 14, 30)));
    }

    @Test
    void slotStartingWhenTheAcceptedEventEndsIsEligible() {
        //intervals are half-open: 13:00-14:00 and 14:00-15:00 do not overlap
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        assertNull(rule.ineligibleReason(meeting, "s1", slot(14, 0, 15, 0)));
    }

    @Test
    void slotEndingWhenTheAcceptedEventStartsIsEligible() {
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(14, 0, 15, 0), "s1", Event.ACCEPTED);
        assertNull(rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0)));
    }
    //DECLINED/PENDING does not block
    @Test
    void declinedOverlappingEventDoesNotBlock() {
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", "DECLINED");
        assertNull(rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0)));
    }

    @Test
    void pendingOverlappingEventDoesNotBlock() {
        freeAllDay("s1", "s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.PENDING);
        assertNull(rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0)));
    }

    @Test
    void cancelledEventFreesTheStudentsTime() {
        freeAllDay("s1", "s2");
        Event other = scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        service.cancel(other.getId(), true);
        assertNull(rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0)));
    }

    @Test
    void scheduledEventTheStudentIsNotInvitedToIsIgnored() {
        freeAllDay("s1", "s2", "s3");
        scheduled(List.of("s2", "s3"), slot(13, 0, 14, 0), "s2", Event.ACCEPTED);
        assertNull(rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0)));
    }

    @Test
    void EventDoesNotConflictWithItself() {
        freeAllDay("s1", "s2");
        Event event = scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        assertNull(rule.ineligibleReason(event, "s1", slot(13, 0, 14, 0)));
    }
    //rule behaviour tests
    @Test
    void theAvailabilityReasonIsGivenWhenBothRulesFail() {
        free("s1", slot(8, 0, 14, 0));  
        freeAllDay("s2");
        scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
        assertEquals(NOT_AVAILABLE, rule.ineligibleReason(meeting, "s1", slot(13, 30, 14, 30)));
    }

    @Test
    void checkingChangesNothing() {
    freeAllDay("s1", "s2");
    Event other = scheduled(List.of("s1", "s2"), slot(13, 0, 14, 0), "s1", Event.ACCEPTED);
    rule.ineligibleReason(meeting, "s1", slot(13, 0, 14, 0));
    assertEquals(Event.ACCEPTED, other.responseOf("s1"));
    assertSameSlot(slot(13, 0, 14, 0), other.getSlot());
    assertNull(meeting.getSlot());
    assertEquals(2, data.getEvents().size());
}

    //test rule inside SuggestionGenerator: 6 students, the last student has no availability
    @Test
    void RuleNamesTheEligibleStudentsAndGivesTheIneligibleStudentReason() {
        List<String> six = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String id = "p" + i;
            six.add(id);
            data.getStudents().put(id, new Student(id, id, id + "@ucalgary.ca"));
            if (i <= 5) {
                free(id, slot(10, 0, 12, 0));
            }
        }
        Event event = service.createDraft(new EventDraftRequest("Project meeting", six, DAY, DAY, 120, Set.of(), 4));

        SuggestionResult result = new SuggestionGenerator(data, rule).generate(event);

        assertEquals(1, result.getTop().size());
        Suggestion only = result.getTop().get(0);
        assertSameSlot(slot(10, 0, 12, 0), only.getSlot());
        assertEquals(six.subList(0, 5), only.getEligibleIds());
        assertEquals(List.of("p6"), new ArrayList<>(only.getIneligibleReasons().keySet()));
        assertEquals(NOT_AVAILABLE, only.getIneligibleReasons().get("p6"));
    }


    


}
