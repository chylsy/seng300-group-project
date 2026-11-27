package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class TimeInterval {

    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;

    /**
     * Constructor instantiates new TimeInterval object
     * @param date  - Date of time interval
     * @param start - Start time of time interval
     * @param end   - End time of time interval
     */
    public TimeInterval(LocalDate date, LocalTime start, LocalTime end) {

        Validation.require(date != null, "date", "Date is required.");
        Validation.require(start != null, "start", "Start time is required.");
        Validation.require(end != null, "end", "End time is required.");

        /* Times must sit on 30-minute boundaries (e.g. 10:00 or 10:30, not 10:15) */
        Validation.onHalfHour(start);
        Validation.onHalfHour(end);

        /* An interval covers part of one day, so start must come before end */
        Validation.require(start.isBefore(end), "end",
            "Start time must be before end time.");

        /* Must fall within opening hours, 08:00-22:00 */
        Validation.require(!start.isBefore(Room.OPEN) && !end.isAfter(Room.CLOSE), "time",
            "Interval must be within opening hours " + Room.OPEN + "-" + Room.CLOSE + ".");

        this.date   = date;
        this.start  = start;
        this.end    = end;
    }

    /**
     * This method determines if two time intervals overlap
     * @param o - The time interval to compare
     * @return  - True or False
     */
    public boolean overlaps(TimeInterval o) {

        return this.date.equals(o.date)
            && this.start.isBefore(o.end)
            && o.start.isBefore(this.end)
        ;
    }

    /**
     * This method checks if two time intervals are adjacent or overlap
     * @param o - The time interval to compare
     * @return  - True or False
     */
    public boolean touchesOrOverlaps(TimeInterval o) {

        return this.date.equals(o.date)
            && (this.start.isBefore(o.end) || this.start.equals(o.end))
            && (o.start.isBefore(this.end) || o.start.equals(this.end))
        ;
    }
    
    /**
     * This method checks if a time interval covers another
     * @param o - The time interval to check is being covered
     * @return  - True or False
     */
    public boolean covers(TimeInterval o) {

        return this.date.equals(o.date)
            && (this.start.isBefore(o.start) || this.start.equals(o.start))
            && (o.end.isBefore(this.end) || o.end.equals(this.end))
        ;
    }

    /**
     * This method returns the duration of the time interval in minutes
     * @return - An integer representing the duration of the time interval
     */
    public int minutes() {

        return (int) ChronoUnit.MINUTES.between(this.start, this.end);
    }

    /* Accessor (Getter) Methods */

    public LocalDate getDate() {

        return date;
    }

    public LocalTime getStart() {

        return start;
    }

    public LocalTime getEnd() {
        
        return end;
    }
}