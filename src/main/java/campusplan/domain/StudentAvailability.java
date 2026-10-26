package campusplan.domain;

import java.util.*;
import java.time.*;

public class StudentAvailability {

    private final String studentId;

    private final List<TimeInterval> intervals;

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

        intervals.add(i);
        mergeTouchingOrOverlapping();
    }

    /**
     * Removes a time interval from the student's availability and preserves 
     * any portions of existing intervals that fall outside the removed interval
     * @param i - Time interval to remove
     * */
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

    /**
     * Checks whether any of the student's time intervals fully covers the requested time slot
     * @param slot - Time slot (interval) to check
     * @return     - True or False
     * */
    public boolean covers(TimeInterval slot) {

        for (TimeInterval existing : intervals) {

            /* We only need to check whether at least one interval covers slot */
            if (existing.covers(slot)) return true;
        }

        /* No time interval covers slot */
        return false;
    }

    /**
     * Merges overlapping or adjacent availability intervals
     */
    private void mergeTouchingOrOverlapping() {

        boolean foundMerge;

        do {
            foundMerge = false;

            for (int a = 0; a < intervals.size(); a++) {
                for (int b = a + 1; b < intervals.size(); b++) {

                    TimeInterval first = intervals.get(a);
                    TimeInterval second = intervals.get(b);

                    /* Check existing intervals for overlaps or adjacency */
                    if (first.touchesOrOverlaps(second)) {

                        /* Find earliest start time */
                        LocalTime mergedStart;

                        if (first.getStart().isBefore(second.getStart())) {
                            mergedStart = first.getStart();
                        } else {
                            mergedStart = second.getStart();
                        }

                        /* Find latest end time */
                        LocalTime mergedEnd;

                        if (first.getEnd().isAfter(second.getEnd())) {
                            mergedEnd = first.getEnd();
                        } else {
                            mergedEnd = second.getEnd();
                        }

                        /* Create combined interval */
                        TimeInterval merged = new TimeInterval(
                            first.getDate(),
                            mergedStart,
                            mergedEnd
                        );

                        /* Replace the first interval and remove the second  */
                        intervals.set(a, merged);
                        intervals.remove(b);

                        foundMerge = true;
                        break;
                    }
                }

                if (foundMerge) {
                    break;
                }
            }

        } while (foundMerge);
    }

    /* Accessor (Getter) Methods: */

    public String getStudentId() {

        return studentId;
    }

    public List<TimeInterval> getIntervals() {

        return Collections.unmodifiableList(intervals);
    }
}