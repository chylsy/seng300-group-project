package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.domain.Suggestion;
import campusplan.domain.SuggestionResult;
import campusplan.service.EligibilityRule;
import campusplan.service.SuggestionGenerator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SuggestionGeneratorTest {
    private static final LocalDate DAY=LocalDate.of(2026, 11, 2);
    private static final Set<RoomFeature> NONE=Set.of();
    private static final EligibilityRule EVERYONE =(event, studentId, slot) -> null;

    private static List<String> ids(int n){
        List<String> list=new ArrayList<>();
        for (int i=1; i<=n; i++) {
            list.add("s"+i);
        }
        return list;
    }

    private static Event event(int invitees, int minutes, int minAttendance, int days, Set<RoomFeature> features) {
        return new Event("EV-1",new EventDraftRequest("Study night", ids(invitees),DAY,DAY.plusDays(days - 1), minutes, features, minAttendance));
    }

    private static Room room(String id, int capacity, Set<RoomFeature> features){
        return new Room(id, "Room "+id, capacity, features);
    }

    private static SuggestionGenerator generatorFor(EligibilityRule rule, Room... rooms){
        CampusPlanData data=new CampusPlanData();
        for (Room r:rooms){
            data.getRooms().put(r.getId(),r);
        }
        return new SuggestionGenerator(data, rule);
    }

    private static EligibilityRule cannotAttend(String... blocked) {
        Set<String> set=Set.of(blocked);
        return (event,studentId,slot) ->set.contains(studentId) ? "not available" : null;
    }

    private static List<String> describe(SuggestionResult result){
        List<String> lines =new ArrayList<>();
        for (Suggestion s :result.getTop()) {
            lines.add(s.getRoomId() + "@"+s.getSlot().getDate() +" " + s.getSlot().getStart());
        }
        return lines;
    }

    //acceptance example 1: six invitees, five eligible, minimum four, suitable room
    @Test
    void fiveOfSixEligibleNamesTheFiveAndTheReasonForTheSixth() {
        SuggestionResult result = generatorFor(cannotAttend("s6"), room("R1", 10, NONE)).generate(event(6, 60, 4, 1, NONE));
        Suggestion first=result.getTop().get(0);
        assertEquals(List.of("s1", "s2","s3","s4", "s5"), first.getEligibleIds());
        assertEquals(List.of("s6"), new ArrayList<>(first.getIneligibleReasons().keySet()));
        assertEquals("not available",first.getIneligibleReasons().get("s6"));
    }

    //acceptance example 2: five eligible, minimum six (also the no-result case)
    @Test
    void minimumAttendanceAboveTheEligibleCountExcludesEveryCandidate() {
        SuggestionResult result=generatorFor(cannotAttend("s6"), room("R1", 10, NONE)).generate(event(6, 60, 6, 1, NONE));
        assertTrue(result.getTop().isEmpty());
        assertEquals(27, result.getRejectedForAttendance());
        assertEquals(0, result.getRejectedForRoom());
    }

    //acceptance example 3:room holds five, event invites six
    @Test
    void roomTooSmallIsExcludedEvenWhenEnoughInviteesAreEligible() {
        SuggestionResult result = generatorFor(cannotAttend("s6"), room("R1", 5, NONE)).generate(event(6, 60, 4, 1, NONE));
        assertTrue(result.getTop().isEmpty());
        assertEquals(27,result.getRejectedForRoom());
        assertEquals(0, result.getRejectedForAttendance());
    }
    //no-result counts: room failures are counted first, so the counts do not overlap
    @Test
    void roomFailuresAreCountedFirstSoTheTwoCountsDoNotOverlap() {
        Room noProjector = room("R1", 10, NONE);
        Room withProjector =room("R2", 10, Set.of(RoomFeature.PROJECTOR));
        SuggestionResult result =generatorFor(cannotAttend("s2", "s3", "s4", "s5", "s6"), noProjector, withProjector).generate(event(6, 60, 3, 1, Set.of(RoomFeature.PROJECTOR)));
        assertTrue(result.getTop().isEmpty());
        assertEquals(27, result.getRejectedForRoom());
        assertEquals(27, result.getRejectedForAttendance());
    }

    //boundaries: 60 minutes gives starts 08:00 to 21:00 (27), 180 minutes gives 08:00 to 19:00 (23)
    @Test
    void candidatesRunFromOpeningToTheLastSlotThatEndsAtClosing() {
        EligibilityRule nobody = cannotAttend("s1", "s2");
        assertEquals(27, generatorFor(nobody, room("R1", 10, NONE)).generate(event(2, 60, 2, 1, NONE)).getRejectedForAttendance());
        assertEquals(23, generatorFor(nobody, room("R1", 10, NONE)).generate(event(2, 180, 2, 1, NONE)).getRejectedForAttendance());
    }

    //ranking level 1: eligible count beats an earlier time
    @Test
    void moreEligibleStudentsRankAheadOfAnEarlierTime() {
        EligibilityRule s6BusyBeforeTen=(event, id, slot) -> id.equals("s6") && slot.getStart().isBefore(LocalTime.of(10, 0)) ? "busy" : null;
        SuggestionResult result = generatorFor(s6BusyBeforeTen, room("R1", 10, NONE)).generate(event(6,60, 4, 1, NONE));
        assertEquals(LocalTime.of(10, 0), result.getTop().get(0).getSlot().getStart());
        assertEquals(6, result.getTop().get(0).eligibleCount());
    }

    //ranking level 2: date and time
    @Test
    void sameEligibleCountIsOrderedByDateThenTime() {
        SuggestionResult result = generatorFor(EVERYONE, room("R1", 10, NONE)).generate(event(6, 60, 2, 2, NONE));
        assertEquals(List.of("R1@2026-11-02 08:00", "R1@2026-11-02 08:30", "R1@2026-11-02 09:00", "R1@2026-11-02 09:30","R1@2026-11-02 10:00"),describe(result));
    }

    //ranking levels 3 and 4: unused seats, then room ID (six invitees: R1 has 4 unused seats, R2 and R3 have 2)
    @Test
    void sameDateAndTimeIsOrderedByFewestUnusedSeatsThenRoomId(){
        SuggestionResult result =generatorFor(EVERYONE, room("R3", 8, NONE), room("R1", 10,NONE), room("R2", 8,NONE)).generate(event(6, 60, 2, 1, NONE));
        assertEquals(List.of("R2@2026-11-02 08:00", "R3@2026-11-02 08:00", "R1@2026-11-02 08:00", "R2@2026-11-02 08:30", "R3@2026-11-02 08:30"), describe(result));
    }

    //acceptance example 6: repeated searches return the same order
    @Test
    void repeatedRequestsReturnTheSameOrder(){
        SuggestionGenerator generator=generatorFor(EVERYONE,room("R3", 8, NONE), room("R1", 10, NONE),room("R2",8, NONE));
        Event event=event(6, 60, 2, 2, NONE);
        assertEquals(describe(generator.generate(event)),describe(generator.generate(event)));
    }

    @Test
    void neverMoreThanFiveSuggestionsAreReturned(){
        SuggestionResult result=generatorFor(EVERYONE, room("R1", 10, NONE)).generate(event(6, 60, 2, 1, NONE));
        assertEquals(5,result.getTop().size());
    }

}