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

public class AttendanceService {

    private final CampusPlanData data;

    public AttendanceService(CampusPlanData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    /**
     * Records a response for an invited student.
     */
    public void recordResponse(
            String eventId,
            String studentId,
            String response) {

        Event event = getEvent(eventId);

        if (!event.getInviteeIds().contains(studentId)) {
            throw new NotFoundException("Student", studentId);
        }

        validateResponse(response);

        if (Event.ACCEPTED.equals(response)) {
            validateAcceptedResponse(event, studentId);
        }

        event.getResponses().put(studentId, response);
    }

    /**
     * Returns all responses for an event.
     */
    public Map<String, String> getResponses(String eventId) {
        return getEvent(eventId).getResponses();
    }

    /**
     * Counts ACCEPTED responses.
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

    private Event getEvent(String eventId) {

        Event event = data.getEvents().get(eventId);

        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        return event;
    }

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

    private void validateNoConflict(
            Event currentEvent,
            String studentId,
            TimeInterval slot) {

        for (Event other : data.getEvents().values()) {

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