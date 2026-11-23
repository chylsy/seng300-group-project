package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import campusplan.error.ValidationException;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.EventStatus;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.domain.Student;
import campusplan.domain.StudentAvailability;
import campusplan.domain.TimeInterval;
import campusplan.service.EventPlanningService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventConfirmCancelTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);
    private static final TimeInterval SLOT = new TimeInterval(DAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

    private CampusPlanData data;
    private EventPlanningService service;
    private Event draft;

    @BeforeEach
    void setUp() {
        data = new CampusPlanData();
        for (String id : List.of("s1", "s2", "s3")) {
            data.getStudents().put(id, new Student(id, id, id + "@ucalgary.ca"));
            StudentAvailability availability = new StudentAvailability(id);
            availability.add(new TimeInterval(DAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
            data.getAvailability().put(id, availability);
        }
        data.getRooms().put("R1", new Room("R1", "Room 1", 10, Set.of(RoomFeature.PROJECTOR)));
        service = new EventPlanningService(data);
        draft = service.createDraft(new EventDraftRequest("Study group", List.of("s1", "s2", "s3"),
                DAY, DAY, 60, Set.of(RoomFeature.PROJECTOR), 2));
    }

    @Test
    void confirmSchedulesTheEvent() {
        service.confirm(draft.getId(), "R1", SLOT);
        assertEquals(EventStatus.SCHEDULED, draft.getStatus());
        assertEquals("R1", draft.getRoomId());
        assertEquals(SLOT, draft.getSlot());
    }

    private void assertStillDraft() {
        assertEquals(EventStatus.DRAFT, draft.getStatus());
        assertNull(draft.getRoomId());
        assertNull(draft.getSlot());
    }

    @Test
    void confirmFailsWhenRoomIsAlreadyBooked() {
        Event other = service.createDraft(new EventDraftRequest("Other", List.of("s1", "s2", "s3"),
                DAY, DAY, 60, Set.of(RoomFeature.PROJECTOR), 2));
        service.confirm(other.getId(), "R1", SLOT);   //someone else books the room first

        assertThrows(ValidationException.class, () -> service.confirm(draft.getId(), "R1", SLOT));
        assertStillDraft();
    }

    @Test
    void confirmFailsWhenEventIsNotADraft() {
        service.confirm(draft.getId(), "R1", SLOT);   //first confirm makes it SCHEDULED
        //a free, otherwise valid slot, so only the draft check can reject it
        TimeInterval later = new TimeInterval(DAY, LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertThrows(ValidationException.class, () -> service.confirm(draft.getId(), "R1", later));
        assertEquals(EventStatus.SCHEDULED, draft.getStatus());
        assertEquals("R1", draft.getRoomId());
        assertEquals(SLOT, draft.getSlot());
    }
}