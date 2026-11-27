package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventStatus;
import campusplan.domain.StudentAvailability;
import campusplan.domain.TimeInterval;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;

import java.util.Map;
import java.util.Objects;

/**
 * Handles attendance responses for scheduled events
 *
 * FR-06:
 * - Records PENDING, ACCEPTED and DECLINED responses
 * - Enforces availability requirements for ACCEPTED responses
 * - Prevents accepting overlapping scheduled events
 * - Provides access to stored responses and attendance counts
 */
public class AttendanceService {

    private final CampusPlanData data;

    /**
     * Uses the shared application data store
     */
    public AttendanceService(CampusPlanData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    /**
     * Records a response for an invited student
     */
    public void recordResponse(
            String eventId,
            String studentId,
            String response) {

        Event event = getEvent(eventId);

        // only invited students may respond
        if (!event.getInviteeIds().contains(studentId)) {
            throw new NotFoundException("Student", studentId);
        }

        validateResponse(response);

        // ACCEPTED has additional requirements
        if (Event.ACCEPTED.equals(response)) {
            validateAcceptedResponse(event, studentId);
        }

        // save the response after every validation succeeds
        event.getResponses().put(studentId, response);
    }

    /**
     * Returns all responses for an event
     */
    public Map<String, String> getResponses(String eventId) {
        return getEvent(eventId).getResponses();
    }

    /**
     * Counts ACCEPTED responses
     */
    public int acceptedCount(String eventId) {

        int count = 0;

        for (String response : getResponses(eventId).values()) {

            if (Event.ACCEPTED.equals(response)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Retrieves the event with the supplied ID
     *
     * Throws NotFoundException if the event does not exist
     */
    private Event getEvent(String eventId) {

        Event event = data.getEvents().get(eventId);

        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        return event;
    }


    /**
     * Ensures that the supplied response is one of the
     * supported FR-06 response values
     */
    private void validateResponse(String response) {

        boolean valid =
                Event.PENDING.equals(response)
                        || Event.ACCEPTED.equals(response)
                        || Event.DECLINED.equals(response);

        if (!valid) {
            throw new ValidationException(
                    "response",
                    "Response must be PENDING, ACCEPTED or DECLINED");
        }
    }

    /**
     * Performs all checks required before a student
     * may ACCEPT an event invitation
     */
    private void validateAcceptedResponse(
            Event event,
            String studentId) {

        if (event.getStatus() != EventStatus.SCHEDULED) {

            throw new ValidationException(
                    "status",
                    "Only scheduled events can be accepted");
        }

        TimeInterval slot = event.getSlot();

        validateAvailability(studentId, slot);

        validateNoConflict(event, studentId, slot);
    }

    /**
     * Confirms that the student's saved availability
     * completely covers the event interval
     */
    private void validateAvailability(
            String studentId,
            TimeInterval slot) {

        StudentAvailability saved =
                data.getAvailability().get(studentId);

        if (saved == null || !saved.covers(slot)) {

            throw new ValidationException(
                    "response",
                    "Student is not available for this event");
        }
    }

    /**
     * Ensures the student has not already ACCEPTED
     * another scheduled event that overlaps this one
     */
    private void validateNoConflict(
            Event currentEvent,
            String studentId,
            TimeInterval slot) {

        for (Event other : data.getEvents().values()) {

            // ignore the event currently being updated
            if (other == currentEvent) {
                continue;
            }

            boolean scheduled =
                    other.getStatus() == EventStatus.SCHEDULED;

            boolean invited =
                    other.getInviteeIds().contains(studentId);

            boolean accepted =
                    invited
                            && Event.ACCEPTED.equals(
                            other.responseOf(studentId));

            if (scheduled
                    && accepted
                    && slot.overlaps(other.getSlot())) {

                throw new ValidationException(
                        "response",
                        "Student already accepted an overlapping event");
            }
        }
    }
}