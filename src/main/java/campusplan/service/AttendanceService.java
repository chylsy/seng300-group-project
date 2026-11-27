package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;

public class AttendanceService {

    private final CampusPlanData data;

    public AttendanceService(CampusPlanData data) {
        this.data = data;
    }

    /**
     * Record a response for a student.
     */
    public void recordResponse(
            String eventId,
            String studentId,
            String response) {

        Event event = getEvent(eventId);

        // verify student is invited

        // verify response is valid

        // if ACCEPTED:
        //     check availability
        //     check for overlapping accepted events

        // save response
    }

    /**
     * Return all responses for an event.
     */
    public Object getResponses(String eventId) {
        Event event = getEvent(eventId);

        // return response collection
        return null;
    }

    /**
     * Count accepted responses.
     */
    public int acceptedCount(String eventId) {

        Event event = getEvent(eventId);

        // count ACCEPTED responses

        return 0;
    }

    /**
     * Find an event or throw an exception.
     */
    private Event getEvent(String eventId) {

        // look up event

        return null;
    }

    /**
     * Verify response is one of:
     * PENDING, ACCEPTED, DECLINED
     */
    private void validateResponse(String response) {

        // validate response value
    }

    /**
     * ACCEPTED-specific validation.
     */
    private void validateAcceptedResponse(
            Event event,
            String studentId) {

        // verify event is scheduled

        // verify availability

        // verify no conflicts
    }

    /**
     * Check student's availability.
     */
    private void validateAvailability(
            String studentId,
            Object slot) {

        // availability logic
    }

    /**
     * Check overlapping accepted events.
     */
    private void validateNoConflict(
            Event event,
            String studentId,
            Object slot) {

        // conflict logic
    }
}