package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.RoomFeature;
import campusplan.error.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventTest {
    //a ready-made draft event with invitees s1 and s2
    private static Event draft() {
        EventDraftRequest request = new EventDraftRequest("Study group", List.of("s1", "s2"),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), 60,
                Set.of(RoomFeature.PROJECTOR), 2);
        return new Event("EV-001", request);
    }

    @Test
    void aNewEventIsADraft() {
        assertTrue(draft().isDraft());
    }

    @Test
    void everyInviteeStartsPending() {
        Event event = draft();
        assertEquals(Event.PENDING, event.responseOf("s1"));
        assertEquals(Event.PENDING, event.responseOf("s2"));
    }

    @Test
    void responseOfUninvitedStudentThrows() {
        NotFoundException e = assertThrows(NotFoundException.class, () -> draft().responseOf("s9"));
        assertEquals("Student not found: s9", e.getMessage());
    }
}