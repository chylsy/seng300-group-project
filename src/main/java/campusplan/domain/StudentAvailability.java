package campusplan.domain;

import java.util.*;
import java.time.*;

public class StudentAvailability {

    private final String studentId;

    private List<TimeInterval> intervals;

    public StudentAvailability(String studentId) {

        this.studentId = studentId;
        this.intervals = new ArrayList<>();
    }

    public void add(TimeInterval i) {

        TimeInterval merged = i;

        /* Check existing intervals for overlaps or adjacency */
        boolean foundMerge;

        do {
            foundMerge = false;

            Iterator<TimeInterval> iterator = intervals.iterator();

            while (iterator.hasNext()) {
                TimeInterval existing = iterator.next();

                if (existing.touchesOrOverlaps(merged)) {

                    /* Find earliest start time */
                    LocalTime mergedStart;

                    if (existing.getStart().isBefore(merged.getStart())) {
                        mergedStart = existing.getStart();
                    } else {
                        mergedStart = merged.getStart();
                    }

                    /* Find latest end time */
                    LocalTime mergedEnd;

                    if (existing.getEnd().isAfter(merged.getEnd())) {
                        mergedEnd = existing.getEnd();
                    } else {
                        mergedEnd = merged.getEnd();
                    }

                    /* Create combined interval */
                    merged = new TimeInterval(
                        merged.getDate(),
                        mergedStart,
                        mergedEnd
                    );

                    iterator.remove();
                    foundMerge = true;
                }
            }

        } while (foundMerge);

        /* Add final merged interval */
        intervals.add(merged);
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