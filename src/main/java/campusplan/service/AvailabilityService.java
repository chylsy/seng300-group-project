package campusplan.service;

import campusplan.domain.*;
import java.util.*;

public class AvailabilityService {

    private final CampusPlanData data;

    public AvailabilityService(CampusPlanData data) {
        this.data = Objects.requireNonNull(data, "Data cannot be null.");
    }

    /**
     * Add an availability interval for a student
     * @param studentId - Student's Identification Number (ID)
     * @param interval  - Interval to add
     */
    public void addAvailability(String studentId, TimeInterval interval) {

        StudentAvailability availability = getAvailability(studentId);
        availability.add(interval);
    }

    /**
     * Remove an availability interval for a student
     * @param studentId - Student Identification Number (ID)
     * @param interval  - Interval to remove
     */
    public void removeAvailability(String studentId, TimeInterval interval) {

        StudentAvailability availability = getAvailability(studentId);
        availability.remove(interval);
    }

    /**
     * Retrieves a student's availability
     * @param studentId - Student's Identification Number (ID)
     * @return          - Student's availability
     */
    public StudentAvailability getAvailability(String studentId) {

        StudentAvailability availability = data.getStudentAvailability(studentId);

        if (availability == null) {
            throw new IllegalArgumentException(
                "Availability not found for student: " + studentId
            );
        }

        return availability;
    }    
    
    /**
     * Checks whether a student is available for the requested interval
     * @param studentId - Student's Identification Number (ID)
     * @param interval  - Interval to check
     * @return          - True or False
     */
    public boolean isAvailable(String studentId, TimeInterval interval) {

        StudentAvailability availability = getAvailability(studentId);
        return availability.covers(interval);
    }
}