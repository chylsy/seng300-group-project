package campusplan;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import campusplan.domain.StudentAvailability;
import campusplan.domain.TimeInterval;

public class StudentAvailabilityTest {

    @Test
    public void mergesConnectedIntervals() {

        StudentAvailability availability = new StudentAvailability("S001");

        LocalDate date = LocalDate.of(2026, 10, 25);

        /* 9:00AM - 11:00AM */
        availability.add(new TimeInterval(
            date, LocalTime.of(9, 0), LocalTime.of(11, 0)
        ));

        /* 1:00PM - 3:00PM */
        availability.add(new TimeInterval(
            date, LocalTime.of(13, 0), LocalTime.of(15, 0)
        ));

        /* 10:30AM-1:00PM */
        availability.add(new TimeInterval(
            date, LocalTime.of(10, 30), LocalTime.of(13, 0)
        ));

        /* Adding these intervals together should create one time interval from 9:00AM - 3:00PM */
        assertEquals(1, availability.getIntervals().size());

        TimeInterval result = availability.getIntervals().get(0);

        assertEquals(LocalTime.of(9, 0), result.getStart());
        assertEquals(LocalTime.of(15, 0), result.getEnd());
    }

    @Test
    public void splitsIntervalOnRemoval() {

        StudentAvailability availability = new StudentAvailability("S001");

        LocalDate date = LocalDate.of(2026, 10, 25);

        /* 9:00AM - 3:00PM */
        availability.add(new TimeInterval(
            date, LocalTime.of(9, 0), LocalTime.of(15, 0)
        ));

        /* Remove the time interval from 11:00AM - 1:00PM */
        availability.remove(new TimeInterval(
            date, LocalTime.of(11, 0), LocalTime.of(13, 0)
        ));

        /* Removing that time interval should split the origihnal interval in two */
        assertEquals(2, availability.getIntervals().size());

        TimeInterval first = availability.getIntervals().get(0);
        TimeInterval second = availability.getIntervals().get(1);

        /* First interval from 9:00AM - 11:00AM */
        assertEquals(LocalTime.of(9, 0), first.getStart());
        assertEquals(LocalTime.of(11, 0), first.getEnd());

        /* Second interval from 1:00PM - 3:00PM */
        assertEquals(LocalTime.of(13, 0), second.getStart());
        assertEquals(LocalTime.of(15, 0), second.getEnd());
    }

    @Test
    public void testCovers() {

        StudentAvailability availability = new StudentAvailability("S001");
        LocalDate date = LocalDate.of(2026, 10, 26);

        /* Student is available from 9:00AM - 12:00PM */
        availability.add(new TimeInterval(
            date, LocalTime.of(9, 0), LocalTime.of(12, 0)
        ));

        /* Case 1: Slot is fully covered (TRUE) */
        TimeInterval slot1 = new TimeInterval(
            date, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );
        assertTrue(availability.covers(slot1));

        /* Case 2: Slot is partially covered (FALSE) */
        TimeInterval slot2 = new TimeInterval(
            date, LocalTime.of(11, 0), LocalTime.of(13, 0)
        );
        assertFalse(availability.covers(slot2));

        /* Case 3: Slot is completely uncovered (FALSE) */
        TimeInterval slot3 = new TimeInterval(
            date, LocalTime.of(14, 0), LocalTime.of(15, 0)
        );
        assertFalse(availability.covers(slot3));
    }

}