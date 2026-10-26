package campusplan.domain;

import java.util.*;

public class StudentAvailability {

    private final String studentId;

    private List<TimeInterval> intervals;

    public StudentAvailability(String studentId) {

        this.studentId = studentId;
        this.intervals = new ArrayList<>();
    }

    public void add(TimeInterval i) {


    }

    public void remove(TimeInterval i) {


    }

    public boolean covers(TimeInterval slot) {

        return false;
    }

    private void mergeTouchingOrOverlapping() {


    }

    /* Accessor (Getter) Methods: */

    public String getStudentId() {

        return studentId;
    }

    public List<TimeInterval> getIntervals() {

        return Collections.unmodifiableList(intervals);
    }



}