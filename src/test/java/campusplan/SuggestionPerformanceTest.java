package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

//NFR-03: 20 invitees, 20 rooms, a seven-day window and 100 existing events.
class SuggestionPerformanceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Test
    void generationFinishesWithinTwoSecondsOverTenRuns() {
        CampusPlanData data = new CampusPlanData();
        for (int r=1; r<=20; r++) {
            String id=String.format("R%02d",r);
            data.getRooms().put(id,new Room(id, "Room "+id, 30, Set.of()));
        }
        for (int i=0; i<100; i++) {
            Event existing = new Event("EV-" + (i+2), new EventDraftRequest("Existing " + i,List.of("x1", "x2"), DAY, DAY, 60, Set.of(), 2));
            LocalDate day=DAY.plusDays(i%7);
            int hour=8 +(i %12);
            existing.schedule(String.format("R%02d",(i % 20) +1),new TimeInterval(day, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0)));
            data.getEvents().put(existing.getId(), existing);
        }
        List<String> invitees=new ArrayList<>();
        for (int s=1; s <=20;s++){
            invitees.add("s" + s);
        }
        Event planned=new Event("EV-1", new EventDraftRequest("Big meeting", invitees, DAY, DAY.plusDays(6), 60, Set.of(), 10));
        EligibilityRule everyone =(event, studentId, slot) ->null;
        SuggestionGenerator generator=new SuggestionGenerator(data, everyone);

        generator.generate(planned);

        long slowestMillis=0;
        for (int run=0; run<10; run++) {
            long begin=System.nanoTime();
            SuggestionResult result = generator.generate(planned);
            long millis=(System.nanoTime() - begin) / 1_000_000;
            slowestMillis=Math.max(slowestMillis, millis);
            assertEquals(5,result.getTop().size());
        }
        assertTrue(slowestMillis<2000, "slowest run took " +slowestMillis +" ms");
    }
}