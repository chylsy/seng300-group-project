package campusplan;

import campusplan.domain.*;
import campusplan.error.NotFoundException;
import campusplan.service.EventRosterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FR-07 — Event list, detail view, and roster CSV export.
 *
 * AC-07-01 to AC-07-13
 */
class EventRosterServiceTest {

    private static final LocalDate D1 = LocalDate.of(2027, 1, 10);
    private static final LocalDate D2 = LocalDate.of(2027, 1, 15);
    private static final LocalDate D3 = LocalDate.of(2027, 1, 20);

    private CampusPlanData data;
    private EventRosterService service;

    // Shared students
    private Student alice;
    private Student bob;
    private Student carol;

    @BeforeEach
    void setUp() {
        data = new CampusPlanData();
        service = new EventRosterService(data);

        alice = new Student("S-001", "Alice Smith", "alice@example.com");
        bob   = new Student("S-002", "Bob Jones",   "bob@example.com");
        carol = new Student("S-003", "Carol Lee",   "carol@example.com");

        data.getStudents().put(alice.getId(), alice);
        data.getStudents().put(bob.getId(),   bob);
        data.getStudents().put(carol.getId(), carol);
    }

    // ── Helper: build an event and put it into data ───────────────────────────

    private Event addEvent(String id, String title, LocalDate windowStart, LocalDate windowEnd,
                           List<String> inviteeIds) {
        EventDraftRequest req = new EventDraftRequest(
                title, inviteeIds, windowStart, windowEnd,
                60, Set.of(), 2);
        Event event = new Event(id, req);
        data.getEvents().put(id, event);
        return event;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-01  listAll returns empty list when no events exist
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void listAll_noEvents_returnsEmptyList() {
        assertTrue(service.listAll().isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-02  listAll sorts by window-start date then by ID
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void listAll_sortsByWindowStartThenId() {
        List<String> ids = List.of(alice.getId(), bob.getId());
        addEvent("EV-C", "Third",  D3, D3, ids);
        addEvent("EV-A", "First",  D1, D2, ids);
        addEvent("EV-B", "Second", D2, D3, ids);

        List<Event> result = service.listAll();
        assertEquals(List.of("EV-A", "EV-B", "EV-C"),
                result.stream().map(Event::getId).toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-03  listAll returns all statuses (DRAFT, SCHEDULED, CANCELLED)
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void listAll_includesAllStatuses() {
        List<String> ids = List.of(alice.getId(), bob.getId());
        Event draft = addEvent("EV-D", "Draft",     D1, D2, ids);
        Event sched = addEvent("EV-S", "Scheduled", D2, D3, ids);

        // Add a room so we can schedule EV-S
        Room room = new Room("R-001", "Lab", 10, Set.of());
        data.getRooms().put(room.getId(), room);
        sched.schedule(room.getId(), new TimeInterval(D2, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        Event cancelled = addEvent("EV-X", "Cancelled", D3, D3, ids);
        cancelled.cancel();

        List<Event> all = service.listAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(e -> e.getStatus() == EventStatus.DRAFT));
        assertTrue(all.stream().anyMatch(e -> e.getStatus() == EventStatus.SCHEDULED));
        assertTrue(all.stream().anyMatch(e -> e.getStatus() == EventStatus.CANCELLED));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-04  listByStatus filters correctly
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void listByStatus_filtersDraftOnly() {
        List<String> ids = List.of(alice.getId(), bob.getId());
        addEvent("EV-D", "Draft", D1, D2, ids);

        Event sched = addEvent("EV-S", "Sched", D2, D3, ids);
        Room room = new Room("R-001", "Lab", 10, Set.of());
        data.getRooms().put(room.getId(), room);
        sched.schedule(room.getId(), new TimeInterval(D2, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Event> drafts = service.listByStatus(EventStatus.DRAFT);
        assertEquals(1, drafts.size());
        assertEquals("EV-D", drafts.get(0).getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-05  listByStatus with null status throws NPE
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void listByStatus_nullStatus_throwsNPE() {
        assertThrows(NullPointerException.class, () -> service.listByStatus(null));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-06  get returns the correct event
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void get_existingEvent_returnsIt() {
        addEvent("EV-001", "Team Meeting", D1, D2, List.of(alice.getId(), bob.getId()));
        Event found = service.get("EV-001");
        assertEquals("Team Meeting", found.getTitle());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-07  get with unknown ID throws NotFoundException
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void get_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.get("EV-GHOST"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-08  exportRosterCsv starts with the required header
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_startsWithRequiredHeader() {
        addEvent("EV-001", "Sprint Retro", D1, D2, List.of(alice.getId(), bob.getId()));
        String csv = service.exportRosterCsv("EV-001");
        assertTrue(csv.startsWith("student_id,name,email,response\n"),
                "First line must be 'student_id,name,email,response'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-09  exportRosterCsv contains one row per invitee with correct data
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_containsOneRowPerInvitee() {
        addEvent("EV-001", "Sprint Retro", D1, D2, List.of(alice.getId(), bob.getId()));
        String csv = service.exportRosterCsv("EV-001");
        String[] lines = csv.split("\n");
        // header + 2 invitees = 3 lines
        assertEquals(3, lines.length);

        // Both invitee rows must be present (order is by student ID asc)
        String body = csv.substring(csv.indexOf('\n') + 1);
        assertTrue(body.contains("S-001"));
        assertTrue(body.contains("S-002"));
        assertTrue(body.contains("alice@example.com"));
        assertTrue(body.contains("bob@example.com"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-10  exportRosterCsv shows PENDING response by default
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_newEvent_responseIsPending() {
        addEvent("EV-001", "Kick-off", D1, D2, List.of(alice.getId(), bob.getId()));
        String csv = service.exportRosterCsv("EV-001");
        // Both invitees should be PENDING
        long pendingCount = csv.lines()
                .skip(1) // skip header
                .filter(line -> line.endsWith(",PENDING"))
                .count();
        assertEquals(2, pendingCount);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-11  exportRosterCsv quotes fields with commas
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_fieldWithComma_isQuoted() {
        // Student whose name contains a comma
        Student tricky = new Student("S-999", "Smith, Jr.", "jr@example.com");
        data.getStudents().put(tricky.getId(), tricky);
        addEvent("EV-002", "Review", D1, D2, List.of(alice.getId(), tricky.getId()));
        String csv = service.exportRosterCsv("EV-002");
        assertTrue(csv.contains("\"Smith, Jr.\""), "Name with comma must be quoted");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-12  exportRosterCsv quotes fields with double-quotes (and doubles them)
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_fieldWithDoubleQuote_isEscaped() {
        Student quoted = new Student("S-998", "O\"Brien", "obrien@example.com");
        data.getStudents().put(quoted.getId(), quoted);
        addEvent("EV-003", "Demo", D1, D2, List.of(alice.getId(), quoted.getId()));
        String csv = service.exportRosterCsv("EV-003");
        assertTrue(csv.contains("\"O\"\"Brien\""), "Double-quote in name must be escaped as \"\"");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-13  exportRosterCsv with unknown eventId throws NotFoundException
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_unknownEvent_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.exportRosterCsv("EV-GHOST"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-14  csvField helper: plain value passes through unchanged
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void csvField_plainValue_unchanged() {
        assertEquals("hello", EventRosterService.csvField("hello"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-15  csvField helper: null returns empty string
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void csvField_null_returnsEmpty() {
        assertEquals("", EventRosterService.csvField(null));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AC-07-16  exportRosterCsv quotes fields with embedded newlines
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    void exportRosterCsv_fieldWithNewline_isQuoted() {
        Student nl = new Student("S-997", "Line\nBreak", "lb@example.com");
        data.getStudents().put(nl.getId(), nl);
        addEvent("EV-004", "NL Test", D1, D2, List.of(alice.getId(), nl.getId()));
        String csv = service.exportRosterCsv("EV-004");
        assertTrue(csv.contains("\"Line\nBreak\""), "Name with newline must be quoted");
    }
}
