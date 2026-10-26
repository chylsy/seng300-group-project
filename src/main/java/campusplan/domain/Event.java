package campusplan.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//one planned event; starts as a DRAFT and is changed only through the service
public class Event {
    private final String id;               //never changes after creation
    private String title;
    private List<String> inviteeIds;
    private LocalDate windowStart;
    private LocalDate windowEnd;
    private int durationMinutes;
    private Set<RoomFeature> requiredFeatures;
    private int minAttendance;
    private EventStatus status;
    private final Map<String, String> responses = new HashMap<>(); //studentId -> answer, empty for now
    private String roomId;                 //null until scheduling picks a room
    private TimeInterval slot;             //null until scheduling picks a time

    //builds a new DRAFT from an already-validated request
    public Event(String id, EventDraftRequest request) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = EventStatus.DRAFT;
        applyDraft(request);
    }

    //replaces the planning details with the request's values, id and status stay the same
    public void applyDraft(EventDraftRequest request) {
        this.title = request.getTitle().trim();
        this.inviteeIds = request.getInviteeIds();
        this.windowStart = request.getWindowStart();
        this.windowEnd = request.getWindowEnd();
        this.durationMinutes = request.getDurationMinutes();
        this.requiredFeatures = request.getRequiredFeatures();
        this.minAttendance = request.getMinAttendance();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getInviteeIds() { return inviteeIds; }
    public LocalDate getWindowStart() { return windowStart; }
    public LocalDate getWindowEnd() { return windowEnd; }
    public int getDurationMinutes() { return durationMinutes; }
    public Set<RoomFeature> getRequiredFeatures() { return requiredFeatures; }
    public int getMinAttendance() { return minAttendance; }
    public EventStatus getStatus() { return status; }
    public Map<String, String> getResponses() { return responses; }
    public String getRoomId() { return roomId; }
    public TimeInterval getSlot() { return slot; }
}