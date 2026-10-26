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

    /**
     * Adds a time interval to the student's availability and merges overlapping or
     * adjacent intervals into a single interval
     * @param i - Time interval to add
     * */
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

    /**
     * Removes a time interval from the student's availability and preserves 
     * any portions of existing intervals that fall outside the removed interval
     * @param i - Time interval to remove
     */
    public void remove(TimeInterval i) {

        List<TimeInterval> remaining = new ArrayList<>();

        for (TimeInterval existing : intervals) {

            /* No overlap so we keep the existing interval */
            if (!existing.overlaps(i)) {
                remaining.add(existing);
                continue;
            }

            /* Keep any portion before the removed interval */
            if (existing.getStart().isBefore(i.getStart())) {

                remaining.add(new TimeInterval(
                    existing.getDate(),
                    existing.getStart(),
                    i.getStart()
                ));
            }

            /* Keep any portion after the removed interval */
            if (existing.getEnd().isAfter(i.getEnd())) {

                remaining.add(new TimeInterval(
                    existing.getDate(),
                    i.getEnd(),
                    existing.getEnd()
                ));
            }
        }

        /* Replace old intervals with remaining ones */
        intervals.clear();
        intervals.addAll(remaining);
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