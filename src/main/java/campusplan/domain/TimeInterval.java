package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;

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

    public int minutes() {

        return 0;
    }

}