package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.Room;
import campusplan.domain.SuggestionResult;
import campusplan.domain.TimeInterval;
import campusplan.service.EligibilityRule;
import campusplan.service.SuggestionGenerator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

//Room conflicts with SCHEDULED events (section 3, example 4).Everyone is made ineligible, so every candidate that passes the room check is counted in rejectedForAttendance.
class SuggestionRoomConflictTest{
    private static final LocalDate DAY =LocalDate.of(2026, 11, 2);
    private static final EligibilityRule NOBODY=(event, studentId, slot) -> "not available";

    private static Event planned(){
        return new Event("EV-1",new EventDraftRequest("Study night", List.of("s1", "s2"), DAY, DAY, 60, Set.of(), 2));
    }

    private static Event scheduled(String id, String roomId, int startHour, int endHour) {
        Event e=new Event(id, new EventDraftRequest("Other", List.of("s8", "s9"), DAY, DAY, 60, Set.of(), 2));
        e.schedule(roomId, new TimeInterval(DAY, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0)));
        return e;
    }

    private static CampusPlanData dataWith(Event... events) {
        CampusPlanData data = new CampusPlanData();
        data.getRooms().put("R1", new Room("R1", "Room R1", 10, Set.of()));
        for (Event e:events){
            data.getEvents().put(e.getId(), e);
        }
        return data;
    }

    @Test
    void overlappingScheduledEventBlocksThoseStartsOnly() {
        //booked 13:00-14:00: starts 12:30, 13:00 and 13:30 overlap, 12:00 and 14:00 only touch
        SuggestionResult result = new SuggestionGenerator(dataWith(scheduled("EV-2", "R1", 13, 14)), NOBODY).generate(planned());
        assertEquals(3, result.getRejectedForRoom());
        assertEquals(24, result.getRejectedForAttendance());
    }

    @Test
    void backToBackBookingsAreBothRespectedAndTheEdgesStayFree() {
        //booked 13:00-14:00 and 14:00-15:00: starts 12:30 to 14:30 overlap, 12:00 and 15:00 are free
        CampusPlanData data=dataWith(scheduled("EV-2", "R1", 13, 14), scheduled("EV-3", "R1", 14, 15));
        SuggestionResult result=new SuggestionGenerator(data, NOBODY).generate(planned());
        assertEquals(5, result.getRejectedForRoom());
        assertEquals(22, result.getRejectedForAttendance());
    }
}