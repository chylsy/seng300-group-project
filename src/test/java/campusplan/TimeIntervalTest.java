package campusplan;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import campusplan.domain.TimeInterval;

/**
 * TimeIntervalTest tests functionality of domain class TimeInterval
 */
public class TimeIntervalTest {
    
    @Test 
    public void sameDatesOverlap() {

        /* Test if two time intervals overlap */
        TimeInterval t1 = new TimeInterval(LocalDate.now(),     /* 25 Oct 2026 10AM-3:30PM */
            LocalTime.of(10, 0, 0), 
            LocalTime.of(15, 30, 0));
        TimeInterval t2 = new TimeInterval(LocalDate.now(),     /* 25 Oct 2026 12:30PM-6PM */
            LocalTime.of(12, 30, 0), 
            LocalTime.of(18, 0, 0));

        /* Assert if t1 and t2 overlap (TRUE) */
        assertTrue(t1.overlaps(t2));

        /* Now test with two time intervals that DO NOT overlap */
        /* 25 Oct 2026 4PM-8PM */
        TimeInterval t3 = new TimeInterval(LocalDate.now(),
            LocalTime.of(16, 0, 0), 
            LocalTime.of(20, 0, 0));

        /* t3 begins AFTER t1 concludes, so they do NOT overlap (FALSE) */
        assertFalse(t1.overlaps(t3));
    }

    @Test 
    public void diffDatesOverlap() {

        /* Test with two time intervals that overlap BUT have different dates */

        /* 25 Oct 2026 10AM-3:30PM */
        TimeInterval t1 = new TimeInterval(LocalDate.now(),     
            LocalTime.of(10, 0, 0), 
            LocalTime.of(15, 30, 0));

        /*  26 Oct 2026 12:30PM-6PM */
        TimeInterval t2 = new TimeInterval(LocalDate.now().plusDays(1),
            LocalTime.of(12, 30, 0), 
            LocalTime.of(18, 0, 0));

        /* Assert if t1 and t3 overlap (FALSE) */
        assertFalse(t1.overlaps(t2));
    }

    @Test 
    public void sameDatesTouch() {

        /* Test if two time intervals overlap */
        TimeInterval t1 = new TimeInterval(LocalDate.now(),     /* 25 Oct 2026 10AM-3PM */
            LocalTime.of(10, 0, 0), 
            LocalTime.of(15, 0, 0));
        TimeInterval t2 = new TimeInterval(LocalDate.now(),     /* 25 Oct 2026 3PM-6PM */
            LocalTime.of(15, 0, 0), 
            LocalTime.of(18, 0, 0));

        /* These dates do not overlap, but are perfectly adjacent */
        assertFalse(t1.overlaps(t2));           /* FALSE */
        assertTrue(t1.touchesOrOverlaps(t2));   /* TRUE */
    }

    @Test 
    public void diffDatesTouch() {

        /* 25 Oct 2026 10AM-3PM */
        TimeInterval t1 = new TimeInterval(LocalDate.now(),     
            LocalTime.of(10, 0, 0), 
            LocalTime.of(15, 0, 0));

        /* 25 Oct 2026 3PM-6PM */
        TimeInterval t2 = new TimeInterval(LocalDate.now().plusDays(1),
            LocalTime.of(15, 0, 0), 
            LocalTime.of(18, 0, 0));

        /* Test two time intervals that touch BUT are on different dates */
        assertFalse(t1.touchesOrOverlaps(t2));
    }

    @Test 
    public void coveredTimeIntervals() {

        /* 25 Oct 2026 8:30AM-5PM */
        TimeInterval t1 = new TimeInterval(LocalDate.now(),     
            LocalTime.of(8, 30, 0), 
            LocalTime.of(17, 0, 0));

        /* 25 Oct 2026 12PM-3:30PM */
        TimeInterval t2 = new TimeInterval(LocalDate.now(),
            LocalTime.of(12, 0, 0), 
            LocalTime.of(15, 30, 0));

        /* t1 covers t2, but t2 does NOT cover t1 */
        assertTrue(t1.covers(t2));
        assertFalse(t2.covers(t1));
    }


}
