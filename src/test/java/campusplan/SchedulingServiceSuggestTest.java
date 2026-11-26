package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.EventStatus;
import campusplan.domain.Room;
import campusplan.error.NotFoundException;
import campusplan.service.SchedulingService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

//NFR-02: a failed or read only call leaves the records exactly as they were
class SchedulingServiceSuggestTest {
    private static final LocalDate DAY=LocalDate.of(2026, 11, 2);
    private static Event draft(){
        return new Event("EV-001", new EventDraftRequest("Study night", List.of("s1", "s2", "s3"), DAY, DAY, 60, Set.of(), 2));
    }

    private static CampusPlanData dataWith(Event event) {
        CampusPlanData data=new CampusPlanData();
        data.getRooms().put("R1",new Room("R1", "Room 1", 10, Set.of()));
        data.getEvents().put(event.getId(), event);
        return data;
    }

    @Test
    void unknownOrMissingEventIdIsRejected() {
        SchedulingService service = new SchedulingService(dataWith(draft()), (event, id, slot) -> null);
        assertThrows(NotFoundException.class,()-> service.suggest("EV-999"));
        assertThrows(NotFoundException.class,() -> service.suggest(null));
    }

    @Test
    void suggestingChangesNothing() {
        Event event=draft();
        CampusPlanData data = dataWith(event);
        new SchedulingService(data, (e, id, slot) -> null).suggest("EV-001");
        assertEquals(EventStatus.DRAFT, event.getStatus());
        assertNull(event.getRoomId());
        assertNull(event.getSlot());
        assertEquals(1, data.getEvents().size());
    }
}