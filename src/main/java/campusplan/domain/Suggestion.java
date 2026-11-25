package campusplan.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//One ranked option for an event: a room, a time, who can attend and why the others cannot (FR-04)

public final class Suggestion {
    private final String roomId;
    private final TimeInterval slot;
    private final List<String> eligibleIds;
    private final Map<String, String> ineligibleReasons;

    public Suggestion(String roomId, TimeInterval slot, List<String> eligibleIds,Map<String, String> ineligibleReasons) {
        Validation.require(roomId!=null&& !roomId.isBlank(), "roomId", "A room is required.");
        Validation.require(slot!=null, "slot", "A time slot is required.");
        Validation.require(eligibleIds != null, "eligibleIds", "The eligible list is required.");
        Validation.require(ineligibleReasons !=null, "ineligibleReasons", "The ineligible reasons are required.");
        this.roomId=roomId;
        this.slot =slot;
        this.eligibleIds=List.copyOf(eligibleIds);
        this.ineligibleReasons=Collections.unmodifiableMap(new LinkedHashMap<>(ineligibleReasons));
    }

    public String getRoomId() {
        return roomId;
    }
    public TimeInterval getSlot() {
        return slot;
    }
    public List<String> getEligibleIds() {
        return eligibleIds;
    }
    public Map<String, String> getIneligibleReasons() {
        return ineligibleReasons;
    }

    public int eligibleCount() {
        return eligibleIds.size();
    }
}