package campusplan;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import campusplan.domain.TimeInterval;
import campusplan.error.ValidationException;

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

    @Test 
    public void testDurations() {

        /* Case 1: 1 hour interval (8:30AM - 9:30AM) */
        TimeInterval t1 = new TimeInterval(
            LocalDate.now(),
            LocalTime.of(8, 30),
            LocalTime.of(9, 30)
        );

        /* 1 hour = 60 minutes */
        assertEquals(60, t1.minutes());

        /* Case 2: 3.5 hour interval (8:00AM - 11:30 AM) */
        TimeInterval t2 = new TimeInterval(
            LocalDate.now(),
            LocalTime.of(8, 0),
            LocalTime.of(11, 30)
        );       

        /* 3.5 hours = 210 minutes */
        assertEquals(210, t2.minutes());

        /* Case 3: 8.5 hour interval (8:00AM - 4:30PM) */
        TimeInterval t3 = new TimeInterval(
            LocalDate.now(),
            LocalTime.of(8, 0),
            LocalTime.of(16, 30)
        );

        /* 8.5 hours = 510 minutes */
        assertEquals(510, t3.minutes());
    }


    /* ==================== VALIDATION (FR-02) ==================== */

    @Test
    public void rejectsIntervalOutsideOpeningHours() {

        /* Starts before opening (7:00AM - 9:00AM) */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(7, 0), LocalTime.of(9, 0)));

        /* Ends after closing (9:00PM - 10:30PM) */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(21, 0), LocalTime.of(22, 30)));
    }

    @Test
    public void rejectsStartAfterEnd() {

        /* Start after end (2:00PM - 1:00PM) */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(14, 0), LocalTime.of(13, 0)));

        /* Start equals end (2:00PM - 2:00PM) */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(14, 0), LocalTime.of(14, 0)));
    }

    @Test
    public void rejectsCrossingDates() {

        /* An interval belongs to one date, so it cannot run past midnight (9:00PM - 1:00AM) */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(21, 0), LocalTime.of(1, 0)));
    }

    @Test
    public void rejectsTimesOffThirtyMinuteBoundaries() {

        /* 10:15AM is not on a 30-minute boundary */
        assertThrows(ValidationException.class, () -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(10, 15), LocalTime.of(11, 0)));
    }

    @Test
    public void acceptsOpeningAndClosingBoundaries() {

        /* Exactly 8:00AM start and exactly 10:00PM end are both allowed */
        assertDoesNotThrow(() -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(8, 0), LocalTime.of(8, 30)));
        assertDoesNotThrow(() -> new TimeInterval(
            LocalDate.of(2026, 11, 24), LocalTime.of(21, 30), LocalTime.of(22, 0)));
    }
}
