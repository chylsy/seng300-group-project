package campusplan.service;
import campusplan.domain.Event;
import campusplan.domain.TimeInterval;

public interface EligibilityRule {
    //returns null if the student can attend, otherwise a short human-readable reason
    String ineligibleReason(Event event, String studentId, TimeInterval slot);
}