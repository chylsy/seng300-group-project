package campusplan.service;

import campusplan.domain.*;

public class AvailabilityService {

    private final CampusPlanData data;

    public AvailabilityService(CampusPlanData data) {
        this.data = data;
    }

    public void addAvailability(String studentId, TimeInterval interval) {


    }


    public void removeAvailability(String studentId, TimeInterval interval) {


    }

    public StudentAvailability getAvailability(String studentId) {

        return null;
    }

    public boolean isAvailable(String studentId, TimeInterval interval) {

        return false;
    }
    
}