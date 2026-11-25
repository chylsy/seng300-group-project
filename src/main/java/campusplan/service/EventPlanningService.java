package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventDraftRequest;
import campusplan.domain.EventStatus;
import campusplan.domain.IdGenerator;
import campusplan.domain.UuidIdGenerator;
import campusplan.domain.Room;
import campusplan.domain.TimeInterval;
import campusplan.domain.StudentAvailability;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

/**
 * Creates, edits and reads draft events. Every rule is checked before any data changes,
 * so a failed call leaves existing records exactly as they were.
 */
public class EventPlanningService {
    private static final Comparator<Event> BY_ID = Comparator.comparing(Event::getId);
    private final CampusPlanData data;
    private final IdGenerator idGenerator;
    private final EligibilityRule rule;

    //normal use: ids come from the team's UUID generator
    public EventPlanningService(CampusPlanData data) {
        this(data, new UuidIdGenerator());
    }

    //tests can pass a fake generator to get predictable ids
    public EventPlanningService(CampusPlanData data, IdGenerator idGenerator) {
        this.data = Objects.requireNonNull(data, "data");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.rule = new AvailabilityEligibilityRule(data);
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

    //schedules a draft event in this room at this time, or throws and changes nothing
    public Event confirm(String eventId, String roomId, TimeInterval slot) {
        Event event = get(eventId);
        requireDraft(event);
        Room room = requireRoom(roomId);
        requireSlot(slot);
        requireSlotMatchesDuration(event, slot);
        requireSlotInWindow(event, slot);
        requireSlotInOpeningHours(slot);
        requireRoomHoldsInvitees(event, room);
        requireRoomHasFeatures(event, room);
        requireRoomFree(room, slot);
        requireEnoughEligible(event, slot);
        event.schedule(roomId, slot);   //the only change, at the very end
        return event;
    }

    //cancels a scheduled event and frees its room, or throws and changes nothing
    public Event cancel(String eventId, boolean confirmed) {
        Event event = get(eventId);
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw new ValidationException("status", "Only SCHEDULED events can be cancelled");
        }
        if (!confirmed) {
            throw new ValidationException("confirmed", "Cancelling needs confirmation");
        }
        event.cancel();
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

    //confirm only works on a draft
    private void requireDraft(Event event) {
        if (!event.isDraft()) {
            throw new ValidationException("status", "Only DRAFT events can be confirmed");
        }
    }

    //the room with this id, or NotFoundException
    private Room requireRoom(String roomId) {
        Room room = data.getRooms().get(roomId);
        if (room == null) {
            throw new NotFoundException("Room", roomId);
        }
        return room;
    }

    //the slot must be exactly as long as the event needs
    private void requireSlotMatchesDuration(Event event, TimeInterval slot) {
        if (slot.minutes() != event.getDurationMinutes()) {
            throw new ValidationException("slot", "Slot length must match the event duration");
        }
    }

    //the slot's day must fall inside the event's date window
    private void requireSlotInWindow(Event event, TimeInterval slot) {
        LocalDate day = slot.getDate();
        if (day.isBefore(event.getWindowStart()) || day.isAfter(event.getWindowEnd())) {
            throw new ValidationException("slot", "Slot must be inside the event's date window");
        }
    }

    //the slot must start and end inside the opening hours
    private void requireSlotInOpeningHours(TimeInterval slot) {
        if (slot.getStart().isBefore(Room.OPEN) || slot.getEnd().isAfter(Room.CLOSE)) {
            throw new ValidationException("slot", "Slot must be within opening hours, 8:00 to 22:00");
        }
    }

    //the room must have space for every invitee
    private void requireRoomHoldsInvitees(Event event, Room room) {
        if (!room.canHold(event.getInviteeIds().size())) {
            throw new ValidationException("roomId", "Room is too small for the invitees");
        }
    }

    //the room must have every feature the event requires
    private void requireRoomHasFeatures(Event event, Room room) {
        if (!room.hasAll(event.getRequiredFeatures())) {
            throw new ValidationException("roomId", "Room is missing a required feature");
        }
    }

    //no other scheduled event may use this room at an overlapping time
    private void requireRoomFree(Room room, TimeInterval slot) {
        for (Event other : data.getEvents().values()) {
            boolean sameRoom = room.getId().equals(other.getRoomId());
            boolean scheduled = other.getStatus() == EventStatus.SCHEDULED;
            if (scheduled && sameRoom && slot.overlaps(other.getSlot())) {
                throw new ValidationException("roomId", "Room is already booked at that time");
            }
        }
    }

    //true if the student's saved availability fully covers the slot
    private boolean isAvailable(String studentId, TimeInterval slot) {
        StudentAvailability saved = data.getAvailability().get(studentId);
        return saved != null && saved.covers(slot);
    }

    //true if the student ACCEPTED another scheduled event that overlaps this slot
    private boolean hasAcceptedConflict(String studentId, TimeInterval slot) {
        for (Event other : data.getEvents().values()) {
            boolean scheduled = other.getStatus() == EventStatus.SCHEDULED;
            boolean invited = other.getInviteeIds().contains(studentId);
            if (scheduled && invited
                    && Event.ACCEPTED.equals(other.responseOf(studentId))
                    && slot.overlaps(other.getSlot())) {
                return true;
            }
        }
        return false;
    }

    //at least minAttendance invitees must pass the shared eligibility rule
    private void requireEnoughEligible(Event event, TimeInterval slot) {
        int eligible = 0;
        for (String studentId : event.getInviteeIds()) {
            if (rule.ineligibleReason(event, studentId, slot) == null) {
                eligible++;
            }
        }
        if (eligible < event.getMinAttendance()) {
            throw new ValidationException("slot", "Not enough eligible invitees for this time");
        }
    }

    //a slot must be given
    private void requireSlot(TimeInterval slot) {
        if (slot == null) {
            throw new ValidationException("slot", "Slot is required");
        }
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