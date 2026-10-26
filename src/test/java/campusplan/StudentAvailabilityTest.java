package campusplan;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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



}