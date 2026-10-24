package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import campusplan.domain.*;
import campusplan.ValidationException;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomTest {
    private static Room room(String name, int capacity, Set<RoomFeature> features) {
        return new Room("room-1",name,capacity,features);
    }
    //name:1-60 characters after trimming
    @Test
    void nameAtLowerAndUpperLimitIsAccepted(){
        assertEquals("A", room("A", 10, Set.of()).getName());
        String sixty = "x".repeat(60);
        assertEquals(sixty, room(sixty, 10, Set.of()).getName());
    }

    @Test
    void nameOutsideLimitsIsRejected() {
        assertThrows(ValidationException.class, () -> room("", 10, Set.of()));
        assertThrows(ValidationException.class, () -> room("   ", 10, Set.of()));
        assertThrows(ValidationException.class, () -> room("x".repeat(61), 10, Set.of()));
        assertThrows(ValidationException.class, () -> room(null, 10, Set.of()));
    }

    @Test
    void nameIsMeasuredAfterTrimming() {
        assertEquals("Lab", room("  Lab  ", 10, Set.of()).getName());
        String sixtyPadded = "  " + "x".repeat(60)+ "  ";
        assertEquals(60, room(sixtyPadded, 10, Set.of()).getName().length());
    }
    //capacity: 1-100
    @Test
    void capacityBoundaries(){
        assertThrows(ValidationException.class, ()->room("R", 0, Set.of()));
        assertEquals(1, room("R", 1, Set.of()).getCapacity());
        assertEquals(100, room("R", 100, Set.of()).getCapacity());
        assertThrows(ValidationException.class, () ->room("R", 101, Set.of()));
        assertThrows(ValidationException.class, ()->room("R", -5, Set.of()));
    }
    //id
    @Test
    void blankIdIsRejected(){
        assertThrows(ValidationException.class,() ->new Room("  ","R", 10, Set.of()));
        assertThrows(ValidationException.class, () ->new Room(null, "R", 10,Set.of()));
    }
    //features:any subset
    @Test
    void anySubsetOfFeaturesIsAccepted() {
        assertTrue(room("R", 10, Set.of()).getFeatures().isEmpty());
        assertEquals(Set.of(RoomFeature.PROJECTOR),room("R", 10, Set.of(RoomFeature.PROJECTOR)). getFeatures());
        assertEquals(EnumSet.allOf(RoomFeature.class),room("R", 10, EnumSet.allOf(RoomFeature.class)).getFeatures());
    }
    @Test
    void nullFeaturesAreRejected(){
        assertThrows(ValidationException.class,() ->room("R", 10,null));
        Set<RoomFeature> withNull = new HashSet<>();
        withNull.add(null);
        assertThrows(ValidationException.class,() ->room("R", 10, withNull));
    }

    @Test
    void featuresAreCopiedAndCannotBeChanged() {
        Set<RoomFeature> original=EnumSet.of(RoomFeature.WHITEBOARD);
        Room r=room("R", 10,original);
        original.add(RoomFeature.PROJECTOR);
        assertEquals(Set.of(RoomFeature.WHITEBOARD), r.getFeatures());
        assertThrows(UnsupportedOperationException.class,() -> r.getFeatures().add(RoomFeature.ACCESSIBLE));
    }
    //canHold: a room holding five cannot host six invitees
    @Test
    void canHoldChecksSeats() {
        Room five=room("R", 5, Set.of());
        assertTrue(five.canHold(4));
        assertTrue(five.canHold(5));
        assertFalse(five.canHold(6));
    }

    //hasAll
    @Test
    void hasAllRequiresEveryFeature(){
        Room r =room("R", 10,Set.of(RoomFeature.WHITEBOARD));
        assertTrue(r.hasAll(Set.of()));
        assertTrue(r.hasAll(null));
        assertTrue(r.hasAll(Set.of(RoomFeature.WHITEBOARD)));
        assertFalse(r.hasAll(Set.of(RoomFeature.PROJECTOR)) );
        assertFalse(r.hasAll(Set.of(RoomFeature.WHITEBOARD,RoomFeature.PROJECTOR)));
    }
    //unique name ignoring case
    @Test
    void nameKeyIgnoresCaseAndSurroundingSpaces(){
        assertEquals(room(" Lab A ", 10, Set.of()).nameKey(), room("lab a", 10, Set.of()).nameKey());
        assertEquals("lab a",room("LAB A", 10, Set.of()).nameKey() );
    }

    @Test
    void openingHoursAreEightToTen(){
        assertEquals(LocalTime.of(8, 0),Room.OPEN);
        assertEquals(LocalTime.of(22, 0),Room.CLOSE);
    }
    @Test
    void roomsWithTheSameIdAreEqual() {
        Room a=new Room("room-1", "A", 10,Set.of());
        Room b=new Room("room-1", "B", 20,Set.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}