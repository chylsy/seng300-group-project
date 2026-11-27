package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventStatus;
import campusplan.domain.Student;
import campusplan.error.NotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FR-07: Event list, detail view, and roster CSV export.
 *
 * <p>Lists events sorted by window-start date then by id. For a chosen event it
 * produces a UTF-8 CSV roster whose header is:
 * <pre>student_id,name,email,response</pre>
 * Fields containing a comma, a double-quote, or a line break are wrapped in
 * double-quotes and any embedded double-quote is doubled ("").
 */
public class EventRosterService {

    /** Column header for the roster CSV, as required by FR-07. */
    public static final String CSV_HEADER = "student_id,name,email,response";

    private static final Comparator<Event> BY_DATE_THEN_ID =
            Comparator.comparing(Event::getWindowStart)
                    .thenComparing(Event::getId);

    private final CampusPlanData data;

    public EventRosterService(CampusPlanData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    // ── listing ──────────────────────────────────────────────────────────────

    /**
     * Returns all events sorted by window-start date, then by ID.
     * An empty list is returned when no events exist.
     */
    public List<Event> listAll() {
        List<Event> events = new ArrayList<>(data.getEvents().values());
        events.sort(BY_DATE_THEN_ID);
        return events;
    }

    /**
     * Returns all events whose status equals {@code status},
     * sorted by window-start date then by ID.
     *
     * @throws IllegalArgumentException if {@code status} is null
     */
    public List<Event> listByStatus(EventStatus status) {
        Objects.requireNonNull(status, "status");
        List<Event> result = new ArrayList<>();
        for (Event e : data.getEvents().values()) {
            if (e.getStatus() == status) {
                result.add(e);
            }
        }
        result.sort(BY_DATE_THEN_ID);
        return result;
    }

    /**
     * Returns the event with this ID.
     *
     * @throws NotFoundException if no event with that ID exists
     */
    public Event get(String eventId) {
        Event event = data.getEvents().get(eventId);
        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }
        return event;
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    /**
     * Builds a UTF-8 CSV roster for the event with the given ID.
     *
     * <p>The first line is always {@value #CSV_HEADER}. Each subsequent line
     * holds one invitee row: student_id, name, email, and that student's
     * current response. Rows are ordered by student ID.
     *
     * <p>Fields are quoted when they contain a comma, a double-quote, or a
     * newline character (CR or LF). Embedded double-quotes are doubled.
     * Lines are separated by {@code \n} (LF).
     *
     * @param eventId the ID of the event whose roster to export
     * @return the complete CSV text (never null, never empty)
     * @throws NotFoundException if the event or any invitee student is missing
     */
    public String exportRosterCsv(String eventId) {
        Event event = get(eventId);
        Map<String, Student> students = data.getStudents();
        Map<String, String> responses = event.getResponses();

        // Sort invitees by student ID for a stable, predictable output order.
        List<String> inviteeIds = new ArrayList<>(event.getInviteeIds());
        inviteeIds.sort(Comparator.naturalOrder());

        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append('\n');

        for (String studentId : inviteeIds) {
            Student student = students.get(studentId);
            if (student == null) {
                throw new NotFoundException("Student", studentId);
            }
            String response = responses.getOrDefault(studentId, Event.PENDING);

            sb.append(csvField(student.getId()))
              .append(',')
              .append(csvField(student.getName()))
              .append(',')
              .append(csvField(student.getEmail()))
              .append(',')
              .append(csvField(response))
              .append('\n');
        }

        return sb.toString();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Quotes a CSV field if it contains a comma, double-quote, CR, or LF.
     * Embedded double-quotes are doubled per RFC 4180.
     * Public so it can be unit-tested directly.
     */
    public static String csvField(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting =
                value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (!needsQuoting) {
            return value;
        }
        // Wrap in quotes and double any embedded quote character.
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
