package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.StudentAvailability;
import campusplan.domain.TimeInterval;
import campusplan.domain.EventStatus;
import java.util.Objects;

//the spec's eligibility rule: free at that time, and no accepted event that overlaps
public class AvailabilityEligibilityRule implements EligibilityRule {
    private final CampusPlanData data;

    public AvailabilityEligibilityRule(CampusPlanData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    //null means the student can attend, otherwise a short reason why not
    @Override
    public String ineligibleReason(Event event, String studentId, TimeInterval slot) {
        StudentAvailability saved = data.getAvailability().get(studentId);
        if (saved == null || !saved.covers(slot)) {
            return "not available at that time";
        }
        for (Event other : data.getEvents().values()) {
            boolean scheduled = other.getStatus() == EventStatus.SCHEDULED;
            boolean invited = other.getInviteeIds().contains(studentId);
            if (scheduled && invited && other != event
                    && Event.ACCEPTED.equals(other.responseOf(studentId))
                    && slot.overlaps(other.getSlot())) {
                return "already accepted another event at that time";
            }
        }
        return null;
    }
}