package campusplan;

import campusplan.command.CommandHistory;
import campusplan.domain.*;
import campusplan.error.DuplicateEmailException;
import campusplan.persistence.JsonDataStore;
import campusplan.service.EventPlanningService;
import campusplan.service.StudentService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CommandHistoryTest {
    private final CampusPlanData data = new CampusPlanData();
    private final CommandHistory history = new CommandHistory(data);
    private final StudentService students = new StudentService(data, new UuidIdGenerator());

    private String create(int n) {
        return history.run("Create student",
                () -> students.create("Student " + n, n + "@example.ca")).getId();
    }

    @Test
    void retainsLatestTwentyAndReplaysOriginalIds() {
        assertFalse(history.undo());
        assertFalse(history.redo());
        String first = create(0);
        String[] ids = new String[20];
        for (int i = 0; i < ids.length; i++) ids[i] = create(i + 1);
        for (int i = ids.length - 1; i >= 0; i--) {
            assertTrue(history.undo());
            assertFalse(data.getStudents().containsKey(ids[i]));
        }
        assertFalse(history.undo());
        assertTrue(data.getStudents().containsKey(first));
        for (String id : ids) {
            assertTrue(history.redo());
            assertTrue(data.getStudents().containsKey(id));
        }
        assertFalse(history.redo());
    }

    @Test
    void failurePreservesRedoAndNewEditClearsIt() {
        String first = create(1);
        String second = create(2);
        history.undo();
        assertThrows(DuplicateEmailException.class, () -> create(1));
        assertTrue(history.redo());
        assertTrue(data.getStudents().containsKey(second));
        history.undo();
        history.run("Rename", () -> students.edit(first, "New name", "1@example.ca"));
        assertFalse(history.redo());
        history.undo();
        assertEquals("Student 1", students.get(first).getName());
    }

    @Test
    void partialFailureRestoresRelatedData() {
        String id = create(1);
        assertThrows(IllegalStateException.class, () -> history.run("Fail mid-change", () -> {
            data.getStudents().clear();
            data.getAvailability().clear();
            throw new IllegalStateException("Injected failure");
        }));
        assertTrue(data.getStudents().containsKey(id));
        assertTrue(history.undo());
        assertTrue(data.isEmpty());
    }

    @Test
    void undoConfirmReleasesReservationAndRedoRestoresIt() {
        CampusPlanData sample = new JsonDataStore(Path.of("sample-data/seed.json")).load();
        EventPlanningService events = new EventPlanningService(sample);
        CommandHistory commands = new CommandHistory(sample);
        TimeInterval slot = new TimeInterval(LocalDate.of(2026, 11, 2),
                LocalTime.of(10, 0), LocalTime.NOON);
        commands.run("Confirm", () -> events.confirm("EV-001", "R002", slot));
        assertTrue(commands.undo());
        assertEquals(EventStatus.DRAFT, events.get("EV-001").getStatus());
        assertNull(events.get("EV-001").getRoomId());
        assertNull(events.get("EV-001").getSlot());
        assertTrue(commands.redo());
        assertEquals("R002", events.get("EV-001").getRoomId());
        assertEquals(slot, events.get("EV-001").getSlot());
    }

    @Test
    void saveAfterUndoRestoresResponsesAndBooking(@TempDir Path dir) {
        CampusPlanData sample = new JsonDataStore(Path.of("sample-data/p2-baseline.json")).load();
        CommandHistory commands = new CommandHistory(sample);
        EventPlanningService events = new EventPlanningService(sample);
        Event original = events.get("EV-001");
        commands.run("Cancel", () -> events.cancel("EV-001", true));
        assertNull(events.get("EV-001").getRoomId());
        assertTrue(commands.undo());
        JsonDataStore store = new JsonDataStore(dir.resolve("saved.json"));
        store.save(sample);
        CampusPlanData restarted = store.load();
        Event saved = restarted.getEvents().get("EV-001");
        assertEquals(EventStatus.SCHEDULED, saved.getStatus());
        assertEquals("R002", saved.getRoomId());
        assertEquals("10:00", saved.getSlot().getStart().toString());
        assertEquals(Event.ACCEPTED, saved.responseOf("S001"));
        assertEquals(Event.DECLINED, saved.responseOf("S002"));
        assertEquals(Event.PENDING, saved.responseOf("S003"));
        assertEquals(original.getResponses(), saved.getResponses());
        assertFalse(new CommandHistory(restarted).undo());
        assertTrue(commands.redo());
        store.save(sample);
        assertEquals(EventStatus.CANCELLED, store.load().getEvents().get("EV-001").getStatus());
    }
}
