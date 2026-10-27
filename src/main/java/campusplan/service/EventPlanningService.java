package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.EventStatus;
import campusplan.domain.IdGenerator;
import campusplan.domain.UuidIdGenerator;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Creates, edits and reads draft events. Every rule is checked before any data changes,
 * so a failed call leaves existing records exactly as they were.
 */
public class EventPlanningService {
    private static final Comparator<Event> BY_ID = Comparator.comparing(Event::getId);
    private final CampusPlanData data;
    private final IdGenerator idGenerator;

    //normal use: ids come from the team's UUID generator
    public EventPlanningService(CampusPlanData data) {
        this(data, new UuidIdGenerator());
    }

    //tests can pass a fake generator to get predictable ids
    public EventPlanningService(CampusPlanData data, IdGenerator idGenerator) {
        this.data = Objects.requireNonNull(data, "data");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    //makes a new DRAFT event, or throws if any rule E1-E7 fails
    public Event createDraft(EventDraftRequest request) {
        validate(request);              //check everything first
        String id = idGenerator.next("event");
        Event event = new Event(id, request);
        data.getEvents().put(id, event); //change data last
        return event;
    }

    //replaces the details of an existing DRAFT, or throws and changes nothing
    public Event editDraft(String id, EventDraftRequest request) {
        Event event = get(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ValidationException("status", "Only DRAFT events can be edited");
        }
        validate(request);
        event.applyDraft(request);
        return event;
    }

    //the event with this id, or NotFoundException
    public Event get(String id) {
        Event event = data.getEvents().get(id);
        if (event == null) {
            throw new NotFoundException("Event", id);
        }
        return event;
    }

    //all events sorted by id, as a new list each time; empty is fine
    public List<Event> listAll() {
        List<Event> events = new ArrayList<>(data.getEvents().values());
        events.sort(BY_ID);
        return events;
    }

    //checks rules E1-E7 without changing anything
    private void validate(EventDraftRequest r) {
        if (r == null) {
            throw new ValidationException("request", "Request is required");
        }
        //E1 title
        if (r.getTitle() == null || r.getTitle().trim().isEmpty() || r.getTitle().trim().length() > 100) {
            throw new ValidationException("title", "Title must be 1-100 characters");
        }
        //E2 invitee count
        List<String> ids = r.getInviteeIds();
        if (ids.size() < 2 || ids.size() > 20) {
            throw new ValidationException("inviteeIds", "Invite between 2 and 20 students");
        }
        //E3 no duplicates
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ValidationException("inviteeIds", "A student cannot be invited twice");
        }
        //E4 window of 1-7 days
        if (r.getWindowStart() == null || r.getWindowEnd() == null) {
            throw new ValidationException("window", "Start and end dates are required");
        }
        long days = ChronoUnit.DAYS.between(r.getWindowStart(), r.getWindowEnd()) + 1;
        if (days < 1 || days > 7) {
            throw new ValidationException("window", "Window must be 1-7 consecutive days");
        }
        //E5 duration
        int minutes = r.getDurationMinutes();
        if (minutes < 30 || minutes > 180 || minutes % 30 != 0) {
            throw new ValidationException("durationMinutes", "Duration must be 30-180 minutes in 30-minute steps");
        }
        //E6 minimum attendance
        if (r.getMinAttendance() < 2 || r.getMinAttendance() > ids.size()) {
            throw new ValidationException("minAttendance", "Minimum attendance must be 2 up to the number invited");
        }
        //E7 every invitee exists
        for (String studentId : ids) {
            if (!data.getStudents().containsKey(studentId)) {
                throw new NotFoundException("Student", studentId);
            }
        }
    }
}