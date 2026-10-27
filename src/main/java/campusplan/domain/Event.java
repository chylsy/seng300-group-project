package campusplan.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//one planned event; starts as a DRAFT and is changed only through the service
public class Event {
    public static final String PENDING = "PENDING"; //every invitee starts here

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

    //independent copy, used by CampusPlanData.deepCopy(); changing the copy never changes the original
    public Event(Event other) {
        this.id = other.id;
        this.title = other.title;
        this.inviteeIds = List.copyOf(other.inviteeIds);
        this.windowStart = other.windowStart;
        this.windowEnd = other.windowEnd;
        this.durationMinutes = other.durationMinutes;
        this.requiredFeatures = Set.copyOf(other.requiredFeatures);
        this.minAttendance = other.minAttendance;
        this.status = other.status;
        this.responses.putAll(other.responses); //new map with the same entries
        this.roomId = other.roomId;
        this.slot = other.slot;
    }

    //replaces the planning details with the request's values, id and status stay the same
    public void applyDraft(EventDraftRequest request) {
        this.title = request.getTitle().trim();
        this.inviteeIds = request.getInviteeIds();
        //students who stay keep their answer, removed students lose theirs, new students start PENDING
        responses.keySet().retainAll(inviteeIds);
        for (String studentId : inviteeIds) {
            responses.putIfAbsent(studentId, PENDING);
        }
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