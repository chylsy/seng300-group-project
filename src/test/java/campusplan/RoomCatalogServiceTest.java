package campusplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.ValidationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import campusplan.service.RoomCatalogService;
import org.junit.jupiter.api.Test;

class RoomCatalogServiceTest {
    private static final Set<RoomFeature> NONE=Set.of();
    private static Room room(String id, int capacity, Set<RoomFeature> features) {
        return new Room(id, "Room "+id, capacity,features);
    }

    private static CampusPlanData dataWith(Room... rooms) {
        CampusPlanData data =new CampusPlanData();
        for (Room r:rooms) {
            data.getRooms().put(r.getId(),r);
        }
        return data;
    }

    private static List<String> ids(List<Room> rooms) {
        return rooms.stream().map(Room::getId).collect(Collectors.toList());
    }

    //listAll
    @Test
    void listAllReturnsEveryRoomSortedById() {
        RoomCatalogService service = new RoomCatalogService(dataWith(room("r3", 10,NONE), room("r1", 4,NONE), room("r2",6, NONE)));
        assertEquals(List.of("r1","r2", "r3"),ids(service.listAll()));
    }

    @Test
    void listAllOnEmptyCatalogueReturnsEmptyList(){
        assertTrue(new RoomCatalogService(dataWith()).listAll().isEmpty());
    }
    @Test
    void changingTheReturnedListDoesNotChangeTheCatalogue(){
        RoomCatalogService service=new RoomCatalogService(dataWith(room("r1", 4, NONE)));
        service.listAll().clear();
        assertEquals(1,service.listAll().size());
    }

    //filter by capacity
    @Test
    void filterByMinimumCapacityKeepsRoomsWithAtLeastThatManySeats() {
        RoomCatalogService service = new RoomCatalogService(dataWith(room("r1", 4, NONE), room("r2", 6, NONE), room("r3", 10, NONE)));
        assertEquals(List.of("r2", "r3"), ids(service.filter(6, NONE)));
    }

    @Test
    void roomWithExactlyTheMinimumCapacityIsIncluded(){
        RoomCatalogService service = new RoomCatalogService(dataWith(room("r1", 6, NONE)));
        assertEquals(List.of("r1"),ids(service.filter(6, NONE)) );
        assertTrue(service.filter(7, NONE).isEmpty());
    }

    //filter by features
    @Test
    void filterRequiresEveryRequestedFeature() {
        Room both=room("r1", 10, Set.of(RoomFeature.WHITEBOARD, RoomFeature.PROJECTOR));
        Room boardOnly = room("r2",10,Set.of(RoomFeature.WHITEBOARD));
        RoomCatalogService service =new RoomCatalogService(dataWith(both, boardOnly));
        Set<RoomFeature> required =Set.of(RoomFeature.WHITEBOARD,RoomFeature.PROJECTOR);
        assertEquals(List.of("r1"),ids(service.filter(1, required)));
    }

    @Test
    void featuresOnlyFilterWorksWithMinimumCapacityOne() {
        RoomCatalogService service=new RoomCatalogService(dataWith(room("r1", 2, Set.of(RoomFeature.ACCESSIBLE)), room("r2", 30, NONE)));
        assertEquals(List.of("r1"),ids(service.filter(1,Set.of(RoomFeature.ACCESSIBLE))));
    }

    @Test
    void nullOrEmptyFeatureSetMeansNoFeatureFilter() {
        RoomCatalogService service=new RoomCatalogService(dataWith(room("r1",5, NONE), room("r2", 8, Set.of(RoomFeature.PROJECTOR))));
        assertEquals(List.of("r1", "r2"),ids(service.filter(1,NONE)));
        assertEquals(List.of("r1","r2"),ids(service.filter(1,null)));
    }

    //combined and empty results
    @Test
    void capacityAndFeaturesAreApplied() {
        RoomCatalogService service =new RoomCatalogService(dataWith(room("r1", 4, Set.of(RoomFeature.PROJECTOR)), room("r2", 10, Set.of(RoomFeature.PROJECTOR)), room("r3", 10, NONE)));
        assertEquals(List.of("r2"),ids(service.filter(6, Set.of(RoomFeature.PROJECTOR))));
    }

    @Test
    void noMatchReturnsEmptyListNotError() {
        RoomCatalogService service =new RoomCatalogService(dataWith(room("r1", 4, NONE)));
        assertTrue(service.filter(100, Set.of(RoomFeature.ACCESSIBLE)).isEmpty());
    }

    @Test
    void filterOnEmptyCatalogueReturnsEmptyList(){
        assertTrue(new RoomCatalogService(dataWith()).filter(1, NONE).isEmpty());
    }

    //invalid input leaves the catalogue unchanged
    @Test
    void minimumCapacityOutsideOneToHundredRejected() {
        CampusPlanData data=dataWith(room("r1", 4, NONE));
        RoomCatalogService service = new RoomCatalogService(data);
        assertThrows(ValidationException.class, ()->service.filter(0, NONE));
        assertThrows(ValidationException.class,()->service.filter(101,NONE));
        assertThrows(ValidationException.class, ()->service.filter(-1, NONE));
        assertEquals(1, data.getRooms().size());
        assertEquals(1, service.filter(1, NONE).size());
    }

    @Test
    void minimumCapacityBoundariesOneNHundredAccepted() {
        RoomCatalogService service =new RoomCatalogService(dataWith(room("r1", 1, NONE), room("r2", 100, NONE)));
        assertEquals(List.of("r1", "r2"), ids(service.filter(1,NONE)));
        assertEquals(List.of("r2"), ids(service.filter(100, NONE)));
    }
}