package campusplan.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

//what the organizer submits to create or edit a draft; read-only once built
public final class EventDraftRequest {
    private final String title;
    private final List<String> inviteeIds;
    private final LocalDate windowStart;
    private final LocalDate windowEnd;
    private final int durationMinutes;
    private final Set<RoomFeature> requiredFeatures;
    private final int minAttendance;

    public EventDraftRequest(String title, List<String> inviteeIds, LocalDate windowStart,
                             LocalDate windowEnd, int durationMinutes,
                             Set<RoomFeature> requiredFeatures, int minAttendance) {
        this.title = title;
        //copies, so changing the caller's lists later cannot change this request
        this.inviteeIds = inviteeIds == null ? List.of() : List.copyOf(inviteeIds);
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.durationMinutes = durationMinutes;
        this.requiredFeatures = requiredFeatures == null ? Set.of() : Set.copyOf(requiredFeatures);
        this.minAttendance = minAttendance;
    }

    public String getTitle() { return title; }
    public List<String> getInviteeIds() { return inviteeIds; }
    public LocalDate getWindowStart() { return windowStart; }
    public LocalDate getWindowEnd() { return windowEnd; }
    public int getDurationMinutes() { return durationMinutes; }
    public Set<RoomFeature> getRequiredFeatures() { return requiredFeatures; }
    public int getMinAttendance() { return minAttendance; }
}